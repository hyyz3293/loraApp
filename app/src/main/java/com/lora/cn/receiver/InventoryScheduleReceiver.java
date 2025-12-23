package com.lora.cn.receiver;

import android.content.Context;
import android.content.Intent;

public class InventoryScheduleReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean enabled = com.blankj.utilcode.util.SPUtils.getInstance().getBoolean("inventory_schedule_enabled", false);
        android.util.Log.i("InventoryScheduleReceiver", "onReceive enabled=" + enabled + " intent=" + (intent != null ? intent.getAction() : ""));
        if (!enabled) return;
        com.lora.cn.network.MqttPacketsClient client = com.lora.cn.network.MqttPacketsClient.getShared();
        int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
        int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
        scheduleNextDay(context.getApplicationContext(), h, m);
        if (!client.isConnected()) {
            android.util.Log.w("InventoryScheduleReceiver", "MQTT未连接，跳过本次定时下行，已计划下一次");
            return;
        }
        com.lora.cn.utils.DownlinkMessageHelper helper = new com.lora.cn.utils.DownlinkMessageHelper(client);
        try {
            com.lora.cn.database.DatabaseHelper db = com.lora.cn.database.DatabaseHelper.getInstance(context.getApplicationContext());
            java.util.List<com.lora.cn.ui.model.Terminal> terminals = db.getAllTerminals();
            int mins = Math.max(0, Math.min(1440, h * 60 + m));
            String alarmTs = String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m);
            for (com.lora.cn.ui.model.Terminal t : terminals) {
                String dev = t.getTerminalId() != null ? t.getTerminalId().trim().replace(" ", "") : "";
                if (dev.length() != 16) continue;
                int dep = (int) Math.max(0, Math.min(255, t.getDepartmentId()));
                int cart = (int) Math.max(0, Math.min(255, t.getRoomId()));
                android.util.Log.i("InventoryScheduleReceiver", "sendDownlink dev=" + dev + " dep=" + dep + " cart=" + cart + " alarm=" + alarmTs);
                helper.sendDownlink8001(dev, 1, 1, dep, cart, 0, 0, 60, 1, new int[]{mins}, true);
                try {
                    com.lora.cn.ui.model.LogInfo li = new com.lora.cn.ui.model.LogInfo();
                    li.setTerminalId(t.getTerminalId());
                    li.setTerminalName(t.getTerminalName());
                    li.setDeviceId(t.getTerminalId());
                    li.setStatusCode(com.lora.cn.ui.constants.LogStatus.TIMED_MAINTENANCE.code);
                    String ts = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                    li.setOperator(com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", ""));
                    li.setOperationTime(ts);
                    li.setCreateTime(ts);
                    li.setAction("终端清点: 定时维护(" + alarmTs + ")");
                    db.addLog(li);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private void scheduleNextDay(Context ctx, int hour, int minute) {
        try {
            android.app.AlarmManager am = (android.app.AlarmManager) ctx.getSystemService(android.content.Context.ALARM_SERVICE);
            if (am == null) return;
            android.content.Intent intent = new android.content.Intent("com.lora.cn.ACTION_INVENTORY_SCHEDULE");
            intent.setClass(ctx, com.lora.cn.receiver.InventoryScheduleReceiver.class);
            android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(ctx, 10001, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
            cal.set(java.util.Calendar.MINUTE, minute);
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
            long trigger = cal.getTimeInMillis();
            am.cancel(pi);
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, trigger, pi);
            } else {
                am.setExact(android.app.AlarmManager.RTC_WAKEUP, trigger, pi);
            }
            android.util.Log.i("InventoryScheduleReceiver", "已计划下一次定时清点: " + String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute));
        } catch (Exception ignored) {}
    }
}
