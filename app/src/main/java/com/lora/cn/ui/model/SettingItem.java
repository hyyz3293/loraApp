package com.lora.cn.ui.model;

public class SettingItem {
    private int iconResId = -1;
    private String title;
    private int viewType;

    private String volume;
    private String num;

    public SettingItem(int iconResId, String title) {
        this.iconResId = iconResId;
        this.title = title;
    }
    public SettingItem(String title) {
        this.title = title;
    }

    public SettingItem(String title, int viewType) {
        this.title = title;
        this.viewType = viewType;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getViewType() {
        return viewType;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }
}