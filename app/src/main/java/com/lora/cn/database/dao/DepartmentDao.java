package com.lora.cn.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.database.entity.Department;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 科室数据访问对象
 */
public class DepartmentDao {
    private DatabaseHelper dbHelper;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    public DepartmentDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }
    
    /**
     * 插入新科室
     */
    public long insertDepartment(Department department) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DatabaseHelper.COLUMN_DEPARTMENT_NAME, department.getDepartmentName());
        values.put(DatabaseHelper.COLUMN_DEPARTMENT_SORT_ORDER, department.getSortOrder());
        values.put(DatabaseHelper.COLUMN_DEPARTMENT_STATUS, department.getStatus());
        
        long result = db.insert(DatabaseHelper.TABLE_DEPARTMENTS, null, values);
        db.close();
        
        return result;
    }
    
    /**
     * 查询所有科室
     */
    public List<Department> getAllDepartments() {
        List<Department> departments = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String orderBy = DatabaseHelper.COLUMN_DEPARTMENT_SORT_ORDER + " ASC, " + 
                        DatabaseHelper.COLUMN_DEPARTMENT_CREATE_TIME + " ASC";
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_DEPARTMENTS, null, null, null, null, null, orderBy);
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Department department = cursorToDepartment(cursor);
                departments.add(department);
            }
            cursor.close();
        }
        
        db.close();
        return departments;
    }
    
    /**
     * 根据ID查询科室
     */
    public Department getDepartmentById(long departmentId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Department department = null;
        
        String selection = DatabaseHelper.COLUMN_DEPARTMENT_ID + " = ?";
        String[] selectionArgs = {String.valueOf(departmentId)};
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_DEPARTMENTS, null, selection, selectionArgs, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            department = cursorToDepartment(cursor);
            cursor.close();
        }
        
        db.close();
        return department;
    }
    
    /**
     * 根据名称查询科室
     */
    public Department getDepartmentByName(String departmentName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Department department = null;
        
        String selection = DatabaseHelper.COLUMN_DEPARTMENT_NAME + " = ?";
        String[] selectionArgs = {departmentName};
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_DEPARTMENTS, null, selection, selectionArgs, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            department = cursorToDepartment(cursor);
            cursor.close();
        }
        
        db.close();
        return department;
    }
    
    /**
     * 更新科室
     */
    public int updateDepartment(Department department) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DatabaseHelper.COLUMN_DEPARTMENT_NAME, department.getDepartmentName());
        values.put(DatabaseHelper.COLUMN_DEPARTMENT_SORT_ORDER, department.getSortOrder());
        values.put(DatabaseHelper.COLUMN_DEPARTMENT_STATUS, department.getStatus());
        values.put(DatabaseHelper.COLUMN_DEPARTMENT_UPDATE_TIME, dateFormat.format(new Date()));
        
        String whereClause = DatabaseHelper.COLUMN_DEPARTMENT_ID + " = ?";
        String[] whereArgs = {String.valueOf(department.getDepartmentId())};
        
        int result = db.update(DatabaseHelper.TABLE_DEPARTMENTS, values, whereClause, whereArgs);
        db.close();
        
        return result;
    }
    
    /**
     * 删除科室
     */
    public int deleteDepartment(long departmentId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        String whereClause = DatabaseHelper.COLUMN_DEPARTMENT_ID + " = ?";
        String[] whereArgs = {String.valueOf(departmentId)};
        
        int result = db.delete(DatabaseHelper.TABLE_DEPARTMENTS, whereClause, whereArgs);
        db.close();
        
        return result;
    }
    
    /**
     * 检查科室名称是否存在
     */
    public boolean isDepartmentNameExists(String departmentName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        boolean exists = false;
        
        String selection = DatabaseHelper.COLUMN_DEPARTMENT_NAME + " = ?";
        String[] selectionArgs = {departmentName};
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_DEPARTMENTS, new String[]{DatabaseHelper.COLUMN_DEPARTMENT_ID}, 
                                selection, selectionArgs, null, null, null);
        
        if (cursor != null) {
            exists = cursor.getCount() > 0;
            cursor.close();
        }
        
        db.close();
        return exists;
    }
    
    /**
     * 获取科室总数
     */
    public int getDepartmentCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int count = 0;
        
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_DEPARTMENTS, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        
        db.close();
        return count;
    }
    
    /**
     * 将Cursor转换为Department对象
     */
    private Department cursorToDepartment(Cursor cursor) {
        Department department = new Department();
        
        department.setDepartmentId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DEPARTMENT_ID)));
        department.setDepartmentName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DEPARTMENT_NAME)));
        department.setSortOrder(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DEPARTMENT_SORT_ORDER)));
        department.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DEPARTMENT_STATUS)));
        
        // 处理创建时间
        String createTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DEPARTMENT_CREATE_TIME));
        if (createTimeStr != null) {
            try {
                department.setCreateTime(dateFormat.parse(createTimeStr));
            } catch (ParseException e) {
                department.setCreateTime(new Date());
            }
        }
        
        // 处理更新时间
        String updateTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DEPARTMENT_UPDATE_TIME));
        if (updateTimeStr != null) {
            try {
                department.setUpdateTime(dateFormat.parse(updateTimeStr));
            } catch (ParseException e) {
                department.setUpdateTime(new Date());
            }
        }
        
        return department;
    }
}