package com.lora.cn.event;

public class OperationBusyEvent {
    public final boolean busy;
    public OperationBusyEvent(boolean busy) { this.busy = busy; }
}