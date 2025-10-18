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

    // 网关帧头/尾（A5/5A）支持
    public static final byte GW_FRAME_HEADER = (byte) 0xA5;  // 网关帧头
    public static final byte GW_FRAME_TAIL = (byte) 0x5A;    // 网关帧尾
    
    // 命令类型
    public static final byte CMD_SEARCH_TERMINAL = 0x01;   // 搜索终端
    public static final byte CMD_ADD_TERMINAL = 0x02;      // 添加终端
    public static final byte CMD_GET_TERMINAL_INFO = 0x03; // 获取终端信息
    public static final byte CMD_SET_TERMINAL_CONFIG = 0x04; // 设置终端配置
    
    // 响应命令
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
        public String payloadHex;    // 原始payload(hex)，用于详情展示
        
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
    
    /**
     * 检查帧是否有效
     */
    public static boolean isValidFrame(byte[] data) {
        return parseFrame(data) != null;
    }

    /**
     * 解析网关上报的 0x0001 事件状态帧的payload(hex)
     * 说明：根据提供的协议字段进行解析，若字段不足或不匹配则尽量容错。
     */
    public static TerminalInfo parseGatewayEvent0001(String hex) {
        if (hex == null || hex.length() < 2) return null;
        try {
            byte[] bytes = hexStringToBytes(hex);
            if (bytes == null || bytes.length < 16) return null;

            // 校验A5/5A头尾（尽量容错：若不匹配也尝试解析）
            int start = 0;
            int end = bytes.length - 1;
            if (bytes[0] == GW_FRAME_HEADER) start = 1; // 跳过A5
            if (bytes[bytes.length - 1] == GW_FRAME_TAIL) end = bytes.length - 2; // 去掉5A

            // 粗略解析：跳过头部到DATA区域，按协议字段取值
            // 由于完整头部结构（设备ID、功能码、流水号、长度等）未完全确定，这里采用启发式：
            // 尝试在数据段开头解析7字节BCD时间，随后依次解析事件(4B)、状态(4B)、电压(2B)、电量(1B)、RSSI(1B)、科室(1B)、台车(1B)、数量(1B)、台架(1B)、应答(1B)

            int idx = start;
            // 可能存在前置头部（设备ID等），尝试定位BCD时间：BCD时间应满足每字节高/低半字节均<=9
            int bcdStart = findBcdTimeStart(bytes, idx, end);
            if (bcdStart < 0) bcdStart = idx; // 找不到则从起始尝试
            idx = bcdStart;

            if (idx + 7 > end) return null;
            long timestamp = bcd7ToUtcMillis(bytes, idx);
            idx += 7;

            // 事件 4字节
            if (idx + 4 > end) return null;
            int events = ((bytes[idx] & 0xFF) << 24) | ((bytes[idx + 1] & 0xFF) << 16) |
                         ((bytes[idx + 2] & 0xFF) << 8) | (bytes[idx + 3] & 0xFF);
            idx += 4;

            // 设备状态 4字节
            if (idx + 4 > end) return null;
            int statusBits = ((bytes[idx] & 0xFF) << 24) | ((bytes[idx + 1] & 0xFF) << 16) |
                             ((bytes[idx + 2] & 0xFF) << 8) | (bytes[idx + 3] & 0xFF);
            idx += 4;

            // 电池电压 2字节（系数0.01V）
            if (idx + 2 > end) return null;
            int voltageRaw = ((bytes[idx] & 0xFF) << 8) | (bytes[idx + 1] & 0xFF);
            float voltage = voltageRaw * 0.01f;
            idx += 2;

            // 电量 1字节
            if (idx + 1 > end) return null;
            int batteryLevel = bytes[idx] & 0xFF;
            idx += 1;

            // RSSI 1字节（138~0 -> -138..0）
            if (idx + 1 > end) return null;
            int rssiRaw = bytes[idx] & 0xFF;
            int rssiDbm = -rssiRaw;
            idx += 1;

            // 科室编号 1字节
            int departmentId = (idx + 1 <= end) ? (bytes[idx] & 0xFF) : 0;
            idx += 1;

            // 台车编号 1字节
            int cartId = (idx + 1 <= end) ? (bytes[idx] & 0xFF) : 0;
            idx += 1;

            // 设备数量 1字节
            int deviceCount = (idx + 1 <= end) ? (bytes[idx] & 0xFF) : 0;
            idx += 1;

            // 台架编号 1字节（0~7）
            int rackIndex = (idx + 1 <= end) ? (bytes[idx] & 0xFF) : 0;
            idx += 1;

            // 应答护士站查询操作指令 1字节（0x00~0x03）
            int ack = (idx + 1 <= end) ? (bytes[idx] & 0xFF) : 0;

            TerminalInfo info = new TerminalInfo();
            info.timestamp = timestamp;
            info.signalStrength = rssiDbm;
            info.batteryLevel = batteryLevel;
            info.status = 1; // 网关上报视为在线
            info.deviceName = "设备" + (cartId == 0 ? "" : ("-" + cartId));
            info.department = departmentId == 0 ? "" : ("科室-" + departmentId);
            info.location = rackIndex == 0 ? "" : ("台架-" + rackIndex);
            info.deviceId = ""; // 设备ID若未能确定，留空或由上层补全
            info.payloadHex = hex;

            return info;
        } catch (Exception e) {
            Log.e(TAG, "parseGatewayEvent0001 error", e);
            return null;
        }
    }

    private static int findBcdTimeStart(byte[] bytes, int start, int end) {
        for (int i = start; i + 7 <= end; i++) {
            boolean ok = true;
            for (int j = 0; j < 7; j++) {
                int b = bytes[i + j] & 0xFF;
                int hi = (b >> 4) & 0xF;
                int lo = b & 0xF;
                if (hi > 9 || lo > 9) { ok = false; break; }
            }
            if (ok) return i;
        }
        return -1;
    }

    private static long bcd7ToUtcMillis(byte[] bytes, int idx) {
        int year = 2000 + bcdByte(bytes[idx]);
        int month = bcdByte(bytes[idx + 1]);
        int day = bcdByte(bytes[idx + 2]);
        int hour = bcdByte(bytes[idx + 3]);
        int minute = bcdByte(bytes[idx + 4]);
        int second = bcdByte(bytes[idx + 5]);
        // idx+6 预留/毫秒等，这里忽略
        java.util.Calendar cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        cal.set(year, Math.max(0, month - 1), day, hour, minute, second);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static int bcdByte(byte b) {
        int v = b & 0xFF;
        return ((v >> 4) & 0xF) * 10 + (v & 0xF);
    }

    private static byte[] hexStringToBytes(String s) {
        if (s == null) return null;
        int len = s.length();
        if ((len & 1) == 1) return null;
        byte[] out = new byte[len / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(s.charAt(i * 2), 16);
            int lo = Character.digit(s.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) return null;
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
    // ---------------- 网关A5/5A帧解析与下行构建扩展 ----------------
    /**
     * 网关完整帧结构(A5/5A)
     */
    public static class GatewayFrame {
        public byte header;            // 0xA5
        public String deviceIdHex;     // 8字节DEVEUI(HEX)
        public int functionCode;       // 2字节(大端)，Bit15为方向位
        public byte seq;               // 1字节流水号
        public int dataLen;            // 2字节(大端)
        public byte[] data;            // n字节DATA
        public byte xor8;              // 1字节异或校验
        public byte tail;              // 0x5A
    }

    /**
     * 解析网关A5/5A完整帧(HEX字符串)。
     * 严格校验帧头/尾与xor8；返回GatewayFrame。若失败返回null。
     */
    public static GatewayFrame parseGatewayFrameHex(String hex) {
        if (hex == null || hex.length() < 2) return null;
        byte[] all = hexStringToBytes(hex);
        if (all == null || all.length < 1 + 8 + 2 + 1 + 2 + 1 + 1) return null;

        // 寻找帧头A5
        int h = -1;
        for (int i = 0; i < all.length; i++) {
            if ((all[i] & 0xFF) == 0xA5) { h = i; break; }
        }
        if (h < 0) return null;

        // 逐字段解析
        int p = h + 1; // 设备ID开始
        if (p + 8 + 2 + 1 + 2 > all.length - 2) return null;

        byte[] devId = new byte[8];
        System.arraycopy(all, p, devId, 0, 8);
        String devHex = bytesToHex(devId);
        p += 8;

        int func = ((all[p] & 0xFF) << 8) | (all[p + 1] & 0xFF);
        p += 2;

        byte seq = all[p++];

        int dataLen = ((all[p] & 0xFF) << 8) | (all[p + 1] & 0xFF);
        p += 2;

        if (p + dataLen + 2 > all.length) return null; // 至少还有xor与tail

        byte[] data = new byte[dataLen];
        if (dataLen > 0) System.arraycopy(all, p, data, 0, dataLen);
        p += dataLen;

        byte xor = all[p++];
        byte tail = all[p++];
        if ((tail & 0xFF) != 0x5A) return null;

        // 校验xor8：从设备ID开始到效验码字段前依次异或
        byte calc1 = 0;
        for (int i = h + 1; i < (p - 1); i++) { // 不含xor本身
            calc1 ^= all[i];
        }

        // 某些实现可能不包含SEQ与LEN字段参与xor，提供容错方案
        byte calc2 = 0;
        int q = h + 1; // devId开始
        for (int i = q; i < q + 8 + 2 + dataLen; i++) {
            if (i >= all.length || i >= (p - 1)) break;
            calc2 ^= all[i];
        }

        if (xor != calc1 && xor != calc2) {
            Log.w(TAG, "xor8 mismatch: got=" + (xor & 0xFF) + ", calc1=" + (calc1 & 0xFF) + ", calc2=" + (calc2 & 0xFF));
            return null;
        }

        GatewayFrame gf = new GatewayFrame();
        gf.header = (byte) 0xA5;
        gf.deviceIdHex = devHex;
        gf.functionCode = func;
        gf.seq = seq;
        gf.dataLen = dataLen;
        gf.data = data;
        gf.xor8 = xor;
        gf.tail = tail;
        return gf;
    }

    /**
     * 将已解析的完整网关帧(上行0001)转换为TerminalInfo。
     */
    public static TerminalInfo parseUplink0001(GatewayFrame gf) {
        if (gf == null) return null;
        // Bit15方向位：0表示设备->服务器上行
        boolean uplink = ((gf.functionCode & 0x8000) == 0);
        int code = gf.functionCode & 0x7FFF;
        if (!uplink || code != 0x0001) return null;
        byte[] bytes = gf.data;
        if (bytes == null || bytes.length < 24) return null; // 最小字段长度

        int idx = 0;
        long timestamp = bcd7ToUtcMillis(bytes, idx);
        idx += 7;

        int events = ((bytes[idx] & 0xFF) << 24) | ((bytes[idx + 1] & 0xFF) << 16) |
                     ((bytes[idx + 2] & 0xFF) << 8) | (bytes[idx + 3] & 0xFF);
        idx += 4;

        int statusBits = ((bytes[idx] & 0xFF) << 24) | ((bytes[idx + 1] & 0xFF) << 16) |
                         ((bytes[idx + 2] & 0xFF) << 8) | (bytes[idx + 3] & 0xFF);
        idx += 4;

        int voltageRaw = ((bytes[idx] & 0xFF) << 8) | (bytes[idx + 1] & 0xFF);
        float voltage = voltageRaw * 0.01f;
        idx += 2;

        int batteryLevel = bytes[idx++] & 0xFF;
        int rssiDbm = -(bytes[idx++] & 0xFF);
        int departmentId = bytes[idx++] & 0xFF;
        int cartId = bytes[idx++] & 0xFF;
        int deviceCount = bytes[idx++] & 0xFF;
        int rackIndex = bytes[idx++] & 0xFF;
        int ack = (idx < bytes.length) ? (bytes[idx] & 0xFF) : 0;

        TerminalInfo info = new TerminalInfo();
        info.timestamp = timestamp;
        info.signalStrength = rssiDbm;
        info.batteryLevel = batteryLevel;
        info.status = 1; // 上报即视为在线
        info.deviceName = "设备" + (cartId == 0 ? "" : ("-" + cartId));
        info.department = departmentId == 0 ? "" : ("科室-" + departmentId);
        info.location = rackIndex == 0 ? "" : ("台架-" + rackIndex);
        info.deviceId = gf.deviceIdHex != null ? gf.deviceIdHex : "";
        info.payloadHex = bytesToHex(bytes);
        return info;
    }

    /**
     * 构建下行[8001]帧：服务器->设备。
     * 该帧包含时间、应答结果、查询指令、科室、台车、登记结果、清除码(4B)、定时上报间隔(2B)。
     */
    public static byte[] buildDownlink8001(String deviceIdHex,
                                           byte seq,
                                           long timeUtcMillis,
                                           int ackResult,
                                           int queryOp,
                                           int departmentId,
                                           int cartId,
                                           int registerResult,
                                           int clearMask,
                                           int reportIntervalMin) {
        byte[] dev = hexStringToBytes(deviceIdHex);
        if (dev == null || dev.length != 8) throw new IllegalArgumentException("deviceIdHex必须为8字节HEX");

        // DATA构建
        byte[] data = new byte[7 + 1 + 1 + 1 + 1 + 1 + 4 + 2];
        int i = 0;
        writeBcd7(data, i, timeUtcMillis); i += 7;
        data[i++] = (byte) (ackResult & 0xFF);
        data[i++] = (byte) (queryOp & 0xFF);
        data[i++] = (byte) (departmentId & 0xFF);
        data[i++] = (byte) (cartId & 0xFF);
        data[i++] = (byte) (registerResult & 0xFF);
        data[i++] = (byte) ((clearMask >> 24) & 0xFF);
        data[i++] = (byte) ((clearMask >> 16) & 0xFF);
        data[i++] = (byte) ((clearMask >> 8) & 0xFF);
        data[i++] = (byte) (clearMask & 0xFF);
        data[i++] = (byte) ((reportIntervalMin >> 8) & 0xFF);
        data[i++] = (byte) (reportIntervalMin & 0xFF);

        // 帧拼装
        int total = 1 + 8 + 2 + 1 + 2 + data.length + 1 + 1;
        byte[] out = new byte[total];
        int p = 0;
        out[p++] = GW_FRAME_HEADER;
        System.arraycopy(dev, 0, out, p, 8); p += 8;
        int func = 0x8001 | 0x8000; // 方向位Bit15=1 + 功能码0x8001
        out[p++] = (byte) ((func >> 8) & 0xFF);
        out[p++] = (byte) (func & 0xFF);
        out[p++] = seq;
        int len = data.length;
        out[p++] = (byte) ((len >> 8) & 0xFF);
        out[p++] = (byte) (len & 0xFF);
        System.arraycopy(data, 0, out, p, len); p += len;

        // xor8：从设备ID开始到效验码前
        byte xor = 0;
        for (int k = 1; k < p; k++) xor ^= out[k];
        out[p++] = xor;
        out[p] = GW_FRAME_TAIL;
        return out;
    }

    private static void writeBcd7(byte[] out, int idx, long utcMillis) {
        java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(utcMillis);
        int year = c.get(java.util.Calendar.YEAR) - 2000;
        int month = c.get(java.util.Calendar.MONTH) + 1;
        int day = c.get(java.util.Calendar.DAY_OF_MONTH);
        int hour = c.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = c.get(java.util.Calendar.MINUTE);
        int second = c.get(java.util.Calendar.SECOND);
        out[idx]     = (byte) (((year / 10) << 4) | (year % 10));
        out[idx + 1] = (byte) (((month / 10) << 4) | (month % 10));
        out[idx + 2] = (byte) (((day / 10) << 4) | (day % 10));
        out[idx + 3] = (byte) (((hour / 10) << 4) | (hour % 10));
        out[idx + 4] = (byte) (((minute / 10) << 4) | (minute % 10));
        out[idx + 5] = (byte) (((second / 10) << 4) | (second % 10));
        out[idx + 6] = 0; // 预留/毫秒忽略
    }
}