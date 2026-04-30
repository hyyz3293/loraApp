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
        String appVer = "App版本：" + com.lora.cn.BuildConfig.VERSION_NAME + " (" + com.lora.cn.BuildConfig.VERSION_CODE +  ")";
        com.blankj.utilcode.util.SPUtils sp = com.blankj.utilcode.util.SPUtils.getInstance();
        String lastDev = sp.getString("terminal_firmware_version_last_device_id", "");
        String fwRaw = "";
        if (lastDev != null && !lastDev.trim().isEmpty()) {
            fwRaw = sp.getString("terminal_firmware_version_" + lastDev, "");
        }
        if (fwRaw == null || fwRaw.trim().isEmpty()) {
            fwRaw = sp.getString("terminal_firmware_version", "");
        }
        String fw = com.lora.cn.utils.LoRaFrameParser.normalizeFirmwareVersionString(fwRaw);
        if (tvAppVersion != null) tvAppVersion.setText(appVer);
        if (tvTerminalVersion != null) {
            if (fw.isEmpty()) {
                tvTerminalVersion.setText("");
                tvTerminalVersion.setVisibility(android.view.View.GONE);
            } else {
                String suffix = (lastDev != null && !lastDev.trim().isEmpty()) ? ("（最近设备 " + lastDev + "）") : "";
                tvTerminalVersion.setText("终端版本" + suffix + "：" + fw);
                tvTerminalVersion.setVisibility(android.view.View.VISIBLE);
            }
        }
    }
}
