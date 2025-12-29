package com.lora.cn.receiver;

import android.content.Context;
import android.content.Intent;

import com.blankj.utilcode.util.LogUtils;

public class InventoryScheduleReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
//        boolean enabled = com.blankj.utilcode.util.SPUtils.getInstance().getBoolean("inventory_schedule_enabled", true);
//        LogUtils.e("InventoryScheduleReceiver", "onReceive enabled=" + enabled + " intent=" + (intent != null ? intent.getAction() : ""));
//        if (!enabled) return;
        com.lora.cn.network.MqttPacketsClient client = com.lora.cn.network.MqttPacketsClient.getShared();
        int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
        int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int day = cal.get(java.util.Calendar.DAY_OF_YEAR);
        String key = String.format(java.util.Locale.getDefault(), "%d-%02d:%02d", day, h, m);
        boolean begun = com.lora.cn.LoraApp.tryBeginInventoryExecute(key);
        try {
            String last = com.blankj.utilcode.util.SPUtils.getInstance().getString("inventory_last_fire_key", "");
            if (key.equals(last) || !begun) return;
            com.blankj.utilcode.util.SPUtils.getInstance().put("inventory_last_fire_key", key);
            scheduleNextDay(context.getApplicationContext(), h, m);
//        try {
//            if (client != null && !client.isConnected()) {
//                com.blankj.utilcode.util.SPUtils sp = com.blankj.utilcode.util.SPUtils.getInstance();
//                boolean localEnabled = sp.getBoolean("mqtt_local_broker_enabled", true);
//                int localPort = sp.getInt("mqtt_local_broker_port", 1883);
//                String brokerUrl = localEnabled ? ("tcp://127.0.0.1:" + (localPort > 0 ? localPort : 1883)) : sp.getString("mqtt_broker_url", "");
//                String topicFilter = sp.getString("mqtt_topic_filter", "/milesight/uplink/#");
//                String username = sp.getString("mqtt_username", "");
//                String password = sp.getString("mqtt_password", "");
//                boolean trustAll = sp.getBoolean("mqtt_trust_all_certs", false);
//                client.connectAndSubscribe(context.getApplicationContext(), brokerUrl, "android-schedule", topicFilter, username, password, trustAll,
//                        new com.lora.cn.network.GatewayPacketsClient.PacketsListener() {
//                            @Override public void onStatus(String msg) { android.util.Log.i("InventoryScheduleReceiver", "MQTT状态: " + msg); }
//                            @Override public void onPackets(java.util.List<com.lora.cn.network.GatewayPacketsClient.PacketRecord> records) { }
//                            @Override public void onError(String error) { android.util.Log.e("InventoryScheduleReceiver", "MQTT错误: " + error); }
//                            @Override public void onComplete() { android.util.Log.i("InventoryScheduleReceiver", "MQTT完成/断开"); }
//                        });
//            }
//        } catch (Exception ignored) {}
        com.lora.cn.utils.DownlinkMessageHelper helper = new com.lora.cn.utils.DownlinkMessageHelper(client);
        com.lora.cn.database.DatabaseHelper db = com.lora.cn.database.DatabaseHelper.getInstance(context.getApplicationContext());
            java.util.List<com.lora.cn.ui.model.Terminal> terminals = db.getAllTerminals();
            int mins = Math.max(0, Math.min(1440, h * 60 + m));
            String alarmTs = String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m);
            int targetCount = 0;
            if (terminals != null) {
                for (com.lora.cn.ui.model.Terminal t : terminals) {
                    String dev0 = t.getTerminalId() != null ? t.getTerminalId().trim().replace(" ", "") : "";
                    if (dev0.length() == 16) targetCount++;
                }
            }
            android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    android.app.NotificationChannel ch = new android.app.NotificationChannel("inventory_schedule", "定时清点", android.app.NotificationManager.IMPORTANCE_DEFAULT);
                    nm.createNotificationChannel(ch);
                }
                android.content.Intent open = new android.content.Intent(context, com.lora.cn.ui.activity.MainActivity.class);
                android.app.PendingIntent contentPi = android.app.PendingIntent.getActivity(context, 30001, open, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
                androidx.core.app.NotificationCompat.Builder b = new androidx.core.app.NotificationCompat.Builder(context, "inventory_schedule")
                        .setSmallIcon(com.lora.cn.R.mipmap.app_logo)
                        .setContentTitle("定时清点已触发")
                        .setContentText("时间: " + alarmTs + "，终端: " + targetCount)
                        .setContentIntent(contentPi)
                        .setAutoCancel(true);
                nm.notify(30002, b.build());
            }
            for (com.lora.cn.ui.model.Terminal t : terminals) {
                String dev = t.getTerminalId() != null ? t.getTerminalId().trim().replace(" ", "") : "";
                if (dev.length() != 16) continue;
                int dep = (int) Math.max(0, Math.min(255, t.getDepartmentId()));
                int cart = (int) Math.max(0, Math.min(255, t.getRoomId()));
                LogUtils.e("InventoryScheduleReceiver", "sendDownlink dev=" + dev + " dep=" + dep + " cart=" + cart + " alarm=" + alarmTs);
                helper.sendDownlink8001(
                        dev,
                        1,
                        0,
                        dep,
                        cart,
                        0,
                        0,
                        com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3),
                        1,
                        new int[]{mins},
                        true);
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
                } catch (Exception ignored) {
                    LogUtils.e("InventoryScheduleReceiver", "ignored dev=" + ignored);

                }
            }
        } catch (Exception ignored) {
            LogUtils.e("InventoryScheduleReceiver", "ignored y  xxx=" + ignored);
        } finally {
            if (begun) com.lora.cn.LoraApp.endInventoryExecute(key);
        }
    }

    private void scheduleNextDay(Context ctx, int hour, int minute) {
        try {
            android.app.AlarmManager am = (android.app.AlarmManager) ctx.getSystemService(android.content.Context.ALARM_SERVICE);
            LogUtils.e("scheduleNextDay", "scheduleNextDay dev=" );
            if (am == null) return;
            android.content.Intent intent = new android.content.Intent("com.lora.cn.ACTION_INVENTORY_SCHEDULE");
            intent.setClass(ctx, com.lora.cn.receiver.InventoryScheduleReceiver.class);
            android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(ctx, 10001, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
            android.content.Intent showIntent = new android.content.Intent(ctx, com.lora.cn.ui.activity.MainActivity.class);
            android.app.PendingIntent showPi = android.app.PendingIntent.getActivity(ctx, 20001, showIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
            cal.set(java.util.Calendar.MINUTE, minute);
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
            long trigger = cal.getTimeInMillis();
            am.cancel(pi);
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                android.app.AlarmManager.AlarmClockInfo info = new android.app.AlarmManager.AlarmClockInfo(trigger, showPi);
                am.setAlarmClock(info, pi);
            } else {
                am.setExact(android.app.AlarmManager.RTC_WAKEUP, trigger, pi);
            }
            LogUtils.e("InventoryScheduleReceiver", "已计划下一次定时清点: " + String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute));
        } catch (Exception ignored) {}
    }
}
