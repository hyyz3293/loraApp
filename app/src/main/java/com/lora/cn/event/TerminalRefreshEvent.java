package com.lora.cn.event;

/**
 * 终端刷新事件
 * 用于EventBus传递终端刷新消息
 */
public class TerminalRefreshEvent {
    private String message;

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