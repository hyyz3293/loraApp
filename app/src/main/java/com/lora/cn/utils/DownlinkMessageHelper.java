package com.lora.cn.utils;

import android.util.Log;
import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.network.MqttPacketsClient;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 下行报文调用辅助类
 * 根据下行报文规则编写调用函数
 */
public class DownlinkMessageHelper {
    
    private static final String TAG = "DownlinkMessageHelper";

    public static int getLowBatteryThresholdPercent() {
        int p = 20;
        try { p = SPUtils.getInstance().getInt("low_battery_threshold_percent", 20); } catch (Exception ignored) {}
        if (p < 0) p = 0;
        if (p > 100) p = 100;
        return p;
    }

    public static int getLowBatteryThresholdVoltageCentiVolt() {
        int p = getLowBatteryThresholdPercent();
        int delta = Math.round(0.6f * p);
        return 300 + delta;
    }

    public static boolean isLowBatteryByVoltageCentiVolt(int batteryVoltageCentiVolt) {
        if (batteryVoltageCentiVolt <= 0) return false;
        return batteryVoltageCentiVolt <= getLowBatteryThresholdVoltageCentiVolt();
    }

    public static boolean isLowBattery(int batteryVoltageCentiVolt, int batteryLevelPercent) {
        return isLowBatteryByVoltageCentiVolt(batteryVoltageCentiVolt);
    }
    
    // MQTT客户端实例
    private MqttPacketsClient mqttClient;
    
    // 下行主题配置
    private static final String DOWNLINK_TOPIC_BASE = "/milesight/downlink";
    private static final int DEFAULT_FPORT = 85;
    private static final Map<String, Long> lastNeed8001LogMsByDev = new ConcurrentHashMap<>();
    
    public DownlinkMessageHelper(MqttPacketsClient mqttClient) {
        this.mqttClient = mqttClient;
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
            int lowBatteryPercent = getLowBatteryThresholdPercent();
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
                Math.max(3, Math.min(1440, reportIntervalMin)),
                alarmCount,
                alarmMinutes
            );
            String payloadHex = LoRaProtocolParser.bytesToHex(downlinkFrame);
            String payloadClean = payloadHex.replaceAll("\\s+", "");
            String base64 = android.util.Base64.encodeToString(hexToBytesLocal(payloadClean), android.util.Base64.NO_WRAP);
            String utcText = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date(utcMs));
            String ackText = ((ackResult & 0x01) == 1) ? "收到数据" : "不正确重发";
            String opText;
            if (queryOp == 0x00) {
                opText = "无操作";
            } else if (queryOp == 0x01) {
                opText = "控制设备指令";
            } else {
                opText = "未知(" + queryOp + ")";
            }
            boolean pBit0 = (clearMask & (1 << 0)) != 0;
            boolean pBit1 = (clearMask & (1 << 1)) != 0;
            boolean pBit2 = (clearMask & (1 << 2)) != 0;
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
                    ", 应答结果含义=" + ackText +
                    ", 护士站操作指令=" + queryOp +
                    ", 护士站操作指令含义=" + opText +
                    ", 科室ID=" + departmentId +
                    ", 台车ID=" + cartId +
                    ", 保留9(1B)=" + String.format(java.util.Locale.US, "0x%02X", 0xFF) +
                    ", 保留10(1B)=" + String.format(java.util.Locale.US, "0x%02X", 0xFF) +
                    ", 护士站操作指令参数=" + String.format(java.util.Locale.US, "0x%08X", clearMask) +
                    ", 设备非法移走告警清除=" + (pBit0 ? "是" : "否") +
                    ", 开启定时维护=" + (pBit1 ? "是" : "否") +
                    ", 终端主动维护标识清除=" + (pBit2 ? "是" : "否") +
                    ", 上报间隔(分钟)=" + Math.max(3, Math.min(1440, reportIntervalMin)) +
                    ", 闹钟数量=" + alarmCount +
                    ", 闹钟分钟列表=" + java.util.Arrays.toString(alarmMinutes) +
                    ", 端口=" + DEFAULT_FPORT +
                    ", 需确认=" + confirmed +
                    ", HEX长度=" + payloadHex.length() +
                    ", Base64长度=" + base64.length() +
                    ", HEX串=" + payloadHex);
            StringBuilder desc = new StringBuilder();
            desc.append("下行8001字段描述:\n");
            desc.append("1 数据产生时间(本地)=").append(utcText).append("\n");
            desc.append("2 保留(4B)=").append(String.format(java.util.Locale.US, "0x%08X", 0xFFFFFFFF)).append("\n");
            desc.append("3 保留(4B)=").append(String.format(java.util.Locale.US, "0x%08X", 0xFFFFFFFF)).append("\n");
            desc.append("4 保留(2B)=").append(String.format(java.util.Locale.US, "0x%04X", 0xFFFF)).append("\n");
            desc.append("5 设置低电量报警预值(%)=").append(lowBatteryPercent).append("\n");
            desc.append("6 应答结果=").append(ackResult).append(" (").append(ackText).append(")").append("\n");
            desc.append("7 设置所属的科室或护士站编号=").append(departmentId).append("\n");
            desc.append("8 设置台车编号=").append(cartId).append("\n");
            desc.append("9 保留(1B)=").append(String.format(java.util.Locale.US, "0x%02X", 0xFF)).append("\n");
            desc.append("10 保留(1B)=").append(String.format(java.util.Locale.US, "0x%02X", 0xFF)).append("\n");
            desc.append("11 护士站操作指令=").append(String.format(java.util.Locale.US, "0x%02X", queryOp)).append(" (").append(opText).append(")").append("\n");
            desc.append("12 护士站操作指令参数=").append(String.format(java.util.Locale.US, "0x%08X", clearMask))
                    .append(" [Bit0清异常取走=").append(pBit0 ? "1" : "0")
                    .append(", Bit1开启定时维护=").append(pBit1 ? "1" : "0")
                    .append(", Bit2清主动维护标识=").append(pBit2 ? "1" : "0").append("]").append("\n");
            desc.append("13 设置休眠间隔(分钟)=").append(Math.max(3, Math.min(1440, reportIntervalMin))).append("\n");
            desc.append("14 设置闹钟数量=").append(alarmCount).append("\n");
            desc.append("15 设置闹钟时刻点列表(分钟)=").append(java.util.Arrays.toString(alarmMinutes));
            Log.i(TAG, desc.toString());
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

//    public void evaluateAndSend8001IfNeeded(com.lora.cn.utils.LoRaFrameParser.ParsedFrame frame,
//                                            com.lora.cn.database.DatabaseHelper db) {
//        try {
//            if (frame == null || frame.deviceId == null || frame.deviceId.isEmpty()) return;
//            if (!isNeedDownlink8001(frame)) return;
//            sendDownlink8001WithCurrentConfig(frame, db);
//        } catch (Exception ignored) {}
//    }

    public boolean isNeedDownlink8001(com.lora.cn.utils.LoRaFrameParser.ParsedFrame frame) {
        try {
            if (frame == null) return false;
            int intervalMin = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
            int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
            int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
            int mins = Math.max(0, Math.min(1440, h * 60 + m));
            int normalizedInterval = Math.max(3, Math.min(1440, intervalMin));
            int lowBattery = getLowBatteryThresholdPercent();
            boolean debug = com.blankj.utilcode.util.SPUtils.getInstance().getBoolean("debug_need_downlink_8001", false);
            Need8001Decision d = evaluateNeed8001(frame, normalizedInterval, mins);
            if (shouldLogNeed8001(frame.deviceId, debug, d.need)) {
                Log.e(TAG,
                        "isNeedDownlink8001 dev=" + (frame.deviceId == null ? "" : frame.deviceId) +
                                " need=" + d.need +
                                " interval=" + frame.sleepIntervalMin + " desiredInterval=" + normalizedInterval +
                                " intervalKnown=" + d.intervalKnown + " intervalMatch=" + d.intervalMatch +
                                " alarmCount=" + frame.alarmCount +
                                " alarmMinutes=" + (frame.alarmMinutes == null ? "null" : Arrays.toString(frame.alarmMinutes)) +
                                " desiredMins=" + mins + "(h=" + h + ",m=" + m + ")" +
                                " scheduleKnown=" + d.scheduleKnown + " scheduleMatch=" + d.scheduleMatch +
                                " lowBatteryTh=" + lowBattery +
                                " evLowBattery=" + frame.evLowBattery +
                                " batteryLevel=" + frame.batteryLevel);
            }
            return d.need;
        } catch (Exception ignored) {
            return false;
        }
    }

//    public void sendDownlink8001WithCurrentConfig(com.lora.cn.utils.LoRaFrameParser.ParsedFrame frame,
//                                                 com.lora.cn.database.DatabaseHelper db) {
//        try {
//            if (frame == null || frame.deviceId == null || frame.deviceId.isEmpty()) return;
//            int depId = 0;
//            int cartId = 0;
//            try {
//                java.util.List<com.lora.cn.ui.model.Terminal> terms = db != null ? db.getAllTerminals() : null;
//                if (terms != null) {
//                    for (com.lora.cn.ui.model.Terminal t : terms) {
//                        if (t != null && frame.deviceId.equalsIgnoreCase(t.getTerminalId())) {
//                            depId = (int) Math.max(0, Math.min(255, t.getDepartmentId()));
//                            cartId = (int) Math.max(0, Math.min(255, t.getRoomId()));
//                            break;
//                        }
//                    }
//                }
//            } catch (Exception ignored) {}
//            int intervalMin = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
//            int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
//            int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
//            int mins = Math.max(0, Math.min(1440, h * 60 + m));
//            int normalizedInterval = Math.max(3, Math.min(1440, intervalMin));
//            sendDownlink8001(frame.deviceId, 1, 0, depId, cartId, 0, 0, normalizedInterval, 1, new int[]{mins}, true);
//        } catch (Exception ignored) {}
//    }

    private static Need8001Decision evaluateNeed8001(LoRaFrameParser.ParsedFrame frame, int normalizedInterval, int mins) {
        boolean intervalKnown = frame.sleepIntervalMin > 0;
        boolean intervalMatch = intervalKnown && (frame.sleepIntervalMin == normalizedInterval);
        boolean scheduleKnown = frame.alarmCount >= 0;
        boolean scheduleMatch = false;
        if (scheduleKnown) {
            if (frame.alarmCount == 1 && frame.alarmMinutes != null && frame.alarmMinutes.length >= 1) {
                scheduleMatch = (frame.alarmMinutes[0] == mins);
            } else if (frame.alarmCount == 0 && mins <= 0) {
                scheduleMatch = true;
            }
        }
        boolean need = !(intervalMatch && scheduleMatch);
        return new Need8001Decision(need, intervalKnown, intervalMatch, scheduleKnown, scheduleMatch);
    }

    private static boolean shouldLogNeed8001(String deviceId, boolean debug, boolean need) {
        if (!debug && !need) return false;
        String dev = deviceId == null ? "" : deviceId.trim();
        long now = System.currentTimeMillis();
        Long last = lastNeed8001LogMsByDev.get(dev);
        long minGap = debug ? 1500L : 8000L;
        if (last != null && now - last < minGap) return false;
        lastNeed8001LogMsByDev.put(dev, now);
        return true;
    }

    private static final class Need8001Decision {
        final boolean need;
        final boolean intervalKnown;
        final boolean intervalMatch;
        final boolean scheduleKnown;
        final boolean scheduleMatch;

        Need8001Decision(boolean need, boolean intervalKnown, boolean intervalMatch, boolean scheduleKnown, boolean scheduleMatch) {
            this.need = need;
            this.intervalKnown = intervalKnown;
            this.intervalMatch = intervalMatch;
            this.scheduleKnown = scheduleKnown;
            this.scheduleMatch = scheduleMatch;
        }
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
