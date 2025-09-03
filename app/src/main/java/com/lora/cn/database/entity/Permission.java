package com.lora.cn.database.entity;

import java.util.Date;
import java.util.List;

/**
 * Permission Entity Class
 */
public class Permission {
    private long permissionId;
    private String permissionCode; // Permission code, e.g. "terminal_list", "terminal_add"
    private String permissionName; // Permission name, e.g. "Terminal List", "Add Terminal"
    private String category; // Permission category, e.g. "terminal", "log", "setting"
    private String description; // Permission description
    private int status; // Status (0/1 switch)
    private Long parentId; // Parent permission ID, null for top-level permissions
    private int level; // Permission level, starting from 0
    private int sortOrder; // Sort order
    private Date createTime;
    private Date updateTime;

    public boolean isParent = false;
    public boolean isSelect = false;
    public boolean isExpand = false;

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
        return childList;
    }

    public void setChild(List<Permission> child) {
        this.childList = child;
    }

    public List<Permission> getChildList() {
        return childList;
    }

    public void setChildList(List<Permission> childList) {
        this.childList = childList;
    }


    public boolean isParent() {
        return isParent;
    }

    public void setParent(boolean parent) {
        isParent = parent;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    public boolean isExpand() {
        return isExpand;
    }

    public void setExpand(boolean expand) {
        isExpand = expand;
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