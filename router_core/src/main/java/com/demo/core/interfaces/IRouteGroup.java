package com.demo.core.interfaces;

import com.demo.annotation.RouterMeta;

import java.util.Map;

/**
 * 路由组
 */
public interface IRouteGroup {

    void loadInto(Map<String, RouterMeta> routerMetaMap);

}
