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
    private String status;
    private int batteryIconResId;
    private String batteryText;
    private boolean isImportant;
    private int signalStrength;
    private long createTime;
    private long updateTime;

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

    public boolean isImportant() {
        return isImportant;
    }

    public void setImportant(boolean important) {
        isImportant = important;
    }

    // 新增字段的getter和setter方法
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
}