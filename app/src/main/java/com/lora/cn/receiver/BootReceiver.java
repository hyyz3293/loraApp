//package com.lora.cn.receiver;
//
//import android.content.Context;
//import android.content.Intent;
//
//public class BootReceiver extends android.content.BroadcastReceiver {
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        if (intent == null) return;
//        String action = intent.getAction();
//        if (!android.content.Intent.ACTION_BOOT_COMPLETED.equals(action)) return;
//        boolean enabled = com.blankj.utilcode.util.SPUtils.getInstance().getBoolean("inventory_schedule_enabled", true);
//        if (!enabled) return;
//        int hour = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
//        int minute = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
//        com.lora.cn.work.InventoryScheduleWorker.scheduleAt(context.getApplicationContext(), hour, minute);
//    }
//}
