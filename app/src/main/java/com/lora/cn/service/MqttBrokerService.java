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
    private android.os.Handler maintenanceEvaluateHandler;
    private com.lora.cn.network.MqttPacketsClient mqttClient;
    private android.os.Handler mainHandler;
    private final java.util.concurrent.atomic.AtomicBoolean mqttConnectInFlight = new java.util.concurrent.atomic.AtomicBoolean(false);
    private int mqttConnectRetry = 0;
    private java.util.concurrent.ExecutorService ioExecutor;
    private android.net.ConnectivityManager connectivityManager;
    private android.net.ConnectivityManager.NetworkCallback networkCallback;
    private final java.util.concurrent.ConcurrentHashMap<String, SleepCycleState> sleepCycleStateByDev = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Long> lastUplinkStoreByDevMs = new java.util.concurrent.ConcurrentHashMap<>();
    private static final String ACTION_MQTT_CLIENT_STATE = "com.lora.cn.MQTT_CLIENT_STATE";
    private final Runnable maintenanceEvaluateRunnable = new Runnable() {
        @Override public void run() {
            try {
                long uid = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault());
                android.content.Context appCtx = getApplicationContext();
                Runnable task = () -> {
                    try {
                        com.lora.cn.database.DatabaseHelper db = com.lora.cn.database.DatabaseHelper.getInstance(appCtx);
                        java.util.List<com.lora.cn.ui.model.MaintenanceInfo> list = db.getMaintenanceRecords(uid);
                        if (list != null) {
                            long now = System.currentTimeMillis();
                            for (com.lora.cn.ui.model.MaintenanceInfo mi : list) {
                                if (mi == null) continue;
                                if (mi.getStatus() != 0) continue;
                                String ct = mi.getCreateTime();
                                if (ct == null || ct.trim().isEmpty()) continue;
                                long ts = now;
                                try { java.util.Date dt = sdf.parse(ct.trim()); if (dt != null) ts = dt.getTime(); } catch (Exception ignored) {}
                                if (ts > now) continue;
                                if (mi.getSentFlag() == 1) continue;
                                String dev = mi.getTerminalId();
                                if (dev == null || dev.isEmpty()) continue;
                            }
                        }
                    } catch (Exception ignored) {}
                };
                if (brokerExecutor != null) {
                    brokerExecutor.execute(task);
                } else {
                    task.run();
                }
            } finally {
                if (maintenanceEvaluateHandler != null) maintenanceEvaluateHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        ensureNotificationChannel();
        brokerExecutor = Executors.newSingleThreadExecutor();
        tickHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        maintenanceEvaluateHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        ioExecutor = Executors.newSingleThreadExecutor();
        registerNetworkMonitor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int port = readPort(intent);
        String ipSummary = getIpSummary();
        startForeground(NOTIFICATION_ID, buildNotification(port, ipSummary));
        boolean localEnabled = com.blankj.utilcode.util.SPUtils.getInstance().getBoolean("mqtt_local_broker_enabled", true);
        if (localEnabled) {
            if (brokerExecutor != null) {
                brokerExecutor.execute(() -> startBrokerIfNeeded(port));
            } else {
                startBrokerIfNeeded(port);
            }
        } else {
            stopBroker();
        }
        if (maintenanceEvaluateHandler != null) {
            maintenanceEvaluateHandler.removeCallbacks(maintenanceEvaluateRunnable);
            maintenanceEvaluateHandler.postDelayed(maintenanceEvaluateRunnable, 1000);
        }
        startUplinkSubscription();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterNetworkMonitor();
        stopBroker();
        if (brokerExecutor != null) {
            try {
                brokerExecutor.shutdownNow();
                brokerExecutor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
        try {
            if (maintenanceEvaluateHandler != null) {
                maintenanceEvaluateHandler.removeCallbacks(maintenanceEvaluateRunnable);
                maintenanceEvaluateHandler = null;
            }
        } catch (Exception ignored) {}
        try { broadcastClientState("stopped"); } catch (Exception ignored) {}
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

    private static class PendingUplink {
        String devEui;
        String devAddr;
        String hex;
        String dr;
        String time;
        String freq;
        String rssi;
        String snr;
        String fport;
        String fcnt;
        PendingUplink(String devEui, String devAddr, String hex, String dr, String time, String freq, String rssi, String snr, String fport, String fcnt) {
            this.devEui = devEui;
            this.devAddr = devAddr;
            this.hex = hex;
            this.dr = dr;
            this.time = time;
            this.freq = freq;
            this.rssi = rssi;
            this.snr = snr;
            this.fport = fport;
            this.fcnt = fcnt;
        }
    }

    private static class SleepCycleState {
        long cycleStartMs;
        boolean firstDropped;
        SleepCycleState(long cycleStartMs, boolean firstDropped) {
            this.cycleStartMs = cycleStartMs;
            this.firstDropped = firstDropped;
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
//                    helper.sendDownlink8001(
//                            dev,
//                            1,
//                            0,
//                            dep,
//                            cart,
//                            0,
//                            0,
//                            com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3),
//                            1,
//                            new int[]{mins},
//                            true);
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

    private boolean isLocalPortOpen(int port) {
        try {
            java.net.Socket s = new java.net.Socket();
            s.connect(new java.net.InetSocketAddress("127.0.0.1", port > 0 ? port : 1883), 200);
            try { s.close(); } catch (Exception ignored) {}
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void startUplinkSubscription() {
        try {
            if (!mqttConnectInFlight.compareAndSet(false, true)) {
                return;
            }
            if (!isNetworkAvailable()) {
                mqttConnectInFlight.set(false);
                broadcastClientState("disconnected");
                return;
            }
            try { broadcastClientState("connecting"); } catch (Exception ignored) {}
            if (mqttClient == null) mqttClient = com.lora.cn.network.MqttPacketsClient.getShared();
            com.blankj.utilcode.util.SPUtils sp = com.blankj.utilcode.util.SPUtils.getInstance();
            boolean localEnabled = sp.getBoolean("mqtt_local_broker_enabled", true);
            int localPort = sp.getInt("mqtt_local_broker_port", 1883);
            String brokerUrl = localEnabled ? ("tcp://127.0.0.1:" + (localPort > 0 ? localPort : 1883)) : sp.getString("mqtt_broker_url", "");
            boolean readyFlag = sp.getBoolean("mqtt_local_broker_ready", false);
            if (localEnabled && !readyFlag && !isLocalPortOpen(localPort)) {
                if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                int delay = Math.min(10000, 1000 * Math.max(1, ++mqttConnectRetry));
                mqttConnectInFlight.set(false);
                mainHandler.postDelayed(this::startUplinkSubscription, delay);
                return;
            }
            String cachedId = sp.getString("mqtt_client_id_service", "");
            if (cachedId == null || cachedId.trim().isEmpty()) {
                String gen = "android-service-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                sp.put("mqtt_client_id_service", gen);
                cachedId = gen;
            }
            final String clientId = cachedId;
            String topicFilter = sp.getString("mqtt_topic_filter", "/milesight/uplink/#");
            String username = sp.getString("mqtt_username", "");
            String password = sp.getString("mqtt_password", "");
            boolean trustAll = sp.getBoolean("mqtt_trust_all_certs", false);
            broadcastClientState("connecting");
            mqttClient.connectAndSubscribe(getApplicationContext(), brokerUrl, clientId, topicFilter,
                    username, password, trustAll,
                    new com.lora.cn.network.GatewayPacketsClient.PacketsListener() {
                        @Override
                        public void onStatus(String msg) {
                            try {
                                if (msg != null && (msg.contains("连接成功") || msg.contains("订阅成功"))) {
                                    mqttConnectRetry = 0;
                                    mqttConnectInFlight.set(false);
                                    broadcastClientState("connected");
                                }
                            } catch (Exception ignored) {}
                        }
                        @Override
                        public void onPackets(java.util.List<com.lora.cn.network.GatewayPacketsClient.PacketRecord> records) {
                            try {
                                int intervalMin = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
                                long intervalMs = Math.max(1L, Math.min(1440L, (long) intervalMin)) * 60_000L;
                                String currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                                if (records != null) {
                                    for (com.lora.cn.network.GatewayPacketsClient.PacketRecord r : records) {
                                        handleUplinkBySleepCycleService(r, currentTime, intervalMs);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                        @Override
                        public void onError(String error) {
                            try {
                                mqttConnectInFlight.set(false);
                                if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                                int delay = Math.min(10000, 1000 * Math.max(1, ++mqttConnectRetry));
                                if (mqttClient != null) {
                                    mqttClient.disconnect();
                                }
                                broadcastClientState("error");
                                mainHandler.postDelayed(MqttBrokerService.this::startUplinkSubscription, delay);
                            } catch (Exception ignored) {}
                        }
                        @Override
                        public void onComplete() {
                            try {
                                mqttConnectInFlight.set(false);
                                if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                                int delay = Math.min(10000, 1000 * Math.max(1, ++mqttConnectRetry));
                                broadcastClientState("disconnected");
                                mainHandler.postDelayed(MqttBrokerService.this::startUplinkSubscription, delay);
                            } catch (Exception ignored) {}
                        }
                    });
        } catch (Exception e) {
            try {
                mqttConnectInFlight.set(false);
                if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                int delay = Math.min(10000, 1000 * Math.max(1, ++mqttConnectRetry));
                broadcastClientState("error");
                mainHandler.postDelayed(this::startUplinkSubscription, delay);
            } catch (Exception ignored) {}
        }
    }

    private boolean isNetworkAvailable() {
        try {
            if (connectivityManager == null) {
                connectivityManager = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            }
            if (connectivityManager == null) return false;
            android.net.Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;
            android.net.NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
            return caps != null && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void registerNetworkMonitor() {
        try {
            connectivityManager = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null || networkCallback != null) return;
            networkCallback = new android.net.ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(android.net.Network network) {
                    try {
                        if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mqttConnectRetry = 0;
                        mqttConnectInFlight.set(false);
                        broadcastClientState("connecting");
                        mainHandler.postDelayed(MqttBrokerService.this::startUplinkSubscription, 800);
                    } catch (Exception ignored) {}
                }

                @Override
                public void onLost(android.net.Network network) {
                    try {
                        mqttConnectInFlight.set(false);
                        if (mqttClient != null) mqttClient.disconnect();
                        broadcastClientState("disconnected");
                    } catch (Exception ignored) {}
                }

                @Override
                public void onUnavailable() {
                    try {
                        mqttConnectInFlight.set(false);
                        broadcastClientState("disconnected");
                    } catch (Exception ignored) {}
                }
            };
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        } catch (Exception e) {
            Log.w("MqttBrokerService", "registerNetworkMonitor warn: " + e.getMessage());
        }
    }

    private void unregisterNetworkMonitor() {
        try {
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception ignored) {
        } finally {
            networkCallback = null;
        }
    }

    private String normalizeDeviceKey(String devEui, String hex) {
        String k = devEui != null ? devEui.trim() : "";
        if (!k.isEmpty() && !"-".equals(k) && !" -".equals(k)) return k;
        try {
            com.lora.cn.utils.LoRaFrameParser.ParsedFrame f = com.lora.cn.utils.LoRaFrameParser.parseFrame(hex);
            String did = f != null ? f.deviceId : null;
            if (did != null && !did.trim().isEmpty()) return did.trim();
        } catch (Exception ignored) {}
        return k.isEmpty() ? (hex == null ? "" : hex) : k;
    }

    private void broadcastClientState(String state) {
        try {
            android.content.Intent i = new android.content.Intent(ACTION_MQTT_CLIENT_STATE);
            i.putExtra("state", state);
            sendBroadcast(i);
            try {
                com.blankj.utilcode.util.SPUtils sp = com.blankj.utilcode.util.SPUtils.getInstance();
                sp.put("mqtt_client_state", state == null ? "" : state);
                boolean ok = "connected".equalsIgnoreCase(state);
                sp.put("mqtt_connected", ok);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    public boolean setTabletTimeFromString(String time) {
        long ms = parseTimeMillisFlexible(time);
        return setTabletTimeMillis(ms);
    }

    private long parseTimeMillisFlexible(String s) {
        if (s == null) return -1L;
        String raw = s.trim();
        if (raw.isEmpty()) return -1L;
        try {
            if (raw.matches("^\\d{13}$")) return Long.parseLong(raw);
            if (raw.matches("^\\d{10}$")) return Long.parseLong(raw) * 1000L;
        } catch (Exception ignored) {}
        java.util.List<java.text.SimpleDateFormat> fs = new java.util.ArrayList<>();
        fs.add(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()));
        fs.add(new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()));
        fs.add(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()));
        for (java.text.SimpleDateFormat f : fs) {
            try {
                java.util.Date d = f.parse(raw);
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
        }
        return -1L;
    }

    public boolean setTabletTimeMillis(long millis) {
        if (millis <= 0L) return false;
        boolean ok = false;
        try { ok = android.os.SystemClock.setCurrentTimeMillis(millis); } catch (Throwable ignored) { ok = false; }
        if (ok) return true;
        java.text.SimpleDateFormat f1 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        java.text.SimpleDateFormat f2 = new java.text.SimpleDateFormat("yyyyMMdd.HHmmss", java.util.Locale.getDefault());
        String ts1 = f1.format(new java.util.Date(millis));
        String ts2 = f2.format(new java.util.Date(millis));
        String[] cmds = new String[]{
                "date -s " + ts1,
                "toybox date -s " + ts1,
                "busybox date -s " + ts1,
                "date " + ts2,
                "toybox date " + ts2,
                "busybox date " + ts2
        };
        for (String c : cmds) {
            try {
                Process p = new ProcessBuilder("su", "-c", c).redirectErrorStream(true).start();
                int code = p.waitFor();
                if (code == 0) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private void handleUplinkBySleepCycleService(com.lora.cn.network.GatewayPacketsClient.PacketRecord r, String currentTime, long intervalMs) {
        if (r == null) return;
        String devEui = r.deviceId != null ? r.deviceId : "-";
        String devAddr = r.devAddr != null ? r.devAddr : "-";
        String hex = r.payloadHex != null ? r.payloadHex : "-";
        String dr = r.dr != null ? r.dr : "-";
        String time = r.time != null ? r.time : currentTime;
        try { setTabletTimeFromString(time); } catch (Exception ignored) {}
        String freq = r.freq != null ? String.valueOf(r.freq) : "-";
        String rssi = r.rssi != null ? String.valueOf(r.rssi) : "-";
        String snr = r.snr != null ? String.valueOf(r.snr) : "-";
        String fport = r.fport != null ? String.valueOf(r.fport) : "-";
        String fcnt = r.fcnt != null ? String.valueOf(r.fcnt) : "-";
        PendingUplink p = new PendingUplink(devEui, devAddr, hex, dr, time, freq, rssi, snr, fport, fcnt);
        String key = normalizeDeviceKey(devEui, hex);
        long nowMs = System.currentTimeMillis();
        SleepCycleState st = sleepCycleStateByDev.get(key);
        boolean newCycle = st == null || nowMs - st.cycleStartMs >= Math.max(1000L, intervalMs);
        if (newCycle) {
            st = new SleepCycleState(nowMs, false);
            sleepCycleStateByDev.put(key, st);
        }
        if (!st.firstDropped) {
            st.firstDropped = true;
            if (!"-".equals(hex) && hex != null && hex.length() > 0) {
                try {
                    if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
                    final String t = time;
                    final String h = hex;
                    ioExecutor.execute(() -> {
                        try { onUplinkDataEventService(t, h); } catch (Exception ignored) {}
                    });
                } catch (Exception ignored) {}
            }
            return;
        }
        processUplinkPacketService(p);
    }

    private void processUplinkPacketService(PendingUplink p) {
        if (p == null) return;
        String devEui = p.devEui;
        String hex = p.hex;
        String time = p.time;
        if (!"-".equals(hex)) {
            long nowMs = System.currentTimeMillis();
            boolean allowStore = true;
            if (devEui != null && !"".equals(devEui) && !" -".equals(devEui)) {
                Long last = lastUplinkStoreByDevMs.get(devEui);
                if (last != null && nowMs - last < 2000) {
                    allowStore = false;
                }
            }
            if (allowStore) {
                if (devEui != null && !"".equals(devEui) && !" -".equals(devEui)) {
                    lastUplinkStoreByDevMs.put(devEui, nowMs);
                }
                if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
                final String broadcastTime = time;
                final String broadcastHex = hex;
                final String devEuiForLog = devEui;
                ioExecutor.execute(() -> {
                    long result = -1L;
                    try { result = com.lora.cn.database.DatabaseHelper.getInstance(getApplicationContext()).addUplinkLog(broadcastHex, broadcastTime); } catch (Exception ignored) {}
                    try {
                        org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.events.UplinkDataEvent(broadcastTime, broadcastHex));
                    } catch (Exception ignored) {}
                });
            }
        }
    }

    private void onUplinkDataEventService(String time, String hex) {
        try {
            com.lora.cn.database.DatabaseHelper db = com.lora.cn.database.DatabaseHelper.getInstance(getApplicationContext());
            com.lora.cn.utils.LoRaFrameParser.ParsedFrame frame = com.lora.cn.utils.LoRaFrameParser.parseFrame(hex);
            if (frame == null) return;
            if (frame.deviceId == null || frame.deviceId.isEmpty()) return;
            try {
                if (!db.isTerminalExists(frame.deviceId)) return;
            } catch (Exception ignored) { return; }
            try {
                int yy = frame.termVerYY;
                int mm = frame.termVerMM;
                int dd = frame.termVerDD;
                int mv = frame.loraModuleVersionCode;
                String fw = com.lora.cn.utils.LoRaFrameParser.buildFirmwareVersionString(yy, mm, dd, mv);
                if (fw != null && !fw.trim().isEmpty()) {
                    com.blankj.utilcode.util.SPUtils.getInstance().put("terminal_firmware_version", fw);
                }
            } catch (Exception ignored) {}
            int latestTimedUnsentMins = -1;
            java.util.List<com.lora.cn.ui.model.MaintenanceInfo> allMForDevice = null;
            java.util.ArrayList<com.lora.cn.ui.model.MaintenanceInfo> dueMaint = new java.util.ArrayList<>();
            try {
                allMForDevice = db.getMaintenanceRecordsByTerminal(frame.deviceId, 0);
                java.util.List<com.lora.cn.ui.model.MaintenanceInfo> allM = allMForDevice;
                if (allM != null) {
                    long now = System.currentTimeMillis();
                    java.text.SimpleDateFormat sdf1 = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault());
                    java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                    java.text.SimpleDateFormat sdf3 = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault());
                    java.text.SimpleDateFormat sdf4 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
                    int latestUnsentMins = -1;
                    long latestUnsentTs = -1L;
                    for (com.lora.cn.ui.model.MaintenanceInfo mi : allM) {
                        if (mi == null) continue;
                        if (mi.getStatus() != 0) continue;
                        String c = mi.getContent();
                        if ("主动维护".equals(c)) continue;
                        if (mi.getSentFlag() == 1 || !com.blankj.utilcode.util.StringUtils.isEmpty(mi.getSentTime())) continue;
                        String ct = mi.getCreateTime();
                        if (ct == null || ct.trim().isEmpty()) continue;
                        try {
                            java.util.Date dt = null;
                            try { dt = sdf1.parse(ct.trim()); } catch (Exception ignored) {}
                            if (dt == null) { try { dt = sdf2.parse(ct.trim()); } catch (Exception ignored) {} }
                            if (dt == null) { try { dt = sdf3.parse(ct.trim()); } catch (Exception ignored) {} }
                            if (dt == null) { try { dt = sdf4.parse(ct.trim()); } catch (Exception ignored) {} }
                            if (dt == null) continue;
                            long ts = dt.getTime();
                            if (ts > now) continue;
                            java.util.Calendar cal = java.util.Calendar.getInstance();
                            cal.setTime(dt);
                            int mins = Math.max(0, Math.min(1440, cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)));
                            if (ts >= latestUnsentTs) {
                                latestUnsentTs = ts;
                                latestUnsentMins = mins;
                            }
                            dueMaint.add(mi);
                        } catch (Exception ignored) {}
                    }
                    if (latestUnsentMins >= 0) latestTimedUnsentMins = latestUnsentMins;
                }
            } catch (Exception ignored) {}
            long nowDedup = System.currentTimeMillis();
            com.blankj.utilcode.util.SPUtils sp = com.blankj.utilcode.util.SPUtils.getInstance();
            com.lora.cn.network.MqttPacketsClient client = com.lora.cn.network.MqttPacketsClient.getShared();
            com.lora.cn.utils.DownlinkMessageHelper helper = new com.lora.cn.utils.DownlinkMessageHelper(client);
            java.util.List<com.lora.cn.ui.model.Terminal> terms = db.getAllTerminals();
            int depId = 0;
            int cartId = 0;
            boolean clearActivePending = false;
            boolean maintenanceTouched = false;
            try {
                if (terms != null) {
                    for (com.lora.cn.ui.model.Terminal t : terms) {
                        if (t != null && frame.deviceId.equalsIgnoreCase(t.getTerminalId())) {
                            depId = (int) Math.max(0, Math.min(255, t.getDepartmentId()));
                            cartId = (int) Math.max(0, Math.min(255, t.getRoomId()));
                            clearActivePending = t.isMaintenanceClearPending();
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}
            boolean maintenanceNeeded = false;
            boolean timedMaintenanceNeeded = false;
            try {
                if (frame.statusFlags != null) {
                    maintenanceNeeded = frame.statusFlags.contains(com.lora.cn.utils.LoRaFrameParser.DeviceStatusFlag.MAINTENANCE_NEEDED);
                    timedMaintenanceNeeded = frame.statusFlags.contains(com.lora.cn.utils.LoRaFrameParser.DeviceStatusFlag.TIMED_MAINTENANCE_NEEDED);
                }
            } catch (Exception ignored) {}
            java.util.List<com.lora.cn.ui.model.MaintenanceInfo> existingAll = allMForDevice != null ? allMForDevice : db.getMaintenanceRecordsByTerminal(frame.deviceId, 0);
            if (maintenanceNeeded) {
                try {
                    try { db.updateTerminalMaintenanceState(frame.deviceId, true, System.currentTimeMillis()); } catch (Exception ignored) {}
                    boolean existsPendingAuto = false;
                    if (existingAll != null) {
                        for (com.lora.cn.ui.model.MaintenanceInfo x : existingAll) {
                            String c = x != null ? x.getContent() : null;
                            String hu = x != null ? x.getHandleUser() : null;
                            String ht = x != null ? x.getHandleTime() : null;
                            boolean unhandled = (hu == null || hu.trim().isEmpty()) && (ht == null || ht.trim().isEmpty());
                            if ("主动维护".equals(c) && unhandled) {
                                existsPendingAuto = true;
                                break;
                            }
                        }
                    }
                    if (!existsPendingAuto) {
                        String name = "";
                        String groups = "";
                        if (terms != null) {
                            for (com.lora.cn.ui.model.Terminal t : terms) {
                                if (t != null && frame.deviceId.equalsIgnoreCase(t.getTerminalId())) {
                                    name = t.getTerminalName();
                                    groups = t.getGroupNamesText();
                                    break;
                                }
                            }
                        }
                        com.lora.cn.ui.model.MaintenanceInfo mi = new com.lora.cn.ui.model.MaintenanceInfo();
                        mi.setTerminalId(frame.deviceId);
                        mi.setTerminalName(name == null ? "" : name);
                        mi.setTerminalGroup(groups == null ? "" : groups);
                        mi.setStatus(0);
                        mi.setContent("主动维护");
                        long uidEff = com.lora.cn.database.DatabaseHelper.getInstance(getApplicationContext()).resolveEffectiveUserIdForAuto();
                        String unameEff = com.lora.cn.database.DatabaseHelper.getInstance(getApplicationContext()).resolveEffectiveUserNameForAuto();
                        mi.setCreateUserId(uidEff > 0 ? uidEff : 0L);
                        mi.setCreateUser(unameEff == null ? "" : unameEff);
                        mi.setCreateTime(new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                        try { db.addMaintenanceRecord(mi); } catch (Exception ignored2) {}
                        maintenanceTouched = true;
                    }
                } catch (Exception ignored) {}
            } else {
                try {
                    try { db.updateTerminalMaintenanceState(frame.deviceId, false, System.currentTimeMillis()); } catch (Exception ignored) {}
                    if (existingAll != null) {
                        String autoUser = com.lora.cn.database.DatabaseHelper.getInstance(getApplicationContext()).resolveEffectiveUserNameForAuto();
                        String autoTime = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                        String autoRemark = "主动维护清除：自动标记已维护";
                        for (com.lora.cn.ui.model.MaintenanceInfo x : existingAll) {
                            if (x == null) continue;
                            if (x.getStatus() != 0) continue;
                            String c = x.getContent();
                            if (!(("主动维护".equals(c)) || (c != null && c.startsWith("设备维护：")))) continue;
                            String hu = x.getHandleUser();
                            String ht = x.getHandleTime();
                            boolean unhandled = (hu == null || hu.trim().isEmpty()) && (ht == null || ht.trim().isEmpty());
                            if (!unhandled) continue;
                            try { db.updateMaintenanceHandled(x.getId(), 0L, autoUser, autoTime, autoRemark); } catch (Exception ignored2) {}
                            maintenanceTouched = true;
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (!timedMaintenanceNeeded) {
                try {
                    if (existingAll != null) {
                        String autoUser = com.lora.cn.database.DatabaseHelper.getInstance(getApplicationContext()).resolveEffectiveUserNameForAuto();
                        String autoTime = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                        String autoRemark = "定时维护清除：自动标记已维护";
                        long now2 = System.currentTimeMillis();
                        java.text.SimpleDateFormat sdf1 = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault());
                        java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                        java.text.SimpleDateFormat sdf3 = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault());
                        java.text.SimpleDateFormat sdf4 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
                        for (com.lora.cn.ui.model.MaintenanceInfo x : existingAll) {
                            if (x == null) continue;
                            if (x.getStatus() != 0) continue;
                            String c = x.getContent();
                            if ("主动维护".equals(c) || (c != null && c.startsWith("设备维护："))) continue;
                            String hu = x.getHandleUser();
                            String ht = x.getHandleTime();
                            boolean unhandled = (hu == null || hu.trim().isEmpty()) && (ht == null || ht.trim().isEmpty());
                            if (!unhandled) continue;
                            String ct = x.getCreateTime();
                            boolean due = true;
                            if (ct != null && !ct.trim().isEmpty()) {
                                try {
                                    java.util.Date dt = null;
                                    try { dt = sdf1.parse(ct.trim()); } catch (Exception ignored) {}
                                    if (dt == null) { try { dt = sdf2.parse(ct.trim()); } catch (Exception ignored) {} }
                                    if (dt == null) { try { dt = sdf3.parse(ct.trim()); } catch (Exception ignored) {} }
                                    if (dt == null) { try { dt = sdf4.parse(ct.trim()); } catch (Exception ignored) {} }
                                    if (dt != null) due = dt.getTime() <= now2;
                                } catch (Exception ignored) {}
                            }
                            if (!due) continue;
                            try { db.updateMaintenanceHandled(x.getId(), 0L, autoUser, autoTime, autoRemark); } catch (Exception ignored2) {}
                            maintenanceTouched = true;
                        }
                    }
                } catch (Exception ignored) {}
            }
            boolean need8001ByConfig = false;
            try { need8001ByConfig = helper.isNeedDownlink8001(frame); } catch (Exception ignored) {}
            boolean timedMaintenanceDue = !dueMaint.isEmpty();
            boolean shouldSend = clearActivePending || timedMaintenanceDue || need8001ByConfig;
            if (!shouldSend) {
                if (maintenanceTouched) {
                    try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("维护刷新:" + frame.deviceId)); } catch (Exception ignored) {}
                }
                return;
            }
            int intervalMin = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
            int normalizedInterval = Math.max(3, Math.min(1440, intervalMin));
            int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
            int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
            int fallbackMins = Math.max(0, Math.min(1440, h * 60 + m));
            int clearMask = (timedMaintenanceDue ? (1 << 1) : 0) | (clearActivePending ? (1 << 2) : 0);
            try {
                helper.sendDownlink8001(frame.deviceId, 1, 1, depId, cartId, 0, clearMask, normalizedInterval, 1, new int[]{fallbackMins}, true);
                if (timedMaintenanceDue) {
                    String sentTime = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                    for (com.lora.cn.ui.model.MaintenanceInfo mi : dueMaint) {
                        try { db.updateMaintenanceSent(mi.getId(), sentTime); } catch (Exception ignored) {}
                    }
                    maintenanceTouched = true;
                }
            } catch (Exception ignored) {}
            if (clearActivePending) {
                try { db.setTerminalMaintenanceClearPending(frame.deviceId, false); } catch (Exception ignored) {}
            }
            if (maintenanceTouched) {
                try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("维护刷新:" + frame.deviceId)); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }
}
