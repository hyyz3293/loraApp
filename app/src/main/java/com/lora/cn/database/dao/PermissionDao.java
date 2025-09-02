package com.lora.cn.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.database.entity.Permission;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限数据访问对象
 */
public class PermissionDao {
    private DatabaseHelper dbHelper;

    public PermissionDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * 插入权限
     */
    public long insertPermission(Permission permission) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_PERMISSION_CODE, permission.getPermissionCode());
        values.put(DatabaseHelper.COLUMN_PERMISSION_NAME, permission.getPermissionName());
        values.put(DatabaseHelper.COLUMN_PERMISSION_CATEGORY, permission.getCategory());
        values.put(DatabaseHelper.COLUMN_PERMISSION_DESCRIPTION, permission.getDescription());
        values.put(DatabaseHelper.COLUMN_PERMISSION_STATUS, permission.getStatus());
        values.put(DatabaseHelper.COLUMN_PERMISSION_CREATE_TIME, permission.getCreateTime());
        values.put(DatabaseHelper.COLUMN_PERMISSION_UPDATE_TIME, permission.getUpdateTime());
        
        return db.insert(DatabaseHelper.TABLE_PERMISSIONS, null, values);
    }

    /**
     * 更新权限
     */
    public int updatePermission(Permission permission) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_PERMISSION_CODE, permission.getPermissionCode());
        values.put(DatabaseHelper.COLUMN_PERMISSION_NAME, permission.getPermissionName());
        values.put(DatabaseHelper.COLUMN_PERMISSION_CATEGORY, permission.getCategory());
        values.put(DatabaseHelper.COLUMN_PERMISSION_DESCRIPTION, permission.getDescription());
        values.put(DatabaseHelper.COLUMN_PERMISSION_STATUS, permission.getStatus());
        values.put(DatabaseHelper.COLUMN_PERMISSION_UPDATE_TIME, permission.getUpdateTime());
        
        return db.update(DatabaseHelper.TABLE_PERMISSIONS, values, 
                DatabaseHelper.COLUMN_PERMISSION_ID + "=?", 
                new String[]{String.valueOf(permission.getPermissionId())});
    }

    /**
     * 删除权限
     */
    public int deletePermission(int permissionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_PERMISSIONS, 
                DatabaseHelper.COLUMN_PERMISSION_ID + "=?", 
                new String[]{String.valueOf(permissionId)});
    }

    /**
     * 根据ID查询权限
     */
    public Permission getPermissionById(int permissionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_PERMISSIONS, null, 
                DatabaseHelper.COLUMN_PERMISSION_ID + "=?", 
                new String[]{String.valueOf(permissionId)}, 
                null, null, null);
        
        Permission permission = null;
        if (cursor.moveToFirst()) {
            permission = cursorToPermission(cursor);
        }
        cursor.close();
        return permission;
    }

    /**
     * 根据权限代码查询权限
     */
    public Permission getPermissionByCode(String permissionCode) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_PERMISSIONS, null, 
                DatabaseHelper.COLUMN_PERMISSION_CODE + "=?", 
                new String[]{permissionCode}, 
                null, null, null);
        
        Permission permission = null;
        if (cursor.moveToFirst()) {
            permission = cursorToPermission(cursor);
        }
        cursor.close();
        return permission;
    }

    /**
     * 查询所有权限
     */
    public List<Permission> getAllPermissions() {
        List<Permission> permissions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_PERMISSIONS, null, null, null, null, null, 
                DatabaseHelper.COLUMN_PERMISSION_CATEGORY + ", " + DatabaseHelper.COLUMN_PERMISSION_NAME);
        
        while (cursor.moveToNext()) {
            permissions.add(cursorToPermission(cursor));
        }
        cursor.close();
        return permissions;
    }

    /**
     * 根据分类查询权限
     */
    public List<Permission> getPermissionsByCategory(String category) {
        List<Permission> permissions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_PERMISSIONS, null, 
                DatabaseHelper.COLUMN_PERMISSION_CATEGORY + "=?", 
                new String[]{category}, 
                null, null, DatabaseHelper.COLUMN_PERMISSION_NAME);
        
        while (cursor.moveToNext()) {
            permissions.add(cursorToPermission(cursor));
        }
        cursor.close();
        return permissions;
    }

    /**
     * 查询启用的权限
     */
    public List<Permission> getActivePermissions() {
        List<Permission> permissions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_PERMISSIONS, null, 
                DatabaseHelper.COLUMN_PERMISSION_STATUS + "=?", 
                new String[]{"1"}, 
                null, null, 
                DatabaseHelper.COLUMN_PERMISSION_CATEGORY + ", " + DatabaseHelper.COLUMN_PERMISSION_NAME);
        
        while (cursor.moveToNext()) {
            permissions.add(cursorToPermission(cursor));
        }
        cursor.close();
        return permissions;
    }

    /**
     * 根据角色ID查询权限
     */
    public List<Permission> getPermissionsByRoleId(int roleId) {
        List<Permission> permissions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String sql = "SELECT p.* FROM " + DatabaseHelper.TABLE_PERMISSIONS + " p " +
                    "INNER JOIN " + DatabaseHelper.TABLE_ROLE_PERMISSIONS + " rp " +
                    "ON p." + DatabaseHelper.COLUMN_PERMISSION_ID + " = rp." + DatabaseHelper.COLUMN_RP_PERMISSION_ID + " " +
                    "WHERE rp." + DatabaseHelper.COLUMN_RP_ROLE_ID + " = ? " +
                    "ORDER BY p." + DatabaseHelper.COLUMN_PERMISSION_CATEGORY + ", p." + DatabaseHelper.COLUMN_PERMISSION_NAME;
        
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(roleId)});
        
        while (cursor.moveToNext()) {
            permissions.add(cursorToPermission(cursor));
        }
        cursor.close();
        return permissions;
    }

    /**
     * 获取所有权限分类
     */
    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(true, DatabaseHelper.TABLE_PERMISSIONS, 
                new String[]{DatabaseHelper.COLUMN_PERMISSION_CATEGORY}, 
                null, null, null, null, 
                DatabaseHelper.COLUMN_PERMISSION_CATEGORY, null);
        
        while (cursor.moveToNext()) {
            categories.add(cursor.getString(0));
        }
        cursor.close();
        return categories;
    }

    /**
     * 检查权限代码是否存在
     */
    public boolean isPermissionCodeExists(String permissionCode, int excludePermissionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COLUMN_PERMISSION_CODE + "=?";
        String[] selectionArgs;
        
        if (excludePermissionId > 0) {
            selection += " AND " + DatabaseHelper.COLUMN_PERMISSION_ID + "!=?";
            selectionArgs = new String[]{permissionCode, String.valueOf(excludePermissionId)};
        } else {
            selectionArgs = new String[]{permissionCode};
        }
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_PERMISSIONS, 
                new String[]{DatabaseHelper.COLUMN_PERMISSION_ID}, 
                selection, selectionArgs, null, null, null);
        
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * 将Cursor转换为Permission对象
     */
    private Permission cursorToPermission(Cursor cursor) {
        Permission permission = new Permission();
        permission.setPermissionId(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_PERMISSION_ID)));
        permission.setPermissionCode(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PERMISSION_CODE)));
        permission.setPermissionName(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PERMISSION_NAME)));
        permission.setCategory(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PERMISSION_CATEGORY)));
        permission.setDescription(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PERMISSION_DESCRIPTION)));
        permission.setStatus(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_PERMISSION_STATUS)));
        permission.setCreateTime(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PERMISSION_CREATE_TIME)));
        permission.setUpdateTime(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PERMISSION_UPDATE_TIME)));
        return permission;
    }
}