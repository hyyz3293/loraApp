package com.lora.cn.database.entity;

import com.lora.cn.utils.LoRaFrameParser;

import java.util.Date;

/**
 * 终端实体类
 */
public class Terminal {
    private int terminalId;
    private String deviceId;
    private String deviceName;
    private String status;
    private int signalStrength;
    private String department;
    private String location;
    private Integer departmentId;    // 科室分类ID
    private Integer roomId;          // 病房号分类ID
    private Integer nursingGroupId;  // 护理组分类ID
    private Integer otherId;         // 其他分类ID
    private String extension;        // 扩展字段
    private Date createTime;
    private Date updateTime;

    public LoRaFrameParser.ParsedFrame parsedFrame;

    // 上行数据相关字段
    private Date dataTime;           // 数据产生时间 (7字节BCD码)
    private Long deviceEvent;        // 设备事件 (4字节)
    private Long deviceStatus;       // 设备状态 (4字节)
    private Integer batteryVoltage;  // 电池电压 (2字节)
    private Integer batteryLevel;    // 电量 (1字节)
    private Integer rssi;            // RSSI (1字节)
    private Integer departmentNumber; // 科室或护士站编号 (1字节)
    private Integer cartNumber;      // 台车编号 (1字节)
    private Integer deviceCount;     // 放置的设备数量 (1字节)
    private Integer rackNumber;      // 设备所属台车台架编号 (1字节)
    private String functionCode;     // 功能码
    private Integer sequenceNumber;  // 序列号
    private Integer dataLength;      // 数据长度
    private String dataContent;      // 数据内容 (JSON格式存储)
    private Integer checksum;        // 校验和

    public Terminal() {
    }

    public Terminal(String deviceId, String deviceName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.status = "在线";
        this.signalStrength = 0;
        this.createTime = new Date();
        this.updateTime = new Date();
    }

    // Getters and Setters
    public int getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(int terminalId) {
        this.terminalId = terminalId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public Integer getNursingGroupId() {
        return nursingGroupId;
    }

    public void setNursingGroupId(Integer nursingGroupId) {
        this.nursingGroupId = nursingGroupId;
    }

    public Integer getOtherId() {
        return otherId;
    }

    public void setOtherId(Integer otherId) {
        this.otherId = otherId;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    // 上行数据相关字段的getter和setter方法
    public Date getDataTime() {
        return dataTime;
    }

    public void setDataTime(Date dataTime) {
        this.dataTime = dataTime;
    }

    public Long getDeviceEvent() {
        return deviceEvent;
    }

    public void setDeviceEvent(Long deviceEvent) {
        this.deviceEvent = deviceEvent;
    }

    public Long getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(Long deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public Integer getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(Integer batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public Integer getRssi() {
        return rssi;
    }

    public void setRssi(Integer rssi) {
        this.rssi = rssi;
    }

    public Integer getDepartmentNumber() {
        return departmentNumber;
    }

    public void setDepartmentNumber(Integer departmentNumber) {
        this.departmentNumber = departmentNumber;
    }

    public Integer getCartNumber() {
        return cartNumber;
    }

    public void setCartNumber(Integer cartNumber) {
        this.cartNumber = cartNumber;
    }

    public Integer getDeviceCount() {
        return deviceCount;
    }

    public void setDeviceCount(Integer deviceCount) {
        this.deviceCount = deviceCount;
    }

    public Integer getRackNumber() {
        return rackNumber;
    }

    public void setRackNumber(Integer rackNumber) {
        this.rackNumber = rackNumber;
    }

    public String getFunctionCode() {
        return functionCode;
    }

    public void setFunctionCode(String functionCode) {
        this.functionCode = functionCode;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Integer getDataLength() {
        return dataLength;
    }

    public void setDataLength(Integer dataLength) {
        this.dataLength = dataLength;
    }

    public String getDataContent() {
        return dataContent;
    }

    public void setDataContent(String dataContent) {
        this.dataContent = dataContent;
    }

    public Integer getChecksum() {
        return checksum;
    }

    public void setChecksum(Integer checksum) {
        this.checksum = checksum;
    }

    @Override
    public String toString() {
        return "Terminal{" +
                "terminalId=" + terminalId +
                ", deviceId='" + deviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", status='" + status + '\'' +
                ", signalStrength=" + signalStrength +
                ", department='" + department + '\'' +
                ", location='" + location + '\'' +
                ", departmentId=" + departmentId +
                ", roomId=" + roomId +
                ", nursingGroupId=" + nursingGroupId +
                ", otherId=" + otherId +
                ", extension='" + extension + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}