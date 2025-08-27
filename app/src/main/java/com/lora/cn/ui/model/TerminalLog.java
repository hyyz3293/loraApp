package com.lora.cn.ui.model;

public class TerminalLog {
    private String logTime;
    private String logStatus;
    private String logName;
    private String logId;
    private String logComplete;
    private String logCompleteTime;
    private String logOperation;

    public TerminalLog() {
    }

    public TerminalLog(String logTime, String logStatus, String logName, 
                      String logId, String logComplete, String logCompleteTime, 
                      String logOperation) {
        this.logTime = logTime;
        this.logStatus = logStatus;
        this.logName = logName;
        this.logId = logId;
        this.logComplete = logComplete;
        this.logCompleteTime = logCompleteTime;
        this.logOperation = logOperation;
    }

    public String getLogTime() {
        return logTime;
    }

    public void setLogTime(String logTime) {
        this.logTime = logTime;
    }

    public String getLogStatus() {
        return logStatus;
    }

    public void setLogStatus(String logStatus) {
        this.logStatus = logStatus;
    }

    public String getLogName() {
        return logName;
    }

    public void setLogName(String logName) {
        this.logName = logName;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getLogComplete() {
        return logComplete;
    }

    public void setLogComplete(String logComplete) {
        this.logComplete = logComplete;
    }

    public String getLogCompleteTime() {
        return logCompleteTime;
    }

    public void setLogCompleteTime(String logCompleteTime) {
        this.logCompleteTime = logCompleteTime;
    }

    public String getLogOperation() {
        return logOperation;
    }

    public void setLogOperation(String logOperation) {
        this.logOperation = logOperation;
    }
}