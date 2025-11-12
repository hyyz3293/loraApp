package com.lora.cn.ui.model;

public class Terminal {
    private long id;
    private String terminalId;
    private String terminalName;
    private String name;
    private String department;
    private String location;
    private int statusIconResId;
    private String statusText;
    private int status;
    private int batteryIconResId;
    private String batteryText;
    private int batteryStatus;
    private boolean isImportant;
    private int signalStrength;
    private long createTime;
    private long updateTime;
    
    // 新增字段
    private long departmentId;
    private long roomId;
    private long nursingGroupId;
    private long otherId;
    private String extension;
    private String deviceCode; // 新增设备CODE
    private boolean isFavorite;
    private int batteryLevel; // 电量百分比
    private int batteryVoltage; // 电池电压(单位0.01V)
    private int rssi; // 原始RSSI (0~138 对应 -138~0dBm)

    public Terminal() {
    }

    public Terminal(String name, String department, String location, 
                   int statusIconResId, String statusText, 
                   int batteryIconResId, String batteryText, 
                   boolean isImportant) {
        this.name = name;
        this.department = department;
        this.location = location;
        this.statusIconResId = statusIconResId;
        this.statusText = statusText;
        this.batteryIconResId = batteryIconResId;
        this.batteryText = batteryText;
        this.isImportant = isImportant;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getTerminalName() {
        return terminalName;
    }

    public void setTerminalName(String terminalName) {
        this.terminalName = terminalName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getStatusIconResId() {
        return statusIconResId;
    }

    public void setStatusIconResId(int statusIconResId) {
        this.statusIconResId = statusIconResId;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getBatteryIconResId() {
        return batteryIconResId;
    }

    public void setBatteryIconResId(int batteryIconResId) {
        this.batteryIconResId = batteryIconResId;
    }

    public String getBatteryText() {
        return batteryText;
    }

    public void setBatteryText(String batteryText) {
        this.batteryText = batteryText;
    }

    public int getBatteryStatus() {
        return batteryStatus;
    }

    public void setBatteryStatus(int batteryStatus) {
        this.batteryStatus = batteryStatus;
    }

    public boolean isImportant() {
        return isImportant;
    }

    public void setImportant(boolean important) {
        isImportant = important;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(long departmentId) {
        this.departmentId = departmentId;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public long getNursingGroupId() {
        return nursingGroupId;
    }

    public void setNursingGroupId(long nursingGroupId) {
        this.nursingGroupId = nursingGroupId;
    }

    public long getOtherId() {
        return otherId;
    }

    public void setOtherId(long otherId) {
        this.otherId = otherId;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public int getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(int batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
}
