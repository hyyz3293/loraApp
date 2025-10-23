package com.lora.cn.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * 数据库帮助类
 * 管理分组表和分类表的两层关系结构
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "lora_app.db";
    private static final int DATABASE_VERSION = 11;
    
    // 分组表
    public static final String TABLE_GROUPS = "groups";
    public static final String COLUMN_GROUP_ID = "group_id";
    public static final String COLUMN_GROUP_NAME = "group_name";
    public static final String COLUMN_GROUP_DESCRIPTION = "group_description";
    public static final String COLUMN_GROUP_CREATE_TIME = "create_time";
    public static final String COLUMN_GROUP_UPDATE_TIME = "update_time";
    
    // 分类表
    public static final String TABLE_CATEGORIES = "categories";
    public static final String COLUMN_CATEGORY_ID = "category_id";
    public static final String COLUMN_CATEGORY_NAME = "category_name";
    public static final String COLUMN_CATEGORY_DESCRIPTION = "category_description";
    public static final String COLUMN_CATEGORY_GROUP_ID = "group_id"; // 外键，关联分组表
    public static final String COLUMN_CATEGORY_CREATE_TIME = "create_time";
    public static final String COLUMN_CATEGORY_UPDATE_TIME = "update_time";
    
    // 科室表
    public static final String TABLE_DEPARTMENTS = "departments";
    public static final String COLUMN_DEPARTMENT_ID = "department_id";
    public static final String COLUMN_DEPARTMENT_NAME = "department_name";
    public static final String COLUMN_DEPARTMENT_SORT_ORDER = "sort_order";
    public static final String COLUMN_DEPARTMENT_STATUS = "status";
    public static final String COLUMN_DEPARTMENT_CREATE_TIME = "create_time";
    public static final String COLUMN_DEPARTMENT_UPDATE_TIME = "update_time";
    
    // 职位表
    public static final String TABLE_POSITIONS = "positions";
    public static final String COLUMN_POSITION_ID = "position_id";
    public static final String COLUMN_POSITION_NAME = "position_name";
    public static final String COLUMN_POSITION_SORT_ORDER = "sort_order";
    public static final String COLUMN_POSITION_STATUS = "status";
    public static final String COLUMN_POSITION_CREATE_TIME = "create_time";
    public static final String COLUMN_POSITION_UPDATE_TIME = "update_time";
    
    // 角色表
    public static final String TABLE_ROLES = "roles";
    public static final String COLUMN_ROLE_ID = "role_id";
    public static final String COLUMN_ROLE_NAME = "role_name";
    public static final String COLUMN_ROLE_DESCRIPTION = "description";
    public static final String COLUMN_ROLE_SORT_ORDER = "sort_order";
    public static final String COLUMN_ROLE_STATUS = "status";
    public static final String COLUMN_ROLE_CREATE_TIME = "create_time";
    public static final String COLUMN_ROLE_UPDATE_TIME = "update_time";
    
    // 权限表
    public static final String TABLE_PERMISSIONS = "permissions";
    public static final String COLUMN_PERMISSION_ID = "permission_id";
    public static final String COLUMN_PERMISSION_CODE = "permission_code";
    public static final String COLUMN_PERMISSION_NAME = "permission_name";
    public static final String COLUMN_PERMISSION_CATEGORY = "category";
    public static final String COLUMN_PERMISSION_DESCRIPTION = "description";
    public static final String COLUMN_PERMISSION_STATUS = "status";
    public static final String COLUMN_PERMISSION_PARENT_ID = "parent_id";
    public static final String COLUMN_PERMISSION_LEVEL = "level";
    public static final String COLUMN_PERMISSION_SORT_ORDER = "sort_order";
    public static final String COLUMN_PERMISSION_CREATE_TIME = "create_time";
    public static final String COLUMN_PERMISSION_UPDATE_TIME = "update_time";
    
    // 角色权限关联表
    public static final String TABLE_ROLE_PERMISSIONS = "role_permissions";
    public static final String COLUMN_ROLE_PERMISSION_ID = "id";
    public static final String COLUMN_ROLE_PERMISSION_ROLE_ID = "role_id";
    public static final String COLUMN_ROLE_PERMISSION_PERMISSION_ID = "permission_id";
    public static final String COLUMN_ROLE_PERMISSION_CREATE_TIME = "create_time";
    public static final String COLUMN_RP_CREATE_TIME = "create_time"; // 别名
    public static final String COLUMN_RP_ROLE_ID = "role_id"; // 别名
    public static final String COLUMN_RP_PERMISSION_ID = "permission_id"; // 别名
    public static final String COLUMN_RP_ID = "id"; // 别名
    
    // 用户表
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_USER_NAME = "user_name";
    public static final String COLUMN_USER_ACCOUNT = "user_account";
    public static final String COLUMN_USER_PASSWORD = "user_password";
    public static final String COLUMN_USER_ROLE_ID = "role_id";
    public static final String COLUMN_USER_STATUS = "status";
    public static final String COLUMN_USER_POSITION_ID = "position_id";
    public static final String COLUMN_USER_DEPARTMENT_ID = "department_id";
    public static final String COLUMN_USER_CODE = "user_code";
    public static final String COLUMN_USER_GENDER = "gender";
    public static final String COLUMN_USER_PHONE = "phone";
    public static final String COLUMN_USER_CREATE_TIME = "create_time";
    public static final String COLUMN_USER_UPDATE_TIME = "update_time";
    
    // 终端表
    public static final String TABLE_TERMINALS = "terminals";
    public static final String COLUMN_TERMINAL_ID = "terminal_id";
    public static final String COLUMN_TERMINAL_DEVICE_ID = "terminal_device_id";
    public static final String COLUMN_TERMINAL_NAME = "terminal_name";
    public static final String COLUMN_TERMINAL_STATUS = "status";
    public static final String COLUMN_TERMINAL_SIGNAL_STRENGTH = "signal_strength";
    public static final String COLUMN_TERMINAL_DEPARTMENT = "department";
    public static final String COLUMN_TERMINAL_LOCATION = "location";
    public static final String COLUMN_TERMINAL_DEPARTMENT_ID = "department_id"; // 科室分类ID
    public static final String COLUMN_TERMINAL_ROOM_ID = "room_id"; // 病房号分类ID
    public static final String COLUMN_TERMINAL_NURSING_GROUP_ID = "nursing_group_id"; // 护理组分类ID
    public static final String COLUMN_TERMINAL_OTHER_ID = "other_id"; // 其他分类ID
    public static final String COLUMN_TERMINAL_EXTENSION = "extension"; // 扩展字段
    public static final String COLUMN_TERMINAL_CREATE_TIME = "create_time";
    public static final String COLUMN_TERMINAL_UPDATE_TIME = "update_time";
    
    // 上行数据相关字段
    public static final String COLUMN_TERMINAL_DATA_TIME = "data_time"; // 数据产生时间
    public static final String COLUMN_TERMINAL_DEVICE_EVENT = "device_event"; // 设备事件
    public static final String COLUMN_TERMINAL_DEVICE_STATUS = "device_status"; // 设备状态
    public static final String COLUMN_TERMINAL_BATTERY_VOLTAGE = "battery_voltage"; // 电池电压
    public static final String COLUMN_TERMINAL_BATTERY_LEVEL = "battery_level"; // 电量
    public static final String COLUMN_TERMINAL_RSSI = "rssi"; // RSSI
    public static final String COLUMN_TERMINAL_DEPARTMENT_NUMBER = "department_number"; // 科室或护士站编号
    public static final String COLUMN_TERMINAL_CART_NUMBER = "cart_number"; // 台车编号
    public static final String COLUMN_TERMINAL_DEVICE_COUNT = "device_count"; // 放置的设备数量
    public static final String COLUMN_TERMINAL_RACK_NUMBER = "rack_number"; // 设备所属台车台架编号

    // 上行数据日志表
    public static final String TABLE_UPLINK_LOGS = "uplink_logs";
    public static final String COLUMN_UPLINK_LOG_ID = "log_id";
    public static final String COLUMN_UPLINK_LOG_TIME = "time";
    public static final String COLUMN_UPLINK_LOG_HEX = "hex";
    public static final String COLUMN_UPLINK_LOG_CREATE_TIME = "create_time";
    
    // 详细上行数据日志表
    public static final String TABLE_DETAILED_UPLINK_LOGS = "detailed_uplink_logs";
    public static final String COLUMN_DETAILED_LOG_ID = "detailed_log_id";
    public static final String COLUMN_DETAILED_LOG_TIME = "time";
    public static final String COLUMN_DETAILED_LOG_HEX = "hex";
    public static final String COLUMN_DETAILED_LOG_CREATE_TIME = "create_time";
    public static final String COLUMN_DETAILED_LOG_DEVICE_ID = "device_id";
    public static final String COLUMN_DETAILED_LOG_FUNCTION_CODE = "function_code";
    public static final String COLUMN_DETAILED_LOG_SEQUENCE_NUMBER = "sequence_number";
    public static final String COLUMN_DETAILED_LOG_DATA_LENGTH = "data_length";
    public static final String COLUMN_DETAILED_LOG_DATA_TIME = "data_time";
    public static final String COLUMN_DETAILED_LOG_DEVICE_EVENT = "device_event";
    public static final String COLUMN_DETAILED_LOG_DEVICE_STATUS = "device_status";
    public static final String COLUMN_DETAILED_LOG_BATTERY_VOLTAGE = "battery_voltage";
    public static final String COLUMN_DETAILED_LOG_BATTERY_LEVEL = "battery_level";
    public static final String COLUMN_DETAILED_LOG_RSSI = "rssi";
    public static final String COLUMN_DETAILED_LOG_DEPARTMENT_NUMBER = "department_number";
    public static final String COLUMN_DETAILED_LOG_CART_NUMBER = "cart_number";
    public static final String COLUMN_DETAILED_LOG_DEVICE_COUNT = "device_count";
    public static final String COLUMN_DETAILED_LOG_RACK_NUMBER = "rack_number";
    public static final String COLUMN_DETAILED_LOG_PARSE_SUCCESS = "parse_success";
    public static final String COLUMN_DETAILED_LOG_PARSE_ERROR = "parse_error";
    
    // 创建分组表的SQL语句
    private static final String CREATE_TABLE_GROUPS = 
        "CREATE TABLE " + TABLE_GROUPS + " (" +
        COLUMN_GROUP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_GROUP_NAME + " TEXT NOT NULL UNIQUE, " +
        COLUMN_GROUP_DESCRIPTION + " TEXT, " +
        COLUMN_GROUP_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_GROUP_UPDATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    // 创建分类表的SQL语句
    private static final String CREATE_TABLE_CATEGORIES = 
        "CREATE TABLE " + TABLE_CATEGORIES + " (" +
        COLUMN_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_CATEGORY_NAME + " TEXT NOT NULL, " +
        COLUMN_CATEGORY_DESCRIPTION + " TEXT, " +
        COLUMN_CATEGORY_GROUP_ID + " INTEGER NOT NULL, " +
        COLUMN_CATEGORY_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_CATEGORY_UPDATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        "FOREIGN KEY (" + COLUMN_CATEGORY_GROUP_ID + ") REFERENCES " + 
        TABLE_GROUPS + "(" + COLUMN_GROUP_ID + ") ON DELETE CASCADE, " +
        "UNIQUE(" + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_GROUP_ID + ")" +
        ")";
    
    // 创建科室表的SQL语句
    private static final String CREATE_TABLE_DEPARTMENTS = 
        "CREATE TABLE " + TABLE_DEPARTMENTS + " (" +
        COLUMN_DEPARTMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_DEPARTMENT_NAME + " TEXT NOT NULL UNIQUE, " +
        COLUMN_DEPARTMENT_SORT_ORDER + " INTEGER DEFAULT 0, " +
        COLUMN_DEPARTMENT_STATUS + " INTEGER DEFAULT 1, " +
        COLUMN_DEPARTMENT_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_DEPARTMENT_UPDATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    // 创建职位表的SQL语句
    private static final String CREATE_TABLE_POSITIONS = 
        "CREATE TABLE " + TABLE_POSITIONS + " (" +
        COLUMN_POSITION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_POSITION_NAME + " TEXT NOT NULL UNIQUE, " +
        COLUMN_POSITION_SORT_ORDER + " INTEGER DEFAULT 0, " +
        COLUMN_POSITION_STATUS + " INTEGER DEFAULT 1, " +
        COLUMN_POSITION_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_POSITION_UPDATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    // 创建角色表的SQL语句
    private static final String CREATE_TABLE_ROLES = 
        "CREATE TABLE " + TABLE_ROLES + " (" +
        COLUMN_ROLE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_ROLE_NAME + " TEXT NOT NULL UNIQUE, " +
        COLUMN_ROLE_DESCRIPTION + " TEXT, " +
        COLUMN_ROLE_SORT_ORDER + " INTEGER DEFAULT 0, " +
        COLUMN_ROLE_STATUS + " INTEGER DEFAULT 1, " +
        COLUMN_ROLE_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_ROLE_UPDATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    // 创建权限表的SQL语句
    private static final String CREATE_TABLE_PERMISSIONS = 
        "CREATE TABLE " + TABLE_PERMISSIONS + " (" +
        COLUMN_PERMISSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_PERMISSION_CODE + " TEXT NOT NULL UNIQUE, " +
        COLUMN_PERMISSION_NAME + " TEXT NOT NULL, " +
        COLUMN_PERMISSION_CATEGORY + " TEXT NOT NULL, " +
        COLUMN_PERMISSION_DESCRIPTION + " TEXT, " +
        COLUMN_PERMISSION_STATUS + " INTEGER DEFAULT 1, " +
        COLUMN_PERMISSION_PARENT_ID + " INTEGER, " +
        COLUMN_PERMISSION_LEVEL + " INTEGER DEFAULT 0, " +
        COLUMN_PERMISSION_SORT_ORDER + " INTEGER DEFAULT 0, " +
        COLUMN_PERMISSION_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_PERMISSION_UPDATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        "FOREIGN KEY (" + COLUMN_PERMISSION_PARENT_ID + ") REFERENCES " + 
        TABLE_PERMISSIONS + "(" + COLUMN_PERMISSION_ID + ") ON DELETE CASCADE" +
        ")";
    
    // 创建角色权限关联表的SQL语句
    private static final String CREATE_TABLE_ROLE_PERMISSIONS = 
        "CREATE TABLE " + TABLE_ROLE_PERMISSIONS + " (" +
        COLUMN_ROLE_PERMISSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_ROLE_PERMISSION_ROLE_ID + " INTEGER NOT NULL, " +
        COLUMN_ROLE_PERMISSION_PERMISSION_ID + " INTEGER NOT NULL, " +
        COLUMN_ROLE_PERMISSION_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        "FOREIGN KEY (" + COLUMN_ROLE_PERMISSION_ROLE_ID + ") REFERENCES " + 
        TABLE_ROLES + "(" + COLUMN_ROLE_ID + ") ON DELETE CASCADE, " +
        "FOREIGN KEY (" + COLUMN_ROLE_PERMISSION_PERMISSION_ID + ") REFERENCES " + 
        TABLE_PERMISSIONS + "(" + COLUMN_PERMISSION_ID + ") ON DELETE CASCADE, " +
        "UNIQUE(" + COLUMN_ROLE_PERMISSION_ROLE_ID + ", " + COLUMN_ROLE_PERMISSION_PERMISSION_ID + ")" +
        ")";
    
    // 创建用户表的SQL语句
    private static final String CREATE_TABLE_USERS = 
        "CREATE TABLE " + TABLE_USERS + " (" +
        COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_USER_NAME + " TEXT NOT NULL, " +
        COLUMN_USER_ACCOUNT + " TEXT NOT NULL UNIQUE, " +
        COLUMN_USER_PASSWORD + " TEXT NOT NULL, " +
        COLUMN_USER_ROLE_ID + " INTEGER NOT NULL, " +
        COLUMN_USER_STATUS + " INTEGER DEFAULT 1, " +
        COLUMN_USER_POSITION_ID + " INTEGER, " +
        COLUMN_USER_DEPARTMENT_ID + " INTEGER, " +
        COLUMN_USER_CODE + " TEXT, " +
        COLUMN_USER_GENDER + " INTEGER DEFAULT 1, " +
        COLUMN_USER_PHONE + " TEXT, " +
        COLUMN_USER_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_USER_UPDATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        "FOREIGN KEY (" + COLUMN_USER_ROLE_ID + ") REFERENCES " + 
        TABLE_ROLES + "(" + COLUMN_ROLE_ID + ") ON DELETE RESTRICT, " +
        "FOREIGN KEY (" + COLUMN_USER_POSITION_ID + ") REFERENCES " + 
        TABLE_POSITIONS + "(" + COLUMN_POSITION_ID + ") ON DELETE SET NULL, " +
        "FOREIGN KEY (" + COLUMN_USER_DEPARTMENT_ID + ") REFERENCES " + 
        TABLE_DEPARTMENTS + "(" + COLUMN_DEPARTMENT_ID + ") ON DELETE SET NULL" +
        ")";
    
    // 创建终端表的SQL语句
    private static final String CREATE_TABLE_TERMINALS = 
        "CREATE TABLE " + TABLE_TERMINALS + " (" +
        COLUMN_TERMINAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_TERMINAL_DEVICE_ID + " TEXT NOT NULL UNIQUE, " +
        COLUMN_TERMINAL_NAME + " TEXT NOT NULL, " +
        COLUMN_TERMINAL_STATUS + " TEXT DEFAULT '在线', " +
        COLUMN_TERMINAL_SIGNAL_STRENGTH + " INTEGER DEFAULT 0, " +
        COLUMN_TERMINAL_DEPARTMENT + " TEXT, " +
        COLUMN_TERMINAL_LOCATION + " TEXT, " +
        COLUMN_TERMINAL_DEPARTMENT_ID + " INTEGER, " +
        COLUMN_TERMINAL_ROOM_ID + " INTEGER, " +
        COLUMN_TERMINAL_NURSING_GROUP_ID + " INTEGER, " +
        COLUMN_TERMINAL_OTHER_ID + " INTEGER, " +
        COLUMN_TERMINAL_EXTENSION + " TEXT, " +
        COLUMN_TERMINAL_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_TERMINAL_UPDATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        // 上行数据相关字段
        COLUMN_TERMINAL_DATA_TIME + " DATETIME, " +
        COLUMN_TERMINAL_DEVICE_EVENT + " INTEGER, " +
        COLUMN_TERMINAL_DEVICE_STATUS + " INTEGER, " +
        COLUMN_TERMINAL_BATTERY_VOLTAGE + " INTEGER, " +
        COLUMN_TERMINAL_BATTERY_LEVEL + " INTEGER, " +
        COLUMN_TERMINAL_RSSI + " INTEGER, " +
        COLUMN_TERMINAL_DEPARTMENT_NUMBER + " INTEGER, " +
        COLUMN_TERMINAL_CART_NUMBER + " INTEGER, " +
        COLUMN_TERMINAL_DEVICE_COUNT + " INTEGER, " +
        COLUMN_TERMINAL_RACK_NUMBER + " INTEGER, " +
        "FOREIGN KEY (" + COLUMN_TERMINAL_DEPARTMENT_ID + ") REFERENCES " + 
        TABLE_CATEGORIES + "(" + COLUMN_CATEGORY_ID + ") ON DELETE SET NULL, " +
        "FOREIGN KEY (" + COLUMN_TERMINAL_ROOM_ID + ") REFERENCES " + 
        TABLE_CATEGORIES + "(" + COLUMN_CATEGORY_ID + ") ON DELETE SET NULL, " +
        "FOREIGN KEY (" + COLUMN_TERMINAL_NURSING_GROUP_ID + ") REFERENCES " + 
        TABLE_CATEGORIES + "(" + COLUMN_CATEGORY_ID + ") ON DELETE SET NULL, " +
        "FOREIGN KEY (" + COLUMN_TERMINAL_OTHER_ID + ") REFERENCES " + 
        TABLE_CATEGORIES + "(" + COLUMN_CATEGORY_ID + ") ON DELETE SET NULL" +
        ")";

    // 创建上行数据日志表的SQL语句
    private static final String CREATE_TABLE_UPLINK_LOGS = 
        "CREATE TABLE " + TABLE_UPLINK_LOGS + " (" +
        COLUMN_UPLINK_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_UPLINK_LOG_TIME + " TEXT NOT NULL, " +
        COLUMN_UPLINK_LOG_HEX + " TEXT NOT NULL, " +
        COLUMN_UPLINK_LOG_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    // 创建详细上行数据日志表的SQL语句
    private static final String CREATE_TABLE_DETAILED_UPLINK_LOGS = 
        "CREATE TABLE " + TABLE_DETAILED_UPLINK_LOGS + " (" +
        COLUMN_DETAILED_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_DETAILED_LOG_TIME + " TEXT NOT NULL, " +
        COLUMN_DETAILED_LOG_HEX + " TEXT NOT NULL, " +
        COLUMN_DETAILED_LOG_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_DETAILED_LOG_DEVICE_ID + " TEXT, " +
        COLUMN_DETAILED_LOG_FUNCTION_CODE + " TEXT, " +
        COLUMN_DETAILED_LOG_SEQUENCE_NUMBER + " INTEGER, " +
        COLUMN_DETAILED_LOG_DATA_LENGTH + " INTEGER, " +
        COLUMN_DETAILED_LOG_DATA_TIME + " DATETIME, " +
        COLUMN_DETAILED_LOG_DEVICE_EVENT + " INTEGER, " +
        COLUMN_DETAILED_LOG_DEVICE_STATUS + " INTEGER, " +
        COLUMN_DETAILED_LOG_BATTERY_VOLTAGE + " INTEGER, " +
        COLUMN_DETAILED_LOG_BATTERY_LEVEL + " INTEGER, " +
        COLUMN_DETAILED_LOG_RSSI + " INTEGER, " +
        COLUMN_DETAILED_LOG_DEPARTMENT_NUMBER + " INTEGER, " +
        COLUMN_DETAILED_LOG_CART_NUMBER + " INTEGER, " +
        COLUMN_DETAILED_LOG_DEVICE_COUNT + " INTEGER, " +
        COLUMN_DETAILED_LOG_RACK_NUMBER + " INTEGER, " +
        COLUMN_DETAILED_LOG_PARSE_SUCCESS + " INTEGER DEFAULT 0, " +
        COLUMN_DETAILED_LOG_PARSE_ERROR + " TEXT" +
        ")";
    
    // 创建索引的SQL语句
    private static final String CREATE_INDEX_CATEGORIES_GROUP_ID = 
        "CREATE INDEX idx_categories_group_id ON " + TABLE_CATEGORIES + 
        "(" + COLUMN_CATEGORY_GROUP_ID + ")";
    
    private static DatabaseHelper instance;
    
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    /**
     * 获取数据库帮助类的单例实例
     */
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建分组表
        db.execSQL(CREATE_TABLE_GROUPS);
        
        // 创建分类表
        db.execSQL(CREATE_TABLE_CATEGORIES);
        
        // 创建科室表
        db.execSQL(CREATE_TABLE_DEPARTMENTS);
        
        // 创建职位表
        db.execSQL(CREATE_TABLE_POSITIONS);
        
        // 创建角色表
        db.execSQL(CREATE_TABLE_ROLES);
        
        // 创建权限表
        db.execSQL(CREATE_TABLE_PERMISSIONS);
        
        // 创建角色权限关联表
        db.execSQL(CREATE_TABLE_ROLE_PERMISSIONS);
        
        // 创建用户表
        db.execSQL(CREATE_TABLE_USERS);
        
        // 创建终端表
        db.execSQL(CREATE_TABLE_TERMINALS);
        
        // 创建上行数据日志表
        db.execSQL(CREATE_TABLE_UPLINK_LOGS);
        
        // 创建详细上行数据日志表
        db.execSQL(CREATE_TABLE_DETAILED_UPLINK_LOGS);
        
        // 创建索引
        db.execSQL(CREATE_INDEX_CATEGORIES_GROUP_ID);
        
        // 插入树形权限数据
        insertTreePermissions(db);
        
        // 插入初始数据
        insertInitialData(db);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // 版本1升级到版本2：添加科室表
            db.execSQL(CREATE_TABLE_DEPARTMENTS);
        }
        
        if (oldVersion < 3) {
            // 版本2升级到版本3：添加职位表
            db.execSQL(CREATE_TABLE_POSITIONS);
        }
        
        if (oldVersion < 4) {
            // 创建角色表、权限表和角色权限关联表
            db.execSQL(CREATE_TABLE_ROLES);
            db.execSQL(CREATE_TABLE_PERMISSIONS);
            db.execSQL(CREATE_TABLE_ROLE_PERMISSIONS);
            
            // 插入初始权限数据
            insertInitialPermissions(db);
        }
        
        if (oldVersion < 5) {
            // 版本4升级到版本5：更新权限数据为树形结构
            // 注意：parent_id、level、sort_order字段在版本4创建表时已经存在
            
            // 清空现有权限数据并重新插入树形权限数据
            db.execSQL("DELETE FROM " + TABLE_PERMISSIONS);
            insertTreePermissions(db);
        }
        
        if (oldVersion < 6) {
            // 版本5升级到版本6：更新树形权限数据
            db.execSQL("DELETE FROM " + TABLE_PERMISSIONS);
            insertTreePermissions(db);
        }
        
        if (oldVersion < 7) {
            // 版本6升级到版本7：创建用户表
            db.execSQL(CREATE_TABLE_USERS);
        }
        
        if (oldVersion < 8) {
            // 版本7升级到版本8：插入默认管理员角色和用户
            // 注意：权限数据在版本4升级时已经插入，这里不需要重复插入
            // 插入默认管理员角色
            insertDefaultAdminRole(db);
            // 插入默认管理员用户
            insertDefaultAdminUser(db);
        }
        
        if (oldVersion < 9) {
            // 版本8升级到版本9：更新终端表结构，添加分类ID字段和扩展字段
            // 备份现有终端数据
            db.execSQL("CREATE TEMPORARY TABLE terminals_backup AS SELECT * FROM " + TABLE_TERMINALS);
            
            // 删除原终端表
            db.execSQL("DROP TABLE " + TABLE_TERMINALS);
            
            // 创建新的终端表
            db.execSQL(CREATE_TABLE_TERMINALS);
            
            // 恢复数据（只恢复兼容的字段）
            db.execSQL("INSERT INTO " + TABLE_TERMINALS + " (" +
                      COLUMN_TERMINAL_DEVICE_ID + ", " +
                      COLUMN_TERMINAL_NAME + ", " +
                      COLUMN_TERMINAL_STATUS + ", " +
                      COLUMN_TERMINAL_SIGNAL_STRENGTH + ", " +
                      COLUMN_TERMINAL_DEPARTMENT + ", " +
                      COLUMN_TERMINAL_LOCATION + ", " +
                      COLUMN_TERMINAL_CREATE_TIME + ", " +
                      COLUMN_TERMINAL_UPDATE_TIME + ") " +
                      "SELECT " +
                      COLUMN_TERMINAL_DEVICE_ID + ", " +
                      COLUMN_TERMINAL_NAME + ", " +
                      COLUMN_TERMINAL_STATUS + ", " +
                      COLUMN_TERMINAL_SIGNAL_STRENGTH + ", " +
                      COLUMN_TERMINAL_DEPARTMENT + ", " +
                      COLUMN_TERMINAL_LOCATION + ", " +
                      COLUMN_TERMINAL_CREATE_TIME + ", " +
                      COLUMN_TERMINAL_UPDATE_TIME + " " +
                      "FROM terminals_backup");
            
            // 删除临时表
            db.execSQL("DROP TABLE terminals_backup");
        }
        
        if (oldVersion < 10) {
            // 版本9升级到版本10：创建上行数据日志表
            db.execSQL(CREATE_TABLE_UPLINK_LOGS);
        }
        
        if (oldVersion < 11) {
            // 版本10升级到版本11：为终端表添加上行数据相关字段
            db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_DATA_TIME + " DATETIME");
            db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_DEVICE_EVENT + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_DEVICE_STATUS + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_BATTERY_VOLTAGE + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_BATTERY_LEVEL + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_RSSI + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_DEPARTMENT_NUMBER + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_CART_NUMBER + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_DEVICE_COUNT + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_RACK_NUMBER + " INTEGER");
        }
        
        // 如果需要完全重建数据库，可以取消注释以下代码
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSITIONS);
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEPARTMENTS);
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUPS);
        // onCreate(db);
    }
    
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // 启用外键约束
        db.execSQL("PRAGMA foreign_keys=ON;");
    }
    
    /**
     * 插入初始数据
     */
    private void insertInitialData(SQLiteDatabase db) {
        // 插入分组数据（分类管理的默认值）
        db.execSQL("INSERT INTO " + TABLE_GROUPS + " (" + COLUMN_GROUP_NAME + ", " + 
                  COLUMN_GROUP_DESCRIPTION + ") VALUES ('科室', '医院科室分类管理')");
        
        db.execSQL("INSERT INTO " + TABLE_GROUPS + " (" + COLUMN_GROUP_NAME + ", " + 
                  COLUMN_GROUP_DESCRIPTION + ") VALUES ('病房号', '病房号码分类管理')");
        
        db.execSQL("INSERT INTO " + TABLE_GROUPS + " (" + COLUMN_GROUP_NAME + ", " + 
                  COLUMN_GROUP_DESCRIPTION + ") VALUES ('护理组', '护理组别分类管理')");
        
        db.execSQL("INSERT INTO " + TABLE_GROUPS + " (" + COLUMN_GROUP_NAME + ", " + 
                  COLUMN_GROUP_DESCRIPTION + ") VALUES ('其他', '其他分类管理')");
        
        // 插入分类数据（各个分组下的具体项目）
        // 科室分类 (group_id = 1)
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + 
                  COLUMN_CATEGORY_DESCRIPTION + ", " + COLUMN_CATEGORY_GROUP_ID + ") VALUES ('神经科', '神经内科', 1)");
        
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + 
                  COLUMN_CATEGORY_DESCRIPTION + ", " + COLUMN_CATEGORY_GROUP_ID + ") VALUES ('骨科', '骨科', 1)");
        
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + 
                  COLUMN_CATEGORY_DESCRIPTION + ", " + COLUMN_CATEGORY_GROUP_ID + ") VALUES ('内科', '内科', 1)");
        
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + 
                  COLUMN_CATEGORY_DESCRIPTION + ", " + COLUMN_CATEGORY_GROUP_ID + ") VALUES ('外科', '外科', 1)");
        
        // 病房号分类 (group_id = 2)
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + 
                  COLUMN_CATEGORY_DESCRIPTION + ", " + COLUMN_CATEGORY_GROUP_ID + ") VALUES ('001', '病房001', 2)");
        
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + 
                  COLUMN_CATEGORY_DESCRIPTION + ", " + COLUMN_CATEGORY_GROUP_ID + ") VALUES ('002', '病房002', 2)");
        
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + 
                  COLUMN_CATEGORY_DESCRIPTION + ", " + COLUMN_CATEGORY_GROUP_ID + ") VALUES ('003', '病房003', 2)");
        
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + 
                  COLUMN_CATEGORY_DESCRIPTION + ", " + COLUMN_CATEGORY_GROUP_ID + ") VALUES ('004', '病房004', 2)");
        
        // 护理组分类 (group_id = 3)
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + 
                  COLUMN_CATEGORY_DESCRIPTION + ", " + COLUMN_CATEGORY_GROUP_ID + ") VALUES ('急救组', '急救护理组', 3)");
        
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + 
                  COLUMN_CATEGORY_DESCRIPTION + ", " + COLUMN_CATEGORY_GROUP_ID + ") VALUES ('日常组', '日常护理组', 3)");
        
        // 其他分类 (group_id = 4) - 暂时为空，用户可以自行添加
        
        // 注意：权限数据已经通过insertTreePermissions插入，这里不需要重复插入
        
        // 插入默认管理员角色
        insertDefaultAdminRole(db);
        
        // 插入默认管理员用户
        insertDefaultAdminUser(db);
    }
    
    /**
     * 插入默认管理员角色
     */
    private void insertDefaultAdminRole(SQLiteDatabase db) {
        // 检查管理员角色是否已存在
        android.database.Cursor cursor = db.rawQuery("SELECT " + COLUMN_ROLE_ID + " FROM " + TABLE_ROLES + " WHERE " + COLUMN_ROLE_NAME + " = '管理员'", null);
        long adminRoleId;
        
        if (cursor.moveToFirst()) {
            // 管理员角色已存在，获取其ID
            adminRoleId = cursor.getLong(0);
            cursor.close();
        } else {
            cursor.close();
            // 插入管理员角色
            ContentValues values = new ContentValues();
            values.put(COLUMN_ROLE_NAME, "管理员");
            values.put(COLUMN_ROLE_DESCRIPTION, "系统管理员，拥有所有权限");
            values.put(COLUMN_ROLE_SORT_ORDER, 1);
            values.put(COLUMN_ROLE_STATUS, 1);
            
            adminRoleId = db.insert(TABLE_ROLES, null, values);
        }
        
        // 检查是否已经分配了权限
        cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ROLE_PERMISSIONS + " WHERE " + COLUMN_ROLE_PERMISSION_ROLE_ID + " = ?", new String[]{String.valueOf(adminRoleId)});
        int permissionCount = 0;
        if (cursor.moveToFirst()) {
            permissionCount = cursor.getInt(0);
        }
        cursor.close();
        
        // 如果还没有分配权限，则为管理员角色分配所有权限
        if (permissionCount == 0) {
            db.execSQL("INSERT INTO " + TABLE_ROLE_PERMISSIONS + " (" + 
                      COLUMN_ROLE_PERMISSION_ROLE_ID + ", " + COLUMN_ROLE_PERMISSION_PERMISSION_ID + 
                      ") SELECT " + adminRoleId + ", " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS);
        }
    }
    
    /**
     * 插入默认管理员用户
     */
    private void insertDefaultAdminUser(SQLiteDatabase db) {
        // 检查admin用户是否已存在
        android.database.Cursor cursor = db.rawQuery("SELECT " + COLUMN_USER_ID + " FROM " + TABLE_USERS + " WHERE " + COLUMN_USER_ACCOUNT + " = 'admin'", null);
        
        if (cursor.moveToFirst()) {
            // admin用户已存在，不需要重复插入
            cursor.close();
            return;
        }
        cursor.close();
        
        // 获取管理员角色ID
        cursor = db.rawQuery("SELECT " + COLUMN_ROLE_ID + " FROM " + TABLE_ROLES + " WHERE " + COLUMN_ROLE_NAME + " = '管理员'", null);
        long adminRoleId = 1; // 默认值
        
        if (cursor.moveToFirst()) {
            adminRoleId = cursor.getLong(0);
        }
        cursor.close();
        
        // 插入默认管理员用户 admin/123456
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_NAME, "管理员");
        values.put(COLUMN_USER_ACCOUNT, "admin");
        values.put(COLUMN_USER_PASSWORD, "123456");
        values.put(COLUMN_USER_ROLE_ID, adminRoleId);
        values.put(COLUMN_USER_STATUS, 1);
        values.put(COLUMN_USER_GENDER, 1);
        
        long userId = db.insert(TABLE_USERS, null, values);
        
        if (userId > 0) {
            Log.d("DatabaseHelper", "默认管理员用户创建成功，用户ID: " + userId + ", 角色ID: " + adminRoleId);
        } else {
            Log.e("DatabaseHelper", "默认管理员用户创建失败");
        }
    }
    
    /**
     * 插入初始权限数据
     */
    private void insertInitialPermissions(SQLiteDatabase db) {
        // 终端管理权限
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('terminal_list', '终端列表', 'terminal', '查看终端列表')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('terminal_add', '添加终端', 'terminal', '添加新终端设备')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('terminal_detail', '终端详情', 'terminal', '查看终端详细信息')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('terminal_edit', '编辑终端', 'terminal', '编辑终端信息')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('terminal_delete', '删除终端', 'terminal', '删除终端设备')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('terminal_mark', '标记终端', 'terminal', '标记终端状态')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('terminal_confirm', '确认处理', 'terminal', '确认终端处理结果')");
        
        // 日志管理权限
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('log_info', '日志信息', 'log', '查看日志信息')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('log_export', '导出日志', 'log', '导出日志文件')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('log_confirm', '确认处理', 'log', '确认日志处理结果')");
        
        // 清理终端权限
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('clean_terminal', '清理终端', 'clean', '清理终端数据')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('clean_export', '导出清理', 'clean', '导出清理数据')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('clean_start_count', '开始清点', 'clean', '开始清点操作')");
        
        // 设置权限
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('setting_device', '设备设置', 'setting', '设备相关设置')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('setting_sound', '声音设置', 'setting', '声音相关设置')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('setting_wifi', 'WiFi连接', 'setting', 'WiFi连接设置')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('setting_ip', 'IP配置', 'setting', 'IP地址配置')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('setting_count', '清点次数', 'setting', '清点次数设置')");
        
        // 角色管理权限
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('role_add', '新增角色', 'role', '新增角色')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('role_edit', '编辑角色', 'role', '编辑角色信息')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('role_delete', '删除角色', 'role', '删除角色')");
        
        // 用户管理权限
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('user_add', '新增用户', 'user', '新增用户账户')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('user_edit', '编辑用户', 'user', '编辑用户信息')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('user_delete', '删除用户', 'user', '删除用户账户')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('user_reset_password', '重置密码', 'user', '重置用户密码')");
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('user_disable', '启用/禁用用户', 'user', '启用或禁用用户账户')");
    }
    
    /**
     * 插入树形权限数据
     */
    private void insertTreePermissions(SQLiteDatabase db) {
        // 插入完整的树形权限数据，使用正确的SQL语法
        
        // Level 0 - 顶级权限
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('terminal_list', '终端列表', 'terminal', '终端管理模块', 1, NULL, 0, 1)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('log_info', '日志信息', 'log', '日志管理模块', 1, NULL, 0, 2)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('clean_terminal', '清理终端', 'clean', '清理终端模块', 1, NULL, 0, 3)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('setting', '设置', 'setting', '系统设置模块', 1, NULL, 0, 4)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('role_management', '角色管理', 'role', '角色管理模块', 1, NULL, 0, 5)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('user_management', '用户管理', 'user', '用户管理模块', 1, NULL, 0, 6)");

        // Level 1 - 终端列表的子权限
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('terminal_add', '添加终端', 'terminal', '添加新终端设备', 1, 1, 1, 1)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('terminal_detail', '终端详情', 'terminal', '查看终端详细信息', 1, 1, 1, 2)");

        // Level 2 - 终端详情的子权限
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('terminal_edit', '编辑', 'terminal', '编辑终端信息', 1, 8, 2, 1)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('terminal_delete', '删除', 'terminal', '删除终端设备', 1, 8, 2, 2)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('terminal_mark', '标记', 'terminal', '标记终端状态', 1, 8, 2, 3)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('terminal_confirm', '确认处理', 'terminal', '确认终端处理结果', 1, 8, 2, 4)");

        // Level 1 - 日志信息的子权限
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('log_export', '导出', 'log', '导出日志文件', 1, 2, 1, 1)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('log_confirm', '确认处理', 'log', '确认日志处理结果', 1, 2, 1, 2)");

        // Level 1 - 清理终端的子权限
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('clean_export', '导出', 'clean', '导出清理数据', 1, 3, 1, 1)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('clean_start_count', '开始清点', 'clean', '开始清点操作', 1, 3, 1, 2)");

        // Level 1 - 设置的子权限
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('setting_device', '设备设置', 'setting', '设备相关设置', 1, 4, 1, 1)");



        // Level 2 - 设备设置的子权限
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('setting_sound', '声音设置', 'setting', '声音相关设置', 1, 16, 2, 1)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('setting_wifi', 'WiFi连接', 'setting', 'WiFi连接设置', 1, 16, 2, 2)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('setting_ip', 'IP配置', 'setting', 'IP地址配置', 1, 16, 2, 3)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('setting_count', '清点次数', 'setting', '清点次数设置', 1, 16, 2, 4)");

        // Level 1 - 角色管理的子权限
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('role_add', '新增', 'role', '新增角色', 1, 5, 1, 1)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('role_edit', '编辑', 'role', '编辑角色信息', 1, 5, 1, 2)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('role_delete', '删除', 'role', '删除角色', 1, 5, 1, 3)");

        // Level 1 - 用户管理的子权限
        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('user_add', '新增', 'user', '新增用户', 1, 6, 1, 1)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('user_edit', '编辑', 'user', '编辑用户信息', 1, 6, 1, 2)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('user_delete', '删除', 'user', '删除用户', 1, 6, 1, 3)");

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('user_reset_password', '重置密码', 'user', '重置用户密码', 1, 6, 1, 4)");
    }
    
    // 终端相关的数据库操作方法
    
    /**
     * 检查终端是否已存在
     */
    public boolean isTerminalExists(String terminalDeviceId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_TERMINALS + 
                      " WHERE " + COLUMN_TERMINAL_DEVICE_ID + " = ?";
        android.database.Cursor cursor = db.rawQuery(query, new String[]{terminalDeviceId});
        
        boolean exists = false;
        if (cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0;
        }
        cursor.close();
        return exists;
    }
    
    /**
     * 添加新终端到数据库
     */
    public long addTerminal(com.lora.cn.ui.model.Terminal terminal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_TERMINAL_DEVICE_ID, terminal.getTerminalId());
        values.put(COLUMN_TERMINAL_NAME, terminal.getTerminalName());
        values.put(COLUMN_TERMINAL_STATUS, terminal.getStatus());
        values.put(COLUMN_TERMINAL_SIGNAL_STRENGTH, terminal.getSignalStrength());
        values.put(COLUMN_TERMINAL_DEPARTMENT, terminal.getDepartment());
        values.put(COLUMN_TERMINAL_LOCATION, terminal.getLocation());
        
        long result = db.insert(TABLE_TERMINALS, null, values);
        return result;
    }
    
    /**
     * 更新终端信息
     */
    public boolean updateTerminal(com.lora.cn.ui.model.Terminal terminal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_TERMINAL_DEVICE_ID, terminal.getTerminalId());
        values.put(COLUMN_TERMINAL_NAME, terminal.getTerminalName());
        values.put(COLUMN_TERMINAL_STATUS, terminal.getStatus());
        values.put(COLUMN_TERMINAL_SIGNAL_STRENGTH, terminal.getSignalStrength());
        values.put(COLUMN_TERMINAL_DEPARTMENT, terminal.getDepartment());
        values.put(COLUMN_TERMINAL_LOCATION, terminal.getLocation());
        
        // 上行数据相关字段
        if (terminal.getDataTime() != null) {
            values.put(COLUMN_TERMINAL_DATA_TIME, terminal.getDataTime().getTime());
        }
        if (terminal.getDeviceEvent() != null) {
            values.put(COLUMN_TERMINAL_DEVICE_EVENT, terminal.getDeviceEvent());
        }
        if (terminal.getDeviceStatus() != null) {
            values.put(COLUMN_TERMINAL_DEVICE_STATUS, terminal.getDeviceStatus());
        }
        if (terminal.getBatteryVoltage() != null) {
            values.put(COLUMN_TERMINAL_BATTERY_VOLTAGE, terminal.getBatteryVoltage());
        }
        if (terminal.getBatteryLevel() != null) {
            values.put(COLUMN_TERMINAL_BATTERY_LEVEL, terminal.getBatteryLevel());
        }
        if (terminal.getRssi() != null) {
            values.put(COLUMN_TERMINAL_RSSI, terminal.getRssi());
        }
        if (terminal.getDepartmentNumber() != null) {
            values.put(COLUMN_TERMINAL_DEPARTMENT_NUMBER, terminal.getDepartmentNumber());
        }
        if (terminal.getCartNumber() != null) {
            values.put(COLUMN_TERMINAL_CART_NUMBER, terminal.getCartNumber());
        }
        if (terminal.getDeviceCount() != null) {
            values.put(COLUMN_TERMINAL_DEVICE_COUNT, terminal.getDeviceCount());
        }
        if (terminal.getRackNumber() != null) {
            values.put(COLUMN_TERMINAL_RACK_NUMBER, terminal.getRackNumber());
        }
        
        values.put(COLUMN_TERMINAL_UPDATE_TIME, System.currentTimeMillis());
        
        int rowsAffected = db.update(TABLE_TERMINALS, values, 
                COLUMN_TERMINAL_DEVICE_ID + "=?", 
                new String[]{terminal.getTerminalId()});
        
        return rowsAffected > 0;
    }
    
    /**
     * 根据设备ID获取终端信息
     */
    public com.lora.cn.ui.model.Terminal getTerminalByDeviceId(String deviceId) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        String query = "SELECT * FROM " + TABLE_TERMINALS + " WHERE " + COLUMN_TERMINAL_DEVICE_ID + " = ?";
        android.database.Cursor cursor = db.rawQuery(query, new String[]{deviceId});
        
        com.lora.cn.ui.model.Terminal terminal = null;
        if (cursor.moveToFirst()) {
            terminal = new com.lora.cn.ui.model.Terminal();
            terminal.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_ID)));
            terminal.setTerminalId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_DEVICE_ID)));
            terminal.setTerminalName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_NAME)));
            terminal.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_STATUS)));
            terminal.setSignalStrength(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_SIGNAL_STRENGTH)));
            terminal.setDepartment(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_DEPARTMENT)));
            terminal.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_LOCATION)));
            
            // 上行数据相关字段
            int dataTimeIndex = cursor.getColumnIndex(COLUMN_TERMINAL_DATA_TIME);
            if (!cursor.isNull(dataTimeIndex)) {
                terminal.setDataTime(new java.util.Date(cursor.getLong(dataTimeIndex)));
            }
            
            int deviceEventIndex = cursor.getColumnIndex(COLUMN_TERMINAL_DEVICE_EVENT);
            if (!cursor.isNull(deviceEventIndex)) {
                terminal.setDeviceEvent(cursor.getLong(deviceEventIndex));
            }
            
            int deviceStatusIndex = cursor.getColumnIndex(COLUMN_TERMINAL_DEVICE_STATUS);
            if (!cursor.isNull(deviceStatusIndex)) {
                terminal.setDeviceStatus(cursor.getLong(deviceStatusIndex));
            }
            
            int batteryVoltageIndex = cursor.getColumnIndex(COLUMN_TERMINAL_BATTERY_VOLTAGE);
            if (!cursor.isNull(batteryVoltageIndex)) {
                terminal.setBatteryVoltage(cursor.getInt(batteryVoltageIndex));
            }
            
            int batteryLevelIndex = cursor.getColumnIndex(COLUMN_TERMINAL_BATTERY_LEVEL);
            if (!cursor.isNull(batteryLevelIndex)) {
                terminal.setBatteryLevel(cursor.getInt(batteryLevelIndex));
            }
            
            int rssiIndex = cursor.getColumnIndex(COLUMN_TERMINAL_RSSI);
            if (!cursor.isNull(rssiIndex)) {
                terminal.setRssi(cursor.getInt(rssiIndex));
            }
            
            int departmentNumberIndex = cursor.getColumnIndex(COLUMN_TERMINAL_DEPARTMENT_NUMBER);
            if (!cursor.isNull(departmentNumberIndex)) {
                terminal.setDepartmentNumber(cursor.getInt(departmentNumberIndex));
            }
            
            int cartNumberIndex = cursor.getColumnIndex(COLUMN_TERMINAL_CART_NUMBER);
            if (!cursor.isNull(cartNumberIndex)) {
                terminal.setCartNumber(cursor.getInt(cartNumberIndex));
            }
            
            int deviceCountIndex = cursor.getColumnIndex(COLUMN_TERMINAL_DEVICE_COUNT);
            if (!cursor.isNull(deviceCountIndex)) {
                terminal.setDeviceCount(cursor.getInt(deviceCountIndex));
            }
            
            int rackNumberIndex = cursor.getColumnIndex(COLUMN_TERMINAL_RACK_NUMBER);
            if (!cursor.isNull(rackNumberIndex)) {
                terminal.setRackNumber(cursor.getInt(rackNumberIndex));
            }
        }
        cursor.close();
        return terminal;
    }
    
    /**
     * 获取所有终端列表
     */
    public java.util.List<com.lora.cn.ui.model.Terminal> getAllTerminals() {
        java.util.List<com.lora.cn.ui.model.Terminal> terminals = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String query = "SELECT * FROM " + TABLE_TERMINALS + " ORDER BY " + COLUMN_TERMINAL_CREATE_TIME + " DESC";
        android.database.Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                com.lora.cn.ui.model.Terminal terminal = new com.lora.cn.ui.model.Terminal();
                terminal.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_ID)));
                terminal.setTerminalId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_DEVICE_ID)));
                terminal.setTerminalName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_NAME)));
                terminal.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_STATUS)));
                terminal.setSignalStrength(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_SIGNAL_STRENGTH)));
                terminal.setDepartment(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_DEPARTMENT)));
                terminal.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_LOCATION)));
                
                terminals.add(terminal);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return terminals;
    }
    
    // 上行数据日志相关方法
    
    /**
     * 添加上行数据日志
     * @param time 时间
     * @param hex hex数据
     * @return 插入的行ID，失败返回-1
     */
    public long addUplinkLog(String time, String hex) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_UPLINK_LOG_TIME, time);
        values.put(COLUMN_UPLINK_LOG_HEX, hex);
        
        long result = db.insert(TABLE_UPLINK_LOGS, null, values);
        db.close();
        return result;
    }
    
    /**
     * 获取所有上行数据日志
     * @return 上行数据日志列表
     */
    public java.util.List<UplinkLog> getAllUplinkLogs() {
        java.util.List<UplinkLog> logs = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        android.database.Cursor cursor = db.query(TABLE_UPLINK_LOGS, null, null, null, null, null, 
                COLUMN_UPLINK_LOG_CREATE_TIME + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                UplinkLog log = new UplinkLog();
                log.setLogId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_UPLINK_LOG_ID)));
                log.setTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPLINK_LOG_TIME)));
                log.setHex(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPLINK_LOG_HEX)));
                log.setCreateTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPLINK_LOG_CREATE_TIME)));
                logs.add(log);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return logs;
    }
    
    /**
     * 根据时间范围获取上行数据日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 上行数据日志列表
     */
    public java.util.List<UplinkLog> getUplinkLogsByTimeRange(String startTime, String endTime) {
        java.util.List<UplinkLog> logs = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selection = COLUMN_UPLINK_LOG_CREATE_TIME + " BETWEEN ? AND ?";
        String[] selectionArgs = {startTime, endTime};
        
        android.database.Cursor cursor = db.query(TABLE_UPLINK_LOGS, null, selection, selectionArgs, 
                null, null, COLUMN_UPLINK_LOG_CREATE_TIME + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                UplinkLog log = new UplinkLog();
                log.setLogId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_UPLINK_LOG_ID)));
                log.setTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPLINK_LOG_TIME)));
                log.setHex(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPLINK_LOG_HEX)));
                log.setCreateTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPLINK_LOG_CREATE_TIME)));
                logs.add(log);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return logs;
    }
    
    /**
     * 删除指定时间之前的上行数据日志
     * @param beforeTime 指定时间
     * @return 删除的行数
     */
    public int deleteUplinkLogsBefore(String beforeTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = COLUMN_UPLINK_LOG_CREATE_TIME + " < ?";
        String[] whereArgs = {beforeTime};
        
        int deletedRows = db.delete(TABLE_UPLINK_LOGS, whereClause, whereArgs);
        db.close();
        return deletedRows;
    }
    
    /**
     * 上行数据日志实体类
     */
    public static class UplinkLog {
        private int logId;
        private String time;
        private String hex;
        private String createTime;
        
        public int getLogId() { return logId; }
        public void setLogId(int logId) { this.logId = logId; }
        
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        
        public String getHex() { return hex; }
        public void setHex(String hex) { this.hex = hex; }
        
        public String getCreateTime() { return createTime; }
        public void setCreateTime(String createTime) { this.createTime = createTime; }
    }
    
    /**
     * 添加详细上行数据日志
     * @param log 详细日志对象
     * @return 插入的行ID
     */
    public long addDetailedUplinkLog(com.lora.cn.database.entity.DetailedUplinkLog log) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_DETAILED_LOG_TIME, log.getTime());
        values.put(COLUMN_DETAILED_LOG_HEX, log.getHex());
        
        // 处理createTime - 将Date转换为String
        if (log.getCreateTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            values.put(COLUMN_DETAILED_LOG_CREATE_TIME, sdf.format(log.getCreateTime()));
        } else {
            values.put(COLUMN_DETAILED_LOG_CREATE_TIME, "");
        }
        
        values.put(COLUMN_DETAILED_LOG_DEVICE_ID, log.getDeviceId());
        values.put(COLUMN_DETAILED_LOG_FUNCTION_CODE, log.getFunctionCode());
        values.put(COLUMN_DETAILED_LOG_SEQUENCE_NUMBER, log.getSequenceNumber());
        values.put(COLUMN_DETAILED_LOG_DATA_LENGTH, log.getDataLength());
        
        // 处理dataTime - 将Date转换为String
        if (log.getDataTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            values.put(COLUMN_DETAILED_LOG_DATA_TIME, sdf.format(log.getDataTime()));
        } else {
            values.put(COLUMN_DETAILED_LOG_DATA_TIME, "");
        }
        values.put(COLUMN_DETAILED_LOG_DEVICE_EVENT, log.getDeviceEvent());
        values.put(COLUMN_DETAILED_LOG_DEVICE_STATUS, log.getDeviceStatus());
        values.put(COLUMN_DETAILED_LOG_BATTERY_VOLTAGE, log.getBatteryVoltage());
        values.put(COLUMN_DETAILED_LOG_BATTERY_LEVEL, log.getBatteryLevel());
        values.put(COLUMN_DETAILED_LOG_RSSI, log.getRssi());
        values.put(COLUMN_DETAILED_LOG_DEPARTMENT_NUMBER, log.getDepartmentNumber());
        values.put(COLUMN_DETAILED_LOG_CART_NUMBER, log.getCartNumber());
        values.put(COLUMN_DETAILED_LOG_DEVICE_COUNT, log.getDeviceCount());
        values.put(COLUMN_DETAILED_LOG_RACK_NUMBER, log.getRackNumber());
        values.put(COLUMN_DETAILED_LOG_PARSE_SUCCESS, log.isParseSuccess() ? 1 : 0);
        values.put(COLUMN_DETAILED_LOG_PARSE_ERROR, log.getParseError());
        
        long id = db.insert(TABLE_DETAILED_UPLINK_LOGS, null, values);
        db.close();
        return id;
    }
    
    /**
     * 获取所有详细上行数据日志
     * @return 详细日志列表
     */
    public java.util.List<com.lora.cn.database.entity.DetailedUplinkLog> getAllDetailedUplinkLogs() {
        java.util.List<com.lora.cn.database.entity.DetailedUplinkLog> logs = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        android.database.Cursor cursor = db.query(TABLE_DETAILED_UPLINK_LOGS, null, null, null, null, null, 
                COLUMN_DETAILED_LOG_CREATE_TIME + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                com.lora.cn.database.entity.DetailedUplinkLog log = new com.lora.cn.database.entity.DetailedUplinkLog();
                log.setLogId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_ID)));
                log.setTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_TIME)));
                log.setHex(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_HEX)));
                // 处理日期字段
                String createTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_CREATE_TIME));
                if (createTimeStr != null) {
                    try {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        log.setCreateTime(sdf.parse(createTimeStr));
                    } catch (java.text.ParseException e) {
                        log.setCreateTime(new java.util.Date());
                    }
                }
                
                String dataTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_DATA_TIME));
                if (dataTimeStr != null) {
                    try {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        log.setDataTime(sdf.parse(dataTimeStr));
                    } catch (java.text.ParseException e) {
                        log.setDataTime(new java.util.Date());
                    }
                }
                
                log.setDeviceId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_DEVICE_ID)));
                log.setFunctionCode(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_FUNCTION_CODE)));
                log.setSequenceNumber(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_SEQUENCE_NUMBER)));
                log.setDataLength(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_DATA_LENGTH)));
                log.setDeviceEvent(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_DEVICE_EVENT)));
                log.setDeviceStatus(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_DEVICE_STATUS)));
                log.setBatteryVoltage(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_BATTERY_VOLTAGE)));
                log.setBatteryLevel(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_BATTERY_LEVEL)));
                log.setRssi(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_RSSI)));
                log.setDepartmentNumber(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_DEPARTMENT_NUMBER)));
                log.setCartNumber(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_CART_NUMBER)));
                log.setDeviceCount(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_DEVICE_COUNT)));
                log.setRackNumber(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_RACK_NUMBER)));
                log.setParseSuccess(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_PARSE_SUCCESS)) == 1);
                log.setParseError(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_PARSE_ERROR)));
                logs.add(log);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return logs;
    }
    
    /**
     * 根据时间范围获取详细上行数据日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 详细日志列表
     */
    public java.util.List<com.lora.cn.database.entity.DetailedUplinkLog> getDetailedUplinkLogsByTimeRange(String startTime, String endTime) {
        java.util.List<com.lora.cn.database.entity.DetailedUplinkLog> logs = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selection = COLUMN_DETAILED_LOG_CREATE_TIME + " BETWEEN ? AND ?";
        String[] selectionArgs = {startTime, endTime};
        
        android.database.Cursor cursor = db.query(TABLE_DETAILED_UPLINK_LOGS, null, selection, selectionArgs, 
                null, null, COLUMN_DETAILED_LOG_CREATE_TIME + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                com.lora.cn.database.entity.DetailedUplinkLog log = new com.lora.cn.database.entity.DetailedUplinkLog();
                log.setLogId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_ID)));
                log.setTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_TIME)));
                log.setHex(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_HEX)));
                // 设置createTime
                String createTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_CREATE_TIME));
                if (createTimeStr != null) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        log.setCreateTime(sdf.parse(createTimeStr));
                    } catch (java.text.ParseException e) {
                        log.setCreateTime(new java.util.Date());
                    }
                } else {
                    log.setCreateTime(new java.util.Date());
                }
                log.setDeviceId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_DEVICE_ID)));
                log.setFunctionCode(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_FUNCTION_CODE)));
                log.setSequenceNumber(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_SEQUENCE_NUMBER)));
                log.setDataLength(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_DATA_LENGTH)));
                // 设置dataTime
                String dataTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_DATA_TIME));
                if (dataTimeStr != null) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        log.setDataTime(sdf.parse(dataTimeStr));
                    } catch (java.text.ParseException e) {
                        log.setDataTime(new java.util.Date());
                    }
                } else {
                    log.setDataTime(new java.util.Date());
                }
                log.setDeviceEvent(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_DEVICE_EVENT)));
                log.setDeviceStatus(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_DEVICE_STATUS)));
                log.setBatteryVoltage(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_BATTERY_VOLTAGE)));
                log.setBatteryLevel(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_BATTERY_LEVEL)));
                log.setRssi(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_RSSI)));
                log.setDepartmentNumber(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_DEPARTMENT_NUMBER)));
                log.setCartNumber(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_CART_NUMBER)));
                log.setDeviceCount(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_DEVICE_COUNT)));
                log.setRackNumber(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_RACK_NUMBER)));
                log.setParseSuccess(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_PARSE_SUCCESS)) == 1);
                log.setParseError(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAILED_LOG_PARSE_ERROR)));
                logs.add(log);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return logs;
    }
    
    /**
     * 删除指定时间之前的详细上行数据日志
     * @param beforeTime 指定时间
     * @return 删除的行数
     */
    public int deleteDetailedUplinkLogsBefore(String beforeTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = COLUMN_DETAILED_LOG_CREATE_TIME + " < ?";
        String[] whereArgs = {beforeTime};
        
        int deletedRows = db.delete(TABLE_DETAILED_UPLINK_LOGS, whereClause, whereArgs);
        db.close();
        return deletedRows;
    }
}