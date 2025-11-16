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

    @Override
    public void onCreate() {
        super.onCreate();
        ensureNotificationChannel();
        brokerExecutor = Executors.newSingleThreadExecutor();
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
}