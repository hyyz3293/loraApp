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
        // 设备CODE
        values.put(DatabaseHelper.COLUMN_TERMINAL_DEVICE_CODE, terminal.getDeviceCode());
        values.put(DatabaseHelper.COLUMN_TERMINAL_STATUS, com.lora.cn.ui.constants.TerminalStatusConstants.codeToText(terminal.getStatus()));
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
            int codeIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEVICE_CODE);
            if (codeIdx != -1) {
                terminal.setDeviceCode(cursor.getString(codeIdx));
            }
            {
                String st = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_STATUS));
                terminal.setStatus(com.lora.cn.ui.constants.TerminalStatusConstants.textToCode(st));
            }
            terminal.setSignalStrength(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_SIGNAL_STRENGTH)));
            terminal.setDepartment(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT)));
            terminal.setLocation(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_LOCATION)));
            terminal.setDepartmentId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT_ID)));
            terminal.setRoomId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_ROOM_ID)));
            terminal.setNursingGroupId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_NURSING_GROUP_ID)));
            terminal.setOtherId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_OTHER_ID)));
            terminal.setExtension(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_EXTENSION)));
            int fav = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_IS_FAVORITE));
            int favUserId = 0;
            int favUserIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_FAVORITE_USER_ID);
            if (favUserIdx != -1) favUserId = cursor.getInt(favUserIdx);
            long currentUserId = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
            terminal.setFavorite(fav == 1 && favUserId == (int) currentUserId);
            terminal.setCreateTime(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_CREATE_TIME)));
            terminal.setUpdateTime(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_UPDATE_TIME)));
            // 读取电量、电压、RSSI
            int blIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_BATTERY_LEVEL);
            if (blIdx != -1) terminal.setBatteryLevel(cursor.getInt(blIdx));
            terminal.setBatteryStatus(terminal.getBatteryLevel() <= 20 ? 0 : 1);
            int bvIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_BATTERY_VOLTAGE);
            if (bvIdx != -1) terminal.setBatteryVoltage(cursor.getInt(bvIdx));
            int rssiIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_RSSI);
            if (rssiIdx != -1) terminal.setRssi(cursor.getInt(rssiIdx));
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
                int codeIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEVICE_CODE);
                if (codeIdx != -1) {
                    terminal.setDeviceCode(cursor.getString(codeIdx));
                }
                {
                    String st = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_STATUS));
                    terminal.setStatus(com.lora.cn.ui.constants.TerminalStatusConstants.textToCode(st));
                }
                terminal.setSignalStrength(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_SIGNAL_STRENGTH)));
                terminal.setDepartment(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT)));
                terminal.setLocation(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_LOCATION)));
                terminal.setDepartmentId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT_ID)));
                terminal.setRoomId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_ROOM_ID)));
                terminal.setNursingGroupId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_NURSING_GROUP_ID)));
                terminal.setOtherId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_OTHER_ID)));
                terminal.setExtension(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_EXTENSION)));
                int fav2 = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_IS_FAVORITE));
                int favUserId2 = 0;
                int favUserIdx2 = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_FAVORITE_USER_ID);
                if (favUserIdx2 != -1) favUserId2 = cursor.getInt(favUserIdx2);
                long currentUserId2 = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
                terminal.setFavorite(fav2 == 1 && favUserId2 == (int) currentUserId2);
                terminal.setCreateTime(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_CREATE_TIME)));
                terminal.setUpdateTime(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_UPDATE_TIME)));
                // 读取电量、电压、RSSI
                int blIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_BATTERY_LEVEL);
                if (blIdx != -1) terminal.setBatteryLevel(cursor.getInt(blIdx));
                terminal.setBatteryStatus(terminal.getBatteryLevel() <= 20 ? 0 : 1);
                int bvIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_BATTERY_VOLTAGE);
                if (bvIdx != -1) terminal.setBatteryVoltage(cursor.getInt(bvIdx));
                int rssiIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_RSSI);
                if (rssiIdx != -1) terminal.setRssi(cursor.getInt(rssiIdx));
                
                terminals.add(terminal);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return terminals;
    }
}
