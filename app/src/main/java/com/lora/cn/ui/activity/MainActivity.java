package com.lora.cn.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.lora.cn.R;
import com.lora.cn.ui.adapter.MainPagerAdapter;
import com.lora.cn.ui.adapter.MenuTabAdapter;
import com.lora.cn.ui.model.MenuTab;
import com.lora.cn.ui.fragment.setting.user.UserInfoFragment;
import com.lora.cn.ui.fragment.setting.user.UserInfoEditFragment;
import com.lora.cn.ui.fragment.setting.user.PasswordChangeFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvMenuTabs;
    private ViewPager2 viewPager;
    private MenuTabAdapter menuTabAdapter;
    private MainPagerAdapter pagerAdapter;
    private List<MenuTab> menuTabs;
    private ImageView btnLogout;
    private TextView tvUserName;
    private FrameLayout fragmentUserInfoContainer;
    private UserInfoFragment userInfoFragment;
    
    private int currentTabIndex = 0;
    private boolean isUserInfoVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        initMenuTabs();
        initViewPager();
        initListeners();
        
        // 默认显示终端列表
        menuTabs.get(0).setSelected(true);
        menuTabAdapter.notifyDataSetChanged();
    }

    private void initViews() {
        rvMenuTabs = findViewById(R.id.rv_menu_tabs);
        viewPager = findViewById(R.id.view_pager);
        btnLogout = findViewById(R.id.logout);
        tvUserName = findViewById(R.id.tv_user_name);
        fragmentUserInfoContainer = findViewById(R.id.fragment_user_info_container);
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

    private void initViewPager() {
        pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        // 设置ViewPager2的页面切换监听
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTabSelection(position);
            }
        });
    }

    private void initListeners() {
        menuTabAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener<MenuTab>() {
            @Override
            public void onClick(@NonNull BaseQuickAdapter<MenuTab, ?> baseQuickAdapter, @NonNull View view, int position) {
                switchToTab(position);
            }
        });
        
        btnLogout.setOnClickListener(v -> logout());
        
        tvUserName.setOnClickListener(v -> toggleUserInfo());
    }

    private void switchToTab(int tabIndex) {
        if (currentTabIndex == tabIndex) {
            return;
        }
        
        // 切换ViewPager2到指定页面
        viewPager.setCurrentItem(tabIndex, false); // false表示无动画切换
    }

    private void updateTabSelection(int tabIndex) {
        currentTabIndex = tabIndex;
        
        // 更新标签选中状态
        for (int i = 0; i < menuTabs.size(); i++) {
            menuTabs.get(i).setSelected(i == tabIndex);
        }
        
        menuTabAdapter.submitList(menuTabs);
        menuTabAdapter.notifyDataSetChanged();
    }

    private void toggleUserInfo() {
        if (isUserInfoVisible) {
            hideUserInfo();
        } else {
            showUserInfo();
        }
    }

    private void showUserInfo() {
        if (userInfoFragment == null) {
            userInfoFragment = new UserInfoFragment();
            userInfoFragment.setOnUserInfoActionListener(new UserInfoFragment.OnUserInfoActionListener() {
                @Override
                public void onCloseUserInfo() {
                    hideUserInfo();
                }

                @Override
                public void onEditProfile() {
                    // 跳转到用户信息编辑Fragment，保留顶部
                    UserInfoEditFragment editFragment = new UserInfoEditFragment();
                    editFragment.setOnUserInfoEditListener(new UserInfoEditFragment.OnUserInfoEditListener() {
                        @Override
                        public void onCancelEdit() {
                            // 返回到UserInfoFragment
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_user_info_container, userInfoFragment)
                                    .commit();
                        }

                        @Override
                        public void onSaveUserInfo(UserInfoEditFragment.UserInfo userInfo) {
                            // 保存用户信息逻辑
                            // 返回到UserInfoFragment
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_user_info_container, userInfoFragment)
                                    .commit();
                        }
                    });
                    
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_user_info_container, editFragment)
                            .commit();
                }

                @Override
                public void onChangePassword() {
                    // 跳转到密码修改Fragment，保留顶部
                    PasswordChangeFragment passwordFragment = new PasswordChangeFragment();
                    passwordFragment.setOnPasswordChangeListener(new PasswordChangeFragment.OnPasswordChangeListener() {
                        @Override
                        public void onCancelPasswordChange() {
                            // 返回到UserInfoFragment
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_user_info_container, userInfoFragment)
                                    .commit();
                        }

                        @Override
                        public void onSavePassword(String oldPassword, String newPassword) {
                            // 保存密码逻辑
                            // 返回到UserInfoFragment
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_user_info_container, userInfoFragment)
                                    .commit();
                        }
                    });
                    
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_user_info_container, passwordFragment)
                            .commit();
                }
            });
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_user_info_container, userInfoFragment)
                .commit();

        fragmentUserInfoContainer.setVisibility(View.VISIBLE);
        rvMenuTabs.setVisibility(View.INVISIBLE);
        viewPager.setVisibility(View.GONE);
        isUserInfoVisible = true;
    }

    private void hideUserInfo() {
        fragmentUserInfoContainer.setVisibility(View.GONE);
        viewPager.setVisibility(View.VISIBLE);
        rvMenuTabs.setVisibility(View.VISIBLE);
        isUserInfoVisible = false;

        if (userInfoFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(userInfoFragment)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (isUserInfoVisible) {
            hideUserInfo();
        } else {
            super.onBackPressed();
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