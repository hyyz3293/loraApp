package com.lora.cn.database;

import android.content.Context;
import android.util.Log;
import com.lora.cn.database.entity.Category;
import com.lora.cn.database.entity.Group;

import java.util.List;

/**
 * 数据库使用示例
 * 演示如何使用DatabaseManager进行数据库操作
 */
public class DatabaseExample {
    private static final String TAG = "DatabaseExample";
    private DatabaseManager dbManager;
    
    public DatabaseExample(Context context) {
        dbManager = DatabaseManager.getInstance(context);
    }
    
    /**
     * 演示基本的CRUD操作
     */
    public void demonstrateBasicOperations() {
        Log.d(TAG, "=== 开始演示数据库基本操作 ===");
        
        try {
            // 1. 添加分组
            Log.d(TAG, "1. 添加分组");
            long groupId1 = dbManager.addGroup("测试分组1", "这是第一个测试分组");
            long groupId2 = dbManager.addGroup("测试分组2", "这是第二个测试分组");
            Log.d(TAG, "添加分组成功，ID: " + groupId1 + ", " + groupId2);
            
            // 2. 查询所有分组
            Log.d(TAG, "2. 查询所有分组");
            List<Group> groups = dbManager.getAllGroups();
            for (Group group : groups) {
                Log.d(TAG, "分组: " + group.toString());
            }
            
            // 3. 添加分类
            Log.d(TAG, "3. 添加分类");
            long categoryId1 = dbManager.addCategory("测试分类1", "分组1下的分类1", groupId1);
            long categoryId2 = dbManager.addCategory("测试分类2", "分组1下的分类2", groupId1);
            long categoryId3 = dbManager.addCategory("测试分类3", "分组2下的分类1", groupId2);
            Log.d(TAG, "添加分类成功，ID: " + categoryId1 + ", " + categoryId2 + ", " + categoryId3);
            
            // 4. 查询分组及其分类
            Log.d(TAG, "4. 查询分组及其分类");
            List<Group> groupsWithCategories = dbManager.getGroupsWithCategories();
            for (Group group : groupsWithCategories) {
                Log.d(TAG, "分组: " + group.getGroupName() + " (" + group.getGroupDescription() + ")");
                if (group.getCategories() != null) {
                    for (Category category : group.getCategories()) {
                        Log.d(TAG, "  └─ 分类: " + category.getCategoryName() + " (" + category.getCategoryDescription() + ")");
                    }
                }
            }
            
            // 5. 更新分组
            Log.d(TAG, "5. 更新分组");
            boolean updateResult = dbManager.updateGroup(groupId1, "更新后的分组1", "更新后的描述");
            Log.d(TAG, "更新分组结果: " + updateResult);
            
            // 6. 更新分类
            Log.d(TAG, "6. 更新分类");
            boolean updateCategoryResult = dbManager.updateCategory(categoryId1, "更新后的分类1", "更新后的分类描述", groupId1);
            Log.d(TAG, "更新分类结果: " + updateCategoryResult);
            
            // 7. 查询统计信息
            Log.d(TAG, "7. 查询统计信息");
            DatabaseManager.DatabaseStats stats = dbManager.getDatabaseStats();
            Log.d(TAG, "统计信息: " + stats.toString());
            
            // 8. 删除分类
            Log.d(TAG, "8. 删除分类");
            boolean deleteCategoryResult = dbManager.deleteCategory(categoryId3);
            Log.d(TAG, "删除分类结果: " + deleteCategoryResult);
            
            // 9. 删除分组（会级联删除相关分类）
            Log.d(TAG, "9. 删除分组");
            boolean deleteGroupResult = dbManager.deleteGroup(groupId2);
            Log.d(TAG, "删除分组结果: " + deleteGroupResult);
            
            // 10. 最终统计
            Log.d(TAG, "10. 最终统计");
            DatabaseManager.DatabaseStats finalStats = dbManager.getDatabaseStats();
            Log.d(TAG, "最终统计信息: " + finalStats.toString());
            
        } catch (Exception e) {
            Log.e(TAG, "操作失败: " + e.getMessage(), e);
        }
        
        Log.d(TAG, "=== 数据库操作演示完成 ===");
    }
    
    /**
     * 演示查询操作
     */
    public void demonstrateQueryOperations() {
        Log.d(TAG, "=== 开始演示查询操作 ===");
        
        try {
            // 1. 根据名称查询分组
            Log.d(TAG, "1. 根据名称查询分组");
            Group group = dbManager.getGroupByName("设备管理");
            if (group != null) {
                Log.d(TAG, "找到分组: " + group.toString());
                
                // 2. 查询该分组下的所有分类
                Log.d(TAG, "2. 查询分组下的分类");
                List<Category> categories = dbManager.getCategoriesByGroupId(group.getGroupId());
                for (Category category : categories) {
                    Log.d(TAG, "分类: " + category.toString());
                }
            } else {
                Log.d(TAG, "未找到指定分组");
            }
            
            // 3. 查询所有分类及其分组信息
            Log.d(TAG, "3. 查询所有分类及其分组信息");
            List<Category> categoriesWithGroup = dbManager.getCategoriesWithGroup();
            for (Category category : categoriesWithGroup) {
                Log.d(TAG, "分类: " + category.getCategoryName() + 
                          " -> 分组: " + (category.getGroup() != null ? category.getGroup().getGroupName() : "无"));
            }
            
            // 4. 检查名称是否存在
            Log.d(TAG, "4. 检查名称是否存在");
            boolean groupExists = dbManager.isGroupNameExists("设备管理");
            boolean categoryExists = dbManager.isCategoryNameExistsInGroup("终端设备", group != null ? group.getGroupId() : 1);
            Log.d(TAG, "分组'设备管理'是否存在: " + groupExists);
            Log.d(TAG, "分类'终端设备'在分组中是否存在: " + categoryExists);
            
        } catch (Exception e) {
            Log.e(TAG, "查询操作失败: " + e.getMessage(), e);
        }
        
        Log.d(TAG, "=== 查询操作演示完成 ===");
    }
    
    /**
     * 演示错误处理
     */
    public void demonstrateErrorHandling() {
        Log.d(TAG, "=== 开始演示错误处理 ===");
        
        try {
            // 1. 尝试添加重复的分组名称
            Log.d(TAG, "1. 尝试添加重复的分组名称");
            try {
                dbManager.addGroup("设备管理", "重复的分组名称");
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "捕获到预期的异常: " + e.getMessage());
            }
            
            // 2. 尝试在不存在的分组中添加分类
            Log.d(TAG, "2. 尝试在不存在的分组中添加分类");
            try {
                dbManager.addCategory("测试分类", "描述", 999999);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "捕获到预期的异常: " + e.getMessage());
            }
            
            // 3. 尝试添加重复的分类名称（在同一分组中）
            Log.d(TAG, "3. 尝试添加重复的分类名称");
            Group group = dbManager.getGroupByName("设备管理");
            if (group != null) {
                try {
                    dbManager.addCategory("终端设备", "重复的分类名称", group.getGroupId());
                } catch (IllegalArgumentException e) {
                    Log.d(TAG, "捕获到预期的异常: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "错误处理演示失败: " + e.getMessage(), e);
        }
        
        Log.d(TAG, "=== 错误处理演示完成 ===");
    }
}