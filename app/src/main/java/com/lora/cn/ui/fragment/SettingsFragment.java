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

import com.lora.cn.R;
import com.lora.cn.ui.adapter.TerminalSettingAdapter;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        initViews(view);
        initSettingData();
        
        return view;
    }

    private void initViews(View view) {
        terminalSettingRecycle = view.findViewById(R.id.terminal_setting_recycle);
    }

    private void initSettingData() {
        // 创建设置项数据
        List<SettingItem> settingList = new ArrayList<>();
        settingList.add(new SettingItem(R.mipmap.ic_setting1, "设备设置"));
        settingList.add(new SettingItem(R.mipmap.ic_setting2, "分组管理"));
        settingList.add(new SettingItem(R.mipmap.ic_setting3, "角色管理"));
        settingList.add(new SettingItem(R.mipmap.ic_setting4, "用户管理"));
        settingList.add(new SettingItem(R.mipmap.ic_setting5, "科室管理"));
        settingList.add(new SettingItem(R.mipmap.ic_setting6, "科室管理"));

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
        
        // 根据位置跳转到不同的Fragment
        switch (position) {
            case 0: // 设备设置
                targetFragment = DeviceSettingFragment.newInstance();
                break;
            case 1: // 分组管理
                targetFragment = GroupManagementFragment.newInstance();
                break;
            case 2: // 角色管理
                targetFragment = RoleManagementFragment.newInstance();
                break;
            case 3: // 用户管理
                targetFragment = UserManagementFragment.newInstance();
                break;
            case 4: // 科室管理
            case 5: // 科室管理（重复项）
                targetFragment = DepartmentManagementFragment.newInstance();
                break;
        }
        
        if (targetFragment != null) {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, targetFragment);
            transaction.addToBackStack(null); // 添加到回退栈，支持返回
            transaction.commit();
        }
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }
}