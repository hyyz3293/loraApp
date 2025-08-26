package com.lora.cn.ui.model;

public class MenuTab {
    private String title;
    private int tabIndex;
    private boolean isSelected;

    public MenuTab(String title, int tabIndex) {
        this.title = title;
        this.tabIndex = tabIndex;
        this.isSelected = false;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}