package com.lora.cn.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.database.entity.Category;
import com.lora.cn.database.entity.Group;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 分类数据访问对象
 */
public class CategoryDao {
    private DatabaseHelper dbHelper;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    public CategoryDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }
    
    /**
     * 插入新分类
     */
    public long insertCategory(Category category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DatabaseHelper.COLUMN_CATEGORY_NAME, category.getCategoryName());
        values.put(DatabaseHelper.COLUMN_CATEGORY_DESCRIPTION, category.getCategoryDescription());
        values.put(DatabaseHelper.COLUMN_CATEGORY_GROUP_ID, category.getGroupId());
        
        long result = db.insert(DatabaseHelper.TABLE_CATEGORIES, null, values);
        
        return result;
    }
    
    /**
     * 根据ID查询分类
     */
    public Category getCategoryById(long categoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Category category = null;
        
        String selection = DatabaseHelper.COLUMN_CATEGORY_ID + " = ?";
        String[] selectionArgs = {String.valueOf(categoryId)};
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_CATEGORIES, null, selection, selectionArgs, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            category = cursorToCategory(cursor);
            cursor.close();
        }
        
        return category;
    }
    
    /**
     * 根据分组ID查询所有分类
     */
    public List<Category> getCategoriesByGroupId(long groupId) {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String selection = DatabaseHelper.COLUMN_CATEGORY_GROUP_ID + " = ?";
        String[] selectionArgs = {String.valueOf(groupId)};
        String orderBy = DatabaseHelper.COLUMN_CATEGORY_CREATE_TIME + " ASC";
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_CATEGORIES, null, selection, selectionArgs, null, null, orderBy);
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Category category = cursorToCategory(cursor);
                categories.add(category);
            }
            cursor.close();
        }
        
        return categories;
    }
    
    /**
     * 获取所有分类
     */
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String orderBy = DatabaseHelper.COLUMN_CATEGORY_GROUP_ID + " ASC, " + 
                        DatabaseHelper.COLUMN_CATEGORY_CREATE_TIME + " ASC";
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_CATEGORIES, null, null, null, null, null, orderBy);
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Category category = cursorToCategory(cursor);
                categories.add(category);
            }
            cursor.close();
        }
        
        return categories;
    }
    
    /**
     * 获取分类及其关联的分组信息
     */
    public List<Category> getCategoriesWithGroup() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String sql = "SELECT c.*, g." + DatabaseHelper.COLUMN_GROUP_NAME + ", g." + 
                    DatabaseHelper.COLUMN_GROUP_DESCRIPTION + ", g." + 
                    DatabaseHelper.COLUMN_GROUP_CREATE_TIME + " as group_create_time, g." + 
                    DatabaseHelper.COLUMN_GROUP_UPDATE_TIME + " as group_update_time " +
                    "FROM " + DatabaseHelper.TABLE_CATEGORIES + " c " +
                    "LEFT JOIN " + DatabaseHelper.TABLE_GROUPS + " g ON c." + 
                    DatabaseHelper.COLUMN_CATEGORY_GROUP_ID + " = g." + DatabaseHelper.COLUMN_GROUP_ID + 
                    " ORDER BY g." + DatabaseHelper.COLUMN_GROUP_ID + " ASC, c." + 
                    DatabaseHelper.COLUMN_CATEGORY_CREATE_TIME + " ASC";
        
        Cursor cursor = db.rawQuery(sql, null);
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Category category = cursorToCategory(cursor);
                
                // 设置关联的分组信息
                Group group = new Group();
                group.setGroupId(category.getGroupId());
                group.setGroupName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GROUP_NAME)));
                group.setGroupDescription(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GROUP_DESCRIPTION)));
                
                // 解析分组时间
                try {
                    String groupCreateTimeStr = cursor.getString(cursor.getColumnIndexOrThrow("group_create_time"));
                    String groupUpdateTimeStr = cursor.getString(cursor.getColumnIndexOrThrow("group_update_time"));
                    
                    if (groupCreateTimeStr != null) {
                        group.setCreateTime(dateFormat.parse(groupCreateTimeStr));
                    }
                    if (groupUpdateTimeStr != null) {
                        group.setUpdateTime(dateFormat.parse(groupUpdateTimeStr));
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                
                category.setGroup(group);
                categories.add(category);
            }
            cursor.close();
        }
        
        return categories;
    }
    
    /**
     * 更新分类
     */
    public int updateCategory(Category category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DatabaseHelper.COLUMN_CATEGORY_NAME, category.getCategoryName());
        values.put(DatabaseHelper.COLUMN_CATEGORY_DESCRIPTION, category.getCategoryDescription());
        values.put(DatabaseHelper.COLUMN_CATEGORY_GROUP_ID, category.getGroupId());
        values.put(DatabaseHelper.COLUMN_CATEGORY_UPDATE_TIME, dateFormat.format(new Date()));
        
        String whereClause = DatabaseHelper.COLUMN_CATEGORY_ID + " = ?";
        String[] whereArgs = {String.valueOf(category.getCategoryId())};
        
        int result = db.update(DatabaseHelper.TABLE_CATEGORIES, values, whereClause, whereArgs);
        
        return result;
    }
    
    /**
     * 删除分类
     */
    public int deleteCategory(long categoryId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        String whereClause = DatabaseHelper.COLUMN_CATEGORY_ID + " = ?";
        String[] whereArgs = {String.valueOf(categoryId)};
        
        int result = db.delete(DatabaseHelper.TABLE_CATEGORIES, whereClause, whereArgs);
        
        return result;
    }
    
    /**
     * 根据分组ID删除所有分类
     */
    public int deleteCategoriesByGroupId(long groupId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        String whereClause = DatabaseHelper.COLUMN_CATEGORY_GROUP_ID + " = ?";
        String[] whereArgs = {String.valueOf(groupId)};
        
        int result = db.delete(DatabaseHelper.TABLE_CATEGORIES, whereClause, whereArgs);
        
        return result;
    }
    
    /**
     * 检查分类名称在指定分组中是否存在
     */
    public boolean isCategoryNameExistsInGroup(String categoryName, long groupId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        boolean exists = false;
        
        String selection = DatabaseHelper.COLUMN_CATEGORY_NAME + " = ? AND " + 
                          DatabaseHelper.COLUMN_CATEGORY_GROUP_ID + " = ?";
        String[] selectionArgs = {categoryName, String.valueOf(groupId)};
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_CATEGORIES, null, selection, selectionArgs, null, null, null);
        
        if (cursor != null) {
            exists = cursor.getCount() > 0;
            cursor.close();
        }
        
        return exists;
    }
    
    /**
     * 检查分类名称在指定分组中是否存在（排除指定ID）
     */
    public boolean isCategoryNameExistsInGroup(String categoryName, long groupId, long excludeCategoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        boolean exists = false;
        
        String selection = DatabaseHelper.COLUMN_CATEGORY_NAME + " = ? AND " + 
                          DatabaseHelper.COLUMN_CATEGORY_GROUP_ID + " = ? AND " +
                          DatabaseHelper.COLUMN_CATEGORY_ID + " != ?";
        String[] selectionArgs = {categoryName, String.valueOf(groupId), String.valueOf(excludeCategoryId)};
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_CATEGORIES, null, selection, selectionArgs, null, null, null);
        
        if (cursor != null) {
            exists = cursor.getCount() > 0;
            cursor.close();
        }
        
        return exists;
    }
    
    /**
     * 获取指定分组的分类总数
     */
    public int getCategoryCountByGroupId(long groupId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int count = 0;
        
        String sql = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_CATEGORIES + 
                    " WHERE " + DatabaseHelper.COLUMN_CATEGORY_GROUP_ID + " = ?";
        String[] selectionArgs = {String.valueOf(groupId)};
        
        Cursor cursor = db.rawQuery(sql, selectionArgs);
        
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        
        db.close();
        return count;
    }
    
    /**
     * 获取分类总数
     */
    public int getCategoryCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int count = 0;
        
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_CATEGORIES, null);
        
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        
        db.close();
        return count;
    }
    
    /**
     * 将Cursor转换为Category对象
     */
    private Category cursorToCategory(Cursor cursor) {
        Category category = new Category();
        
        category.setCategoryId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_ID)));
        category.setCategoryName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_NAME)));
        category.setCategoryDescription(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_DESCRIPTION)));
        category.setGroupId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_GROUP_ID)));
        
        // 解析时间字段
        String createTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_CREATE_TIME));
        String updateTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_UPDATE_TIME));
        
        try {
            if (createTimeStr != null) {
                category.setCreateTime(dateFormat.parse(createTimeStr));
            }
            if (updateTimeStr != null) {
                category.setUpdateTime(dateFormat.parse(updateTimeStr));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        return category;
    }
}