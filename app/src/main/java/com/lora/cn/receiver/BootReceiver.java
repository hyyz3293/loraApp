package com.lora.cn.receiver;

import android.content.Context;
import android.content.Intent;

public class BootReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!android.content.Intent.ACTION_BOOT_COMPLETED.equals(action)) return;
        boolean enabled = com.blankj.utilcode.util.SPUtils.getInstance().getBoolean("inventory_schedule_enabled", false);
        if (!enabled) return;
        int hour = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
        int minute = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
        android.app.AlarmManager am = (android.app.AlarmManager) context.getSystemService(android.content.Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent("com.lora.cn.ACTION_INVENTORY_SCHEDULE");
        i.setClass(context.getApplicationContext(), com.lora.cn.receiver.InventoryScheduleReceiver.class);
        android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(context.getApplicationContext(), 10001, i, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
        cal.set(java.util.Calendar.MINUTE, minute);
        long trigger = cal.getTimeInMillis();
        long now = System.currentTimeMillis();
        if (trigger <= now) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
            trigger = cal.getTimeInMillis();
        }
        am.cancel(pi);
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, trigger, pi);
        } else {
            am.setExact(android.app.AlarmManager.RTC_WAKEUP, trigger, pi);
        }
    }
}
