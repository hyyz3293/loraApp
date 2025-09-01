package com.lora.cn.ui.fragment.setting;

import android.Manifest;
import android.content.Intent;
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
import com.blankj.utilcode.util.UtilsTransActivity;
import com.lora.cn.R;
import com.lora.cn.ui.activity.WebViewActivity;
import com.lora.cn.ui.adapter.TerminalSettingAdapter;
import com.lora.cn.ui.adapter.TerminalSettingDeviceAdapter;
import com.lora.cn.ui.fragment.setting.device.IpConfigFragment;
import com.lora.cn.ui.fragment.setting.device.WifiSettingFragment;
import com.lora.cn.ui.model.SettingItem;

import java.util.ArrayList;
import java.util.List;

public class DeviceSettingFragment extends Fragment {

    private RecyclerView terminalSettingRecycle;
    private TerminalSettingDeviceAdapter terminalSettingAdapter;
    private TextView mTvBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_setting, container, false);

        initViews(view);
        initSettingData();
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
        // 创建设置项数据
        List<SettingItem> settingList = new ArrayList<>();
        settingList.add(new SettingItem("音量设置", 1, 0));
        settingList.add(new SettingItem( "WiFIi连接", 0, 1));
        settingList.add(new SettingItem( "IP配置", 0, 2));
        settingList.add(new SettingItem(  "清点次数(非管理员角色)", 2, 3));
        settingList.add(new SettingItem( "低电量报警值", 2, 4, "20%"));
        settingList.add(new SettingItem( "返回首页时间", 2, 5, "60"));

        // 设置RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext()); // 3列网格布局
        terminalSettingRecycle.setLayoutManager(layoutManager);

        terminalSettingAdapter = new TerminalSettingDeviceAdapter();
        terminalSettingRecycle.setAdapter(terminalSettingAdapter);

        // 设置点击事件监听器
        terminalSettingAdapter.setOnItemClickListener((adapter, view, position) -> {
            SettingItem settingItem = settingList.get(position);
            if (settingItem.getViewType() == 0 || settingItem.getIndex() < 3)
                onSettingClick(position, settingItem);
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
    public static DeviceSettingFragment newInstance() {
        return new DeviceSettingFragment();
    }
}