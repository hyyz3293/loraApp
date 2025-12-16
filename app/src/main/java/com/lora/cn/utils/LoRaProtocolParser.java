package com.lora.cn.utils;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * LoRa协议解析工具类
 * 基于lora.docx文档中的协议规范实现
 */
public class LoRaProtocolParser {
    
    private static final String TAG = "LoRaProtocolParser";
    
    // LoRa协议常量
    public static final byte FRAME_HEADER = (byte) 0xAA;  // 帧头
    public static final byte FRAME_TAIL = (byte) 0x55;    // 帧尾
    
    // 命令类型
    public static final byte CMD_SEARCH_TERMINAL = 0x01;   // 搜索终端
    public static final byte CMD_ADD_TERMINAL = 0x02;      // 添加终端
    public static final byte CMD_GET_TERMINAL_INFO = 0x03; // 获取终端信息
    public static final byte CMD_SET_TERMINAL_CONFIG = 0x04; // 设置终端配置
    
    // 响应类型
    public static final byte RESP_TERMINAL_FOUND = (byte) 0x81;   // 发现终端
    public static final byte RESP_TERMINAL_ADDED = (byte) 0x82;   // 终端已添加
    public static final byte RESP_TERMINAL_INFO = (byte) 0x83;    // 终端信息
    public static final byte RESP_CONFIG_SUCCESS = (byte) 0x84;   // 配置成功
    
    /**
     * LoRa数据帧结构
     */
    public static class LoRaFrame {
        public byte header;        // 帧头 0xAA
        public byte length;        // 数据长度
        public byte command;       // 命令字
        public byte[] data;        // 数据内容
        public byte checksum;      // 校验和
        public byte tail;          // 帧尾 0x55
        
        public LoRaFrame() {
            this.header = FRAME_HEADER;
            this.tail = FRAME_TAIL;
        }
    }
    
    /**
     * 终端设备信息
     */
    public static class TerminalInfo {
        public String deviceId;      // 设备ID
        public String deviceName;    // 设备名称
        public String department;    // 所属科室
        public String location;      // 位置信息
        public int signalStrength;   // 信号强度
        public int batteryLevel;     // 电池电量
        public int status;           // 设备状态 (0:离线, 1:在线, 2:异常)
        public long timestamp;       // 时间戳
        public String payloadHex;    // Payload十六进制数据
        
        public TerminalInfo() {
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * 创建搜索终端命令帧
     */
    public static byte[] createSearchTerminalFrame() {
        LoRaFrame frame = new LoRaFrame();
        frame.command = CMD_SEARCH_TERMINAL;
        frame.data = new byte[0]; // 搜索命令无需数据
        frame.length = (byte) (1 + frame.data.length); // 命令字 + 数据长度
        
        return buildFrame(frame);
    }
    
    /**
     * 创建添加终端命令帧
     */
    public static byte[] createAddTerminalFrame(String deviceId) {
        LoRaFrame frame = new LoRaFrame();
        frame.command = CMD_ADD_TERMINAL;
        frame.data = deviceId.getBytes();
        frame.length = (byte) (1 + frame.data.length);
        
        return buildFrame(frame);
    }
    
    /**
     * 创建获取终端信息命令帧
     */
    public static byte[] createGetTerminalInfoFrame(String deviceId) {
        LoRaFrame frame = new LoRaFrame();
        frame.command = CMD_GET_TERMINAL_INFO;
        frame.data = deviceId.getBytes();
        frame.length = (byte) (1 + frame.data.length);
        
        return buildFrame(frame);
    }
    
    /**
     * 构建完整的数据帧
     */
    private static byte[] buildFrame(LoRaFrame frame) {
        int totalLength = 4 + frame.data.length; // 帧头 + 长度 + 命令 + 数据 + 校验 + 帧尾
        byte[] frameBytes = new byte[totalLength];
        
        int index = 0;
        frameBytes[index++] = frame.header;
        frameBytes[index++] = frame.length;
        frameBytes[index++] = frame.command;
        
        // 复制数据
        if (frame.data.length > 0) {
            System.arraycopy(frame.data, 0, frameBytes, index, frame.data.length);
            index += frame.data.length;
        }
        
        // 计算校验和
        frame.checksum = calculateChecksum(frameBytes, 1, index - 1);
        frameBytes[index++] = frame.checksum;
        frameBytes[index] = frame.tail;
        
        return frameBytes;
    }
    
    /**
     * 解析接收到的LoRa数据帧
     */
    public static LoRaFrame parseFrame(byte[] data) {
        if (data == null || data.length < 5) {
            Log.e(TAG, "Invalid frame data: too short");
            return null;
        }
        
        // 检查帧头和帧尾
        if (data[0] != FRAME_HEADER || data[data.length - 1] != FRAME_TAIL) {
            Log.e(TAG, "Invalid frame header or tail");
            return null;
        }
        
        LoRaFrame frame = new LoRaFrame();
        frame.header = data[0];
        frame.length = data[1];
        frame.command = data[2];
        
        // 提取数据部分
        int dataLength = frame.length - 1; // 减去命令字长度
        if (dataLength > 0) {
            frame.data = new byte[dataLength];
            System.arraycopy(data, 3, frame.data, 0, dataLength);
        } else {
            frame.data = new byte[0];
        }
        
        frame.checksum = data[data.length - 2];
        frame.tail = data[data.length - 1];
        
        // 验证校验和
        byte calculatedChecksum = calculateChecksum(data, 1, data.length - 2);
        if (frame.checksum != calculatedChecksum) {
            Log.e(TAG, "Checksum mismatch");
            return null;
        }
        
        return frame;
    }
    
    /**
     * 解析终端信息响应
     */
    public static TerminalInfo parseTerminalInfo(LoRaFrame frame) {
        if (frame == null || frame.command != RESP_TERMINAL_INFO) {
            return null;
        }
        
        try {
            String dataStr = new String(frame.data);
            String[] parts = dataStr.split(",");
            
            if (parts.length >= 6) {
                TerminalInfo info = new TerminalInfo();
                info.deviceId = parts[0];
                info.deviceName = parts[1];
                info.department = parts[2];
                info.location = parts[3];
                info.signalStrength = Integer.parseInt(parts[4]);
                info.batteryLevel = Integer.parseInt(parts[5]);
                info.status = parts.length > 6 ? Integer.parseInt(parts[6]) : 1;
                
                return info;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing terminal info", e);
        }
        
        return null;
    }
    
    /**
     * 解析搜索到的终端列表
     */
    public static List<TerminalInfo> parseSearchResponse(LoRaFrame frame) {
        List<TerminalInfo> terminals = new ArrayList<>();
        
        if (frame == null || frame.command != RESP_TERMINAL_FOUND) {
            return terminals;
        }
        
        try {
            String dataStr = new String(frame.data);
            String[] terminalEntries = dataStr.split(";");
            
            for (String entry : terminalEntries) {
                if (!entry.trim().isEmpty()) {
                    String[] parts = entry.split(",");
                    if (parts.length >= 4) {
                        TerminalInfo info = new TerminalInfo();
                        info.deviceId = parts[0];
                        info.deviceName = parts[1];
                        info.signalStrength = Integer.parseInt(parts[2]);
                        info.batteryLevel = Integer.parseInt(parts[3]);
                        info.status = 1; // 搜索到的设备默认为在线状态
                        
                        terminals.add(info);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing search response", e);
        }
        
        return terminals;
    }
    
    /**
     * 计算校验和
     */
    private static byte calculateChecksum(byte[] data, int start, int end) {
        byte checksum = 0;
        for (int i = start; i < end; i++) {
            checksum ^= data[i];
        }
        return checksum;
    }
    
    /**
     * 将字节数组转换为十六进制字符串（用于调试）
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    public static byte[] buildDownlink8001(String deviceIdHex,
                                           byte sequence,
                                           long utcMs,
                                           int ackResult,
                                           int queryOp,
                                           int departmentId,
                                           int cartId,
                                           int registerResult,
                                           int clearMask,
                                           int reportIntervalMin,
                                           int alarmCount,
                                           int[] alarmMinutes) {
        byte[] deviceId = hexToBytes(deviceIdHex);
        if (deviceId == null) deviceId = new byte[8];
        if (deviceId.length != 8) {
            byte[] fixed = new byte[8];
            int copy = Math.min(deviceId.length, 8);
            System.arraycopy(deviceId, 0, fixed, 0, copy);
            deviceId = fixed;
        }
        byte[] func = new byte[]{(byte) 0x80, 0x01};
        byte[] time = buildBcdTimeBytes(utcMs);
        byte ack = (byte) (ackResult & 0x01);
        byte qop = (byte) (queryOp & 0xFF);
        byte dep = (byte) (departmentId & 0xFF);
        byte cart = (byte) (cartId & 0xFF);
        byte reg = (byte) (registerResult & 0xFF);
        byte[] clear = new byte[]{
                (byte) ((clearMask >> 24) & 0xFF),
                (byte) ((clearMask >> 16) & 0xFF),
                (byte) ((clearMask >> 8) & 0xFF),
                (byte) (clearMask & 0xFF)
        };
        int interval = Math.max(5, Math.min(1440, reportIntervalMin));
        byte[] intervalBytes = new byte[]{(byte) ((interval >> 8) & 0xFF), (byte) (interval & 0xFF)};
        int ac = Math.max(0, Math.min(2, alarmCount));
        int listLen = 0;
        if (ac > 0 && alarmMinutes != null) {
            listLen = Math.min(ac, alarmMinutes.length) * 2;
        } else {
            ac = 0;
            listLen = 0;
        }
        int len = 7 + 1 + 1 + 1 + 1 + 1 + 4 + 2 + 1 + listLen;
        int totalLen = 1 + deviceId.length + func.length + 1 + 2 + len + 1 + 1;
        byte[] frame = new byte[totalLen];
        int idx = 0;
        frame[idx++] = (byte) 0xA5;
        System.arraycopy(deviceId, 0, frame, idx, deviceId.length); idx += deviceId.length;
        System.arraycopy(func, 0, frame, idx, func.length); idx += func.length;
        frame[idx++] = sequence;
        frame[idx++] = (byte) ((len >> 8) & 0xFF);
        frame[idx++] = (byte) (len & 0xFF);
        System.arraycopy(time, 0, frame, idx, time.length); idx += time.length;
        frame[idx++] = ack;
        frame[idx++] = qop;
        frame[idx++] = dep;
        frame[idx++] = cart;
        frame[idx++] = reg;
        System.arraycopy(clear, 0, frame, idx, clear.length); idx += clear.length;
        System.arraycopy(intervalBytes, 0, frame, idx, intervalBytes.length); idx += intervalBytes.length;
        frame[idx++] = (byte) (ac & 0xFF);
        for (int i = 0; i < ac && alarmMinutes != null && i < alarmMinutes.length; i++) {
            int m = Math.max(0, Math.min(1440, alarmMinutes[i]));
            frame[idx++] = (byte) ((m >> 8) & 0xFF);
            frame[idx++] = (byte) (m & 0xFF);
        }
        byte xor = 0;
        for (int i = 1; i < idx; i++) xor ^= frame[i];
        frame[idx++] = xor;
        frame[idx++] = (byte) 0x5A;
        return frame;
    }

    public static byte[] buildDownlink8001Simple(String deviceIdHex) {
        long utc = System.currentTimeMillis();
        return buildDownlink8001(deviceIdHex, (byte) 0x01, utc, 1, 0, 0, 0, 0, 0, 60, 0, null);
    }

    private static byte[] buildBcdTimeBytes(long utcMs) {
        java.util.TimeZone tz = java.util.TimeZone.getTimeZone("UTC");
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault());
        sdf.setTimeZone(tz);
        String s = sdf.format(new java.util.Date(utcMs));
        byte[] out = new byte[7];
        for (int i = 0; i < 7; i++) {
            int hi = Character.digit(s.charAt(i * 2), 10);
            int lo = Character.digit(s.charAt(i * 2 + 1), 10);
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static byte[] hexToBytes(String hex) {
        if (hex == null) return null;
        String h = hex.replaceAll("[^0-9A-Fa-f]", "");
        if (h.length() % 2 != 0) h = "0" + h;
        int len = h.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            int v = Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
            out[i] = (byte) v;
        }
        return out;
    }
    
    /**
     * 检查帧是否有效
     */
    public static boolean isValidFrame(byte[] data) {
        return parseFrame(data) != null;
    }

    

    /** HEX字符串转字节数组 */
    private static byte[] hexStringToByteArray(String hex) {
        hex = hex.replaceAll("\\s+", "");
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
