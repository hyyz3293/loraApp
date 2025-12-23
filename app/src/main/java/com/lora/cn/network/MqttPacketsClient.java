package com.lora.cn.network;

import android.content.Context;
import android.util.Log;

import com.blankj.utilcode.util.LogUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 通过 MQTT 从 LoRa 网关/网络服务器订阅上行数据。
 * - 支持 tcp:// 与 ssl://（可选信任自签证书，仅限局域网使用）
 * - 将收到的消息解析为 GatewayPacketsClient.PacketRecord 并回调
 */
public class MqttPacketsClient {

    private static final String TAG = "MqttPacketsClient";
    private static final Pattern HEX_PATTERN = Pattern.compile("([0-9a-fA-F]{16,})");

    private static volatile MqttPacketsClient sharedInstance;
    private MqttAndroidClient client;
    private GatewayPacketsClient.PacketsListener listener;
    private android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private int subscribeRetry = 0;
    private final java.util.LinkedList<Runnable> pendingPublishes = new java.util.LinkedList<>();
    private volatile boolean connecting = false;
    private String currentBrokerUrl;
    private String currentClientId;
    private String currentTopicFilter;

    public static MqttPacketsClient getShared() {
        if (sharedInstance == null) {
            synchronized (MqttPacketsClient.class) {
                if (sharedInstance == null) {
                    sharedInstance = new MqttPacketsClient();
                }
            }
        }
        return sharedInstance;
    }

    /**
     * 连接并订阅主题。
     * @param context Android 上下文
     * @param brokerUrl 例如 tcp://192.168.1.1:1883 或 ssl://192.168.1.1:8883
     * @param clientId MQTT 客户端 ID（保持唯一）
     * @param topicFilter 主题过滤，例如 application/+/device/+/event/up 或 LoRa/Topic
     * @param username 可选用户名
     * @param password 可选密码
     * @param trustAllCerts 仅当使用 ssl:// 且为自签证书时设为 true（仅限内网）
     * @param listener 回调，复用 GatewayPacketsClient 的 PacketsListener
     */
    public void connectAndSubscribe(Context context,
                                    String brokerUrl,
                                    String clientId,
                                    String topicFilter,
                                    String username,
                                    String password,
                                    boolean trustAllCerts,
                                    GatewayPacketsClient.PacketsListener listener) {
        this.listener = listener;
        this.currentBrokerUrl = brokerUrl;
        this.currentClientId = clientId;
        this.currentTopicFilter = topicFilter;
        if (context == null || brokerUrl == null || brokerUrl.isEmpty()) {
            if (listener != null) listener.onError("参数无效：context/brokerUrl");
            return;
        }
        try {
            if (client != null && client.isConnected()) {
                if (listener != null) listener.onStatus("复用已有MQTT连接，继续订阅：" + topicFilter);
                android.util.Log.i(TAG, "复用MQTT连接: url=" + brokerUrl + ", clientId=" + clientId);
                scheduleSubscribe(topicFilter);
                flushPendingIfConnected();
                return;
            }
            if (connecting && client != null) {
                if (listener != null) listener.onStatus("MQTT正在连接，稍后订阅：" + topicFilter);
                scheduleSubscribe(topicFilter);
                return;
            }
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setAutomaticReconnect(true);
            opts.setCleanSession(true);
            opts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            if (username != null && !username.isEmpty()) opts.setUserName(username);
            if (password != null && !password.isEmpty()) opts.setPassword(password.toCharArray());

            if (brokerUrl.startsWith("ssl://") && trustAllCerts) {
                opts.setSocketFactory(buildUnsafeSocketFactory());
            }

            client = new MqttAndroidClient(context.getApplicationContext(), brokerUrl, clientId);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    if (listener != null) listener.onStatus("MQTT连接丢失：" + (cause == null ? "" : cause.getMessage()));
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload(), java.nio.charset.StandardCharsets.UTF_8);
                    // 将 payload 转为 hex 格式
                    String payloadHex = bytesToHex(message.getPayload());
                    android.util.Log.e(TAG, "messageArrived topic=" + topic + ", payload=" + payload);
                    //android.util.Log.e(TAG, "messageArrived payloadHex=" + payloadHex);
                    android.util.Log.e(TAG, "messageArrived topic=" + topic + ", messageInfo=" + message.toString());

                    if (listener != null) listener.onStatus("收到消息：" + topic);
                    java.util.List<com.lora.cn.network.GatewayPacketsClient.PacketRecord> records = parseToRecords(topic, payload);
                    if (!records.isEmpty() && listener != null) listener.onPackets(records);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 订阅消息无需处理发送完成
                }
            });

            if (listener != null) listener.onStatus("MQTT连接：" + brokerUrl);
            android.util.Log.i(TAG, "准备连接MQTT: url=" + brokerUrl + ", clientId=" + clientId + ", mqttVersion=3.1.1, topicFilter=" + topicFilter);
            connecting = true;
            client.connect(opts, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        connecting = false;
                        if (listener != null) listener.onStatus("MQTT连接成功，订阅：" + topicFilter);
                        subscribeRetry = 0;
                        scheduleSubscribe(topicFilter);
                        android.util.Log.i(TAG, "MQTT连接成功: url=" + brokerUrl + ", clientId=" + clientId);
                        flushPendingIfConnected();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        connecting = false;
                        String msg = (exception == null ? "" : exception.getMessage());
                        boolean already = msg != null && (msg.contains("已连接客户机") || msg.contains("Client is connected"));
                        if (already) {
                            if (listener != null) listener.onStatus("MQTT已连接，复用并订阅：" + topicFilter);
                            android.util.Log.w(TAG, "MQTT已连接（服务端返回32100），继续订阅: url=" + brokerUrl + ", clientId=" + clientId);
                            scheduleSubscribe(topicFilter);
                            flushPendingIfConnected();
                            return;
                        }
                        if (listener != null) listener.onError("MQTT连接失败：" + msg);
                        android.util.Log.e(TAG, "MQTT连接失败: url=" + brokerUrl + ", clientId=" + clientId + ", err=" + msg, exception);
                    }
                });
        } catch (Exception e) {
            if (listener != null) listener.onError("MQTT初始化异常：" + e.getMessage());
            android.util.Log.e(TAG, "MQTT初始化异常: " + e.getMessage(), e);
        }
    }

    public boolean isConnected() {
        try { return client != null && client.isConnected(); } catch (Exception ignore) { return false; }
    }

    private void flushPendingIfConnected() {
        try {
            if (client != null && client.isConnected()) {
                while (!pendingPublishes.isEmpty()) {
                    Runnable r = pendingPublishes.poll();
                    if (r != null) {
                        try { r.run(); } catch (Exception ignore) {}
                    }
                }
            }
        } catch (Exception ignore) {}
    }

    private void scheduleSubscribe(String topicFilter) {
        mainHandler.postDelayed(() -> {
            try {
                if (client == null) return;
                if (!client.isConnected()) {
                    if (subscribeRetry++ < 10) scheduleSubscribe(topicFilter);
                    else if (listener != null) listener.onError("订阅失败：连接未就绪");
                    return;
                }
                client.subscribe(topicFilter, 0, null, new IMqttActionListener() {
                    @Override public void onSuccess(IMqttToken asyncActionToken) {
                        if (listener != null) listener.onStatus("订阅成功：" + topicFilter);
                    }
                    @Override public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        if (listener != null) listener.onError("订阅失败：" + (exception == null ? "" : exception.getMessage()));
                    }
                });
                if (topicFilter != null && topicFilter.endsWith("/#")) {
                    String base = topicFilter.substring(0, topicFilter.length() - 2);
                    client.subscribe(base, 0, null, new IMqttActionListener() {
                        @Override public void onSuccess(IMqttToken asyncActionToken) {
                            if (listener != null) listener.onStatus("额外订阅成功：" + base);
                        }
                        @Override public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            if (listener != null) listener.onError("额外订阅失败：" + (exception == null ? "" : exception.getMessage()));
                        }
                    });
                }
            } catch (Exception ex) {
                if (subscribeRetry++ < 10) scheduleSubscribe(topicFilter);
                else if (listener != null) listener.onError("订阅异常：" + ex.getMessage());
            }
        }, 300);
    }

    /**
     * 断开连接并释放资源
     */
    public void disconnect() {
        try {
            if (client != null) {
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.unregisterResources();
                client.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "disconnect warn: " + e.getMessage());
        } finally {
            if (listener != null) listener.onComplete();
        }
    }

    // 解析消息为 PacketRecord（支持 ChirpStack/TTS 常见结构与纯HEX）
    private List<GatewayPacketsClient.PacketRecord> parseToRecords(String topic, String payload) {
        List<GatewayPacketsClient.PacketRecord> out = new ArrayList<>();
        if (payload == null || payload.isEmpty()) return out;
        try {
            String trimmed = payload.trim();
            // 尝试 JSON
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                JsonElement root = JsonParser.parseString(trimmed);
                if (root.isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject();
                    GatewayPacketsClient.PacketRecord r = new GatewayPacketsClient.PacketRecord();
                    // devEUI/设备ID
                    r.deviceId = getString(obj, "devEUI", "devEui", "deviceId", "devEuiHex");
                    if ((r.deviceId == null || r.deviceId.isEmpty())) {
                        // TTS: end_device_ids.dev_eui
                        if (obj.has("end_device_ids") && obj.get("end_device_ids").isJsonObject()) {
                            JsonObject ed = obj.get("end_device_ids").getAsJsonObject();
                            r.deviceId = getString(ed, "dev_eui", "devEui");
                        }
                        // 或从主题解析（例如 application/<appId>/device/<devEui>/event/up）
                        if (r.deviceId == null || r.deviceId.isEmpty()) r.deviceId = extractDevEuiFromTopic(topic);
                    }

                    // payload/base64
                    String base64 = getString(obj, "data", "frmPayload", "phyPayload");
                    if (base64 != null && !base64.isEmpty()) {
                        try {
                            byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                            r.payloadHex = bytesToHex(bytes);
                        } catch (IllegalArgumentException ignore) {
                            // 非法base64，继续其它字段
                        }
                    }
                    if (r.payloadHex == null || r.payloadHex.isEmpty()) {
                        String hexCandidate = getString(obj, "payload", "macPayload");
                        if (hexCandidate != null) {
                            Matcher m = HEX_PATTERN.matcher(hexCandidate);
                            if (m.find()) r.payloadHex = m.group(1);
                        }
                    }

                    r.time = getString(obj, "time", "ts", "timestamp");
                    r.rssi = getInt(obj, "rssi");
                    r.snr = getDouble(obj, "snr");
                    r.freq = getDouble(obj, "frequency", "freq");
                    r.rawLine = obj.toString();

                    if (r.payloadHex != null && r.payloadHex.length() >= 16) {
                        out.add(r);
                    }
                }
            } else {
                // 尝试纯文本HEX
                Matcher m = HEX_PATTERN.matcher(trimmed);
                if (m.find()) {
                    GatewayPacketsClient.PacketRecord r = new GatewayPacketsClient.PacketRecord();
                    r.payloadHex = m.group(1);
                    r.rawLine = trimmed;
                    // 从主题尝试提取 devEUI
                    r.deviceId = extractDevEuiFromTopic(topic);
                    out.add(r);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "parseToRecords warn: " + e.getMessage());
        }
        return out;
    }

    private String extractDevEuiFromTopic(String topic) {
        if (topic == null) return null;
        try {
            // 例如 application/<appId>/device/<devEui>/event/up 或自定义主题中包含 16位/8字节的hex
            String[] segs = topic.split("/");
            for (int i = 0; i < segs.length; i++) {
                if ("device".equalsIgnoreCase(segs[i]) && i + 1 < segs.length) {
                    String candidate = segs[i + 1];
                    if (candidate != null && candidate.matches("[0-9a-fA-F]{16}")) return candidate;
                }
            }
            Matcher m = HEX_PATTERN.matcher(topic);
            if (m.find()) return m.group(1);
        } catch (Exception ignore) {}
        return null;
    }

    // --------- 工具方法 ---------
    private String getString(JsonObject o, String... keys) {
        for (String k : keys) {
            if (o.has(k) && !o.get(k).isJsonNull()) {
                JsonElement el = o.get(k);
                if (el.isJsonPrimitive()) return el.getAsString();
                if (el.isJsonObject() && o.get(k).getAsJsonObject().has("text")) {
                    JsonElement t = o.get(k).getAsJsonObject().get("text");
                    if (t.isJsonPrimitive()) return t.getAsString();
                }
            }
        }
        return null;
    }

    private Integer getInt(JsonObject o, String... keys) {
        for (String k : keys) {
            if (o.has(k) && !o.get(k).isJsonNull() && o.get(k).isJsonPrimitive()) {
                try { return o.get(k).getAsInt(); } catch (Exception ignore) {}
            }
        }
        return null;
    }

    private Double getDouble(JsonObject o, String... keys) {
        for (String k : keys) {
            if (o.has(k) && !o.get(k).isJsonNull() && o.get(k).isJsonPrimitive()) {
                try { return o.get(k).getAsDouble(); } catch (Exception ignore) {}
            }
        }
        return null;
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * 构造一个信任所有证书的 SSLSocketFactory，仅用于局域网自签环境。
     */
    private SSLSocketFactory buildUnsafeSocketFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            Log.w(TAG, "buildUnsafeSocketFactory warn: " + e.getMessage());
            return null;
        }
    }

    /**
     * 发布下行到通用主题（方式1：devEUI在消息体中）。
     * 主题示例：/milesight/downlink
     * 内容示例：{"devEUI":"24E124460E040073","confirmed":true,"fport":85,"data":"/x0gAA=="}
     */
    public void publishDownlinkSimple(String topicBase,
                                      String devEui,
                                      String payloadHex,
                                      int fport,
                                      boolean confirmed) {
        try {
            if (client == null || !client.isConnected()) {
                pendingPublishes.add(() -> {
                    publishDownlinkSimple(topicBase, devEui, payloadHex, fport, confirmed);
                });
                if (listener != null) listener.onStatus("MQTT未连接，已加入下发队列：" + devEui);
                android.util.Log.w(TAG, "MQTT未连接，加入队列等待连接: topicBase=" + topicBase + ", devEUI=" + devEui + ", queueSize=" + pendingPublishes.size());
                return;
            }
            String b64 = hexToBase64(payloadHex);
            String json = String.format(java.util.Locale.US,
                    "{\"devEUI\":\"%s\",\"confirmed\":%s,\"fport\":%d,\"data\":\"%s\"}",
                    devEui, confirmed ? "true" : "false", fport, b64);
            org.eclipse.paho.client.mqttv3.MqttMessage msg = new org.eclipse.paho.client.mqttv3.MqttMessage(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            msg.setQos(0);
            msg.setRetained(false);
            client.publish(topicBase, msg);
            if (listener != null) listener.onStatus("下发到主题：" + topicBase + " devEUI=" + devEui);
            android.util.Log.i(TAG, "已发布下行(simple): topic=" + topicBase + ", devEUI=" + devEui + ", fport=" + fport + ", confirmed=" + confirmed + ", payloadHexLen=" + (payloadHex == null ? 0 : payloadHex.length()) + ", base64Len=" + b64.length() + ", hex=" + payloadHex + ", json=" + json);
        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            if (listener != null) listener.onError("下发失败：" + e.getMessage());
            android.util.Log.e(TAG, "下发失败(simple): topic=" + topicBase + ", devEUI=" + devEui + ", err=" + e.getMessage(), e);
        }
        }

    /**
     * 发布下行到按设备主题（方式2：devEUI在主题路径中）。
     * 主题示例：/milesight/downlink/$deveui → 实际发布 /milesight/downlink/<devEui>
     * 内容示例：{"confirmed":true,"fport":85,"data":"CQEA/w=="}
     */
    public void publishDownlinkByDevEuiTopic(String topicBase,
                                             String devEui,
                                             String payloadHex,
                                             int fport,
                                             boolean confirmed) {
        try {
            if (client == null || !client.isConnected()) {
                pendingPublishes.add(() -> {
                    publishDownlinkByDevEuiTopic(topicBase, devEui, payloadHex, fport, confirmed);
                });
                if (listener != null) listener.onStatus("MQTT未连接，已加入下发队列：" + devEui);
                android.util.Log.w(TAG, "MQTT未连接，加入队列等待连接(by-topic): base=" + topicBase + ", devEUI=" + devEui + ", queueSize=" + pendingPublishes.size());
                return;
            }
            String b64 = hexToBase64(payloadHex);
            String json = String.format(java.util.Locale.US,
                    "{\"confirmed\":%s,\"fport\":%d,\"data\":\"%s\"}",
                    confirmed ? "true" : "false", fport, b64);
            String topic = topicBase.endsWith("/") ? (topicBase + devEui) : (topicBase + "/" + devEui);
            org.eclipse.paho.client.mqttv3.MqttMessage msg = new org.eclipse.paho.client.mqttv3.MqttMessage(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            msg.setQos(0);
            msg.setRetained(false);
            client.publish(topic, msg);
            if (listener != null) listener.onStatus("下发到主题：" + topic);
            android.util.Log.i(TAG, "已发布下行(by-topic): topic=" + topic + ", devEUI=" + devEui + ", fport=" + fport + ", confirmed=" + confirmed + ", payloadHexLen=" + (payloadHex == null ? 0 : payloadHex.length()) + ", base64Len=" + b64.length() + ", hex=" + payloadHex + ", json=" + json);
        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            if (listener != null) listener.onError("下发失败：" + e.getMessage());
            android.util.Log.e(TAG, "下发失败(by-topic): base=" + topicBase + ", devEUI=" + devEui + ", err=" + e.getMessage(), e);
        }
        }

    // 将HEX转换为Base64（Milesight下发要求data为Base64）
    private String hexToBase64(String hex) {
        byte[] bytes = hexToBytes(hex);
        if (bytes == null) return "";
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null) return null;
        String clean = hex.replaceAll("[^0-9A-Fa-f]", "");
        if (clean.length() % 2 != 0) return null;
        int len = clean.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(clean.charAt(i), 16);
            int lo = Character.digit(clean.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) return null;
            out[i / 2] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
