package com.demo.processor;

/**
 * @author 尉迟涛
 * create time : 2020/3/3 15:50
 * description :
 */
public class Constants {

    public static final String ARGUMENT_MODULE_NAME = "moduleName";

    public static final String ACTIVITY = "android.app.Activity";
    public static final String FRAGMENT = "androidx.fragment.app.Fragment";

    public static final String ROUTE_GROUP = "com.demo.core.interfaces.IRouteGroup";
    public static final String ROUTE_ROOT = "com.demo.core.interfaces.IRouteRoot";

    public static final String GROUP_CLASS_NAME = "Router$$Group$$";
    public static final String GROUP_PARAM_NAME = "routers";
    public static final String GROUP_METHOD_NAME = "loadInto";

    public static final String ROOT_CLASS_NAME = "Router$$Root$$";
    public static final String ROOT_PARAM_NAME = "groups";
    public static final String ROOT_METHOD_NAME = "loadInto";

    public static final String PACKAGE_NAME = "com.demo.router.generated";
}
