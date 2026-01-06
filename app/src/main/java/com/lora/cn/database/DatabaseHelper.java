package com.lora.cn.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.blankj.utilcode.util.LogUtils;

/**
 * 数据库帮助类
 * 管理分组表和分类表的两层关系结构
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "lora_app.db";
    private static final int DATABASE_VERSION = 23;
    
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
    // 未绑定终端的日志表
    public static final String TABLE_LOGS_UNBOUND = "logs_unbound";
    public static final String COLUMN_LOG_ID = "log_id";
    public static final String COLUMN_LOG_TERMINAL_ID = "terminal_id";
    public static final String COLUMN_LOG_TERMINAL_NAME = "terminal_name";
    public static final String COLUMN_LOG_DEVICE_ID = "device_id";
    public static final String COLUMN_LOG_STATUS = "status";
    public static final String COLUMN_LOG_OPERATOR = "operator";
    public static final String COLUMN_LOG_OPERATION_TIME = "operation_time";
    public static final String COLUMN_LOG_ACTION = "action";
    public static final String COLUMN_LOG_CREATE_TIME = "create_time";

    public static final String TABLE_MAINTENANCE = "maintenance_records";
    public static final String COLUMN_MAINTENANCE_ID = "maintenance_id";
    public static final String COLUMN_MAINTENANCE_TERMINAL_ID = "terminal_id";
    public static final String COLUMN_MAINTENANCE_DEVICE_ID = "device_id";
    public static final String COLUMN_MAINTENANCE_TERMINAL_NAME = "terminal_name";
    public static final String COLUMN_MAINTENANCE_TERMINAL_GROUP = "terminal_group";
    public static final String COLUMN_MAINTENANCE_STATUS = "status";
    public static final String COLUMN_MAINTENANCE_CONTENT = "content";
    public static final String COLUMN_MAINTENANCE_CREATE_USER_ID = "create_user_id";
    public static final String COLUMN_MAINTENANCE_CREATE_USER = "create_user";
    public static final String COLUMN_MAINTENANCE_CREATE_TIME = "create_time";
    public static final String COLUMN_MAINTENANCE_HANDLE_USER_ID = "handle_user_id";
    public static final String COLUMN_MAINTENANCE_HANDLE_USER = "handle_user";
    public static final String COLUMN_MAINTENANCE_HANDLE_TIME = "handle_time";
    public static final String COLUMN_MAINTENANCE_SENT_FLAG = "sent_flag";
    public static final String COLUMN_MAINTENANCE_SENT_TIME = "sent_time";
    
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
    public static final String COLUMN_TERMINAL_GROUP_IDS = "group_ids"; // 动态分组ID对列表: gid:cid,逗号分隔
    public static final String COLUMN_TERMINAL_GROUP_NAMES = "group_names"; // 动态分组名称对列表: gname-cname,逗号分隔
    public static final String COLUMN_TERMINAL_IS_FAVORITE = "is_favorite"; // 收藏状态
    public static final String COLUMN_TERMINAL_FAVORITE_USER_ID = "favorite_user_id"; // 收藏用户ID
    public static final String COLUMN_TERMINAL_CREATE_TIME = "create_time";
    public static final String COLUMN_TERMINAL_UPDATE_TIME = "update_time";
    public static final String COLUMN_TERMINAL_MAINTENANCE_ACTIVE = "maintenance_active";
    public static final String COLUMN_TERMINAL_MAINTENANCE_TIME = "maintenance_time";
    
    // 用户关注映射表
    public static final String TABLE_USER_FAVORITES = "user_favorites";
    public static final String COLUMN_UF_ID = "id";
    public static final String COLUMN_UF_USER_ID = "user_id";
    public static final String COLUMN_UF_DEVICE_ID = "terminal_device_id";
    public static final String COLUMN_UF_CREATE_TIME = "create_time";
    private static final String CREATE_TABLE_USER_FAVORITES =
        "CREATE TABLE " + TABLE_USER_FAVORITES + " (" +
        COLUMN_UF_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_UF_USER_ID + " INTEGER NOT NULL, " +
        COLUMN_UF_DEVICE_ID + " TEXT NOT NULL, " +
        COLUMN_UF_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        "UNIQUE(" + COLUMN_UF_USER_ID + "," + COLUMN_UF_DEVICE_ID + ")" +
        ")";

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
        COLUMN_TERMINAL_STATUS + " TEXT DEFAULT '正常在线', " +
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
        COLUMN_TERMINAL_GROUP_IDS + " TEXT, " +
        COLUMN_TERMINAL_GROUP_NAMES + " TEXT, " +
        COLUMN_TERMINAL_IS_FAVORITE + " INTEGER DEFAULT 0, " +
        COLUMN_TERMINAL_FAVORITE_USER_ID + " INTEGER DEFAULT 0, " +
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
        "CREATE TABLE IF NOT EXISTS " + TABLE_LOGS + " (" +
        COLUMN_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_LOG_TERMINAL_ID + " TEXT NOT NULL, " +
        COLUMN_LOG_TERMINAL_NAME + " TEXT NOT NULL, " +
        COLUMN_LOG_DEVICE_ID + " TEXT NOT NULL, " +
        COLUMN_LOG_STATUS + " INTEGER NOT NULL, " +
        COLUMN_LOG_OPERATOR + " TEXT, " +
        COLUMN_LOG_OPERATION_TIME + " TEXT, " +
        COLUMN_LOG_ACTION + " TEXT NOT NULL, " +
        COLUMN_LOG_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        "handle_user TEXT, " +
        "handle_time TEXT, " +
        "handle_remark TEXT" +
        ")";

    // 创建未绑定终端的日志表（字段与日志表一致）
    private static final String CREATE_TABLE_LOGS_UNBOUND =
        "CREATE TABLE IF NOT EXISTS " + TABLE_LOGS_UNBOUND + " (" +
        COLUMN_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_LOG_TERMINAL_ID + " TEXT NOT NULL, " +
        COLUMN_LOG_TERMINAL_NAME + " TEXT NOT NULL, " +
        COLUMN_LOG_DEVICE_ID + " TEXT NOT NULL, " +
        COLUMN_LOG_STATUS + " INTEGER NOT NULL, " +
        COLUMN_LOG_OPERATOR + " TEXT, " +
        COLUMN_LOG_OPERATION_TIME + " TEXT, " +
        COLUMN_LOG_ACTION + " TEXT NOT NULL, " +
        COLUMN_LOG_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        "handle_user TEXT, " +
        "handle_time TEXT, " +
        "handle_remark TEXT" +
        ")";

    private static final String CREATE_TABLE_MAINTENANCE =
        "CREATE TABLE IF NOT EXISTS " + TABLE_MAINTENANCE + " (" +
        COLUMN_MAINTENANCE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_MAINTENANCE_TERMINAL_ID + " TEXT NOT NULL, " +
        COLUMN_MAINTENANCE_TERMINAL_NAME + " TEXT, " +
        COLUMN_MAINTENANCE_TERMINAL_GROUP + " TEXT, " +
        COLUMN_MAINTENANCE_STATUS + " INTEGER DEFAULT 0, " +
        COLUMN_MAINTENANCE_CONTENT + " TEXT, " +
        COLUMN_MAINTENANCE_CREATE_USER_ID + " INTEGER DEFAULT 0, " +
        COLUMN_MAINTENANCE_CREATE_USER + " TEXT, " +
        COLUMN_MAINTENANCE_CREATE_TIME + " TEXT, " +
        COLUMN_MAINTENANCE_HANDLE_USER_ID + " INTEGER DEFAULT 0, " +
        COLUMN_MAINTENANCE_HANDLE_USER + " TEXT, " +
        COLUMN_MAINTENANCE_HANDLE_TIME + " TEXT, " +
        COLUMN_MAINTENANCE_SENT_FLAG + " INTEGER DEFAULT 0, " +
        COLUMN_MAINTENANCE_SENT_TIME + " TEXT, " +
        "handle_remark TEXT" +
        ")";
    
    // 创建索引的SQL语句
    private static final String CREATE_INDEX_CATEGORIES_GROUP_ID = 
        "CREATE INDEX idx_categories_group_id ON " + TABLE_CATEGORIES + 
        "(" + COLUMN_CATEGORY_GROUP_ID + ")";
    
    private static DatabaseHelper instance;
    private final Object maintenanceSchemaLock = new Object();
    private volatile boolean maintenanceSchemaEnsured = false;
    private volatile Boolean maintenanceHasDeviceIdColumn = null;
    
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
        try {
            db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_MAINTENANCE_ACTIVE + " INTEGER DEFAULT 0");
        } catch (Exception ignored) {}
        try {
            db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_MAINTENANCE_TIME + " INTEGER DEFAULT 0");
        } catch (Exception ignored) {}
        
        // 创建日志表
        db.execSQL(CREATE_TABLE_LOGS);
        // 创建未绑定日志表
        db.execSQL(CREATE_TABLE_LOGS_UNBOUND);
        db.execSQL(CREATE_TABLE_MAINTENANCE);
        // 创建用户收藏关系表
        db.execSQL(CREATE_TABLE_USER_FAVORITES);
        
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
        if (oldVersion < 21) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_MAINTENANCE_ACTIVE + " INTEGER DEFAULT 0");
                db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_MAINTENANCE_TIME + " INTEGER DEFAULT 0");
            } catch (Exception ignored) {}
        }
        // 版本14 -> 15：为终端添加电池电压与RSSI原始值列
        if (oldVersion < 15) {
            try { db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_BATTERY_VOLTAGE + " INTEGER DEFAULT 0"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_RSSI + " INTEGER DEFAULT 0"); } catch (Exception ignored) {}
        }
        if (oldVersion < 16) {
            try { db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_FAVORITE_USER_ID + " INTEGER DEFAULT 0"); } catch (Exception ignored) {}
        }
        if (oldVersion < 17) {
            try { db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS); } catch (Exception ignored) {}
            db.execSQL(CREATE_TABLE_LOGS);
        }
        if (oldVersion < 18) {
            try { db.execSQL(CREATE_TABLE_LOGS_UNBOUND); } catch (Exception ignored) {}
        }
        if (oldVersion < 19) {
            try { db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_GROUP_IDS + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_TERMINALS + " ADD COLUMN " + COLUMN_TERMINAL_GROUP_NAMES + " TEXT"); } catch (Exception ignored) {}
        }
        if (oldVersion < 20) {
            try { db.execSQL(CREATE_TABLE_USER_FAVORITES); } catch (Exception ignored) {}
        }
        if (oldVersion < 22) {
            try { db.execSQL(CREATE_TABLE_MAINTENANCE); } catch (Exception ignored) {}
        }
        if (oldVersion < 23) {
            try { ensureMaintenanceSchema(db); } catch (Exception ignored) {}
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

    public boolean isFavoriteForUser(long userId, String deviceId) {
        if (userId <= 0 || deviceId == null) return false;
        SQLiteDatabase db = this.getReadableDatabase();
        android.database.Cursor c = db.rawQuery("SELECT 1 FROM " + TABLE_USER_FAVORITES + " WHERE " + COLUMN_UF_USER_ID + "=? AND " + COLUMN_UF_DEVICE_ID + "=? LIMIT 1",
                new String[]{String.valueOf(userId), deviceId});
        boolean res = c.moveToFirst();
        c.close();
        return res;
    }

    public void setFavoriteForUser(long userId, String deviceId, boolean favorite) {
        if (userId <= 0 || deviceId == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        if (favorite) {
            ContentValues v = new ContentValues();
            v.put(COLUMN_UF_USER_ID, userId);
            v.put(COLUMN_UF_DEVICE_ID, deviceId);
            v.put(COLUMN_UF_CREATE_TIME, System.currentTimeMillis());
            db.insertWithOnConflict(TABLE_USER_FAVORITES, null, v, SQLiteDatabase.CONFLICT_IGNORE);
        } else {
            db.delete(TABLE_USER_FAVORITES, COLUMN_UF_USER_ID + "=? AND " + COLUMN_UF_DEVICE_ID + "=?",
                    new String[]{String.valueOf(userId), deviceId});
        }
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

        // 插入默认科室数据（用于科室管理和用户管理的科室下拉）
        db.execSQL("INSERT INTO " + TABLE_DEPARTMENTS + " (" + COLUMN_DEPARTMENT_NAME + ", " + COLUMN_DEPARTMENT_SORT_ORDER + ", " + COLUMN_DEPARTMENT_STATUS + ") VALUES ('科室1', 1, 1)");
        db.execSQL("INSERT INTO " + TABLE_DEPARTMENTS + " (" + COLUMN_DEPARTMENT_NAME + ", " + COLUMN_DEPARTMENT_SORT_ORDER + ", " + COLUMN_DEPARTMENT_STATUS + ") VALUES ('科室2', 2, 1)");
        db.execSQL("INSERT INTO " + TABLE_DEPARTMENTS + " (" + COLUMN_DEPARTMENT_NAME + ", " + COLUMN_DEPARTMENT_SORT_ORDER + ", " + COLUMN_DEPARTMENT_STATUS + ") VALUES ('科室3', 3, 1)");
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
        android.database.Cursor cursor = db.rawQuery("SELECT " + COLUMN_USER_ID + ", " + COLUMN_USER_ROLE_ID + " FROM " + TABLE_USERS + " WHERE " + COLUMN_USER_ACCOUNT + " = 'admin'", null);
        
        if (cursor.moveToFirst()) {
            // admin用户已存在，确保其角色为“管理员”
            long userId = cursor.getLong(0);
            long currentRoleId = cursor.getLong(1);
            cursor.close();

            // 获取管理员角色ID（若不存在则创建）
            android.database.Cursor rc = db.rawQuery("SELECT " + COLUMN_ROLE_ID + " FROM " + TABLE_ROLES + " WHERE " + COLUMN_ROLE_NAME + " = '管理员'", null);
            long adminRoleId = -1;
            if (rc.moveToFirst()) {
                adminRoleId = rc.getLong(0);
            }
            rc.close();
            if (adminRoleId == -1) {
                ContentValues roleValues = new ContentValues();
                roleValues.put(COLUMN_ROLE_NAME, "管理员");
                roleValues.put(COLUMN_ROLE_DESCRIPTION, "系统管理员，拥有所有权限");
                roleValues.put(COLUMN_ROLE_SORT_ORDER, 1);
                roleValues.put(COLUMN_ROLE_STATUS, 1);
                adminRoleId = db.insert(TABLE_ROLES, null, roleValues);
                // 分配所有权限
                db.execSQL("INSERT INTO " + TABLE_ROLE_PERMISSIONS + " (" + COLUMN_ROLE_PERMISSION_ROLE_ID + ", " + COLUMN_ROLE_PERMISSION_PERMISSION_ID + ") SELECT " + adminRoleId + ", " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS);
            }

            if (adminRoleId > 0 && currentRoleId != adminRoleId) {
                ContentValues up = new ContentValues();
                up.put(COLUMN_USER_ROLE_ID, adminRoleId);
                db.update(TABLE_USERS, up, COLUMN_USER_ID + "=?", new String[]{String.valueOf(userId)});
                android.util.Log.d("DatabaseHelper", "已为默认管理员用户分配管理员角色: userId=" + userId + ", roleId=" + adminRoleId);
            }
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

    public void debugLogAdminRoleAndUser() {
        try {
            SQLiteDatabase db = getReadableDatabase();
            long rid = -1;
            android.database.Cursor rc = db.rawQuery("SELECT " + COLUMN_ROLE_ID + " FROM " + TABLE_ROLES + " WHERE " + COLUMN_ROLE_NAME + "='管理员'", null);
            if (rc.moveToFirst()) rid = rc.getLong(0);
            rc.close();
            android.util.Log.d("DatabaseHelper", "管理员角色查询结果 roleId=" + rid);

            android.database.Cursor uc = db.rawQuery("SELECT " + COLUMN_USER_ID + ", " + COLUMN_USER_ACCOUNT + ", " + COLUMN_USER_ROLE_ID + " FROM " + TABLE_USERS + " WHERE " + COLUMN_USER_ACCOUNT + "='admin'", null);
            if (uc.moveToFirst()) {
                long uid = uc.getLong(0);
                String acc = uc.getString(1);
                long urid = uc.getLong(2);
                android.util.Log.d("DatabaseHelper", "默认用户查询结果 userId=" + uid + ", account=" + acc + ", roleId=" + urid);
            } else {
                android.util.Log.d("DatabaseHelper", "默认用户查询结果: 未找到admin账号");
            }
            uc.close();
        } catch (Exception e) {
            android.util.Log.e("DatabaseHelper", "debugLogAdminRoleAndUser异常: " + e.getMessage());
        }
    }
    
    public void ensureDefaultAdminRoleAssigned() {
        try {
            SQLiteDatabase db = getWritableDatabase();
            long adminRid = -1;
            android.database.Cursor rc = db.rawQuery("SELECT " + COLUMN_ROLE_ID + " FROM " + TABLE_ROLES + " WHERE " + COLUMN_ROLE_NAME + "='管理员'", null);
            if (rc.moveToFirst()) adminRid = rc.getLong(0);
            rc.close();
            if (adminRid <= 0) {
                ContentValues roleValues = new ContentValues();
                roleValues.put(COLUMN_ROLE_NAME, "管理员");
                roleValues.put(COLUMN_ROLE_DESCRIPTION, "系统管理员，拥有所有权限");
                roleValues.put(COLUMN_ROLE_SORT_ORDER, 1);
                roleValues.put(COLUMN_ROLE_STATUS, 1);
                adminRid = db.insert(TABLE_ROLES, null, roleValues);
                db.execSQL("INSERT INTO " + TABLE_ROLE_PERMISSIONS + " (" + COLUMN_ROLE_PERMISSION_ROLE_ID + ", " + COLUMN_ROLE_PERMISSION_PERMISSION_ID + ") SELECT " + adminRid + ", " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS);
            }
            db.execSQL("INSERT OR IGNORE INTO " + TABLE_ROLE_PERMISSIONS + " (" + COLUMN_ROLE_PERMISSION_ROLE_ID + ", " + COLUMN_ROLE_PERMISSION_PERMISSION_ID + ") " +
                    "SELECT " + adminRid + ", " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_ID + " NOT IN (" +
                    "SELECT " + COLUMN_ROLE_PERMISSION_PERMISSION_ID + " FROM " + TABLE_ROLE_PERMISSIONS + " WHERE " + COLUMN_ROLE_PERMISSION_ROLE_ID + "=" + adminRid + ")");
            android.database.Cursor c = db.rawQuery("SELECT " + COLUMN_USER_ID + ", " + COLUMN_USER_ROLE_ID + " FROM " + TABLE_USERS + " WHERE " + COLUMN_USER_ACCOUNT + "='admin' OR " + COLUMN_USER_NAME + "='管理员'", null);
            while (c.moveToNext()) {
                long uid = c.getLong(0);
                long currentRid = c.getLong(1);
                if (adminRid > 0 && currentRid != adminRid) {
                    ContentValues up = new ContentValues();
                    up.put(COLUMN_USER_ROLE_ID, adminRid);
                    db.update(TABLE_USERS, up, COLUMN_USER_ID + "=?", new String[]{String.valueOf(uid)});
                }
            }
            c.close();
        } catch (Exception ignored) {}
    }
    
    /**
     * 插入初始权限数据
     */
    private void insertInitialPermissions(SQLiteDatabase db) {
        // 终端管理权限
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('terminal_list', '终端列表', 'terminal', '查看终端列表')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('terminal_add', '添加终端', 'terminal', '添加新终端设备')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('terminal_detail', '终端详情', 'terminal', '查看终端详细信息')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('terminal_edit', '编辑终端', 'terminal', '编辑终端信息')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('terminal_delete', '删除终端', 'terminal', '删除终端设备')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('terminal_mark', '标记终端', 'terminal', '标记终端状态')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('terminal_confirm', '确认处理', 'terminal', '确认终端处理结果')");
        
        // 日志管理权限
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('log_info', '日志信息', 'log', '查看日志信息')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('log_export', '导出日志', 'log', '导出日志文件')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('log_confirm', '确认处理', 'log', '确认日志处理结果')");
        
        // 清理终端权限
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('clean_terminal', '清理终端', 'clean', '清理终端数据')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('clean_export', '导出清理', 'clean', '导出清理数据')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('clean_start_count', '开始清点', 'clean', '开始清点操作')");
        
        // 设置权限
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('setting_device', '设备设置', 'setting', '设备相关设置')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('setting_sound', '声音设置', 'setting', '声音相关设置')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('setting_wifi', 'WiFi连接', 'setting', 'WiFi连接设置')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('setting_ip', 'IP配置', 'setting', 'IP地址配置')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('setting_count', '清点次数', 'setting', '清点次数设置')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('setting_sleep_interval', '设备休眠间隔', 'setting', '设备休眠间隔设置')");
        
        // 角色管理权限（归属设置）
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('role_add', '新增角色', 'setting', '新增角色')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('role_edit', '编辑角色', 'setting', '编辑角色信息')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('role_delete', '删除角色', 'setting', '删除角色')");
        
        // 用户管理权限（归属设置）
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('user_add', '新增用户', 'setting', '新增用户账户')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('user_edit', '编辑用户', 'setting', '编辑用户信息')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('user_delete', '删除用户', 'setting', '删除用户账户')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('user_reset_password', '重置密码', 'setting', '重置用户密码')");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" + COLUMN_PERMISSION_CODE + ", " + 
                  COLUMN_PERMISSION_NAME + ", " + COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ") VALUES ('user_disable', '启用/禁用用户', 'setting', '启用或禁用用户账户')");
    }
    
    /**
     * 插入树形权限数据
     */
    private void insertTreePermissions(SQLiteDatabase db) {
        // 插入完整的树形权限数据，使用正确的SQL语法
        
        // Level 0 - 顶级权限
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('terminal_list', '终端列表', 'terminal', '终端管理模块', 1, NULL, 0, 1)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('log_info', '日志信息', 'log', '日志管理模块', 1, NULL, 0, 2)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('clean_terminal', '清理终端', 'clean', '清理终端模块', 1, NULL, 0, 3)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('setting', '设置', 'setting', '系统设置模块', 1, NULL, 0, 4)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('role_management', '角色管理', 'setting', '角色管理模块', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='setting' LIMIT 1), 1, 5)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('user_management', '用户管理', 'setting', '用户管理模块', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='setting' LIMIT 1), 1, 6)");

        // Level 1 - 终端列表的子权限
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('terminal_add', '添加终端', 'terminal', '添加新终端设备', 1, 1, 1, 1)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('terminal_detail', '终端详情', 'terminal', '查看终端详细信息', 1, 1, 1, 2)");

        // Level 2 - 终端详情的子权限
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('terminal_edit', '编辑', 'terminal', '编辑终端信息', 1, 8, 2, 1)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('terminal_delete', '删除', 'terminal', '删除终端设备', 1, 8, 2, 2)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('terminal_mark', '收藏', 'terminal', '收藏终端状态', 1, 8, 2, 3)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('terminal_confirm', '确认处理', 'terminal', '确认终端处理结果', 1, 8, 2, 4)");

        // Level 1 - 日志信息的子权限
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('log_export', '导出', 'log', '导出日志文件', 1, 2, 1, 1)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('log_confirm', '确认处理', 'log', '确认日志处理结果', 1, 2, 1, 2)");

        // Level 1 - 清理终端的子权限
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('clean_export', '导出', 'clean', '导出清理数据', 1, 3, 1, 1)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('clean_start_count', '开始清点', 'clean', '开始清点操作', 1, 3, 1, 2)");

        // Level 1 - 设置的子权限
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('setting_device', '设备设置', '设置相关', '设备相关设置', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='setting' LIMIT 1), 1, 1)");



        // Level 2 - 设备设置的子权限
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('setting_sound', '声音设置', '设置相关', '声音相关设置', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='setting_device' LIMIT 1), 2, 1)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('setting_wifi', 'WiFi连接', '设置相关', 'WiFi连接设置', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='setting_device' LIMIT 1), 2, 2)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('setting_ip', 'IP配置', '设置相关', 'IP地址配置', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='setting_device' LIMIT 1), 2, 3)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('setting_count', '清点次数', '设置相关', '清点次数设置', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='setting_device' LIMIT 1), 2, 4)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('setting_low_battery', '低电量报警值', '设置相关', '低电量阈值设置', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='setting_device' LIMIT 1), 2, 5)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('setting_home_return', '返回首页时间', '设置相关', '自动返回首页时间设置', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='setting_device' LIMIT 1), 2, 6)");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('setting_inventory', '定时清点', '设置相关', '定时清点设置', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='setting_device' LIMIT 1), 2, 7)");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('setting_sleep_interval', '设备休眠间隔', '设置相关', '设备休眠间隔设置', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='setting_device' LIMIT 1), 2, 8)");

        // Level 1 - 角色管理的子权限
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('role_add', '新增', 'role', '新增角色', 1, 5, 1, 1)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('role_edit', '编辑', 'role', '编辑角色信息', 1, 5, 1, 2)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('role_delete', '删除', 'role', '删除角色', 1, 5, 1, 3)");

        // Level 1 - 用户管理的子权限（挂载到 user_management）
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('user_add', '新增', 'setting', '新增用户', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='user_management' LIMIT 1), 1, 1)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('user_edit', '编辑', 'setting', '编辑用户信息', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='user_management' LIMIT 1), 1, 2)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('user_delete', '删除', 'setting', '删除用户', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='user_management' LIMIT 1), 1, 3)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('user_reset_password', '重置密码', 'setting', '重置用户密码', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='user_management' LIMIT 1), 1, 4)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('user_disable', '启用/禁用', 'setting', '启用或禁用用户账户', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='user_management' LIMIT 1), 1, 5)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('user_delete', '删除', 'user', '删除用户', 1, 6, 1, 3)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('user_reset_password', '重置密码', 'user', '重置用户密码', 1, 6, 1, 4)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('department_management', '科室管理', 'setting', '科室管理模块', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='setting' LIMIT 1), 1, 7)");

        // Level 1 - 分组管理（挂载到 setting）
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('group_management', '分组管理', 'setting', '分组管理模块', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='setting' LIMIT 1), 1, 9)");
        // Level 2 - 分组管理子权限
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('group_add', '新增', 'setting', '新增分组', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='group_management' LIMIT 1), 2, 1)");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('group_edit', '编辑', 'setting', '编辑分组', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='group_management' LIMIT 1), 2, 2)");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('group_delete', '删除', 'setting', '删除分组', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='group_management' LIMIT 1), 2, 3)");

        // Level 2 - 分类管理（挂到分组管理下）
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('category_management', '分类管理', 'setting', '分组下的分类管理', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='group_management' LIMIT 1), 2, 4)");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('category_add', '新增', 'setting', '新增分类', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='category_management' LIMIT 1), 3, 1)");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('category_edit', '编辑', 'setting', '编辑分类', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='category_management' LIMIT 1), 3, 2)");
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('category_delete', '删除', 'setting', '删除分类', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='category_management' LIMIT 1), 3, 3)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('department_add', '新增', 'setting', '新增科室', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='department_management' LIMIT 1), 2, 1)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('department_edit', '编辑', 'setting', '编辑科室', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='department_management' LIMIT 1), 2, 2)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('department_delete', '删除', 'setting', '删除科室', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='department_management' LIMIT 1), 2, 3)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('position_management', '职位管理', 'setting', '职位管理模块', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='setting' LIMIT 1), 1, 8)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('position_add', '新增', 'setting', '新增职位', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='position_management' LIMIT 1), 2, 1)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('position_edit', '编辑', 'setting', '编辑职位', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='position_management' LIMIT 1), 2, 2)");

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('position_delete', '删除', 'setting', '删除职位', 1, (SELECT " + COLUMN_PERMISSION_ID + " FROM " + TABLE_PERMISSIONS + " WHERE " + COLUMN_PERMISSION_CODE + "='position_management' LIMIT 1), 2, 3)");
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
        values.put(COLUMN_TERMINAL_STATUS, com.lora.cn.ui.constants.TerminalStatusConstants.codeToText(terminal.getStatus()));
        values.put(COLUMN_TERMINAL_SIGNAL_STRENGTH, terminal.getSignalStrength());
        values.put(COLUMN_TERMINAL_BATTERY_LEVEL, terminal.getBatteryLevel());
        values.put(COLUMN_TERMINAL_BATTERY_VOLTAGE, terminal.getBatteryVoltage());
        values.put(COLUMN_TERMINAL_RSSI, terminal.getRssi());
        values.put(COLUMN_TERMINAL_DEPARTMENT, terminal.getDepartment());
        values.put(COLUMN_TERMINAL_LOCATION, terminal.getLocation());
        if (terminal.getDepartmentId() > 0) {
            values.put(COLUMN_TERMINAL_DEPARTMENT_ID, terminal.getDepartmentId());
        }
        if (terminal.getRoomId() > 0) {
            values.put(COLUMN_TERMINAL_ROOM_ID, terminal.getRoomId());
        }
        if (terminal.getNursingGroupId() > 0) {
            values.put(COLUMN_TERMINAL_NURSING_GROUP_ID, terminal.getNursingGroupId());
        }
        if (terminal.getOtherId() > 0) {
            values.put(COLUMN_TERMINAL_OTHER_ID, terminal.getOtherId());
        }
        values.put(COLUMN_TERMINAL_EXTENSION, terminal.getExtension());
        if (terminal.getGroupIdsText() != null) values.put(COLUMN_TERMINAL_GROUP_IDS, terminal.getGroupIdsText());
        if (terminal.getGroupNamesText() != null) values.put(COLUMN_TERMINAL_GROUP_NAMES, terminal.getGroupNamesText());
        values.put(COLUMN_TERMINAL_IS_FAVORITE, terminal.isFavorite() ? 1 : 0);
        values.put(COLUMN_TERMINAL_CREATE_TIME, System.currentTimeMillis());
        values.put(COLUMN_TERMINAL_UPDATE_TIME, System.currentTimeMillis());
        
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
                {
                    String st = cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_STATUS));
                    terminal.setStatus(com.lora.cn.ui.constants.TerminalStatusConstants.textToCode(st));
                }
                terminal.setSignalStrength(cursor.getInt(cursor.getColumnIndex(COLUMN_TERMINAL_SIGNAL_STRENGTH)));
                
                // 设置电量
                int batteryLevelIndex = cursor.getColumnIndex(COLUMN_TERMINAL_BATTERY_LEVEL);
                if (batteryLevelIndex != -1) {
                    terminal.setBatteryLevel(cursor.getInt(batteryLevelIndex));
                } else {
                    // 如果没有电量字段，使用信号强度作为默认值
                    terminal.setBatteryLevel(cursor.getInt(cursor.getColumnIndex(COLUMN_TERMINAL_SIGNAL_STRENGTH)));
                }
                int lowTh = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
                terminal.setBatteryStatus(terminal.getBatteryLevel() <= lowTh ? 0 : 1);
                
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
                
                // 设置扩展与分组字段
                terminal.setExtension(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_EXTENSION)));
                int giIdx = cursor.getColumnIndex(COLUMN_TERMINAL_GROUP_IDS);
                if (giIdx != -1) terminal.setGroupIdsText(cursor.getString(giIdx));
                int gnIdx = cursor.getColumnIndex(COLUMN_TERMINAL_GROUP_NAMES);
                if (gnIdx != -1) terminal.setGroupNamesText(cursor.getString(gnIdx));
                
                // 设置收藏状态
                long currentUserId = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
                try { terminal.setFavorite(isFavoriteForUser(currentUserId, terminal.getTerminalId())); } catch (Exception ignored) {}
                
                // 设置时间戳
                terminal.setCreateTime(cursor.getLong(cursor.getColumnIndex(COLUMN_TERMINAL_CREATE_TIME)));
                terminal.setUpdateTime(cursor.getLong(cursor.getColumnIndex(COLUMN_TERMINAL_UPDATE_TIME)));
                long nowMs = System.currentTimeMillis();
                long timeoutMs = 3 * 60 * 1000L;
                if (terminal.getUpdateTime() > 0 && nowMs - terminal.getUpdateTime() > timeoutMs) {
                    terminal.setStatus(com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE);
                }
                
                int maintActiveIdx = cursor.getColumnIndex(COLUMN_TERMINAL_MAINTENANCE_ACTIVE);
                if (maintActiveIdx != -1) {
                    terminal.setMaintenanceActive(cursor.getInt(maintActiveIdx) == 1);
                }
                int maintTimeIdx = cursor.getColumnIndex(COLUMN_TERMINAL_MAINTENANCE_TIME);
                if (maintTimeIdx != -1) {
                    terminal.setMaintenanceTime(cursor.getLong(maintTimeIdx));
                }
                
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
        int statusCode = com.lora.cn.ui.constants.LogStatus.fromText(status);
        if (statusCode == 0) {
            return -1;
        }
        values.put(COLUMN_LOG_STATUS, statusCode);
        values.put(COLUMN_LOG_OPERATOR, operator == null ? "" : operator);
        values.put(COLUMN_LOG_OPERATION_TIME, operationTime == null ? "" : operationTime);
        values.put(COLUMN_LOG_ACTION, action);
        boolean exists = (deviceId != null && isTerminalExists(deviceId)) || (terminalId != null && isTerminalExists(terminalId));
        String targetTable = exists ? TABLE_LOGS : TABLE_LOGS_UNBOUND;
        long result = db.insert(targetTable, null, values);
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
        int stCodeObj = logInfo.getStatusCode();
        if (stCodeObj == 0) {
            return -1;
        }
        values.put(COLUMN_LOG_STATUS, stCodeObj);
        values.put(COLUMN_LOG_OPERATOR, logInfo.getOperator() == null ? "" : logInfo.getOperator());
        values.put(COLUMN_LOG_OPERATION_TIME, logInfo.getOperationTime() == null ? "" : logInfo.getOperationTime());
        values.put(COLUMN_LOG_ACTION, logInfo.getAction());
        String ct = logInfo.getCreateTime();
        if (ct == null || ct.trim().isEmpty()) {
            ct = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
        }
        values.put(COLUMN_LOG_CREATE_TIME, ct);
        values.put("handle_user", logInfo.getHandleUser());
        values.put("handle_time", logInfo.getHandleTime());
        values.put("handle_remark", logInfo.getHandleRemark());
        boolean exists = (logInfo.getDeviceId() != null && isTerminalExists(logInfo.getDeviceId()))
                || (logInfo.getTerminalId() != null && isTerminalExists(logInfo.getTerminalId()));
        String targetTable2 = exists ? TABLE_LOGS : TABLE_LOGS_UNBOUND;
        long result = db.insert(targetTable2, null, values);
        return result;
    }

    // 动作字符串过滤：仅允许上行/下行
    private boolean isLoggableAction(String action) {
        if (action == null) return false;
        String a = action.trim();
        return a.startsWith("接收上行数据") || a.startsWith("发送下行数据") || a.contains("功能码=")
                || a.startsWith("设置") || a.contains("设置") || a.startsWith("终端清点");
    }

    /**
     * 添加上行数据日志
     * @param hex 十六进制数据
     * @return 插入的行ID
     */
    public long addUplinkLog(String hex) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // 解析帧
        com.lora.cn.utils.LoRaFrameParser.ParsedFrame frame = com.lora.cn.utils.LoRaFrameParser.parseFrame(hex);
        String deviceId = frame != null ? frame.deviceId : null;
        String terminalName = "";
        int statusCode = 0;
        String operator = "";
        String operationTime = "";
        String action = "接收上行数据";

        // 尝试根据帧中的时间替换操作时间
        
        int currentLockState = (frame != null) ? frame.stPowerLockOn : -1;
        int lastLockStateSnapshot = getLastLockStateByDeviceId(deviceId);
        int lockChangeStatusCode = 0;
        if (currentLockState != -1 && currentLockState != lastLockStateSnapshot) {
            lockChangeStatusCode = (currentLockState == 1)
                    ? com.lora.cn.ui.constants.LogStatus.LOCK_CLOSE.code
                    : com.lora.cn.ui.constants.LogStatus.LOCK_OPEN.code;
        }

        // 根据设备ID查询终端表，获取终端名称等
        int mappedLogFromTerminal = 0;
        try {
            java.util.List<com.lora.cn.ui.model.Terminal> terminals = getAllTerminals();
            if (deviceId != null && terminals != null) {
                for (com.lora.cn.ui.model.Terminal t : terminals) {
                    if (deviceId.equalsIgnoreCase(t.getTerminalId())) {
                        terminalName = t.getTerminalName();
                        try {
                            int tsCode = t.getStatus();
                            mappedLogFromTerminal =
                                    (tsCode == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE)
                                            ? com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code
                                            : (tsCode == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ONLINE)
                                            ? com.lora.cn.ui.constants.LogStatus.ONLINE.code
                                            : (tsCode == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ABNORMAL_TAKEN)
                                            ? com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                                            : (tsCode == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_NORMAL_TAKEN)
                                            ? com.lora.cn.ui.constants.LogStatus.DEVICE_ON.code
                                            : 0;
                        } catch (Exception ignored) {}
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}

        // 根据设备事件/状态位映射日志状态（来源：上行设备事件/状态）
        if (frame != null) {
            // 事件优先级：低电量 > 丢失 > 开锁/上锁 > 打开/关闭 > 取走/放入 > 定期上报 > 护士站查询
            // 备注：statusCode 使用 LogStatus 枚举的 code，便于统一展示


            boolean clearedIllegalRemovalByAck = false;
            try { clearedIllegalRemovalByAck = (frame.nurseAckParams & 0x1L) != 0; } catch (Exception ignored) {}
            if (frame.stPowerLockOn == 1 && (frame.stLayer1NotInPlace == 1 ||
                    frame.stLayer2NotInPlace == 1 || frame.stLayer3NotInPlace == 1 ||
                   frame.stLayer4NotInPlace == 1 || frame.stLayer5NotInPlace == 1)) {
               if (!clearedIllegalRemovalByAck) {
                   statusCode = com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code;
               } else {
                   statusCode = 0;
               }
            } else if ((frame.stLayer1NotInPlace == 0 &&
                     frame.stLayer2NotInPlace == 0 && frame.stLayer3NotInPlace == 0 &&
                     frame.stLayer4NotInPlace == 0 && frame.stLayer5NotInPlace == 0)) {
                 // 在线：电源锁关，且任一层板在位
                 statusCode = com.lora.cn.ui.constants.LogStatus.ONLINE.code;
             } else if (frame.stPowerLockOn == 0 && (frame.stLayer1NotInPlace == 1 ||
                     frame.stLayer2NotInPlace == 1 || frame.stLayer3NotInPlace == 1 ||
                     frame.stLayer4NotInPlace == 1 || frame.stLayer5NotInPlace == 1)) {
                 // 正常（取走）
                 statusCode = com.lora.cn.ui.constants.LogStatus.DEVICE_ON.code;
            }  else if (frame.stPowerLockOn == 0) {
                 // 开锁
                 if (!isLastLogStatus(deviceId, com.lora.cn.ui.constants.LogStatus.LOCK_OPEN.code)) {
                     statusCode = com.lora.cn.ui.constants.LogStatus.LOCK_OPEN.code;
                 }
            } else if (frame.stPowerLockOn == 1) {
                 // 上锁
                 if (!isLastLogStatus(deviceId, com.lora.cn.ui.constants.LogStatus.LOCK_CLOSE.code)) {
                     statusCode = com.lora.cn.ui.constants.LogStatus.LOCK_CLOSE.code;
                 }
            }  else if (frame.evManualPut == 1) {
                // 设备关闭（放入）
                statusCode = com.lora.cn.ui.constants.LogStatus.DEVICE_OFF.code;
            }  else  {
                // 未匹配事件，不标记为在线
                statusCode = 0;
            }
        }

        // 构造动作描述
        // 为保证发现页面解析hex，这里统一保留原始hex
        action = "接收上行数据: " + hex;

        values.put(COLUMN_LOG_TERMINAL_ID, deviceId != null ? deviceId : "");
        values.put(COLUMN_LOG_TERMINAL_NAME, terminalName);
        values.put(COLUMN_LOG_DEVICE_ID, deviceId != null ? deviceId : "");
        values.put(COLUMN_LOG_STATUS, statusCode);
        String nowStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
        String bcdStr;
        try {
            bcdStr = (frame != null && frame.dataTime != null)
                    ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(frame.dataTime)
                    : nowStr;
        } catch (Exception e) {
            bcdStr = nowStr;
        }
        values.put(COLUMN_LOG_OPERATOR, operator);
        values.put(COLUMN_LOG_OPERATION_TIME, bcdStr);
        values.put(COLUMN_LOG_ACTION, action);
        values.put(COLUMN_LOG_CREATE_TIME, bcdStr);

        String handleTimeNow = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
        String handleUserNow = com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", "");
        if (handleUserNow == null || handleUserNow.trim().isEmpty()) handleUserNow = "系统自动";
        try {
            if (deviceId != null && !deviceId.trim().isEmpty()) {
                markOfflineLogsHandled(deviceId, handleTimeNow, handleUserNow);
            }
        } catch (Exception ignored) {
            LogUtils.e("markOfflineLogsHandled:" + ignored);
        }

        boolean exists = deviceId != null && isTerminalExists(deviceId);
        String targetTable = exists ? TABLE_LOGS : TABLE_LOGS_UNBOUND;
        long result = -1L;
        boolean skipBySameStatus = false;
        boolean suppressAbnormalRepeat = false;
        Integer lastStatusGlobal = null;
        String lastTimeGlobal = null;
        android.database.Cursor c1 = db.rawQuery(
                "SELECT " + COLUMN_LOG_STATUS + ", " + COLUMN_LOG_CREATE_TIME + " FROM " + TABLE_LOGS +
                        " WHERE " + COLUMN_LOG_DEVICE_ID + "=? " +
                        " ORDER BY " + COLUMN_LOG_CREATE_TIME + " DESC LIMIT 1",
                new String[]{deviceId != null ? deviceId : ""});
        try {
            if (c1 != null && c1.moveToFirst()) {
                lastStatusGlobal = c1.getInt(0);
                lastTimeGlobal = c1.getString(1);
            }
        } finally {
            if (c1 != null) c1.close();
        }
        android.database.Cursor c2 = db.rawQuery(
                "SELECT " + COLUMN_LOG_STATUS + ", " + COLUMN_LOG_CREATE_TIME + " FROM " + TABLE_LOGS_UNBOUND +
                        " WHERE " + COLUMN_LOG_DEVICE_ID + "=? " +
                        " ORDER BY " + COLUMN_LOG_CREATE_TIME + " DESC LIMIT 1",
                new String[]{deviceId != null ? deviceId : ""});
        try {
            if (c2 != null && c2.moveToFirst()) {
                String t2 = c2.getString(1);
                if (lastTimeGlobal == null || (t2 != null && t2.compareTo(lastTimeGlobal) > 0)) {
                    lastStatusGlobal = c2.getInt(0);
                    lastTimeGlobal = t2;
                }
            }
        } finally {
            if (c2 != null) c2.close();
        }
        if (lastStatusGlobal != null && lastStatusGlobal == statusCode) {
            skipBySameStatus = true;
        }
        if (statusCode == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code) {
            try {
                java.util.List<com.lora.cn.ui.model.Terminal> terminals2 = getAllTerminals();
                if (deviceId != null && terminals2 != null) {
                    for (com.lora.cn.ui.model.Terminal t2 : terminals2) {
                        if (t2 != null && deviceId.equalsIgnoreCase(t2.getTerminalId())) {
                            int st2 = t2.getStatus();
                            if (st2 == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ABNORMAL_TAKEN) {
                                suppressAbnormalRepeat = true;
                            }
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        boolean skipByTerminalStateSame = (statusCode != 0 && mappedLogFromTerminal == statusCode);

        if (statusCode != 0 && !skipBySameStatus && !suppressAbnormalRepeat && !skipByTerminalStateSame) {
            result = db.insert(targetTable, null, values);
            LogUtils.e("上行数据库插入=\n" + targetTable + "-----result：" + result);
            LogUtils.e("上行数据库插入=\n" + values);
        }

        LogUtils.e("开关锁 状态----lockChangeStatusCode=" + lockChangeStatusCode + "-----" + (statusCode != lockChangeStatusCode));

        long lockResult = -1L;
        if (lockChangeStatusCode > 0 && statusCode != lockChangeStatusCode) {
            boolean skipLockDuplicate = false;
            android.database.Cursor cLock = db.rawQuery(
                    "SELECT " + COLUMN_LOG_STATUS + " FROM " + targetTable +
                            " WHERE " + COLUMN_LOG_DEVICE_ID + "=? " +
                            " ORDER BY " + COLUMN_LOG_ID + " DESC LIMIT 1",
                    new String[]{deviceId != null ? deviceId : ""});
            try {
                if (cLock != null && cLock.moveToFirst()) {
                    int lastSt2 = cLock.getInt(0);
                    if (lastSt2 == lockChangeStatusCode) skipLockDuplicate = true;
                }
            } finally {
                if (cLock != null) cLock.close();
            }
            if (!skipLockDuplicate) {
                ContentValues vLock = new ContentValues();
                vLock.put(COLUMN_LOG_TERMINAL_ID, deviceId != null ? deviceId : "");
                vLock.put(COLUMN_LOG_TERMINAL_NAME, terminalName);
                vLock.put(COLUMN_LOG_DEVICE_ID, deviceId != null ? deviceId : "");
                vLock.put(COLUMN_LOG_STATUS, lockChangeStatusCode);
                String nowStr2 = bcdStr;
                vLock.put(COLUMN_LOG_OPERATOR, operator);
                vLock.put(COLUMN_LOG_OPERATION_TIME, nowStr2);
                vLock.put(COLUMN_LOG_ACTION, action);
                vLock.put(COLUMN_LOG_CREATE_TIME, nowStr2);
                lockResult = db.insert(targetTable, null, vLock);
                LogUtils.e("开关锁 写入");
            }
        }

        // 同步更新终端设备的电量、信号强度，并依据上面得到的 statusCode 更新终端状态
        try {
            if (frame != null && deviceId != null) {
                updateTerminalMetricsByDeviceId(deviceId, frame.batteryLevel, frame.rssi, frame.batteryVoltage);
                if (statusCode == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code) {
                    int rows = updateTerminalStatusByDeviceId(deviceId, com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ABNORMAL_LOST);
                    android.util.Log.d("DatabaseHelper", "按日志状态更新终端为异常取走 deviceId=" + deviceId + ", rows=" + rows);
                } else if (statusCode == com.lora.cn.ui.constants.LogStatus.DEVICE_ON.code) {
                    int rows = updateTerminalStatusByDeviceId(deviceId, com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_NORMAL_TAKEN);
                    android.util.Log.d("DatabaseHelper", "按日志状态更新终端为正常取走 deviceId=" + deviceId + ", rows=" + rows);
                } else if (statusCode == com.lora.cn.ui.constants.LogStatus.ONLINE.code) {
                    int rows = updateTerminalStatusByDeviceId(deviceId, com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ONLINE);
                    android.util.Log.d("DatabaseHelper", "按日志状态更新终端为正常在线 deviceId=" + deviceId + ", rows=" + rows);
                    try {
                        String autoUser = handleUserNow;
                        markAlertLogsHandled(deviceId, handleTimeNow, autoUser, new int[]{
                                com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code,
                                com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code,
                                com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                        });
                    } catch (Exception ignored) {}
                } else if (statusCode == com.lora.cn.ui.constants.LogStatus.LOCK_OPEN.code
                        || statusCode == com.lora.cn.ui.constants.LogStatus.LOCK_CLOSE.code
                        || statusCode == com.lora.cn.ui.constants.LogStatus.DEVICE_ON.code
                        || statusCode == com.lora.cn.ui.constants.LogStatus.DEVICE_OFF.code) {
                    try {
                        String autoUser = handleUserNow;
                        markAlertLogsHandled(deviceId, handleTimeNow, autoUser, new int[]{
                                com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code,
                                com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code,
                                com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                        });
                    } catch (Exception ignored) {}
                } else {
                    android.util.Log.d("DatabaseHelper", "日志状态不触发终端状态变更 deviceId=" + deviceId + ", statusCode=" + statusCode);
                }
                // 兜底：如果上一终端状态为异常取走，而本次状态属于“恢复正常”的事件，则无条件自动处理异常/离线/低电量日志
                try {
                    boolean wasAbnormal = (mappedLogFromTerminal == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code);
                    boolean isRecoveryEvent = statusCode == com.lora.cn.ui.constants.LogStatus.ONLINE.code
                            || statusCode == com.lora.cn.ui.constants.LogStatus.DEVICE_ON.code;
//                            || statusCode == com.lora.cn.ui.constants.LogStatus.LOCK_OPEN.code
//                            || statusCode == com.lora.cn.ui.constants.LogStatus.LOCK_CLOSE.code
//                            || statusCode == com.lora.cn.ui.constants.LogStatus.DEVICE_OFF.code;
                    if (wasAbnormal && isRecoveryEvent) {
                        String autoUser2 = handleUserNow;
                        markAlertLogsHandled(deviceId, handleTimeNow, autoUser2, new int[]{
                                com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code,
                                com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code,
                                com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                        });
                    }
                } catch (Exception ignored) {}
                try {
                    org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("已入库刷新:" + deviceId));
                } catch (Exception ignored) {

                }
                int lowTh = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
                String lbKey = "low_battery_flag_device_" + deviceId;
                boolean flagged = com.blankj.utilcode.util.SPUtils.getInstance().getBoolean(lbKey, false);
                if (frame.batteryLevel <= lowTh) {
                    if (!flagged) {
                        ContentValues v2 = new ContentValues();
                        v2.put(COLUMN_LOG_TERMINAL_ID, deviceId != null ? deviceId : "");
                        v2.put(COLUMN_LOG_TERMINAL_NAME, terminalName);
                        v2.put(COLUMN_LOG_DEVICE_ID, deviceId != null ? deviceId : "");
                        v2.put(COLUMN_LOG_STATUS, com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code);
                        v2.put(COLUMN_LOG_OPERATOR, "");
                        v2.put(COLUMN_LOG_OPERATION_TIME, nowStr);
                        v2.put(COLUMN_LOG_ACTION, "设备低电量");
                        v2.put(COLUMN_LOG_CREATE_TIME, nowStr);
                        db.insert(targetTable, null, v2);
                        com.blankj.utilcode.util.SPUtils.getInstance().put(lbKey, true);
                    }
                } else {
                    if (flagged) {
                        com.blankj.utilcode.util.SPUtils.getInstance().put(lbKey, false);
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("DatabaseHelper", "更新终端电量/信号/状态失败", e);
        }

        return (result > 0L) ? result : lockResult;
    }

    private void markOfflineLogsHandled(String deviceId, String handleTime, String handleUser) {
        if (deviceId == null || deviceId.trim().isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        java.util.List<Long> ids = new java.util.ArrayList<>();
        android.database.Cursor c1 = db.rawQuery(
                "SELECT " + COLUMN_LOG_ID + ", handle_user, handle_time FROM " + TABLE_LOGS +
                        " WHERE " + COLUMN_LOG_DEVICE_ID + "=? AND " + COLUMN_LOG_STATUS + "=?",
                new String[]{deviceId, String.valueOf(com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code)});
        while (c1.moveToNext()) {
            long id = c1.getLong(0);
            ids.add(id);
        }
        c1.close();
        android.database.Cursor c2 = db.rawQuery(
                "SELECT " + COLUMN_LOG_ID + ", handle_user, handle_time FROM " + TABLE_LOGS_UNBOUND +
                        " WHERE " + COLUMN_LOG_DEVICE_ID + "=? AND " + COLUMN_LOG_STATUS + "=?",
                new String[]{deviceId, String.valueOf(com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code)});
        java.util.List<Long> idsUnbound = new java.util.ArrayList<>();
        while (c2.moveToNext()) {
            long id = c2.getLong(0);
            idsUnbound.add(id);
        }
        c2.close();
        if (ids.isEmpty() && idsUnbound.isEmpty()) return;
        ContentValues v = new ContentValues();
        String user2 = handleUser;
        if (user2 == null || user2.trim().isEmpty()) {
            user2 = com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", "系统自动");
        }
        v.put("handle_user", user2);
        v.put("handle_time", handleTime == null ? "" : handleTime);
        v.put("handle_remark", "自动处理");
        android.util.Log.d("DatabaseHelper", "离线处理准备 deviceId=" + deviceId + ", ids=" + ids.size() + ", idsUnbound=" + idsUnbound.size() + ", user=" + user2 + ", time=" + (handleTime == null ? "" : handleTime));
        int updated1 = 0;
        for (Long id : ids) {
            try { updated1 += db.update(TABLE_LOGS, v, COLUMN_LOG_ID + "=?", new String[]{String.valueOf(id)}); } catch (Exception ignored) {}
        }
        int updated2 = 0;
        for (Long id : idsUnbound) {
            try { updated2 += db.update(TABLE_LOGS_UNBOUND, v, COLUMN_LOG_ID + "=?", new String[]{String.valueOf(id)}); } catch (Exception ignored) {}
        }
        android.util.Log.d("DatabaseHelper", "离线处理完成 deviceId=" + deviceId + ", updatedLogs=" + updated1 + ", updatedUnbound=" + updated2);
        try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("自动处理离线:" + deviceId)); } catch (Exception ignored) {}
    }

    private void markAlertLogsHandled(String deviceId, String handleTime, String handleUser, int[] statuses) {
        if (deviceId == null || deviceId.trim().isEmpty()) return;
        if (statuses == null || statuses.length == 0) return;
        SQLiteDatabase db = this.getWritableDatabase();
        java.util.List<Long> ids = new java.util.ArrayList<>();
        java.util.List<Long> idsUnbound = new java.util.ArrayList<>();
        for (int s : statuses) {
            android.database.Cursor c1 = db.rawQuery(
                    "SELECT " + COLUMN_LOG_ID + ", handle_user, handle_time FROM " + TABLE_LOGS +
                            " WHERE " + COLUMN_LOG_DEVICE_ID + "=? AND " + COLUMN_LOG_STATUS + "=?",
                    new String[]{deviceId, String.valueOf(s)});
            while (c1.moveToNext()) {
                long id = c1.getLong(0);
                ids.add(id);
            }
            c1.close();
            android.database.Cursor c2 = db.rawQuery(
                    "SELECT " + COLUMN_LOG_ID + ", handle_user, handle_time FROM " + TABLE_LOGS_UNBOUND +
                            " WHERE " + COLUMN_LOG_DEVICE_ID + "=? AND " + COLUMN_LOG_STATUS + "=?",
                    new String[]{deviceId, String.valueOf(s)});
            while (c2.moveToNext()) {
                long id = c2.getLong(0);
                idsUnbound.add(id);
            }
            c2.close();
        }
        if (ids.isEmpty() && idsUnbound.isEmpty()) return;
        ContentValues v = new ContentValues();
        String user2 = handleUser;
        if (user2 == null || user2.trim().isEmpty()) {
            user2 = com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", "系统自动");
        }
        v.put("handle_user", user2);
        v.put("handle_time", handleTime == null ? "" : handleTime);
        v.put("handle_remark", "自动处理");
        android.util.Log.d("DatabaseHelper", "异常/低电量/离线 自动处理准备 deviceId=" + deviceId + ", ids=" + ids.size() + ", idsUnbound=" + idsUnbound.size() + ", user=" + user2 + ", time=" + (handleTime == null ? "" : handleTime));
        int updated1 = 0;
        for (Long id : ids) {
            try { updated1 += db.update(TABLE_LOGS, v, COLUMN_LOG_ID + "=?", new String[]{String.valueOf(id)}); } catch (Exception ignored) {}
        }
        int updated2 = 0;
        for (Long id : idsUnbound) {
            try { updated2 += db.update(TABLE_LOGS_UNBOUND, v, COLUMN_LOG_ID + "=?", new String[]{String.valueOf(id)}); } catch (Exception ignored) {}
        }
        android.util.Log.d("DatabaseHelper", "异常/低电量/离线 自动处理完成 deviceId=" + deviceId + ", updatedLogs=" + updated1 + ", updatedUnbound=" + updated2);
        try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("自动处理恢复:" + deviceId)); } catch (Exception ignored) {}
    }

    private int getLastLockStateByDeviceId(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) return -1;
        SQLiteDatabase db = this.getReadableDatabase();
        int last = -1;
        String inLockCodes = com.lora.cn.ui.constants.LogStatus.LOCK_OPEN.code + "," + com.lora.cn.ui.constants.LogStatus.LOCK_CLOSE.code;
        android.database.Cursor c1 = db.rawQuery(
                "SELECT " + COLUMN_LOG_STATUS + " FROM " + TABLE_LOGS +
                        " WHERE " + COLUMN_LOG_DEVICE_ID + "=? AND " + COLUMN_LOG_STATUS + " IN (" + inLockCodes + ") " +
                        " ORDER BY " + COLUMN_LOG_ID + " DESC LIMIT 1",
                new String[]{deviceId});
        try {
            if (c1 != null && c1.moveToFirst()) {
                int st = c1.getInt(0);
                last = (st == com.lora.cn.ui.constants.LogStatus.LOCK_OPEN.code) ? 0 :
                        (st == com.lora.cn.ui.constants.LogStatus.LOCK_CLOSE.code) ? 1 : -1;
            }
        } finally {
            if (c1 != null) c1.close();
        }
        if (last != -1) return last;
        android.database.Cursor c2 = db.rawQuery(
                "SELECT " + COLUMN_LOG_STATUS + " FROM " + TABLE_LOGS_UNBOUND +
                        " WHERE " + COLUMN_LOG_DEVICE_ID + "=? AND " + COLUMN_LOG_STATUS + " IN (" + inLockCodes + ") " +
                        " ORDER BY " + COLUMN_LOG_ID + " DESC LIMIT 1",
                new String[]{deviceId});
        try {
            if (c2 != null && c2.moveToFirst()) {
                int st = c2.getInt(0);
                last = (st == com.lora.cn.ui.constants.LogStatus.LOCK_OPEN.code) ? 0 :
                        (st == com.lora.cn.ui.constants.LogStatus.LOCK_CLOSE.code) ? 1 : -1;
            }
        } finally {
            if (c2 != null) c2.close();
        }
        return last;
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
        }
    }

    /**
     * 根据设备ID更新终端状态
     */
    public int updateTerminalStatusByDeviceId(String deviceId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_TERMINAL_STATUS, status);
            values.put(COLUMN_TERMINAL_UPDATE_TIME, System.currentTimeMillis());
            return db.update(TABLE_TERMINALS, values,
                    COLUMN_TERMINAL_DEVICE_ID + "=?",
                    new String[]{deviceId});
        } finally {
        }
    }

    /**
     * 插入一条“设备离线”日志；仅跳过连续离线的重复。
     */
    public long addOfflineLog(String deviceId, String terminalName) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = deviceId != null && isTerminalExists(deviceId);
        if (!exists) return -1L;
        String targetTable = TABLE_LOGS;
        android.database.Cursor c = db.rawQuery(
                "SELECT " + COLUMN_LOG_STATUS + " FROM " + targetTable +
                        " WHERE " + COLUMN_LOG_DEVICE_ID + "=? " +
                        " ORDER BY " + COLUMN_LOG_ID + " DESC LIMIT 1",
                new String[]{deviceId != null ? deviceId : ""});
        try {
            if (c != null && c.moveToFirst()) {
                int lastSt = c.getInt(0);
                if (lastSt == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code) return -1L;
            }
        } finally {
            if (c != null) c.close();
        }

        String nowStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
        ContentValues v = new ContentValues();
        v.put(COLUMN_LOG_TERMINAL_ID, deviceId != null ? deviceId : "");
        v.put(COLUMN_LOG_TERMINAL_NAME, terminalName != null ? terminalName : "");
        v.put(COLUMN_LOG_DEVICE_ID, deviceId != null ? deviceId : "");
        v.put(COLUMN_LOG_STATUS, com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code);
        v.put(COLUMN_LOG_OPERATOR, "");
        v.put(COLUMN_LOG_OPERATION_TIME, nowStr);
        v.put(COLUMN_LOG_ACTION, "设备离线");
        v.put(COLUMN_LOG_CREATE_TIME, nowStr);
        return db.insert(targetTable, null, v);
    }

    public long addLowBatteryLog(String deviceId, String terminalName) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean exists = deviceId != null && isTerminalExists(deviceId);
        if (!exists) return -1L;
        String targetTable = TABLE_LOGS;
        android.database.Cursor c = db.rawQuery(
                "SELECT " + COLUMN_LOG_STATUS + " FROM " + targetTable +
                        " WHERE " + COLUMN_LOG_DEVICE_ID + "=? " +
                        " ORDER BY " + COLUMN_LOG_ID + " DESC LIMIT 1",
                new String[]{deviceId != null ? deviceId : ""});
        try {
            if (c != null && c.moveToFirst()) {
                int lastSt = c.getInt(0);
                if (lastSt == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code) return -1L;
            }
        } finally {
            if (c != null) c.close();
        }
        String nowStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
        ContentValues v = new ContentValues();
        v.put(COLUMN_LOG_TERMINAL_ID, deviceId != null ? deviceId : "");
        v.put(COLUMN_LOG_TERMINAL_NAME, terminalName != null ? terminalName : "");
        v.put(COLUMN_LOG_DEVICE_ID, deviceId != null ? deviceId : "");
        v.put(COLUMN_LOG_STATUS, com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code);
        v.put(COLUMN_LOG_OPERATOR, "");
        v.put(COLUMN_LOG_OPERATION_TIME, nowStr);
        v.put(COLUMN_LOG_ACTION, "设备低电量");
        v.put(COLUMN_LOG_CREATE_TIME, nowStr);
        return db.insert(targetTable, null, v);
    }

    /**
     * 将RSSI(0~138，代表-138~0dBm)映射为0~4根信号条
     */
    private int mapRssiToBars(int rssiRaw) {
        int v = Math.max(0, Math.min(138, rssiRaw));
        if (v <= 65) return 4;
        if (v <= 75) return 3;
        if (v <= 85) return 2;
        if (v <= 95) return 1;
        return 0;
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
            int st = cursor.getInt(cursor.getColumnIndex(COLUMN_LOG_STATUS));
            log.setStatusCode(st);
            log.setOperator(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_OPERATOR)));
            log.setOperationTime(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_OPERATION_TIME)));
            log.setAction(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_ACTION)));
            log.setCreateTime(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_CREATE_TIME)));
            int hUserIdx = cursor.getColumnIndex("handle_user");
            int hTimeIdx = cursor.getColumnIndex("handle_time");
            int hRemarkIdx = cursor.getColumnIndex("handle_remark");
            if (hUserIdx != -1) log.setHandleUser(cursor.getString(hUserIdx));
            if (hTimeIdx != -1) log.setHandleTime(cursor.getString(hTimeIdx));
            if (hRemarkIdx != -1) log.setHandleRemark(cursor.getString(hRemarkIdx));
            
            logs.add(log);
        }
        cursor.close();
        return logs;
    }

    public void syncLowBatteryFlags() {
        try {
            int lowTh = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
            java.util.List<com.lora.cn.ui.model.Terminal> terminals = getAllTerminals();
            String nowStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
            SQLiteDatabase db = this.getWritableDatabase();
            for (com.lora.cn.ui.model.Terminal t : terminals) {
                String deviceId = t.getTerminalId();
                String lbKey = "low_battery_flag_device_" + deviceId;
                boolean flagged = com.blankj.utilcode.util.SPUtils.getInstance().getBoolean(lbKey, false);
                if (t.getBatteryLevel() <= lowTh) {
                    if (!flagged) {
                        android.content.ContentValues v2 = new android.content.ContentValues();
                        v2.put(COLUMN_LOG_TERMINAL_ID, deviceId != null ? deviceId : "");
                        v2.put(COLUMN_LOG_TERMINAL_NAME, t.getTerminalName());
                        v2.put(COLUMN_LOG_DEVICE_ID, deviceId != null ? deviceId : "");
                        v2.put(COLUMN_LOG_STATUS, com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code);
                        v2.put(COLUMN_LOG_OPERATOR, "");
                        v2.put(COLUMN_LOG_OPERATION_TIME, nowStr);
                        v2.put(COLUMN_LOG_ACTION, "设备低电量");
                        v2.put(COLUMN_LOG_CREATE_TIME, nowStr);
                        db.insert(TABLE_LOGS, null, v2);
                        com.blankj.utilcode.util.SPUtils.getInstance().put(lbKey, true);
                    }
                } else {
                    if (flagged) {
                        com.blankj.utilcode.util.SPUtils.getInstance().put(lbKey, false);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * 获取只绑定到已添加终端的日志（terminal_id 存在于终端表）
     */
    public java.util.List<com.lora.cn.ui.model.LogInfo> getAllLogsBoundToTerminals() {
        java.util.List<com.lora.cn.ui.model.LogInfo> logs = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_LOGS + " WHERE EXISTS (" +
                "SELECT 1 FROM " + TABLE_TERMINALS + " t WHERE t." + COLUMN_TERMINAL_DEVICE_ID + " = " + TABLE_LOGS + "." + COLUMN_LOG_TERMINAL_ID +
                ") ORDER BY " + COLUMN_LOG_CREATE_TIME + " DESC";
        android.database.Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            com.lora.cn.ui.model.LogInfo log = new com.lora.cn.ui.model.LogInfo();
            log.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_LOG_ID)));
            log.setTerminalId(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_TERMINAL_ID)));
            log.setTerminalName(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_TERMINAL_NAME)));
            log.setDeviceId(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_DEVICE_ID)));
            int st = cursor.getInt(cursor.getColumnIndex(COLUMN_LOG_STATUS));
            log.setStatusCode(st);
            log.setOperator(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_OPERATOR)));
            log.setOperationTime(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_OPERATION_TIME)));
            log.setAction(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_ACTION)));
            log.setCreateTime(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_CREATE_TIME)));
            int hUserIdx = cursor.getColumnIndex("handle_user");
            int hTimeIdx = cursor.getColumnIndex("handle_time");
            int hRemarkIdx = cursor.getColumnIndex("handle_remark");
            if (hUserIdx != -1) log.setHandleUser(cursor.getString(hUserIdx));
            if (hTimeIdx != -1) log.setHandleTime(cursor.getString(hTimeIdx));
            if (hRemarkIdx != -1) log.setHandleRemark(cursor.getString(hRemarkIdx));
            logs.add(log);
        }
        cursor.close();
        return logs;
    }

    /** 获取未绑定到终端的日志 */
    public java.util.List<com.lora.cn.ui.model.LogInfo> getAllUnboundLogs() {
        java.util.List<com.lora.cn.ui.model.LogInfo> logs = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_LOGS_UNBOUND + " ORDER BY " + COLUMN_LOG_CREATE_TIME + " DESC";
        android.database.Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            com.lora.cn.ui.model.LogInfo log = new com.lora.cn.ui.model.LogInfo();
            log.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_LOG_ID)));
            log.setTerminalId(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_TERMINAL_ID)));
            log.setTerminalName(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_TERMINAL_NAME)));
            log.setDeviceId(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_DEVICE_ID)));
            int st = cursor.getInt(cursor.getColumnIndex(COLUMN_LOG_STATUS));
            log.setStatusCode(st);
            log.setOperator(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_OPERATOR)));
            log.setOperationTime(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_OPERATION_TIME)));
            log.setAction(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_ACTION)));
            log.setCreateTime(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_CREATE_TIME)));
            int hUserIdx = cursor.getColumnIndex("handle_user");
            int hTimeIdx = cursor.getColumnIndex("handle_time");
            int hRemarkIdx = cursor.getColumnIndex("handle_remark");
            if (hUserIdx != -1) log.setHandleUser(cursor.getString(hUserIdx));
            if (hTimeIdx != -1) log.setHandleTime(cursor.getString(hTimeIdx));
            if (hRemarkIdx != -1) log.setHandleRemark(cursor.getString(hRemarkIdx));
            logs.add(log);
        }
        cursor.close();
        return logs;
    }

    public int queryLogsCount(String startTime, String endTime, int typeSel, int policeSel, boolean includeUnbound) {
        SQLiteDatabase db = this.getReadableDatabase();
        String where = buildLogWhereClause(startTime, endTime, typeSel, policeSel);
        String sql = "SELECT COUNT(*) FROM " + TABLE_LOGS + where;
        int total = 0;
        android.database.Cursor c = db.rawQuery(sql, null);
        if (c.moveToFirst()) total += c.getInt(0);
        c.close();
        if (includeUnbound) {
            String sql2 = "SELECT COUNT(*) FROM " + TABLE_LOGS_UNBOUND + where;
            android.database.Cursor c2 = db.rawQuery(sql2, null);
            if (c2.moveToFirst()) total += c2.getInt(0);
            c2.close();
        }
        return total;
    }

    public java.util.List<com.lora.cn.ui.model.LogInfo> queryLogsPaged(String startTime, String endTime, int typeSel, int policeSel, boolean includeUnbound, int pageSize, int pageIndex) {
        java.util.List<com.lora.cn.ui.model.LogInfo> out = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String where = buildLogWhereClause(startTime, endTime, typeSel, policeSel);
        int offset = Math.max(0, pageIndex) * Math.max(1, pageSize);
        String base = "SELECT * FROM %s" + where;
        String sql;
        if (includeUnbound) {
            sql = String.format(java.util.Locale.getDefault(), base, TABLE_LOGS) +
                  " UNION ALL " + String.format(java.util.Locale.getDefault(), base, TABLE_LOGS_UNBOUND) +
                  " ORDER BY " + COLUMN_LOG_CREATE_TIME + " DESC LIMIT " + pageSize + " OFFSET " + offset;
        } else {
            sql = String.format(java.util.Locale.getDefault(), base, TABLE_LOGS) +
                  " ORDER BY " + COLUMN_LOG_CREATE_TIME + " DESC LIMIT " + pageSize + " OFFSET " + offset;
        }
        android.database.Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            com.lora.cn.ui.model.LogInfo log = new com.lora.cn.ui.model.LogInfo();
            log.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_LOG_ID)));
            log.setTerminalId(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_TERMINAL_ID)));
            log.setTerminalName(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_TERMINAL_NAME)));
            log.setDeviceId(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_DEVICE_ID)));
            int st = cursor.getInt(cursor.getColumnIndex(COLUMN_LOG_STATUS));
            log.setStatusCode(st);
            log.setOperator(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_OPERATOR)));
            log.setOperationTime(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_OPERATION_TIME)));
            log.setAction(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_ACTION)));
            log.setCreateTime(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_CREATE_TIME)));
            int hUserIdx = cursor.getColumnIndex("handle_user");
            int hTimeIdx = cursor.getColumnIndex("handle_time");
            int hRemarkIdx = cursor.getColumnIndex("handle_remark");
            if (hUserIdx != -1) log.setHandleUser(cursor.getString(hUserIdx));
            if (hTimeIdx != -1) log.setHandleTime(cursor.getString(hTimeIdx));
            if (hRemarkIdx != -1) log.setHandleRemark(cursor.getString(hRemarkIdx));
            out.add(log);
        }
        cursor.close();
        return out;
    }

    private String buildLogWhereClause(String startTime, String endTime, int typeSel, int policeSel) {
        //LogUtils.e("buildLogWhereClause ==" + typeSel + "======" + policeSel  + "=====" + startTime +  "--" + endTime);
        java.util.List<String> conds = new java.util.ArrayList<>();
        if (startTime != null && !startTime.isEmpty()) conds.add(COLUMN_LOG_CREATE_TIME + " >= '" + startTime + "' ");
        if (endTime != null && !endTime.isEmpty()) conds.add(COLUMN_LOG_CREATE_TIME + " <= '" + endTime + "' ");
        if (typeSel == 0) {
            conds.add(COLUMN_LOG_STATUS + " IN (" +
                    com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code + "," +
                    com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code + "," +
                    com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code + "," +
                    com.lora.cn.ui.constants.LogStatus.LOCK_OPEN.code + "," +
                    com.lora.cn.ui.constants.LogStatus.LOCK_CLOSE.code +
                    ")");
        } else if (typeSel == 1) {
        } else if (typeSel == 2) {
            conds.add(COLUMN_LOG_STATUS + " = " + com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code);
        } else if (typeSel == 3) {
            conds.add(COLUMN_LOG_STATUS + " = " + com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code);
        } else if (typeSel == 4) {
            conds.add(COLUMN_LOG_STATUS + " IN (" + com.lora.cn.ui.constants.LogStatus.LOCK_OPEN.code + "," + com.lora.cn.ui.constants.LogStatus.LOCK_CLOSE.code + ")");
        } else if (typeSel == 5) {
            conds.add(COLUMN_LOG_STATUS + " = " + com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code);
        }
        if (policeSel == 1 || policeSel == 2) {
            if (policeSel == 1) {
                conds.add("((handle_user IS NOT NULL AND TRIM(handle_user) <> '') OR (handle_time IS NOT NULL AND TRIM(handle_time) <> ''))");
            } else {
                conds.add("((handle_user IS NULL OR TRIM(handle_user) = '') AND (handle_time IS NULL OR TRIM(handle_time) = ''))");
            }
        }
        if (conds.isEmpty()) return "";
        String  json  =" WHERE " + android.text.TextUtils.join(" AND ", conds);
        //LogUtils.e("buildLogWhereClause ==" + json);
        return json;
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
            int st = cursor.getInt(cursor.getColumnIndex(COLUMN_LOG_STATUS));
            log.setStatusCode(st);
            log.setOperator(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_OPERATOR)));
            log.setOperationTime(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_OPERATION_TIME)));
            log.setAction(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_ACTION)));
            log.setCreateTime(cursor.getString(cursor.getColumnIndex(COLUMN_LOG_CREATE_TIME)));
            int hUserIdx = cursor.getColumnIndex("handle_user");
            int hTimeIdx = cursor.getColumnIndex("handle_time");
            int hRemarkIdx = cursor.getColumnIndex("handle_remark");
            if (hUserIdx != -1) log.setHandleUser(cursor.getString(hUserIdx));
            if (hTimeIdx != -1) log.setHandleTime(cursor.getString(hTimeIdx));
            if (hRemarkIdx != -1) log.setHandleRemark(cursor.getString(hRemarkIdx));
            
            logs.add(log);
        }
        cursor.close();
        return logs;
    }
    
    // 初始化示例日志数据
//    public void initSampleLogData() {
//        SQLiteDatabase db = this.getReadableDatabase();
//        android.database.Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_LOGS, null);
//        cursor.moveToFirst();
//        int count = cursor.getInt(0);
//        cursor.close();
//
//        // 如果日志表为空，则添加示例数据
//        if (count == 0) {
//            addLog("1", "终端设备001", "DEV001", "正常在线", "张三", "2024-01-15 10:35:00", "数据上传");
//            addLog("2", "终端设备002", "DEV002", "低电量", "李四", "", "电量检测");
//            addLog("3", "终端设备003", "DEV003", "异常取走", "王五", "", "设备检查");
//            addLog("4", "终端设备004", "DEV004", "正常在线", "赵六", "2024-01-15 07:20:00", "状态更新");
//            addLog("5", "终端设备005", "DEV005", "异常丢失", "孙七", "", "异常处理");
//            addLog("1", "终端设备001", "DEV001", "离线", "张三", "2024-01-15 11:00:00", "设备维护");
//            addLog("6", "终端设备006", "DEV006", "正常在线", "周八", "2024-01-15 08:15:30", "定期检查");
//            addLog("7", "终端设备007", "DEV007", "低电量", "吴九", "", "电池更换");
//            addLog("2", "终端设备002", "DEV002", "正常在线", "李四", "2024-01-15 12:30:45", "电量恢复");
//            addLog("8", "终端设备008", "DEV008", "异常取走", "郑十", "", "紧急查找");
//        }
//    }

//    // 清理示例日志数据：删除 device_id 为 DEV00X 的记录或示例人员
//    public int cleanSampleLogData() {
//        SQLiteDatabase db = this.getWritableDatabase();
//        // 通过设备ID模式和示例操作者名称进行清理
//        String whereClause = COLUMN_LOG_DEVICE_ID + " LIKE 'DEV00%' OR " + COLUMN_LOG_OPERATOR + " IN (?,?,?,?,?,?,?,?)";
//        String[] whereArgs = new String[]{"张三","李四","王五","赵六","孙七","周八","吴九","郑十"};
//        int deleted = db.delete(TABLE_LOGS, whereClause, whereArgs);
//        return deleted;
//    }

    /**
     * 更新终端收藏状态
     */
    public int updateTerminalFavoriteStatus(String terminalId, boolean isFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_TERMINAL_IS_FAVORITE, isFavorite ? 1 : 0);
        long currentUserId = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", 0);
        values.put(COLUMN_TERMINAL_FAVORITE_USER_ID, isFavorite ? (int) currentUserId : 0);
        values.put(COLUMN_TERMINAL_UPDATE_TIME, System.currentTimeMillis());
        
        return db.update(TABLE_TERMINALS, values, 
                COLUMN_TERMINAL_DEVICE_ID + "=?", 
                new String[]{terminalId});
    }

//    public java.util.List<com.lora.cn.ui.model.Terminal> getTerminalsPaged(int limit, int offset) {
//        java.util.List<com.lora.cn.ui.model.Terminal> terminals = new java.util.ArrayList<>();
//        SQLiteDatabase db = this.getReadableDatabase();
//        String sql = "SELECT * FROM " + TABLE_TERMINALS + " ORDER BY " + COLUMN_TERMINAL_UPDATE_TIME + " DESC LIMIT ? OFFSET ?";
//        android.database.Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(limit), String.valueOf(offset)});
//        if (cursor.moveToFirst()) {
//            do {
//                com.lora.cn.ui.model.Terminal terminal = new com.lora.cn.ui.model.Terminal();
//                terminal.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_TERMINAL_ID)));
//                terminal.setTerminalId(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_DEVICE_ID)));
//                terminal.setDeviceCode(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_DEVICE_CODE)));
//                terminal.setTerminalName(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_NAME)));
//                String stText = cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_STATUS));
//                terminal.setStatus(com.lora.cn.ui.constants.TerminalStatusConstants.textToCode(stText));
//                terminal.setSignalStrength(cursor.getInt(cursor.getColumnIndex(COLUMN_TERMINAL_SIGNAL_STRENGTH)));
//                terminal.setDepartment(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_DEPARTMENT)));
//                terminal.setLocation(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_LOCATION)));
//                terminal.setDepartmentId(cursor.getInt(cursor.getColumnIndex(COLUMN_TERMINAL_DEPARTMENT_ID)));
//                terminal.setRoomId(cursor.getInt(cursor.getColumnIndex(COLUMN_TERMINAL_ROOM_ID)));
//                terminal.setNursingGroupId(cursor.getInt(cursor.getColumnIndex(COLUMN_TERMINAL_NURSING_GROUP_ID)));
//                terminal.setOtherId(cursor.getInt(cursor.getColumnIndex(COLUMN_TERMINAL_OTHER_ID)));
//                int batteryLevelIndex = cursor.getColumnIndex(COLUMN_TERMINAL_BATTERY_LEVEL);
//                if (batteryLevelIndex != -1) {
//                    terminal.setBatteryLevel(cursor.getInt(batteryLevelIndex));
//                } else {
//                    terminal.setBatteryLevel(cursor.getInt(cursor.getColumnIndex(COLUMN_TERMINAL_SIGNAL_STRENGTH)));
//                }
//                terminal.setExtension(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_EXTENSION)));
//                terminal.setCreateTime(cursor.getLong(cursor.getColumnIndex(COLUMN_TERMINAL_CREATE_TIME)));
//                terminal.setUpdateTime(cursor.getLong(cursor.getColumnIndex(COLUMN_TERMINAL_UPDATE_TIME)));
//                long nowMs = System.currentTimeMillis();
//                if (terminal.getUpdateTime() > 0 && nowMs - terminal.getUpdateTime() > 3 * 60 * 1000L) {
//                    terminal.setStatus(com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE);
//                }
//                terminals.add(terminal);
//            } while (cursor.moveToNext());
//        }
//        cursor.close();
//        return terminals;
//    }
//
//    public int getTerminalsCount() {
//        SQLiteDatabase db = this.getReadableDatabase();
//        android.database.Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TERMINALS, null);
//        int count = 0;
//        if (cursor.moveToFirst()) {
//            count = cursor.getInt(0);
//        }
//        cursor.close();
//        return count;
//    }
    
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
     * 更新终端维护状态
     */
    public int updateTerminalMaintenanceState(String terminalId, boolean active, long timeMs) {
        SQLiteDatabase db = this.getWritableDatabase();
        ensureColumnIfMissing(db, TABLE_TERMINALS, COLUMN_TERMINAL_MAINTENANCE_ACTIVE, "INTEGER DEFAULT 0");
        ensureColumnIfMissing(db, TABLE_TERMINALS, COLUMN_TERMINAL_MAINTENANCE_TIME, "INTEGER DEFAULT 0");
        ContentValues values = new ContentValues();
        values.put(COLUMN_TERMINAL_MAINTENANCE_ACTIVE, active ? 1 : 0);
        values.put(COLUMN_TERMINAL_MAINTENANCE_TIME, Math.max(0L, timeMs));
        values.put(COLUMN_TERMINAL_UPDATE_TIME, System.currentTimeMillis());
        return db.update(TABLE_TERMINALS, values,
                COLUMN_TERMINAL_DEVICE_ID + "=?",
                new String[]{terminalId == null ? "" : terminalId});
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
        try {
            int result = db.delete(TABLE_TERMINALS, COLUMN_TERMINAL_DEVICE_ID + "=?", new String[]{deviceId});
            if (result > 0) {
                try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("删除终端:" + deviceId)); } catch (Exception ignored) {}
            }
            return result;
        } finally {
        }
    }

    public boolean isLastLogStatus(String deviceId, int status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_LOG_STATUS + " FROM " + TABLE_LOGS + 
                       " WHERE " + COLUMN_LOG_DEVICE_ID + " = ? " +
                       " ORDER BY " + COLUMN_LOG_ID + " DESC LIMIT 1";
        android.database.Cursor cursor = db.rawQuery(query, new String[]{deviceId});
        boolean result = false;
        if (cursor.moveToFirst()) {
            result = cursor.getInt(0) == status;
        }
        cursor.close();
        return result;
    }

    public void checkAndLogOfflineDevices() {
        SQLiteDatabase db = this.getWritableDatabase();
        long now = System.currentTimeMillis();
        long timeout = 3 * 60 * 1000L;
        long threshold = now - timeout;
        
        String sql = "SELECT " + COLUMN_TERMINAL_DEVICE_ID + ", " + COLUMN_TERMINAL_NAME + 
                     " FROM " + TABLE_TERMINALS + 
                     " WHERE " + COLUMN_TERMINAL_UPDATE_TIME + " < ? AND " + COLUMN_TERMINAL_STATUS + " != ?";
        android.database.Cursor c = db.rawQuery(sql, new String[]{String.valueOf(threshold), com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_OFFLINE});
        
        while (c.moveToNext()) {
            String did = c.getString(0);
            String name = c.getString(1);
            
            ContentValues v = new ContentValues();
            v.put(COLUMN_TERMINAL_STATUS, com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_OFFLINE);
            db.update(TABLE_TERMINALS, v, COLUMN_TERMINAL_DEVICE_ID + "=?", new String[]{did});
            
            addOfflineLog(did, name);
        }
        c.close();
    }

    /**
     * 标记日志为已处理，并记录处理人/处理时间/备注
     */
    public int updateLogHandled(long logId, String handleUser, String handleTime, String handleRemark) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("handle_user", handleUser == null ? "" : handleUser);
        values.put("handle_time", handleTime == null ? "" : handleTime);
        values.put("handle_remark", handleRemark == null ? "" : handleRemark);
        try {
            int r = db.update(TABLE_LOGS, values, COLUMN_LOG_ID + "=?", new String[]{String.valueOf(logId)});
            if (r == 0) {
                r = db.update(TABLE_LOGS_UNBOUND, values, COLUMN_LOG_ID + "=?", new String[]{String.valueOf(logId)});
            }
            try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("处理刷新:" + logId)); } catch (Exception ignored) {}
            return r;
        } finally {
        }
    }

    public long addMaintenanceRecord(com.lora.cn.ui.model.MaintenanceInfo info) {
        if (info == null) return -1L;
        SQLiteDatabase db = this.getWritableDatabase();
        ensureMaintenanceSchema(db);
        ContentValues v = new ContentValues();
        String tid = info.getTerminalId() == null ? "" : info.getTerminalId();
        v.put(COLUMN_MAINTENANCE_TERMINAL_ID, tid);
        Boolean hasDeviceIdCol = maintenanceHasDeviceIdColumn;
        if (hasDeviceIdCol == null) {
            hasDeviceIdCol = hasColumn(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_DEVICE_ID);
            maintenanceHasDeviceIdColumn = hasDeviceIdCol;
        }
        if (hasDeviceIdCol) {
            v.put(COLUMN_MAINTENANCE_DEVICE_ID, tid);
        }
        v.put(COLUMN_MAINTENANCE_TERMINAL_NAME, info.getTerminalName() == null ? "" : info.getTerminalName());
        v.put(COLUMN_MAINTENANCE_TERMINAL_GROUP, info.getTerminalGroup() == null ? "" : info.getTerminalGroup());
        v.put(COLUMN_MAINTENANCE_STATUS, info.getStatus());
        v.put(COLUMN_MAINTENANCE_CONTENT, info.getContent() == null ? "" : info.getContent());
        v.put(COLUMN_MAINTENANCE_CREATE_USER_ID, info.getCreateUserId());
        v.put(COLUMN_MAINTENANCE_CREATE_USER, info.getCreateUser() == null ? "" : info.getCreateUser());
        v.put(COLUMN_MAINTENANCE_CREATE_TIME, info.getCreateTime() == null ? "" : info.getCreateTime());
        v.put(COLUMN_MAINTENANCE_HANDLE_USER_ID, info.getHandleUserId());
        v.put(COLUMN_MAINTENANCE_HANDLE_USER, info.getHandleUser() == null ? "" : info.getHandleUser());
        v.put(COLUMN_MAINTENANCE_HANDLE_TIME, info.getHandleTime() == null ? "" : info.getHandleTime());
        try {
            java.lang.reflect.Method m = info.getClass().getMethod("getHandleRemark");
            Object hr = m.invoke(info);
            v.put("handle_remark", hr == null ? "" : String.valueOf(hr));
        } catch (Exception ignored) {
            v.put("handle_remark", "");
        }
        v.put(COLUMN_MAINTENANCE_SENT_FLAG, 0);
        v.put(COLUMN_MAINTENANCE_SENT_TIME, "");
        return db.insert(TABLE_MAINTENANCE, null, v);
    }

    private String resolveMaintenanceIdColumn(SQLiteDatabase db) {
        if (db == null) return COLUMN_MAINTENANCE_ID;
        if (hasColumn(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_ID)) return COLUMN_MAINTENANCE_ID;
        if (hasColumn(db, TABLE_MAINTENANCE, "id")) return "id";
        if (hasColumn(db, TABLE_MAINTENANCE, "_id")) return "_id";
        return "rowid";
    }

    public java.util.List<com.lora.cn.ui.model.MaintenanceInfo> getMaintenanceRecords(long createUserId) {
        java.util.List<com.lora.cn.ui.model.MaintenanceInfo> out = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        ensureMaintenanceSchema(db);
        String idCol = resolveMaintenanceIdColumn(db);
        String orderCol = "rowid".equalsIgnoreCase(idCol) ? "rowid" : idCol;
        String selectId = ("rowid".equalsIgnoreCase(idCol) ? "rowid" : idCol) + " AS _mid";
        String sql;
        String[] args = null;
        if (createUserId > 0) {
            sql = "SELECT " + selectId + ", * FROM " + TABLE_MAINTENANCE + " WHERE " + COLUMN_MAINTENANCE_CREATE_USER_ID + "=? ORDER BY " + orderCol + " DESC";
            args = new String[]{String.valueOf(createUserId)};
        } else {
            sql = "SELECT " + selectId + ", * FROM " + TABLE_MAINTENANCE + " ORDER BY " + orderCol + " DESC";
        }
        android.database.Cursor c;
        try {
            c = db.rawQuery(sql, args);
        } catch (Exception e) {
            ensureMaintenanceSchema(db);
            c = db.rawQuery(sql, args);
        }
        while (c.moveToNext()) {
            com.lora.cn.ui.model.MaintenanceInfo mi = new com.lora.cn.ui.model.MaintenanceInfo();
            int midIdx = c.getColumnIndex("_mid");
            mi.setId(midIdx != -1 ? c.getLong(midIdx) : 0L);
            mi.setTerminalId(c.getString(c.getColumnIndex(COLUMN_MAINTENANCE_TERMINAL_ID)));
            mi.setTerminalName(c.getString(c.getColumnIndex(COLUMN_MAINTENANCE_TERMINAL_NAME)));
            mi.setTerminalGroup(c.getString(c.getColumnIndex(COLUMN_MAINTENANCE_TERMINAL_GROUP)));
            mi.setStatus(c.getInt(c.getColumnIndex(COLUMN_MAINTENANCE_STATUS)));
            mi.setContent(c.getString(c.getColumnIndex(COLUMN_MAINTENANCE_CONTENT)));
            mi.setCreateUserId(c.getLong(c.getColumnIndex(COLUMN_MAINTENANCE_CREATE_USER_ID)));
            mi.setCreateUser(c.getString(c.getColumnIndex(COLUMN_MAINTENANCE_CREATE_USER)));
            mi.setCreateTime(c.getString(c.getColumnIndex(COLUMN_MAINTENANCE_CREATE_TIME)));
            mi.setHandleUserId(c.getLong(c.getColumnIndex(COLUMN_MAINTENANCE_HANDLE_USER_ID)));
            mi.setHandleUser(c.getString(c.getColumnIndex(COLUMN_MAINTENANCE_HANDLE_USER)));
            mi.setHandleTime(c.getString(c.getColumnIndex(COLUMN_MAINTENANCE_HANDLE_TIME)));
            try {
                int sflagIdx = c.getColumnIndex(COLUMN_MAINTENANCE_SENT_FLAG);
                if (sflagIdx != -1) {
                    java.lang.reflect.Method m2 = mi.getClass().getMethod("setSentFlag", int.class);
                    m2.invoke(mi, c.getInt(sflagIdx));
                }
            } catch (Exception ignored) {}
            try {
                int stimeIdx = c.getColumnIndex(COLUMN_MAINTENANCE_SENT_TIME);
                if (stimeIdx != -1) {
                    java.lang.reflect.Method m3 = mi.getClass().getMethod("setSentTime", String.class);
                    m3.invoke(mi, c.getString(stimeIdx));
                }
            } catch (Exception ignored) {}
            try {
                String hr = c.getString(c.getColumnIndex("handle_remark"));
                java.lang.reflect.Method m = mi.getClass().getMethod("setHandleRemark", String.class);
                m.invoke(mi, hr);
            } catch (Exception ignored) {}
            out.add(mi);
        }
        c.close();
        return out;
    }

    public java.util.List<com.lora.cn.ui.model.MaintenanceInfo> getMaintenanceRecordsByTerminal(String terminalId, long createUserId) {
        java.util.List<com.lora.cn.ui.model.MaintenanceInfo> out = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        ensureMaintenanceSchema(db);
        String idCol = resolveMaintenanceIdColumn(db);
        String orderCol = "rowid".equalsIgnoreCase(idCol) ? "rowid" : idCol;
        String selectId = ("rowid".equalsIgnoreCase(idCol) ? "rowid" : idCol) + " AS _mid";
        String tid = terminalId == null ? "" : terminalId;
        String sql;
        String[] args;
        if (createUserId > 0) {
            sql = "SELECT " + selectId + ", * FROM " + TABLE_MAINTENANCE +
                    " WHERE " + COLUMN_MAINTENANCE_TERMINAL_ID + "=? AND " + COLUMN_MAINTENANCE_CREATE_USER_ID + "=?" +
                    " ORDER BY " + orderCol + " DESC";
            args = new String[]{tid, String.valueOf(createUserId)};
        } else {
            sql = "SELECT " + selectId + ", * FROM " + TABLE_MAINTENANCE +
                    " WHERE " + COLUMN_MAINTENANCE_TERMINAL_ID + "=?" +
                    " ORDER BY " + orderCol + " DESC";
            args = new String[]{tid};
        }
        android.database.Cursor c;
        try {
            c = db.rawQuery(sql, args);
        } catch (Exception e) {
            ensureMaintenanceSchema(db);
            c = db.rawQuery(sql, args);
        }
        while (c.moveToNext()) {
            com.lora.cn.ui.model.MaintenanceInfo mi = new com.lora.cn.ui.model.MaintenanceInfo();
            int midIdx = c.getColumnIndex("_mid");
            mi.setId(midIdx != -1 ? c.getLong(midIdx) : 0L);
            mi.setTerminalId(c.getString(c.getColumnIndex(COLUMN_MAINTENANCE_TERMINAL_ID)));
            mi.setTerminalName(c.getString(c.getColumnIndex(COLUMN_MAINTENANCE_TERMINAL_NAME)));
            mi.setTerminalGroup(c.getString(c.getColumnIndex(COLUMN_MAINTENANCE_TERMINAL_GROUP)));
            mi.setStatus(c.getInt(c.getColumnIndex(COLUMN_MAINTENANCE_STATUS)));
            mi.setContent(c.getString(c.getColumnIndex(COLUMN_MAINTENANCE_CONTENT)));
            mi.setCreateUserId(c.getLong(c.getColumnIndex(COLUMN_MAINTENANCE_CREATE_USER_ID)));
            mi.setCreateUser(c.getString(c.getColumnIndex(COLUMN_MAINTENANCE_CREATE_USER)));
            mi.setCreateTime(c.getString(c.getColumnIndex(COLUMN_MAINTENANCE_CREATE_TIME)));
            mi.setHandleUserId(c.getLong(c.getColumnIndex(COLUMN_MAINTENANCE_HANDLE_USER_ID)));
            mi.setHandleUser(c.getString(c.getColumnIndex(COLUMN_MAINTENANCE_HANDLE_USER)));
            mi.setHandleTime(c.getString(c.getColumnIndex(COLUMN_MAINTENANCE_HANDLE_TIME)));
            try {
                int sflagIdx = c.getColumnIndex(COLUMN_MAINTENANCE_SENT_FLAG);
                if (sflagIdx != -1) {
                    java.lang.reflect.Method m2 = mi.getClass().getMethod("setSentFlag", int.class);
                    m2.invoke(mi, c.getInt(sflagIdx));
                }
            } catch (Exception ignored) {}
            try {
                int stimeIdx = c.getColumnIndex(COLUMN_MAINTENANCE_SENT_TIME);
                if (stimeIdx != -1) {
                    java.lang.reflect.Method m3 = mi.getClass().getMethod("setSentTime", String.class);
                    m3.invoke(mi, c.getString(stimeIdx));
                }
            } catch (Exception ignored) {}
            try {
                String hr = c.getString(c.getColumnIndex("handle_remark"));
                java.lang.reflect.Method m = mi.getClass().getMethod("setHandleRemark", String.class);
                m.invoke(mi, hr);
            } catch (Exception ignored) {}
            out.add(mi);
        }
        c.close();
        return out;
    }

    public int getMaintenanceCountByTerminal(String terminalId, long createUserId) {
        SQLiteDatabase db = this.getReadableDatabase();
        ensureMaintenanceSchema(db);
        String tid = terminalId == null ? "" : terminalId;
        String sql;
        String[] args;
        if (createUserId > 0) {
            sql = "SELECT COUNT(*) FROM " + TABLE_MAINTENANCE +
                    " WHERE " + COLUMN_MAINTENANCE_TERMINAL_ID + "=? AND " + COLUMN_MAINTENANCE_CREATE_USER_ID + "=?";
            args = new String[]{tid, String.valueOf(createUserId)};
        } else {
            sql = "SELECT COUNT(*) FROM " + TABLE_MAINTENANCE +
                    " WHERE " + COLUMN_MAINTENANCE_TERMINAL_ID + "=?";
            args = new String[]{tid};
        }
        android.database.Cursor c = null;
        try {
            c = db.rawQuery(sql, args);
            if (c.moveToFirst()) return c.getInt(0);
            return 0;
        } catch (Exception e) {
            ensureMaintenanceSchema(db);
            try {
                if (c != null) c.close();
            } catch (Exception ignored) {}
            c = db.rawQuery(sql, args);
            if (c.moveToFirst()) return c.getInt(0);
            return 0;
        } finally {
            try { if (c != null) c.close(); } catch (Exception ignored) {}
        }
    }

    public int updateMaintenanceHandled(long maintenanceId, long handleUserId, String handleUser, String handleTime, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ensureMaintenanceSchema(db);
        String idCol = resolveMaintenanceIdColumn(db);
        ContentValues v = new ContentValues();
        v.put(COLUMN_MAINTENANCE_STATUS, 1);
        v.put(COLUMN_MAINTENANCE_HANDLE_USER_ID, handleUserId);
        v.put(COLUMN_MAINTENANCE_HANDLE_USER, handleUser == null ? "" : handleUser);
        v.put(COLUMN_MAINTENANCE_HANDLE_TIME, handleTime == null ? "" : handleTime);
        v.put("handle_remark", content == null ? "" : content);
        return db.update(TABLE_MAINTENANCE, v, idCol + "=?", new String[]{String.valueOf(maintenanceId)});
    }

    public int updateMaintenanceSent(long maintenanceId, String sentTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ensureMaintenanceSchema(db);
        String idCol = resolveMaintenanceIdColumn(db);
        ContentValues v = new ContentValues();
        v.put(COLUMN_MAINTENANCE_SENT_FLAG, 1);
        v.put(COLUMN_MAINTENANCE_SENT_TIME, sentTime == null ? "" : sentTime);
        return db.update(TABLE_MAINTENANCE, v, idCol + "=?", new String[]{String.valueOf(maintenanceId)});
    }

    public int updateMaintenanceContent(long maintenanceId, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ensureMaintenanceSchema(db);
        String idCol = resolveMaintenanceIdColumn(db);
        ContentValues v = new ContentValues();
        v.put(COLUMN_MAINTENANCE_CONTENT, content == null ? "" : content);
        return db.update(TABLE_MAINTENANCE, v, idCol + "=?", new String[]{String.valueOf(maintenanceId)});
    }

    public int updateMaintenanceCreateTimeAndContent(long maintenanceId, String content, String createTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ensureMaintenanceSchema(db);
        String idCol = resolveMaintenanceIdColumn(db);
        ContentValues v = new ContentValues();
        v.put(COLUMN_MAINTENANCE_CONTENT, content == null ? "" : content);
        v.put(COLUMN_MAINTENANCE_CREATE_TIME, createTime == null ? "" : createTime);
        return db.update(TABLE_MAINTENANCE, v, idCol + "=?", new String[]{String.valueOf(maintenanceId)});
    }

    public int deleteMaintenanceRecord(long maintenanceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ensureMaintenanceSchema(db);
        String idCol = resolveMaintenanceIdColumn(db);
        return db.delete(TABLE_MAINTENANCE, idCol + "=?", new String[]{String.valueOf(maintenanceId)});
    }

    private void ensureMaintenanceSchema(SQLiteDatabase db) {
        if (db == null) return;
        if (maintenanceSchemaEnsured) return;
        synchronized (maintenanceSchemaLock) {
            if (maintenanceSchemaEnsured) return;
        try { db.execSQL(CREATE_TABLE_MAINTENANCE); } catch (Exception ignored) {}
        ensureColumnIfMissing(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_DEVICE_ID, "TEXT NOT NULL DEFAULT ''");
        ensureColumnIfMissing(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_TERMINAL_ID, "TEXT NOT NULL DEFAULT ''");
        ensureColumnIfMissing(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_TERMINAL_NAME, "TEXT");
        ensureColumnIfMissing(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_TERMINAL_GROUP, "TEXT");
        ensureColumnIfMissing(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_STATUS, "INTEGER DEFAULT 0");
        ensureColumnIfMissing(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_CONTENT, "TEXT");
        ensureColumnIfMissing(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_CREATE_USER_ID, "INTEGER DEFAULT 0");
        ensureColumnIfMissing(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_CREATE_USER, "TEXT");
        ensureColumnIfMissing(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_CREATE_TIME, "TEXT");
        ensureColumnIfMissing(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_HANDLE_USER_ID, "INTEGER DEFAULT 0");
        ensureColumnIfMissing(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_HANDLE_USER, "TEXT");
        ensureColumnIfMissing(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_HANDLE_TIME, "TEXT");
        ensureColumnIfMissing(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_SENT_FLAG, "INTEGER DEFAULT 0");
        ensureColumnIfMissing(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_SENT_TIME, "TEXT");
        ensureColumnIfMissing(db, TABLE_MAINTENANCE, "handle_remark", "TEXT");

            try {
                maintenanceHasDeviceIdColumn = hasColumn(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_DEVICE_ID);
                if (maintenanceHasDeviceIdColumn && hasColumn(db, TABLE_MAINTENANCE, COLUMN_MAINTENANCE_TERMINAL_ID)) {
                    String key = "maintenance_device_id_backfilled_v1";
                    boolean done = com.blankj.utilcode.util.SPUtils.getInstance().getBoolean(key, false);
                    if (!done) {
                        db.execSQL(
                                "UPDATE " + TABLE_MAINTENANCE +
                                        " SET " + COLUMN_MAINTENANCE_TERMINAL_ID + "=" + COLUMN_MAINTENANCE_DEVICE_ID +
                                        " WHERE (" + COLUMN_MAINTENANCE_TERMINAL_ID + " IS NULL OR " + COLUMN_MAINTENANCE_TERMINAL_ID + "='')" +
                                        " AND " + COLUMN_MAINTENANCE_DEVICE_ID + " IS NOT NULL AND " + COLUMN_MAINTENANCE_DEVICE_ID + "<>''");
                        db.execSQL(
                                "UPDATE " + TABLE_MAINTENANCE +
                                        " SET " + COLUMN_MAINTENANCE_DEVICE_ID + "=" + COLUMN_MAINTENANCE_TERMINAL_ID +
                                        " WHERE (" + COLUMN_MAINTENANCE_DEVICE_ID + " IS NULL OR " + COLUMN_MAINTENANCE_DEVICE_ID + "='')" +
                                        " AND " + COLUMN_MAINTENANCE_TERMINAL_ID + " IS NOT NULL AND " + COLUMN_MAINTENANCE_TERMINAL_ID + "<>''");
                        com.blankj.utilcode.util.SPUtils.getInstance().put(key, true);
                    }
                }
            } catch (Exception ignored) {}
            maintenanceSchemaEnsured = true;
        }
    }

    private void ensureColumnIfMissing(SQLiteDatabase db, String table, String column, String sqlTypeAndDefault) {
        if (db == null) return;
        if (table == null || table.trim().isEmpty()) return;
        if (column == null || column.trim().isEmpty()) return;
        if (hasColumn(db, table, column)) return;
        try {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + (sqlTypeAndDefault == null ? "TEXT" : sqlTypeAndDefault));
        } catch (Exception ignored) {}
    }

    // 移除插入处理日志，改为仅更新现有异常/离线/低电量日志的处理人、时间、备注

    private boolean hasColumn(SQLiteDatabase db, String table, String column) {
        android.database.Cursor c = null;
        try {
            c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
            int nameIdx = c.getColumnIndex("name");
            while (c.moveToNext()) {
                String n = nameIdx >= 0 ? c.getString(nameIdx) : null;
                if (column.equalsIgnoreCase(n)) return true;
            }
            return false;
        } catch (Exception ignored) {
            return false;
        } finally {
            try { if (c != null) c.close(); } catch (Exception ignored) {}
        }
    }
}
