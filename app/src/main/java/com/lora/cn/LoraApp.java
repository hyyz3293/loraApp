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
            try { com.lora.cn.utils.ViewEffects.registerGlobal(this); } catch (Exception ignored) {}
            try {
                com.lora.cn.utils.LogUtils.init(getApplicationContext());
            } catch (Exception ignored) {}
            try { com.lora.cn.utils.CrashLogger.install(getApplicationContext()); } catch (Exception ignored) {}
            try {
            com.lora.cn.utils.LogcatCapture.start(getApplicationContext());
        } catch (Exception ignored) {}
            try {
                com.blankj.utilcode.util.SPUtils sp = com.blankj.utilcode.util.SPUtils.getInstance();
                boolean localEnabled = sp.getBoolean("mqtt_local_broker_enabled", true);
                int localPort = sp.getInt("mqtt_local_broker_port", 1883);
                if (localEnabled) {
                    android.content.Intent svc = new android.content.Intent(getApplicationContext(), com.lora.cn.service.MqttBrokerService.class);
                    svc.putExtra("port", localPort > 0 ? localPort : 1883);
                    try {
                        androidx.core.content.ContextCompat.startForegroundService(getApplicationContext(), svc);
                    } catch (Exception e) {
                        try { getApplicationContext().startService(svc); } catch (Exception ignored) {}
                    }
                    try { sp.put("mqtt_client_in_service", true); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
//        try {
//            com.blankj.utilcode.util.SPUtils sp = com.blankj.utilcode.util.SPUtils.getInstance();
//            boolean initialized = sp.getBoolean("inventory_schedule_initialized", false);
//            if (!initialized) {
//                sp.put("inventory_schedule_hour", 7);
//                sp.put("inventory_schedule_minute", 0);
//                sp.put("inventory_schedule_enabled", true);
//                sp.put("inventory_schedule_initialized", true);
//                com.lora.cn.work.InventoryScheduleWorker.scheduleAt(getApplicationContext(), 7, 0);
//            } else {
//                boolean enabled = sp.getBoolean("inventory_schedule_enabled", true);
//                if (enabled) {
//                    int h = sp.getInt("inventory_schedule_hour", 7);
//                    int m = sp.getInt("inventory_schedule_minute", 0);
//                    com.lora.cn.work.InventoryScheduleWorker.scheduleAt(getApplicationContext(), h, m);
//                }
//            }
//        } catch (Exception ignored) {}
    }
    
//    public static void scheduleInventory(Context ctx, int hour, int minute) {
//        try {
//            com.lora.cn.utils.LogUtils.e("scheduleInventory", "测试测试 hour=" + hour + " minute=" + minute);
//            com.blankj.utilcode.util.SPUtils sp = com.blankj.utilcode.util.SPUtils.getInstance();
//            sp.put("inventory_schedule_hour", hour);
//            sp.put("inventory_schedule_minute", minute);
//            sp.put("inventory_schedule_enabled", true);
//            android.content.Context useCtx = ctx != null ? ctx : com.blankj.utilcode.util.Utils.getApp();
//            com.lora.cn.work.InventoryScheduleWorker.scheduleAt(useCtx, hour, minute);
//        } catch (Exception ignored) {}
//    }
    
//    public static boolean tryBeginInventoryExecute(String key) {
//        try {
//            if (key == null) return false;
//            synchronized (LoraApp.class) {
//                String curr = com.blankj.utilcode.util.SPUtils.getInstance().getString("inventory_executing_key", "");
//                if (key.equals(curr)) return false;
//                com.blankj.utilcode.util.SPUtils.getInstance().put("inventory_executing_key", key);
//                return true;
//            }
//        } catch (Exception ignored) {}
//        return false;
//    }
//
//    public static void endInventoryExecute(String key) {
//        try {
//            synchronized (LoraApp.class) {
//                String curr = com.blankj.utilcode.util.SPUtils.getInstance().getString("inventory_executing_key", "");
//                if (key != null && key.equals(curr)) {
//                    com.blankj.utilcode.util.SPUtils.getInstance().remove("inventory_executing_key");
//                }
//            }
//        } catch (Exception ignored) {}
//    }
}
