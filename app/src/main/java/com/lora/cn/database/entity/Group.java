package com.lora.cn.database.entity;

import java.util.Date;
import java.util.List;

/**
 * 分组实体类
 */
public class Group {
    private long groupId;
    private String groupName;
    private String groupDescription;
    private Date createTime;
    private Date updateTime;
    
    // 关联的分类列表（一对多关系）
    private List<Category> categories;
    
    public Group() {
    }
    
    public Group(String groupName, String groupDescription) {
        this.groupName = groupName;
        this.groupDescription = groupDescription;
    }
    
    public Group(long groupId, String groupName, String groupDescription, Date createTime, Date updateTime) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.groupDescription = groupDescription;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }
    
    // Getter和Setter方法
    public long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public String getGroupDescription() {
        return groupDescription;
    }
    
    public void setGroupDescription(String groupDescription) {
        this.groupDescription = groupDescription;
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
    
    public List<Category> getCategories() {
        return categories;
    }
    
    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }
    
    @Override
    public String toString() {
        return "Group{" +
                "groupId=" + groupId +
                ", groupName='" + groupName + '\'' +
                ", groupDescription='" + groupDescription + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Group group = (Group) o;
        
        if (groupId != group.groupId) return false;
        return groupName != null ? groupName.equals(group.groupName) : group.groupName == null;
    }
    
    @Override
    public int hashCode() {
        int result = (int) (groupId ^ (groupId >>> 32));
        result = 31 * result + (groupName != null ? groupName.hashCode() : 0);
        return result;
    }
}