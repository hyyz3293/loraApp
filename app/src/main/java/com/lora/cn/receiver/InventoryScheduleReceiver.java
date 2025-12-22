package com.lora.cn.receiver;

import android.content.Context;
import android.content.Intent;

public class InventoryScheduleReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean enabled = com.blankj.utilcode.util.SPUtils.getInstance().getBoolean("inventory_schedule_enabled", false);
        if (!enabled) return;
        com.lora.cn.network.MqttPacketsClient client = new com.lora.cn.network.MqttPacketsClient();
        String brokerUrl;
        com.blankj.utilcode.util.SPUtils sp = com.blankj.utilcode.util.SPUtils.getInstance();
        int localPort = sp.getInt("mqtt_local_broker_port", 1883);
        brokerUrl = "tcp://127.0.0.1:" + (localPort > 0 ? localPort : 1883);
        String topicFilter = sp.getString("mqtt_topic_filter", "/milesight/uplink/#");
        String username = sp.getString("mqtt_username", "");
        String password = sp.getString("mqtt_password", "");
        boolean trustAll = sp.getBoolean("mqtt_trust_all_certs", false);
        String clientId = "android-schedule-" + System.currentTimeMillis();
        client.connectAndSubscribe(context.getApplicationContext(), brokerUrl, clientId, topicFilter, username, password, trustAll, new com.lora.cn.network.GatewayPacketsClient.PacketsListener() {
            @Override public void onStatus(String msg) {}
            @Override public void onError(String error) {}
            @Override public void onComplete() {}
            @Override public void onPackets(java.util.List<com.lora.cn.network.GatewayPacketsClient.PacketRecord> records) {}
        });
        com.lora.cn.utils.DownlinkMessageHelper helper = new com.lora.cn.utils.DownlinkMessageHelper(client);
        try {
            com.lora.cn.database.DatabaseHelper db = com.lora.cn.database.DatabaseHelper.getInstance(context.getApplicationContext());
            java.util.List<com.lora.cn.ui.model.Terminal> terminals = db.getAllTerminals();
            for (com.lora.cn.ui.model.Terminal t : terminals) {
                String dev = t.getTerminalId() != null ? t.getTerminalId().trim().replace(" ", "") : "";
                if (dev.length() != 16) continue;
                int dep = (int) Math.max(0, Math.min(255, t.getDepartmentId()));
                int cart = (int) Math.max(0, Math.min(255, t.getRoomId()));
                helper.sendDownlink8001(dev, 1, 1, dep, cart, 0, 0, 60, true);
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
                    li.setAction("终端清点: 定时维护");
                    //db.addLog(li);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }
}
