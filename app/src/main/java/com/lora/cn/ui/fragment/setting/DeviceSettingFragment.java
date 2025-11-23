package com.lora.cn.ui.fragment.setting;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.User;
import com.lora.cn.ui.adapter.TerminalSettingDeviceAdapter;
import com.lora.cn.ui.fragment.setting.device.IpConfigFragment;
import com.lora.cn.ui.fragment.setting.device.WifiSettingFragment;
import com.lora.cn.ui.model.SettingItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DeviceSettingFragment extends Fragment {

    private RecyclerView terminalSettingRecycle;
    private TerminalSettingDeviceAdapter terminalSettingAdapter;
    private TextView mTvBack;
    
    // 权限相关
    private DatabaseManager databaseManager;
    private int currentUserRoleId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_setting, container, false);

        // 初始化数据库管理器
        databaseManager = DatabaseManager.getInstance(requireContext());
        
        // 初始化用户角色ID
        long userId = SPUtils.getInstance().getLong("current_user_id", -1);
        if (userId != -1) {
            User user = databaseManager.getUserById(userId);
            if (user != null) {
                currentUserRoleId = (int)user.getRoleId();
            }
        }
        
        initViews(view);
        
        // 检查设备设置权限
        if (hasPermission("setting_device")) {
            initSettingData();
        } else {
            Toast.makeText(requireContext(), "您没有设备设置的权限", Toast.LENGTH_SHORT).show();
        }
        
        initListener();

        return view;
    }

    private void initViews(View view) {
        terminalSettingRecycle = view.findViewById(R.id.terminal_recycle_device);
        mTvBack = view.findViewById(R.id.back);
    }

    private void initListener() {
        mTvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            }
        });
    }

    private void initSettingData() {
        // 创建设置项数据（按权限动态添加）
        List<SettingItem> settingList = new ArrayList<>();
        if (hasPermission("setting_sound")) {
            settingList.add(new SettingItem("音量设置", 1, 0));
        }
        if (hasPermission("setting_wifi")) {
            settingList.add(new SettingItem("WiFIi连接", 0, 1));
        }
        if (hasPermission("setting_ip")) {
            settingList.add(new SettingItem("IP配置", 0, 2));
        }
        if (hasPermission("setting_count")) {
            settingList.add(new SettingItem("清点次数(非管理员角色)", 2, 3));
        }
        if (hasPermission("setting_low_battery")) {
            settingList.add(new SettingItem("低电量报警值", 2, 4, "20%"));
        }
        if (hasPermission("setting_home_return")) {
            settingList.add(new SettingItem("返回首页时间", 2, 5, "60"));
        }

        // 设置RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext()); // 3列网格布局
        terminalSettingRecycle.setLayoutManager(layoutManager);

        terminalSettingAdapter = new TerminalSettingDeviceAdapter();
        terminalSettingRecycle.setAdapter(terminalSettingAdapter);

        // 设置点击事件监听器（按索引校验具体权限）
        terminalSettingAdapter.setOnItemClickListener((adapter, view, position) -> {
            SettingItem settingItem = settingList.get(position);
            int idx = settingItem.getIndex();
            String permCode = null;
            switch (idx) {
                case 0: permCode = "setting_sound"; break;
                case 1: permCode = "setting_wifi"; break;
                case 2: permCode = "setting_ip"; break;
                case 3: permCode = "setting_count"; break;
                case 4: permCode = "setting_low_battery"; break;
                case 5: permCode = "setting_home_return"; break;
            }
            if (permCode != null && hasPermission(permCode)) {
                onSettingClick(idx, settingItem);
            } else {
                Toast.makeText(requireContext(), "您没有该设置项的权限", Toast.LENGTH_SHORT).show();
            }
        });

        // 提交数据到适配器
        terminalSettingAdapter.submitList(settingList);
    }

    private void onSettingClick(int position, SettingItem settingItem) {
        Fragment targetFragment = null;
        // 根据位置跳转到不同的Fragment
        switch (position) {
            case 1: //WIFI
                PermissionUtils.permission(Manifest.permission.ACCESS_FINE_LOCATION)
                        .callback(new PermissionUtils.FullCallback() {
                            @Override
                            public void onGranted(@NonNull List<String> granted) {
                                Fragment  targetFragment = WifiSettingFragment.newInstance();
                                if (targetFragment != null) {
                                    // 使用父Fragment管理器进行导航
                                    FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                                    transaction.replace(R.id.settings_fragment_container, targetFragment);
                                    transaction.addToBackStack(null); // 添加到回退栈，支持返回
                                    transaction.commit();
                                }
                            }

                            @Override
                            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {

                            }
                        }).request();

                break;
            case 2: //IP
                targetFragment = IpConfigFragment.newInstance();
                if (targetFragment != null) {
                    // 使用父Fragment管理器进行导航
                    FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                    transaction.replace(R.id.settings_fragment_container, targetFragment);
                    transaction.addToBackStack(null); // 添加到回退栈，支持返回
                    transaction.commit();
                }
                break;
        }


    }
    
    /**
     * 检查当前用户是否有指定权限
     */
    private boolean hasPermission(String permissionCode) {
        if (currentUserRoleId == -1) {
            return false;
        }
        return databaseManager.hasPermission(currentUserRoleId, permissionCode);
    }
    
    public static DeviceSettingFragment newInstance() {
        return new DeviceSettingFragment();
    }
}