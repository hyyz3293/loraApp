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
import com.lora.cn.network.GatewayClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.os.Handler;
import android.os.Looper;

public class TerminalDetailDialog extends Dialog {

    private final Context context;
    private final LoRaProtocolParser.TerminalInfo info;
    private GatewayClient gatewayClient;
    private Handler handler;
    private boolean refreshing = false;

    private ProgressBar pbBattery;
    private TextView tvDeviceName, tvDeviceId, tvDepartment, tvLocation, tvSignal, tvBattery, tvStatus, tvTime, tvPayload;
    private TextView btnClose, btnCopy, btnRefresh, tvRefreshStatus;

    public TerminalDetailDialog(@NonNull Context context, LoRaProtocolParser.TerminalInfo info) {
        super(context);
        this.context = context;
        this.info = info;
        this.gatewayClient = new GatewayClient();
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
        tvRefreshStatus = view.findViewById(R.id.tv_refresh_status);

        tvTitle.setText("终端详情");
        tvPayload.setMovementMethod(ScrollingMovementMethod.getInstance());
        bindInfo(info);

        btnClose.setOnClickListener(v -> {
            gatewayClient.stopScan();
            dismiss();
        });
        btnCopy.setOnClickListener(v -> copyHexToClipboard());
        btnRefresh.setOnClickListener(v -> startShortScan());

        setOnDismissListener(d -> gatewayClient.stopScan());
    }

    private void bindInfo(LoRaProtocolParser.TerminalInfo i) {
        tvDeviceName.setText("设备名称: " + safe(i.deviceName));
        tvDeviceId.setText("设备ID: " + safe(i.deviceId));
        tvDepartment.setText("所属科室: " + safe(i.department));
        tvLocation.setText("位置信息: " + safe(i.location));
        tvSignal.setText("信号强度: " + i.signalStrength + " dBm");
        tvBattery.setText("电池电量: " + i.batteryLevel + "%");
        tvStatus.setText("状态: " + statusText(i.status));
        tvTime.setText("时间: " + formatTime(i.timestamp));
        tvPayload.setText("原始HEX: " + safe(i.payloadHex));
        if (pbBattery != null) pbBattery.setProgress(Math.max(0, Math.min(100, i.batteryLevel)));
    }

    private void copyHexToClipboard() {
        String hex = safe(info.payloadHex);
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("payload_hex", hex));
            if (tvRefreshStatus != null) tvRefreshStatus.setText("已复制HEX到剪贴板");
        }
    }

    private void startShortScan() {
        if (refreshing) return;
        refreshing = true;
        if (tvRefreshStatus != null) tvRefreshStatus.setText("正在刷新...");
        if (btnRefresh != null) btnRefresh.setEnabled(false);
        final String targetId = info.deviceId == null ? "" : info.deviceId.trim();
        gatewayClient.startScan(new GatewayClient.ScanListener() {
            @Override
            public void onDeviceFound(LoRaProtocolParser.TerminalInfo found) {
                handler.post(() -> {
                    boolean match = (targetId.isEmpty() || targetId.equalsIgnoreCase(found.deviceId));
                    if (match) {
                        info.deviceId = found.deviceId;
                        info.deviceName = found.deviceName;
                        info.department = found.department;
                        info.location = found.location;
                        info.signalStrength = found.signalStrength;
                        info.batteryLevel = found.batteryLevel;
                        info.status = found.status;
                        info.timestamp = found.timestamp;
                        info.payloadHex = found.payloadHex;
                        bindInfo(info);
                        if (tvRefreshStatus != null) tvRefreshStatus.setText("已获取最新上报");
                    }
                });
            }

            @Override
            public void onStatus(String msg) {
                handler.post(() -> { 
                    if (tvRefreshStatus != null) tvRefreshStatus.setText(msg); 
                    Log.d("TerminalDetailDialog", "刷新状态: " + msg);
                });
            }

            @Override
            public void onError(String error) {
                Log.e("TerminalDetailDialog", "刷新扫描错误: " + error);
                handler.post(() -> {
                    refreshing = false;
                    if (btnRefresh != null) btnRefresh.setEnabled(true);
                    showRefreshError(error);
                });
            }

            @Override
            public void onComplete() {
                handler.post(() -> {
                    refreshing = false;
                    if (btnRefresh != null) btnRefresh.setEnabled(true);
                    if (tvRefreshStatus != null) tvRefreshStatus.append(" 任务完成");
                });
            }
        });

        // 超时制动：8秒后停止扫描
        handler.postDelayed(() -> {
            gatewayClient.stopScan();
        }, 8000);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String statusText(int status) {
        switch (status) {
            case 0: return "离线";
            case 1: return "在线";
            case 2: return "异常";
            default: return String.valueOf(status);
        }
    }

    private void showRefreshError(String error) {
        if (tvRefreshStatus != null) {
            tvRefreshStatus.setText("刷新失败: " + error);
        }
        
        // 创建详细的错误对话框
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("刷新失败");
        
        String detailedMessage = "无法获取终端最新数据：\n" + error;
        String suggestions = "";
        
        // 根据错误类型提供具体的解决建议
        if (error.contains("Connection refused") || error.contains("连接被拒绝")) {
            suggestions = "\n\n可能原因及解决方案：\n" +
                    "• 网关设备可能已关闭或重启中\n" +
                    "• 网关服务未在端口6000上运行\n" +
                    "• 网关防火墙阻止了连接\n\n" +
                    "建议操作：\n" +
                    "1. 检查网关设备电源和运行状态\n" +
                    "2. 等待1-2分钟后重试\n" +
                    "3. 联系网关管理员检查服务状态";
        } else if (error.contains("timeout") || error.contains("超时")) {
            suggestions = "\n\n可能原因及解决方案：\n" +
                    "• 网络连接不稳定或延迟较高\n" +
                    "• 网关设备负载过高\n" +
                    "• 终端设备可能已离线\n\n" +
                    "建议操作：\n" +
                    "1. 检查WiFi信号强度\n" +
                    "2. 移动到网关设备附近重试\n" +
                    "3. 稍后再次尝试刷新";
        } else if (error.contains("无法ping通") || error.contains("网络不可达")) {
            suggestions = "\n\n可能原因及解决方案：\n" +
                    "• 设备与网关不在同一网络\n" +
                    "• 网关IP地址已更改\n" +
                    "• 网络配置问题\n\n" +
                    "建议操作：\n" +
                    "1. 检查WiFi连接状态\n" +
                    "2. 确认网关IP地址是否正确\n" +
                    "3. 重新连接WiFi网络";
        } else {
            suggestions = "\n\n通用解决方案：\n" +
                    "1. 检查网络连接状态\n" +
                    "2. 确认网关设备正常运行\n" +
                    "3. 稍后重试或联系技术支持\n" +
                    "4. 如果问题持续，请记录错误信息";
        }
        
        builder.setMessage(detailedMessage + suggestions);
        builder.setPositiveButton("确定", null);
        builder.setNeutralButton("重试", (dialog, which) -> {
            dialog.dismiss();
            startShortScan();
        });
        
        builder.create().show();
    }

    private String formatTime(long ts) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date(ts));
        } catch (Exception e) {
            return String.valueOf(ts);
        }
    }
}