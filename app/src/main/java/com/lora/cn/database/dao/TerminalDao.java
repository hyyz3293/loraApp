package com.lora.cn.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.ui.model.Terminal;


import java.util.ArrayList;
import java.util.List;

/**
 * 终端数据访问对象
 */
public class TerminalDao {
    
    private DatabaseHelper dbHelper;
    
    public TerminalDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }
    
    /**
     * 插入终端
     */
    public long insertTerminal(Terminal terminal) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DatabaseHelper.COLUMN_TERMINAL_DEVICE_ID, terminal.getTerminalId());
        values.put(DatabaseHelper.COLUMN_TERMINAL_NAME, terminal.getTerminalName());
        values.put(DatabaseHelper.COLUMN_TERMINAL_STATUS, terminal.getStatus());
        values.put(DatabaseHelper.COLUMN_TERMINAL_SIGNAL_STRENGTH, terminal.getSignalStrength());
        values.put(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT, terminal.getDepartment());
        values.put(DatabaseHelper.COLUMN_TERMINAL_LOCATION, terminal.getLocation());
        // 仅当分类ID为有效正数时写入，避免外键约束失败
        if (terminal.getDepartmentId() > 0) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT_ID, terminal.getDepartmentId());
        }
        if (terminal.getRoomId() > 0) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_ROOM_ID, terminal.getRoomId());
        }
        if (terminal.getNursingGroupId() > 0) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_NURSING_GROUP_ID, terminal.getNursingGroupId());
        }
        if (terminal.getOtherId() > 0) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_OTHER_ID, terminal.getOtherId());
        }
        values.put(DatabaseHelper.COLUMN_TERMINAL_EXTENSION, terminal.getExtension());
        values.put(DatabaseHelper.COLUMN_TERMINAL_IS_FAVORITE, terminal.isFavorite() ? 1 : 0);
        values.put(DatabaseHelper.COLUMN_TERMINAL_CREATE_TIME, System.currentTimeMillis());
        values.put(DatabaseHelper.COLUMN_TERMINAL_UPDATE_TIME, System.currentTimeMillis());
        
        return db.insert(DatabaseHelper.TABLE_TERMINALS, null, values);
    }
    
    /**
     * 更新终端收藏状态
     */
    public int updateTerminalFavoriteStatus(String terminalId, boolean isFavorite) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DatabaseHelper.COLUMN_TERMINAL_IS_FAVORITE, isFavorite ? 1 : 0);
        values.put(DatabaseHelper.COLUMN_TERMINAL_UPDATE_TIME, System.currentTimeMillis());
        
        int result = db.update(DatabaseHelper.TABLE_TERMINALS, values, 
                DatabaseHelper.COLUMN_TERMINAL_DEVICE_ID + "=?", 
                new String[]{terminalId});
        
        // 记录收藏状态变更日志
        try {
            com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
            logInfo.setTerminalId(terminalId);
            logInfo.setTerminalName(""); // 这里可以通过查询获取终端名称
            logInfo.setDeviceId(terminalId);
            logInfo.setStatus(result > 0 ? "成功" : "失败");
            logInfo.setOperator("系统管理员"); // 这里可以根据实际登录用户设置
            logInfo.setAction(isFavorite ? "收藏终端" : "取消收藏");
            logInfo.setOperationTime(String.valueOf(System.currentTimeMillis()));
            logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
            
            dbHelper.addLog(logInfo);
        } catch (Exception e) {
            // 日志记录失败不影响主要操作
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * 检查设备ID是否存在
     */
    public boolean isDeviceIdExists(String deviceId, long excludeId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TERMINALS + 
                      " WHERE " + DatabaseHelper.COLUMN_TERMINAL_DEVICE_ID + "=?";
        
        if (excludeId > 0) {
            query += " AND " + DatabaseHelper.COLUMN_TERMINAL_ID + "!=?";
        }
        
        Cursor cursor = db.rawQuery(query, excludeId > 0 ? 
                new String[]{deviceId, String.valueOf(excludeId)} : 
                new String[]{deviceId});
        
        boolean exists = false;
        if (cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0;
        }
        cursor.close();
        return exists;
    }
    
    /**
     * 根据终端ID获取终端信息
     */
    public Terminal getTerminalByDeviceId(String deviceId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_TERMINALS + 
                      " WHERE " + DatabaseHelper.COLUMN_TERMINAL_DEVICE_ID + "=?";
        
        Cursor cursor = db.rawQuery(query, new String[]{deviceId});
        Terminal terminal = null;
        
        if (cursor.moveToFirst()) {
            terminal = new Terminal();
            terminal.setId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_ID)));
            terminal.setTerminalId(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEVICE_ID)));
            terminal.setTerminalName(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_NAME)));
            terminal.setStatus(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_STATUS)));
            terminal.setSignalStrength(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_SIGNAL_STRENGTH)));
            terminal.setDepartment(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT)));
            terminal.setLocation(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_LOCATION)));
            terminal.setDepartmentId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT_ID)));
            terminal.setRoomId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_ROOM_ID)));
            terminal.setNursingGroupId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_NURSING_GROUP_ID)));
            terminal.setOtherId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_OTHER_ID)));
            terminal.setExtension(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_EXTENSION)));
            terminal.setFavorite(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_IS_FAVORITE)) == 1);
            terminal.setCreateTime(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_CREATE_TIME)));
            terminal.setUpdateTime(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_UPDATE_TIME)));
        }
        
        cursor.close();
        return terminal;
    }
    
    /**
     * 获取所有终端
     */
    public List<Terminal> getAllTerminals() {
        List<Terminal> terminals = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_TERMINALS + 
                      " ORDER BY " + DatabaseHelper.COLUMN_TERMINAL_CREATE_TIME + " DESC";
        Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                Terminal terminal = new Terminal();
                terminal.setId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_ID)));
                terminal.setTerminalId(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEVICE_ID)));
                terminal.setTerminalName(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_NAME)));
                terminal.setStatus(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_STATUS)));
                terminal.setSignalStrength(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_SIGNAL_STRENGTH)));
                terminal.setDepartment(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT)));
                terminal.setLocation(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_LOCATION)));
                terminal.setDepartmentId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT_ID)));
                terminal.setRoomId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_ROOM_ID)));
                terminal.setNursingGroupId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_NURSING_GROUP_ID)));
                terminal.setOtherId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_OTHER_ID)));
                terminal.setExtension(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_EXTENSION)));
                terminal.setFavorite(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_IS_FAVORITE)) == 1);
                terminal.setCreateTime(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_CREATE_TIME)));
                terminal.setUpdateTime(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_UPDATE_TIME)));
                
                terminals.add(terminal);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return terminals;
    }
}