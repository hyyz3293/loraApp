package com.lora.cn.ui.activity;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.lora.cn.R;

public class VersionInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_version_info);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        android.view.View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
//        TextView tvAppName = findViewById(R.id.tv_app_name);
//        if (tvAppName != null) {
//            tvAppName.setText(getString(R.string.app_name));
//        }
        TextView tvAppVersion = findViewById(R.id.tv_app_version);
        TextView tvTerminalVersion = findViewById(R.id.tv_terminal_version);
        String appVer = "App版本：" + com.lora.cn.BuildConfig.VERSION_NAME + " (" + com.lora.cn.BuildConfig.VERSION_CODE + ")";
        String termVer = "终端版本：" + "Ver-20260203-1.7.7";
        if (tvAppVersion != null) tvAppVersion.setText(appVer);
        if (tvTerminalVersion != null) tvTerminalVersion.setText(termVer);
    }
}
