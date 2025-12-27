package com.lora.cn;

import android.app.Application;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import java.util.Calendar;

import com.blankj.utilcode.util.Utils;

public class LoraApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
        try {
            com.lora.cn.utils.LogUtils.init(getApplicationContext());
        } catch (Exception ignored) {}
        try { com.lora.cn.utils.CrashLogger.install(getApplicationContext()); } catch (Exception ignored) {}
        try {
            com.lora.cn.utils.LogcatCapture.start(getApplicationContext());
        } catch (Exception ignored) {}
        try {
            com.blankj.utilcode.util.SPUtils sp = com.blankj.utilcode.util.SPUtils.getInstance();
            boolean initialized = sp.getBoolean("inventory_schedule_initialized", false);
            if (!initialized) {
                sp.put("inventory_schedule_hour", 7);
                sp.put("inventory_schedule_minute", 0);
                sp.put("inventory_schedule_enabled", true);
                sp.put("inventory_schedule_initialized", true);
                com.lora.cn.work.InventoryScheduleWorker.scheduleAt(getApplicationContext(), 7, 0);
            } else {
                boolean enabled = sp.getBoolean("inventory_schedule_enabled", true);
                if (enabled) {
                    int h = sp.getInt("inventory_schedule_hour", 7);
                    int m = sp.getInt("inventory_schedule_minute", 0);
                    com.lora.cn.work.InventoryScheduleWorker.scheduleAt(getApplicationContext(), h, m);
                }
            }
        } catch (Exception ignored) {}
    }
    
    private void scheduleInventory(Context ctx, int hour, int minute) {}
}
