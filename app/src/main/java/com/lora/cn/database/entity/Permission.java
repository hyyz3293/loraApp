package com.lora.cn.database.entity;

import java.util.Date;
import java.util.List;

/**
 * 权限实体类
 */
public class Permission {
    private long permissionId;
    private String permissionCode; // 权限代码，如 "terminal_list", "terminal_add" 等
    private String permissionName; // 权限名称，如 "终端列表", "添加终端" 等
    private String category; // 权限分类，如 "terminal", "log", "setting" 等
    private String description; // 权限描述
    private int status; // 状态（0/1 开关）
    private Long parentId; // 父权限ID，顶级权限为null
    private int level; // 权限层级，从0开始
    private int sortOrder; // 排序顺序
    private Date createTime;
    private Date updateTime;

    private List<Permission> childList;

    public Permission() {
    }
    
    public Permission(String permissionCode, String permissionName, String category, String description, int status) {
        this.permissionCode = permissionCode;
        this.permissionName = permissionName;
        this.category = category;
        this.description = description;
        this.status = status;
        this.level = 0;
        this.sortOrder = 0;
    }
    
    public Permission(String permissionCode, String permissionName, String category, String description, int status, Long parentId, int level, int sortOrder) {
        this.permissionCode = permissionCode;
        this.permissionName = permissionName;
        this.category = category;
        this.description = description;
        this.status = status;
        this.parentId = parentId;
        this.level = level;
        this.sortOrder = sortOrder;
    }
    
    public Permission(long permissionId, String permissionCode, String permissionName, String category, String description, int status, Date createTime, Date updateTime) {
        this.permissionId = permissionId;
        this.permissionCode = permissionCode;
        this.permissionName = permissionName;
        this.category = category;
        this.description = description;
        this.status = status;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }
    
    public long getPermissionId() {
        return permissionId;
    }
    
    public void setPermissionId(long permissionId) {
        this.permissionId = permissionId;
    }
    
    public String getPermissionCode() {
        return permissionCode;
    }
    
    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }
    
    public String getPermissionName() {
        return permissionName;
    }
    
    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public int getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<Permission> getChild() {
        return child;
    }

    public void setChild(List<Permission> child) {
        this.child = child;
    }

    @Override
    public String toString() {
        return "Permission{" +
                "permissionId=" + permissionId +
                ", permissionCode='" + permissionCode + '\'' +
                ", permissionName='" + permissionName + '\'' +
                ", category='" + category + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", parentId=" + parentId +
                ", level=" + level +
                ", sortOrder=" + sortOrder +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}