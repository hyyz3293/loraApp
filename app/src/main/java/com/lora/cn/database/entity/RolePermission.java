package com.lora.cn.database.entity;

import java.util.Date;

/**
 * 角色权限关联实体类
 * 用于存储角色和权限的多对多关系
 */
public class RolePermission {
    private long id;
    private long roleId; // 角色ID
    private long permissionId; // 权限ID
    private Date createTime;
    
    public RolePermission() {
    }
    
    public RolePermission(long roleId, long permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }
    
    public RolePermission(long id, long roleId, long permissionId, Date createTime) {
        this.id = id;
        this.roleId = roleId;
        this.permissionId = permissionId;
        this.createTime = createTime;
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getRoleId() {
        return roleId;
    }
    
    public void setRoleId(long roleId) {
        this.roleId = roleId;
    }
    
    public long getPermissionId() {
        return permissionId;
    }
    
    public void setPermissionId(long permissionId) {
        this.permissionId = permissionId;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    
    @Override
    public String toString() {
        return "RolePermission{" +
                "id=" + id +
                ", roleId=" + roleId +
                ", permissionId=" + permissionId +
                ", createTime=" + createTime +
                '}';
    }
}