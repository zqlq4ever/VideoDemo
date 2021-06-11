package com.luqian.demo;

import com.alibaba.android.arouter.launcher.ARouter;
import com.luqian.rtc.BaseApp;
import com.tencent.bugly.Bugly;

/**
 * @author LUQIAN
 * @date 2021/6/8
 */
public class App extends BaseApp {
    @Override
    public void onCreate() {
        super.onCreate();
        Bugly.init(getApplicationContext(), "3576273b6e", false);
        if (BuildConfig.DEBUG) {           // 这两行必须写在init之前，否则这些配置在init过程中将无效
            ARouter.openLog();     // 打印日志
            ARouter.openDebug();   // 开启调试模式(如果在InstantRun模式下运行，必须开启调试模式！线上版本需要关闭,否则有安全风险)
        }
        ARouter.init(getApp()); // 尽可能早，推荐在Application中初始化
    }
}
