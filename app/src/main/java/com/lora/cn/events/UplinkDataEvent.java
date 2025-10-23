package com.lora.cn.events;

/**
 * EventBus事件类，用于广播上行数据
 */
public class UplinkDataEvent {
    private String time;
    private String hex;

    public UplinkDataEvent(String time, String hex) {
        this.time = time;
        this.hex = hex;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getHex() {
        return hex;
    }

    public void setHex(String hex) {
        this.hex = hex;
    }

    @Override
    public String toString() {
        return "UplinkDataEvent{" +
                "time='" + time + '\'' +
                ", hex='" + hex + '\'' +
                '}';
    }
}