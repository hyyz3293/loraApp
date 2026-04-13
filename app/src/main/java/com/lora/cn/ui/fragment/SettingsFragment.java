package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.lora.cn.ui.fragment.setting.device.IpConfigFragment;
import com.lora.cn.ui.model.SettingItem;
import com.lora.cn.database.entity.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {

    private RecyclerView terminalSettingRecycle;
    private TerminalSettingAdapter terminalSettingAdapter;
    private View settingsMainContainer;
    private View settingsFragmentContainer;
    private DatabaseManager databaseManager;
    private int currentUserRoleId = -1;
    private final Set<String> grantedPermissions = new HashSet<>();
    private ExecutorService ioExecutor;
    private Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        initViews(view);
        initAsync();
        setupRecyclerView();
        setupBackStackListener();

        return view;
    }

    @Override
    public void onDestroyView() {
        try {
            if (ioExecutor != null) {
                ioExecutor.shutdownNow();
            }
        } catch (Exception ignored) {}
        ioExecutor = null;
        mainHandler = null;
        grantedPermissions.clear();
        super.onDestroyView();
    }

    private void initAsync() {
        databaseManager = DatabaseManager.getInstance(requireContext());
        if (ioExecutor == null) ioExecutor = Executors.newSingleThreadExecutor();
        if (mainHandler == null) mainHandler = new Handler(Looper.getMainLooper());
        if (terminalSettingRecycle != null) terminalSettingRecycle.setEnabled(false);
        loadPermissionsAsync();
    }

    private void initViews(View view) {
        terminalSettingRecycle = view.findViewById(R.id.terminal_setting_recycle);
        settingsMainContainer = view.findViewById(R.id.settings_main_container);
        settingsFragmentContainer = view.findViewById(R.id.settings_fragment_container);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        terminalSettingRecycle.setLayoutManager(layoutManager);

        terminalSettingAdapter = new TerminalSettingAdapter();
        terminalSettingRecycle.setAdapter(terminalSettingAdapter);
        terminalSettingAdapter.setOnItemClickListener((adapter, view, position) -> {
            SettingItem settingItem = terminalSettingAdapter.getItem(position);
            if (settingItem != null) {
                onSettingClick(position, settingItem);
            }
        });
    }

    private void loadPermissionsAsync() {
        if (ioExecutor == null || mainHandler == null) return;
        final android.content.Context appContext = requireContext().getApplicationContext();
        ioExecutor.execute(() -> {
            int roleId = -1;
            Set<String> permissions = new HashSet<>();
            try {
                long currentUserId = SPUtils.getInstance().getLong("current_user_id", -1);
                if (currentUserId != -1) {
                    User user = databaseManager.getUserById(currentUserId);
                    if (user != null) {
                        roleId = (int) user.getRoleId();
                    }
                }
                if (roleId > 0) {
                    String[] permissionCodes = new String[] {
                            "setting_device",
                            "setting_ip",
                            "group_management",
                            "role_management",
                            "user_management",
                            "department_management",
                            "position_management",
                            "setting"
                    };
                    DatabaseManager dm = DatabaseManager.getInstance(appContext);
                    for (String code : permissionCodes) {
                        try {
                            if (dm.hasPermission(roleId, code)) {
                                permissions.add(code);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}

            final int finalRoleId = roleId;
            final List<SettingItem> settingList = buildSettingList(permissions);
            mainHandler.post(() -> {
                if (!isAdded() || terminalSettingAdapter == null) return;
                currentUserRoleId = finalRoleId;
                grantedPermissions.clear();
                grantedPermissions.addAll(permissions);
                terminalSettingAdapter.submitList(settingList);
                terminalSettingAdapter.notifyDataSetChanged();
                if (terminalSettingRecycle != null) terminalSettingRecycle.setEnabled(true);
            });
        });
    }

    private List<SettingItem> buildSettingList(Set<String> permissions) {
        List<SettingItem> settingList = new ArrayList<>();
        if (hasPermission("setting_device")) {
            settingList.add(new SettingItem(R.mipmap.ic_setting1, "设备设置"));
        }
//        if (hasPermission("setting_ip")) {
//            // 直接增加网关IP信息的快捷入口
//            settingList.add(new SettingItem(R.mipmap.ic_setting1, "网关IP信息"));
//            // 新增 MQTT 设置入口（同属网络配置权限）
//            settingList.add(new SettingItem(R.mipmap.ic_setting1, "MQTT设置"));
//        }
        if (hasPermission("group_management")) {
            settingList.add(new SettingItem(R.mipmap.ic_setting2, "分组管理"));
        }
        if (hasPermission("role_management")) {
            settingList.add(new SettingItem(R.mipmap.ic_setting3, "角色管理"));
        }
        if (hasPermission("user_management")) {
            settingList.add(new SettingItem(R.mipmap.ic_setting4, "用户管理"));
        }
        if (hasPermission("department_management")) {
            settingList.add(new SettingItem(R.mipmap.ic_setting5, "科室管理"));
        }
        if (hasPermission("position_management")) {
            settingList.add(new SettingItem(R.mipmap.ic_setting6, "职位管理"));
        }
        //settingList.add(new SettingItem(R.mipmap.ic_setting6, "维护列表"));
//        if (hasPermission("setting")) {
//            settingList.add(new SettingItem(R.mipmap.ic_setting2, "自动返回首页时间"));
//        }
        return settingList;
    }

    private void onSettingClick(int position, SettingItem settingItem) {
        Fragment targetFragment = null;
        String settingName = settingItem.getTitle();
        
        // 根据设置项名称跳转到不同的Fragment
        switch (settingName) {
            case "设备设置":
                if (hasPermission("setting_device")) {
                    targetFragment = DeviceSettingFragment.newInstance();
                }
                break;
            case "网关IP信息":
                if (hasPermission("setting_ip")) {
                    targetFragment = IpConfigFragment.newInstance();
                }
                break;
            case "MQTT设置":
                if (hasPermission("setting_ip")) {
                    targetFragment = com.lora.cn.ui.fragment.setting.device.MqttConfigFragment.newInstance();
                }
                break;
            case "分组管理":
                if (hasPermission("group_management")) {
                    targetFragment = GroupManagementFragment.newInstance();
                }
                break;
            case "角色管理":
                if (hasPermission("role_management")) {
                    targetFragment = RoleManagementFragment.newInstance();
                }
                break;
            case "用户管理":
                if (hasPermission("user_management")) {
                    targetFragment = UserManagementFragment.newInstance();
                }
                break;
            case "科室管理":
                if (hasPermission("department_management")) {
                    targetFragment = DepartmentManagementFragment.newInstance();
                }
                break;
            case "职位管理":
                if (hasPermission("position_management")) {
                    targetFragment = PositionManagementFragment.newInstance();
                }
                break;
            case "维护列表":
                targetFragment = MaintenanceHomeListFragment.newInstance();
                break;
            case "自动返回首页时间":
                if (hasPermission("setting")) {
                    long sec = com.blankj.utilcode.util.SPUtils.getInstance().getLong("home_auto_return_timeout_sec", 60);
                    com.lora.cn.utils.DialogUtils.showNumberEditDialog(getContext(), "自动返回首页时间(秒)", "请输入秒数", String.valueOf(sec), "秒", new com.lora.cn.utils.DialogUtils.OnConfirmListener() {
                        @Override
                        public void onConfirm(String newValue) {
                            try {
                                long v = Long.parseLong(newValue.trim());
                                if (v <= 0) v = 60;
                                com.blankj.utilcode.util.SPUtils.getInstance().put("home_auto_return_timeout_sec", v);
                                android.widget.Toast.makeText(getContext(), "已设置为 " + v + " 秒", android.widget.Toast.LENGTH_SHORT).show();
                            } catch (NumberFormatException e) {
                                android.widget.Toast.makeText(getContext(), "请输入有效的秒数", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
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
        return currentUserRoleId > 0 && grantedPermissions.contains(permissionCode);
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }
}
