package com.lora.cn.database.entity;

import java.util.Date;

/**
 * 终端实体类
 */
public class Terminal {
    private int terminalId;
    private String deviceId;
    private String deviceName;
    private String status;
    private int signalStrength;
    private String department;
    private String location;
    private Integer departmentId;    // 科室分类ID
    private Integer roomId;          // 病房号分类ID
    private Integer nursingGroupId;  // 护理组分类ID
    private Integer otherId;         // 其他分类ID
    private String extension;        // 扩展字段
    private Date createTime;
    private Date updateTime;

    public Terminal() {
    }

    public Terminal(String deviceId, String deviceName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.status = "在线";
        this.signalStrength = 0;
        this.createTime = new Date();
        this.updateTime = new Date();
    }

    // Getters and Setters
    public int getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(int terminalId) {
        this.terminalId = terminalId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public Integer getNursingGroupId() {
        return nursingGroupId;
    }

    public void setNursingGroupId(Integer nursingGroupId) {
        this.nursingGroupId = nursingGroupId;
    }

    public Integer getOtherId() {
        return otherId;
    }

    public void setOtherId(Integer otherId) {
        this.otherId = otherId;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "Terminal{" +
                "terminalId=" + terminalId +
                ", deviceId='" + deviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", status='" + status + '\'' +
                ", signalStrength=" + signalStrength +
                ", department='" + department + '\'' +
                ", location='" + location + '\'' +
                ", departmentId=" + departmentId +
                ", roomId=" + roomId +
                ", nursingGroupId=" + nursingGroupId +
                ", otherId=" + otherId +
                ", extension='" + extension + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}