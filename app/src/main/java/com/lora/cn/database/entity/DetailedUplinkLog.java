package com.lora.cn.database.entity;

import com.lora.cn.utils.LoRaFrameParser;
import java.util.Date;

/**
 * 详细上行数据日志实体类
 * 包含解析后的所有数据字段
 */
public class DetailedUplinkLog {
    
    private int logId;                    // 日志ID
    private String time;                  // 接收时间
    private String hex;                   // 原始HEX数据
    private Date createTime;              // 创建时间
    
    // 解析后的帧数据
    private String deviceId;              // 设备ID (8字节)
    private String functionCode;          // 功能码 (2字节)
    private int sequenceNumber;           // 流水号 (1字节)
    private int dataLength;              // 数据长度 (2字节)
    
    // 解析后的数据内容字段
    private Date dataTime;               // 数据产生时间 (7字节BCD)
    private long deviceEvent;            // 设备事件 (4字节)
    private long deviceStatus;           // 设备状态 (4字节)
    private int batteryVoltage;          // 电池电压 (2字节)
    private int batteryLevel;            // 电量 (1字节)
    private int rssi;                    // RSSI (1字节)
    private int departmentNumber;        // 科室或护士站编号 (1字节)
    private int cartNumber;              // 台车编号 (1字节)
    private int deviceCount;             // 放置的设备数量 (1字节)
    private int rackNumber;              // 设备所属台车台架编号 (1字节)
    
    // 解析状态
    private boolean parseSuccess;        // 解析是否成功
    private String parseError;           // 解析错误信息
    
    // 构造函数
    public DetailedUplinkLog() {}
    
    public DetailedUplinkLog(String time, String hex) {
        this.time = time;
        this.hex = hex;
        this.createTime = new Date();
    }
    
    // Getter和Setter方法
    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }
    
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    
    public String getHex() { return hex; }
    public void setHex(String hex) { this.hex = hex; }
    
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public String getFunctionCode() { return functionCode; }
    public void setFunctionCode(String functionCode) { this.functionCode = functionCode; }
    
    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    
    public int getDataLength() { return dataLength; }
    public void setDataLength(int dataLength) { this.dataLength = dataLength; }
    
    public Date getDataTime() { return dataTime; }
    public void setDataTime(Date dataTime) { this.dataTime = dataTime; }
    
    public long getDeviceEvent() { return deviceEvent; }
    public void setDeviceEvent(long deviceEvent) { this.deviceEvent = deviceEvent; }
    
    public long getDeviceStatus() { return deviceStatus; }
    public void setDeviceStatus(long deviceStatus) { this.deviceStatus = deviceStatus; }
    
    public int getBatteryVoltage() { return batteryVoltage; }
    public void setBatteryVoltage(int batteryVoltage) { this.batteryVoltage = batteryVoltage; }
    
    public int getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(int batteryLevel) { this.batteryLevel = batteryLevel; }
    
    public int getRssi() { return rssi; }
    public void setRssi(int rssi) { this.rssi = rssi; }
    
    public int getDepartmentNumber() { return departmentNumber; }
    public void setDepartmentNumber(int departmentNumber) { this.departmentNumber = departmentNumber; }
    
    public int getCartNumber() { return cartNumber; }
    public void setCartNumber(int cartNumber) { this.cartNumber = cartNumber; }
    
    public int getDeviceCount() { return deviceCount; }
    public void setDeviceCount(int deviceCount) { this.deviceCount = deviceCount; }
    
    public int getRackNumber() { return rackNumber; }
    public void setRackNumber(int rackNumber) { this.rackNumber = rackNumber; }
    
    public boolean isParseSuccess() { return parseSuccess; }
    public void setParseSuccess(boolean parseSuccess) { this.parseSuccess = parseSuccess; }
    
    public String getParseError() { return parseError; }
    public void setParseError(String parseError) { this.parseError = parseError; }
    
    /**
     * 从ParsedFrame填充数据
     */
    public void fillFromParsedFrame(LoRaFrameParser.ParsedFrame frame) {
        if (frame != null) {
            this.deviceId = frame.deviceId;
            this.functionCode = frame.functionCode;
            this.sequenceNumber = frame.sequenceNumber;
            this.dataLength = frame.dataLength;
            this.dataTime = frame.dataTime;
            this.deviceEvent = frame.deviceEvent;
            this.deviceStatus = frame.deviceStatus;
            this.batteryVoltage = frame.batteryVoltage;
            this.batteryLevel = frame.batteryLevel;
            this.rssi = frame.rssi;
            this.departmentNumber = frame.departmentNumber;
            this.cartNumber = frame.cartNumber;
            this.deviceCount = frame.deviceCount;
            this.rackNumber = frame.rackNumber;
            this.parseSuccess = true;
            this.parseError = null;
        } else {
            this.parseSuccess = false;
            this.parseError = "帧解析失败";
        }
    }
    
    /**
     * 检查是否有设备异常事件
     */
    public boolean hasDeviceAlert() {
        return LoRaFrameParser.hasDeviceAlert(this.deviceEvent);
    }
    
    /**
     * 获取设备事件描述
     */
    public String getDeviceEventDescription() {
        return LoRaFrameParser.getDeviceEventDescription(this.deviceEvent);
    }
    
    @Override
    public String toString() {
        return "DetailedUplinkLog{" +
                "logId=" + logId +
                ", deviceId='" + deviceId + '\'' +
                ", functionCode='" + functionCode + '\'' +
                ", batteryLevel=" + batteryLevel +
                ", cartNumber=" + cartNumber +
                ", departmentNumber=" + departmentNumber +
                ", parseSuccess=" + parseSuccess +
                '}';
    }
}