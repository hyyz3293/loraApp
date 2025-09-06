package com.lora.cn.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 数据库帮助类
 * 管理分组表和分类表的两层关系结构
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "lora_app.db";
    private static final int DATABASE_VERSION = 6;
    
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
        // 插入示例分组数据
        db.execSQL("INSERT INTO " + TABLE_GROUPS + " (" + COLUMN_GROUP_NAME + ", " + 
                  COLUMN_GROUP_DESCRIPTION + ") VALUES ('设备管理', '管理各类设备相关功能')");
        
        db.execSQL("INSERT INTO " + TABLE_GROUPS + " (" + COLUMN_GROUP_NAME + ", " + 
                  COLUMN_GROUP_DESCRIPTION + ") VALUES ('用户管理', '管理用户账户和权限')");
        
        db.execSQL("INSERT INTO " + TABLE_GROUPS + " (" + COLUMN_GROUP_NAME + ", " + 
                  COLUMN_GROUP_DESCRIPTION + ") VALUES ('系统设置', '系统配置和参数设置')");
        
        // 插入示例分类数据
        // 设备管理分类
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + 
                  COLUMN_CATEGORY_DESCRIPTION + ", " + COLUMN_CATEGORY_GROUP_ID + ") VALUES ('终端设备', '终端设备管理', 1)");
        
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + 
                  COLUMN_CATEGORY_DESCRIPTION + ", " + COLUMN_CATEGORY_GROUP_ID + ") VALUES ('网络设备', '网络设备配置', 1)");
        
        // 用户管理分类
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + 
                  COLUMN_CATEGORY_DESCRIPTION + ", " + COLUMN_CATEGORY_GROUP_ID + ") VALUES ('用户账户', '用户账户管理', 2)");
        
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + 
                  COLUMN_CATEGORY_DESCRIPTION + ", " + COLUMN_CATEGORY_GROUP_ID + ") VALUES ('权限管理', '用户权限设置', 2)");
        
        // 系统设置分类
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + 
                  COLUMN_CATEGORY_DESCRIPTION + ", " + COLUMN_CATEGORY_GROUP_ID + ") VALUES ('基础配置', '系统基础参数配置', 3)");
        
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COLUMN_CATEGORY_NAME + ", " + 
                  COLUMN_CATEGORY_DESCRIPTION + ", " + COLUMN_CATEGORY_GROUP_ID + ") VALUES ('高级设置', '系统高级功能设置', 3)");
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

        db.execSQL("INSERT INTO " + TABLE_PERMISSIONS + " (" +
                COLUMN_PERMISSION_CODE + ", " + COLUMN_PERMISSION_NAME + ", " +
                COLUMN_PERMISSION_CATEGORY + ", " + COLUMN_PERMISSION_DESCRIPTION + ", " +
                COLUMN_PERMISSION_STATUS + ", " + COLUMN_PERMISSION_PARENT_ID + ", " +
                COLUMN_PERMISSION_LEVEL + ", " + COLUMN_PERMISSION_SORT_ORDER +
                ") VALUES ('user_disable', '启用/禁用', 'user', '启用或禁用用户账户', 1, 6, 1, 5)");
    }
}