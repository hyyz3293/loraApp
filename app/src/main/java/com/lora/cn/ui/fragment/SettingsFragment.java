package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.ui.adapter.TerminalSettingAdapter;
import com.lora.cn.ui.fragment.setting.PositionManagementFragment;
import com.lora.cn.ui.fragment.setting.DepartmentManagementFragment;
import com.lora.cn.ui.fragment.setting.DeviceSettingFragment;
import com.lora.cn.ui.fragment.setting.GroupManagementFragment;
import com.lora.cn.ui.fragment.setting.RoleManagementFragment;
import com.lora.cn.ui.fragment.setting.UserManagementFragment;
import com.lora.cn.ui.model.SettingItem;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    private RecyclerView terminalSettingRecycle;
    private TerminalSettingAdapter terminalSettingAdapter;
    private View settingsMainContainer;
    private View settingsFragmentContainer;
    private DatabaseManager databaseManager;
    private int currentUserRoleId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        initDatabase();
        initViews(view);
        initSettingData();
        setupBackStackListener();
        
        return view;
    }
    
    private void initDatabase() {
        databaseManager = DatabaseManager.getInstance(requireContext());
        // 获取当前登录用户的角色ID
        currentUserRoleId = SPUtils.getInstance().getInt("current_user_role_id", -1);
    }

    private void initViews(View view) {
        terminalSettingRecycle = view.findViewById(R.id.terminal_setting_recycle);
        settingsMainContainer = view.findViewById(R.id.settings_main_container);
        settingsFragmentContainer = view.findViewById(R.id.settings_fragment_container);
    }

    private void initSettingData() {
        // 创建设置项数据，根据权限过滤
        List<SettingItem> settingList = new ArrayList<>();
        
        // 检查各项权限并添加对应设置项
        if (hasPermission("DEVICE_SETTING")) {
            settingList.add(new SettingItem(R.mipmap.ic_setting1, "设备设置"));
        }
        if (hasPermission("GROUP_MANAGEMENT")) {
            settingList.add(new SettingItem(R.mipmap.ic_setting2, "分组管理"));
        }
        if (hasPermission("ROLE_MANAGEMENT")) {
            settingList.add(new SettingItem(R.mipmap.ic_setting3, "角色管理"));
        }
        if (hasPermission("USER_MANAGEMENT")) {
            settingList.add(new SettingItem(R.mipmap.ic_setting4, "用户管理"));
        }
        if (hasPermission("DEPARTMENT_MANAGEMENT")) {
            settingList.add(new SettingItem(R.mipmap.ic_setting5, "科室管理"));
        }
        if (hasPermission("POSITION_MANAGEMENT")) {
            settingList.add(new SettingItem(R.mipmap.ic_setting6, "职位管理"));
        }


        // 设置RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext()); // 3列网格布局
        terminalSettingRecycle.setLayoutManager(layoutManager);
        
        terminalSettingAdapter = new TerminalSettingAdapter();
        terminalSettingRecycle.setAdapter(terminalSettingAdapter);
        
        // 设置点击事件监听器
        terminalSettingAdapter.setOnItemClickListener((adapter, view, position) -> {
            SettingItem settingItem = settingList.get(position);
            onSettingClick(position, settingItem);
        });
        
        // 提交数据到适配器
        terminalSettingAdapter.submitList(settingList);
    }

    private void onSettingClick(int position, SettingItem settingItem) {
        Fragment targetFragment = null;
        String settingName = settingItem.getTitle();
        
        // 根据设置项名称跳转到不同的Fragment
        switch (settingName) {
            case "设备设置":
                if (hasPermission("DEVICE_SETTING")) {
                    targetFragment = DeviceSettingFragment.newInstance();
                }
                break;
            case "分组管理":
                if (hasPermission("GROUP_MANAGEMENT")) {
                    targetFragment = GroupManagementFragment.newInstance();
                }
                break;
            case "角色管理":
                if (hasPermission("ROLE_MANAGEMENT")) {
                    targetFragment = RoleManagementFragment.newInstance();
                }
                break;
            case "用户管理":
                if (hasPermission("USER_MANAGEMENT")) {
                    targetFragment = UserManagementFragment.newInstance();
                }
                break;
            case "科室管理":
                if (hasPermission("DEPARTMENT_MANAGEMENT")) {
                    targetFragment = DepartmentManagementFragment.newInstance();
                }
                break;
            case "职位管理":
                if (hasPermission("POSITION_MANAGEMENT")) {
                    targetFragment = PositionManagementFragment.newInstance();
                }
                break;
        }
        
        if (targetFragment != null) {
            // 使用子Fragment管理器进行导航，在当前Fragment内部切换
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.replace(R.id.settings_fragment_container, targetFragment);
            transaction.addToBackStack(null);
            transaction.commit();
            
            // 隐藏主设置页面，显示子Fragment容器
            settingsMainContainer.setVisibility(View.GONE);
            settingsFragmentContainer.setVisibility(View.VISIBLE);
        }
    }

    private void setupBackStackListener() {
        // 监听子Fragment回退栈变化
        getChildFragmentManager().addOnBackStackChangedListener(() -> {
            if (getChildFragmentManager().getBackStackEntryCount() == 0) {
                // 回退栈为空，显示主设置页面
                settingsMainContainer.setVisibility(View.VISIBLE);
                settingsFragmentContainer.setVisibility(View.GONE);
            }
        });
    }
    
    /**
     * 检查当前用户是否有指定权限
     * @param permissionCode 权限代码
     * @return 是否有权限
     */
    private boolean hasPermission(String permissionCode) {
        if (currentUserRoleId == -1) {
            return false;
        }
        return databaseManager.hasPermission(currentUserRoleId, permissionCode);
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }
}