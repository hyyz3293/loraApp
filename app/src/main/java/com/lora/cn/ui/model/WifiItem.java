package com.lora.cn.ui.model;

public class WifiItem {
    private String ssid;
    private int signalLevel;
    private boolean isSecured;
    private boolean isConnected;
    
    public WifiItem(String ssid, int signalLevel, boolean isSecured) {
        this.ssid = ssid;
        this.signalLevel = signalLevel;
        this.isSecured = isSecured;
        this.isConnected = false;
    }
    
    public String getSsid() {
        return ssid;
    }
    
    public void setSsid(String ssid) {
        this.ssid = ssid;
    }
    
    public int getSignalLevel() {
        return signalLevel;
    }
    
    public void setSignalLevel(int signalLevel) {
        this.signalLevel = signalLevel;
    }
    
    public boolean isSecured() {
        return isSecured;
    }
    
    public void setSecured(boolean secured) {
        isSecured = secured;
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public void setConnected(boolean connected) {
        isConnected = connected;
    }
    
    public String getSignalStrengthText() {
        if (signalLevel > -50) {
            return "强";
        } else if (signalLevel > -70) {
            return "中";
        } else {
            return "弱";
        }
    }
}