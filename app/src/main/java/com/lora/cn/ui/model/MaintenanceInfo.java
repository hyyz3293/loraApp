package com.lora.cn.ui.model;

public class MaintenanceInfo {
    private long id;
    private String terminalId;
    private String terminalName;
    private String terminalGroup;
    private int status;
    private String content;
    private long createUserId;
    private String createUser;
    private String createTime;
    private long handleUserId;
    private String handleUser;
    private String handleTime;
    private String handleRemark;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTerminalId() { return terminalId; }
    public void setTerminalId(String terminalId) { this.terminalId = terminalId; }

    public String getTerminalName() { return terminalName; }
    public void setTerminalName(String terminalName) { this.terminalName = terminalName; }

    public String getTerminalGroup() { return terminalGroup; }
    public void setTerminalGroup(String terminalGroup) { this.terminalGroup = terminalGroup; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getCreateUserId() { return createUserId; }
    public void setCreateUserId(long createUserId) { this.createUserId = createUserId; }

    public String getCreateUser() { return createUser; }
    public void setCreateUser(String createUser) { this.createUser = createUser; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public long getHandleUserId() { return handleUserId; }
    public void setHandleUserId(long handleUserId) { this.handleUserId = handleUserId; }

    public String getHandleUser() { return handleUser; }
    public void setHandleUser(String handleUser) { this.handleUser = handleUser; }

    public String getHandleTime() { return handleTime; }
    public void setHandleTime(String handleTime) { this.handleTime = handleTime; }
    
    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String handleRemark) { this.handleRemark = handleRemark; }
}

