package com.lora.cn.database.entity;

import java.util.Date;

/**
 * 科室实体类
 */
public class Department {
    private long departmentId;
    private String departmentName;
    private int sortOrder; // 排序
    private int status; // 状态（0/1 开关）
    private Date createTime;
    private Date updateTime;
    
    public Department() {
    }
    
    public Department(String departmentName, int sortOrder, int status) {
        this.departmentName = departmentName;
        this.sortOrder = sortOrder;
        this.status = status;
    }
    
    public Department(long departmentId, String departmentName, int sortOrder, int status, Date createTime, Date updateTime) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.sortOrder = sortOrder;
        this.status = status;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }
    
    // Getter和Setter方法
    public long getDepartmentId() {
        return departmentId;
    }
    
    public void setDepartmentId(long departmentId) {
        this.departmentId = departmentId;
    }
    
    public String getDepartmentName() {
        return departmentName;
    }
    
    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
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
        return "Department{" +
                "departmentId=" + departmentId +
                ", departmentName='" + departmentName + '\'' +
                ", sortOrder=" + sortOrder +
                ", status=" + status +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}