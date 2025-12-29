package com.lora.cn.utils;

import android.util.Log;
import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.network.MqttPacketsClient;

/**
 * 下行报文调用辅助类
 * 根据下行报文规则编写调用函数
 */
public class DownlinkMessageHelper {
    
    private static final String TAG = "DownlinkMessageHelper";
    
    // MQTT客户端实例
    private MqttPacketsClient mqttClient;
    
    // 下行主题配置
    private static final String DOWNLINK_TOPIC_BASE = "/milesight/downlink";
    private static final int DEFAULT_FPORT = 85;
    
    public DownlinkMessageHelper(MqttPacketsClient mqttClient) {
        this.mqttClient = mqttClient;
    }
    
    /**
     * 发送下行8001报文到指定设备
     * @param deviceIdHex 设备ID (16位HEX字符串)
     * @param ackResult 应答结果 (0:成功, 1:失败)
     * @param queryOp 查询指令 (0:无操作, 1:查询状态, 2:查询配置等)
     * @param departmentId 科室ID
     * @param cartId 台车ID
     * @param registerResult 登记结果 (0:未登记, 1:已登记)
     * @param clearMask 清除码 (4字节掩码)
     * @param reportIntervalMin 定时上报间隔(分钟)
     * @param confirmed 是否需要确认
     */
    public void sendDownlink8001(String deviceIdHex, 
                                int ackResult,
                                int queryOp,
                                int departmentId,
                                int cartId,
                                int registerResult,
                                int clearMask,
                                int reportIntervalMin,
                                boolean confirmed) {
        try {
            if (mqttClient == null) {
                Log.e(TAG, "MQTT客户端未初始化");
                return;
            }
            
            int alarmCount = 1;
            int[] alarmMinutes = new int[]{getGlobalAlarmMinute()};
            int intervalMin = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", reportIntervalMin);
            intervalMin = Math.max(3, Math.min(1440, intervalMin));
            int lowBatteryPercent = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
            byte sequence = (byte) (System.currentTimeMillis() & 0xFF);
            long utcMs = System.currentTimeMillis();
            byte[] downlinkFrame = LoRaProtocolParser.buildDownlink8001Full(
                deviceIdHex,
                sequence,
                utcMs,
                0xFFFFFFFF,
                0xFFFFFFFF,
                0xFFFF,
                lowBatteryPercent,
                ackResult,
                departmentId,
                cartId,
                0xFF,
                0xFF,
                queryOp,
                clearMask,
                intervalMin,
                alarmCount,
                alarmMinutes
            );
            
            // 转换为HEX字符串
            String payloadHex = LoRaProtocolParser.bytesToHex(downlinkFrame);
            String payloadClean = payloadHex.replaceAll("\\s+", "");
            String base64 = android.util.Base64.encodeToString(hexToBytesLocal(payloadClean), android.util.Base64.NO_WRAP);
            String utcText = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date(utcMs));
            Log.i(TAG,
                    "下行8001字段: 设备ID=" + deviceIdHex +
                    ", 序列号=" + (sequence & 0xFF) +
                    ", UTC毫秒=" + utcMs +
                    ", 本地时间=" + utcText +
                    ", 保留2(4B)=" + String.format(java.util.Locale.US, "0x%08X", 0xFFFFFFFF) +
                    ", 保留3(4B)=" + String.format(java.util.Locale.US, "0x%08X", 0xFFFFFFFF) +
                    ", 保留4(2B)=" + String.format(java.util.Locale.US, "0x%04X", 0xFFFF) +
                    ", 低电量阈值(%)=" + lowBatteryPercent +
                    ", 应答结果=" + ackResult +
                    ", 查询操作=" + queryOp +
                    ", 科室ID=" + departmentId +
                    ", 台车ID=" + cartId +
                    ", 保留9(1B)=" + String.format(java.util.Locale.US, "0x%02X", 0xFF) +
                    ", 保留10(1B)=" + String.format(java.util.Locale.US, "0x%02X", 0xFF) +
                    ", 清除掩码=" + String.format(java.util.Locale.US, "0x%08X", clearMask) +
                    ", 上报间隔(分钟)=" + intervalMin +
                    ", 闹钟数量=" + alarmCount +
                    ", 闹钟分钟列表=" + java.util.Arrays.toString(alarmMinutes) +
                    ", 端口=" + DEFAULT_FPORT +
                    ", 需确认=" + confirmed +
                    ", HEX长度=" + payloadHex.length() +
                    ", Base64长度=" + base64.length() +
                    ", HEX串=" + payloadHex);
            
            // 通过MQTT发送下行报文 (使用设备主题方式)
            mqttClient.publishDownlinkByDevEuiTopic(
                DOWNLINK_TOPIC_BASE,
                deviceIdHex,
                payloadHex,
                DEFAULT_FPORT,
                confirmed
            );
            
            Log.i(TAG, "发送下行8001报文成功: deviceId=" + deviceIdHex + 
                      ", ackResult=" + ackResult + 
                      ", queryOp=" + queryOp + 
                      ", departmentId=" + departmentId + 
                      ", cartId=" + cartId + 
                      ", registerResult=" + registerResult + 
                      ", clearMask=" + clearMask + 
                      ", reportInterval=" + reportIntervalMin + "分钟" +
                      ", alarms=" + alarmCount +
                      ", payloadHex=" + payloadHex);
                      
        } catch (Exception e) {
            Log.e(TAG, "发送下行8001报文失败: " + e.getMessage(), e);
        }
    }

    public void sendDownlink8001(String deviceIdHex,
                                 int ackResult,
                                 int queryOp,
                                 int departmentId,
                                 int cartId,
                                 int registerResult,
                                 int clearMask,
                                 int reportIntervalMin,
                                 int alarmCount,
                                 int[] alarmMinutes,
                                 boolean confirmed) {
        try {
            if (mqttClient == null) {
                Log.e(TAG, "MQTT客户端未初始化");
                return;
            }
            int lowBatteryPercent = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
            byte sequence = (byte) (System.currentTimeMillis() & 0xFF);
            long utcMs = System.currentTimeMillis();
            byte[] downlinkFrame = LoRaProtocolParser.buildDownlink8001Full(
                deviceIdHex,
                sequence,
                utcMs,
                0xFFFFFFFF,
                0xFFFFFFFF,
                0xFFFF,
                lowBatteryPercent,
                ackResult,
                departmentId,
                cartId,
                0xFF,
                0xFF,
                queryOp,
                clearMask,
                Math.max(5, Math.min(1440, reportIntervalMin)),
                alarmCount,
                alarmMinutes
            );
            String payloadHex = LoRaProtocolParser.bytesToHex(downlinkFrame);
            String payloadClean = payloadHex.replaceAll("\\s+", "");
            String base64 = android.util.Base64.encodeToString(hexToBytesLocal(payloadClean), android.util.Base64.NO_WRAP);
            String utcText = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date(utcMs));
            Log.i(TAG,
                    "下行8001字段: 设备ID=" + deviceIdHex +
                    ", 序列号=" + (sequence & 0xFF) +
                    ", UTC毫秒=" + utcMs +
                    ", 本地时间=" + utcText +
                    ", 保留2(4B)=" + String.format(java.util.Locale.US, "0x%08X", 0xFFFFFFFF) +
                    ", 保留3(4B)=" + String.format(java.util.Locale.US, "0x%08X", 0xFFFFFFFF) +
                    ", 保留4(2B)=" + String.format(java.util.Locale.US, "0x%04X", 0xFFFF) +
                    ", 低电量阈值(%)=" + lowBatteryPercent +
                    ", 应答结果=" + ackResult +
                    ", 查询操作=" + queryOp +
                    ", 科室ID=" + departmentId +
                    ", 台车ID=" + cartId +
                    ", 保留9(1B)=" + String.format(java.util.Locale.US, "0x%02X", 0xFF) +
                    ", 保留10(1B)=" + String.format(java.util.Locale.US, "0x%02X", 0xFF) +
                    ", 清除掩码=" + String.format(java.util.Locale.US, "0x%08X", clearMask) +
                    ", 上报间隔(分钟)=" + Math.max(3, Math.min(1440, reportIntervalMin)) +
                    ", 闹钟数量=" + alarmCount +
                    ", 闹钟分钟列表=" + java.util.Arrays.toString(alarmMinutes) +
                    ", 端口=" + DEFAULT_FPORT +
                    ", 需确认=" + confirmed +
                    ", HEX长度=" + payloadHex.length() +
                    ", Base64长度=" + base64.length() +
                    ", HEX串=" + payloadHex);
            mqttClient.publishDownlinkByDevEuiTopic(
                DOWNLINK_TOPIC_BASE,
                deviceIdHex,
                payloadHex,
                DEFAULT_FPORT,
                confirmed
            );
            Log.i(TAG, "发送下行8001报文成功: deviceId=" + deviceIdHex +
                    ", ackResult=" + ackResult +
                    ", queryOp=" + queryOp +
                    ", departmentId=" + departmentId +
                    ", cartId=" + cartId +
                    ", registerResult=" + registerResult +
                    ", clearMask=" + clearMask +
                    ", reportInterval=" + reportIntervalMin + "分钟" +
                    ", alarms=" + alarmCount);
        } catch (Exception e) {
            Log.e(TAG, "发送下行8001报文失败: " + e.getMessage(), e);
        }
    }

    /**
     * 简化入口：仅传入设备ID与是否确认，功能ID固定8001
     */
    public void sendDownlink(String deviceIdHex, String functionIdHex, boolean confirmed) {
        if (functionIdHex == null || !functionIdHex.equalsIgnoreCase("8001")) {
            Log.e(TAG, "暂不支持功能ID=" + functionIdHex + " 的下行构建");
            return;
        }
        try {
            byte[] frame = LoRaProtocolParser.buildDownlink8001Simple(deviceIdHex);
            String payloadHex = LoRaProtocolParser.bytesToHex(frame);
            mqttClient.publishDownlinkByDevEuiTopic(
                DOWNLINK_TOPIC_BASE,
                deviceIdHex,
                payloadHex,
                DEFAULT_FPORT,
                confirmed
            );
            Log.i(TAG, "已下发8001到设备: devEUI=" + deviceIdHex + ", payload=" + payloadHex);
        } catch (Exception e) {
            Log.e(TAG, "下发8001失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送标准应答下行报文
     * 用于对设备上行数据进行应答确认
     * @param deviceIdHex 设备ID
     * @param ackResult 应答结果 (0:成功, 1:失败)
     */
    public void sendAckDownlink(String deviceIdHex, int ackResult) {
        sendDownlink8001(
            deviceIdHex,
            ackResult,
            0,
            0,
            0,
            0,
            0,
            com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3),
            true
        );
    }
    public void sendAckDownlink(String deviceIdHex, int ackResult, int alarmCount, int[] alarmMinutes) {
        try {
            int lowBatteryPercent = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
            int intervalMin = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
            byte[] downlinkFrame = LoRaProtocolParser.buildDownlink8001Full(
                deviceIdHex,
                (byte) (System.currentTimeMillis() & 0xFF),
                System.currentTimeMillis(),
                0xFFFFFFFF,
                0xFFFFFFFF,
                0xFFFF,
                lowBatteryPercent,
                ackResult,
                0,
                0,
                0xFF,
                0xFF,
                0,
                0,
                intervalMin,
                alarmCount,
                alarmMinutes
            );
            String payloadHex = LoRaProtocolParser.bytesToHex(downlinkFrame);
            Log.i(TAG, "准备下发ACK: devEUI=" + deviceIdHex + ", ack=" + ackResult + ", alarms=" + alarmCount + ", payloadHexLen=" + payloadHex.length());
            mqttClient.publishDownlinkByDevEuiTopic(
                DOWNLINK_TOPIC_BASE,
                deviceIdHex,
                payloadHex,
                DEFAULT_FPORT,
                true
            );
            Log.i(TAG, "ACK下发已调用publish: devEUI=" + deviceIdHex);
        } catch (Exception e) {
            Log.e(TAG, "ACK下发失败: devEUI=" + deviceIdHex + ", err=" + e.getMessage(), e);
        }
    }
    
    /**
     * 发送查询状态下行报文
     * 用于主动查询设备状态
     * @param deviceIdHex 设备ID
     */
    public void sendQueryStatusDownlink(String deviceIdHex) {
        try {
            int lowBatteryPercent = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
            int intervalMin = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
            byte[] downlinkFrame = LoRaProtocolParser.buildDownlink8001Full(
                deviceIdHex,
                (byte) (System.currentTimeMillis() & 0xFF),
                System.currentTimeMillis(),
                0xFFFFFFFF,
                0xFFFFFFFF,
                0xFFFF,
                lowBatteryPercent,
                1,
                0,
                0,
                0xFF,
                0xFF,
                1,
                0,
                intervalMin,
                1,
                new int[]{getGlobalAlarmMinute()}
            );
            String payloadHex = LoRaProtocolParser.bytesToHex(downlinkFrame);
            mqttClient.publishDownlinkByDevEuiTopic(
                DOWNLINK_TOPIC_BASE,
                deviceIdHex,
                payloadHex,
                DEFAULT_FPORT,
                true
            );
            Log.i(TAG, "查询下发已调用publish: devEUI=" + deviceIdHex);
        } catch (Exception e) {
            Log.e(TAG, "查询下发失败: devEUI=" + deviceIdHex + ", err=" + e.getMessage(), e);
        }
    }
    public void sendQueryStatusDownlink(String deviceIdHex, int alarmCount, int[] alarmMinutes) {
        try {
            int lowBatteryPercent = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
            byte[] downlinkFrame = LoRaProtocolParser.buildDownlink8001Full(
                deviceIdHex,
                (byte) (System.currentTimeMillis() & 0xFF),
                System.currentTimeMillis(),
                0xFFFFFFFF,
                0xFFFFFFFF,
                0xFFFF,
                lowBatteryPercent,
                0,
                0,
                0,
                0xFF,
                0xFF,
                1,
                0,
                com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3),
                alarmCount,
                alarmMinutes
            );
            String payloadHex = LoRaProtocolParser.bytesToHex(downlinkFrame);
            Log.i(TAG, "准备下发查询: devEUI=" + deviceIdHex + ", alarms=" + alarmCount + ", payloadHexLen=" + payloadHex.length());
            mqttClient.publishDownlinkByDevEuiTopic(
                DOWNLINK_TOPIC_BASE,
                deviceIdHex,
                payloadHex,
                DEFAULT_FPORT,
                true
            );
            Log.i(TAG, "查询下发已调用publish: devEUI=" + deviceIdHex);
        } catch (Exception e) {
            Log.e(TAG, "查询下发失败: devEUI=" + deviceIdHex + ", err=" + e.getMessage(), e);
        }
    }
    
    /**
     * 发送设备配置下行报文
     * 用于配置设备参数
     * @param deviceIdHex 设备ID
     * @param departmentId 科室ID
     * @param cartId 台车ID
     * @param reportIntervalMin 上报间隔(分钟)
     */
    public void sendConfigDownlink(String deviceIdHex, 
                                  int departmentId, 
                                  int cartId, 
                                  int reportIntervalMin) {
        try {
            int lowBatteryPercent = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
            int intervalMin = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", reportIntervalMin);
            intervalMin = Math.max(5, Math.min(1440, intervalMin));
            byte[] downlinkFrame = LoRaProtocolParser.buildDownlink8001Full(
                deviceIdHex,
                (byte) (System.currentTimeMillis() & 0xFF),
                System.currentTimeMillis(),
                0xFFFFFFFF,
                0xFFFFFFFF,
                0xFFFF,
                lowBatteryPercent,
                1,
                departmentId,
                cartId,
                0xFF,
                0xFF,
                0,
                0,
                intervalMin,
                1,
                new int[]{getGlobalAlarmMinute()}
            );
            String payloadHex = LoRaProtocolParser.bytesToHex(downlinkFrame);
            mqttClient.publishDownlinkByDevEuiTopic(
                DOWNLINK_TOPIC_BASE,
                deviceIdHex,
                payloadHex,
                DEFAULT_FPORT,
                true
            );
            Log.i(TAG, "配置下发已调用publish: devEUI=" + deviceIdHex);
        } catch (Exception e) {
            Log.e(TAG, "配置下发失败: devEUI=" + deviceIdHex + ", err=" + e.getMessage(), e);
        }
    }
    public void sendConfigDownlink(String deviceIdHex,
                                   int departmentId,
                                   int cartId,
                                   int reportIntervalMin,
                                   int alarmCount,
                                   int[] alarmMinutes) {
        try {
            int lowBatteryPercent = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
            byte[] downlinkFrame = LoRaProtocolParser.buildDownlink8001Full(
                deviceIdHex,
                (byte) (System.currentTimeMillis() & 0xFF),
                System.currentTimeMillis(),
                0xFFFFFFFF,
                0xFFFFFFFF,
                0xFFFF,
                lowBatteryPercent,
                1,
                departmentId,
                cartId,
                0xFF,
                0xFF,
                0,
                0,
                com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", reportIntervalMin),
                alarmCount,
                alarmMinutes
            );
            String payloadHex = LoRaProtocolParser.bytesToHex(downlinkFrame);
            Log.i(TAG, "准备下发配置: devEUI=" + deviceIdHex + ", dep=" + departmentId + ", cart=" + cartId + ", interval=" + reportIntervalMin + ", alarms=" + alarmCount + ", payloadHexLen=" + payloadHex.length());
            mqttClient.publishDownlinkByDevEuiTopic(
                DOWNLINK_TOPIC_BASE,
                deviceIdHex,
                payloadHex,
                DEFAULT_FPORT,
                true
            );
            Log.i(TAG, "配置下发已调用publish: devEUI=" + deviceIdHex);
        } catch (Exception e) {
            Log.e(TAG, "配置下发失败: devEUI=" + deviceIdHex + ", err=" + e.getMessage(), e);
        }
    }
    
    /**
     * 发送清除数据下行报文
     * 用于清除设备特定数据
     * @param deviceIdHex 设备ID
     * @param clearMask 清除掩码 (bit0:清除事件, bit1:清除状态, bit2:清除配置等)
     */
    public void sendClearDataDownlink(String deviceIdHex, int clearMask) {
        try {
            int lowBatteryPercent = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
            int intervalMin = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
            byte[] downlinkFrame = LoRaProtocolParser.buildDownlink8001Full(
                deviceIdHex,
                (byte) (System.currentTimeMillis() & 0xFF),
                System.currentTimeMillis(),
                0xFFFFFFFF,
                0xFFFFFFFF,
                0xFFFF,
                lowBatteryPercent,
                1,
                0,
                0,
                0xFF,
                0xFF,
                1,
                clearMask,
                intervalMin,
                1,
                new int[]{getGlobalAlarmMinute()}
            );
            String payloadHex = LoRaProtocolParser.bytesToHex(downlinkFrame);
            mqttClient.publishDownlinkByDevEuiTopic(
                DOWNLINK_TOPIC_BASE,
                deviceIdHex,
                payloadHex,
                DEFAULT_FPORT,
                true
            );
            Log.i(TAG, "清除下发已调用publish: devEUI=" + deviceIdHex);
        } catch (Exception e) {
            Log.e(TAG, "清除下发失败: devEUI=" + deviceIdHex + ", err=" + e.getMessage(), e);
        }
    }
    public void sendClearDataDownlink(String deviceIdHex, int clearMask, int alarmCount, int[] alarmMinutes) {
        try {
            int lowBatteryPercent = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
            byte[] downlinkFrame = LoRaProtocolParser.buildDownlink8001Full(
                deviceIdHex,
                (byte) (System.currentTimeMillis() & 0xFF),
                System.currentTimeMillis(),
                0xFFFFFFFF,
                0xFFFFFFFF,
                0xFFFF,
                lowBatteryPercent,
                1,
                0,
                0,
                0xFF,
                0xFF,
                1,
                clearMask,
                60,
                alarmCount,
                alarmMinutes
            );
            String payloadHex = LoRaProtocolParser.bytesToHex(downlinkFrame);
            Log.i(TAG, "准备下发清除: devEUI=" + deviceIdHex + ", clearMask=0x" + Integer.toHexString(clearMask) + ", alarms=" + alarmCount + ", payloadHexLen=" + payloadHex.length());
            mqttClient.publishDownlinkByDevEuiTopic(
                DOWNLINK_TOPIC_BASE,
                deviceIdHex,
                payloadHex,
                DEFAULT_FPORT,
                true
            );
            Log.i(TAG, "清除下发已调用publish: devEUI=" + deviceIdHex);
        } catch (Exception e) {
            Log.e(TAG, "清除下发失败: devEUI=" + deviceIdHex + ", err=" + e.getMessage(), e);
        }
    }

    public String buildDownlink8001Hex(String deviceIdHex, int clearMask, int intervalMin) {
        int lowBatteryPercent = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
        byte[] frame = LoRaProtocolParser.buildDownlink8001Full(
                deviceIdHex,
                (byte) 0x01,
                System.currentTimeMillis(),
                0xFFFFFFFF,
                0xFFFFFFFF,
                0xFFFF,
                lowBatteryPercent,
                1,
                0,
                0,
                0xFF,
                0xFF,
                0,
                clearMask,
                Math.max(5, Math.min(1440, com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", intervalMin))),
                1,
                new int[]{getGlobalAlarmMinute()});
        return LoRaProtocolParser.bytesToHex(frame);
    }

    public void sendDownlink8001Config(String deviceIdHex, int clearMask) {
        int lowBatteryPercent = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
        int intervalMin = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
        byte[] frame = LoRaProtocolParser.buildDownlink8001Full(
                deviceIdHex,
                (byte) 0x01,
                System.currentTimeMillis(),
                0xFFFFFFFF,
                0xFFFFFFFF,
                0xFFFF,
                lowBatteryPercent,
                1,
                0,
                0,
                0xFF,
                0xFF,
                0,
                clearMask,
                intervalMin,
                1,
                new int[]{getGlobalAlarmMinute()});
        String payloadHex = LoRaProtocolParser.bytesToHex(frame);
        mqttClient.publishDownlinkByDevEuiTopic(
                DOWNLINK_TOPIC_BASE,
                deviceIdHex,
                payloadHex,
                DEFAULT_FPORT,
                true);
        Log.i(TAG, "下发8001配置: devEUI=" + deviceIdHex + ", clearMask=" + clearMask + ", interval=5, payload=" + payloadHex);
    }

    public void sendRawHexDownlink(String deviceIdHex, String payloadHex) {
        if (mqttClient == null) {
            Log.e(TAG, "MQTT客户端未初始化");
            return;
        }
        if (payloadHex == null) {
            Log.e(TAG, "原始HEX为空");
            return;
        }
        String clean = payloadHex.replaceAll("\\s+", "");
        if (clean.isEmpty()) {
            Log.e(TAG, "原始HEX为空字符串");
            return;
        }
        if (clean.length() % 2 != 0) {
            Log.e(TAG, "原始HEX长度不是偶数: " + clean.length());
            return;
        }
        if (!clean.matches("(?i)[0-9a-f]+")) {
            Log.e(TAG, "原始HEX包含非法字符: " + clean);
            return;
        }
        Log.i(TAG, "准备发送原始HEX下行: devEUI=" + deviceIdHex + ", payloadHexLen=" + clean.length());
        mqttClient.publishDownlinkByDevEuiTopic(
                DOWNLINK_TOPIC_BASE,
                deviceIdHex,
                clean,
                DEFAULT_FPORT,
                true);
        Log.i(TAG, "原始HEX下行已调用publish: devEUI=" + deviceIdHex);
    }

    private int getGlobalAlarmMinute() {
        int h = SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
        int m = SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
        int minutes = h * 60 + m;
        if (minutes < 0) minutes = 0;
        if (minutes > 1440) minutes = 1440;
        return minutes;
    }

    private byte[] hexToBytesLocal(String hex) {
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
