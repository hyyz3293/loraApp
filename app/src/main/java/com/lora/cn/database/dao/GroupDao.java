package com.lora.cn.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.database.entity.Group;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 分组数据访问对象
 */
public class GroupDao {
    private DatabaseHelper dbHelper;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    public GroupDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }
    
    /**
     * 插入新分组
     */
    public long insertGroup(Group group) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DatabaseHelper.COLUMN_GROUP_NAME, group.getGroupName());
        values.put(DatabaseHelper.COLUMN_GROUP_DESCRIPTION, group.getGroupDescription());
        
        long result = db.insert(DatabaseHelper.TABLE_GROUPS, null, values);
        
        return result;
    }
    
    /**
     * 根据ID查询分组
     */
    public Group getGroupById(long groupId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Group group = null;
        
        String selection = DatabaseHelper.COLUMN_GROUP_ID + " = ?";
        String[] selectionArgs = {String.valueOf(groupId)};
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_GROUPS, null, selection, selectionArgs, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            group = cursorToGroup(cursor);
            cursor.close();
        }
        
        return group;
    }
    
    /**
     * 根据名称查询分组
     */
    public Group getGroupByName(String groupName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Group group = null;
        
        String selection = DatabaseHelper.COLUMN_GROUP_NAME + " = ?";
        String[] selectionArgs = {groupName};
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_GROUPS, null, selection, selectionArgs, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            group = cursorToGroup(cursor);
            cursor.close();
        }
        
        return group;
    }
    
    /**
     * 获取所有分组
     */
    public List<Group> getAllGroups() {
        List<Group> groups = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String orderBy = DatabaseHelper.COLUMN_GROUP_CREATE_TIME + " ASC";
        Cursor cursor = db.query(DatabaseHelper.TABLE_GROUPS, null, null, null, null, null, orderBy);
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Group group = cursorToGroup(cursor);
                groups.add(group);
            }
            cursor.close();
        }
        
        return groups;
    }
    
    /**
     * 更新分组
     */
    public int updateGroup(Group group) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DatabaseHelper.COLUMN_GROUP_NAME, group.getGroupName());
        values.put(DatabaseHelper.COLUMN_GROUP_DESCRIPTION, group.getGroupDescription());
        values.put(DatabaseHelper.COLUMN_GROUP_UPDATE_TIME, dateFormat.format(new Date()));
        
        String whereClause = DatabaseHelper.COLUMN_GROUP_ID + " = ?";
        String[] whereArgs = {String.valueOf(group.getGroupId())};
        
        int result = db.update(DatabaseHelper.TABLE_GROUPS, values, whereClause, whereArgs);
        
        return result;
    }
    
    /**
     * 删除分组
     */
    public int deleteGroup(long groupId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        String whereClause = DatabaseHelper.COLUMN_GROUP_ID + " = ?";
        String[] whereArgs = {String.valueOf(groupId)};
        
        int result = db.delete(DatabaseHelper.TABLE_GROUPS, whereClause, whereArgs);
        db.close();
        
        return result;
    }
    
    /**
     * 检查分组名称是否存在
     */
    public boolean isGroupNameExists(String groupName) {
        return getGroupByName(groupName) != null;
    }
    
    /**
     * 检查分组名称是否存在（排除指定ID）
     */
    public boolean isGroupNameExists(String groupName, long excludeGroupId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        boolean exists = false;
        
        String selection = DatabaseHelper.COLUMN_GROUP_NAME + " = ? AND " + 
                          DatabaseHelper.COLUMN_GROUP_ID + " != ?";
        String[] selectionArgs = {groupName, String.valueOf(excludeGroupId)};
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_GROUPS, null, selection, selectionArgs, null, null, null);
        
        if (cursor != null) {
            exists = cursor.getCount() > 0;
            cursor.close();
        }
        
        db.close();
        return exists;
    }
    
    /**
     * 获取分组总数
     */
    public int getGroupCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int count = 0;
        
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_GROUPS, null);
        
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
     * 将Cursor转换为Group对象
     */
    private Group cursorToGroup(Cursor cursor) {
        Group group = new Group();
        
        group.setGroupId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GROUP_ID)));
        group.setGroupName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GROUP_NAME)));
        group.setGroupDescription(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GROUP_DESCRIPTION)));
        
        // 解析时间字段
        String createTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GROUP_CREATE_TIME));
        String updateTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GROUP_UPDATE_TIME));
        
        try {
            if (createTimeStr != null) {
                group.setCreateTime(dateFormat.parse(createTimeStr));
            }
            if (updateTimeStr != null) {
                group.setUpdateTime(dateFormat.parse(updateTimeStr));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        return group;
    }
}