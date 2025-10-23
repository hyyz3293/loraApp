package com.lora.cn.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.database.entity.Terminal;
import java.util.ArrayList;
import java.util.Date;
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
        values.put(DatabaseHelper.COLUMN_TERMINAL_DEVICE_ID, terminal.getDeviceId());
        values.put(DatabaseHelper.COLUMN_TERMINAL_NAME, terminal.getDeviceName());
        values.put(DatabaseHelper.COLUMN_TERMINAL_STATUS, terminal.getStatus());
        values.put(DatabaseHelper.COLUMN_TERMINAL_SIGNAL_STRENGTH, terminal.getSignalStrength());
        values.put(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT, terminal.getDepartment());
        values.put(DatabaseHelper.COLUMN_TERMINAL_LOCATION, terminal.getLocation());
        
        // 分类ID字段
        if (terminal.getDepartmentId() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT_ID, terminal.getDepartmentId());
        }
        if (terminal.getRoomId() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_ROOM_ID, terminal.getRoomId());
        }
        if (terminal.getNursingGroupId() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_NURSING_GROUP_ID, terminal.getNursingGroupId());
        }
        if (terminal.getOtherId() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_OTHER_ID, terminal.getOtherId());
        }
        
        values.put(DatabaseHelper.COLUMN_TERMINAL_EXTENSION, terminal.getExtension());
        
        // 上行数据相关字段
        if (terminal.getDataTime() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DATA_TIME, String.valueOf(terminal.getDataTime().getTime()));
        }
        if (terminal.getDeviceEvent() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DEVICE_EVENT, terminal.getDeviceEvent());
        }
        if (terminal.getDeviceStatus() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DEVICE_STATUS, terminal.getDeviceStatus());
        }
        if (terminal.getBatteryVoltage() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_BATTERY_VOLTAGE, terminal.getBatteryVoltage());
        }
        if (terminal.getBatteryLevel() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_BATTERY_LEVEL, terminal.getBatteryLevel());
        }
        if (terminal.getRssi() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_RSSI, terminal.getRssi());
        }
        if (terminal.getDepartmentNumber() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT_NUMBER, terminal.getDepartmentNumber());
        }
        if (terminal.getCartNumber() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_CART_NUMBER, terminal.getCartNumber());
        }
        if (terminal.getDeviceCount() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DEVICE_COUNT, terminal.getDeviceCount());
        }
        if (terminal.getRackNumber() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_RACK_NUMBER, terminal.getRackNumber());
        }
        if (terminal.getFunctionCode() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_FUNCTION_CODE, terminal.getFunctionCode());
        }
        if (terminal.getSequenceNumber() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_SEQUENCE_NUMBER, terminal.getSequenceNumber());
        }
        if (terminal.getDataLength() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DATA_LENGTH, terminal.getDataLength());
        }
        if (terminal.getDataContent() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DATA_CONTENT, terminal.getDataContent());
        }
        if (terminal.getChecksum() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_CHECKSUM, terminal.getChecksum());
        }
        
        // 时间字段
        if (terminal.getCreateTime() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_CREATE_TIME, String.valueOf(terminal.getCreateTime().getTime()));
        } else {
            values.put(DatabaseHelper.COLUMN_TERMINAL_CREATE_TIME, String.valueOf(System.currentTimeMillis()));
        }
        
        if (terminal.getUpdateTime() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_UPDATE_TIME, String.valueOf(terminal.getUpdateTime().getTime()));
        } else {
            values.put(DatabaseHelper.COLUMN_TERMINAL_UPDATE_TIME, String.valueOf(System.currentTimeMillis()));
        }
        
        return db.insert(DatabaseHelper.TABLE_TERMINALS, null, values);
    }

    /**
     * 更新终端
     */
    public int updateTerminal(Terminal terminal) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TERMINAL_DEVICE_ID, terminal.getDeviceId());
        values.put(DatabaseHelper.COLUMN_TERMINAL_NAME, terminal.getDeviceName());
        values.put(DatabaseHelper.COLUMN_TERMINAL_STATUS, terminal.getStatus());
        values.put(DatabaseHelper.COLUMN_TERMINAL_SIGNAL_STRENGTH, terminal.getSignalStrength());
        values.put(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT, terminal.getDepartment());
        values.put(DatabaseHelper.COLUMN_TERMINAL_LOCATION, terminal.getLocation());
        
        // 分类ID字段
        if (terminal.getDepartmentId() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT_ID, terminal.getDepartmentId());
        } else {
            values.putNull(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT_ID);
        }
        if (terminal.getRoomId() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_ROOM_ID, terminal.getRoomId());
        } else {
            values.putNull(DatabaseHelper.COLUMN_TERMINAL_ROOM_ID);
        }
        if (terminal.getNursingGroupId() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_NURSING_GROUP_ID, terminal.getNursingGroupId());
        } else {
            values.putNull(DatabaseHelper.COLUMN_TERMINAL_NURSING_GROUP_ID);
        }
        if (terminal.getOtherId() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_OTHER_ID, terminal.getOtherId());
        } else {
            values.putNull(DatabaseHelper.COLUMN_TERMINAL_OTHER_ID);
        }
        
        values.put(DatabaseHelper.COLUMN_TERMINAL_EXTENSION, terminal.getExtension());
        
        // 上行数据相关字段
        if (terminal.getDataTime() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DATA_TIME, String.valueOf(terminal.getDataTime().getTime()));
        }
        if (terminal.getDeviceEvent() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DEVICE_EVENT, terminal.getDeviceEvent());
        }
        if (terminal.getDeviceStatus() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DEVICE_STATUS, terminal.getDeviceStatus());
        }
        if (terminal.getBatteryVoltage() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_BATTERY_VOLTAGE, terminal.getBatteryVoltage());
        }
        if (terminal.getBatteryLevel() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_BATTERY_LEVEL, terminal.getBatteryLevel());
        }
        if (terminal.getRssi() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_RSSI, terminal.getRssi());
        }
        if (terminal.getDepartmentNumber() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT_NUMBER, terminal.getDepartmentNumber());
        }
        if (terminal.getCartNumber() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_CART_NUMBER, terminal.getCartNumber());
        }
        if (terminal.getDeviceCount() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DEVICE_COUNT, terminal.getDeviceCount());
        }
        if (terminal.getRackNumber() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_RACK_NUMBER, terminal.getRackNumber());
        }
        if (terminal.getFunctionCode() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_FUNCTION_CODE, terminal.getFunctionCode());
        }
        if (terminal.getSequenceNumber() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_SEQUENCE_NUMBER, terminal.getSequenceNumber());
        }
        if (terminal.getDataLength() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DATA_LENGTH, terminal.getDataLength());
        }
        if (terminal.getDataContent() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_DATA_CONTENT, terminal.getDataContent());
        }
        if (terminal.getChecksum() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_CHECKSUM, terminal.getChecksum());
        }
        
        // 更新时间
        if (terminal.getUpdateTime() != null) {
            values.put(DatabaseHelper.COLUMN_TERMINAL_UPDATE_TIME, String.valueOf(terminal.getUpdateTime().getTime()));
        } else {
            values.put(DatabaseHelper.COLUMN_TERMINAL_UPDATE_TIME, String.valueOf(System.currentTimeMillis()));
        }
        
        return db.update(DatabaseHelper.TABLE_TERMINALS, values, 
                DatabaseHelper.COLUMN_TERMINAL_ID + "=?", 
                new String[]{String.valueOf(terminal.getTerminalId())});
    }

    /**
     * 删除终端
     */
    public int deleteTerminal(int terminalId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_TERMINALS, 
                DatabaseHelper.COLUMN_TERMINAL_ID + "=?", 
                new String[]{String.valueOf(terminalId)});
    }

    /**
     * 根据ID查询终端
     */
    public Terminal getTerminalById(int terminalId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_TERMINALS, null, 
                DatabaseHelper.COLUMN_TERMINAL_ID + "=?", 
                new String[]{String.valueOf(terminalId)}, 
                null, null, null);
        
        Terminal terminal = null;
        if (cursor.moveToFirst()) {
            terminal = cursorToTerminal(cursor);
        }
        cursor.close();
        return terminal;
    }

    /**
     * 根据设备ID查询终端
     */
    public Terminal getTerminalByDeviceId(String deviceId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_TERMINALS, null, 
                DatabaseHelper.COLUMN_TERMINAL_DEVICE_ID + "=?", 
                new String[]{deviceId}, 
                null, null, null);
        
        Terminal terminal = null;
        if (cursor.moveToFirst()) {
            terminal = cursorToTerminal(cursor);
        }
        cursor.close();
        return terminal;
    }

    /**
     * 查询所有终端
     */
    public List<Terminal> getAllTerminals() {
        List<Terminal> terminals = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_TERMINALS, null, null, null, null, null, 
                DatabaseHelper.COLUMN_TERMINAL_CREATE_TIME + " DESC");
        
        while (cursor.moveToNext()) {
            terminals.add(cursorToTerminal(cursor));
        }
        cursor.close();
        return terminals;
    }

    /**
     * 根据状态查询终端
     */
    public List<Terminal> getTerminalsByStatus(String status) {
        List<Terminal> terminals = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_TERMINALS, null, 
                DatabaseHelper.COLUMN_TERMINAL_STATUS + "=?", 
                new String[]{status}, 
                null, null, DatabaseHelper.COLUMN_TERMINAL_CREATE_TIME + " DESC");
        
        while (cursor.moveToNext()) {
            terminals.add(cursorToTerminal(cursor));
        }
        cursor.close();
        return terminals;
    }

    /**
     * 根据分类ID查询终端
     */
    public List<Terminal> getTerminalsByCategory(String categoryType, int categoryId) {
        List<Terminal> terminals = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String columnName;
        switch (categoryType) {
            case "department":
                columnName = DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT_ID;
                break;
            case "room":
                columnName = DatabaseHelper.COLUMN_TERMINAL_ROOM_ID;
                break;
            case "nursing_group":
                columnName = DatabaseHelper.COLUMN_TERMINAL_NURSING_GROUP_ID;
                break;
            case "other":
                columnName = DatabaseHelper.COLUMN_TERMINAL_OTHER_ID;
                break;
            default:
                return terminals; // 返回空列表
        }
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_TERMINALS, null, 
                columnName + "=?", 
                new String[]{String.valueOf(categoryId)}, 
                null, null, DatabaseHelper.COLUMN_TERMINAL_CREATE_TIME + " DESC");
        
        while (cursor.moveToNext()) {
            terminals.add(cursorToTerminal(cursor));
        }
        cursor.close();
        return terminals;
    }

    /**
     * 搜索终端（根据设备ID或设备名称）
     */
    public List<Terminal> searchTerminals(String keyword) {
        List<Terminal> terminals = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String selection = DatabaseHelper.COLUMN_TERMINAL_DEVICE_ID + " LIKE ? OR " +
                          DatabaseHelper.COLUMN_TERMINAL_NAME + " LIKE ?";
        String[] selectionArgs = {"%" + keyword + "%", "%" + keyword + "%"};
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_TERMINALS, null, 
                selection, selectionArgs, 
                null, null, DatabaseHelper.COLUMN_TERMINAL_CREATE_TIME + " DESC");
        
        while (cursor.moveToNext()) {
            terminals.add(cursorToTerminal(cursor));
        }
        cursor.close();
        return terminals;
    }

    /**
     * 检查设备ID是否存在
     */
    public boolean isDeviceIdExists(String deviceId, int excludeTerminalId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COLUMN_TERMINAL_DEVICE_ID + "=?";
        String[] selectionArgs;
        
        if (excludeTerminalId > 0) {
            selection += " AND " + DatabaseHelper.COLUMN_TERMINAL_ID + "!=?";
            selectionArgs = new String[]{deviceId, String.valueOf(excludeTerminalId)};
        } else {
            selectionArgs = new String[]{deviceId};
        }
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_TERMINALS, 
                new String[]{DatabaseHelper.COLUMN_TERMINAL_ID}, 
                selection, selectionArgs, null, null, null);
        
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * 获取终端总数
     */
    public int getTerminalCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TERMINALS, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * 将Cursor转换为Terminal对象
     */
    private Terminal cursorToTerminal(Cursor cursor) {
        Terminal terminal = new Terminal();
        terminal.setTerminalId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TERMINAL_ID)));
        terminal.setDeviceId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TERMINAL_DEVICE_ID)));
        terminal.setDeviceName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TERMINAL_NAME)));
        terminal.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TERMINAL_STATUS)));
        terminal.setSignalStrength(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TERMINAL_SIGNAL_STRENGTH)));
        terminal.setDepartment(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT)));
        terminal.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TERMINAL_LOCATION)));
        
        // 分类ID字段（可能为null）
        int departmentIdIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT_ID);
        if (departmentIdIndex != -1 && !cursor.isNull(departmentIdIndex)) {
            terminal.setDepartmentId(cursor.getInt(departmentIdIndex));
        }
        
        int roomIdIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_ROOM_ID);
        if (roomIdIndex != -1 && !cursor.isNull(roomIdIndex)) {
            terminal.setRoomId(cursor.getInt(roomIdIndex));
        }
        
        int nursingGroupIdIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_NURSING_GROUP_ID);
        if (nursingGroupIdIndex != -1 && !cursor.isNull(nursingGroupIdIndex)) {
            terminal.setNursingGroupId(cursor.getInt(nursingGroupIdIndex));
        }
        
        int otherIdIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_OTHER_ID);
        if (otherIdIndex != -1 && !cursor.isNull(otherIdIndex)) {
            terminal.setOtherId(cursor.getInt(otherIdIndex));
        }
        
        terminal.setExtension(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TERMINAL_EXTENSION)));
        
        // 上行数据相关字段（可能为null）
        int dataTimeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DATA_TIME);
        if (dataTimeIndex != -1 && !cursor.isNull(dataTimeIndex)) {
            String dataTimeStr = cursor.getString(dataTimeIndex);
            if (dataTimeStr != null && !dataTimeStr.isEmpty()) {
                try {
                    terminal.setDataTime(new Date(Long.parseLong(dataTimeStr)));
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }
        
        int deviceEventIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEVICE_EVENT);
        if (deviceEventIndex != -1 && !cursor.isNull(deviceEventIndex)) {
            terminal.setDeviceEvent(cursor.getLong(deviceEventIndex));
        }
        
        int deviceStatusIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEVICE_STATUS);
        if (deviceStatusIndex != -1 && !cursor.isNull(deviceStatusIndex)) {
            terminal.setDeviceStatus(cursor.getLong(deviceStatusIndex));
        }
        
        int batteryVoltageIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_BATTERY_VOLTAGE);
        if (batteryVoltageIndex != -1 && !cursor.isNull(batteryVoltageIndex)) {
            terminal.setBatteryVoltage(cursor.getInt(batteryVoltageIndex));
        }
        
        int batteryLevelIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_BATTERY_LEVEL);
        if (batteryLevelIndex != -1 && !cursor.isNull(batteryLevelIndex)) {
            terminal.setBatteryLevel(cursor.getInt(batteryLevelIndex));
        }
        
        int rssiIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_RSSI);
        if (rssiIndex != -1 && !cursor.isNull(rssiIndex)) {
            terminal.setRssi(cursor.getInt(rssiIndex));
        }
        
        int departmentNumberIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEPARTMENT_NUMBER);
        if (departmentNumberIndex != -1 && !cursor.isNull(departmentNumberIndex)) {
            terminal.setDepartmentNumber(cursor.getInt(departmentNumberIndex));
        }
        
        int cartNumberIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_CART_NUMBER);
        if (cartNumberIndex != -1 && !cursor.isNull(cartNumberIndex)) {
            terminal.setCartNumber(cursor.getInt(cartNumberIndex));
        }
        
        int deviceCountIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DEVICE_COUNT);
        if (deviceCountIndex != -1 && !cursor.isNull(deviceCountIndex)) {
            terminal.setDeviceCount(cursor.getInt(deviceCountIndex));
        }
        
        int rackNumberIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_RACK_NUMBER);
        if (rackNumberIndex != -1 && !cursor.isNull(rackNumberIndex)) {
            terminal.setRackNumber(cursor.getInt(rackNumberIndex));
        }
        
        int functionCodeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_FUNCTION_CODE);
        if (functionCodeIndex != -1 && !cursor.isNull(functionCodeIndex)) {
            terminal.setFunctionCode(cursor.getString(functionCodeIndex));
        }
        
        int sequenceNumberIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_SEQUENCE_NUMBER);
        if (sequenceNumberIndex != -1 && !cursor.isNull(sequenceNumberIndex)) {
            terminal.setSequenceNumber(cursor.getInt(sequenceNumberIndex));
        }
        
        int dataLengthIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DATA_LENGTH);
        if (dataLengthIndex != -1 && !cursor.isNull(dataLengthIndex)) {
            terminal.setDataLength(cursor.getInt(dataLengthIndex));
        }
        
        int dataContentIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_DATA_CONTENT);
        if (dataContentIndex != -1 && !cursor.isNull(dataContentIndex)) {
            terminal.setDataContent(cursor.getString(dataContentIndex));
        }
        
        int checksumIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TERMINAL_CHECKSUM);
        if (checksumIndex != -1 && !cursor.isNull(checksumIndex)) {
            terminal.setChecksum(cursor.getInt(checksumIndex));
        }
        
        // 时间字段
        String createTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TERMINAL_CREATE_TIME));
        if (createTimeStr != null && !createTimeStr.isEmpty()) {
            try {
                terminal.setCreateTime(new Date(Long.parseLong(createTimeStr)));
            } catch (NumberFormatException e) {
                terminal.setCreateTime(new Date());
            }
        } else {
            terminal.setCreateTime(new Date());
        }
        
        String updateTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TERMINAL_UPDATE_TIME));
        if (updateTimeStr != null && !updateTimeStr.isEmpty()) {
            try {
                terminal.setUpdateTime(new Date(Long.parseLong(updateTimeStr)));
            } catch (NumberFormatException e) {
                terminal.setUpdateTime(new Date());
            }
        } else {
            terminal.setUpdateTime(new Date());
        }
        
        return terminal;
    }
}