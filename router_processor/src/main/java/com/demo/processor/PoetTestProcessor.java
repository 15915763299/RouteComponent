package com.demo.processor;


import com.demo.annotation.PoetTest;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * 使用JavaPoet实现自动生成代码
 * github：https://github.com/square/javapoet
 * 参考文章：
 * https://blog.csdn.net/u010976213/article/details/91999309
 * https://www.jianshu.com/p/160a832ce135
 */
@AutoService(Processor.class)
// java 8
@SupportedSourceVersion(SourceVersion.RELEASE_8)
// 指定注解
@SupportedAnnotationTypes({"com.demo.annotation.PoetTest"})
public class PoetTestProcessor extends AbstractProcessor {

    private boolean isGenerated = false;

//    @Override
//    public Set<String> getSupportedAnnotationTypes() {
//        Set<String> supportTypes = new LinkedHashSet<>();
//        supportTypes.add(PoetTest.class.getCanonicalName());
//        return supportTypes;
//    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        // 注意：注解处理过程是需要经过多轮处理的
        // 编译的时候会报 javax.annotation.processing.FilerException: Attempt to recreate a file for type XX
        if (!isGenerated) {
            // create method
            MethodSpec main = MethodSpec.methodBuilder("main")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(String[].class, "args")
                    .addStatement("$T.out.println($S)", System.class, "这是我用JavaPoet自动生成的代码，现在被执行了")
                    .build();

            // create class, add method
            TypeSpec typeSpec = TypeSpec.classBuilder("HelloPoet")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(main)
                    .build();

            // create file, add class
            JavaFile javaFile = JavaFile.builder("com.demo.processor", typeSpec)
                    .build();

            try {
                javaFile.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                e.printStackTrace();
            }
            isGenerated = true;
        }
        return true;
    }
}

//    $S/$T find in source code
//    private void addArgument(String format, char c, Object arg) {
//      switch (c) {
//        case 'N':
//          this.args.add(argToName(arg));
//          break;
//        case 'L':
//          this.args.add(argToLiteral(arg));
//          break;
//        case 'S':
//          this.args.add(argToString(arg));
//          break;
//        case 'T':
//          this.args.add(argToType(arg));
//          break;
//        default:
//          throw new IllegalArgumentException(
//              String.format("invalid format string: '%s'", format));
//      }
//    }