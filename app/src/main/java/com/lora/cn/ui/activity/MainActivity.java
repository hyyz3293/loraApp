package com.lora.cn.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.blankj.utilcode.util.SPUtils;
import com.chad.library.adapter4.BaseQuickAdapter;
import com.lora.cn.R;
import com.lora.cn.events.UplinkDataEvent;
import com.lora.cn.ui.model.Terminal;
import com.lora.cn.ui.adapter.MainPagerAdapter;
import com.lora.cn.ui.adapter.MenuTabAdapter;
import com.lora.cn.ui.model.MenuTab;
import com.lora.cn.ui.fragment.setting.user.UserInfoFragment;
import com.lora.cn.ui.fragment.AddDeviceFragment;

import java.util.ArrayList;
import java.util.List;

import com.lora.cn.network.MqttPacketsClient;
import com.lora.cn.network.GatewayPacketsClient;
import com.lora.cn.database.DatabaseHelper;
import org.greenrobot.eventbus.EventBus;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView rvMenuTabs;
    private ViewPager2 viewPager;
    private MenuTabAdapter menuTabAdapter;
    private MainPagerAdapter pagerAdapter;
    private List<MenuTab> menuTabs;
    private ImageView btnLogout;
    private TextView tvUserName;
    private FrameLayout fragmentUserInfoContainer;
    private FrameLayout fragmentDeviceListContainer;
    private UserInfoFragment userInfoFragment;
    
    private int currentTabIndex = 0;
    private boolean isUserInfoVisible = false;
    private boolean isDeviceListVisible = false;

    // 全局 MQTT 客户端（MainActivity 启动并维持）
    private MqttPacketsClient mqttClient;
    private DatabaseHelper databaseHelper;
    private static final long TEST_INTERVAL = 30 * 1000; // 1分钟
    private android.content.BroadcastReceiver brokerReadyReceiver;

    // 自动返回首页计时
    private android.os.Handler autoReturnHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private long lastNonHomeStartMs = 0L;
    private final Runnable autoReturnRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (!isOnHome()) {
                    if (lastNonHomeStartMs == 0L) {
                        lastNonHomeStartMs = System.currentTimeMillis();
                    }
                    long timeoutMs = getAutoReturnTimeoutMs();
                    if (System.currentTimeMillis() - lastNonHomeStartMs >= timeoutMs) {
                        Log.i(TAG, "超时未在首页，自动返回首页");
                        navigateHome();
                    }
                } else {
                    lastNonHomeStartMs = 0L;
                }
            } catch (Exception e) {
                Log.e(TAG, "自动返回首页检查异常: " + e.getMessage());
            } finally {
                autoReturnHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化数据库助手
        databaseHelper = DatabaseHelper.getInstance(this);
        
        initViews();
        initMenuTabs();
        initViewPager();
        initListeners();
        initUserInfo();
        
        // 默认显示终端列表
        menuTabs.get(0).setSelected(true);
        menuTabAdapter.notifyDataSetChanged();

        try {
            com.blankj.utilcode.util.SPUtils sp = com.blankj.utilcode.util.SPUtils.getInstance();
            boolean localEnabled = sp.getBoolean("mqtt_local_broker_enabled", true);
            int localPort = sp.getInt("mqtt_local_broker_port", 1883);
            if (true) {
                android.content.Intent svc = new android.content.Intent(this, com.lora.cn.service.MqttBrokerService.class);
                svc.putExtra("port", localPort > 0 ? localPort : 1883);
                androidx.core.content.ContextCompat.startForegroundService(this, svc);
            }
            // 注册监听：本地Broker就绪后再连接
            brokerReadyReceiver = new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(android.content.Context context, android.content.Intent intent) {
                    if ("com.lora.cn.MQTT_BROKER_READY".equals(intent.getAction())) {
                        android.util.Log.i(TAG, "收到MQTT_BROKER_READY广播，开始连接本地MQTT");
                        startGlobalMqttLogging();
                    }
                }
            };
            android.content.IntentFilter filter = new android.content.IntentFilter("com.lora.cn.MQTT_BROKER_READY");
            registerReceiver(brokerReadyReceiver, filter);
            // 若服务端已就绪（比如用户此前已启动），立即连接
            if (sp.getBoolean("mqtt_local_broker_ready", false)) {
                startGlobalMqttLogging();
            }
        } catch (Exception ignored) {
            Log.e("tag", "error" + ignored);
        }

        // 在 MainActivity 启动 MQTT 连接并打印上下行日志
        startTestTimer();
        // 启动全局MQTT日志监听（优先连接本地Broker）
        startGlobalMqttLogging();

        // 启动自动返回首页的周期检查
        autoReturnHandler.removeCallbacks(autoReturnRunnable);
        autoReturnHandler.postDelayed(autoReturnRunnable, 1000);
    } 

    private void startTestTimer() {
        // 这里需要实现定时器逻辑，用于定期执行测试任务
        // 由于没有具体的实现细节，这里提供一个空的实现
        Log.d(TAG, "startTestTimer called - timer functionality not implemented");
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        // 任意用户交互（点击/触摸）重置自动返回首页计时
        if (ev != null && ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            if (!isOnHome()) {
                lastNonHomeStartMs = System.currentTimeMillis();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void initViews() {
        rvMenuTabs = findViewById(R.id.rv_menu_tabs);
        viewPager = findViewById(R.id.view_pager);
        btnLogout = findViewById(R.id.logout);
        tvUserName = findViewById(R.id.tv_user_name);
        fragmentUserInfoContainer = findViewById(R.id.fragment_user_info_container);
        fragmentDeviceListContainer = findViewById(R.id.fragment_device_list_container);
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
        // 禁止用户手势滑动
        viewPager.setUserInputEnabled(false);
        
        // 设置ViewPager2的页面切换监听
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTabSelection(position);
                // 非首页开始计时
                if (position != 0) {
                    lastNonHomeStartMs = System.currentTimeMillis();
                } else {
                    lastNonHomeStartMs = 0L;
                }
            }
        });
    }

    private void initUserInfo() {
        // 获取当前登录用户名并设置到tvUserName
        String currentUserName = SPUtils.getInstance().getString("current_user_name", "用户");
        tvUserName.setText(currentUserName);
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

    public void showDeviceList() {
        if (isDeviceListVisible) {
            return;
        }

        // 隐藏其他界面
        if (isUserInfoVisible) {
            hideUserInfo();
        }

        // 显示设备列表Fragment
        com.lora.cn.ui.fragment.DeviceListFragment fragment = com.lora.cn.ui.fragment.DeviceListFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_device_list_container, fragment)
                .addToBackStack("device_list")
                .commit();

        fragmentDeviceListContainer.setVisibility(View.VISIBLE);
        rvMenuTabs.setVisibility(View.INVISIBLE);
        viewPager.setVisibility(View.GONE);
        isDeviceListVisible = true;
        lastNonHomeStartMs = System.currentTimeMillis();
    }

    public void hideDeviceList() {
        if (!isDeviceListVisible) {
            return;
        }

        fragmentDeviceListContainer.setVisibility(View.GONE);
        viewPager.setVisibility(View.VISIBLE);
        rvMenuTabs.setVisibility(View.VISIBLE);
        isDeviceListVisible = false;

        // 清除Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_device_list_container, new Fragment())
                .commit();
    }

    private void showUserInfo() {
        if (userInfoFragment == null) {
            userInfoFragment = new UserInfoFragment();
            userInfoFragment.setOnUserInfoActionListener(new UserInfoFragment.OnUserInfoActionListener() {
                @Override
                public void onCloseUserInfo() {
                    hideUserInfo();
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
        lastNonHomeStartMs = System.currentTimeMillis();
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
        if (isOnHome()) {
            lastNonHomeStartMs = 0L;
        }
    }

    @Override
    public void onBackPressed() {
        if (isDeviceListVisible) {
            hideDeviceList();
        } else if (isUserInfoVisible) {
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

    // ---------------- MQTT 全局连接与日志 -----------------
    private void startGlobalMqttLogging() {
        try {
            if (mqttClient == null) mqttClient = new MqttPacketsClient();
            com.blankj.utilcode.util.SPUtils sp = com.blankj.utilcode.util.SPUtils.getInstance();
            int localPort = sp.getInt("mqtt_local_broker_port", 1883);
            String brokerUrl = "tcp://127.0.0.1:" + (localPort > 0 ? localPort : 1883);
            android.util.Log.i(TAG, "使用本地MQTT Broker: " + brokerUrl);
            final String clientId = "android-" + System.currentTimeMillis();
            String topicFilter = sp.getString("mqtt_topic_filter", "/milesight/uplink/#");
            String username = sp.getString("mqtt_username", "");
            String password = sp.getString("mqtt_password", "");
            boolean trustAll = sp.getBoolean("mqtt_trust_all_certs", false);
            mqttClient.connectAndSubscribe(getApplicationContext(), brokerUrl, clientId, topicFilter,
                    username, password, trustAll,
                    new GatewayPacketsClient.PacketsListener() {
                        @Override
                        public void onStatus(String msg) {
                            Log.d(TAG, "MQTT状态 onStatus: " + msg);
                        }
                        @Override
                        public void onPackets(java.util.List<com.lora.cn.network.GatewayPacketsClient.PacketRecord> records) {
                            if (records == null || records.isEmpty()) {
                                Log.e(TAG, "收到上行数据条数: 0");
                                return;
                            }
                            Log.e(TAG, "收到上行数据条数: " + records.size());
                            
                            // 获取当前时间
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            String currentTime = sdf.format(new Date());
                            
                            for (com.lora.cn.network.GatewayPacketsClient.PacketRecord r : records) {
                                String devEui = r.deviceId != null ? r.deviceId : "-";
                                String devAddr = r.devAddr != null ? r.devAddr : "-";
                                String hex = r.payloadHex != null ? r.payloadHex : "-";
                                String dr = r.dr != null ? r.dr : "-";
                                String time = r.time != null ? r.time : currentTime;
                                String freq = r.freq != null ? String.valueOf(r.freq) : "-";
                                String rssi = r.rssi != null ? String.valueOf(r.rssi) : "-";
                                String snr = r.snr != null ? String.valueOf(r.snr) : "-";
                                String fport = r.fport != null ? String.valueOf(r.fport) : "-";
                                String fcnt = r.fcnt != null ? String.valueOf(r.fcnt) : "-";
                                
                                // 存储到数据库
                                if (!"-".equals(hex)) {
                                    // 存储到上行数据日志表
                                    long result = databaseHelper.addUplinkLog(time, hex);
                                    Log.d(TAG, "上行数据存储到上行日志表，结果: " + result);
                                    
                                    // 同时存储到日志信息表
                                    try {
                                        long logResult = databaseHelper.addLog(
                                            devEui,                    // terminalId
                                            "上行数据设备",              // terminalName  
                                            devEui,                    // deviceId
                                            "数据接收",                 // status
                                            "系统",                    // operator
                                            time,                      // operationTime
                                            "接收上行数据: " + hex       // action
                                        );
                                        Log.d(TAG, "上行数据存储到日志信息表，结果: " + logResult);
                                    } catch (Exception e) {
                                        Log.e(TAG, "存储上行数据到日志信息表失败: " + e.getMessage());
                                    }
                                    
                                    // 通过EventBus广播（UplinkDataEvent暂时不可用）
                                    UplinkDataEvent event = new UplinkDataEvent(time, hex);
                                    EventBus.getDefault().post(event);
                                    Log.d(TAG, "上行数据准备广播: time=" + time + ", hex=" + hex);
                                }
                                
                                Log.i(TAG,
                                        "UPLINK devEUI=" + devEui +
                                        " devAddr=" + devAddr +
                                        " fport=" + fport +
                                        " fcnt=" + fcnt +
                                        " rssi=" + rssi +
                                        " snr=" + snr +
                                        " freq=" + freq +
                                        " dr=" + dr +
                                        " time=" + time +
                                        " hex=" + hex);
                            }
                        }
                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "MQTT错误: " + error);
                        }
                        @Override
                        public void onComplete() {
                            Log.d(TAG, "MQTT完成/断开");
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "启动MQTT日志输出失败", e);
        }
    }

    /**
     * 提供下行发送（方式1：通用主题，devEUI在消息体中）
     */
    public void sendDownlinkSimple(String devEui, String payloadHex, int fport, boolean confirmed) {
        try {
            if (mqttClient == null) {
                mqttClient = new MqttPacketsClient();
                Log.w(TAG, "MQTT客户端未初始化，已创建实例但未连接");
            }
            mqttClient.publishDownlinkSimple("/milesight/downlink", devEui, payloadHex, fport, confirmed);
            Log.i(TAG, "DOWNLINK(simple) devEUI=" + devEui + " fport=" + fport + " hex=" + payloadHex + " confirmed=" + confirmed);
        } catch (Exception e) {
            Log.e(TAG, "下行发送失败(simple)：" + e.getMessage());
        }
    }

    /**
     * 提供下行发送（方式2：按设备主题，devEUI在主题路径中）
     */
    public void sendDownlinkByDevEuiTopic(String devEui, String payloadHex, int fport, boolean confirmed) {
        try {
            if (mqttClient == null) {
                mqttClient = new MqttPacketsClient();
                Log.w(TAG, "MQTT客户端未初始化，已创建实例但未连接");
            }
            mqttClient.publishDownlinkByDevEuiTopic("/milesight/downlink", devEui, payloadHex, fport, confirmed);
            Log.i(TAG, "DOWNLINK(by-topic) devEUI=" + devEui + " fport=" + fport + " hex=" + payloadHex + " confirmed=" + confirmed);
        } catch (Exception e) {
            Log.e(TAG, "下行发送失败(by-topic)：" + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        try {
            if (mqttClient != null) {
                mqttClient.disconnect();
            }
            if (brokerReadyReceiver != null) {
                unregisterReceiver(brokerReadyReceiver);
                brokerReadyReceiver = null;
            }
            autoReturnHandler.removeCallbacks(autoReturnRunnable);
        } catch (Exception e) {
            Log.e(TAG, "发送测试上行数据失败: " + e.getMessage());
        }
    }

    private boolean isOnHome() {
        // 首页：无用户信息/设备列表覆盖，且当前Tab为首页（index=0）
        return !isUserInfoVisible && !isDeviceListVisible && currentTabIndex == 0;
    }

    private long getAutoReturnTimeoutMs() {
        long sec = com.blankj.utilcode.util.SPUtils.getInstance().getLong("home_auto_return_timeout_sec", 60);
        if (sec <= 0) sec = 60;
        return sec * 1000L;
    }

    private void navigateHome() {
        try {
            // 切回首页Tab
            if (viewPager != null) viewPager.setCurrentItem(0, true);
            // 关闭覆盖层
            if (isUserInfoVisible) hideUserInfo();
            if (isDeviceListVisible) hideDeviceList();
            // 清空回退栈（确保返回首页）
            getSupportFragmentManager().popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            // 显示首页视图
            rvMenuTabs.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);
            fragmentUserInfoContainer.setVisibility(View.GONE);
            fragmentDeviceListContainer.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "navigateHome 异常: " + e.getMessage());
        } finally {
            lastNonHomeStartMs = 0L;
        }
    }


// 显示添加设备界面（UI模型）
public void showAddDeviceFragment(com.lora.cn.ui.model.Terminal uiTerminal) {
    // 隐藏用户信息界面
    if (isUserInfoVisible) {
        hideUserInfo();
    }
    // 构建并显示 AddDeviceFragment
    AddDeviceFragment fragment = AddDeviceFragment.newInstance(uiTerminal);
    getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_device_list_container, fragment)
            .addToBackStack("add_device")
            .commit();

    fragmentDeviceListContainer.setVisibility(View.VISIBLE);
    rvMenuTabs.setVisibility(View.INVISIBLE);
    viewPager.setVisibility(View.GONE);
    isDeviceListVisible = true;
}

// 兼容调用：从数据库实体转换到UI模型并显示
public void showAddDeviceFragment(com.lora.cn.database.entity.Terminal entityTerminal) {
    com.lora.cn.ui.model.Terminal uiTerminal = new com.lora.cn.ui.model.Terminal();
    // 以设备ID作为终端ID显示
    if (entityTerminal.getDeviceId() != null) {
        uiTerminal.setTerminalId(entityTerminal.getDeviceId());
    } else {
        uiTerminal.setTerminalId(String.valueOf(entityTerminal.getTerminalId()));
    }
    uiTerminal.setTerminalName(entityTerminal.getDeviceName());
    uiTerminal.setStatus(entityTerminal.getStatus());
    uiTerminal.setSignalStrength(entityTerminal.getSignalStrength());
    uiTerminal.setDepartment(entityTerminal.getDepartment());
    uiTerminal.setLocation(entityTerminal.getLocation());
    if (entityTerminal.getDepartmentId() != null) {
        uiTerminal.setDepartmentId(entityTerminal.getDepartmentId());
    }
    if (entityTerminal.getRoomId() != null) {
        uiTerminal.setRoomId(entityTerminal.getRoomId());
    }
    if (entityTerminal.getNursingGroupId() != null) {
        uiTerminal.setNursingGroupId(entityTerminal.getNursingGroupId());
    }
    if (entityTerminal.getOtherId() != null) {
        uiTerminal.setOtherId(entityTerminal.getOtherId());
    }
    uiTerminal.setExtension(entityTerminal.getExtension());
    if (entityTerminal.getBatteryLevel() != null) {
        uiTerminal.setBatteryLevel(entityTerminal.getBatteryLevel());
    }
    // 显示添加设备界面
    showAddDeviceFragment(uiTerminal);
}
}