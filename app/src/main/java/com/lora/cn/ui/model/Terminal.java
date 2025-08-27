package com.lora.cn.ui.model;

public class Terminal {
    private String name;
    private String department;
    private String location;
    private int statusIconResId;
    private String statusText;
    private int batteryIconResId;
    private String batteryText;
    private boolean isImportant;

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
}