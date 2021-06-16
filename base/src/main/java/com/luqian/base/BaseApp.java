package com.luqian.base;

import android.app.Application;

import com.alibaba.android.arouter.launcher.ARouter;
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
        //  这两行必须写在 init 之前，否则这些配置在 init 过程中将无效
        if (BuildConfig.DEBUG) {
            //  打印日志
            ARouter.openLog();
            //  开启调试模式(如果在InstantRun模式下运行，必须开启调试模式！线上版本需要关闭,否则有安全风险)
            ARouter.openDebug();
        }
        //  尽可能早，推荐在 Application 中初始化
        ARouter.init(getApp());
        XLog.init(LogLevel.ALL);
    }

    public static BaseApp getApp() {
        return mBaseApp;
    }
}
