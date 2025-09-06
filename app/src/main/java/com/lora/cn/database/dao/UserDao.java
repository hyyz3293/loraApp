package com.lora.cn.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.database.entity.User;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 用户数据访问对象
 */
public class UserDao {
    private DatabaseHelper dbHelper;

    public UserDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * 插入用户
     */
    public long insertUser(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_NAME, user.getUserName());
        values.put(DatabaseHelper.COLUMN_USER_ACCOUNT, user.getUserAccount());
        values.put(DatabaseHelper.COLUMN_USER_PASSWORD, user.getUserPassword());
        values.put(DatabaseHelper.COLUMN_USER_ROLE_ID, user.getRoleId());
        values.put(DatabaseHelper.COLUMN_USER_STATUS, user.getStatus());
        values.put(DatabaseHelper.COLUMN_USER_POSITION_ID, user.getPositionId());
        values.put(DatabaseHelper.COLUMN_USER_DEPARTMENT_ID, user.getDepartmentId());
        values.put(DatabaseHelper.COLUMN_USER_CODE, user.getUserCode());
        values.put(DatabaseHelper.COLUMN_USER_GENDER, user.getGender());
        values.put(DatabaseHelper.COLUMN_USER_PHONE, user.getPhone());
        
        // 将Date转换为时间戳字符串存储
        if (user.getCreateTime() != null) {
            values.put(DatabaseHelper.COLUMN_USER_CREATE_TIME, String.valueOf(user.getCreateTime().getTime()));
        } else {
            values.put(DatabaseHelper.COLUMN_USER_CREATE_TIME, String.valueOf(System.currentTimeMillis()));
        }
        
        if (user.getUpdateTime() != null) {
            values.put(DatabaseHelper.COLUMN_USER_UPDATE_TIME, String.valueOf(user.getUpdateTime().getTime()));
        } else {
            values.put(DatabaseHelper.COLUMN_USER_UPDATE_TIME, String.valueOf(System.currentTimeMillis()));
        }
        
        return db.insert(DatabaseHelper.TABLE_USERS, null, values);
    }

    /**
     * 更新用户
     */
    public int updateUser(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_NAME, user.getUserName());
        values.put(DatabaseHelper.COLUMN_USER_ACCOUNT, user.getUserAccount());
        values.put(DatabaseHelper.COLUMN_USER_ROLE_ID, user.getRoleId());
        values.put(DatabaseHelper.COLUMN_USER_STATUS, user.getStatus());
        values.put(DatabaseHelper.COLUMN_USER_POSITION_ID, user.getPositionId());
        values.put(DatabaseHelper.COLUMN_USER_DEPARTMENT_ID, user.getDepartmentId());
        values.put(DatabaseHelper.COLUMN_USER_CODE, user.getUserCode());
        values.put(DatabaseHelper.COLUMN_USER_GENDER, user.getGender());
        values.put(DatabaseHelper.COLUMN_USER_PHONE, user.getPhone());
        
        // 将Date转换为时间戳字符串存储
        if (user.getUpdateTime() != null) {
            values.put(DatabaseHelper.COLUMN_USER_UPDATE_TIME, String.valueOf(user.getUpdateTime().getTime()));
        } else {
            values.put(DatabaseHelper.COLUMN_USER_UPDATE_TIME, String.valueOf(System.currentTimeMillis()));
        }
        
        return db.update(DatabaseHelper.TABLE_USERS, values, 
                DatabaseHelper.COLUMN_USER_ID + "=?", 
                new String[]{String.valueOf(user.getUserId())});
    }

    /**
     * 更新用户密码
     */
    public int updateUserPassword(int userId, String newPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_PASSWORD, newPassword);
        values.put(DatabaseHelper.COLUMN_USER_UPDATE_TIME, String.valueOf(System.currentTimeMillis()));
        
        return db.update(DatabaseHelper.TABLE_USERS, values, 
                DatabaseHelper.COLUMN_USER_ID + "=?", 
                new String[]{String.valueOf(userId)});
    }

    /**
     * 删除用户
     */
    public int deleteUser(int userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_USERS, 
                DatabaseHelper.COLUMN_USER_ID + "=?", 
                new String[]{String.valueOf(userId)});
    }

    /**
     * 根据ID查询用户
     */
    public User getUserById(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, 
                DatabaseHelper.COLUMN_USER_ID + "=?", 
                new String[]{String.valueOf(userId)}, 
                null, null, null);
        
        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }
        cursor.close();
        return user;
    }

    /**
     * 根据账号查询用户
     */
    public User getUserByAccount(String userAccount) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, 
                DatabaseHelper.COLUMN_USER_ACCOUNT + "=?", 
                new String[]{userAccount}, 
                null, null, null);
        
        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }
        cursor.close();
        return user;
    }

    /**
     * 查询所有用户
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, null, null, null, null, 
                DatabaseHelper.COLUMN_USER_CREATE_TIME + " DESC");
        
        while (cursor.moveToNext()) {
            users.add(cursorToUser(cursor));
        }
        cursor.close();
        return users;
    }

    /**
     * 查询启用的用户
     */
    public List<User> getActiveUsers() {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, 
                DatabaseHelper.COLUMN_USER_STATUS + "=?", 
                new String[]{"1"}, 
                null, null, DatabaseHelper.COLUMN_USER_CREATE_TIME + " DESC");
        
        while (cursor.moveToNext()) {
            users.add(cursorToUser(cursor));
        }
        cursor.close();
        return users;
    }

    /**
     * 根据角色ID查询用户
     */
    public List<User> getUsersByRoleId(int roleId) {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, 
                DatabaseHelper.COLUMN_USER_ROLE_ID + "=?", 
                new String[]{String.valueOf(roleId)}, 
                null, null, DatabaseHelper.COLUMN_USER_CREATE_TIME + " DESC");
        
        while (cursor.moveToNext()) {
            users.add(cursorToUser(cursor));
        }
        cursor.close();
        return users;
    }

    /**
     * 检查用户账号是否存在
     */
    public boolean isUserAccountExists(String userAccount, int excludeUserId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COLUMN_USER_ACCOUNT + "=?";
        String[] selectionArgs;
        
        if (excludeUserId > 0) {
            selection += " AND " + DatabaseHelper.COLUMN_USER_ID + "!=?";
            selectionArgs = new String[]{userAccount, String.valueOf(excludeUserId)};
        } else {
            selectionArgs = new String[]{userAccount};
        }
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, 
                new String[]{DatabaseHelper.COLUMN_USER_ID}, 
                selection, selectionArgs, null, null, null);
        
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * 用户登录验证
     */
    public User authenticateUser(String userAccount, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, 
                DatabaseHelper.COLUMN_USER_ACCOUNT + "=? AND " + 
                DatabaseHelper.COLUMN_USER_PASSWORD + "=? AND " + 
                DatabaseHelper.COLUMN_USER_STATUS + "=?", 
                new String[]{userAccount, password, "1"}, 
                null, null, null);
        
        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }
        cursor.close();
        return user;
    }

    /**
     * 将Cursor转换为User对象
     */
    private User cursorToUser(Cursor cursor) {
        User user = new User();
        user.setUserId(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ID)));
        user.setUserName(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_NAME)));
        user.setUserAccount(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ACCOUNT)));
        user.setUserPassword(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_PASSWORD)));
        user.setRoleId(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ROLE_ID)));
        user.setStatus(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_STATUS)));
        
        // 处理可能为null的字段
        int positionIdIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_POSITION_ID);
        if (!cursor.isNull(positionIdIndex)) {
            user.setPositionId(cursor.getInt(positionIdIndex));
        }
        
        int departmentIdIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_DEPARTMENT_ID);
        if (!cursor.isNull(departmentIdIndex)) {
            user.setDepartmentId(cursor.getInt(departmentIdIndex));
        }
        
        user.setUserCode(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_CODE)));
        user.setGender(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_GENDER)));
        user.setPhone(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_PHONE)));
        
        // 将时间戳字符串转换为Date对象
        String createTimeStr = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_CREATE_TIME));
        if (createTimeStr != null && !createTimeStr.isEmpty()) {
            try {
                user.setCreateTime(new Date(Long.parseLong(createTimeStr)));
            } catch (NumberFormatException e) {
                user.setCreateTime(new Date());
            }
        } else {
            user.setCreateTime(new Date());
        }
        
        String updateTimeStr = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_UPDATE_TIME));
        if (updateTimeStr != null && !updateTimeStr.isEmpty()) {
            try {
                user.setUpdateTime(new Date(Long.parseLong(updateTimeStr)));
            } catch (NumberFormatException e) {
                user.setUpdateTime(new Date());
            }
        } else {
            user.setUpdateTime(new Date());
        }
        
        return user;
    }
}