package com.lora.cn.database.entity;

import java.util.Date;

/**
 * 角色实体类
 */
public class Role {
    private long roleId;
    private String roleName;
    private String description;
    private int sortOrder; // 排序号
    private int status; // 状态（0/1 开关）
    private Date createTime;
    private Date updateTime;
    
    public Role() {
    }
    
    public Role(String roleName, String description, int sortOrder, int status) {
        this.roleName = roleName;
        this.description = description;
        this.sortOrder = sortOrder;
        this.status = status;
    }
    
    public Role(long roleId, String roleName, String description, int sortOrder, int status, Date createTime, Date updateTime) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.description = description;
        this.sortOrder = sortOrder;
        this.status = status;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }
    
    public long getRoleId() {
        return roleId;
    }
    
    public void setRoleId(long roleId) {
        this.roleId = roleId;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
        return "Role{" +
                "roleId=" + roleId +
                ", roleName='" + roleName + '\'' +
                ", description='" + description + '\'' +
                ", sortOrder=" + sortOrder +
                ", status=" + status +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}