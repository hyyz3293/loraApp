package com.lora.cn;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseHelper;
import java.lang.reflect.Method;

/**
 * 数据库调试Activity
 * 用于检查默认管理员用户和角色的创建状态
 */
public class DatabaseDebugActivity extends Activity {
    private static final String TAG = "DatabaseDebug";
    private TextView debugInfoView;
    private StringBuilder debugInfo;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_debug);
        
        Button checkButton = findViewById(R.id.btn_check_database);
        Button recreateButton = findViewById(R.id.btn_recreate_admin);
        debugInfoView = findViewById(R.id.tv_debug_info);
        
        checkButton.setOnClickListener(v -> checkDatabaseStatus());
        recreateButton.setOnClickListener(v -> recreateAdminData());
    }
    
    private void checkDatabaseStatus() {
        debugInfo = new StringBuilder();
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        addDebugInfo("=== 数据库状态检查 ===");
        addDebugInfo("数据库版本: " + db.getVersion());
        
        // 1. 检查权限表
        checkPermissions(db);
        
        // 2. 检查角色表
        checkRoles(db);
        
        // 3. 检查角色权限关联表
        checkRolePermissions(db);
        
        // 4. 检查用户表
        checkUsers(db);
        
        db.close();
        debugInfoView.setText(debugInfo.toString());
    }
    
    private void recreateAdminData() {
        debugInfo = new StringBuilder();
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        addDebugInfo("=== 重新创建管理员数据 ===");
        recreateDefaultData(db);
        
        db.close();
        debugInfoView.setText(debugInfo.toString());
    }
    
    private void checkPermissions(SQLiteDatabase db) {
        addDebugInfo("\n--- 权限表检查 ---");
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM permissions", null);
        if (cursor.moveToFirst()) {
            int count = cursor.getInt(0);
            addDebugInfo("权限总数: " + count);
        }
        cursor.close();
        
        // 显示前3个权限
        cursor = db.rawQuery("SELECT permission_id, permission_code, permission_name FROM permissions LIMIT 3", null);
        while (cursor.moveToNext()) {
            addDebugInfo("权限: ID=" + cursor.getInt(0) + ", 代码=" + cursor.getString(1) + ", 名称=" + cursor.getString(2));
        }
        cursor.close();
    }
    
    private void checkRoles(SQLiteDatabase db) {
        addDebugInfo("\n--- 角色表检查 ---");
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM roles", null);
        if (cursor.moveToFirst()) {
            int count = cursor.getInt(0);
            addDebugInfo("角色总数: " + count);
        }
        cursor.close();
        
        // 检查所有角色
        cursor = db.rawQuery("SELECT role_id, role_name, description FROM roles", null);
        while (cursor.moveToNext()) {
            addDebugInfo("角色: ID=" + cursor.getInt(0) + ", 名称=" + cursor.getString(1) + ", 描述=" + cursor.getString(2));
        }
        cursor.close();
        
        // 特别检查管理员角色
        cursor = db.rawQuery("SELECT role_id FROM roles WHERE role_name = '管理员'", null);
        if (cursor.moveToFirst()) {
            addDebugInfo("✅ 管理员角色存在，ID: " + cursor.getInt(0));
        } else {
            addDebugInfo("❌ 管理员角色不存在!");
        }
        cursor.close();
    }
    
    private void checkRolePermissions(SQLiteDatabase db) {
        addDebugInfo("\n--- 角色权限关联表检查 ---");
        
        // 获取管理员角色ID
        Cursor cursor = db.rawQuery("SELECT role_id FROM roles WHERE role_name = '管理员'", null);
        if (cursor.moveToFirst()) {
            long adminRoleId = cursor.getLong(0);
            cursor.close();
            
            cursor = db.rawQuery("SELECT COUNT(*) FROM role_permissions WHERE role_id = ?", new String[]{String.valueOf(adminRoleId)});
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                addDebugInfo("管理员角色权限数量: " + count);
            }
            cursor.close();
        } else {
            cursor.close();
            addDebugInfo("无法检查管理员角色权限，角色不存在");
        }
    }
    
    private void checkUsers(SQLiteDatabase db) {
        addDebugInfo("\n--- 用户表检查 ---");
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM users", null);
        if (cursor.moveToFirst()) {
            int count = cursor.getInt(0);
            addDebugInfo("用户总数: " + count);
        }
        cursor.close();
        
        // 显示所有用户
        cursor = db.rawQuery("SELECT user_id, user_name, user_account, role_id, status FROM users", null);
        while (cursor.moveToNext()) {
            addDebugInfo("用户: ID=" + cursor.getInt(0) + ", 姓名=" + cursor.getString(1) + ", 账号=" + cursor.getString(2) + ", 角色ID=" + cursor.getInt(3) + ", 状态=" + cursor.getInt(4));
        }
        cursor.close();
        
        // 特别检查admin用户
        cursor = db.rawQuery("SELECT user_id, role_id FROM users WHERE user_account = 'admin'", null);
        if (cursor.moveToFirst()) {
            addDebugInfo("✅ admin用户存在，ID: " + cursor.getInt(0) + ", 角色ID: " + cursor.getInt(1));
        } else {
            addDebugInfo("❌ admin用户不存在!");
        }
        cursor.close();
    }
    
    private void recreateDefaultData(SQLiteDatabase db) {
        try {
            // 删除现有的admin用户（如果存在）
            db.execSQL("DELETE FROM users WHERE user_account = 'admin'");
            addDebugInfo("已删除现有admin用户");
            
            // 删除现有的管理员角色（如果存在）
            db.execSQL("DELETE FROM role_permissions WHERE role_id IN (SELECT role_id FROM roles WHERE role_name = '管理员')");
            db.execSQL("DELETE FROM roles WHERE role_name = '管理员'");
            addDebugInfo("已删除现有管理员角色");
            
            // 重新创建管理员角色和用户
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
            
            // 使用反射调用私有方法
            java.lang.reflect.Method insertPermissionsMethod = DatabaseHelper.class.getDeclaredMethod("insertInitialPermissions", SQLiteDatabase.class);
            insertPermissionsMethod.setAccessible(true);
            insertPermissionsMethod.invoke(dbHelper, db);
            addDebugInfo("重新插入权限数据");
            
            java.lang.reflect.Method insertRoleMethod = DatabaseHelper.class.getDeclaredMethod("insertDefaultAdminRole", SQLiteDatabase.class);
            insertRoleMethod.setAccessible(true);
            insertRoleMethod.invoke(dbHelper, db);
            addDebugInfo("重新创建管理员角色");
            
            java.lang.reflect.Method insertUserMethod = DatabaseHelper.class.getDeclaredMethod("insertDefaultAdminUser", SQLiteDatabase.class);
            insertUserMethod.setAccessible(true);
            insertUserMethod.invoke(dbHelper, db);
            addDebugInfo("重新创建admin用户");
            
            // 再次检查
            addDebugInfo("\n--- 重新创建后检查 ---");
            Cursor cursor = db.rawQuery("SELECT user_id, role_id FROM users WHERE user_account = 'admin'", null);
            if (cursor.moveToFirst()) {
                addDebugInfo("✅ admin用户重新创建成功，ID: " + cursor.getInt(0) + ", 角色ID: " + cursor.getInt(1));
            } else {
                addDebugInfo("❌ admin用户重新创建失败!");
            }
            cursor.close();
            
        } catch (Exception e) {
            addDebugInfo("重新创建过程中出错: " + e.getMessage());
            Log.e(TAG, "重新创建默认数据失败", e);
        }
    }
    
    private void addDebugInfo(String info) {
        debugInfo.append(info).append("\n");
        Log.d(TAG, info);
    }
}