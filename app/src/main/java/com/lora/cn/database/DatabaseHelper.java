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
    private static final int DATABASE_VERSION = 8;
    
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
        COLUMN_TERMINAL_NAME + " TEXT NOT NULL, " +
        COLUMN_TERMINAL_STATUS + " TEXT DEFAULT '在线', " +
        COLUMN_TERMINAL_SIGNAL_STRENGTH + " INTEGER DEFAULT 0, " +
        COLUMN_TERMINAL_DEPARTMENT + " TEXT, " +
        COLUMN_TERMINAL_LOCATION + " TEXT, " +
        COLUMN_TERMINAL_CREATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_TERMINAL_UPDATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
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
                terminal.setTerminalName(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_NAME)));
                terminal.setStatus(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_STATUS)));
                terminal.setSignalStrength(cursor.getInt(cursor.getColumnIndex(COLUMN_TERMINAL_SIGNAL_STRENGTH)));
                terminal.setDepartment(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_DEPARTMENT)));
                terminal.setLocation(cursor.getString(cursor.getColumnIndex(COLUMN_TERMINAL_LOCATION)));
                
                terminals.add(terminal);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return terminals;
    }
}