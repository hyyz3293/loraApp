package com.lora.cn.work;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class InventoryScheduleWorker extends Worker {
    public InventoryScheduleWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
        int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int day = cal.get(java.util.Calendar.DAY_OF_YEAR);
        String key = String.format(java.util.Locale.getDefault(), "%d-%02d:%02d", day, h, m);
        String last = com.blankj.utilcode.util.SPUtils.getInstance().getString("inventory_last_fire_key", "");
        boolean begun = com.lora.cn.LoraApp.tryBeginInventoryExecute(key);
        try {
            if (key.equals(last) || !begun) {
                scheduleFromPrefs(ctx);
                return Result.success();
            }
            com.blankj.utilcode.util.SPUtils.getInstance().put("inventory_last_fire_key", key);
        com.lora.cn.network.MqttPacketsClient client = com.lora.cn.network.MqttPacketsClient.getShared();
        try {
            com.blankj.utilcode.util.SPUtils sp = com.blankj.utilcode.util.SPUtils.getInstance();
            boolean localEnabled = sp.getBoolean("mqtt_local_broker_enabled", true);
            int localPort = sp.getInt("mqtt_local_broker_port", 1883);
            String brokerUrl = localEnabled ? ("tcp://127.0.0.1:" + (localPort > 0 ? localPort : 1883)) : sp.getString("mqtt_broker_url", "");
            String topicFilter = sp.getString("mqtt_topic_filter", "/milesight/uplink/#");
            String username = sp.getString("mqtt_username", "");
            String password = sp.getString("mqtt_password", "");
            boolean trustAll = sp.getBoolean("mqtt_trust_all_certs", false);
            if (TextUtils.isEmpty(brokerUrl)) {
                String gw = sp.getString("gateway_ip", "");
                if (!TextUtils.isEmpty(gw)) brokerUrl = "tcp://" + gw + ":1883";
            }
            if (!client.isConnected() && !TextUtils.isEmpty(brokerUrl)) {
                client.connectAndSubscribe(ctx, brokerUrl, "android-worker", topicFilter, username, password, trustAll,
                        new com.lora.cn.network.GatewayPacketsClient.PacketsListener() {
                            @Override public void onStatus(String msg) {}
                            @Override public void onPackets(java.util.List<com.lora.cn.network.GatewayPacketsClient.PacketRecord> records) {}
                            @Override public void onError(String error) {}
                            @Override public void onComplete() {}
                        });
            }
        } catch (Exception ignored) {}
        String alarmTs = String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m);
        try {
            com.lora.cn.database.DatabaseHelper db = com.lora.cn.database.DatabaseHelper.getInstance(ctx);
            java.util.List<com.lora.cn.ui.model.Terminal> terminals = db.getAllTerminals();
            int targetCount = 0;
            if (terminals != null) {
                for (com.lora.cn.ui.model.Terminal t : terminals) {
                    String dev0 = t.getTerminalId() != null ? t.getTerminalId().trim().replace(" ", "") : "";
                    if (dev0.length() == 16) targetCount++;
                }
            }
            android.app.NotificationManager nm = (android.app.NotificationManager) ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    android.app.NotificationChannel ch = new android.app.NotificationChannel("inventory_schedule", "定时清点", android.app.NotificationManager.IMPORTANCE_DEFAULT);
                    nm.createNotificationChannel(ch);
                }
                android.content.Intent open = new android.content.Intent(ctx, com.lora.cn.ui.activity.MainActivity.class);
                android.app.PendingIntent contentPi = android.app.PendingIntent.getActivity(ctx, 30001, open, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
                NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, "inventory_schedule")
                        .setSmallIcon(com.lora.cn.R.mipmap.app_logo)
                        .setContentTitle("定时清点已触发")
                        .setContentText("时间: " + alarmTs + "，终端: " + targetCount)
                        .setContentIntent(contentPi)
                        .setAutoCancel(true);
                nm.notify(30002, b.build());
            }
            int mins = Math.max(0, Math.min(1440, h * 60 + m));
            com.lora.cn.utils.DownlinkMessageHelper helper = new com.lora.cn.utils.DownlinkMessageHelper(client);
            if (terminals != null) {
                for (com.lora.cn.ui.model.Terminal t : terminals) {
                    String dev = t.getTerminalId() != null ? t.getTerminalId().trim().replace(" ", "") : "";
                    if (dev.length() != 16) continue;
                    int dep = (int) Math.max(0, Math.min(255, t.getDepartmentId()));
                    int cart = (int) Math.max(0, Math.min(255, t.getRoomId()));
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
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
        scheduleFromPrefs(ctx);
        return Result.success();
        } finally {
            if (begun) com.lora.cn.LoraApp.endInventoryExecute(key);
        }
    }

    public static void scheduleFromPrefs(Context ctx) {
        int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
        int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
        scheduleAt(ctx, h, m);
    }

    public static void scheduleAt(Context ctx, int hour, int minute) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        long now = System.currentTimeMillis();
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
        cal.set(java.util.Calendar.MINUTE, minute);
        long trigger = cal.getTimeInMillis();
        if (trigger <= now) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
            trigger = cal.getTimeInMillis();
        }
        long delay = Math.max(1000L, trigger - now);
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(InventoryScheduleWorker.class)
                .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(ctx).enqueueUniqueWork("inventory_schedule", ExistingWorkPolicy.REPLACE, req);
    }
}
