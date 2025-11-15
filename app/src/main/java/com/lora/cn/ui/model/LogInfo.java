package com.lora.cn.ui.model;

public class LogInfo {
    private long id;
    private String terminalId;
    private String terminalName;
    private String deviceId;
    private int statusCode;
    private String operator = "";
    private String operationTime = "";
    private String action;
    private String createTime;

    public LogInfo() {
    }

    public LogInfo(String terminalId, String terminalName, String deviceId, 
                   int statusCode, String operator, String operationTime, String action) {
        this.terminalId = terminalId;
        this.terminalName = terminalName;
        this.deviceId = deviceId;
        this.statusCode = statusCode;
        this.operator = operator;
        this.operationTime = operationTime;
        this.action = action;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getTerminalName() {
        return terminalName;
    }

    public void setTerminalName(String terminalName) {
        this.terminalName = terminalName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(String operationTime) {
        this.operationTime = operationTime;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    private String handleUser = "";
    private String handleTime = "";
    private String handleRemark = "";

    public String getHandleUser() { return handleUser; }
    public void setHandleUser(String handleUser) { this.handleUser = handleUser; }
    public String getHandleTime() { return handleTime; }
    public void setHandleTime(String handleTime) { this.handleTime = handleTime; }
    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String handleRemark) { this.handleRemark = handleRemark; }
}
