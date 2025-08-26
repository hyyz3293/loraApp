package com.lora.cn;

import android.app.Application;

import com.blankj.utilcode.util.Utils;

public class LoraApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
    }
}
