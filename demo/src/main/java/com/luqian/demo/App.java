package com.luqian.demo;

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
    }
}
