package com.demo.core.core;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.demo.annotation.RouterMeta;
import com.demo.core.Depository;
import com.demo.core.Utils;
import com.demo.core.interfaces.IRouteGroup;
import com.demo.core.interfaces.IRouteRoot;

import java.util.Map;
import java.util.Set;

/**
 * 入口
 */
public class JumpRouter {

    /**
     * 饿汗，类的延迟加载
     */
    private static class Instance {
        static JumpRouter router = new JumpRouter();
    }

    public static JumpRouter get() {
        return Instance.router;
    }

    private static final String PACKAGE_NAME = "com.demo.router.generated";
    private static final String ROOT_CLASS_NAME = "Router$$Root$$";
    //private static final String GROUP_CLASS_NAME = "JumpRouter$$Group$$";
    private static final String TAG = "JumpRouter";
    private Application application;

    private Handler mHandler;

    private JumpRouter() {
        mHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 初始化-收集路由表，必须在Application中初始化
     */
    public void initRouter(Application application) {
        this.application = application;
        try {
            loadRouteTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 收集加载路由表
     */
    private void loadRouteTable() throws Exception {
        if (application == null) {
            throw new IllegalArgumentException("initRouter(Application application) must Application init");
        }

        //获取所有APT 生成的路由类的全类名
        Set<String> routerMap = Utils.getFileNameByPackageName(application, PACKAGE_NAME);
        for (String className : routerMap) {
            //获取root中注册的路由分组信息，存储到本地仓库中。
            if (className.startsWith(PACKAGE_NAME + "." + ROOT_CLASS_NAME)) {
                Object instance = Class.forName(className).getConstructor().newInstance();
                if (instance instanceof IRouteRoot) {
                    IRouteRoot routeRoot = (IRouteRoot) instance;
                    routeRoot.loadInto(Depository.rootMap);
                }
            }
        }

        Log.e(TAG, "路由表分组信息 「");
        for (Map.Entry<String, Class<? extends IRouteGroup>> entry : Depository.rootMap.entrySet()) {
            Log.e(TAG, "【key --> " + entry.getKey() + ": value --> " + entry.getValue() + "]");
        }
        Log.e(TAG, " 」");
    }

    public JumpCard jump(String path) {
        if (TextUtils.isEmpty(path)) {
            throw new RuntimeException("路由地址无效");
        } else {
            return new JumpCard(path, getGroupName(path));
        }
    }

    public JumpCard jump(String path, String group) {
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(group)) {
            throw new RuntimeException("路由地址无效");
        } else {
            return new JumpCard(path, group);
        }
    }

    public JumpCard jump(String path, String group, Bundle bundle) {
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(group)) {
            throw new RuntimeException("路由地址无效");
        } else {
            return new JumpCard(path, group, bundle);
        }
    }


    /**
     * 开始跳转
     */
    public Object navigation(Context context, final JumpCard jumpCard, final int requestCode) {
        try {
            produceJumpCard(jumpCard);
        } catch (Exception e) {
            e.printStackTrace();
        }
        switch (jumpCard.getType()) {
            case ACTIVITY:
                if (context == null) {
                    context = this.application;
                }
                final Intent intent = new Intent(context, jumpCard.getDestination());
                intent.putExtras(jumpCard.getExtras());
                if (jumpCard.getFlags() != -1) {
                    intent.setFlags(jumpCard.getFlags());
                } else if (!(context instanceof Activity)) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                final Context finalContext = context;
                mHandler.post(() -> {
                    //可能需要返回码
                    if (requestCode > 0) {
                        ActivityCompat.startActivityForResult(
                                (Activity) finalContext,
                                intent,
                                requestCode,
                                jumpCard.getOptionsBundle()
                        );
                    } else {
                        ActivityCompat.startActivity(
                                finalContext,
                                intent,
                                jumpCard.getOptionsBundle()
                        );
                    }

                    if ((0 != jumpCard.getEnterAnim() || 0 != jumpCard.getExitAnim()) && finalContext instanceof Activity) {
                        //老版本
                        ((Activity) finalContext).overridePendingTransition(
                                jumpCard.getEnterAnim(),
                                jumpCard.getExitAnim()
                        );
                    }
                });
                break;
            case FRAGMENT:
                Class<?> fragment = jumpCard.getDestination();
                try {
                    Object instance = fragment.getConstructor().newInstance();
                    if (instance instanceof Fragment) {
                        ((Fragment) instance).setArguments(jumpCard.getExtras());
                    }
                    return instance;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            default:
                break;
        }
        return null;
    }

    /**
     * 准备跳卡
     */
    private void produceJumpCard(JumpCard card) {
        //获取仓库中存储的 具体每个组的信息
        RouterMeta routerMeta = Depository.groupMap.get(card.getPath());
        if (routerMeta == null) {//没有记录在仓库中，从路由表的分组信息中查找
            Class<? extends IRouteGroup> groupClass = Depository.rootMap.get(card.getGroup());
            if (groupClass == null) {
                throw new RuntimeException("没有找到对应的路由表信息：" + card.getGroup() + ":" + card.getPath());
            }
            IRouteGroup routeGroup;
            try {
                routeGroup = groupClass.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("路由映射表信息记录失败");
            }
            routeGroup.loadInto(Depository.groupMap);
            //已经准备过了,并将路由的信息存储在groupMap ， 就可以移除了
            Depository.rootMap.remove(card.getGroup());
            produceJumpCard(card);//再次进入
        } else {
            //设置要跳转的类
            card.setDestination(routerMeta.getDestination());
            //设置要跳转的类型
            card.setType(routerMeta.getType());
        }

    }

    /**
     * 通过路由地址获取分组名
     */
    private String getGroupName(String path) {
        if (!path.startsWith("/")) {
            throw new RuntimeException(path + ": 不能有效的提取group，地址必须设置/XX/XX,请查看地址是否设置正确");
        }
        try {
            String group = path.substring(1, path.indexOf("/", 1));
            if (TextUtils.isEmpty(group)) {
                throw new RuntimeException(path + ": 不能有效的提取group,地址必须设置/XX/XX,请查看地址是否设置正确");
            }
            return group;
        } catch (Exception e) {
            return null;
        }
    }

}
