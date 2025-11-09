package com.lora.cn.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * LoRa帧数据解析工具类
 * 帧结构：帧头(1) + 设备ID(8) + 功能码(2) + 流水号(1) + 数据长度(2) + 数据内容(n) + 校验码(1) + 帧尾(1)
 */
public class LoRaFrameParser {
    
    // 帧头和帧尾常量
    private static final byte FRAME_HEADER = (byte) 0xA5;
    private static final byte FRAME_TAIL = (byte) 0x5A;
    
    /**
     * 解析后的帧数据
     */
    public static class ParsedFrame {
        public String deviceId;           // 设备ID (8字节)
        public String functionCode;       // 功能码 (2字节)
        public int sequenceNumber;        // 流水号 (1字节)
        public int dataLength;           // 数据长度 (2字节)
        public byte[] dataContent;       // 数据内容 (n字节)
        public byte checksum;            // 校验码 (1字节)
        
        // 解析后的数据内容字段
        public Date dataTime;            // 数据产生时间 (7字节BCD)
        public long deviceEvent;         // 设备事件 (4字节)
        public long deviceStatus;        // 设备状态 (4字节)
        public int batteryVoltage;       // 电池电压 (2字节)
        public int batteryLevel;         // 电量 (1字节)
        public int rssi;                 // RSSI (1字节)
        public int departmentNumber;     // 科室或护士站编号 (1字节)
        public int cartNumber;           // 台车编号 (1字节)
        public int deviceCount;          // 放置的设备数量 (1字节)
        public int rackNumber;           // 设备所属台车台架编号 (1字节)

        // 设备事件 bit 展开
        public int evPowerLockOpen;      // Bit0: 电源开关锁-开事件 (0/1)
        public int evPowerLockClose;     // Bit1: 电源开关锁-关事件 (0/1)
        public int evPeriodicReport;     // Bit2: 定期主动上报 (0/1)
        public int evManualTake;         // Bit3: 设备主动取走信息上报 (0/1)
        public int evManualPut;          // Bit4: 设备主动放入信息上报 (0/1)
        public int evIllegalRemoval;     // Bit5: 设备非法移走报警信息上报 (0/1)
        public int evLowBattery;         // Bit6: 电池电量低于20%信息上报 (0/1)
        public int evNurseQuery;         // Bit7: 护士站查询操作指令事件 (0/1)

        // 设备状态 bit 展开
        public int stPowerLockOn;        // Bit0: 启用电源开关锁状态 1:开 0:关
        public int stLayer1NotInPlace;   // Bit1: 第1层开关状态 1:不在位 0:在位
        public int stLayer2NotInPlace;   // Bit2: 第2层开关状态 1:不在位 0:在位
        public int stLayer3NotInPlace;   // Bit3: 第3层开关状态 1:不在位 0:在位
        public int stLayer4NotInPlace;   // Bit4: 第4层开关状态 1:不在位 0:在位
        public int stLayer5NotInPlace;   // Bit5: 第5层开关状态 1:不在位 0:在位
        // 枚举集合（事件/状态）
        public java.util.EnumSet<DeviceEventFlag> eventFlags = java.util.EnumSet.noneOf(DeviceEventFlag.class);
        public java.util.EnumSet<DeviceStatusFlag> statusFlags = java.util.EnumSet.noneOf(DeviceStatusFlag.class);
        
        @Override
        public String toString() {
            return "ParsedFrame{" +
                    "deviceId='" + deviceId + '\'' +
                    ", functionCode='" + functionCode + '\'' +
                    ", sequenceNumber=" + sequenceNumber +
                    ", dataLength=" + dataLength +
                    ", batteryLevel=" + batteryLevel +
                    ", cartNumber=" + cartNumber +
                    ", departmentNumber=" + departmentNumber +
                    ", eventFlags=" + eventFlags +
                    ", statusFlags=" + statusFlags +
                    '}';
        }
    }
    
    /**
     * 解析hex字符串为LoRa帧数据
     * @param hexString hex字符串
     * @return 解析后的帧数据，解析失败返回null
     */
    public static ParsedFrame parseFrame(String hexString) {
        if (hexString == null || hexString.length() < 32) { // 最小帧长度检查
            return null;
        }
        
        try {
            // 移除空格并转换为大写
            hexString = hexString.replaceAll("\\s+", "").toUpperCase();
            
            // 转换为字节数组
            byte[] frameBytes = hexStringToByteArray(hexString);
            
            if (frameBytes.length < 16) { // 最小帧长度：1+8+2+1+2+1+1 = 16字节
                return null;
            }
            
            // 验证帧头和帧尾
            if (frameBytes[0] != FRAME_HEADER || frameBytes[frameBytes.length - 1] != FRAME_TAIL) {
                return null;
            }
            
            ParsedFrame frame = new ParsedFrame();
            int offset = 1; // 跳过帧头
            
            // 解析设备ID (8字节)
            frame.deviceId = bytesToHexString(frameBytes, offset, 8);
            offset += 8;
            
            // 解析功能码 (2字节)
            frame.functionCode = bytesToHexString(frameBytes, offset, 2);
            offset += 2;
            
            // 解析流水号 (1字节)
            frame.sequenceNumber = frameBytes[offset] & 0xFF;
            offset += 1;
            
            // 解析数据长度 (2字节，大端序)
            frame.dataLength = ((frameBytes[offset] & 0xFF) << 8) | (frameBytes[offset + 1] & 0xFF);
            offset += 2;
            
            // 验证数据长度
            if (offset + frame.dataLength + 2 > frameBytes.length) { // +2 for checksum and tail
                return null;
            }
            
            // 解析数据内容
            frame.dataContent = new byte[frame.dataLength];
            System.arraycopy(frameBytes, offset, frame.dataContent, 0, frame.dataLength);
            
            // 解析数据内容字段
            parseDataContent(frame);
            
            offset += frame.dataLength;
            
            // 解析校验码
            frame.checksum = frameBytes[offset];
            
            return frame;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 解析数据内容字段
     */
    private static void parseDataContent(ParsedFrame frame) {
        if (frame.dataContent == null || frame.dataContent.length < 24) {
            return;
        }
        
        try {
            int offset = 0;
            
            // 1. 数据产生时间 (7字节BCD码)
            frame.dataTime = parseBCDTime(frame.dataContent, offset);
            offset += 7;
            
            // 2. 设备事件 (4字节)
            frame.deviceEvent = bytesToLong(frame.dataContent, offset, 4);
            offset += 4;

            // 展开事件位
            frame.evPowerLockOpen  = ((frame.deviceEvent & 0x01L) != 0) ? 1 : 0;
            frame.evPowerLockClose = ((frame.deviceEvent & 0x02L) != 0) ? 1 : 0;
            frame.evPeriodicReport = ((frame.deviceEvent & 0x04L) != 0) ? 1 : 0;
            frame.evManualTake     = ((frame.deviceEvent & 0x08L) != 0) ? 1 : 0;
            frame.evManualPut      = ((frame.deviceEvent & 0x10L) != 0) ? 1 : 0;
            frame.evIllegalRemoval = ((frame.deviceEvent & 0x20L) != 0) ? 1 : 0;
            frame.evLowBattery     = ((frame.deviceEvent & 0x40L) != 0) ? 1 : 0;
            frame.evNurseQuery     = ((frame.deviceEvent & 0x80L) != 0) ? 1 : 0;
            // 填充事件枚举集合
            frame.eventFlags = DeviceEventFlag.fromMask(frame.deviceEvent);
            // 填充事件枚举集合
            try {
                frame.eventFlags = DeviceEventFlag.fromMask(frame.deviceEvent);
            } catch (Throwable ignored) {}
            
            // 3. 设备状态 (4字节)
            frame.deviceStatus = bytesToLong(frame.dataContent, offset, 4);
            offset += 4;

            // 展开状态位
            frame.stPowerLockOn       = ((frame.deviceStatus & 0x01L) != 0) ? 1 : 0;
            frame.stLayer1NotInPlace  = ((frame.deviceStatus & 0x02L) != 0) ? 1 : 0;
            frame.stLayer2NotInPlace  = ((frame.deviceStatus & 0x04L) != 0) ? 1 : 0;
            frame.stLayer3NotInPlace  = ((frame.deviceStatus & 0x08L) != 0) ? 1 : 0;
            frame.stLayer4NotInPlace  = ((frame.deviceStatus & 0x10L) != 0) ? 1 : 0;
            frame.stLayer5NotInPlace  = ((frame.deviceStatus & 0x20L) != 0) ? 1 : 0;
            // 填充状态枚举集合
            frame.statusFlags = DeviceStatusFlag.fromMask(frame.deviceStatus);
            // 填充状态枚举集合
            try {
                frame.statusFlags = DeviceStatusFlag.fromMask(frame.deviceStatus);
            } catch (Throwable ignored) {}
            
            // 4. 电池电压 (2字节)
            frame.batteryVoltage = ((frame.dataContent[offset] & 0xFF) << 8) | (frame.dataContent[offset + 1] & 0xFF);
            offset += 2;
            
            // 5. 电量 (1字节)
            frame.batteryLevel = frame.dataContent[offset] & 0xFF;
            offset += 1;
            
            // 6. RSSI (1字节)
            frame.rssi = frame.dataContent[offset] & 0xFF;
            offset += 1;
            
            // 7. 科室或护士站编号 (1字节)
            frame.departmentNumber = frame.dataContent[offset] & 0xFF;
            offset += 1;
            
            // 8. 台车编号 (1字节)
            frame.cartNumber = frame.dataContent[offset] & 0xFF;
            offset += 1;
            
            // 9. 放置的设备数量 (1字节)
            frame.deviceCount = frame.dataContent[offset] & 0xFF;
            offset += 1;
            
            // 10. 设备所属台车台架编号 (1字节)
            if (offset < frame.dataContent.length) {
                frame.rackNumber = frame.dataContent[offset] & 0xFF;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 设备事件枚举（bit掩码） */
    public enum DeviceEventFlag {
        POWER_LOCK_OPEN(0x01L, "电源开锁-开"),
        POWER_LOCK_CLOSE(0x02L, "电源开锁-关"),
        PERIODIC_REPORT(0x04L, "定期主动上报"),
        MANUAL_TAKE(0x08L, "主动取走"),
        MANUAL_PUT(0x10L, "主动放入"),
        ILLEGAL_REMOVAL(0x20L, "非法移走报警"),
        LOW_BATTERY(0x40L, "低电量<20%"),
        NURSE_QUERY(0x80L, "护士站查询");

        public final long mask;
        private final String label;
        DeviceEventFlag(long mask, String label) { this.mask = mask; this.label = label; }
        public String label() { return label; }
        public static java.util.EnumSet<DeviceEventFlag> fromMask(long bits) {
            java.util.EnumSet<DeviceEventFlag> set = java.util.EnumSet.noneOf(DeviceEventFlag.class);
            for (DeviceEventFlag f : values()) {
                if ((bits & f.mask) != 0) set.add(f);
            }
            return set;
        }
    }

    /** 设备状态枚举（bit掩码） */
    public enum DeviceStatusFlag {
        POWER_LOCK_ON(0x01L, "电源锁:开"),
        LAYER1_NOT_IN_PLACE(0x02L, "层1:不在位"),
        LAYER2_NOT_IN_PLACE(0x04L, "层2:不在位"),
        LAYER3_NOT_IN_PLACE(0x08L, "层3:不在位"),
        LAYER4_NOT_IN_PLACE(0x10L, "层4:不在位"),
        LAYER5_NOT_IN_PLACE(0x20L, "层5:不在位");

        public final long mask;
        private final String label;
        DeviceStatusFlag(long mask, String label) { this.mask = mask; this.label = label; }
        public String label() { return label; }
        public static java.util.EnumSet<DeviceStatusFlag> fromMask(long bits) {
            java.util.EnumSet<DeviceStatusFlag> set = java.util.EnumSet.noneOf(DeviceStatusFlag.class);
            for (DeviceStatusFlag f : values()) {
                if ((bits & f.mask) != 0) set.add(f);
            }
            return set;
        }
    }
    
    /**
     * 解析BCD时间 (YYYYMMDDhhmmss)
     */
    private static Date parseBCDTime(byte[] data, int offset) {
        try {
            StringBuilder timeStr = new StringBuilder();
            for (int i = 0; i < 7; i++) {
                int bcd = data[offset + i] & 0xFF;
                timeStr.append(String.format("%02d", bcd));
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
            return sdf.parse(timeStr.toString());
        } catch (Exception e) {
            return new Date();
        }
    }
    
    /**
     * 字节数组转长整型
     */
    private static long bytesToLong(byte[] data, int offset, int length) {
        long result = 0;
        for (int i = 0; i < length; i++) {
            result = (result << 8) | (data[offset + i] & 0xFF);
        }
        return result;
    }
    
    /**
     * hex字符串转字节数组
     */
    private static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
    
    /**
     * 字节数组转hex字符串
     */
    private static String bytesToHexString(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + length && i < bytes.length; i++) {
            sb.append(String.format("%02X", bytes[i] & 0xFF));
        }
        return sb.toString();
    }
    
    /**
     * 检查设备事件是否有异常
     */
    public static boolean hasDeviceAlert(long deviceEvent) {
        // 检查Bit5: 设备非法移走报警信息上报
        // 检查Bit6: 电池电量低于20%信息上报
        return (deviceEvent & 0x20) != 0 || (deviceEvent & 0x40) != 0;
    }
    
    /**
     * 获取设备事件描述
     */
    public static String getDeviceEventDescription(long deviceEvent) {
        StringBuilder desc = new StringBuilder();
        
        if ((deviceEvent & 0x01) != 0) desc.append("电源开关锁-开事件; ");
        if ((deviceEvent & 0x02) != 0) desc.append("电源开关锁-关事件; ");
        if ((deviceEvent & 0x04) != 0) desc.append("定期主动上报; ");
        if ((deviceEvent & 0x08) != 0) desc.append("设备主动取走信息上报; ");
        if ((deviceEvent & 0x10) != 0) desc.append("设备主动放入信息上报; ");
        if ((deviceEvent & 0x20) != 0) desc.append("设备非法移走报警; ");
        if ((deviceEvent & 0x40) != 0) desc.append("电池电量低于20%; ");
        if ((deviceEvent & 0x80) != 0) desc.append("护士站查询操作指令事件; ");
        
        return desc.length() > 0 ? desc.toString() : "正常";
    }
}