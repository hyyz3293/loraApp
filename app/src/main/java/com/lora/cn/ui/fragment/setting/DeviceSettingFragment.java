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
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.text.format.DateFormat;
import androidx.appcompat.app.AlertDialog;

import com.lora.cn.BuildConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;

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
//        if (hasPermission("setting_wifi")) {
//            settingList.add(new SettingItem("WiFIi连接", 0, 1));
//        }
        if (hasPermission("setting_ip")) {
            settingList.add(new SettingItem("IP信息", 0, 2));
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
        if (hasPermission("setting_inventory")) {
            int h = SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
            int m = SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
            String ts = String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m);
            settingList.add(new SettingItem("定时清点      " +  ts, 0, 6));
        }
        if (hasPermission("setting_sleep_interval")) {
            int sleepMin = SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
            settingList.add(new SettingItem("设备休眠间隔(分钟)", 2, 7, String.valueOf(sleepMin)));
        }
        settingList.add(new SettingItem("版本信息", 0, 8));

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
                case 6: permCode = "setting_inventory"; break;
                case 7: permCode = "setting_sleep_interval"; break;
                case 8: permCode = "setting_device"; break;
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
            case 6:
                showInventoryTimePicker();
                break;
            case 8:
                targetFragment = com.lora.cn.ui.fragment.setting.VersionInfoFragment.newInstance();
                if (targetFragment != null) {
                    androidx.fragment.app.FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                    transaction.replace(R.id.settings_fragment_container, targetFragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
                break;
        }


    }

    private void showVersionInfo() {
        try {
            String buildDate = "";
            try { buildDate = String.valueOf(BuildConfig.class.getField("BUILD_DATE").get(null)); } catch (Exception ignored) {}
            if (buildDate == null || buildDate.trim().isEmpty()) buildDate = "-";
            String msg = "APP(Build " + BuildConfig.VERSION_CODE + " " + buildDate + ")\n"
                    + "VersionName " + BuildConfig.VERSION_NAME;
            new AlertDialog.Builder(requireContext())
                    .setTitle("版本信息")
                    .setMessage(msg)
                    .setPositiveButton("确定", null)
                    .show();
        } catch (Exception ignored) {}
    }
    
    private void showInventoryTimePicker() {
        android.content.Context ctx = requireContext();
        int defHour = SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
        int defMinute = SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
        boolean is24 = DateFormat.is24HourFormat(ctx);
        android.app.TimePickerDialog dlg = new android.app.TimePickerDialog(ctx, (view, hourOfDay, minute) -> {
            SPUtils.getInstance().put("inventory_schedule_hour", hourOfDay);
            SPUtils.getInstance().put("inventory_schedule_minute", minute);
            SPUtils.getInstance().put("inventory_schedule_enabled", true);
            //scheduleInventory(hourOfDay, minute);
            try {
//                com.lora.cn.database.DatabaseHelper db = com.lora.cn.database.DatabaseHelper.getInstance(ctx.getApplicationContext());
//                com.lora.cn.ui.model.LogInfo li = new com.lora.cn.ui.model.LogInfo();
//                li.setTerminalId("SYS");
//                li.setTerminalName("系统设置");
//                li.setDeviceId("SYS");
//                li.setStatusCode(0);
//                li.setOperator(com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", ""));
//                String ts = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
//                li.setOperationTime(ts);
//                li.setCreateTime(ts);
//                li.setAction("设置: 定时清点=" + String.format(java.util.Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
                //db.addLog(li);
            } catch (Exception ignored) {}
            Toast.makeText(ctx, "已设置定时清点: " + String.format(java.util.Locale.getDefault(), "%02d:%02d", hourOfDay, minute), Toast.LENGTH_SHORT).show();
            initSettingData();
        }, defHour, defMinute, is24);
        dlg.show();
    }
//
//    private void scheduleInventory(int hour, int minute) {
//        android.content.Context ctx = requireContext().getApplicationContext();
//        try { com.lora.cn.LoraApp.scheduleInventory(ctx, hour, minute); } catch (Exception ignored) {}
//
//        AlarmManager am = (AlarmManager) ctx.getSystemService(android.content.Context.ALARM_SERVICE);
//        if (am == null) return;
//        Intent intent = new Intent("com.lora.cn.ACTION_INVENTORY_SCHEDULE");
//        intent.setClass(ctx, com.lora.cn.receiver.InventoryScheduleReceiver.class);
//        PendingIntent pi = PendingIntent.getBroadcast(ctx, 10001, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//        Intent showIntent = new Intent(ctx, com.lora.cn.ui.activity.MainActivity.class);
//        PendingIntent showPi = PendingIntent.getActivity(ctx, 20001, showIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//        Calendar cal = Calendar.getInstance();
//        cal.set(Calendar.SECOND, 0);
//        cal.set(Calendar.MILLISECOND, 0);
//        cal.set(Calendar.HOUR_OF_DAY, hour);
//        cal.set(Calendar.MINUTE, minute);
//        long trigger = cal.getTimeInMillis();
//        long now = System.currentTimeMillis();
//        if (trigger <= now) {
//            trigger = now + 60_000L;
//        }
//        try { com.lora.cn.utils.LogUtils.e("DeviceSettingFragment", "定时执行设置: " + String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute) + ", trigger=" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date(trigger))); } catch (Exception ignored) {}
//        am.cancel(pi);
//        if (android.os.Build.VERSION.SDK_INT >= 23) {
//            AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(trigger, showPi);
//            am.setAlarmClock(info, pi);
//        } else {
//            am.setExact(AlarmManager.RTC_WAKEUP, trigger, pi);
//        }
//    }
    
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
