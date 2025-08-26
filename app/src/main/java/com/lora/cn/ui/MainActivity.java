package com.lora.cn.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.lora.cn.R;
import com.lora.cn.ui.fragment.TerminalListFragment;
import com.lora.cn.ui.fragment.LogInfoFragment;
import com.lora.cn.ui.fragment.TerminalCheckFragment;
import com.lora.cn.ui.fragment.SettingsFragment;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView btnTerminalList, btnLogInfo, btnTerminalCheck, btnSettings;
    private View indicatorTerminalList, indicatorLogInfo, indicatorTerminalCheck, indicatorSettings;
    private TextView btnLogout;
    
    private Fragment currentFragment;
    private int currentTabIndex = 0; // 0: 终端列表, 1: 日志信息, 2: 清点终端, 3: 设置

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        initListeners();
        
        // 默认显示终端列表
        switchToTab(0);
    }
    
    private void initViews() {
        btnTerminalList = findViewById(R.id.btn_terminal_list);
        btnLogInfo = findViewById(R.id.btn_log_info);
        btnTerminalCheck = findViewById(R.id.btn_terminal_check);
        btnSettings = findViewById(R.id.btn_settings);
        
        indicatorTerminalList = findViewById(R.id.indicator_terminal_list);
        indicatorLogInfo = findViewById(R.id.indicator_log_info);
        indicatorTerminalCheck = findViewById(R.id.indicator_terminal_check);
        indicatorSettings = findViewById(R.id.indicator_settings);
        
        btnLogout = findViewById(R.id.btn_logout);
    }
    
    private void initListeners() {
        btnTerminalList.setOnClickListener(this);
        btnLogInfo.setOnClickListener(this);
        btnTerminalCheck.setOnClickListener(this);
        btnSettings.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_terminal_list) {
            switchToTab(0);
        } else if (id == R.id.btn_log_info) {
            switchToTab(1);
        } else if (id == R.id.btn_terminal_check) {
            switchToTab(2);
        } else if (id == R.id.btn_settings) {
            switchToTab(3);
        } else if (id == R.id.btn_logout) {
            logout();
        }
    }
    
    private void switchToTab(int tabIndex) {
        if (currentTabIndex == tabIndex) {
            return;
        }
        
        currentTabIndex = tabIndex;
        
        // 更新选中指示器
        updateIndicators(tabIndex);
        
        // 切换Fragment
        Fragment fragment = null;
        switch (tabIndex) {
            case 0:
                fragment = new TerminalListFragment();
                break;
            case 1:
                fragment = new LogInfoFragment();
                break;
            case 2:
                fragment = new TerminalCheckFragment();
                break;
            case 3:
                fragment = new SettingsFragment();
                break;
        }
        
        if (fragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (currentFragment != null) {
                transaction.replace(R.id.fragment_container, fragment);
            } else {
                transaction.add(R.id.fragment_container, fragment);
            }
            transaction.commit();
            currentFragment = fragment;
        }
    }
    
    private void updateIndicators(int selectedIndex) {
        // 重置所有指示器
        indicatorTerminalList.setVisibility(View.INVISIBLE);
        indicatorLogInfo.setVisibility(View.INVISIBLE);
        indicatorTerminalCheck.setVisibility(View.INVISIBLE);
        indicatorSettings.setVisibility(View.INVISIBLE);
        
        // 显示选中的指示器
        switch (selectedIndex) {
            case 0:
                indicatorTerminalList.setVisibility(View.VISIBLE);
                break;
            case 1:
                indicatorLogInfo.setVisibility(View.VISIBLE);
                break;
            case 2:
                indicatorTerminalCheck.setVisibility(View.VISIBLE);
                break;
            case 3:
                indicatorSettings.setVisibility(View.VISIBLE);
                break;
        }
    }
    
    private void logout() {
        // 跳转到登录页面
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}