package com.demo.core.interfaces;

import java.util.Map;

/**
 * 根节点
 */
public interface IRouteRoot {

    void loadInto(Map<String, Class<? extends IRouteGroup>> routers);

}
