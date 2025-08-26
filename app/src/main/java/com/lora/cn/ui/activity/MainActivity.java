package com.lora.cn.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.lora.cn.R;
import com.lora.cn.ui.fragment.TerminalListFragment;
import com.lora.cn.ui.fragment.LogInfoFragment;
import com.lora.cn.ui.fragment.TerminalCheckFragment;
import com.lora.cn.ui.fragment.SettingsFragment;
import com.lora.cn.ui.adapter.MenuTabAdapter;
import com.lora.cn.ui.model.MenuTab;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvMenuTabs;
    private MenuTabAdapter menuTabAdapter;
    private List<MenuTab> menuTabs;
    private ImageView btnLogout;
    
    private Fragment currentFragment;
    private int currentTabIndex = 0; // 0: 终端列表, 1: 日志信息, 2: 清点终端, 3: 设置

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        initMenuTabs();
        initListeners();
        
        // 默认显示终端列表
        menuTabs.get(0).setSelected(true);
        menuTabAdapter.notifyDataSetChanged();
        switchToTab(0);
    }
    
    private void initViews() {
        rvMenuTabs = findViewById(R.id.rv_menu_tabs);
        btnLogout = findViewById(R.id.logout);
    }
    
    private void initMenuTabs() {
        menuTabs = new ArrayList<>();
        menuTabs.add(new MenuTab("终端列表", 0));
        menuTabs.add(new MenuTab("日志信息", 1));
        menuTabs.add(new MenuTab("清点终端", 2));
        menuTabs.add(new MenuTab("设置", 3));
        
        // 设置RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvMenuTabs.setLayoutManager(layoutManager);
        
        menuTabAdapter = new MenuTabAdapter();
        menuTabAdapter.submitList(menuTabs);
        rvMenuTabs.setAdapter(menuTabAdapter);
    }
    
    private void initListeners() {
//        menuTabAdapter.setOnTabClickListener((position, menuTab) -> {
//            switchToTab(position);
//        });
        menuTabAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener<MenuTab>() {
            @Override
            public void onClick(@NonNull BaseQuickAdapter<MenuTab, ?> baseQuickAdapter, @NonNull View view, int position) {
                switchToTab(position);
            }
        });
        
        btnLogout.setOnClickListener(v -> logout());
    }

    private void switchToTab(int tabIndex) {
        if (currentTabIndex == tabIndex) {
            return;
        }
        
        currentTabIndex = tabIndex;

        for (int i = 0; i < menuTabs.size(); i++) {
            menuTabs.get(i).setSelected(false);
            if (i == currentTabIndex) {
                menuTabs.get(i).setSelected(true);
            }
        }

        menuTabAdapter.submitList(menuTabs);
        menuTabAdapter.notifyDataSetChanged();

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

    private void logout() {
        // 跳转到登录页面
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}