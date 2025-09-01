package com.lora.cn.database.entity;

import java.util.Date;

/**
 * 分类实体类
 */
public class Category {
    private long categoryId;
    private String categoryName;
    private String categoryDescription;
    private long groupId; // 外键，关联分组表
    private Date createTime;
    private Date updateTime;
    
    // 关联的分组对象（多对一关系）
    private Group group;
    
    public Category() {
    }
    
    public Category(String categoryName, String categoryDescription, long groupId) {
        this.categoryName = categoryName;
        this.categoryDescription = categoryDescription;
        this.groupId = groupId;
    }
    
    public Category(long categoryId, String categoryName, String categoryDescription, 
                   long groupId, Date createTime, Date updateTime) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryDescription = categoryDescription;
        this.groupId = groupId;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }
    
    // Getter和Setter方法
    public long getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    public String getCategoryDescription() {
        return categoryDescription;
    }
    
    public void setCategoryDescription(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }
    
    public long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(long groupId) {
        this.groupId = groupId;
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
    
    public Group getGroup() {
        return group;
    }
    
    public void setGroup(Group group) {
        this.group = group;
        if (group != null) {
            this.groupId = group.getGroupId();
        }
    }
    
    @Override
    public String toString() {
        return "Category{" +
                "categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", categoryDescription='" + categoryDescription + '\'' +
                ", groupId=" + groupId +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Category category = (Category) o;
        
        if (categoryId != category.categoryId) return false;
        if (groupId != category.groupId) return false;
        return categoryName != null ? categoryName.equals(category.categoryName) : category.categoryName == null;
    }
    
    @Override
    public int hashCode() {
        int result = (int) (categoryId ^ (categoryId >>> 32));
        result = 31 * result + (categoryName != null ? categoryName.hashCode() : 0);
        result = 31 * result + (int) (groupId ^ (groupId >>> 32));
        return result;
    }
}