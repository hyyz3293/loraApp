package com.lora.cn.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.database.entity.RolePermission;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色权限关联数据访问对象
 */
public class RolePermissionDao {
    private DatabaseHelper dbHelper;

    public RolePermissionDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * 添加角色权限关联
     */
    public long insertRolePermission(RolePermission rolePermission) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_RP_ROLE_ID, rolePermission.getRoleId());
        values.put(DatabaseHelper.COLUMN_RP_PERMISSION_ID, rolePermission.getPermissionId());
        values.put(DatabaseHelper.COLUMN_RP_CREATE_TIME, rolePermission.getCreateTime());
        
        return db.insert(DatabaseHelper.TABLE_ROLE_PERMISSIONS, null, values);
    }

    /**
     * 删除角色权限关联
     */
    public int deleteRolePermission(int roleId, int permissionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_ROLE_PERMISSIONS, 
                DatabaseHelper.COLUMN_RP_ROLE_ID + "=? AND " + DatabaseHelper.COLUMN_RP_PERMISSION_ID + "=?", 
                new String[]{String.valueOf(roleId), String.valueOf(permissionId)});
    }

    /**
     * 删除角色的所有权限
     */
    public int deleteRolePermissionsByRoleId(int roleId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_ROLE_PERMISSIONS, 
                DatabaseHelper.COLUMN_RP_ROLE_ID + "=?", 
                new String[]{String.valueOf(roleId)});
    }

    /**
     * 删除权限的所有关联
     */
    public int deleteRolePermissionsByPermissionId(int permissionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_ROLE_PERMISSIONS, 
                DatabaseHelper.COLUMN_RP_PERMISSION_ID + "=?", 
                new String[]{String.valueOf(permissionId)});
    }

    /**
     * 批量设置角色权限
     */
    public boolean setRolePermissions(int roleId, List<Integer> permissionIds) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        
        try {
            // 先删除该角色的所有权限
            deleteRolePermissionsByRoleId(roleId);
            
            // 再添加新的权限
            String currentTime = String.valueOf(System.currentTimeMillis());
            for (Integer permissionId : permissionIds) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permissionId);
                rolePermission.setCreateTime(currentTime);
                
                insertRolePermission(rolePermission);
            }
            
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * 检查角色是否拥有某个权限
     */
    public boolean hasPermission(int roleId, int permissionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_ROLE_PERMISSIONS, 
                new String[]{DatabaseHelper.COLUMN_RP_ID}, 
                DatabaseHelper.COLUMN_RP_ROLE_ID + "=? AND " + DatabaseHelper.COLUMN_RP_PERMISSION_ID + "=?", 
                new String[]{String.valueOf(roleId), String.valueOf(permissionId)}, 
                null, null, null);
        
        boolean hasPermission = cursor.getCount() > 0;
        cursor.close();
        return hasPermission;
    }

    /**
     * 检查角色是否拥有某个权限（通过权限代码）
     */
    public boolean hasPermissionByCode(int roleId, String permissionCode) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String sql = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_ROLE_PERMISSIONS + " rp " +
                    "INNER JOIN " + DatabaseHelper.TABLE_PERMISSIONS + " p " +
                    "ON rp." + DatabaseHelper.COLUMN_RP_PERMISSION_ID + " = p." + DatabaseHelper.COLUMN_PERMISSION_ID + " " +
                    "WHERE rp." + DatabaseHelper.COLUMN_RP_ROLE_ID + " = ? " +
                    "AND p." + DatabaseHelper.COLUMN_PERMISSION_CODE + " = ? " +
                    "AND p." + DatabaseHelper.COLUMN_PERMISSION_STATUS + " = 1";
        
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(roleId), permissionCode});
        
        boolean hasPermission = false;
        if (cursor.moveToFirst()) {
            hasPermission = cursor.getInt(0) > 0;
        }
        cursor.close();
        return hasPermission;
    }

    /**
     * 获取角色的权限ID列表
     */
    public List<Integer> getPermissionIdsByRoleId(int roleId) {
        List<Integer> permissionIds = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_ROLE_PERMISSIONS, 
                new String[]{DatabaseHelper.COLUMN_RP_PERMISSION_ID}, 
                DatabaseHelper.COLUMN_RP_ROLE_ID + "=?", 
                new String[]{String.valueOf(roleId)}, 
                null, null, null);
        
        while (cursor.moveToNext()) {
            permissionIds.add(cursor.getInt(0));
        }
        cursor.close();
        return permissionIds;
    }

    /**
     * 获取拥有某个权限的角色ID列表
     */
    public List<Integer> getRoleIdsByPermissionId(int permissionId) {
        List<Integer> roleIds = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_ROLE_PERMISSIONS, 
                new String[]{DatabaseHelper.COLUMN_RP_ROLE_ID}, 
                DatabaseHelper.COLUMN_RP_PERMISSION_ID + "=?", 
                new String[]{String.valueOf(permissionId)}, 
                null, null, null);
        
        while (cursor.moveToNext()) {
            roleIds.add(cursor.getInt(0));
        }
        cursor.close();
        return roleIds;
    }

    /**
     * 获取所有角色权限关联
     */
    public List<RolePermission> getAllRolePermissions() {
        List<RolePermission> rolePermissions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_ROLE_PERMISSIONS, null, null, null, null, null, 
                DatabaseHelper.COLUMN_RP_CREATE_TIME + " DESC");
        
        while (cursor.moveToNext()) {
            rolePermissions.add(cursorToRolePermission(cursor));
        }
        cursor.close();
        return rolePermissions;
    }

    /**
     * 根据角色ID获取角色权限关联
     */
    public List<RolePermission> getRolePermissionsByRoleId(int roleId) {
        List<RolePermission> rolePermissions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_ROLE_PERMISSIONS, null, 
                DatabaseHelper.COLUMN_RP_ROLE_ID + "=?", 
                new String[]{String.valueOf(roleId)}, 
                null, null, DatabaseHelper.COLUMN_RP_CREATE_TIME + " DESC");
        
        while (cursor.moveToNext()) {
            rolePermissions.add(cursorToRolePermission(cursor));
        }
        cursor.close();
        return rolePermissions;
    }

    /**
     * 将Cursor转换为RolePermission对象
     */
    private RolePermission cursorToRolePermission(Cursor cursor) {
        RolePermission rolePermission = new RolePermission();
        rolePermission.setId(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_RP_ID)));
        rolePermission.setRoleId(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_RP_ROLE_ID)));
        rolePermission.setPermissionId(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_RP_PERMISSION_ID)));
        rolePermission.setCreateTime(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_RP_CREATE_TIME)));
        return rolePermission;
    }
}