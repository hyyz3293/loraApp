package com.lora.cn.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.text.method.ScrollingMovementMethod;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.util.Log;

import androidx.annotation.NonNull;

import com.lora.cn.R;
import com.lora.cn.utils.LoRaProtocolParser;
// import com.lora.cn.network.GatewayClient; // 已切换MQTT，不再使用网关扫描
import com.lora.cn.network.MqttPacketsClient;
import com.lora.cn.network.GatewayPacketsClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.os.Handler;
import android.os.Looper;

public class TerminalDetailDialog extends Dialog {

    private final Context context;
    private final LoRaProtocolParser.TerminalInfo info;
    // private GatewayClient gatewayClient; // 移除
    private MqttPacketsClient mqttClient;
    private Handler handler;
    private boolean refreshing = false;

    private TextView tvDeviceName;
    private TextView tvDeviceId;
    private TextView tvDepartment;
    private TextView tvLocation;
    private TextView tvSignal;
    private TextView tvBattery;
    private TextView tvStatus;
    private TextView tvTime;
    private TextView tvPayload;
    private ProgressBar pbBattery;
    private View btnClose;
    private View btnCopy;
    private View btnRefresh;
    private View btnDownlink;
    private TextView tvRefreshStatus;

    public TerminalDetailDialog(@NonNull Context context, LoRaProtocolParser.TerminalInfo info) {
        super(context);
        this.context = context;
        this.info = info;
        // this.gatewayClient = new GatewayClient(); // 移除
        this.handler = new Handler(Looper.getMainLooper());
        initDialog();
    }

    private void initDialog() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_terminal_detail, null);
        setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tv_title);
        tvDeviceName = view.findViewById(R.id.tv_device_name);
        tvDeviceId = view.findViewById(R.id.tv_device_id);
        tvDepartment = view.findViewById(R.id.tv_department);
        tvLocation = view.findViewById(R.id.tv_location);
        tvSignal = view.findViewById(R.id.tv_signal);
        tvBattery = view.findViewById(R.id.tv_battery);
        tvStatus = view.findViewById(R.id.tv_status);
        tvTime = view.findViewById(R.id.tv_time);
        tvPayload = view.findViewById(R.id.tv_payload);
        pbBattery = view.findViewById(R.id.pb_battery);
        btnClose = view.findViewById(R.id.btn_close);
        btnCopy = view.findViewById(R.id.btn_copy);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        btnDownlink = view.findViewById(R.id.btn_downlink);
        tvRefreshStatus = view.findViewById(R.id.tv_refresh_status);

        tvTitle.setText("终端详情");
        tvPayload.setMovementMethod(ScrollingMovementMethod.getInstance());
        bindInfo(info);

        btnClose.setOnClickListener(v -> {
            if (mqttClient != null) mqttClient.disconnect();
            dismiss();
        });
        btnCopy.setOnClickListener(v -> copyHexToClipboard());
        btnRefresh.setOnClickListener(v -> startMqttListen());
        btnDownlink.setOnClickListener(v -> sendDownlink8001Default());

        setOnDismissListener(d -> {
            if (mqttClient != null) mqttClient.disconnect();
        });
    }

    private void bindInfo(LoRaProtocolParser.TerminalInfo info) {
        tvDeviceName.setText(info.deviceName != null ? info.deviceName : "-");
        tvDeviceId.setText(info.deviceId != null ? info.deviceId : "-");
        tvDepartment.setText(info.department != null ? info.department : "-");
        tvLocation.setText(info.location != null ? info.location : "-");
        tvSignal.setText(String.valueOf(info.signalStrength));
        tvBattery.setText(info.batteryLevel + "%");
        pbBattery.setProgress(info.batteryLevel);
        tvStatus.setText(info.status == 1 ? "在线" : (info.status == 0 ? "离线" : "异常"));
        tvTime.setText(formatTime(info.timestamp));
        tvPayload.setText(info.payloadHex != null ? info.payloadHex : "");
    }

    private String formatTime(long ts) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(ts));
        } catch (Exception e) {
            return "";
        }
    }

    private void copyHexToClipboard() {
        try {
            String hex = tvPayload.getText() != null ? tvPayload.getText().toString() : "";
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("payloadHex", hex));
                tvRefreshStatus.setText("已复制HEX到剪贴板");
            }
        } catch (Exception e) {
            Log.w("TerminalDetailDialog", "复制失败: " + e.getMessage());
        }
    }

    private void startMqttListen() {
        if (refreshing) return;
        refreshing = true;
        tvRefreshStatus.setText("通过MQTT监听设备上行数据...");
        try {
            if (mqttClient == null) mqttClient = new MqttPacketsClient();
            final String brokerUrl = "tcp://broker.emqx.io:1883";
            final String clientId = "android-" + System.currentTimeMillis();
            final String topicFilter = "/milesight/uplink/#";
            mqttClient.connectAndSubscribe(context, brokerUrl, clientId, topicFilter,
                    null, null, false,
                    new GatewayPacketsClient.PacketsListener() {
                        @Override
                        public void onStatus(String msg) {
                            handler.post(() -> {
                                tvRefreshStatus.setText(msg);
                                Log.d("TerminalDetailDialog", "MQTT状态 TerminalDetail: " + msg);
                            });
                        }



                        @Override
                        public void onPackets(java.util.List<GatewayPacketsClient.PacketRecord> records) {
                            if (records == null || records.isEmpty()) return;
                            // 找到匹配当前设备ID的记录
                            for (GatewayPacketsClient.PacketRecord r : records) {
                                String dev = r.deviceId != null ? r.deviceId : "";
                                if (dev.equalsIgnoreCase(info.deviceId)) {
                                    handler.post(() -> {
                                        tvPayload.setText(r.payloadHex != null ? r.payloadHex : "");
                                        tvTime.setText(formatTime(System.currentTimeMillis()));
                                        tvRefreshStatus.setText("已接收到该设备上行数据");
                                        refreshing = false;
                                    });
                                    break;
                                }
                            }
                        }
                        @Override
                        public void onError(String error) {
                            handler.post(() -> {
                                tvRefreshStatus.setText("MQTT错误: " + error);
                                refreshing = false;
                            });
                        }
                        @Override
                        public void onComplete() {
                            handler.post(() -> {
                                tvRefreshStatus.setText("MQTT监听已结束");
                                refreshing = false;
                            });
                        }
                    });
        } catch (Exception e) {
            handler.post(() -> {
                tvRefreshStatus.setText("MQTT监听启动失败: " + e.getMessage());
                refreshing = false;
            });
        }
    }

    // 原 refresh 走网关扫描逻辑已移除
    private void startShortScan() {
        startMqttListen();
    }

    // 下发帮助方法：构建8001帧并通过Milesight下行主题发布
    public void sendDownlink8001Default() {
        try {
            String devEui = info.deviceId != null ? info.deviceId.trim().replace(" ", "") : "";
            if (devEui.isEmpty() || devEui.length() != 16) {
                tvRefreshStatus.setText("无效的设备ID（需16位HEX devEUI）");
                return;
            }
            byte seq = 0x01;
            long nowUtc = System.currentTimeMillis();
            int ackResult = 1;
            int queryOp = 0;
            int departmentId = 0;
            int cartId = 0;
            int registerResult = 1;
            int clearMask = 0;
            int reportIntervalMin = 5;
            byte[] frame = LoRaProtocolParser.buildDownlink8001(devEui, seq, nowUtc, ackResult, queryOp, departmentId, cartId, registerResult, clearMask, reportIntervalMin);
            String hex = bytesToHexCompact(frame);

            if (mqttClient == null) {
                mqttClient = new MqttPacketsClient();
            }
            try {
                // 发布到通用主题，devEUI放在消息体中
                mqttClient.publishDownlinkSimple("/milesight/downlink", devEui, hex, 85, true);
                tvRefreshStatus.setText("已下发8001到设备：" + devEui);
            } catch (Exception e) {
                tvRefreshStatus.setText("下发失败，请先点击刷新建立MQTT连接");
            }
        } catch (Exception e) {
            tvRefreshStatus.setText("下发构建失败：" + e.getMessage());
        }
    }

    private String bytesToHexCompact(byte[] bytes) {
        if (bytes == null) return "";
        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}