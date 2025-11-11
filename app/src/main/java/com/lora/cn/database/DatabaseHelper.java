package com.lora.cn.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 数据库帮助类
 * 管理分组表和分类表的两层关系结构
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "lora_app.db";
    private static final int DATABASE_VERSION = 15;
    
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
    
    // 日志表
    public static final String TABLE_LOGS = "logs";
    public static final String COLUMN_LOG_ID = "log_id";
    public static final String COLUMN_LOG_TERMINAL_ID = "terminal_id";
    public static final String COLUMN_LOG_TERMINAL_NAME = "terminal_name";
    public static final String COLUMN_LOG_DEVICE_ID = "device_id";
    public static final String COLUMN_LOG_STATUS = "status";
    public static final String COLUMN_LOG_OPERATOR = "operator";
    public static final String COLUMN_LOG_OPERATION_TIME = "operation_time";
    public static final String COLUMN_LOG_ACTION = "action";
    public static final String COLUMN_LOG_CREATE_TIME = "create_time";
    
    // 终端表
    public static final String TABLE_TERMINALS = "terminals";
    public static final String COLUMN_TERMINAL_ID = "terminal_id";
    public static final String COLUMN_TERMINAL_DEVICE_ID = "terminal_device_id";
    public static final String COLUMN_TERMINAL_DEVICE_CODE = "device_code"; // 新增设备CODE字段
    public static final String COLUMN_TERMINAL_NAME = "terminal_name";
    public static final String COLUMN_TERMINAL_STATUS = "status";
    public static final String COLUMN_TERMINAL_SIGNAL_STRENGTH = "signal_strength";
    public static final String COLUMN_TERMINAL_BATTERY_LEVEL = "battery_level"; // 电量字段
    public static final String COLUMN_TERMINAL_BATTERY_VOLTAGE = "battery_voltage"; // 电池电压字段 (单位0.01V)
    public static final String COLUMN_TERMINAL_RSSI = "rssi"; // 原始RSSI字段 (0~138 对应 -138~0dBm)
    public static final String COLUMN_TERMINAL_DEPARTMENT = "department";
    public static final String COLUMN_TERMINAL_LOCATION = "location";
    public static final String COLUMN_TERMINAL_DEPARTMENT_ID = "department_id"; // 科室分类ID
    public static final String COLUMN_TERMINAL_ROOM_ID = "room_id"; // 病房号分类ID
    public static final String COLUMN_TERMINAL_NURSING_GROUP_ID = "nursing_group_id"; // 护理组分类ID
    public static final String COLUMN_TERMINAL_OTHER_ID = "other_id"; // 其他分类ID
    public static final String COLUMN_TERMINAL_EXTENSION = "extension"; // 扩展字段
    public static final String COLUMN_TERMINAL_IS_FAVORITE = "is_favorite"; // 收藏状态
    public static final String COLUMN_TERMINAL_CREATE_TIME = "create_time";
    public static final String COLUMN_TERMINAL_UPDATE_TIME = "update_time";
    
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
        COLUMN_TERMINAL_DEVICE_CODE + " TEXT, " +
        COLUMN_TERMINAL_NAME + " TEXT NOT NULL, " +
        COLUMN_TERMINAL_STATUS + " TEXT DEFAULT '在线', " +
        COLUMN_TERMINAL_SIGNAL_STRENGTH + " INTEGER DEFAULT 0, " +
        COLUMN_TERMINAL_BATTERY_LEVEL + " INTEGER DEFAULT 100, " +
        COLUMN_TERMINAL_BATTERY_VOLTAGE + " INTEGER DEFAULT 0, " +
        COLUMN_TERMINAL_RSSI + " INTEGER DEFAULT 0, " +
        COLUMN_TERMINAL_DEPARTMENT + " TEXT, " +
        COLUMN_TERMINAL_LOCATION + " TEXT, " +
        COLUMN_TERMINAL_DEPARTMENT_ID + " INTEGER, " +
        COLUMN_TERMINAL_ROOM_ID + " INTEGER, " +
        COLUMN_TERMINAL_NURSING_GROUP_ID + " INTEGER, " +
        COLUMN_TERMINAL_OTHER_ID + " INTEGER, " +
        COLUMN_TERMINAL_EXTENSION + " TEXT, " +
        COLUMN_TERMINAL_IS_FAVORITE + " INTEGER DEFAULT 0, " +
        COLUMN_TERMINAL_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_TERMINAL_UPDATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        "FOREIGN KEY (" + COLUMN_TERMINAL_DEPARTMENT_ID + ") REFERENCES " + 
        TABLE_CATEGORIES + "(" + COLUMN_CATEGORY_ID + ") ON DELETE SET NULL, " +
        "FOREIGN KEY (" + COLUMN_TERMINAL_ROOM_ID + ") REFERENCES " + 
        TABLE_CATEGORIES + "(" + COLUMN_CATEGORY_ID + ") ON DELETE SET NULL, " +
        "FOREIGN KEY (" + COLUMN_TERMINAL_NURSING_GROUP_ID + ") REFERENCES " + 
        TABLE_CATEGORIES + "(" + COLUMN_CATEGORY_ID + ") ON DELETE SET NULL, " +
        "FOREIGN KEY (" + COLUMN_TERMINAL_OTHER_ID + ") REFERENCES " + 
        TABLE_CATEGORIES + "(" + COLUMN_CATEGORY_ID + ") ON DELETE SET NULL" +
        ")";
    
    // 创建日志表的SQL语句
    private static final String CREATE_TABLE_LOGS = 
        "CREATE TABLE " + TABLE_LOGS + " (" +
        COLUMN_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_LOG_TERMINAL_ID + " TEXT NOT NULL, " +
        COLUMN_LOG_TERMINAL_NAME + " TEXT NOT NULL, " +
        COLUMN_LOG_DEVICE_ID + " TEXT NOT NULL, " +
        COLUMN_LOG_STATUS + " TEXT NOT NULL, " +
        COLUMN_LOG_OPERATOR + " TEXT, " +
        COLUMN_LOG_OPERATION_TIME + " TEXT, " +
        COLUMN_LOG_ACTION + " TEXT NOT NULL, " +
        COLUMN_LOG_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
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
        
        // 创建日志表
        db.execSQL(CREATE_TABLE_LOGS);
        
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
            // 版本9升级到版本10：添加is_favorite列和battery_level列
            db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + 
                      COLUMN_TERMINAL_IS_FAVORITE + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + 
                      COLUMN_TERMINAL_BATTERY_LEVEL + " INTEGER DEFAULT 100");
        }
        
        if (oldVersion < 11) {
            // 版本10升级到版本11：创建日志表
            db.execSQL(CREATE_TABLE_LOGS);
        }
        // 版本13 -> 14：新增设备CODE字段
        if (oldVersion < 14) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_DEVICE_CODE + " TEXT");
            } catch (Exception ignored) {}
        }
        // 版本14 -> 15：为终端添加电池电压与RSSI原始值列
        if (oldVersion < 15) {
            try { db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_BATTERY_VOLTAGE + " INTEGER DEFAULT 0"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_RSSI + " INTEGER DEFAULT 0"); } catch (Exception ignored) {}
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
        // 设备CODE（新字段）
        values.put(COLUMN_TERMINAL_DEVICE_CODE, terminal.getDeviceCode());
        values.put(COLUMN_TERMINAL_NAME, terminal.getTerminalName());
        values.put(COLUMN_TERMINAL_STATUS, terminal.getStatus());
        values.put(COLUMN_TERMINAL_SIGNAL_STRENGTH, terminal.getSignalStrength());
        values.put(COLUMN_TERMINAL_BATTERY_LEVEL, terminal.getBatteryLevel());
        values.put(COLUMN_TERMINAL_BATTERY_VOLTAGE, terminal.getBatteryVoltage());
        values.put(COLUMN_TERMINAL_RSSI, terminal.getRssi());
        values.put(COLUMN_TERMINAL_DEPARTMENT, terminal.getDepartment());
        values.put(COLUMN_TERMINAL_LOCATION, terminal.getLocation());
        
        long result = db.insert(TABLE_TERMINALS, null, values);
        return result;
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
                terminal.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_TERMINAL_ID)));
                terminal.setTerminalId(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_DEVICE_ID)));
                int codeIdx = cursor.getColumnIndex(COLUMN_TERMINAL_DEVICE_CODE);
                if (codeIdx != -1) {
                    terminal.setDeviceCode(cursor.getString(codeIdx));
                }
                terminal.setTerminalName(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_NAME)));
                terminal.setStatus(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_STATUS)));
                terminal.setSignalStrength(cursor.getInt(cursor.getColumnIndex(COLUMN_TERMINAL_SIGNAL_STRENGTH)));
                
                // 设置电量
                int batteryLevelIndex = cursor.getColumnIndex(COLUMN_TERMINAL_BATTERY_LEVEL);
                if (batteryLevelIndex != -1) {
                    terminal.setBatteryLevel(cursor.getInt(batteryLevelIndex));
                } else {
                    // 如果没有电量字段，使用信号强度作为默认值
                    terminal.setBatteryLevel(cursor.getInt(cursor.getColumnIndex(COLUMN_TERMINAL_SIGNAL_STRENGTH)));
                }
                
                terminal.setDepartment(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_DEPARTMENT)));
                terminal.setLocation(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_LOCATION)));

                // 读取电池电压与原始RSSI（若存在列）
                int batteryVoltageIndex = cursor.getColumnIndex(COLUMN_TERMINAL_BATTERY_VOLTAGE);
                if (batteryVoltageIndex != -1) {
                    terminal.setBatteryVoltage(cursor.getInt(batteryVoltageIndex));
                }
                int rssiIndex = cursor.getColumnIndex(COLUMN_TERMINAL_RSSI);
                if (rssiIndex != -1) {
                    terminal.setRssi(cursor.getInt(rssiIndex));
                }
                
                // 设置分类ID
                terminal.setDepartmentId(cursor.getLong(cursor.getColumnIndex(COLUMN_TERMINAL_DEPARTMENT_ID)));
                terminal.setRoomId(cursor.getLong(cursor.getColumnIndex(COLUMN_TERMINAL_ROOM_ID)));
                terminal.setNursingGroupId(cursor.getLong(cursor.getColumnIndex(COLUMN_TERMINAL_NURSING_GROUP_ID)));
                terminal.setOtherId(cursor.getLong(cursor.getColumnIndex(COLUMN_TERMINAL_OTHER_ID)));
                
                // 设置扩展字段
                terminal.setExtension(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_EXTENSION)));
                
                // 设置收藏状态
                terminal.setFavorite(cursor.getInt(cursor.getColumnIndex(COLUMN_TERMINAL_IS_FAVORITE)) == 1);
                
                // 设置时间戳
                terminal.setCreateTime(cursor.getLong(cursor.getColumnIndex(COLUMN_TERMINAL_CREATE_TIME)));
                terminal.setUpdateTime(cursor.getLong(cursor.getColumnIndex(COLUMN_TERMINAL_UPDATE_TIME)));
                
                terminals.add(terminal);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return terminals;
    }

    // 日志操作方法
    public long addLog(String terminalId, String terminalName, String deviceId, String status, String operator, String operationTime, String action) {
        // 仅记录上行/下行日志，其它操作不入库
        if (!isLoggableAction(action)) {
            return -1;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LOG_TERMINAL_ID, terminalId);
        values.put(COLUMN_LOG_TERMINAL_NAME, terminalName);
        values.put(COLUMN_LOG_DEVICE_ID, deviceId);
        values.put(COLUMN_LOG_STATUS, status);
        values.put(COLUMN_LOG_OPERATOR, operator);
        values.put(COLUMN_LOG_OPERATION_TIME, operationTime);
        values.put(COLUMN_LOG_ACTION, action);
        long result = db.insert(TABLE_LOGS, null, values);
        db.close();
        return result;
    }
    
    // 重载的日志操作方法，接受LogInfo对象
    public long addLog(com.lora.cn.ui.model.LogInfo logInfo) {
        // 仅记录上行/下行日志，其它操作不入库
        if (!isLoggableAction(logInfo.getAction())) {
            return -1;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LOG_TERMINAL_ID, logInfo.getTerminalId());
        values.put(COLUMN_LOG_TERMINAL_NAME, logInfo.getTerminalName());
        values.put(COLUMN_LOG_DEVICE_ID, logInfo.getDeviceId());
        values.put(COLUMN_LOG_STATUS, logInfo.getStatus());
        values.put(COLUMN_LOG_OPERATOR, logInfo.getOperator());
        values.put(COLUMN_LOG_OPERATION_TIME, logInfo.getOperationTime());
        values.put(COLUMN_LOG_ACTION, logInfo.getAction());
        long result = db.insert(TABLE_LOGS, null, values);
        db.close();
        return result;
    }

    // 动作字符串过滤：仅允许上行/下行
    private boolean isLoggableAction(String action) {
        if (action == null) return false;
        String a = action.trim();
        return a.startsWith("接收上行数据") || a.startsWith("发送下行数据") || a.contains("功能码=");
    }

    /**
     * 添加上行数据日志
     * @param time 时间字符串
     * @param hex 十六进制数据
     * @return 插入的行ID
     */
    public long addUplinkLog(String time, String hex) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // 解析帧
        com.lora.cn.utils.LoRaFrameParser.ParsedFrame frame = com.lora.cn.utils.LoRaFrameParser.parseFrame(hex);
        String deviceId = frame != null ? frame.deviceId : null;
        String terminalName = "上行数据";
        String status = "";
        String operator = "";
        String operationTime = "";
        String action = "接收上行数据";

        // 尝试根据帧中的时间替换操作时间
        try {
            if (frame != null && frame.dataTime != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                operationTime = sdf.format(frame.dataTime);
            }
        } catch (Exception ignored) {}

        // 根据设备ID查询终端表，获取终端名称等
        try {
            java.util.List<com.lora.cn.ui.model.Terminal> terminals = getAllTerminals();
            if (deviceId != null && terminals != null) {
                for (com.lora.cn.ui.model.Terminal t : terminals) {
                    if (deviceId.equalsIgnoreCase(t.getTerminalId())) {
                        terminalName = t.getTerminalName();
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}

        // 根据设备事件/状态位映射日志状态（来源：上行设备事件/状态）
        if (frame != null) {
            // 事件优先级：低电量 > 丢失 > 开锁/上锁 > 打开/关闭 > 取走/放入 > 定期上报 > 护士站查询
            if (frame.evLowBattery == 1) {
                status = "低电量报警";
            } else if (frame.evIllegalRemoval == 1) {
                status = "设备丢失";
            } else if (frame.evPowerLockOpen == 1) {
                status = "开锁";
            } else if (frame.evPowerLockClose == 1) {
                status = "上锁";
            } else if (frame.evManualTake == 1) {
                status = "设备打开";
            } else if (frame.evManualPut == 1) {
                status = "设备关闭";
            } else if (frame.evPeriodicReport == 1) {
                status = "定期上报";
            } else if (frame.evNurseQuery == 1) {
                status = "护士站查询";
            }
        }

        // 构造动作描述
        if (frame != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("功能码=").append(frame.functionCode)
              .append(", 流水号=").append(frame.sequenceNumber)
              .append(", 电量=").append(frame.batteryLevel).append("%")
              .append(", RSSI=").append(frame.rssi)
              .append(", 事件=").append(com.lora.cn.utils.LoRaFrameParser.getDeviceEventDescription(frame.deviceEvent));
            action = sb.toString();
        } else {
            action = "接收上行数据: " + hex;
        }

        values.put(COLUMN_LOG_TERMINAL_ID, deviceId != null ? deviceId : "uplink");
        values.put(COLUMN_LOG_TERMINAL_NAME, terminalName);
        values.put(COLUMN_LOG_DEVICE_ID, deviceId != null ? deviceId : "mqtt");
        values.put(COLUMN_LOG_STATUS, status);
        values.put(COLUMN_LOG_OPERATOR, operator);
        values.put(COLUMN_LOG_OPERATION_TIME, operationTime);
        values.put(COLUMN_LOG_ACTION, action);

        long result = db.insert(TABLE_LOGS, null, values);

        // 同步更新终端设备的电量、信号强度，并记扩展信息
        try {
            if (frame != null && deviceId != null) {
                updateTerminalMetricsByDeviceId(deviceId, frame.batteryLevel, frame.rssi, frame.batteryVoltage);
            }
        } catch (Exception e) {
            android.util.Log.e("DatabaseHelper", "更新终端电量/信号失败", e);
        }

        db.close();
        return result;
    }

    /**
     * 根据设备ID更新终端的电量与信号强度，并在扩展字段写入电压与RSSI
     */
    public boolean updateTerminalMetricsByDeviceId(String deviceId, int batteryLevel, int rssi, int batteryVoltage) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_TERMINAL_BATTERY_LEVEL, batteryLevel);
            values.put(COLUMN_TERMINAL_SIGNAL_STRENGTH, mapRssiToBars(rssi));
            values.put(COLUMN_TERMINAL_BATTERY_VOLTAGE, batteryVoltage);
            values.put(COLUMN_TERMINAL_RSSI, rssi);
            int rows = db.update(TABLE_TERMINALS, values, COLUMN_TERMINAL_DEVICE_ID + "=?", new String[]{deviceId});
            return rows > 0;
        } finally {
            db.close();
        }
    }

    /**
     * 将RSSI(0~138，代表-138~0dBm)映射为0~4根信号条
     */
    private int mapRssiToBars(int rssiRaw) {
        int dbm = -Math.max(0, Math.min(138, rssiRaw));
        if (dbm >= -70) return 4;       // 强
        if (dbm >= -85) return 3;       // 较强
        if (dbm >= -100) return 2;      // 一般
        if (dbm >= -120) return 1;      // 较弱
        return 0;                        // 很弱/无信号
    }

    public java.util.List<com.lora.cn.ui.model.LogInfo> getAllLogs() {
        java.util.List<com.lora.cn.ui.model.LogInfo> logs = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String query = "SELECT * FROM " + TABLE_LOGS + " ORDER BY " + COLUMN_LOG_CREATE_TIME + " DESC";
        android.database.Cursor cursor = db.rawQuery(query, null);
        
        while (cursor.moveToNext()) {
            com.lora.cn.ui.model.LogInfo log = new com.lora.cn.ui.model.LogInfo();
            log.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_LOG_ID)));
            log.setTerminalId(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_TERMINAL_ID)));
            log.setTerminalName(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_TERMINAL_NAME)));
            log.setDeviceId(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_DEVICE_ID)));
            log.setStatus(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_STATUS)));
            log.setOperator(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_OPERATOR)));
            log.setOperationTime(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_OPERATION_TIME)));
            log.setAction(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_ACTION)));
            log.setCreateTime(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_CREATE_TIME)));
            
            logs.add(log);
        }
        cursor.close();
        db.close();
        return logs;
    }

    public java.util.List<com.lora.cn.ui.model.LogInfo> getLogsByTerminalId(String terminalId) {
        java.util.List<com.lora.cn.ui.model.LogInfo> logs = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String query = "SELECT * FROM " + TABLE_LOGS + " WHERE " + COLUMN_LOG_TERMINAL_ID + " = ? ORDER BY " + COLUMN_LOG_CREATE_TIME + " DESC";
        android.database.Cursor cursor = db.rawQuery(query, new String[]{terminalId});
        
        while (cursor.moveToNext()) {
            com.lora.cn.ui.model.LogInfo log = new com.lora.cn.ui.model.LogInfo();
            log.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_LOG_ID)));
            log.setTerminalId(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_TERMINAL_ID)));
            log.setTerminalName(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_TERMINAL_NAME)));
            log.setDeviceId(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_DEVICE_ID)));
            log.setStatus(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_STATUS)));
            log.setOperator(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_OPERATOR)));
            log.setOperationTime(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_OPERATION_TIME)));
            log.setAction(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_ACTION)));
            log.setCreateTime(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_CREATE_TIME)));
            
            logs.add(log);
        }
        cursor.close();
        db.close();
        return logs;
    }
    
    // 初始化示例日志数据
    public void initSampleLogData() {
        SQLiteDatabase db = this.getReadableDatabase();
        android.database.Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_LOGS, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        db.close();
        
        // 如果日志表为空，则添加示例数据
        if (count == 0) {
            addLog("1", "终端设备001", "DEV001", "在线", "张三", "2024-01-15 10:35:00", "数据上传");
            addLog("2", "终端设备002", "DEV002", "低电量", "李四", "", "电量检测");
            addLog("3", "终端设备003", "DEV003", "设备丢失", "王五", "", "设备检查");
            addLog("4", "终端设备004", "DEV004", "在线", "赵六", "2024-01-15 07:20:00", "状态更新");
            addLog("5", "终端设备005", "DEV005", "异常丢失", "孙七", "", "异常处理");
            addLog("1", "终端设备001", "DEV001", "离线", "张三", "2024-01-15 11:00:00", "设备维护");
            addLog("6", "终端设备006", "DEV006", "在线", "周八", "2024-01-15 08:15:30", "定期检查");
            addLog("7", "终端设备007", "DEV007", "低电量", "吴九", "", "电池更换");
            addLog("2", "终端设备002", "DEV002", "在线", "李四", "2024-01-15 12:30:45", "电量恢复");
            addLog("8", "终端设备008", "DEV008", "设备丢失", "郑十", "", "紧急查找");
        }
    }

    // 清理示例日志数据：删除 device_id 为 DEV00X 的记录或示例人员
    public int cleanSampleLogData() {
        SQLiteDatabase db = this.getWritableDatabase();
        // 通过设备ID模式和示例操作者名称进行清理
        String whereClause = COLUMN_LOG_DEVICE_ID + " LIKE 'DEV00%' OR " + COLUMN_LOG_OPERATOR + " IN (?,?,?,?,?,?,?,?)";
        String[] whereArgs = new String[]{"张三","李四","王五","赵六","孙七","周八","吴九","郑十"};
        int deleted = db.delete(TABLE_LOGS, whereClause, whereArgs);
        db.close();
        return deleted;
    }

    /**
     * 更新终端收藏状态
     */
    public int updateTerminalFavoriteStatus(String terminalId, boolean isFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_TERMINAL_IS_FAVORITE, isFavorite ? 1 : 0);
        values.put(COLUMN_TERMINAL_UPDATE_TIME, System.currentTimeMillis());
        
        return db.update(TABLE_TERMINALS, values, 
                COLUMN_TERMINAL_DEVICE_ID + "=?", 
                new String[]{terminalId});
    }
    
    /**
     * 更新终端名称
     */
    public boolean updateTerminalName(String terminalId, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_TERMINAL_NAME, newName);
        values.put(COLUMN_TERMINAL_UPDATE_TIME, System.currentTimeMillis());
        
        int result = db.update(TABLE_TERMINALS, values, 
                COLUMN_TERMINAL_DEVICE_ID + "=?", 
                new String[]{terminalId});
        
        return result > 0;
    }
    
    /**
     * 更新终端所属科室
     */
    public boolean updateTerminalDepartment(String terminalId, String newDepartment) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_TERMINAL_DEPARTMENT, newDepartment);
        values.put(COLUMN_TERMINAL_UPDATE_TIME, System.currentTimeMillis());
        
        int result = db.update(TABLE_TERMINALS, values, 
                COLUMN_TERMINAL_DEVICE_ID + "=?", 
                new String[]{terminalId});
        
        return result > 0;
    }
    
    /**
     * 更新终端位置信息
     */
    public boolean updateTerminalLocation(String terminalId, String newLocation) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_TERMINAL_LOCATION, newLocation);
        values.put(COLUMN_TERMINAL_UPDATE_TIME, System.currentTimeMillis());
        
        int result = db.update(TABLE_TERMINALS, values, 
                COLUMN_TERMINAL_DEVICE_ID + "=?", 
                new String[]{terminalId});
        
        return result > 0;
    }

    /**
     * 删除终端（按设备ID）
     */
    public int deleteTerminalByDeviceId(String deviceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_TERMINALS, COLUMN_TERMINAL_DEVICE_ID + "=?", new String[]{deviceId});
        return result;
    }
}