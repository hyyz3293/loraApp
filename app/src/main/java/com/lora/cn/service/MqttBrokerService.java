package com.lora.cn.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.app.Service;
import android.util.Log;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.R;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;

public class MqttBrokerService extends Service {

    private static final String CHANNEL_ID = "mqtt_broker";
    private static final int NOTIFICATION_ID = 1001;

    private Server broker;
    private int currentPort = -1;
    private ExecutorService brokerExecutor;
    private android.os.Handler tickHandler;
    private final java.util.concurrent.atomic.AtomicReference<String> lastFireKey = new java.util.concurrent.atomic.AtomicReference<>("");

    @Override
    public void onCreate() {
        super.onCreate();
        ensureNotificationChannel();
        brokerExecutor = Executors.newSingleThreadExecutor();
        tickHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int port = readPort(intent);
        String ipSummary = getIpSummary();
        startForeground(NOTIFICATION_ID, buildNotification(port, ipSummary));
        if (brokerExecutor != null) {
            brokerExecutor.execute(() -> startBrokerIfNeeded(port));
        } else {
            startBrokerIfNeeded(port);
        }
        //startTicking();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopBroker();
        if (brokerExecutor != null) {
            try {
                brokerExecutor.shutdownNow();
                brokerExecutor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int readPort(Intent intent) {
        int port = 1883;
        if (intent != null && intent.hasExtra("port")) {
            port = intent.getIntExtra("port", 1883);
        } else {
            port = SPUtils.getInstance().getInt("mqtt_local_broker_port", 1883);
        }
        return port <= 0 ? 1883 : port;
    }

    private void startBrokerIfNeeded(int port) {
        if (broker != null && currentPort == port) return;
        stopBroker();

        try {
            broker = new Server();
            Properties props = new Properties();
            props.setProperty("host", "0.0.0.0");
            props.setProperty("port", String.valueOf(port));
            // 允许匿名连接，使用应用内部可写持久化路径，避免默认MapDB路径不可写
            props.setProperty("allow_anonymous", "true");
            java.io.File storeFile = new java.io.File(getFilesDir(), "moquette_store.mapdb");
            props.setProperty("persistent_store", storeFile.getAbsolutePath());
            MemoryConfig config = new MemoryConfig(props);
            broker.startServer(config);
            currentPort = port;
            String ipSummary = getIpSummary();
            String primaryIp = "";
            if (ipSummary != null && !ipSummary.isEmpty()) {
                String[] arr = ipSummary.split(",");
                if (arr.length > 0) primaryIp = arr[0].trim();
            }
            Log.i(CHANNEL_ID, "MQTT Broker started. Port=" + port + ", IPs=" + ipSummary);
            com.blankj.utilcode.util.SPUtils.getInstance().put("mqtt_ip_summary", ipSummary == null ? "" : ipSummary);
            if (!primaryIp.isEmpty()) {
                com.blankj.utilcode.util.SPUtils.getInstance().put("mqtt_primary_ip", primaryIp);
            }
            // 标记就绪并广播
            com.blankj.utilcode.util.SPUtils.getInstance().put("mqtt_local_broker_ready", true);
            Intent ready = new Intent("com.lora.cn.MQTT_BROKER_READY");
            ready.putExtra("port", port);
            ready.putExtra("ips", ipSummary);
            sendBroadcast(ready);
            // 刷新通知文案，加入IP信息
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(NOTIFICATION_ID, buildNotification(port, ipSummary));
        } catch (Exception e) {
            // 启动失败时停止前台服务
            Log.e(CHANNEL_ID, "MQTT Broker start failed", e);
            stopSelf();
        }
    }

    private void stopBroker() {
        try {
            if (broker != null) {
                broker.stopServer();
            }
        } catch (Exception ignored) {
        } finally {
            broker = null;
            currentPort = -1;
            // 标记未就绪并广播
            com.blankj.utilcode.util.SPUtils.getInstance().put("mqtt_local_broker_ready", false);
            Intent stopped = new Intent("com.lora.cn.MQTT_BROKER_STOPPED");
            sendBroadcast(stopped);
        }
    }

    private void ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "MQTT Broker", NotificationManager.IMPORTANCE_LOW);
                nm.createNotificationChannel(ch);
                NotificationChannel ch2 = new NotificationChannel("inventory_schedule", "定时清点", NotificationManager.IMPORTANCE_DEFAULT);
                nm.createNotificationChannel(ch2);
            }
        }
    }

    private Notification buildNotification(int port, String ipSummary) {
        String content = "监听端口: " + port + (ipSummary.isEmpty() ? "" : (" | IP: " + ipSummary));
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MQTT服务端已启动")
                .setContentText(content)
                .setSmallIcon(R.mipmap.app_logo)
                .setOngoing(true);
        return b.build();
    }

    private String getIpSummary() {
        try {
            List<String> ips = new LinkedList<>();
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface nif : Collections.list(en)) {
                if (!nif.isUp() || nif.isLoopback()) continue;
                for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                    if (addr.isLoopbackAddress()) continue;
                    String host = addr.getHostAddress();
                    // 仅取IPv4
                    if (host != null && host.indexOf(':') < 0) {
                        ips.add(host);
                    }
                }
            }
            return String.join(",", ips);
        } catch (Exception ignored) {
            return "";
        }
    }

    private void startTicking() {
        if (tickHandler == null) return;
        tickHandler.removeCallbacksAndMessages(null);
        tickHandler.post(tickRunnable);
    }

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                //LogUtils.e("---");
                boolean enabled = com.blankj.utilcode.util.SPUtils.getInstance().getBoolean("inventory_schedule_enabled", true);
                if (enabled) {
                    int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
                    int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    int ch = cal.get(java.util.Calendar.HOUR_OF_DAY);
                    int cm = cal.get(java.util.Calendar.MINUTE);
                    int day = cal.get(java.util.Calendar.DAY_OF_YEAR);
                    String key = String.format(java.util.Locale.getDefault(), "%d-%02d:%02d", day, h, m);
                    String currKey = String.format(java.util.Locale.getDefault(), "%d-%02d:%02d", day, ch, cm);
                    String last = com.blankj.utilcode.util.SPUtils.getInstance().getString("inventory_last_fire_key", "");
                    //LogUtils.e("--- ch=" + ch+ "--cm=" + cm + "-----h=" + h + "----" + m + "____" + !key.equals(last)) ;
                    if (h == ch && m == cm && !key.equals(last)) {
                        lastFireKey.set(key);
                        com.blankj.utilcode.util.SPUtils.getInstance().put("inventory_last_fire_key", key);
                        performInventoryDownlink();
                    } else if (!currKey.equals(last) && !key.equals(last) && (h != ch || m != cm)) {
                    }
                }
            } catch (Exception ignored) {}
            if (tickHandler != null) tickHandler.postDelayed(this, 1000);
        }
    };

    private void performInventoryDownlink() {
        int h0 = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
        int m0 = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
        java.util.Calendar cal0 = java.util.Calendar.getInstance();
        int day0 = cal0.get(java.util.Calendar.DAY_OF_YEAR);
        String key0 = String.format(java.util.Locale.getDefault(), "%d-%02d:%02d", day0, h0, m0);
        String last0 = com.blankj.utilcode.util.SPUtils.getInstance().getString("inventory_last_fire_key", "");
        if (key0.equals(last0)) return;
        lastFireKey.set(key0);
        com.blankj.utilcode.util.SPUtils.getInstance().put("inventory_last_fire_key", key0);
        Context ctx = getApplicationContext();
        int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
        int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
        String alarmTs = String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m);
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                Intent open = new Intent(ctx, com.lora.cn.ui.activity.MainActivity.class);
                android.app.PendingIntent contentPi = android.app.PendingIntent.getActivity(ctx, 30001, open, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
                NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, "inventory_schedule")
                        .setSmallIcon(com.lora.cn.R.mipmap.app_logo)
                        .setContentTitle("定时清点已触发")
                        .setContentText("时间: " + alarmTs)
                        .setContentIntent(contentPi)
                        .setAutoCancel(true);
                nm.notify(30002, b.build());
            }
        } catch (Exception ignored) {}
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
            if (!client.isConnected() && brokerUrl != null && !brokerUrl.isEmpty()) {
                client.connectAndSubscribe(ctx, brokerUrl, "android-ticker", topicFilter, username, password, trustAll,
                        new com.lora.cn.network.GatewayPacketsClient.PacketsListener() {
                            @Override public void onStatus(String msg) {}
                            @Override public void onPackets(java.util.List<com.lora.cn.network.GatewayPacketsClient.PacketRecord> records) {}
                            @Override public void onError(String error) {}
                            @Override public void onComplete() {}
                        });
            }
        } catch (Exception ignored) {}
        try {
            com.lora.cn.database.DatabaseHelper db = com.lora.cn.database.DatabaseHelper.getInstance(ctx);
            java.util.List<com.lora.cn.ui.model.Terminal> terminals = db.getAllTerminals();
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
                        //db.addLog(li);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }
}
