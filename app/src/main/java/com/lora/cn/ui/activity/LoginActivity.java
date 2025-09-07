package com.lora.cn.ui.activity;

import android.os.Bundle;

import android.content.Intent;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.R;
import com.lora.cn.constant.SpConstant;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.User;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private EditText etUser;
    private EditText etPwd;
    private View userDropdownArea;
    private TextView btnLogin;

    private DatabaseManager databaseManager;
    private List<User> allUsers;
    private String[] userOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // 隐藏ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 初始化数据库管理器
        databaseManager = DatabaseManager.getInstance(this);
        loadUsers();

        // 绑定视图
        etUser = findViewById(R.id.login_user);
        etPwd = findViewById(R.id.login_pwd);
        userDropdownArea = findViewById(R.id.login_user_dw);
        btnLogin = findViewById(R.id.login);

        // 密码输入样式
        if (etPwd != null) {
            etPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etPwd.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }

        // 账号下拉选择
        if (userDropdownArea != null) {
            userDropdownArea.setOnClickListener(v -> showUserPicker());
        }
        if (etUser != null) {
            etUser.setOnClickListener(v -> showUserPicker());
        }

        // 登录按钮
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> attemptLogin());
        }
    }

    private void loadUsers() {
        try {
            allUsers = databaseManager.getActiveUsers();
            List<String> userNames = new ArrayList<>();
            for (User user : allUsers) {
                userNames.add(user.getUserName() + " (" + user.getUserAccount() + ")");
            }
            userOptions = userNames.toArray(new String[0]);
        } catch (Exception e) {
            // 如果数据库中没有用户，使用默认选项
            userOptions = new String[]{"请先在用户管理中添加用户"};
            allUsers = new ArrayList<>();
        }
    }

    private void showUserPicker() {
        new AlertDialog.Builder(this)
                .setTitle("选择账号")
                .setItems(userOptions, (dialog, which) -> {
                    if (etUser != null) {
                        if (allUsers != null && which < allUsers.size()) {
                            User selectedUser = allUsers.get(which);
                            etUser.setText(selectedUser.getUserAccount());
                        } else {
                            etUser.setText(userOptions[which]);
                        }
                    }
                })
                .show();
    }

    private void attemptLogin() {
        String user = etUser != null ? etUser.getText().toString().trim() : "";
        String pwd = etPwd != null ? etPwd.getText().toString().trim() : "";

        if (user.isEmpty()) {
            Toast.makeText(this, "请输入账号", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pwd.isEmpty()) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 使用数据库验证用户
        try {
            User authenticatedUser = databaseManager.authenticateUser(user, pwd);
            if (authenticatedUser != null) {
                // 登录成功，保存登录状态和用户信息
                SPUtils.getInstance().put(SpConstant.IS_LOGIN, true);
                SPUtils.getInstance().put("current_user_id", authenticatedUser.getUserId());
                SPUtils.getInstance().put("current_user_name", authenticatedUser.getUserName());
                SPUtils.getInstance().put("current_user_account", authenticatedUser.getUserAccount());
                
                Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "账号或者密码错误！", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            LogUtils.e("登录验证失败：" + e.getMessage());
            Toast.makeText(this, "登录验证失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}