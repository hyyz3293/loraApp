package com.lora.cn.database.entity;

import java.util.Date;

/**
 * 职位实体类
 */
public class Position {
    private long positionId;
    private String positionName;
    private int sortOrder; // 排序
    private int status; // 状态（0/1 开关）
    private Date createTime;
    private Date updateTime;
    
    public Position() {
    }
    
    public Position(String positionName, int sortOrder, int status) {
        this.positionName = positionName;
        this.sortOrder = sortOrder;
        this.status = status;
    }
    
    public Position(long positionId, String positionName, int sortOrder, int status, Date createTime, Date updateTime) {
        this.positionId = positionId;
        this.positionName = positionName;
        this.sortOrder = sortOrder;
        this.status = status;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }
    
    // Getter和Setter方法
    public long getPositionId() {
        return positionId;
    }
    
    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }
    
    public String getPositionName() {
        return positionName;
    }
    
    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }
    
    public int getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
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
        return "Position{" +
                "positionId=" + positionId +
                ", positionName='" + positionName + '\'' +
                ", sortOrder=" + sortOrder +
                ", status=" + status +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}