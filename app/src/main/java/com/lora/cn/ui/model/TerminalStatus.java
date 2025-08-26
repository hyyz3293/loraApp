package com.lora.cn.ui.model;

public class TerminalStatus {
    private String title;
    private int iconResId;
    private int count;
    private boolean isSelected;

    public TerminalStatus(String title, int iconResId, int count) {
        this.title = title;
        this.iconResId = iconResId;
        this.count = count;
        this.isSelected = false;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}