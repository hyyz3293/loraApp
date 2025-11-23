package com.lora.cn.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.database.entity.Position;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 职位数据访问对象
 */
public class PositionDao {
    private DatabaseHelper dbHelper;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    public PositionDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }
    
    /**
     * 插入新职位
     */
    public long insertPosition(Position position) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DatabaseHelper.COLUMN_POSITION_NAME, position.getPositionName());
        values.put(DatabaseHelper.COLUMN_POSITION_SORT_ORDER, position.getSortOrder());
        values.put(DatabaseHelper.COLUMN_POSITION_STATUS, position.getStatus());
        
        long result = db.insert(DatabaseHelper.TABLE_POSITIONS, null, values);
        
        return result;
    }
    
    /**
     * 查询所有职位
     */
    public List<Position> getAllPositions() {
        List<Position> positions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String orderBy = DatabaseHelper.COLUMN_POSITION_SORT_ORDER + " ASC, " + 
                        DatabaseHelper.COLUMN_POSITION_CREATE_TIME + " ASC";
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_POSITIONS, null, null, null, null, null, orderBy);
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Position position = cursorToPosition(cursor);
                positions.add(position);
            }
            cursor.close();
        }
        
        return positions;
    }
    
    /**
     * 根据ID查询职位
     */
    public Position getPositionById(long positionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Position position = null;
        
        String selection = DatabaseHelper.COLUMN_POSITION_ID + " = ?";
        String[] selectionArgs = {String.valueOf(positionId)};
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_POSITIONS, null, selection, selectionArgs, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            position = cursorToPosition(cursor);
            cursor.close();
        }
        
        return position;
    }
    
    /**
     * 根据名称查询职位
     */
    public Position getPositionByName(String positionName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Position position = null;
        
        String selection = DatabaseHelper.COLUMN_POSITION_NAME + " = ?";
        String[] selectionArgs = {positionName};
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_POSITIONS, null, selection, selectionArgs, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            position = cursorToPosition(cursor);
            cursor.close();
        }
        
        return position;
    }
    
    /**
     * 更新职位
     */
    public int updatePosition(Position position) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DatabaseHelper.COLUMN_POSITION_NAME, position.getPositionName());
        values.put(DatabaseHelper.COLUMN_POSITION_SORT_ORDER, position.getSortOrder());
        values.put(DatabaseHelper.COLUMN_POSITION_STATUS, position.getStatus());
        values.put(DatabaseHelper.COLUMN_POSITION_UPDATE_TIME, dateFormat.format(new Date()));
        
        String whereClause = DatabaseHelper.COLUMN_POSITION_ID + " = ?";
        String[] whereArgs = {String.valueOf(position.getPositionId())};
        
        int result = db.update(DatabaseHelper.TABLE_POSITIONS, values, whereClause, whereArgs);
        
        return result;
    }
    
    /**
     * 删除职位
     */
    public int deletePosition(long positionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        String whereClause = DatabaseHelper.COLUMN_POSITION_ID + " = ?";
        String[] whereArgs = {String.valueOf(positionId)};
        
        int result = db.delete(DatabaseHelper.TABLE_POSITIONS, whereClause, whereArgs);
        
        return result;
    }
    
    /**
     * 检查职位名称是否存在
     */
    public boolean isPositionNameExists(String positionName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        boolean exists = false;
        
        String selection = DatabaseHelper.COLUMN_POSITION_NAME + " = ?";
        String[] selectionArgs = {positionName};
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_POSITIONS, new String[]{DatabaseHelper.COLUMN_POSITION_ID}, 
                                selection, selectionArgs, null, null, null);
        
        if (cursor != null) {
            exists = cursor.getCount() > 0;
            cursor.close();
        }
        
        return exists;
    }
    
    /**
     * 获取职位总数
     */
    public int getPositionCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int count = 0;
        
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_POSITIONS, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        
        db.close();
        return count;
    }
    
    /**
     * 将Cursor转换为Position对象
     */
    private Position cursorToPosition(Cursor cursor) {
        Position position = new Position();
        
        position.setPositionId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_POSITION_ID)));
        position.setPositionName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_POSITION_NAME)));
        position.setSortOrder(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_POSITION_SORT_ORDER)));
        position.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_POSITION_STATUS)));
        
        // 处理创建时间
        String createTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_POSITION_CREATE_TIME));
        if (createTimeStr != null) {
            try {
                position.setCreateTime(dateFormat.parse(createTimeStr));
            } catch (ParseException e) {
                position.setCreateTime(new Date());
            }
        }
        
        // 处理更新时间
        String updateTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_POSITION_UPDATE_TIME));
        if (updateTimeStr != null) {
            try {
                position.setUpdateTime(dateFormat.parse(updateTimeStr));
            } catch (ParseException e) {
                position.setUpdateTime(new Date());
            }
        }
        
        return position;
    }
}