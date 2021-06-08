package com.luqian.rtc;

import android.app.Application;

import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;

/**
 * @author LUQIAN
 * @date 2021/6/8
 */
public class BaseApp extends Application {

    private static BaseApp mBaseApp;

    @Override
    public void onCreate() {
        super.onCreate();
        mBaseApp = this;
        XLog.init(LogLevel.ALL);
    }

    public static BaseApp getApp() {
        return mBaseApp;
    }
}
