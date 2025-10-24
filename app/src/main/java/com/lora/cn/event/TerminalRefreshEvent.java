package com.lora.cn.event;

/**
 * 终端刷新事件
 * 用于通知TerminalListFragment刷新终端列表
 */
public class TerminalRefreshEvent {
    private String message;
    
    public TerminalRefreshEvent() {
        this.message = "终端列表需要刷新";
    }
    
    public TerminalRefreshEvent(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}