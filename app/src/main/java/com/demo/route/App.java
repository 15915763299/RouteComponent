package com.demo.route;

import android.app.Application;
import android.content.Context;

import com.demo.core.core.JumpRouter;

/**
 * @author 尉迟涛
 * create time : 2020/3/3 17:35
 * description :
 */
public class App extends Application {

    private static App app;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        app = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        JumpRouter.get().initRouter(this);
    }
}
