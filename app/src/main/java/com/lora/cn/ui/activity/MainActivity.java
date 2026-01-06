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

import com.blankj.utilcode.util.LogUtils;
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
    private View rlAlertIcon;
    private ImageView ivAlertIcon;
    private ImageView ivLogo;
    private android.widget.TextView btnShareLogs;
    private android.widget.TextView btnMaintenanceBadge;
    
    private int currentTabIndex = 0;
    private boolean isUserInfoVisible = false;
    private boolean isDeviceListVisible = false;

    private MqttPacketsClient mqttClient;
    private DatabaseHelper databaseHelper;
    private java.util.concurrent.ExecutorService ioExecutor;
    private android.os.Handler mainHandler;
    private final java.util.concurrent.atomic.AtomicInteger badgeSeq = new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger alertEvalSeq = new java.util.concurrent.atomic.AtomicInteger();
    private int lastBadgeCount = -1;
    private int lastBadgeQueueSize = -1;
    private long lastBadgeRequestMs = 0L;
    private boolean badgeRequestDelayed = false;
    private int mqttReadyRetry = 0;
    private final Runnable badgeRequestRunnable = new Runnable() {
        @Override
        public void run() {
            badgeRequestDelayed = false;
            updatePendingBadge();
        }
    };
    private static final long TEST_INTERVAL = 10 * 1000; // 30秒
    private android.content.BroadcastReceiver brokerReadyReceiver;
    private android.os.Handler testUplinkHandler;
    private final Runnable testUplinkRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                final String time = sdf.format(new Date());
                final String hex = generateTestUplinkHex();
                if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
                android.content.Context appCtx = getApplicationContext();
                ioExecutor.execute(() -> {
                    try {
                        com.lora.cn.database.DatabaseHelper db = databaseHelper != null
                                ? databaseHelper
                                : com.lora.cn.database.DatabaseHelper.getInstance(appCtx);
                        long result = db.addUplinkLog(hex);
                        Log.d(TAG, "自动测试上行写入日志库结果: " + result);
                        EventBus.getDefault().post(new UplinkDataEvent(time, hex));
                    } catch (Exception e) {
                        Log.e(TAG, "自动测试上行失败", e);
                    }
                });
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
    private java.lang.Integer pendingCountOverride = null;
    private long pendingCountOverrideTs = 0L;
    private long lastNonHomeStartMs = 0L;
    private volatile long lastInteractionMs = System.currentTimeMillis();
    private volatile boolean autoReturnBusy = false;
    private final Runnable autoReturnRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                long timeoutMs = getAutoReturnTimeoutMs();
                if (timeoutMs <= 0) {
                    autoReturnHandler.postDelayed(this, 1000);
                    return;
                }
                if (autoReturnBusy) {
                    lastInteractionMs = System.currentTimeMillis();
                }
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
        if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
        if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        try {
            com.lora.cn.utils.LogUtils.init(getApplicationContext());
            com.lora.cn.utils.CrashLogger.install(getApplicationContext());
        } catch (Exception ignored) {}
        try {
            databaseHelper.ensureDefaultAdminRoleAssigned();
            databaseHelper.debugLogAdminRoleAndUser();
            databaseHelper.syncLowBatteryFlags();
        } catch (Exception e) {
            android.util.Log.e(TAG, "初始化管理员角色/用户日志失败: " + e.getMessage());
        }

        alertMuted = com.blankj.utilcode.util.SPUtils.getInstance().getBoolean("global_alert_muted", false);

        initViews();
        initMenuTabs();
        initViewPager();
        initListeners();
        initUserInfo();
        
        // 默认显示终端列表
        menuTabs.get(0).setSelected(true);
        menuTabAdapter.notifyDataSetChanged();
        applyImmersiveMode();

        try {
            LogUtils.e("android.os.Build.VERSION.SDK_INT===" + android.os.Build.VERSION.SDK_INT);
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(android.content.Context.ALARM_SERVICE);
                boolean prompted = com.blankj.utilcode.util.SPUtils.getInstance().getBoolean("exact_alarm_permission_prompted", false);
                if (am != null && !am.canScheduleExactAlarms() && !prompted) {
                    com.blankj.utilcode.util.SPUtils.getInstance().put("exact_alarm_permission_prompted", true);
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("需要精确闹钟权限")
                            .setMessage("为确保定时清点准时执行，请允许精确闹钟。")
                            .setPositiveButton("去开启", (d, w) -> {
                                try {
                                    android.content.Intent i = new android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                                    i.setData(android.net.Uri.parse("package:" + getPackageName()));
                                    startActivity(i);
                                } catch (Exception e) {
                                    android.content.Intent i2 = new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    i2.setData(android.net.Uri.parse("package:" + getPackageName()));
                                    startActivity(i2);
                                }
                            })
                            .setNegativeButton("稍后", null)
                            .show();
                }
            }
        } catch (Exception ignored) {
            LogUtils.e("ERROR===" + ignored);
        }

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
                        int p = intent != null ? intent.getIntExtra("port", -1) : -1;
                        String ips = intent != null ? intent.getStringExtra("ips") : "";
                        android.util.Log.i(TAG, "收到MQTT_BROKER_READY广播: port=" + p + ", ips=" + ips + "，开始连接本地MQTT");
                        startGlobalMqttLogging();
                    }
                }
            };
            android.content.IntentFilter filter = new android.content.IntentFilter("com.lora.cn.MQTT_BROKER_READY");
            registerReceiver(brokerReadyReceiver, filter);
            // 若服务端已就绪（比如用户此前已启动），立即连接
            if (sp.getBoolean("mqtt_local_broker_ready", false)) {
                android.util.Log.i(TAG, "检测到mqtt_local_broker_ready=true，立即连接本地MQTT");
                startGlobalMqttLogging();
            }
        } catch (Exception ignored) {
            Log.e("tag", "error" + ignored);
        }

        // 在 MainActivity 启动 MQTT 连接并打印上下行日志
//        if (SPUtils.getInstance().getBoolean("uplink_test_enabled", true)) {
//            startTestTimer();
//        }

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

    } 

    public MqttPacketsClient getMqttClient() {
        return mqttClient;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!org.greenrobot.eventbus.EventBus.getDefault().isRegistered(this)) {
            org.greenrobot.eventbus.EventBus.getDefault().register(this);
        }
        applyImmersiveMode();
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
        try {
            if (maintenanceEvaluateHandler != null) {
                maintenanceEvaluateHandler.removeCallbacks(maintenanceEvaluateRunnable);
                maintenanceEvaluateHandler = null;
            }
        } catch (Exception ignored) {}
        super.onStop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersiveMode();
    }

    private void applyImmersiveMode() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.view.WindowInsetsController c = getWindow().getInsetsController();
                if (c != null) {
                    c.hide(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars());
                    c.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                android.view.View decor = getWindow().getDecorView();
                int flags = android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                decor.setSystemUiVisibility(flags);
            }
        } catch (Exception ignored) {}
    }

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    public void onTerminalRefreshEvent(com.lora.cn.event.TerminalRefreshEvent event) {
        try { pendingCountOverride = null; updatePendingBadge(); updateMaintenanceBadge(); } catch (Exception ignored) {}
    }

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    public void onAlertPendingCountEvent(com.lora.cn.event.AlertPendingCountEvent event) {
        try { pendingCountOverride = event != null ? event.count : null; pendingCountOverrideTs = System.currentTimeMillis(); updatePendingBadge(); } catch (Exception ignored) {}
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

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    public void onOperationBusyEvent(com.lora.cn.event.OperationBusyEvent event) {
        autoReturnBusy = event != null && event.busy;
        if (autoReturnBusy) {
            lastInteractionMs = System.currentTimeMillis();
        }
    }

    private void initViews() {
        rvMenuTabs = findViewById(R.id.rv_menu_tabs);
        viewPager = findViewById(R.id.view_pager);
        btnLogout = findViewById(R.id.logout);
        tvUserName = findViewById(R.id.tv_user_name);
        ivLogo = findViewById(R.id.iv_logo);
        btnShareLogs = findViewById(R.id.btn_share_logs);
        btnMaintenanceBadge = findViewById(R.id.btn_maintenance_badge);
        fragmentUserInfoContainer = findViewById(R.id.fragment_user_info_container);
        fragmentDeviceListContainer = findViewById(R.id.fragment_device_list_container);
        rlAlertIcon = findViewById(R.id.rl_alert_icon);
        ivAlertIcon = findViewById(R.id.iv_alert_icon);
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

        if (rlAlertIcon != null) {
            rlAlertIcon.setOnClickListener(this::toggleGlobalMute);
        }
        if (ivErrorSmall != null) {
            ivErrorSmall.setOnClickListener(v -> minimizePending());
        }
        if (llAlertPendingSmall != null) {
            llAlertPendingSmall.setOnClickListener(v -> openAlertPendingList());
            llAlertPendingSmall.setOnTouchListener(new android.view.View.OnTouchListener() {
                float downRawX;
                float downRawY;
                float dX;
                float dY;
                boolean moved;
                @Override
                public boolean onTouch(android.view.View v, android.view.MotionEvent event) {
                    android.view.View parent = (android.view.View) v.getParent();
                    if (parent == null) return false;
                    int[] loc = new int[2];
                    parent.getLocationOnScreen(loc);
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            downRawX = event.getRawX();
                            downRawY = event.getRawY();
                            dX = v.getX() - (downRawX - loc[0]);
                            dY = v.getY() - (downRawY - loc[1]);
                            moved = false;
                            v.animate().alpha(0.6f).setDuration(0).start();
                            return true;
                        case android.view.MotionEvent.ACTION_MOVE: {
                            float rawX = event.getRawX();
                            float rawY = event.getRawY();
                            float newX = rawX - loc[0] + dX;
                            float newY = rawY - loc[1] + dY;
                            int parentWidth = parent.getWidth();
                            int parentHeight = parent.getHeight();
                            int vWidth = v.getWidth();
                            int vHeight = v.getHeight();
                            if (parentWidth > 0 && parentHeight > 0 && vWidth > 0 && vHeight > 0) {
                                newX = Math.max(0, Math.min(parentWidth - vWidth, newX));
                                newY = Math.max(0, Math.min(parentHeight - vHeight, newY));
                            }
                            v.setX(newX);
                            v.setY(newY);
                            if (!moved) {
                                float dx = Math.abs(rawX - downRawX);
                                float dy = Math.abs(rawY - downRawY);
                                moved = dx > 8 || dy > 8;
                            }
                            return true;
                        }
                        case android.view.MotionEvent.ACTION_UP:
                            if (!moved) {
                                openAlertPendingList();
                            } else {
                                com.blankj.utilcode.util.SPUtils.getInstance().put("alert_small_last_x", (int) v.getX());
                                com.blankj.utilcode.util.SPUtils.getInstance().put("alert_small_last_y", (int) v.getY());
                            }
                            v.animate().alpha(1f).setDuration(0).start();
                            return true;
                        default:
                            v.animate().alpha(1f).setDuration(0).start();
                            return false;
                    }
                }
            });
            llAlertPendingSmall.post(() -> {
                int x = com.blankj.utilcode.util.SPUtils.getInstance().getInt("alert_small_last_x", Integer.MIN_VALUE);
                int y = com.blankj.utilcode.util.SPUtils.getInstance().getInt("alert_small_last_y", Integer.MIN_VALUE);
                if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE) {
                    android.view.View parent = (android.view.View) llAlertPendingSmall.getParent();
                    int parentWidth = parent != null ? parent.getWidth() : 0;
                    int parentHeight = parent != null ? parent.getHeight() : 0;
                    int vWidth = llAlertPendingSmall.getWidth();
                    int vHeight = llAlertPendingSmall.getHeight();
                    float newX = x;
                    float newY = y;
                    if (parentWidth > 0 && parentHeight > 0 && vWidth > 0 && vHeight > 0) {
                        newX = Math.max(0, Math.min(parentWidth - vWidth, newX));
                        newY = Math.max(0, Math.min(parentHeight - vHeight, newY));
                    }
                    llAlertPendingSmall.setX(newX);
                    llAlertPendingSmall.setY(newY);
                }
            });
        }
        if (ivErrorClose != null) {
            ivErrorClose.setOnClickListener(v -> {
                v.setPressed(true);
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> v.setPressed(false), 180);
                if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
                if (llAlertPendingSmall != null) setSmallVisible(true);

                //LogUtils.e("llAlertPendingSmall=Visibility===7===" + llAlertPendingSmall.getVisibility());
            });
        }
        if (tvErrorVoiceNo != null) {
            tvErrorVoiceNo.setOnClickListener(this::toggleGlobalMute);
        }
        if (tvErrorComplete != null) {
            tvErrorComplete.setOnClickListener(v -> {
                v.setPressed(true);
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> v.setPressed(false), 180);
                showImmediateHandleDialog();
            });
            tvErrorComplete.setText("确认处理");
        }
        updateAlertMutedUI();
        setupSecretShareLogs();
        try { updateMaintenanceBadge(); } catch (Exception ignored) {}
    }

    private void toggleGlobalMute(View v) {
        alertMuted = !alertMuted;
        com.blankj.utilcode.util.SPUtils.getInstance().put("global_alert_muted", alertMuted);
        if (alertMuted) {
            stopAlertRinging();
        }
        updateAlertMutedUI();
        if (v != null) {
            v.setPressed(true);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> v.setPressed(false), 200);
        }
        android.widget.Toast.makeText(this, alertMuted ? "已静音" : "已取消静音", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void updateAlertMutedUI() {
        try {
            if (tvErrorVoiceNo != null) {
                tvErrorVoiceNo.setText(alertMuted ? "取消静音" : "静音");
            }
            android.view.View slash = findViewById(R.id.error_muted_mark);
            if (slash != null) slash.setVisibility(alertMuted ? android.view.View.VISIBLE : android.view.View.GONE);
            if (ivAlertIcon != null) {
                ivAlertIcon.setImageResource(alertMuted ? R.drawable.ic_alert_horn_red_muted : R.drawable.ic_alert_horn_red);
            }
        } catch (Exception ignored) {}
    }
    
    private void initMenuTabs() {
        menuTabs = new ArrayList<>();
        menuTabs.add(new MenuTab("终端列表", 0));
        menuTabs.add(new MenuTab("清点终端", 1));
        menuTabs.add(new MenuTab("日志信息", 2));
        menuTabs.add(new MenuTab("维护列表", 3));
        menuTabs.add(new MenuTab("设置", 4));
        menuTabs.add(new MenuTab("下行测试", 5));
        menuTabs.add(new MenuTab("上行解析", 6));
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
        if (btnMaintenanceBadge != null) {
            btnMaintenanceBadge.setOnClickListener(v -> switchToTab(3));
        }
    }

    private int secretTapCount = 0;
    private long firstTapTs = 0L;
    private void setupSecretShareLogs() {
        if (ivLogo != null) {
            ivLogo.setOnClickListener(v -> {
                long now = System.currentTimeMillis();
                if (now - firstTapTs > 3000) {
                    secretTapCount = 0;
                    firstTapTs = now;
                }
                secretTapCount++;
                if (secretTapCount >= 5) {
                    secretTapCount = 0;
                    firstTapTs = 0L;
                    try {
                        java.io.File f1 = com.lora.cn.utils.LogUtils.getLogFile();
                        java.io.File appBase = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);
                        if (appBase == null) appBase = getExternalFilesDir(null);
                        java.io.File logsDirApp = appBase != null ? new java.io.File(appBase, "LoraAppLogs") : null;
                        java.io.File f2 = null;
                        if (logsDirApp != null && logsDirApp.exists()) {
                            java.io.File[] arr = logsDirApp.listFiles();
                            if (arr != null) {
                                long best = -1L;
                                for (java.io.File f : arr) {
                                    String n = f != null ? f.getName() : "";
                                    if (n.startsWith("app_logcat") && n.endsWith(".txt")) {
                                        long lm = f.lastModified();
                                        if (lm > best) { best = lm; f2 = f; }
                                    }
                                }
                            }
                        }
                        java.io.File downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                        java.io.File logsDir = new java.io.File(downloads, "LoraAppLogs");
                        java.io.File f2Legacy = null;
                        if (logsDir.exists()) {
                            java.io.File[] arr2 = logsDir.listFiles();
                            if (arr2 != null) {
                                long best2 = -1L;
                                for (java.io.File f : arr2) {
                                    String n = f != null ? f.getName() : "";
                                    if (n.startsWith("app_logcat") && n.endsWith(".txt")) {
                                        long lm = f.lastModified();
                                        if (lm > best2) { best2 = lm; f2Legacy = f; }
                                    }
                                }
                            }
                        }
                        java.util.ArrayList<java.io.File> files = new java.util.ArrayList<>();
                        if (f1 != null && f1.exists()) files.add(f1);
                        if (f2 != null && f2.exists()) files.add(f2);
                        else if (f2Legacy != null && f2Legacy.exists()) files.add(f2Legacy);
                        StringBuilder sb = new StringBuilder();
                        sb.append("是否分享以下日志文件：\n");
                        for (java.io.File f : files) {
                            sb.append(f.getAbsolutePath()).append("\n");
                        }
                        if (files.isEmpty()) {
                            sb.append("暂无可分享日志文件");
                        }
                        android.util.Log.i(TAG, "日志路径:\n" + sb.toString());
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("分享日志")
                                .setMessage(sb.toString())
                                .setPositiveButton("分享", (d, w2) -> {
                                    try {
                                        if (files.isEmpty()) {
                                            android.widget.Toast.makeText(this, "暂无可分享日志文件", android.widget.Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        java.util.ArrayList<android.net.Uri> uris = new java.util.ArrayList<>();
                                        for (java.io.File f : files) {
                                            android.net.Uri u = androidx.core.content.FileProvider.getUriForFile(this, "com.lora.cn.fileprovider", f);
                                            uris.add(u);
                                        }
                                        android.content.Intent share = new android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
                                        share.setType("text/plain");
                                        share.putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris);
                                        share.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        startActivity(android.content.Intent.createChooser(share, "分享日志文件"));
                                    } catch (Exception e) {
                                        android.widget.Toast.makeText(this, "分享失败: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("取消", null)
                                .show();
                    } catch (Exception e) {
                        android.widget.Toast.makeText(this, "准备分享失败: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        if (btnShareLogs != null) {
            btnShareLogs.setOnClickListener(v -> {
                try {
                    java.util.ArrayList<android.net.Uri> uris = new java.util.ArrayList<>();
                    java.io.File f1 = com.lora.cn.utils.LogUtils.getLogFile();
                    if (f1 != null && f1.exists()) {
                        uris.add(androidx.core.content.FileProvider.getUriForFile(this, "com.lora.cn.fileprovider", f1));
                    }
                    java.io.File appBase = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);
                    if (appBase == null) appBase = getExternalFilesDir(null);
                    java.io.File logsDirApp = appBase != null ? new java.io.File(appBase, "LoraAppLogs") : null;
                    java.io.File f2 = null;
                    if (logsDirApp != null && logsDirApp.exists()) {
                        java.io.File[] arr = logsDirApp.listFiles();
                        if (arr != null) {
                            long best = -1L;
                            for (java.io.File f : arr) {
                                String n = f != null ? f.getName() : "";
                                if (n.startsWith("app_logcat") && n.endsWith(".txt")) {
                                    long lm = f.lastModified();
                                    if (lm > best) { best = lm; f2 = f; }
                                }
                            }
                        }
                    }
                    if (f2 != null && f2.exists()) {
                        uris.add(androidx.core.content.FileProvider.getUriForFile(this, "com.lora.cn.fileprovider", f2));
                    } else {
                        java.io.File downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                        java.io.File logsDir = new java.io.File(downloads, "LoraAppLogs");
                        java.io.File f2Legacy = null;
                        if (logsDir.exists()) {
                            java.io.File[] arr2 = logsDir.listFiles();
                            if (arr2 != null) {
                                long best2 = -1L;
                                for (java.io.File f : arr2) {
                                    String n = f != null ? f.getName() : "";
                                    if (n.startsWith("app_logcat") && n.endsWith(".txt")) {
                                        long lm = f.lastModified();
                                        if (lm > best2) { best2 = lm; f2Legacy = f; }
                                    }
                                }
                            }
                        }
                        if (f2Legacy != null && f2Legacy.exists()) {
                            uris.add(androidx.core.content.FileProvider.getUriForFile(this, "com.lora.cn.fileprovider", f2Legacy));
                        }
                    }
                    if (uris.isEmpty()) {
                        android.widget.Toast.makeText(this, "暂无可分享日志文件", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    android.content.Intent share = new android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
                    share.setType("text/plain");
                    share.putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris);
                    share.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(android.content.Intent.createChooser(share, "分享日志文件"));
                } catch (Exception e) {
                    android.widget.Toast.makeText(this, "分享失败: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
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
            com.lora.cn.utils.DialogUtils.showRemarkDialog(this, title, "已处理", new com.lora.cn.utils.DialogUtils.OnConfirmListener() {
                @Override
                public void onConfirm(String remark) {
                    String user = com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", "");
                    String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                    try {
                        if (finalTarget.logId > 0) databaseHelper.updateLogHandled(finalTarget.logId, user, time, remark);
                        if ("设备离线".equals(finalTarget.title)) {
                            try {
                                java.util.List<com.lora.cn.ui.model.LogInfo> devLogs = databaseHelper.getLogsByTerminalId(finalTarget.code);
                                if (devLogs != null) {
                                    for (com.lora.cn.ui.model.LogInfo li : devLogs) {
                                        if (li == null) continue;
                                        boolean unhandled = (li.getHandleUser() == null || li.getHandleUser().trim().isEmpty())
                                                && (li.getHandleTime() == null || li.getHandleTime().trim().isEmpty());
                                        if (unhandled && li.getStatusCode() == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code) {
                                            databaseHelper.updateLogHandled(li.getId(), user, time, remark);
                                        }
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                        int mask = 0;
                        if ("异常取走".equals(finalTarget.title)) mask |= 0x00000001;
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
        try { updateMaintenanceBadge(); } catch (Exception ignored) {}
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
    private final java.util.Map<String, Long> lastAlertLogIds = new java.util.HashMap<>();
    private int pendingAlertCount = 0;
    private boolean alertMuted = false;
    private final java.util.Deque<AlertItem> alertQueue = new java.util.ArrayDeque<>();
    private String lastShownKey = null;
    private AlertItem currentAlert = null;
    private android.media.MediaPlayer alertPlayer;
    private final java.util.Map<String, Integer> lastAlertTypes = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> lastHandledTypes = new java.util.HashMap<>();
    private final java.util.Set<String> offlineAlertedKeys = new java.util.HashSet<>();
    private android.os.Handler alertEvaluateHandler;
    private final Runnable alertEvaluateRunnable = new Runnable() {
        @Override public void run() {
            try {
                if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
                if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                int token = alertEvalSeq.incrementAndGet();
                java.util.Map<String, Integer> typesSnapshot = new java.util.HashMap<>(lastAlertTypes);
                java.util.Map<String, Long> logIdsSnapshot = new java.util.HashMap<>(lastAlertLogIds);
                android.content.Context appCtx = getApplicationContext();
                ioExecutor.execute(() -> {
                    EvaluateResult res = null;
                    try {
                        com.lora.cn.database.DatabaseHelper db = databaseHelper != null
                                ? databaseHelper
                                : com.lora.cn.database.DatabaseHelper.getInstance(appCtx);
                        try { db.checkAndLogOfflineDevices(); } catch (Exception ignored) {}
                        res = computeAlertOverlay(db, typesSnapshot, logIdsSnapshot);
                    } catch (Exception ignored) {}
                    EvaluateResult finalRes = res;
                    if (mainHandler != null) {
                        mainHandler.post(() -> {
                            if (token != alertEvalSeq.get()) return;
                            try { applyAlertOverlayResult(finalRes); } catch (Exception ignored) {}
                        });
                    }
                });
            } finally {
                if (alertEvaluateHandler != null) alertEvaluateHandler.postDelayed(this, 1000);
            }
        }
    };

    private android.os.Handler maintenanceEvaluateHandler;
    private final Runnable maintenanceEvaluateRunnable = new Runnable() {
        @Override public void run() {
            try {
                if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
                if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                long uid = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault());
                android.content.Context appCtx = getApplicationContext();
                ioExecutor.execute(() -> {
                    try {
                        com.lora.cn.database.DatabaseHelper db = databaseHelper != null
                                ? databaseHelper
                                : com.lora.cn.database.DatabaseHelper.getInstance(appCtx);
                        java.util.List<com.lora.cn.ui.model.MaintenanceInfo> list = db.getMaintenanceRecords(uid);
                        if (list != null) {
                            com.lora.cn.network.MqttPacketsClient client = mqttClient != null ? mqttClient : com.lora.cn.network.MqttPacketsClient.getShared();
                            com.lora.cn.utils.DownlinkMessageHelper helper = new com.lora.cn.utils.DownlinkMessageHelper(client);
                            long now = System.currentTimeMillis();
                            for (com.lora.cn.ui.model.MaintenanceInfo mi : list) {
                                if (mi == null) continue;
                                if (mi.getStatus() != 0) continue;
                                String ct = mi.getCreateTime();
                                if (ct == null || ct.trim().isEmpty()) continue;
                                long ts = now;
                                try { java.util.Date dt = sdf.parse(ct.trim()); if (dt != null) ts = dt.getTime(); } catch (Exception ignored) {}
                                if (ts > now) continue;
                                if (mi.getSentFlag() == 1) continue;
                                String dev = mi.getTerminalId();
                                if (dev == null || dev.isEmpty()) continue;
                                try {
                                    int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
                                    int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
                                    int mins = Math.max(0, Math.min(1440, h * 60 + m));
                                    int interval = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
                                    helper.sendDownlink8001(
                                            dev,
                                            0,
                                            0,
                                            0,
                                            0,
                                            0,
                                            0,
                                            Math.max(3, Math.min(1440, interval)),
                                            1,
                                            new int[]{mins},
                                            true
                                    );
                                    String sentTime = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                                    db.updateMaintenanceSent(mi.getId(), sentTime);
                                    android.util.Log.i(TAG, "已下发维护提醒: dev=" + dev + " recordId=" + mi.getId() + " time=" + ct);
                                } catch (Exception e) {
                                    android.util.Log.e(TAG, "下发维护失败: " + e.getMessage());
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                });
            } finally {
                if (maintenanceEvaluateHandler != null) maintenanceEvaluateHandler.postDelayed(this, 60000);
            }
        }
    };

    private static final class AlertAction {
        final AlertItem item;
        final boolean ring;
        AlertAction(AlertItem item, boolean ring) {
            this.item = item;
            this.ring = ring;
        }
    }

    private static final class EvaluateResult {
        final java.util.ArrayList<AlertAction> actions = new java.util.ArrayList<>();
        final java.util.HashSet<String> clearDevs = new java.util.HashSet<>();
        final java.util.HashMap<String, Integer> typeUpdates = new java.util.HashMap<>();
        final java.util.HashMap<String, Long> logIdUpdates = new java.util.HashMap<>();
        boolean queuedAny;
        boolean touchedDb;
    }

    private String lastSmallKey;
    private boolean lastSmallVisible;

    private EvaluateResult computeAlertOverlay(com.lora.cn.database.DatabaseHelper db,
                                              java.util.Map<String, Integer> lastTypes,
                                              java.util.Map<String, Long> lastLogIds) {
        EvaluateResult out = new EvaluateResult();
        if (db == null) return out;
        java.util.List<com.lora.cn.ui.model.Terminal> all = null;
        try { all = db.getAllTerminals(); } catch (Exception ignored) {}
        if (all == null || all.isEmpty()) return out;
        String nowStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
        int lowTh = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);

        for (com.lora.cn.ui.model.Terminal t : all) {
            if (t == null) continue;
            String devId = t.getTerminalId();
            if (devId == null || devId.isEmpty()) continue;
            Integer last = lastTypes != null ? lastTypes.get(devId) : null;
            int status = t.getStatus();
            boolean isOffline = status == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE;
            boolean isAbnormal = status == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ABNORMAL_TAKEN;
            boolean isLow = t.getBatteryLevel() <= lowTh;

            if (isAbnormal) {
                boolean newly = last == null || last != com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code;
                long latestId = -1L;
                String latestTime = null;
                try {
                    java.util.List<com.lora.cn.ui.model.LogInfo> logs = db.getLogsByTerminalId(devId);
                    if (logs != null) {
                        for (com.lora.cn.ui.model.LogInfo li : logs) {
                            if (li != null && li.getStatusCode() == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code) {
                                latestId = li.getId();
                                latestTime = li.getCreateTime();
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
                String key = devId + ":异常取走";
                Long prev = lastLogIds != null ? lastLogIds.get(key) : null;
                boolean newLogDetected = latestId > 0 && (prev == null || prev != latestId);
                if (newLogDetected || (newly && latestId <= 0)) {
                    if (newly) {
                        AlertItem item = new AlertItem();
                        item.title = "异常取走";
                        item.name = t.getTerminalName();
                        item.code = devId;
                        item.time = latestTime != null ? latestTime : nowStr;
                        item.logId = latestId;
                        out.actions.add(new AlertAction(item, true));
                        out.queuedAny = true;
                    }
                    if (latestId > 0) out.logIdUpdates.put(key, latestId);
                    out.typeUpdates.put(devId, com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code);
                    if (newly) {
                        try {
                            db.updateTerminalStatusByDeviceId(devId, com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ABNORMAL_LOST);
                            out.touchedDb = true;
                        } catch (Exception ignored) {}
                    }
                }
                continue;
            }

            if (isOffline) {
                boolean newly = last == null || last != com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
                if (newly) {
                    try {
                        long nid = db.addOfflineLog(devId, t.getTerminalName());
                        if (nid > 0) {
                            out.logIdUpdates.put(devId + ":设备离线", nid);
                            out.touchedDb = true;
                        }
                    } catch (Exception ignored) {}
                }
                long latestId = -1L;
                String latestTime = null;
                try {
                    java.util.List<com.lora.cn.ui.model.LogInfo> logs = db.getLogsByTerminalId(devId);
                    if (logs != null) {
                        for (com.lora.cn.ui.model.LogInfo li : logs) {
                            if (li != null && li.getStatusCode() == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code) {
                                latestId = li.getId();
                                latestTime = li.getCreateTime();
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
                String key = devId + ":设备离线";
                Long prev = lastLogIds != null ? lastLogIds.get(key) : null;
                if (latestId > 0 && (prev == null || prev != latestId)) {
                    if (newly) {
                        AlertItem item = new AlertItem();
                        item.title = "设备离线";
                        item.name = t.getTerminalName();
                        item.code = devId;
                        item.time = latestTime != null ? latestTime : nowStr;
                        item.logId = latestId;
                        out.actions.add(new AlertAction(item, true));
                        out.queuedAny = true;
                    }
                    out.logIdUpdates.put(key, latestId);
                    out.typeUpdates.put(devId, com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code);
                    if (newly) {
                        try {
                            db.updateTerminalStatusByDeviceId(devId, com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_OFFLINE);
                            out.touchedDb = true;
                        } catch (Exception ignored) {}
                    }
                }
                continue;
            }

            if (isLow) {
                boolean newly = last == null || last != com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code;
                if (newly) {
                    try {
                        long nid = db.addLowBatteryLog(devId, t.getTerminalName());
                        if (nid > 0) {
                            out.logIdUpdates.put(devId + ":设备低电量", nid);
                            out.touchedDb = true;
                        }
                    } catch (Exception ignored) {}
                }
                long latestId = -1L;
                String latestTime = null;
                try {
                    java.util.List<com.lora.cn.ui.model.LogInfo> logs = db.getLogsByTerminalId(devId);
                    if (logs != null) {
                        for (com.lora.cn.ui.model.LogInfo li : logs) {
                            if (li != null && li.getStatusCode() == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code) {
                                latestId = li.getId();
                                latestTime = li.getCreateTime();
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
                String key = devId + ":设备低电量";
                Long prev = lastLogIds != null ? lastLogIds.get(key) : null;
                if (latestId > 0 && (prev == null || prev != latestId)) {
                    AlertItem item = new AlertItem();
                    item.title = "设备低电量";
                    item.name = t.getTerminalName();
                    item.code = devId;
                    item.time = latestTime != null ? latestTime : nowStr;
                    item.logId = latestId;
                    out.actions.add(new AlertAction(item, newly));
                    if (newly) out.queuedAny = true;
                    out.logIdUpdates.put(key, latestId);
                    out.typeUpdates.put(devId, com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code);
                }
                continue;
            }

            out.clearDevs.add(devId);
        }
        return out;
    }

    private void applyAlertOverlayResult(EvaluateResult result) {
        if (result == null) {
            updatePendingBadge();
            return;
        }
        int beforeQueueSize = alertQueue.size();
        boolean queueChanged = false;

        if (!result.logIdUpdates.isEmpty()) {
            lastAlertLogIds.putAll(result.logIdUpdates);
        }
        if (!result.typeUpdates.isEmpty()) {
            lastAlertTypes.putAll(result.typeUpdates);
        }
        if (!result.clearDevs.isEmpty()) {
            for (String devId : result.clearDevs) {
                if (devId == null) continue;
                java.util.Deque<AlertItem> newQueue = new java.util.ArrayDeque<>();
                for (AlertItem ai : alertQueue) {
                    boolean sameDev = devId.equalsIgnoreCase(ai.code);
                    boolean abnormal = "异常取走".equals(ai.title) || "设备低电量".equals(ai.title) || "设备离线".equals(ai.title);
                    if (!(sameDev && abnormal)) newQueue.addLast(ai);
                }
                if (newQueue.size() != alertQueue.size()) queueChanged = true;
                alertQueue.clear();
                alertQueue.addAll(newQueue);
                lastAlertTypes.remove(devId);
            }
        }

        if (!result.actions.isEmpty()) {
            String newKey = null;
            for (AlertAction a : result.actions) {
                if (a == null || a.item == null) continue;
                String devId = a.item.code;
                String title = a.item.title;
                if (devId != null && title != null) {
                    if (!existsInQueue(devId, title)) {
                        alertQueue.addLast(a.item);
                        queueChanged = true;
                        newKey = devId + ":" + title;
                    }
                    int sc = getStatusCodeForTitle(title);
                    if (sc != 0) lastAlertTypes.put(devId, sc);
                }
                if (a.ring && !alertMuted) startAlertRinging30s();
            }
            boolean smallVisible = llAlertPendingSmall != null && llAlertPendingSmall.getVisibility() == View.VISIBLE;
            if (result.queuedAny && smallVisible && (newKey == null || newKey.equals(lastSmallKey))) {
                if (llAlertPendingSmall != null) setSmallVisible(true);
                if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
                lastSmallKey = newKey;
                updatePendingBadge();
                int afterQueueSize2 = alertQueue.size();
                if (result.queuedAny || result.touchedDb || queueChanged || beforeQueueSize != afterQueueSize2) {
                    try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("离线刷新")); } catch (Exception ignored) {}
                }
                return;
            }
        }

        pendingAlertCount = alertQueue.size();
        if (result.queuedAny) {
            showLatestPending();
        } else {
            boolean hasQueue = !alertQueue.isEmpty();
            boolean bigVisible = llAlertPending != null && llAlertPending.getVisibility() == View.VISIBLE;
            if (hasQueue) {
                if (!bigVisible) {
                    boolean smallVisible = llAlertPendingSmall != null && llAlertPendingSmall.getVisibility() == View.VISIBLE;
                    String keyCandidate = null;
                    if (!alertQueue.isEmpty()) {
                        AlertItem ai = alertQueue.peekLast();
                        if (ai != null) keyCandidate = (ai.code == null ? "" : ai.code) + ":" + (ai.title == null ? "" : ai.title);
                    }
                    if (smallVisible && keyCandidate != null && keyCandidate.equals(lastSmallKey)) {
                        if (llAlertPendingSmall != null) setSmallVisible(true);
                        if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
                        lastSmallKey = keyCandidate;
                    } else {
                        showLatestPending();
                    }
                } else {
                    if (llAlertPendingSmall != null) setSmallVisible(false);
                    //LogUtils.e("llAlertPendingSmall=Visibility===8===" + (llAlertPendingSmall != null ? llAlertPendingSmall.getVisibility() : -1));
                }
            } else {
                if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
                //LogUtils.e("llAlertPendingSmall=Visibility===9===" + llAlertPendingSmall.getVisibility());
            }
        }
        updatePendingBadge();
        int afterQueueSize = alertQueue.size();
        if (result.queuedAny || result.touchedDb || queueChanged || beforeQueueSize != afterQueueSize) {
            try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("离线刷新")); } catch (Exception ignored) {}
        }
    }

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.ASYNC)
    public void onUplinkDataEvent(UplinkDataEvent event) {
        if (event == null) return;
        try {
            if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            android.content.Context appCtx = getApplicationContext();
            com.lora.cn.database.DatabaseHelper db = databaseHelper != null ? databaseHelper : com.lora.cn.database.DatabaseHelper.getInstance(appCtx);

            String hex = event.getHex();
            LoRaFrameParser.ParsedFrame frame = LoRaFrameParser.parseFrame(hex);
            if (frame == null) return;
            if (frame.deviceId == null || frame.deviceId.isEmpty()) return;

            try {
                if (!db.isTerminalExists(frame.deviceId)) return;
            } catch (Exception ignored) { return; }

            try {
                com.lora.cn.network.MqttPacketsClient client = mqttClient != null ? mqttClient : com.lora.cn.network.MqttPacketsClient.getShared();
                com.lora.cn.utils.DownlinkMessageHelper helper = new com.lora.cn.utils.DownlinkMessageHelper(client);
                java.util.List<com.lora.cn.ui.model.Terminal> terms = db.getAllTerminals();
                long depId = 0L;
                long roomId = 0L;
                if (terms != null) {
                    for (com.lora.cn.ui.model.Terminal t : terms) {
                        if (t != null && frame.deviceId.equalsIgnoreCase(t.getTerminalId())) {
                            depId = t.getDepartmentId();
                            roomId = t.getRoomId();
                            break;
                        }
                    }
                }
                int dep = (int) Math.max(0, Math.min(255, depId));
                int cart = (int) Math.max(0, Math.min(255, roomId));
                int intervalMin = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
                int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
                int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
                int mins = Math.max(0, Math.min(1440, h * 60 + m));
                boolean maintenanceNeeded = false;
                try {
                    if (frame.statusFlags != null) {
                        maintenanceNeeded = frame.statusFlags.contains(com.lora.cn.utils.LoRaFrameParser.DeviceStatusFlag.MAINTENANCE_NEEDED);
                    }
                } catch (Exception ignored) {}
                if (maintenanceNeeded) {
                    try {
                        try { db.updateTerminalMaintenanceState(frame.deviceId, true, System.currentTimeMillis()); } catch (Exception ignored) {}
                        boolean existsPendingAuto = false;
                        java.util.List<com.lora.cn.ui.model.MaintenanceInfo> existingAll = databaseHelper.getMaintenanceRecordsByTerminal(frame.deviceId, 0);
                        if (existingAll != null) {
                            for (com.lora.cn.ui.model.MaintenanceInfo x : existingAll) {
                                String c = x != null ? x.getContent() : null;
                                if ("设备维护：需要维护".equals(c) && x.getStatus() == 0) {
                                    existsPendingAuto = true;
                                    break;
                                }
                            }
                        }
                        if (!existsPendingAuto) {
                            String name = "";
                            String groups = "";
                            if (terms != null) {
                                for (com.lora.cn.ui.model.Terminal t : terms) {
                                    if (t != null && frame.deviceId.equalsIgnoreCase(t.getTerminalId())) {
                                        name = t.getTerminalName();
                                        groups = t.getGroupNamesText();
                                        break;
                                    }
                                }
                            }
                            com.lora.cn.ui.model.MaintenanceInfo mi = new com.lora.cn.ui.model.MaintenanceInfo();
                            mi.setTerminalId(frame.deviceId);
                            mi.setTerminalName(name == null ? "" : name);
                            mi.setTerminalGroup(groups == null ? "" : groups);
                            mi.setStatus(0);
                            mi.setContent("设备维护：需要维护");
                            long uid = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
                            String uname = com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", "");
                            mi.setCreateUserId(uid > 0 ? uid : 0L);
                            mi.setCreateUser(uname == null ? "" : uname);
                            mi.setCreateTime(new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                            try { databaseHelper.addMaintenanceRecord(mi); } catch (Exception ignored2) {}
                        }
                    } catch (Exception ignored) {}
                } else {
                    try {
                        try { db.updateTerminalMaintenanceState(frame.deviceId, false, System.currentTimeMillis()); } catch (Exception ignored) {}
                        java.util.List<com.lora.cn.ui.model.MaintenanceInfo> existingAll = databaseHelper.getMaintenanceRecordsByTerminal(frame.deviceId, 0);
                        if (existingAll != null) {
                            String autoUser = "系统自动";
                            String autoTime = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                            String autoRemark = "设备恢复：自动标记已维护";
                            for (com.lora.cn.ui.model.MaintenanceInfo x : existingAll) {
                                String c = x != null ? x.getContent() : null;
                                if ("设备维护：需要维护".equals(c) && x.getStatus() == 0) {
                                    try { databaseHelper.updateMaintenanceHandled(x.getId(), 0L, autoUser, autoTime, autoRemark); } catch (Exception ignored2) {}
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
                helper.sendDownlink8001(frame.deviceId, 1, 0, dep, cart, 0, 0, intervalMin, 1, new int[]{mins}, true);
            } catch (Exception ignored) {}

            int statusCode = 0;
            try {
                java.util.List<com.lora.cn.ui.model.LogInfo> logs = db.getLogsByTerminalId(frame.deviceId);
                if (logs != null && !logs.isEmpty()) statusCode = logs.get(0).getStatusCode();
            } catch (Exception ignored) {}

            int lowThEvt = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
            boolean isLowEvt = frame.batteryLevel <= lowThEvt;
            if (isLowEvt) statusCode = com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code;

            boolean isAlert = statusCode == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                    || statusCode == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code;
            String devId = frame.deviceId;
            String msg = isAlert
                    ? (statusCode == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code ? "异常取走" : "设备低电量")
                    : null;
            AlertItem computedItem = null;
            if (isAlert) {
                try {
                    computedItem = buildAlertItem(frame, msg);
                } catch (Exception ignored) {}
                if (computedItem == null) {
                    computedItem = new AlertItem();
                    computedItem.title = msg;
                    computedItem.name = "";
                    computedItem.code = devId;
                    computedItem.time = event.getTime() != null ? event.getTime() : "";
                    computedItem.logId = -1L;
                }
            }

            int finalStatusCode = statusCode;
            AlertItem finalItem = computedItem;
            String finalMsg = msg;
            mainHandler.post(() -> {
                try {
                    if (finalStatusCode == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                            || finalStatusCode == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code) {
                        Integer last = lastAlertTypes.get(devId);
                        if (last == null || last != finalStatusCode) {
                            lastShownKey = null;
                            boolean alreadyInQueue = existsInQueue(devId, finalMsg);
                            if (!alreadyInQueue && finalItem != null) {
                                alertQueue.addLast(finalItem);
                            }
                            lastAlertTypes.put(devId, finalStatusCode);
                            updatePendingBadge();
                            if (!alertMuted) startAlertRinging30s();
                            boolean bigVisible = llAlertPending != null && llAlertPending.getVisibility() == View.VISIBLE;
                            if (!bigVisible) {
                                boolean smallVisible = llAlertPendingSmall != null && llAlertPendingSmall.getVisibility() == View.VISIBLE;
                                String keyCandidate = (devId == null ? "" : devId) + ":" + (finalMsg == null ? "" : finalMsg);
                                if (smallVisible && keyCandidate.equals(lastSmallKey)) {
                                    if (llAlertPendingSmall != null) setSmallVisible(true);
                                    if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
                                    lastSmallKey = keyCandidate;
                                } else {
                                    showLatestPending();
                                }
                            } else {
                                if (llAlertPendingSmall != null) setSmallVisible(false);
                                //LogUtils.e("llAlertPendingSmall=Visibility===10===" + llAlertPendingSmall.getVisibility());
                            }
                        } else {
                            updatePendingBadge();
                            if (llAlertPendingSmall != null && pendingAlertCount > 0) setSmallVisible(true);
                            //LogUtils.e("llAlertPendingSmall=Visibility===11===" + llAlertPendingSmall.getVisibility());
                        }
                    } else {
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
                                // 不主动在此隐藏小窗，让徽标逻辑决定是否展示，避免闪烁
                                //LogUtils.e("llAlertPendingSmall=Visibility===12===" + llAlertPendingSmall.getVisibility());
                            } else {
                                boolean smallVisible = llAlertPendingSmall != null && llAlertPendingSmall.getVisibility() == View.VISIBLE;
                                String keyCandidate = (devId == null ? "" : devId) + ":" + (finalMsg == null ? "" : finalMsg);
                                if (smallVisible && keyCandidate.equals(lastSmallKey)) {
                                    if (llAlertPendingSmall != null) setSmallVisible(true);
                                    if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
                                    lastSmallKey = keyCandidate;
                                } else {
                                    showLatestPending();
                                }
                            }
                        }
                    }
                    try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("状态刷新")); } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    private boolean existsInQueue(String devId, String title) {
        if (devId == null) return false;
        for (AlertItem ai : alertQueue) {
            if (ai == null) continue;
            if (devId.equalsIgnoreCase(ai.code) && title.equals(ai.title)) return true;
        }
        return false;
    }

    private void showLatestPending() {
        currentAlert = alertQueue.peekLast();
        if (currentAlert == null) return;
        String key = (currentAlert.code == null ? "" : currentAlert.code) + ":" + (currentAlert.title == null ? "" : currentAlert.title);
        if (lastShownKey != null && lastShownKey.equals(key)) {
            updatePendingBadge();
            if (llAlertPendingSmall != null) setSmallVisible(false);
            //LogUtils.e("llAlertPendingSmall=Visibility===13===" + llAlertPendingSmall.getVisibility());
            if (llAlertPending != null) llAlertPending.setVisibility(View.VISIBLE);
            return;
        }
        if (tvErrorTitle != null) tvErrorTitle.setText(currentAlert.title);
        if (tvErrorName != null) tvErrorName.setText(currentAlert.name);
        if (tvErrorCode != null) tvErrorCode.setText(currentAlert.code);
        if (tvErrorTime != null) tvErrorTime.setText(currentAlert.time);
        updatePendingBadge();
        if (llAlertPendingSmall != null) setSmallVisible(false);
        //LogUtils.e("llAlertPendingSmall=Visibility===14===" + llAlertPendingSmall.getVisibility());
        if (llAlertPending != null) llAlertPending.setVisibility(View.VISIBLE);
        int sc = getStatusCodeForTitle(currentAlert.title);
        Integer handled = lastHandledTypes.get(currentAlert.code);
        boolean needConfirm = handled == null || handled != sc;
        if (tvErrorComplete != null) tvErrorComplete.setVisibility(needConfirm ? View.VISIBLE : View.VISIBLE);
        refreshHandledStatusFromDBAsync(currentAlert.code, sc);
        lastShownKey = key;
        lastSmallKey = null;
    }

    private void minimizePending() {
        if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
        if (llAlertPendingSmall != null) setSmallVisible(true);
        if (llAlertPendingSmall != null) {
            llAlertPendingSmall.post(() -> {
                android.view.View parent = (android.view.View) llAlertPendingSmall.getParent();
                if (parent != null) {
                    int parentWidth = parent.getWidth();
                    int parentHeight = parent.getHeight();
                    int vWidth = llAlertPendingSmall.getWidth();
                    int vHeight = llAlertPendingSmall.getHeight();
                    float x = llAlertPendingSmall.getX();
                    float y = llAlertPendingSmall.getY();
                    if (parentWidth > 0 && parentHeight > 0 && vWidth > 0 && vHeight > 0) {
                        float nx = Math.max(0, Math.min(parentWidth - vWidth, x));
                        float ny = Math.max(0, Math.min(parentHeight - vHeight, y));
                        llAlertPendingSmall.setX(nx);
                        llAlertPendingSmall.setY(ny);
                    }
                    llAlertPendingSmall.bringToFront();
                }
            });
        }
        if (currentAlert != null) {
            String key = (currentAlert.code == null ? "" : currentAlert.code) + ":" + (currentAlert.title == null ? "" : currentAlert.title);
            lastSmallKey = key;
        } else if (!alertQueue.isEmpty()) {
            AlertItem ai = alertQueue.peekLast();
            if (ai != null) {
                String key = (ai.code == null ? "" : ai.code) + ":" + (ai.title == null ? "" : ai.title);
                lastSmallKey = key;
            }
        }
        updatePendingBadge();
    }

    private void expandPending() {
//        if (!alertQueue.isEmpty()) {
//            showLatestPending();
//        } else {
//            if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
//            if (llAlertPendingSmall != null) setSmallVisible(false);
//        }
        openAlertPendingList();
    }

    private void handleCurrentAlert() {
        if (currentAlert != null) {
            alertQueue.remove(currentAlert);
            lastShownKey = null;
            try { lastHandledTypes.put(currentAlert.code, getStatusCodeForTitle(currentAlert.title)); } catch (Exception ignored) {}
            currentAlert = null;
        } else if (!alertQueue.isEmpty()) {
            alertQueue.removeLast();
        }
        pendingAlertCount = alertQueue.size();
        if (!alertQueue.isEmpty()) {
            if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
            if (llAlertPendingSmall != null) setSmallVisible(true);
            //LogUtils.e("llAlertPendingSmall=Visibility===2===" + llAlertPendingSmall.getVisibility());
            updatePendingBadge();
        } else {
            if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
            if (llAlertPendingSmall != null) setSmallVisible(false);
            //LogUtils.e("llAlertPendingSmall=Visibility===3===" + llAlertPendingSmall.getVisibility());
        }
    }

    private int getStatusCodeForTitle(String title) {
        if ("异常取走".equals(title)) return com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code;
        if ("设备低电量".equals(title)) return com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code;
        if ("设备离线".equals(title)) return com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
        return 0;
    }

    private long parseMillis(String time) {
        if (time == null || time.length() == 0) return -1L;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date d = sdf.parse(time);
            return d != null ? d.getTime() : -1L;
        } catch (Exception e) { return -1L; }
    }

    private void refreshHandledStatusFromDB(String devId) {
        if (devId == null) return;
        try {
            com.lora.cn.database.DatabaseHelper db = databaseHelper != null
                    ? databaseHelper
                    : com.lora.cn.database.DatabaseHelper.getInstance(getApplicationContext());
            java.util.List<com.lora.cn.ui.model.LogInfo> logs = db.getLogsByTerminalId(devId);
            if (logs != null && !logs.isEmpty()) {
                for (com.lora.cn.ui.model.LogInfo li : logs) {
                    String hu = li.getHandleUser();
                    if (hu != null && !hu.trim().isEmpty()) {
                        lastHandledTypes.put(devId, li.getStatusCode());
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void refreshHandledStatusFromDBAsync(String devId, int currentStatusCode) {
        if (devId == null) return;
        try {
            if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            android.content.Context appCtx = getApplicationContext();
            ioExecutor.execute(() -> {
                Integer handledCode = null;
                try {
                    com.lora.cn.database.DatabaseHelper db = databaseHelper != null ? databaseHelper : com.lora.cn.database.DatabaseHelper.getInstance(appCtx);
                    java.util.List<com.lora.cn.ui.model.LogInfo> logs = db.getLogsByTerminalId(devId);
                    if (logs != null) {
                        for (com.lora.cn.ui.model.LogInfo li : logs) {
                            String hu = li.getHandleUser();
                            if (hu != null && !hu.trim().isEmpty()) {
                                handledCode = li.getStatusCode();
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
                Integer finalHandledCode = handledCode;
                mainHandler.post(() -> {
                    try {
                        if (finalHandledCode != null) lastHandledTypes.put(devId, finalHandledCode);
                        boolean stillCurrent = currentAlert != null && devId.equalsIgnoreCase(currentAlert.code);
                        if (!stillCurrent) return;
                        Integer handled = lastHandledTypes.get(devId);
                        boolean needConfirm = handled == null || handled != currentStatusCode;
                        if (tvErrorComplete != null) tvErrorComplete.setVisibility(needConfirm ? View.VISIBLE : View.VISIBLE);
                    } catch (Exception ignored) {}
                });
            });
        } catch (Exception ignored) {}
    }

    public void updatePendingBadge() {
        try {
            Integer override = pendingCountOverride;
            if (override != null) {
                long nowTs = System.currentTimeMillis();
                long age = nowTs - pendingCountOverrideTs;
                if (age <= 3000) {
                    applyPendingBadgeUi(override);
                    return;
                } else {
                    pendingCountOverride = null;
                }
            }
            if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            long now = android.os.SystemClock.uptimeMillis();
            long delta = now - lastBadgeRequestMs;
            if (delta < 800) {
                long delay = 800 - delta;
                if (!badgeRequestDelayed) {
                    badgeRequestDelayed = true;
                    mainHandler.removeCallbacks(badgeRequestRunnable);
                    mainHandler.postDelayed(badgeRequestRunnable, delay);
                }
                return;
            }
            lastBadgeRequestMs = now;
            android.content.Context appCtx = getApplicationContext();
            int token = badgeSeq.incrementAndGet();
            ioExecutor.execute(() -> {
                int count = 0;
                try {
                    com.lora.cn.database.DatabaseHelper db = databaseHelper != null ? databaseHelper : com.lora.cn.database.DatabaseHelper.getInstance(appCtx);
                    try { db.syncLowBatteryFlags(); } catch (Exception ignored) {}
                    java.util.List<com.lora.cn.ui.model.LogInfo> all = db.getAllLogsBoundToTerminals();
                    java.util.Map<String, com.lora.cn.ui.model.LogInfo> latest = new java.util.HashMap<>();
                    for (com.lora.cn.ui.model.LogInfo li : all) {
                        if (li == null) continue;
                        int s = li.getStatusCode();
                        boolean candidate = s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                                || s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code
                                || s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
                        if (!candidate) continue;
                        String key = (li.getTerminalId() == null ? "" : li.getTerminalId()) + ":" + s;
                        com.lora.cn.ui.model.LogInfo prev = latest.get(key);
                        long prevT = prev != null ? parseMillis(prev.getCreateTime()) : -1L;
                        long curT = parseMillis(li.getCreateTime());
                        if (prev == null || curT >= prevT) latest.put(key, li);
                    }
                    java.util.Map<String, Long> lastHandledTime = new java.util.HashMap<>();
                    for (com.lora.cn.ui.model.LogInfo li : all) {
                        if (li == null) continue;
                        String hu = li.getHandleUser();
                        String htStr = li.getHandleTime();
                        if ((hu != null && !hu.trim().isEmpty()) || (htStr != null && !htStr.trim().isEmpty())) {
                            long t = parseMillis(li.getCreateTime());
                            String key = li.getTerminalId();
                            Long prev = lastHandledTime.get(key);
                            if (prev == null || t >= prev) lastHandledTime.put(key, t);
                        }
                    }
                    java.util.Map<String, com.lora.cn.ui.model.Terminal> terminalById = new java.util.HashMap<>();
                    java.util.List<com.lora.cn.ui.model.Terminal> allTerms = db.getAllTerminals();
                    if (allTerms != null) {
                        for (com.lora.cn.ui.model.Terminal t : allTerms) {
                            if (t == null) continue;
                            terminalById.put(t.getTerminalId(), t);
                        }
                    }
                    int lowTh = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
                    int c = 0;
                    for (com.lora.cn.ui.model.LogInfo li : latest.values()) {
                        if (li == null) continue;
                        Long ht = lastHandledTime.get(li.getTerminalId());
                        long at = parseMillis(li.getCreateTime());
                        int s = li.getStatusCode();
                        if (s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code) {
                            com.lora.cn.ui.model.Terminal t = terminalById.get(li.getTerminalId());
                            if (t != null) {
                                boolean isLowNow = t.getBatteryLevel() <= lowTh;
                                boolean afterHandled = ht == null || at > ht;
                                if (isLowNow && afterHandled) c++;
                            }
                        } else {
                            com.lora.cn.ui.model.Terminal t = terminalById.get(li.getTerminalId());
                            boolean isOfflineCase = s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
                            boolean devStillOffline = t != null && t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE;
                            boolean afterHandled = ht == null || at > ht;
                            if (isOfflineCase) {
                                if (devStillOffline && afterHandled) c++;
                            } else {
                                if (afterHandled) c++;
                            }
                        }
                    }
                    count = c;
                } catch (Exception ignored) {}
                int finalCount = count;
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        if (token != badgeSeq.get()) return;
                        applyPendingBadgeUi(finalCount);
                    });
                }
            });
        } catch (Exception e) {
            android.util.Log.e(TAG, "更新待处理徽标失败", e);
        }
    }

    private void applyPendingBadgeUi(int count) {
        pendingAlertCount = count;
        if (tvErrorNumber != null) tvErrorNumber.setText(String.valueOf(count));
        if (llAlertPendingSmall != null) {
            boolean bigVisible = llAlertPending != null && llAlertPending.getVisibility() == View.VISIBLE;
            int queueSizeLocal = alertQueue != null ? alertQueue.size() : 0;
            boolean hasTypes = lastAlertTypes != null && !lastAlertTypes.isEmpty();
            boolean shouldShowSmall = !bigVisible && (count > 0 || queueSizeLocal > 0 || hasTypes);
            setSmallVisible(shouldShowSmall);
            boolean currentSmall = llAlertPendingSmall.getVisibility() == View.VISIBLE;
            //LogUtils.e("llAlertPendingSmall=Visibility===4===" + shouldShowSmall + "=======" + (currentSmall ? View.VISIBLE : View.GONE));
        }
        int queueSize = alertQueue.size();
        if (count != lastBadgeCount || queueSize != lastBadgeQueueSize) {
            lastBadgeCount = count;
            lastBadgeQueueSize = queueSize;
            android.util.Log.d(TAG, "updatePendingBadge count=" + count + ", queueSize=" + queueSize);
        }
    }

    private void setSmallVisible(boolean show) {
        if (llAlertPendingSmall == null) return;
        boolean currently = llAlertPendingSmall.getVisibility() == View.VISIBLE;
        if (show == currently) return;
        llAlertPendingSmall.setVisibility(show ? View.VISIBLE : View.GONE);
        lastSmallVisible = show;
        if (show) {
            llAlertPendingSmall.post(() -> {
                android.view.View parent = (android.view.View) llAlertPendingSmall.getParent();
                if (parent != null) {
                    int parentWidth = parent.getWidth();
                    int parentHeight = parent.getHeight();
                    int vWidth = llAlertPendingSmall.getWidth();
                    int vHeight = llAlertPendingSmall.getHeight();
                    float x = llAlertPendingSmall.getX();
                    float y = llAlertPendingSmall.getY();
                    if (parentWidth > 0 && parentHeight > 0 && vWidth > 0 && vHeight > 0) {
                        float nx = Math.max(0, Math.min(parentWidth - vWidth, x));
                        float ny = Math.max(0, Math.min(parentHeight - vHeight, y));
                        llAlertPendingSmall.setX(nx);
                        llAlertPendingSmall.setY(ny);
                    }
                    llAlertPendingSmall.bringToFront();
                }
            });
            if (!alertQueue.isEmpty()) {
                AlertItem ai = alertQueue.peekLast();
                if (ai != null) {
                    String key = (ai.code == null ? "" : ai.code) + ":" + (ai.title == null ? "" : ai.title);
                    lastSmallKey = key;
                }
            }
        }
    }

    private void updateMaintenanceBadge() {
        try {
            if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            long uid = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
            ioExecutor.execute(() -> {
                int count = 0;
                try {
                    com.lora.cn.database.DatabaseHelper db = databaseHelper != null ? databaseHelper : com.lora.cn.database.DatabaseHelper.getInstance(getApplicationContext());
                    java.util.List<com.lora.cn.ui.model.MaintenanceInfo> list = db.getMaintenanceRecords(uid);
                    java.util.Set<String> pendingDevSet = new java.util.HashSet<>();
                    if (list != null) {
                        for (com.lora.cn.ui.model.MaintenanceInfo mi : list) {
                            if (mi == null) continue;
                            String c = mi.getContent();
                            if ("设备维护：需要维护".equals(c) && mi.getStatus() == 0) {
                                String dev = mi.getTerminalId();
                                if (dev != null && dev.length() > 0) pendingDevSet.add(dev);
                            }
                        }
                    }
                    count = pendingDevSet.size();
                } catch (Exception ignored) {}
                final int finalCount = count;
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        if (btnMaintenanceBadge != null) btnMaintenanceBadge.setText("需维修: " + finalCount);
                    });
                }
            });
        } catch (Exception e) {
            android.util.Log.e(TAG, "更新需维修数量失败", e);
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
            pendingCountOverride = null;
            updatePendingBadge();
        } catch (Exception ignored) {}
    }

    private AlertItem buildAlertItem(LoRaFrameParser.ParsedFrame frame, String msg) {
        String name = "";
        String code = frame != null ? frame.deviceId : "";
        String time = "";
        long logId = -1L;
        try {
            com.lora.cn.database.DatabaseHelper db = databaseHelper != null
                    ? databaseHelper
                    : com.lora.cn.database.DatabaseHelper.getInstance(getApplicationContext());
            List<com.lora.cn.ui.model.Terminal> terminals = db.getAllTerminals();
            if (terminals != null) {
                for (com.lora.cn.ui.model.Terminal t : terminals) {
                    if (t.getTerminalId() != null && t.getTerminalId().equalsIgnoreCase(code)) {
                        name = t.getTerminalName();
                        break;
                    }
                }
            }
            List<com.lora.cn.ui.model.LogInfo> logs = db.getLogsByTerminalId(code);
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
            int beforeQueueSize = alertQueue.size();
            boolean touchedDb = false;
            java.util.List<com.lora.cn.ui.model.Terminal> all = databaseHelper.getAllTerminals();
            boolean queuedAny = false;
            for (com.lora.cn.ui.model.Terminal t : all) {
                String devId = t.getTerminalId();
                Integer last = lastAlertTypes.get(devId);
                int status = t.getStatus();
                boolean isOffline = status == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE;
                boolean isAbnormal = status == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ABNORMAL_TAKEN;
                int lowTh = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
                boolean isLow = t.getBatteryLevel() <= lowTh;
                if (isAbnormal) {
                    boolean newly = last == null || last != com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code;
                    java.util.List<com.lora.cn.ui.model.LogInfo> logs = databaseHelper.getLogsByTerminalId(devId);
                    long latestId = -1L; String latestTime = null;
                    if (logs != null) {
                        for (com.lora.cn.ui.model.LogInfo li : logs) {
                            if (li.getStatusCode() == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code) { latestId = li.getId(); latestTime = li.getCreateTime(); break; }
                        }
                    }
                    String key = devId + ":异常取走";
                    Long prev = lastAlertLogIds.get(key);
                boolean newLogDetected = latestId > 0 && (prev == null || prev != latestId);
                if (newLogDetected || (newly && latestId <= 0)) {
                    if (newly) {
                        AlertItem item = new AlertItem(); item.title = "异常取走"; item.name = t.getTerminalName(); item.code = devId; item.time = latestTime != null ? latestTime : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                        if (!existsInQueue(devId, item.title)) alertQueue.addLast(item);
                        pendingAlertCount = alertQueue.size();
                        startAlertRinging30s(); queuedAny = true;
                    }
                    if (latestId > 0) lastAlertLogIds.put(key, latestId);
                    lastAlertTypes.put(devId, com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code);
                    try {
                        if (newly) {
                            databaseHelper.updateTerminalStatusByDeviceId(devId, com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ABNORMAL_LOST);
                            touchedDb = true;
                            }
                        } catch (Exception ignored) {}
                    }
                } else if (isOffline) {
                    boolean newly = last == null || last != com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
                    if (newly) {
                        try {
                            long nid = databaseHelper.addOfflineLog(devId, t.getTerminalName());
                            if (nid > 0) {
                                String nowStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                                String key = devId + ":设备离线";
                                lastAlertLogIds.put(key, nid);
                                touchedDb = true;
                            }
                        } catch (Exception ignored) {}
                    }
                    java.util.List<com.lora.cn.ui.model.LogInfo> logs = databaseHelper.getLogsByTerminalId(devId);
                    long latestId = -1L; String latestTime = null;
                    if (logs != null) {
                        for (com.lora.cn.ui.model.LogInfo li : logs) {
                            if (li.getStatusCode() == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code) { latestId = li.getId(); latestTime = li.getCreateTime(); break; }
                        }
                    }
                    String key = devId + ":设备离线";
                    Long prev = lastAlertLogIds.get(key);
                    if (latestId > 0 && (prev == null || prev != latestId)) {
                        if (newly) {
                            AlertItem item = new AlertItem(); item.title = "设备离线"; item.name = t.getTerminalName(); item.code = devId; item.time = latestTime != null ? latestTime : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                            if (!existsInQueue(devId, item.title)) alertQueue.addLast(item);
                            pendingAlertCount = alertQueue.size();
                            startAlertRinging30s(); queuedAny = true;
                        }
                        lastAlertLogIds.put(key, latestId);
                        lastAlertTypes.put(devId, com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code);
                        try {
                            if (newly) {
                                databaseHelper.updateTerminalStatusByDeviceId(devId, com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_OFFLINE);
                                touchedDb = true;
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (isLow) {
                    boolean newly = last == null || last != com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code;
                    if (newly) {
                        try {
                            long nid = databaseHelper.addLowBatteryLog(devId, t.getTerminalName());
                            if (nid > 0) {
                                String keyIns = devId + ":设备低电量";
                                lastAlertLogIds.put(keyIns, nid);
                                touchedDb = true;
                            }
                        } catch (Exception ignored) {}
                    }
                    java.util.List<com.lora.cn.ui.model.LogInfo> logs = databaseHelper.getLogsByTerminalId(devId);
                    long latestId = -1L; String latestTime = null;
                    if (logs != null) {
                        for (com.lora.cn.ui.model.LogInfo li : logs) {
                            if (li.getStatusCode() == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code) { latestId = li.getId(); latestTime = li.getCreateTime(); break; }
                        }
                    }
                    String key = devId + ":设备低电量";
                    Long prev = lastAlertLogIds.get(key);
                    if (latestId > 0 && (prev == null || prev != latestId)) {
                        AlertItem item = new AlertItem(); item.title = "设备低电量"; item.name = t.getTerminalName(); item.code = devId; item.time = latestTime != null ? latestTime : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                        if (!existsInQueue(devId, item.title)) alertQueue.addLast(item);
                        lastAlertLogIds.put(key, latestId);
                        lastAlertTypes.put(devId, com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code);
                        pendingAlertCount = alertQueue.size();
                        if (newly) { startAlertRinging30s(); queuedAny = true; }
                    }
                }
                if (!isAbnormal && !isOffline && !isLow) {
                    lastAlertTypes.remove(devId);
                }
            }
        if (queuedAny) {
            boolean smallVisible = llAlertPendingSmall != null && llAlertPendingSmall.getVisibility() == View.VISIBLE;
            String keyCandidate = null;
            if (!alertQueue.isEmpty()) {
                AlertItem ai = alertQueue.peekLast();
                if (ai != null) keyCandidate = (ai.code == null ? "" : ai.code) + ":" + (ai.title == null ? "" : ai.title);
            }
            if (smallVisible && keyCandidate != null && keyCandidate.equals(lastSmallKey)) {
                if (llAlertPendingSmall != null) setSmallVisible(true);
                if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
                lastSmallKey = keyCandidate;
            } else {
                showLatestPending();
            }
        } else {
            boolean bigVisible = llAlertPending != null && llAlertPending.getVisibility() == View.VISIBLE;
            if (!alertQueue.isEmpty()) {
                if (!bigVisible) {
                    showLatestPending();
                } else {
                    if (llAlertPendingSmall != null) setSmallVisible(false);
                    //LogUtils.e("llAlertPendingSmall=Visibility===5===" + (llAlertPendingSmall != null ? llAlertPendingSmall.getVisibility() : -1));
                }
            } else {
                if (llAlertPendingSmall != null) setSmallVisible(false);
                //LogUtils.e("llAlertPendingSmall=Visibility===5===" + llAlertPendingSmall.getVisibility());
            }
        }
            updatePendingBadge();
            int afterQueueSize = alertQueue.size();
            if (queuedAny || touchedDb || beforeQueueSize != afterQueueSize) {
                try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("离线刷新")); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private void openAlertPendingList() {
        try {
            androidx.fragment.app.Fragment fragment = new com.lora.cn.ui.fragment.AlertPendingListFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_device_list_container, fragment)
                    .addToBackStack("alert_pending")
                    .commit();
            setSmallVisible(true);
            llAlertPending.setVisibility(View.GONE);

            fragmentDeviceListContainer.setVisibility(View.VISIBLE);
            rvMenuTabs.setVisibility(View.INVISIBLE);
            viewPager.setVisibility(View.GONE);

            //LogUtils.e("llAlertPendingSmall=Visibility===6===" + llAlertPendingSmall.getVisibility());
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
            ringHandler.postDelayed(ringStopRunnable, 10000);
        } catch (Exception ignored) {}
    }
    public void stopAlertRinging() {
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

    public void showMaintenanceTab() {
        try {
            hideDeviceList();
        } catch (Exception ignored) {}
        viewPager.setCurrentItem(3, false);
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

        fragmentDeviceListContainer.setVisibility(View.VISIBLE);
        rvMenuTabs.setVisibility(View.INVISIBLE);
        viewPager.setVisibility(View.GONE);
        isDeviceListVisible = true;
        lastNonHomeStartMs = System.currentTimeMillis();

        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        try {
            int count = fm.getBackStackEntryCount();
            for (int i = count - 1; i >= 0; i--) {
                androidx.fragment.app.FragmentManager.BackStackEntry e = fm.getBackStackEntryAt(i);
                if ("device_list".equals(e.getName())) {
                    fm.popBackStackImmediate("device_list", 0);
                    return;
                }
            }
        } catch (Exception ignored) {}

        androidx.fragment.app.Fragment current = fm.findFragmentById(R.id.fragment_device_list_container);
        if (current instanceof com.lora.cn.ui.fragment.DeviceListFragment) return;

        com.lora.cn.ui.fragment.DeviceListFragment fragment = com.lora.cn.ui.fragment.DeviceListFragment.newInstance();
        fm.beginTransaction()
                .replace(R.id.fragment_device_list_container, fragment)
                .addToBackStack("device_list")
                .commit();
    }

    public void hideDeviceList() {
        if (!isDeviceListVisible) {
            return;
        }

        fragmentDeviceListContainer.setVisibility(View.GONE);
        viewPager.setVisibility(View.VISIBLE);
        rvMenuTabs.setVisibility(View.VISIBLE);
        isDeviceListVisible = false;

        try {
            getSupportFragmentManager().popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } catch (Exception ignored) {}

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
            if (mqttClient == null) mqttClient = com.lora.cn.network.MqttPacketsClient.getShared();
            com.blankj.utilcode.util.SPUtils sp = com.blankj.utilcode.util.SPUtils.getInstance();
            int localPort = sp.getInt("mqtt_local_broker_port", 1883);
            String brokerUrl = "tcp://127.0.0.1:" + (localPort > 0 ? localPort : 1883);
            boolean readyFlag = sp.getBoolean("mqtt_local_broker_ready", false);
            android.util.Log.i(TAG, "准备连接MQTT: brokerUrl=" + brokerUrl + ", readyFlag=" + readyFlag + ", attempt=" + mqttReadyRetry);
            if (!isLocalPortOpen(localPort)) {
                android.util.Log.w(TAG, "本地端口未打开，延迟重试: port=" + localPort + ", attempt=" + mqttReadyRetry);
                if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                if (mqttReadyRetry < 40) {
                    mqttReadyRetry++;
                    mainHandler.postDelayed(this::startGlobalMqttLogging, 500);
                    return;
                }
            }
            android.util.Log.i(TAG, "使用本地MQTT Broker: " + brokerUrl);
            final String clientId = "android-main";
            String topicFilter = sp.getString("mqtt_topic_filter", "/milesight/uplink/#");
            String username = sp.getString("mqtt_username", "");
            String password = sp.getString("mqtt_password", "");
            boolean trustAll = sp.getBoolean("mqtt_trust_all_certs", false);
            android.util.Log.i(TAG, "开始连接MQTT: clientId=" + clientId + ", topicFilter=" + topicFilter);
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
                            
                            boolean wroteAny = false;
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
                                    com.lora.cn.utils.LogUtils.i(TAG, "准备入库上行数据 hex=" + hex);
                                    // 存储到上行数据日志表
                                    long result = databaseHelper.addUplinkLog(hex);
                                    Log.d(TAG, "上行数据存储到上行日志表，结果: " + result);
                                    com.lora.cn.utils.LogUtils.i(TAG, "上行数据入库结果: " + result);
                                    wroteAny = true;
                                    
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
                                com.lora.cn.utils.LogUtils.i(TAG,
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
                            if (wroteAny) {
                                try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("已入库刷新")); } catch (Exception ignored) {}
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

//    /**
//     * 提供下行发送（方式1：通用主题，devEUI在消息体中）
//     */
//    public void sendDownlinkSimple(String devEui, String payloadHex, int fport, boolean confirmed) {
//        try {
//            if (mqttClient == null) {
//                mqttClient = new MqttPacketsClient();
//                Log.w(TAG, "MQTT客户端未初始化，已创建实例但未连接");
//            }
//            mqttClient.publishDownlinkSimple("/milesight/downlink", devEui, payloadHex, fport, confirmed);
//            Log.i(TAG, "DOWNLINK(simple) devEUI=" + devEui + " fport=" + fport + " hex=" + payloadHex + " confirmed=" + confirmed);
//        } catch (Exception e) {
//            Log.e(TAG, "下行发送失败(simple)：" + e.getMessage());
//        }
//    }

//    /**
//     * 提供下行发送（方式2：按设备主题，devEUI在主题路径中）
//     */
//    public void sendDownlinkByDevEuiTopic(String devEui, String payloadHex, int fport, boolean confirmed) {
//        try {
//            if (mqttClient == null) {
//                mqttClient = new MqttPacketsClient();
//                Log.w(TAG, "MQTT客户端未初始化，已创建实例但未连接");
//            }
//            mqttClient.publishDownlinkByDevEuiTopic("/milesight/downlink", devEui, payloadHex, fport, confirmed);
//            Log.i(TAG, "DOWNLINK(by-topic) devEUI=" + devEui + " fport=" + fport + " hex=" + payloadHex + " confirmed=" + confirmed);
//        } catch (Exception e) {
//            Log.e(TAG, "下行发送失败(by-topic)：" + e.getMessage());
//        }
//    }

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
            try {
                if (ioExecutor != null) ioExecutor.shutdownNow();
            } catch (Exception ignored) {}
            ioExecutor = null;
            mainHandler = null;
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

    private boolean isLocalPortOpen(int port) {
        try {
            java.net.Socket s = new java.net.Socket();
            s.connect(new java.net.InetSocketAddress("127.0.0.1", port > 0 ? port : 1883), 200);
            try { s.close(); } catch (Exception ignored) {}
            return true;
        } catch (Exception e) {
            return false;
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
            long seq = SPUtils.getInstance().getLong("uplink_test_seq", 1L);
            SPUtils.getInstance().put("uplink_test_seq", seq + 1L);

            String devEui = String.format(Locale.US, "28E200010003%04X", (int) (seq & 0xFFFF));
            String tpl = "A528E2000100032509000100001820250926080808000000080000007E017261740000000000CF5A";
            return tpl.substring(0, 2) + devEui + tpl.substring(18);
        } catch (Exception e) {
            return "A528E2000100030001000100001820250926080808000000080000007E017261740000000000CF5A";
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
                int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
                int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
                int mins = Math.max(0, Math.min(1440, h * 60 + m));
                int interval = Math.max(3, Math.min(1440, com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3)));
                helper.sendDownlink8001(
                        devHex,
                        1,
                        1,
                        0,
                        0,
                        0,
                        mask,
                        interval,
                        1,
                        new int[]{mins},
                        true
                );
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "下发处理下行失败 devEUI=" + devHex + ", mask=" + mask, e);
        }
    }
}
