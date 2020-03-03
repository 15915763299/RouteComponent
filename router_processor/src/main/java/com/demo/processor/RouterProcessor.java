package com.demo.processor;

import com.demo.annotation.Router;
import com.demo.annotation.RouterMeta;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Router 注解处理器
 * <p>
 * AutoService: AnnotationProcessor register
 * <p>
 * SupportedSourceVersion: compiled java version
 * {@link AbstractProcessor#getSupportedSourceVersion()}
 * <p>
 * SupportedAnnotationTypes: Allow AnnotationProcessor process annotation
 * {@link AbstractProcessor#getSupportedAnnotationTypes()}
 * <p>
 * SupportedOptions: 注解处理器接收的参数
 * {@link AbstractProcessor#getSupportedOptions()}
 */
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({"com.demo.annotation.Router"})
@SupportedOptions({Constants.ARGUMENT_MODULE_NAME})
public class RouterProcessor extends AbstractProcessor {

    /**
     * 日志，用Messager打印消息
     */
    private Log log;
    /**
     * 文件生成器 类/资源
     */
    private Filer filer;
    /**
     * 类型工具类
     */
    private Types typeUtils;
    /**
     * 节点工具类
     */
    private Elements elementUtils;
    /**
     * 模块的名称
     */
    private String moduleName;
    /**
     * 根： key 组名  value 类名
     */
    private Map<String, String> rootMap = new TreeMap<>();

    /**
     * 组： key 组名  value 对应组的路由信息
     * 组名默认是path的第一个节点，比如“/app/main”的默认组名就是app
     */
    private Map<String, List<RouterMeta>> groupMap = new HashMap<>();

    /**
     * init process environment utils
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        filer = processingEnvironment.getFiler();
        typeUtils = processingEnvironment.getTypeUtils();
        elementUtils = processingEnvironment.getElementUtils();

        // 这里要使用特殊的log才能打印信息
        Messager messager = processingEnvironment.getMessager();
        log = Log.newLog(messager);

        // 获取传递的参数，在gradle配置
        Map<String, String> options = processingEnvironment.getOptions();
        if (!options.isEmpty()) {
            moduleName = options.get(Constants.ARGUMENT_MODULE_NAME);
            log.i("module:" + moduleName);
        }
        if (Utils.isEmpty(moduleName)) {
            throw new NullPointerException("Not set Processor Parameter");
        }
    }

    /**
     * process annotation
     *
     * @param set              The set of nodes that support processing annotations
     * @param roundEnvironment Current or previous operating environment，annotation that can be found by this object
     * @return true already processed ，follow-up will not be dealt with
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (!set.isEmpty()) {
            //Set nodes annotated by Router
            Set<? extends Element> annotatedWith = roundEnvironment.getElementsAnnotatedWith(Router.class);
            if (annotatedWith != null) {
                processRouter(annotatedWith);
            }
            return true;
        }
        return false;
    }

    /**
     * 处理被注解的节点集合
     */
    private void processRouter(Set<? extends Element> annotatedWith) {
        TypeElement activity = elementUtils.getTypeElement(Constants.ACTIVITY);
        TypeElement fragment = elementUtils.getTypeElement(Constants.FRAGMENT);

        //单个的节点
        RouterMeta routerMeta;
        for (Element element : annotatedWith) {
            // 获取类信息 如Activity类
            TypeMirror typeMirror = element.asType();
            // 获取节点的注解信息
            Router annotation = element.getAnnotation(Router.class);
            log.i(typeMirror + " | " + activity.asType());
            //只能指定的类上面使用
            if (typeUtils.isSubtype(typeMirror, activity.asType())) {
                //存储路由相关的信息
                routerMeta = new RouterMeta(RouterMeta.Type.ACTIVITY, annotation, element);
            } else if (typeUtils.isSubtype(typeMirror, fragment.asType())) {
                //存储路由相关的信息
                routerMeta = new RouterMeta(RouterMeta.Type.FRAGMENT, annotation, element);
            } else {
                throw new RuntimeException("Just Support Activity Router!");
            }
            // 验证并添加
            checkRouterGroup(routerMeta);
        }

        //获取组节点
        TypeElement routeGroupElement = elementUtils.getTypeElement(Constants.ROUTE_GROUP);
        //获取根节点
        TypeElement routeRootElement = elementUtils.getTypeElement(Constants.ROUTE_ROOT);

        //生成 $$Group$$ 记录分组表
        generatedGroupTable(routeGroupElement);

        //生成 $$Root$$ 记录路由表
        generatedRootTable(routeRootElement, routeGroupElement);
    }

    /**
     * 检查设置路由组
     */
    private void checkRouterGroup(RouterMeta routerMeta) {
        if (routerVerify(routerMeta)) {
            List<RouterMeta> routerMetas = groupMap.get(routerMeta.getGroup());
            if (Utils.isEmpty(routerMetas)) {
                // 未查到分组，创建分组
                routerMetas = new ArrayList<>();
                routerMetas.add(routerMeta);
                groupMap.put(routerMeta.getGroup(), routerMetas);
            } else {
                // 查到分组，加入组
                routerMetas.add(routerMeta);
            }
        } else {
            log.i("router path no verify, please check");
        }
    }

    /**
     * 验证路由地址配置是否正确合法性
     *
     * @param routerMeta 存储的路由bean对象
     * @return true 路由地址配置正确  false 路由地址配置不正确
     */
    private boolean routerVerify(RouterMeta routerMeta) {
        String path = routerMeta.getPath();
        if (Utils.isEmpty(path)) {
            throw new NullPointerException("@Router path not to be null or to length() == 0");
        }
        if (!path.startsWith("/")) {//路由地址必须以/开头
            throw new IllegalArgumentException("@Router path must / first");
        }

        //检查是否配置group如果没有配置 则从path中截取组名
        String group = routerMeta.getGroup();
        if (Utils.isEmpty(group)) {
            String defaultGroup = path.substring(1, path.indexOf("/", 1));
            //截取的还是为空
            if (Utils.isEmpty(defaultGroup)) {
                return false;
            }
            //设置group
            routerMeta.setGroup(defaultGroup);
        }
        return true;
    }


    /**
     * 该生成类用于将同一分组的路由添加到同一个map中
     * 先生成一个个group
     */
    private void generatedGroupTable(TypeElement routeGroupElement) {
        //创建参数类型 Map<String,RouterMeta>
        ParameterizedTypeName atlas = ParameterizedTypeName.get(ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(RouterMeta.class));

        //创建参数 Map<String,RouterMeta> atlas
        ParameterSpec altlas = ParameterSpec
                .builder(atlas, Constants.GROUP_PARAM_NAME)
                .build();

        //遍历分组，每一个分组，创建一个$$Group$$类
        for (Map.Entry<String, List<RouterMeta>> entry : groupMap.entrySet()) {

            MethodSpec.Builder builder = MethodSpec.methodBuilder(Constants.GROUP_METHOD_NAME)
                    .addModifiers(PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(altlas);

            //遍历 生成函数体
            List<RouterMeta> groupData = entry.getValue();
            for (RouterMeta meta : groupData) {
                //$S = String
                //$T = class
                //添加函数体
                builder.addStatement(
                        Constants.GROUP_PARAM_NAME + ".put($S,$T.build($T.$L,$T.class,$S,$S))",
                        meta.getPath(),
                        ClassName.get(RouterMeta.class),
                        ClassName.get(RouterMeta.Type.class),
                        meta.getType(),
                        ClassName.get((TypeElement) meta.getElement()),
                        meta.getPath(),
                        meta.getGroup()
                );
            }
            MethodSpec loadInto = builder.build();

            String groupClassName = Constants.GROUP_CLASS_NAME + entry.getKey();

            TypeSpec typeSpec = TypeSpec.classBuilder(groupClassName)//类名
                    .addSuperinterface(ClassName.get(routeGroupElement))//实现接口IRouteGroup
                    .addModifiers(PUBLIC)
                    .addMethod(loadInto)
                    .build();

            JavaFile javaFile = JavaFile
                    .builder(Constants.PACKAGE_NAME, typeSpec)//包名和类
                    .build();
            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            rootMap.put(entry.getKey(), groupClassName);
        }
    }
    // 结果举例：
    // public class Router$$Group$$app implements IRouteGroup {
    //   @Override
    //   public void loadInto(Map<String, RouterMeta> routers) {
    //     routers.put("/app/main",RouterMeta.build(RouterMeta.Type.ACTIVITY,MainActivity.class,"/app/main","app"));
    //   }
    // }

    /**
     * 该生成类用于将所有分组加入到同一个根map中
     * 再将所有group一起生成一个root
     */
    private void generatedRootTable(TypeElement routeRootElement, TypeElement routeGroupElement) {
        //类型 Map<String,Class<? extends IRouteGroup>> routes>
        ParameterizedTypeName atlas = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(
                        ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(ClassName.get(routeGroupElement))
                )
        );

        //创建参数  Map<String,Class<? extends IRouteGroup>>> routes
        ParameterSpec altlas = ParameterSpec
                .builder(atlas, Constants.ROOT_PARAM_NAME)//参数名
                .build();//创建参数

        //public void loadInfo(Map<String,Class<? extends IRouteGroup>> routes> routes)
        //函数 public void loadInfo(Map<String,Class<? extends IRouteGroup>> routes> routes)
        MethodSpec.Builder loadIntoMethodOfRootBuilder = MethodSpec.methodBuilder
                (Constants.ROOT_METHOD_NAME)
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(altlas);

        //函数体
        for (Map.Entry<String, String> entry : rootMap.entrySet()) {
            loadIntoMethodOfRootBuilder.addStatement(
                    Constants.ROOT_PARAM_NAME + ".put($S, $T.class)",
                    entry.getKey(),
                    ClassName.get(Constants.PACKAGE_NAME, entry.getValue())
            );
        }

        //生成 $Root$类
        String rootClassName = Constants.ROOT_CLASS_NAME + moduleName;
        try {
            JavaFile.builder(Constants.PACKAGE_NAME,
                    TypeSpec.classBuilder(rootClassName)
                            .addSuperinterface(ClassName.get(routeRootElement))
                            .addModifiers(PUBLIC)
                            .addMethod(loadIntoMethodOfRootBuilder.build())
                            .build()
            ).build().writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // 结果举例：
    // public class Router$$Root$$app implements IRouteRoot {
    //   @Override
    //   public void loadInto(Map<String, Class<? extends IRouteGroup>> groups) {
    //     groups.put("app", Router$$Group$$app.class);
    //   }
    // }

}
