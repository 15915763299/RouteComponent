package com.demo.core;

import com.demo.annotation.RouterMeta;
import com.demo.core.interfaces.IRouteGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * 存储路由表的本地仓库
 */
public class Depository {

    /**
     * 存储路由表的分组信息
     * HashMap 默认容器大小为16，为了减少HashMap 扩容导致的性能损耗，这里将容器大小设置大一些，具体根据项目预算要多少个。
     */
    public static final Map<String, Class<? extends IRouteGroup>> rootMap = new HashMap<>(30);

    /**
     * 存储路由表保存组的具体组的信息
     */
    public static final Map<String, RouterMeta> groupMap = new HashMap<>(50);

}
