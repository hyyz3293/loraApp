package com.lora.cn.ui;

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

import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.R;
import com.lora.cn.constant.SpConstant;

public class LoginActivity extends AppCompatActivity {

    private EditText etUser;
    private EditText etPwd;
    private View userDropdownArea;
    private TextView btnLogin;

    private final String[] userOptions = new String[]{"医生A", "护士B", "管理员"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // 隐藏ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

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

    private void showUserPicker() {
        new AlertDialog.Builder(this)
                .setTitle("选择账号")
                .setItems(userOptions, (dialog, which) -> {
                    if (etUser != null) {
                        etUser.setText(userOptions[which]);
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

        boolean pass = false;
        if (user.equals(SpConstant.DEFAULT_USER) && pwd.equals(SpConstant.DEFAULT_PWD)) {
            pass = true;
        }

        if (pass) {
            SPUtils.getInstance().put(SpConstant.IS_LOGIN, true);

            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "账号或者密码错误！", Toast.LENGTH_SHORT).show();
        }

        //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}