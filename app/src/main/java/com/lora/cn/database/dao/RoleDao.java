package com.lora.cn.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.database.entity.Role;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色数据访问对象
 */
public class RoleDao {
    private DatabaseHelper dbHelper;

    public RoleDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * 插入角色
     */
    public long insertRole(Role role) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ROLE_NAME, role.getRoleName());
        values.put(DatabaseHelper.COLUMN_ROLE_DESCRIPTION, role.getDescription());
        values.put(DatabaseHelper.COLUMN_ROLE_SORT_ORDER, role.getSortOrder());
        values.put(DatabaseHelper.COLUMN_ROLE_STATUS, role.getStatus());
        values.put(DatabaseHelper.COLUMN_ROLE_CREATE_TIME, role.getCreateTime());
        values.put(DatabaseHelper.COLUMN_ROLE_UPDATE_TIME, role.getUpdateTime());
        
        return db.insert(DatabaseHelper.TABLE_ROLES, null, values);
    }

    /**
     * 更新角色
     */
    public int updateRole(Role role) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ROLE_NAME, role.getRoleName());
        values.put(DatabaseHelper.COLUMN_ROLE_DESCRIPTION, role.getDescription());
        values.put(DatabaseHelper.COLUMN_ROLE_SORT_ORDER, role.getSortOrder());
        values.put(DatabaseHelper.COLUMN_ROLE_STATUS, role.getStatus());
        values.put(DatabaseHelper.COLUMN_ROLE_UPDATE_TIME, role.getUpdateTime());
        
        return db.update(DatabaseHelper.TABLE_ROLES, values, 
                DatabaseHelper.COLUMN_ROLE_ID + "=?", 
                new String[]{String.valueOf(role.getRoleId())});
    }

    /**
     * 删除角色
     */
    public int deleteRole(int roleId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_ROLES, 
                DatabaseHelper.COLUMN_ROLE_ID + "=?", 
                new String[]{String.valueOf(roleId)});
    }

    /**
     * 根据ID查询角色
     */
    public Role getRoleById(int roleId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_ROLES, null, 
                DatabaseHelper.COLUMN_ROLE_ID + "=?", 
                new String[]{String.valueOf(roleId)}, 
                null, null, null);
        
        Role role = null;
        if (cursor.moveToFirst()) {
            role = cursorToRole(cursor);
        }
        cursor.close();
        return role;
    }

    /**
     * 查询所有角色
     */
    public List<Role> getAllRoles() {
        List<Role> roles = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_ROLES, null, null, null, null, null, 
                DatabaseHelper.COLUMN_ROLE_CREATE_TIME + " DESC");
        
        while (cursor.moveToNext()) {
            roles.add(cursorToRole(cursor));
        }
        cursor.close();
        return roles;
    }

    /**
     * 查询启用的角色
     */
    public List<Role> getActiveRoles() {
        List<Role> roles = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_ROLES, null, 
                DatabaseHelper.COLUMN_ROLE_STATUS + "=?", 
                new String[]{"1"}, 
                null, null, DatabaseHelper.COLUMN_ROLE_CREATE_TIME + " DESC");
        
        while (cursor.moveToNext()) {
            roles.add(cursorToRole(cursor));
        }
        cursor.close();
        return roles;
    }

    /**
     * 根据角色名称查询角色
     */
    public Role getRoleByName(String roleName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_ROLES, null, 
                DatabaseHelper.COLUMN_ROLE_NAME + "=?", 
                new String[]{roleName}, 
                null, null, null);
        
        Role role = null;
        if (cursor.moveToFirst()) {
            role = cursorToRole(cursor);
        }
        cursor.close();
        return role;
    }

    /**
     * 检查角色名称是否存在
     */
    public boolean isRoleNameExists(String roleName, int excludeRoleId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COLUMN_ROLE_NAME + "=?";
        String[] selectionArgs;
        
        if (excludeRoleId > 0) {
            selection += " AND " + DatabaseHelper.COLUMN_ROLE_ID + "!=?";
            selectionArgs = new String[]{roleName, String.valueOf(excludeRoleId)};
        } else {
            selectionArgs = new String[]{roleName};
        }
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_ROLES, 
                new String[]{DatabaseHelper.COLUMN_ROLE_ID}, 
                selection, selectionArgs, null, null, null);
        
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * 获取角色总数
     */
    public int getRoleCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_ROLES, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * 将Cursor转换为Role对象
     */
    private Role cursorToRole(Cursor cursor) {
        Role role = new Role();
        role.setRoleId(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ROLE_ID)));
        role.setRoleName(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ROLE_NAME)));
        role.setDescription(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ROLE_DESCRIPTION)));
        role.setSortOrder(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ROLE_SORT_ORDER)));
        role.setStatus(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ROLE_STATUS)));
        role.setCreateTime(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ROLE_CREATE_TIME)));
        role.setUpdateTime(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ROLE_UPDATE_TIME)));
        return role;
    }
}