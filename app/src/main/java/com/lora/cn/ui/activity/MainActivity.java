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
import com.google.gson.Gson;
import com.lora.cn.R;
import com.lora.cn.constant.SpConstant;
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
import com.lora.cn.utils.LoRaFrameParser;

import org.greenrobot.eventbus.EventBus;
import com.lora.cn.event.TerminalRefreshEvent;
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
    private static final long TEST_INTERVAL = 10 * 1000; // 30秒
    private android.content.BroadcastReceiver brokerReadyReceiver;
    private android.os.Handler testUplinkHandler;
    private final Runnable testUplinkRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String time = sdf.format(new Date());
                String hex = generateTestUplinkHex();

                long result = databaseHelper.addUplinkLog(time, hex);
                Log.d(TAG, "自动测试上行写入日志库结果: " + result);

                UplinkDataEvent event = new UplinkDataEvent(time, hex);
                EventBus.getDefault().post(event);
                Log.d(TAG, "自动测试上行广播: time=" + time + ", hex=" + hex);
            } catch (Exception e) {
                Log.e(TAG, "自动测试上行失败", e);
            } finally {
                if (testUplinkHandler != null) {
                    testUplinkHandler.postDelayed(this, TEST_INTERVAL);
                }
            }
        }
    };

    // 自动返回首页计时
    private android.os.Handler autoReturnHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private long lastNonHomeStartMs = 0L;
    private volatile long lastInteractionMs = System.currentTimeMillis();
    private final Runnable autoReturnRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                long timeoutMs = getAutoReturnTimeoutMs();
                long idleMs = System.currentTimeMillis() - lastInteractionMs;
                if (!isOnHome() && idleMs >= timeoutMs) {
                    Log.i(TAG, "空闲超时，自动返回首页");
                    navigateHome();
                }
            } catch (Exception e) {
                Log.e(TAG, "自动返回首页检查异常: " + e.getMessage());
            } finally {
                autoReturnHandler.postDelayed(this, 1000);
            }
        }
    };

//    private android.os.Handler startupLogHandler = new android.os.Handler(android.os.Looper.getMainLooper());
//    private final Runnable startupLogRunnable = new Runnable() {
//        @Override
//        public void run() {
//            try {
//                com.lora.cn.database.DatabaseHelper db = databaseHelper != null ? databaseHelper : com.lora.cn.database.DatabaseHelper.getInstance(MainActivity.this);
//                db.updateTerminalStatusByDeviceId("SIM_DEV", com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ABNORMAL_LOST);
//                db.addLog(
//                        "SIM_DEV",
//                        "模拟设备",
//                        "SIM_DEV",
//                        "异常丢失",
//                        "",
//                        "",
//                        "接收上行数据: 模拟异常取走"
//                );
//                Log.i(TAG, "已发送启动2分钟后的异常取走日志");
//            } catch (Exception e) {
//                Log.e(TAG, "发送异常取走日志失败: " + e.getMessage());
//            }
//        }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化数据库助手
        databaseHelper = DatabaseHelper.getInstance(this);
        try {
            com.lora.cn.utils.LogUtils.init(getApplicationContext());
            com.lora.cn.utils.CrashLogger.install(getApplicationContext());
        } catch (Exception ignored) {}
        try {
            databaseHelper.ensureDefaultAdminRoleAssigned();
            databaseHelper.debugLogAdminRoleAndUser();
        } catch (Exception e) {
            android.util.Log.e(TAG, "初始化管理员角色/用户日志失败: " + e.getMessage());
        }

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
        //stTimer();
        // 启动全局MQTT日志监听（优先连接本地Broker）
        startGlobalMqttLogging();

        // 启动自动返回首页的周期检查
        autoReturnHandler.removeCallbacks(autoReturnRunnable);
        autoReturnHandler.postDelayed(autoReturnRunnable, 1000);

//        startupLogHandler.removeCallbacks(startupLogRunnable);
//        startupLogHandler.postDelayed(startupLogRunnable, 120000);
        
        if (alertEvaluateHandler == null) {
            alertEvaluateHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        }
        alertEvaluateHandler.removeCallbacks(alertEvaluateRunnable);
        alertEvaluateHandler.postDelayed(alertEvaluateRunnable, 1000);

        LoRaFrameParser.ParsedFrame frameData = LoRaFrameParser.parseFrame("A528E2000100032509000100001820250926080808000000080000007E017261740000000000CF5A");

        Log.d(TAG, " ================ 1111: " + new Gson().toJson(frameData));



    } 

    @Override
    protected void onStart() {
        super.onStart();
        if (!org.greenrobot.eventbus.EventBus.getDefault().isRegistered(this)) {
            org.greenrobot.eventbus.EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onStop() {
        if (org.greenrobot.eventbus.EventBus.getDefault().isRegistered(this)) {
            org.greenrobot.eventbus.EventBus.getDefault().unregister(this);
        }
        try {
            if (alertPlayer != null) {
                if (alertPlayer.isPlaying()) alertPlayer.stop();
                alertPlayer.release();
                alertPlayer = null;
            }
        } catch (Exception ignored) {}
        super.onStop();
    }

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    public void onTerminalRefreshEvent(com.lora.cn.event.TerminalRefreshEvent event) {
        try { updatePendingBadge(); } catch (Exception ignored) {}
    }

    private void startTestTimer() {
        try {
            if (testUplinkHandler == null) {
                testUplinkHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            }
            testUplinkHandler.removeCallbacks(testUplinkRunnable);
            testUplinkHandler.post(testUplinkRunnable);
            Log.d(TAG, "自动测试上行计时器已启动，间隔: " + TEST_INTERVAL + "ms");
        } catch (Exception e) {
            Log.e(TAG, "启动自动测试上行计时器失败", e);
        }
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (ev != null && ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            lastInteractionMs = System.currentTimeMillis();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        lastInteractionMs = System.currentTimeMillis();
    }

    private void initViews() {
        rvMenuTabs = findViewById(R.id.rv_menu_tabs);
        viewPager = findViewById(R.id.view_pager);
        btnLogout = findViewById(R.id.logout);
        tvUserName = findViewById(R.id.tv_user_name);
        fragmentUserInfoContainer = findViewById(R.id.fragment_user_info_container);
        fragmentDeviceListContainer = findViewById(R.id.fragment_device_list_container);
        View globalOverlay = findViewById(R.id.global_overlay_container);
        if (globalOverlay != null) globalOverlay.bringToFront();
        llAlertPending = findViewById(R.id.ll_alert_pending);
        llAlertPendingSmall = findViewById(R.id.ll_alert_pending_small);
        tvErrorNumber = findViewById(R.id.error_number);
        tvErrorTitle = findViewById(R.id.error_title);
        tvErrorName = findViewById(R.id.error_name);
        tvErrorCode = findViewById(R.id.error_code);
        tvErrorTime = findViewById(R.id.error_time);
        ivErrorSmall = findViewById(R.id.error_small);
        ivErrorClose = findViewById(R.id.error_close);
        tvErrorVoiceNo = findViewById(R.id.error_voice_no);
        tvErrorComplete = findViewById(R.id.error_complte);

        if (ivErrorSmall != null) {
            ivErrorSmall.setOnClickListener(v -> minimizePending());
        }
        if (llAlertPendingSmall != null) {
            llAlertPendingSmall.setOnClickListener(v -> openAlertPendingList());
        }
        if (ivErrorClose != null) {
            ivErrorClose.setOnClickListener(v -> {
                if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
                if (llAlertPendingSmall != null) llAlertPendingSmall.setVisibility(View.VISIBLE);
            });
        }
        if (tvErrorVoiceNo != null) {
            tvErrorVoiceNo.setOnClickListener(v -> {
                alertMuted = !alertMuted;
                if (alertMuted) {
                    stopAlertRinging();
                }
                android.widget.Toast.makeText(this, alertMuted ? "已静音" : "已取消静音", android.widget.Toast.LENGTH_SHORT).show();
            });
        }
        if (tvErrorComplete != null) {
            tvErrorComplete.setOnClickListener(v -> showImmediateHandleDialog());
            tvErrorComplete.setText("确认处理");
        }
    }
    
    private void initMenuTabs() {
        menuTabs = new ArrayList<>();
        menuTabs.add(new MenuTab("终端列表", 0));
        menuTabs.add(new MenuTab("日志信息", 1));
        menuTabs.add(new MenuTab("清点终端", 2));
        menuTabs.add(new MenuTab("设置", 3));
        //menuTabs.add(new MenuTab("报警处理", -1));
        
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
                com.lora.cn.ui.model.MenuTab tab = menuTabs.get(position);
                if (tab != null && "报警处理".equals(tab.getTitle())) {
                    showImmediateHandleDialog();
                } else {
                    switchToTab(position);
                }
            }
        });
        
        btnLogout.setOnClickListener(v -> confirmLogout());

        tvUserName.setOnClickListener(v -> toggleUserInfo());
    }

    private void showImmediateHandleDialog() {
        try {
            AlertItem target = null;
            if (!alertQueue.isEmpty()) {
                target = alertQueue.peekLast();
            } else {
                com.lora.cn.database.DatabaseHelper db = databaseHelper != null ? databaseHelper : com.lora.cn.database.DatabaseHelper.getInstance(this);
                java.util.List<com.lora.cn.ui.model.LogInfo> all = db.getAllLogs();
                com.lora.cn.ui.model.LogInfo pick = null;
                for (com.lora.cn.ui.model.LogInfo li : all) {
                    int s = li.getStatusCode();
                    boolean candidate = s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                            || s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code
                            || s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
                    if (!candidate) continue;
                    if (pick == null) pick = li;
                }
                if (pick != null) {
                    target = new AlertItem();
                    target.title = com.lora.cn.ui.constants.LogStatus.toText(pick.getStatusCode());
                    target.name = pick.getTerminalName();
                    target.code = pick.getTerminalId();
                    target.time = pick.getCreateTime();
                    target.logId = pick.getId();
                }
            }
            if (target == null) return;
            String devHex = target.code != null ? target.code : "";
            String title = "确认处理";
            AlertItem finalTarget = target;
            com.lora.cn.utils.DialogUtils.showRemarkDialog(this, title, "", new com.lora.cn.utils.DialogUtils.OnConfirmListener() {
                @Override
                public void onConfirm(String remark) {
                    String user = com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", "");
                    String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                    try {
                        if (finalTarget.logId > 0) databaseHelper.updateLogHandled(finalTarget.logId, user, time, remark);
                        int mask = 0;
                        if ("异常取走".equals(finalTarget.title)) mask |= 0x00000001;
                        if ("设备低电量".equals(finalTarget.title)) mask |= 0x00000002;
                        if (mask != 0) {
                            try { sendHandleDownlink(devHex, mask); } catch (Exception ignored) {}
                        }
                        handleAlertHandled(devHex, 0);
                    } catch (Exception ignored) {}
                }
            });
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        try { updatePendingBadge(); } catch (Exception ignored) {}
    }

    // 全局报警窗口状态
    private View llAlertPending;
    private View llAlertPendingSmall;
    private android.widget.TextView tvErrorNumber;
    private android.widget.TextView tvErrorTitle;
    private android.widget.TextView tvErrorName;
    private android.widget.TextView tvErrorCode;
    private android.widget.TextView tvErrorTime;
    private android.widget.ImageView ivErrorSmall;
    private android.widget.ImageView ivErrorClose;
    private android.widget.TextView tvErrorVoiceNo;
    private android.widget.TextView tvErrorComplete;
    private int pendingAlertCount = 0;
    private boolean alertMuted = false;
    private final java.util.Deque<AlertItem> alertQueue = new java.util.ArrayDeque<>();
    private AlertItem currentAlert = null;
    private android.media.MediaPlayer alertPlayer;
    private final java.util.Map<String, Integer> lastAlertTypes = new java.util.HashMap<>();
    private final java.util.Set<String> offlineAlertedKeys = new java.util.HashSet<>();
    private android.os.Handler alertEvaluateHandler;
    private final Runnable alertEvaluateRunnable = new Runnable() {
        @Override public void run() {
            try { evaluateAlertOverlayGlobal(); } finally { if (alertEvaluateHandler != null) alertEvaluateHandler.postDelayed(this, 60000); }
        }
    };

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    // 订阅上行事件：根据状态位/事件映射警报类型，触发全局处理弹窗
    public void onUplinkDataEvent(UplinkDataEvent event) {
        if (event == null || alertMuted) return;
        String hex = event.getHex();
        LoRaFrameParser.ParsedFrame frame = LoRaFrameParser.parseFrame(hex);
        if (frame == null) return;
        if (frame.deviceId == null || frame.deviceId.isEmpty()) return;
        try {
            if (!databaseHelper.isTerminalExists(frame.deviceId)) {
                return;
            }
        } catch (Exception ignored) {}
        int statusCode = 0;
        try {
            java.util.List<com.lora.cn.ui.model.LogInfo> logs = databaseHelper.getLogsByTerminalId(frame.deviceId);
            if (logs != null && !logs.isEmpty()) {
                statusCode = logs.get(0).getStatusCode();
            }
        } catch (Exception ignored) {}
        if (statusCode == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code || statusCode == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code) {
            String devId = frame.deviceId;
            String msg = statusCode == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code ? "异常取走" : "设备低电量";
            Integer last = lastAlertTypes.get(devId);
            if (last == null || last != statusCode) {
                AlertItem item = buildAlertItem(frame, msg);
                alertQueue.addLast(item);
                lastAlertTypes.put(devId, statusCode);
                updatePendingBadge();
                startAlertRinging30s();
                showLatestPending();
            } else {
                updatePendingBadge();
                if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
                if (llAlertPendingSmall != null && pendingAlertCount > 0) llAlertPendingSmall.setVisibility(View.VISIBLE);
            }
        } else {
            String devId = frame.deviceId;
            if (devId != null) {
                java.util.Deque<AlertItem> newQueue = new java.util.ArrayDeque<>();
                for (AlertItem ai : alertQueue) {
                    boolean sameDev = devId.equalsIgnoreCase(ai.code);
                    boolean abnormal = "异常取走".equals(ai.title) || "设备低电量".equals(ai.title) || "设备离线".equals(ai.title);
                    if (!(sameDev && abnormal)) newQueue.addLast(ai);
                }
                alertQueue.clear();
                alertQueue.addAll(newQueue);
                lastAlertTypes.remove(devId);
                updatePendingBadge();
                if (pendingAlertCount == 0) {
                    if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
                    if (llAlertPendingSmall != null) llAlertPendingSmall.setVisibility(View.GONE);
                } else {
                    showLatestPending();
                }
            }
        }
        try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("状态刷新")); } catch (Exception ignored) {}
    }

    private void showLatestPending() {
        currentAlert = alertQueue.peekLast();
        if (currentAlert == null) return;
        if (tvErrorTitle != null) tvErrorTitle.setText(currentAlert.title);
        if (tvErrorName != null) tvErrorName.setText(currentAlert.name);
        if (tvErrorCode != null) tvErrorCode.setText(currentAlert.code);
        if (tvErrorTime != null) tvErrorTime.setText(currentAlert.time);
        updatePendingBadge();
        if (llAlertPendingSmall != null) llAlertPendingSmall.setVisibility(View.GONE);
        if (llAlertPending != null) llAlertPending.setVisibility(View.VISIBLE);
    }

    private void minimizePending() {
        if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
        updatePendingBadge();
    }

    private void expandPending() {
//        if (!alertQueue.isEmpty()) {
//            showLatestPending();
//        } else {
//            if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
//            if (llAlertPendingSmall != null) llAlertPendingSmall.setVisibility(View.GONE);
//        }
        openAlertPendingList();
    }

    private void handleCurrentAlert() {
        if (currentAlert != null) {
            alertQueue.remove(currentAlert);
            currentAlert = null;
        } else if (!alertQueue.isEmpty()) {
            alertQueue.removeLast();
        }
        pendingAlertCount = alertQueue.size();
        if (!alertQueue.isEmpty()) {
            if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
            if (llAlertPendingSmall != null) llAlertPendingSmall.setVisibility(View.VISIBLE);
            updatePendingBadge();
        } else {
            if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
            if (llAlertPendingSmall != null) llAlertPendingSmall.setVisibility(View.GONE);
        }
    }

    public void updatePendingBadge() {
        try {
            java.util.Set<String> distinct = new java.util.HashSet<>();
            for (AlertItem ai : alertQueue) {
                if (ai == null) continue;
                if ("异常取走".equals(ai.title) || "设备低电量".equals(ai.title) || "设备离线".equals(ai.title)) {
                    distinct.add((ai.code == null ? "" : ai.code) + ":" + ai.title);
                }
            }
            int count = distinct.size();
            pendingAlertCount = count;
            if (tvErrorNumber != null) tvErrorNumber.setText(String.valueOf(count));
            if (llAlertPendingSmall != null) {
                boolean bigVisible = llAlertPending != null && llAlertPending.getVisibility() == View.VISIBLE;
                llAlertPendingSmall.setVisibility(!bigVisible && count > 0 ? View.VISIBLE : View.GONE);
            }
            android.util.Log.d(TAG, "updatePendingBadge count=" + count + ", queueSize=" + alertQueue.size());
        } catch (Exception e) {
            android.util.Log.e(TAG, "更新待处理徽标失败", e);
        }
    }

    public void handleAlertHandled(String devId, int statusCode) {
        try {
            java.util.Deque<AlertItem> newQueue = new java.util.ArrayDeque<>();
            for (AlertItem ai : alertQueue) {
                boolean sameDev = devId != null && devId.equalsIgnoreCase(ai.code);
                boolean abnormal = "异常取走".equals(ai.title) || "设备低电量".equals(ai.title) || "设备离线".equals(ai.title);
                if (!(sameDev && abnormal)) newQueue.addLast(ai);
            }
            alertQueue.clear();
            alertQueue.addAll(newQueue);
            lastAlertTypes.remove(devId);
            pendingAlertCount = alertQueue.size();
            if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
            updatePendingBadge();
        } catch (Exception ignored) {}
    }

    private AlertItem buildAlertItem(LoRaFrameParser.ParsedFrame frame, String msg) {
        String name = "";
        String code = frame != null ? frame.deviceId : "";
        String time = "";
        long logId = -1L;
        try {
            List<com.lora.cn.ui.model.Terminal> terminals = databaseHelper.getAllTerminals();
            if (terminals != null) {
                for (com.lora.cn.ui.model.Terminal t : terminals) {
                    if (t.getTerminalId() != null && t.getTerminalId().equalsIgnoreCase(code)) {
                        name = t.getTerminalName();
                        break;
                    }
                }
            }
            List<com.lora.cn.ui.model.LogInfo> logs = databaseHelper.getLogsByTerminalId(code);
            if (logs != null && !logs.isEmpty()) {
                for (com.lora.cn.ui.model.LogInfo li : logs) {
                    if (li.getStatusCode() == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code ||
                        li.getStatusCode() == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code) {
                        logId = li.getId();
                        break;
                    }
                }
                if (logId <= 0) {
                    logId = logs.get(0).getId();
                }
            }
        } catch (Exception ignored) {}
        if (frame != null && frame.dataTime != null) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                time = sdf.format(frame.dataTime);
            } catch (Exception ignored) {}
        }
        AlertItem item = new AlertItem();
        item.title = msg;
        item.name = name;
        item.code = code;
        item.time = time;
        item.logId = logId;
        return item;
    }

    private static class AlertItem {
        String title;
        String name;
        String code;
        String time;
        long logId;
    }

//    private void showHandleDialogForCurrent() {
//        if (currentAlert == null) return;
//        final android.widget.EditText et = new android.widget.EditText(this);
//        et.setHint("填写处理备注");
//        new androidx.appcompat.app.AlertDialog.Builder(this)
//                .setTitle("确认处理")
//                .setView(et)
//                .setPositiveButton("确定", (d, w) -> {
//                    String remark = et.getText() != null ? et.getText().toString().trim() : "";
//                    String user = com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", "");
//                    String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
//                    long id = currentAlert.logId;
//                    try {
//                        if (id > 0) databaseHelper.updateLogHandled(id, user, time, remark);
//                    } catch (Exception ignored) {}
//                    handleCurrentAlert();
//                })
//                .setNegativeButton("取消", null)
//                .show();
//    }

    private void evaluateAlertOverlayGlobal() {
        try {
            java.util.List<com.lora.cn.ui.model.Terminal> all = databaseHelper.getAllTerminals();
            boolean queuedAny = false;
            for (com.lora.cn.ui.model.Terminal t : all) {
                String devId = t.getTerminalId();
                if (t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE) {
                    Integer last = lastAlertTypes.get(devId);
                    if (last == null || last != com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code) {
                        AlertItem item = new AlertItem();
                        item.title = "设备离线";
                        item.name = t.getTerminalName();
                        item.code = devId;
                        item.time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                        alertQueue.addLast(item);
                        lastAlertTypes.put(devId, com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code);
                        pendingAlertCount = alertQueue.size();
                        try { databaseHelper.addLog(devId, t.getTerminalName(), devId, "设备离线", "", "", "功能码=离线"); } catch (Exception ignored) {}
                        startAlertRinging30s();
                        queuedAny = true;
                    }
                } else {
                    lastAlertTypes.remove(devId);
                }
            }
            if (queuedAny || !alertQueue.isEmpty()) {
                showLatestPending();
            }
            updatePendingBadge();
            try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("离线刷新")); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private void openAlertPendingList() {
        try {
            androidx.fragment.app.Fragment fragment = new com.lora.cn.ui.fragment.AlertPendingListFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_device_list_container, fragment)
                    .addToBackStack("alert_pending")
                    .commit();
            llAlertPendingSmall.setVisibility(View.VISIBLE);
            llAlertPending.setVisibility(View.GONE);

            fragmentDeviceListContainer.setVisibility(View.VISIBLE);
            rvMenuTabs.setVisibility(View.INVISIBLE);
            viewPager.setVisibility(View.GONE);
        } catch (Exception e) {
            android.util.Log.e(TAG, "打开报警处理页面失败", e);
        }
    }

    private android.os.Handler ringHandler;
    private java.lang.Runnable ringStopRunnable;
    private void startAlertRinging30s() {
        try {
            if (alertMuted) return;
            if (alertPlayer != null && alertPlayer.isPlaying()) return;
            if (alertPlayer != null) {
                try { alertPlayer.release(); } catch (Exception ignored) {}
                alertPlayer = null;
            }
            android.content.res.AssetFileDescriptor afd = getAssets().openFd("901028.wav");
            android.media.MediaPlayer mp = new android.media.MediaPlayer();
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.setLooping(true);
            mp.setOnCompletionListener(p -> {
                try { p.release(); } catch (Exception ignored) {}
                if (alertPlayer == p) alertPlayer = null;
            });
            mp.prepare();
            mp.start();
            alertPlayer = mp;
            if (ringHandler == null) ringHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            if (ringStopRunnable != null) ringHandler.removeCallbacks(ringStopRunnable);
            ringStopRunnable = new java.lang.Runnable() {
                @Override public void run() { stopAlertRinging(); }
            };
            ringHandler.postDelayed(ringStopRunnable, 30000);
        } catch (Exception ignored) {}
    }
    private void stopAlertRinging() {
        try {
            if (ringHandler != null && ringStopRunnable != null) {
                ringHandler.removeCallbacks(ringStopRunnable);
            }
            if (alertPlayer != null) {
                try { if (alertPlayer.isPlaying()) alertPlayer.stop(); } catch (Exception ignored) {}
                try { alertPlayer.release(); } catch (Exception ignored) {}
                alertPlayer = null;
            }
        } catch (Exception ignored) {}
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
//        if (isDeviceListVisible) {
//            return;
//        }

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

    private void confirmLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("确认退出登录")
                .setMessage("确定要退出当前账号吗？")
                .setPositiveButton("退出", (dialog, which) -> {
                    SPUtils.getInstance().put(SpConstant.IS_LOGIN, false);
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
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
                                    try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("已入库刷新")); } catch (Exception ignored) {}
                                    
//                                    // 同时存储到日志信息表
//                                    try {
//                                        long logResult = databaseHelper.addLog(
//                                            devEui,
//                                            "上行数据设备",
//                                            devEui,
//                                            "数据接收",
//                                            "",
//                                            "",
//                                            "接收上行数据: " + hex
//                                        );
//                                        Log.d(TAG, "上行数据存储到日志信息表，结果: " + logResult);
//                                    } catch (Exception e) {
//                                        Log.e(TAG, "存储上行数据到日志信息表失败: " + e.getMessage());
//                                    }
                                    
                                    // 通过EventBus广播（UplinkDataEvent暂时不可用）
                                    UplinkDataEvent event = new UplinkDataEvent(time, hex);
                                    EventBus.getDefault().post(event);
                                    Log.d(TAG, "上行数据准备广播: time=" + time + ", hex=" + hex);
                                    LoRaFrameParser.ParsedFrame frameData = LoRaFrameParser.parseFrame(event.getHex());
                                    Log.d(TAG, "==>>stPowerLockOn>: " + frameData.stPowerLockOn);
                                    Log.d(TAG, "==>>stLayer1NotInPlace>: " + frameData.stLayer1NotInPlace);
                                    Log.d(TAG, "==>>stLayer2NotInPlace>: " + frameData.stLayer2NotInPlace);
                                    Log.d(TAG, "==>>stLayer3NotInPlace>: " + frameData.stLayer3NotInPlace);
                                    Log.d(TAG, "==>>stLayer4NotInPlace>: " + frameData.stLayer4NotInPlace);
                                    Log.d(TAG, "==>>stLayer5NotInPlace>: " + frameData.stLayer5NotInPlace);

                                    Log.d(TAG, "==>>frameData=====>: " + new Gson().toJson(frameData));

//                                    D  上行数据存储到上行日志表，结果: 25
//                                    2025-11-15 21:25:18.642  4652-4652  MainActivity            com.lora.cn                          D  上行数据存储到日志信息表，结果: 26
//                                    2025-11-15 21:25:18.666  4652-4652  MainActivity            com.lora.cn                          D  上行数据准备广播: time=2025-11-08T13:31:32.660661Z, hex=A528E2000100012509000105001820250926081049000000040000007E016964720000000000855A
//                                    2025-11-15 21:25:18.680  4652-4652  MainActivity            com.lora.cn                          D  ==>>stPowerLockOn>: 0
//                                    2025-11-15 21:25:18.680  4652-4652  MainActivity            com.lora.cn                          D  ==>>stLayer1NotInPlace>: 1
//                                    2025-11-15 21:25:18.680  4652-4652  MainActivity            com.lora.cn                          D  ==>>stLayer2NotInPlace>: 1
//                                    2025-11-15 21:25:18.681  4652-4652  MainActivity            com.lora.cn                          D  ==>>stLayer3NotInPlace>: 1
//                                    2025-11-15 21:25:18.681  4652-4652  MainActivity            com.lora.cn                          D  ==>>stLayer4NotInPlace>: 1
//                                    2025-11-15 21:25:18.681  4652-4652  MainActivity            com.lora.cn                          D  ==>>stLayer5NotInPlace>: 1

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
            if (testUplinkHandler != null) {
                testUplinkHandler.removeCallbacks(testUplinkRunnable);
                testUplinkHandler = null;
            }
            if (alertEvaluateHandler != null) {
                alertEvaluateHandler.removeCallbacks(alertEvaluateRunnable);
                alertEvaluateHandler = null;
            }
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

    public void goHome() {
        navigateHome();
    }

    // 仅显示首页的覆盖容器，不加载“附近终端”页面
    public void showOverlayOnly() {
        try {
            if (isUserInfoVisible) hideUserInfo();
            fragmentDeviceListContainer.setVisibility(View.VISIBLE);
            rvMenuTabs.setVisibility(View.INVISIBLE);
            viewPager.setVisibility(View.GONE);
            isDeviceListVisible = true;
        } catch (Exception e) {
            Log.e(TAG, "showOverlayOnly 异常: " + e.getMessage());
        }
    }

    // 生成符合解析器的测试上行hex
    private String generateTestUplinkHex() {
        try {
            String devEui = "2013220000000001";
            byte[] buf = new byte[1 + 8 + 2 + 1 + 2 + 24 + 1 + 1];
            int idx = 0;
            buf[idx++] = (byte) 0xA5;
            byte[] devBytes = hexToBytes(devEui);
            System.arraycopy(devBytes, 0, buf, idx, 8);
            idx += 8;
            buf[idx++] = 0x00; buf[idx++] = 0x01;
            buf[idx++] = 0x01;
            buf[idx++] = 0x00; buf[idx++] = 0x18;
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int YY = cal.get(java.util.Calendar.YEAR) % 100;
            int YY2 = cal.get(java.util.Calendar.YEAR) / 100;
            buf[idx++] = (byte) YY2;
            buf[idx++] = (byte) YY;
            buf[idx++] = (byte) (cal.get(java.util.Calendar.MONTH) + 1);
            buf[idx++] = (byte) cal.get(java.util.Calendar.DAY_OF_MONTH);
            buf[idx++] = (byte) cal.get(java.util.Calendar.HOUR_OF_DAY);
            buf[idx++] = (byte) cal.get(java.util.Calendar.MINUTE);
            buf[idx++] = (byte) cal.get(java.util.Calendar.SECOND);
            buf[idx++] = 0x00; buf[idx++] = 0x00; buf[idx++] = 0x00; buf[idx++] = 0x40;
            buf[idx++] = 0x00; buf[idx++] = 0x00; buf[idx++] = 0x00; buf[idx++] = 0x00;
            buf[idx++] = 0x01; buf[idx++] = (byte) 0x68;
            buf[idx++] = 55;
            buf[idx++] = (byte) 90;
            buf[idx++] = 3;
            buf[idx++] = 2;
            buf[idx++] = 5;
            buf[idx++] = 1;
            buf[idx++] = 0x00;
            buf[idx++] = 0x00;
            buf[idx++] = (byte) 0x5A;
            return bytesToHex(buf);
        } catch (Exception e) {
            return "A5" + "2013220000000001" + "0001" + "01" + "0018" + "000000000000000000000000000000000000000000000000" + "00" + "5A";
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
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
    uiTerminal.setStatus(com.lora.cn.ui.constants.TerminalStatusConstants.textToCode(entityTerminal.getStatus()));
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
    // 设备CODE：优先读取数据库实体的deviceCode，若无则回退extension
    try {
        java.lang.reflect.Method m = entityTerminal.getClass().getMethod("getDeviceCode");
        Object code = m.invoke(entityTerminal);
        if (code instanceof String) {
            uiTerminal.setDeviceCode((String) code);
        }
    } catch (Exception ignored) {
        uiTerminal.setDeviceCode(entityTerminal.getExtension());
    }
    if (entityTerminal.getBatteryLevel() != null) {
        uiTerminal.setBatteryLevel(entityTerminal.getBatteryLevel());
    }
    // 显示添加设备界面
    showAddDeviceFragment(uiTerminal);
}

public void sendHandleDownlink(String devHex, int mask) {
    try {
        if (mqttClient != null) {
            com.lora.cn.utils.DownlinkMessageHelper helper = new com.lora.cn.utils.DownlinkMessageHelper(mqttClient);
            helper.sendDownlink8001Config(devHex, mask);
        }
    } catch (Exception e) {
        android.util.Log.e(TAG, "下发处理下行失败 devEUI=" + devHex + ", mask=" + mask, e);
    }
}
}
