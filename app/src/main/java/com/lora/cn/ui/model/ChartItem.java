package com.lora.cn.ui.model;

public class ChartItem {
    private int color;        // 图标颜色
    private String key;       // 键名（如"正常取走"）
    private String value;     // 值（如"11"）
    
    public ChartItem(int color, String key, String value) {
        this.color = color;
        this.key = key;
        this.value = value;
    }
    
    public int getColor() {
        return color;
    }
    
    public void setColor(int color) {
        this.color = color;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
}