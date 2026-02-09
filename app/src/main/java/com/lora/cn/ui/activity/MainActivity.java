package com.lora.cn.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.StringUtils;
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
    private static final int MQTT_STATE_CONNECTING = 0;
    private static final int MQTT_STATE_CONNECTED = 1;
    private static final int MQTT_STATE_STOPPED = 2;

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
    private android.view.View mqttStatusDot;
    private android.view.animation.Animation mqttDotBlinkAnim;
    private volatile int mqttUiState = MQTT_STATE_STOPPED;
    private final java.util.concurrent.atomic.AtomicBoolean mqttConnectInFlight = new java.util.concurrent.atomic.AtomicBoolean(false);
    
    private int currentTabIndex = 0;
    private boolean isUserInfoVisible = false;
    private boolean isDeviceListVisible = false;

    private MqttPacketsClient mqttClient;
    private DatabaseHelper databaseHelper;
    private java.util.concurrent.ExecutorService ioExecutor;
    private android.os.Handler mainHandler;
    private final java.util.concurrent.atomic.AtomicInteger badgeSeq = new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger alertEvalSeq = new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.ConcurrentHashMap<String, Long> lastUplinkStoreByDevMs = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Long> lastMaintenanceEvalByDevMs = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, SleepCycleState> sleepCycleStateByDev = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile long lastAlertEvalTriggerMs = 0L;
    private long appStartMs = 0L;
    private final java.util.concurrent.atomic.AtomicBoolean navInFlight = new java.util.concurrent.atomic.AtomicBoolean(false);
    private int lastBadgeCount = -1;
    private int lastBadgeQueueSize = -1;
    private volatile int lastComputedPendingCount = -1;
    private long lastBadgeRequestMs = 0L;
    private boolean badgeRequestDelayed = false;
    private int mqttReadyRetry = 0;
    private int mqttConnectRetry = 0;
    private final Runnable badgeRequestRunnable = new Runnable() {
        @Override
        public void run() {
            badgeRequestDelayed = false;
            updatePendingBadge();
        }
    };
    private static final long TEST_INTERVAL = 10 * 1000; // 30秒
    private android.content.BroadcastReceiver mqttStateReceiver;
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
//                    try {
//                        com.lora.cn.database.DatabaseHelper db = databaseHelper != null
//                                ? databaseHelper
//                                : com.lora.cn.database.DatabaseHelper.getInstance(appCtx);
//                        long result = db.addUplinkLog(hex);
//                        Log.d(TAG, "自动测试上行写入日志库结果: " + result);
//                        EventBus.getDefault().post(new UplinkDataEvent(time, hex));
//                    } catch (Exception e) {
//                        Log.e(TAG, "自动测试上行失败", e);
//                    }
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

    private static class PendingUplink {
        final String devEui;
        final String devAddr;
        final String hex;
        final String dr;
        final String time;
        final String freq;
        final String rssi;
        final String snr;
        final String fport;
        final String fcnt;
        PendingUplink(String devEui, String devAddr, String hex, String dr, String time, String freq, String rssi, String snr, String fport, String fcnt) {
            this.devEui = devEui;
            this.devAddr = devAddr;
            this.hex = hex;
            this.dr = dr;
            this.time = time;
            this.freq = freq;
            this.rssi = rssi;
            this.snr = snr;
            this.fport = fport;
            this.fcnt = fcnt;
        }
    }

    private static class SleepCycleState {
        long cycleStartMs;
        boolean firstDropped;
        SleepCycleState(long cycleStartMs, boolean firstDropped) {
            this.cycleStartMs = cycleStartMs;
            this.firstDropped = firstDropped;
        }
    }

    private String normalizeDeviceKey(String devEui, String hex) {
        String k = devEui != null ? devEui.trim() : "";
        if (!k.isEmpty() && !"-".equals(k) && !" -".equals(k)) return k;
        try {
            com.lora.cn.utils.LoRaFrameParser.ParsedFrame f = com.lora.cn.utils.LoRaFrameParser.parseFrame(hex);
            String did = f != null ? f.deviceId : null;
            if (did != null && !did.trim().isEmpty()) return did.trim();
        } catch (Exception ignored) {}
        return k.isEmpty() ? (hex == null ? "" : hex) : k;
    }

    private void handleUplinkBySleepCycle(com.lora.cn.network.GatewayPacketsClient.PacketRecord r, String currentTime, long intervalMs) {
        if (r == null) return;
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

        PendingUplink p = new PendingUplink(devEui, devAddr, hex, dr, time, freq, rssi, snr, fport, fcnt);
        String key = normalizeDeviceKey(devEui, hex);
        long nowMs = System.currentTimeMillis();
        SleepCycleState st = sleepCycleStateByDev.get(key);
        boolean newCycle = st == null || nowMs - st.cycleStartMs >= Math.max(1000L, intervalMs);
        if (newCycle) {
            st = new SleepCycleState(nowMs, false);
            sleepCycleStateByDev.put(key, st);
        }
        if (!st.firstDropped) {
            st.firstDropped = true;
            if (!"-".equals(hex) && hex != null && hex.length() > 0) {
                try {
                    if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
                    final String t = time;
                    final String h = hex;
                    ioExecutor.execute(() -> {
                        try { onUplinkDataEvent(new UplinkDataEvent(t, h)); } catch (Exception ignored) {}
                    });
                } catch (Exception ignored) {}
            }
            return;
        }
        processUplinkPacket(p);
    }

    private void processUplinkPacket(PendingUplink p) {
        if (p == null) return;
        String devEui = p.devEui;
        String devAddr = p.devAddr;
        String hex = p.hex;
        String dr = p.dr;
        String time = p.time;
        String freq = p.freq;
        String rssi = p.rssi;
        String snr = p.snr;
        String fport = p.fport;
        String fcnt = p.fcnt;

        if (!"-".equals(hex)) {
            com.lora.cn.utils.LogUtils.i(TAG, "准备入库上行数据 hex=" + hex);
            long nowMs = System.currentTimeMillis();
            boolean allowStore = true;
            if (devEui != null && !"".equals(devEui) && !" -".equals(devEui)) {
                Long last = lastUplinkStoreByDevMs.get(devEui);
                if (last != null && nowMs - last < 2000) {
                    allowStore = false;
                    Log.d(TAG, "跳过频繁上行入库: devEUI=" + devEui + ", hex=" + hex);
                }
            }
            if (allowStore) {
                if (devEui != null && !"".equals(devEui) && !" -".equals(devEui)) {
                    lastUplinkStoreByDevMs.put(devEui, nowMs);
                }
                if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
                final String broadcastTime = time;
                final String broadcastHex = hex;
                final String devEuiForLog = devEui;
                ioExecutor.execute(() -> {
                    try {
                        Log.d(TAG, "入库任务开始: devEUI=" + (devEuiForLog == null ? "" : devEuiForLog) + ", time=" + broadcastTime);
                    } catch (Exception ignored) {}
                    long result = -1L;
                    try { result = databaseHelper.addUplinkLog(broadcastHex); } catch (Exception e) { Log.e(TAG, "addUplinkLog异常: " + e.getMessage()); }
                    Log.d(TAG, "上行数据存储到上行日志表，结果: " + result);
                    com.lora.cn.utils.LogUtils.i(TAG, "上行数据入库结果: " + result);
                    if (result <= 0) {
                        try {
                            com.lora.cn.utils.LoRaFrameParser.ParsedFrame f = com.lora.cn.utils.LoRaFrameParser.parseFrame(broadcastHex);
                            Log.w(TAG, "上行未写入日志表: devEUI=" + (devEuiForLog == null ? "" : devEuiForLog) + ", deviceId=" + (f != null ? f.deviceId : "") + ", hex=" + broadcastHex);
                        } catch (Exception ignored) {}
                    }
                    try {
                        UplinkDataEvent event = new UplinkDataEvent(broadcastTime, broadcastHex);
                        EventBus.getDefault().post(event);
                    } catch (Exception ignored) {}
                    try {
                        long nowEval = System.currentTimeMillis();
                        if (nowEval - lastAlertEvalTriggerMs >= 10000) {
                            lastAlertEvalTriggerMs = nowEval;
                            if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                            mainHandler.post(() -> { try { evaluateAlertsOnce(); } catch (Exception ignored) {} });
                        }
                    } catch (Exception ignored) {}
                });
            } else {
                try {
                    com.lora.cn.utils.LoRaFrameParser.ParsedFrame frame = com.lora.cn.utils.LoRaFrameParser.parseFrame(hex);
                    String did = frame != null ? frame.deviceId : devEui;
                    if (did != null && did.length() > 0 && databaseHelper != null && databaseHelper.isTerminalExists(did)) {
                        try {
                            databaseHelper.updateTerminalMetricsByDeviceId(did, frame != null ? frame.batteryLevel : 0, frame != null ? frame.rssi : 0, frame != null ? frame.batteryVoltage : 0);
                        } catch (Exception ignored) {}
                        try {
                            databaseHelper.updateTerminalStatusByDeviceId(did, com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ONLINE);
                        } catch (Exception ignored) {}
                        try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("uplink_fast")); } catch (Exception ignored) {}
                        try {
                            UplinkDataEvent event = new UplinkDataEvent(time, hex);
                            org.greenrobot.eventbus.EventBus.getDefault().post(event);
                        } catch (Exception ignored) {}
                        try {
                            long nowEval = System.currentTimeMillis();
                            if (nowEval - lastAlertEvalTriggerMs >= 10000) {
                                lastAlertEvalTriggerMs = nowEval;
                                if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                                mainHandler.post(() -> { try { evaluateAlertsOnce(); } catch (Exception ignored) {} });
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
        }

        boolean verboseUplink = false;
        try { verboseUplink = com.blankj.utilcode.util.SPUtils.getInstance().getBoolean("uplink_verbose_log", false); } catch (Exception ignored) {}
        if (verboseUplink) {
            LogUtils.i(TAG,
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
        } else {
            Log.d(TAG, "UPLINK devEUI=" + devEui + ", fcnt=" + fcnt + ", fport=" + fport);
        }
    }

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
                    autoReturnHandler.postDelayed(this, 1500);
                    return;
                }
                if (autoReturnBusy) {
                    lastInteractionMs = System.currentTimeMillis();
                }
                long idleMs = System.currentTimeMillis() - lastInteractionMs;
                if (!isOnHome() && idleMs >= timeoutMs && !navInFlight.get()) {
                    Log.i(TAG, "空闲超时，自动返回首页");
                    navigateHome();
                }
            } catch (Exception e) {
                Log.e(TAG, "自动返回首页检查异常: " + e.getMessage());
            } finally {
                autoReturnHandler.postDelayed(this, 1500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appStartMs = System.currentTimeMillis();
        try { WindowCompat.setDecorFitsSystemWindows(getWindow(), true); } catch (Exception ignored) {}
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        ensureSystemBarsHidden();

        // 初始化数据库助手
        databaseHelper = DatabaseHelper.getInstance(this);
        if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
        if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        try {
            com.lora.cn.utils.LogUtils.init(getApplicationContext());
            com.lora.cn.utils.CrashLogger.install(getApplicationContext());
        } catch (Exception ignored) {}
        try {
            com.lora.cn.database.DatabaseHelper dbInit = databaseHelper;
            java.util.concurrent.ExecutorService exec = ioExecutor;
            if (dbInit != null && exec != null) {
                exec.execute(() -> {
                    try {
                        dbInit.ensureDefaultAdminRoleAssigned();
                        dbInit.debugLogAdminRoleAndUser();
                        dbInit.syncLowBatteryFlags();
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "初始化管理员角色/用户日志失败: " + e.getMessage());
                    }
                });
            }
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
        try { menuTabAdapter.submitList(new java.util.ArrayList<>(menuTabs)); } catch (Exception ignored) {}

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
            mqttStateReceiver = new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(android.content.Context context, android.content.Intent intent) {
                    if ("com.lora.cn.MQTT_CLIENT_STATE".equals(intent.getAction())) {
                        String state = intent != null ? intent.getStringExtra("state") : "";
                        if ("connected".equalsIgnoreCase(state)) {
                            updateMqttDotUi(MQTT_STATE_CONNECTED);
                        } else if ("connecting".equalsIgnoreCase(state)) {
                            updateMqttDotUi(MQTT_STATE_CONNECTING);
                        } else if ("stopped".equalsIgnoreCase(state)) {
                            updateMqttDotUi(MQTT_STATE_STOPPED);
                        } else {
                            updateMqttDotUi(MQTT_STATE_CONNECTING);
                        }
                    }
                }
            };
            android.content.IntentFilter stateFilter = new android.content.IntentFilter("com.lora.cn.MQTT_CLIENT_STATE");
            registerReceiver(mqttStateReceiver, stateFilter);
            // 仅监听服务端状态广播驱动UI
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
        try {
            alertEvaluateHandler.removeCallbacks(alertEvaluateRunnable);
            long sinceStart = System.currentTimeMillis() - appStartMs;
            int initialDelay = sinceStart < 30000 ? 10000 : 5000;
            evaluateAlertsOnce();
            alertEvaluateHandler.postDelayed(alertEvaluateRunnable, initialDelay);
        } catch (Exception ignored) {}

    } 

    public MqttPacketsClient getMqttClient() {
        return mqttClient;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensureSystemBarsHidden();
        try { updateMaintenanceBadge(); } catch (Exception ignored) {}
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!org.greenrobot.eventbus.EventBus.getDefault().isRegistered(this)) {
            org.greenrobot.eventbus.EventBus.getDefault().register(this);
        }
        ensureSystemBarsHidden();
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
        try {
            if (alertEvaluateHandler != null) {
                alertEvaluateHandler.removeCallbacks(alertEvaluateRunnable);
                alertEvaluateHandler = null;
            }
        } catch (Exception ignored) {}
        super.onStop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) ensureSystemBarsHidden();
    }

    private void applyImmersiveMode() {
    }

    private void ensureSystemBarsHidden() {
        try { getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN); } catch (Exception ignored) {}
        try { androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false); } catch (Exception ignored) {}
        try {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.view.WindowInsetsController c = getWindow().getInsetsController();
                if (c != null) {
                    c.hide(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars());
                    c.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                android.view.View decor = getWindow().getDecorView();
                decor.setSystemUiVisibility(
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                                | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                );
            }
        } catch (Exception ignored) {}
    }

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    public void onTerminalRefreshEvent(com.lora.cn.event.TerminalRefreshEvent event) {
        try { pendingCountOverride = null; updateMaintenanceBadge(); } catch (Exception ignored) {}
    }

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    public void onAlertPendingCountEvent(com.lora.cn.event.AlertPendingCountEvent event) {
        try { pendingCountOverride = event != null ? event.count : null; pendingCountOverrideTs = System.currentTimeMillis(); } catch (Exception ignored) {}
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
        mqttStatusDot = findViewById(R.id.mqtt_status_dot);
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
//        try {
//            if (rvMenuTabs != null) {
//                rvMenuTabs.setHasFixedSize(true);
//                rvMenuTabs.setItemAnimator(null);
//                rvMenuTabs.setNestedScrollingEnabled(false);
//            }
//        } catch (Throwable ignored) {}
        tvErrorComplete = findViewById(R.id.error_complte);
        updateMqttDotUi(MQTT_STATE_CONNECTING);

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
                allowAutoHideBig = false;
                if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
                if (llAlertPendingSmall != null) setSmallVisible(true);

                Log.d("", "llAlertPendingSmall=Visibility===7===" + llAlertPendingSmall.getVisibility());
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
                llAlertPending.setVisibility(View.GONE);
                llAlertPendingSmall.setVisibility(View.VISIBLE);
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
        viewPager.setOffscreenPageLimit(1);
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

    private void updateMqttDotUi(int state) {
        mqttUiState = state;
        if (mqttStatusDot == null) return;
        if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler.post(() -> {
            if (mqttStatusDot == null) return;
            int color = 0xFFFF3B30;
            if (mqttUiState == MQTT_STATE_CONNECTED) color = 0xFF34C759;
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            bg.setColor(color);
            mqttStatusDot.setBackground(bg);
            mqttStatusDot.setVisibility(android.view.View.VISIBLE);
            if (mqttUiState == MQTT_STATE_CONNECTING) {
                if (mqttDotBlinkAnim == null) {
                    android.view.animation.AlphaAnimation anim = new android.view.animation.AlphaAnimation(1.0f, 0.2f);
                    anim.setDuration(600);
                    anim.setRepeatMode(android.view.animation.Animation.REVERSE);
                    anim.setRepeatCount(android.view.animation.Animation.INFINITE);
                    mqttDotBlinkAnim = anim;
                }
                mqttStatusDot.clearAnimation();
                mqttStatusDot.startAnimation(mqttDotBlinkAnim);
            } else {
                mqttStatusDot.clearAnimation();
            }
        });
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
                        allowAutoHideBig = true;
                        try {
                            int sc = getStatusCodeForTitle(finalTarget.title);
                            long nowTs = System.currentTimeMillis();
                            lastHandledTypes.put(devHex, sc);
                            lastHandledTimes.put(devHex, nowTs);
                        } catch (Exception ignored) {}
                        handleAlertHandled(devHex, 0);
                    } catch (Exception ignored) {}
                }
            });
        } catch (Exception ignored) {}
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
    private final java.util.Map<String, Long> lastHandledTimes = new java.util.HashMap<>();
    private final java.util.Set<String> offlineAlertedKeys = new java.util.HashSet<>();
    private volatile long lastTabSwitchMs = 0L;
    private volatile long lastMaintenanceBadgeMs = 0L;
    private long lastBadgeQueryMs = 0L;
    private long lastTerminalRefreshRequestMs = 0L;
    private android.os.Handler alertEvaluateHandler;
    private volatile boolean alertEvaluateBusy = false;
    private final Runnable alertEvaluateRunnable = new Runnable() {
        @Override public void run() {
            try {
                if (alertEvaluateBusy) {
                    if (alertEvaluateHandler != null) alertEvaluateHandler.postDelayed(this, 5000);
                    return;
                }
                alertEvaluateBusy = true;
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
                    alertEvaluateBusy = false;
                });
            } finally {
                long now = System.currentTimeMillis();
                long sinceStart = appStartMs > 0 ? (now - appStartMs) : 0L;
                int nextDelay = sinceStart < 30000 ? 10000 : 5000;
                if (alertEvaluateHandler != null) alertEvaluateHandler.postDelayed(this, nextDelay);
            }
        }
    };
    private void evaluateAlertsOnce() {
        try {
            if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
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
                        try { applyAlertOverlayResult(finalRes); } catch (Exception ignored) {}
                    });
                }
            });
        } catch (Exception ignored) {}
    }

    private android.os.Handler maintenanceEvaluateHandler;
    private volatile boolean maintenanceEvaluateBusy = false;
    private final Runnable maintenanceEvaluateRunnable = new Runnable() {
        @Override public void run() {
            try {
                if (maintenanceEvaluateBusy) {
                    if (maintenanceEvaluateHandler != null) maintenanceEvaluateHandler.postDelayed(this, 60000);
                    return;
                }
                maintenanceEvaluateBusy = true;
                if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
                if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                long uid = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
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
                            java.text.SimpleDateFormat sdf1 = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault());
                            java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                            java.util.List<com.lora.cn.ui.model.Terminal> terms = null;
                            try { terms = db.getAllTerminals(); } catch (Exception ignored) {}
                            java.util.HashMap<String, java.util.ArrayList<com.lora.cn.ui.model.MaintenanceInfo>> dueByDev = new java.util.HashMap<>();
                            java.util.HashMap<String, Long> latestTsByDev = new java.util.HashMap<>();
                            java.util.HashMap<String, Integer> latestMinsByDev = new java.util.HashMap<>();
                            for (com.lora.cn.ui.model.MaintenanceInfo mi : list) {
                                if (mi == null) continue;
                                if (mi.getStatus() != 0) continue;
                                if (mi.getSentFlag() == 1) continue;
                                String dev = mi.getTerminalId();
                                if (dev == null || dev.isEmpty()) continue;
                                String ct = mi.getCreateTime();
                                if (ct == null || ct.trim().isEmpty()) continue;
                                java.util.Date dt = null;
                                try { dt = sdf1.parse(ct.trim()); } catch (Exception ignored) {}
                                if (dt == null) { try { dt = sdf2.parse(ct.trim()); } catch (Exception ignored) {} }
                                if (dt == null) continue;
                                long ts = dt.getTime();
                                if (ts > now) continue;
                                java.util.ArrayList<com.lora.cn.ui.model.MaintenanceInfo> bucket = dueByDev.get(dev);
                                if (bucket == null) { bucket = new java.util.ArrayList<>(); dueByDev.put(dev, bucket); }
                                bucket.add(mi);
                                int mins = 0;
                                try {
                                    java.util.Calendar cal = java.util.Calendar.getInstance();
                                    cal.setTime(dt);
                                    mins = Math.max(0, Math.min(1440, cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)));
                                } catch (Exception ignored) {}
                                Long prevTs = latestTsByDev.get(dev);
                                if (prevTs == null || ts >= prevTs) {
                                    latestTsByDev.put(dev, ts);
                                    latestMinsByDev.put(dev, mins);
                                }
                            }
                            for (java.util.Map.Entry<String, java.util.ArrayList<com.lora.cn.ui.model.MaintenanceInfo>> entry : dueByDev.entrySet()) {
                                String dev = entry.getKey();
                                java.util.ArrayList<com.lora.cn.ui.model.MaintenanceInfo> bucket = entry.getValue();
                                if (dev == null || bucket == null || bucket.isEmpty()) continue;
                                try {
                                    int depId = 0;
                                    int cartId = 0;
                                    boolean clearActivePending = false;
                                    if (terms != null) {
                                        for (com.lora.cn.ui.model.Terminal t : terms) {
                                            if (t != null && dev.equalsIgnoreCase(t.getTerminalId())) {
                                                depId = (int) Math.max(0, Math.min(255, t.getDepartmentId()));
                                                cartId = (int) Math.max(0, Math.min(255, t.getRoomId()));
                                                clearActivePending = t.isMaintenanceClearPending();
                                                break;
                                            }
                                        }
                                    }
                                    int interval = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
                                    int normalizedInterval = Math.max(3, Math.min(1440, interval));
                                    Integer minsObj = latestMinsByDev.get(dev);
                                    int mins = minsObj != null ? minsObj : 0;
                                    int clearMask = (1 << 1) | (clearActivePending ? (1 << 2) : 0);
                                    //helper.sendDownlink8001(dev, 1, 0, depId, cartId, 0, clearMask, normalizedInterval, 1, new int[]{mins}, true);
                                } catch (Exception ignored) {}
                            }
                        }
                    } catch (Exception ignored) {}
                    maintenanceEvaluateBusy = false;
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
    private boolean allowAutoHideBig = false;
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
                if (last != null && last != com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code) out.clearDevs.add(devId);
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
                {
                    if (newly || newLogDetected) {
                        AlertItem item = new AlertItem();
                        item.title = "异常取走";
                        item.name = t.getTerminalName();
                        item.code = devId;
                        item.time = latestTime != null ? latestTime : nowStr;
                        item.logId = latestId;
                        out.actions.add(new AlertAction(item, newly));
                        if (newly) out.queuedAny = true;
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
                out.typeUpdates.put(devId, com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code);
                if (last != null && last != com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code) out.clearDevs.add(devId);
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
                if (last != null && last != com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code) out.clearDevs.add(devId);
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
                if (latestId > 0 && (newly || prev == null || prev != latestId)) {
                    AlertItem item = new AlertItem();
                    item.title = "设备低电量";
                    item.name = t.getTerminalName();
                    item.code = devId;
                    item.time = latestTime != null ? latestTime : nowStr;
                    item.logId = latestId;
                    out.actions.add(new AlertAction(item, newly));
                    if (newly) out.queuedAny = true;
                    out.logIdUpdates.put(key, latestId);
                }
                continue;
            }

            out.clearDevs.add(devId);
        }
        return out;
    }

    private long alertUiThrottleUntilMs = 0L;
    private void applyAlertOverlayResult(EvaluateResult result) {
        long nowThrottle = System.currentTimeMillis();
        if (nowThrottle < alertUiThrottleUntilMs) {
            updatePendingBadge();
            return;
        }
        alertUiThrottleUntilMs = nowThrottle + 500L;
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
        if (queueChanged) {
            boolean currentStillInQueue = false;
            if (currentAlert != null) {
                for (AlertItem ai : alertQueue) {
                    if (ai == null) continue;
                    if (currentAlert.code != null && !currentAlert.code.equalsIgnoreCase(ai.code)) continue;
                    if (currentAlert.title != null && !currentAlert.title.equals(ai.title)) continue;
                    if (currentAlert.logId > 0 && ai.logId > 0 && currentAlert.logId != ai.logId) continue;
                    currentStillInQueue = true;
                    break;
                }
                if (!currentStillInQueue) {
                    currentAlert = null;
                    lastShownKey = null;
                    lastSmallKey = null;
                }
            }
            if (alertQueue.isEmpty()) {
                try { stopAlertRinging(); } catch (Exception ignored) {}
                if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
                if (llAlertPendingSmall != null) setSmallVisible(false);
            } else if (currentAlert == null) {
                showLatestPending();
            }
        }

        if (!result.actions.isEmpty()) {
            String newKey = null;
            boolean allowPopup = true;
            for (AlertAction a : result.actions) {
                if (a == null || a.item == null) continue;
                String devId = a.item.code;
                String title = a.item.title;
                if (devId != null && title != null) {
                    int sc = getStatusCodeForTitle(title);
                    Long ht = lastHandledTimes.get(devId);
                    Integer handledType = lastHandledTypes.get(devId);
                    long at = parseMillis(a.item.time);
                    boolean suppress = handledType != null && handledType == sc && ht != null && at > 0 && at <= ht;
                    if (!existsInQueue(devId, title) && !suppress && allowPopup) {
                        alertQueue.addLast(a.item);
                        queueChanged = true;
                        newKey = devId + ":" + title;
                    }
                    if (sc != 0) lastAlertTypes.put(devId, sc);
                }
                if (a.ring && !alertMuted) startAlertRinging30s();
            }
            boolean smallVisible = llAlertPendingSmall != null && llAlertPendingSmall.getVisibility() == View.VISIBLE;
            if (result.queuedAny && allowPopup && smallVisible && (newKey == null || newKey.equals(lastSmallKey))) {
                if (llAlertPendingSmall != null) setSmallVisible(true);
                if (allowAutoHideBig && llAlertPending != null) llAlertPending.setVisibility(View.GONE);
                lastSmallKey = newKey;
                updatePendingBadge();
                int afterQueueSize2 = alertQueue.size();
                if (result.queuedAny || result.touchedDb || queueChanged || beforeQueueSize != afterQueueSize2) {
                    long now = System.currentTimeMillis();
                    if (now - lastTerminalRefreshRequestMs >= 3000) {
                        lastTerminalRefreshRequestMs = now;
                        try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("离线刷新")); } catch (Exception ignored) {}
                    }
                }
                return;
            }
        }

        try {
            pendingCountOverride = null;
            applyPendingBadgeUi(alertQueue.size());
        } catch (Exception ignored) {}

        pendingAlertCount = alertQueue.size();
        if (result.queuedAny && !alertQueue.isEmpty()) {
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
                        if (allowAutoHideBig && llAlertPending != null) llAlertPending.setVisibility(View.GONE);
                        lastSmallKey = keyCandidate;
                    } else {
                        showLatestPending();
                    }
                } else {
                    if (llAlertPendingSmall != null) setSmallVisible(false);
                }
            } else {
                currentAlert = null;
                lastShownKey = null;
                lastSmallKey = null;
                try { stopAlertRinging(); } catch (Exception ignored) {}
                if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
                if (llAlertPendingSmall != null) setSmallVisible(false);
            }
        }
        updatePendingBadge();
        int afterQueueSize = alertQueue.size();
        if (result.queuedAny || result.touchedDb || queueChanged || beforeQueueSize != afterQueueSize) {
            long now = System.currentTimeMillis();
            if (now - lastTerminalRefreshRequestMs >= 3000) {
                lastTerminalRefreshRequestMs = now;
                try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("离线刷新")); } catch (Exception ignored) {}
            }
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


            int latestTimedUnsentMins = -1;
            java.util.List<com.lora.cn.ui.model.MaintenanceInfo> allMForDevice = null;
            java.util.ArrayList<com.lora.cn.ui.model.MaintenanceInfo> dueMaint = new java.util.ArrayList<>();
            try {
                allMForDevice = databaseHelper.getMaintenanceRecordsByTerminal(frame.deviceId, 0);
                java.util.List<com.lora.cn.ui.model.MaintenanceInfo> allM = allMForDevice;
                if (allM != null) {
                    long now = System.currentTimeMillis();
                    java.text.SimpleDateFormat sdf1 = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault());
                    java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                    java.text.SimpleDateFormat sdf3 = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault());
                    java.text.SimpleDateFormat sdf4 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
                    int latestUnsentMins = -1;
                    long latestUnsentTs = -1L;
                    for (com.lora.cn.ui.model.MaintenanceInfo mi : allM) {
                        ////Log.e("", "--- :" + new Gson().toJson(mi));
                        if (mi == null) {
                            Log.e("", "--- : 0 ");
                            continue;
                        }

                        if (mi.getStatus() != 0)  {
                            //Log.e("", "--- : 1 ==mi.getStatus() != 0");
                            continue;
                        }
                        String c = mi.getContent();
                        if ("主动维护".equals(c))  {
                            //Log.e("", "--- : 3 == 主动维护.equals(c)");
                            continue;
                        };
                        if (mi.getSentFlag() == 1 || !StringUtils.isEmpty(mi.getSentTime()))  {
                            //Log.e("", "--- : 4 ===mi.getSentFlag() == 1");
                            continue;
                        }
                        String ct = mi.getCreateTime();
                        if (ct == null || ct.trim().isEmpty()) {
                            ///Log.e("", "--- : 5 ===mi.getCreateTime() == 1");
                            continue;
                        };
                        try {
                            java.util.Date dt = null;
                            try { dt = sdf1.parse(ct.trim()); } catch (Exception ignored) {}
                            if (dt == null) { try { dt = sdf2.parse(ct.trim()); } catch (Exception ignored) {} }
                            if (dt == null) { try { dt = sdf3.parse(ct.trim()); } catch (Exception ignored) {} }
                            if (dt == null) { try { dt = sdf4.parse(ct.trim()); } catch (Exception ignored) {} }
                            if (dt == null) continue;
                            long ts = dt.getTime();
                            if (ts > now) {
                                Log.e("", "--- : 5 ===mi.getTime() == 1");
                                continue;
                            }
                            java.util.Calendar cal = java.util.Calendar.getInstance();
                            cal.setTime(dt);
                            int mins = Math.max(0, Math.min(1440, cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)));
                            if (ts >= latestUnsentTs) {
                                latestUnsentTs = ts;
                                latestUnsentMins = mins;
                            }
                            dueMaint.add(mi);
                        } catch (Exception ignored) {
                            Log.e("", "--- : 6 ===mi.getTime() == 1");
                        }
                    }
                    if (latestUnsentMins >= 0) latestTimedUnsentMins = latestUnsentMins;
                }
            } catch (Exception ignored) {
                Log.e("", "--- : 7 ===mi.getTime() == 1");
            }
            try {
                int cnt = dueMaint.size();
                LogUtils.e(frame.deviceId + "==>到期且未下行(定时维护,sentFlag=0)数量=" + cnt + "，是否>1:" + (cnt > 1));
            } catch (Exception ignored) {
                Log.e("", "--- : 8 ===mi.getTime() == 1");
            }

            try {
                long nowDedup = System.currentTimeMillis();
                Long last = lastMaintenanceEvalByDevMs.get(frame.deviceId);
                if (last != null && nowDedup - last < 800L) return;
                lastMaintenanceEvalByDevMs.put(frame.deviceId, nowDedup);
            } catch (Exception ignored) {
                Log.e("", "--- : 9 ===mi.getTime() == 1");
            }

            try {
                com.lora.cn.network.MqttPacketsClient client = mqttClient != null ? mqttClient : com.lora.cn.network.MqttPacketsClient.getShared();
                com.lora.cn.utils.DownlinkMessageHelper helper = new com.lora.cn.utils.DownlinkMessageHelper(client);
                java.util.List<com.lora.cn.ui.model.Terminal> terms = db.getAllTerminals();
                int depId = 0;
                int cartId = 0;
                boolean clearActivePending = false;
                boolean maintenanceTouched = false;
                try {
                    if (terms != null) {
                        for (com.lora.cn.ui.model.Terminal t : terms) {
                            if (t != null && frame.deviceId.equalsIgnoreCase(t.getTerminalId())) {
                                depId = (int) Math.max(0, Math.min(255, t.getDepartmentId()));
                                cartId = (int) Math.max(0, Math.min(255, t.getRoomId()));
                                clearActivePending = t.isMaintenanceClearPending();
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
                boolean maintenanceNeeded = false;
                boolean timedMaintenanceNeeded = false;
                try {
                    if (frame.statusFlags != null) {
                        maintenanceNeeded = frame.statusFlags.contains(com.lora.cn.utils.LoRaFrameParser.DeviceStatusFlag.MAINTENANCE_NEEDED);
                        timedMaintenanceNeeded = frame.statusFlags.contains(com.lora.cn.utils.LoRaFrameParser.DeviceStatusFlag.TIMED_MAINTENANCE_NEEDED);
                    }
                } catch (Exception ignored) {}
                java.util.List<com.lora.cn.ui.model.MaintenanceInfo> existingAll = allMForDevice != null ? allMForDevice : databaseHelper.getMaintenanceRecordsByTerminal(frame.deviceId, 0);
                if (maintenanceNeeded) {
                    try {
                        try { db.updateTerminalMaintenanceState(frame.deviceId, true, System.currentTimeMillis()); } catch (Exception ignored) {}
                        boolean existsPendingAuto = false;
                        if (existingAll != null) {
                            for (com.lora.cn.ui.model.MaintenanceInfo x : existingAll) {
                                String c = x != null ? x.getContent() : null;
                                String hu = x != null ? x.getHandleUser() : null;
                                String ht = x != null ? x.getHandleTime() : null;
                                boolean unhandled = (hu == null || hu.trim().isEmpty()) && (ht == null || ht.trim().isEmpty());
                                if ("主动维护".equals(c) && unhandled) {
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
                            mi.setContent("主动维护");
                            long uid = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
                            String uname = com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", "");
                            mi.setCreateUserId(uid > 0 ? uid : 0L);
                            mi.setCreateUser(uname == null ? "" : uname);
                            mi.setCreateTime(new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                            try { databaseHelper.addMaintenanceRecord(mi); } catch (Exception ignored2) {}
                            maintenanceTouched = true;
                        }
                    } catch (Exception ignored) {}
                } else {
                    try {
                        try { db.updateTerminalMaintenanceState(frame.deviceId, false, System.currentTimeMillis()); } catch (Exception ignored) {}
                        if (existingAll != null) {
                            String autoUser = "系统自动";
                            String autoTime = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                            String autoRemark = "主动维护清除：自动标记已维护";
                            for (com.lora.cn.ui.model.MaintenanceInfo x : existingAll) {
                                if (x == null) continue;
                                if (x.getStatus() != 0) continue;
                                String c = x.getContent();
                                if (!(("主动维护".equals(c)) || (c != null && c.startsWith("设备维护：")))) continue;
                                String hu = x.getHandleUser();
                                String ht = x.getHandleTime();
                                boolean unhandled = (hu == null || hu.trim().isEmpty()) && (ht == null || ht.trim().isEmpty());
                                if (!unhandled) continue;
                                try { databaseHelper.updateMaintenanceHandled(x.getId(), 0L, autoUser, autoTime, autoRemark); } catch (Exception ignored2) {}
                                maintenanceTouched = true;
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (!timedMaintenanceNeeded) {
                    try {
                        if (existingAll != null) {
                            String autoUser = "系统自动";
                            String autoTime = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                            String autoRemark = "定时维护清除：自动标记已维护";
                            long now = System.currentTimeMillis();
                            java.text.SimpleDateFormat sdf1 = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault());
                            java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                            java.text.SimpleDateFormat sdf3 = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault());
                            java.text.SimpleDateFormat sdf4 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
                            for (com.lora.cn.ui.model.MaintenanceInfo x : existingAll) {
                                if (x == null) continue;
                                if (x.getStatus() != 0) continue;
                                String c = x.getContent();
                                if ("主动维护".equals(c) || (c != null && c.startsWith("设备维护："))) continue;
                                String hu = x.getHandleUser();
                                String ht = x.getHandleTime();
                                boolean unhandled = (hu == null || hu.trim().isEmpty()) && (ht == null || ht.trim().isEmpty());
                                if (!unhandled) continue;
                                String ct = x.getCreateTime();
                                boolean due = true;
                                if (ct != null && !ct.trim().isEmpty()) {
                                    try {
                                        java.util.Date dt = null;
                                        try { dt = sdf1.parse(ct.trim()); } catch (Exception ignored) {}
                                        if (dt == null) { try { dt = sdf2.parse(ct.trim()); } catch (Exception ignored) {} }
                                        if (dt == null) { try { dt = sdf3.parse(ct.trim()); } catch (Exception ignored) {} }
                                        if (dt == null) { try { dt = sdf4.parse(ct.trim()); } catch (Exception ignored) {} }
                                        if (dt != null) due = dt.getTime() <= now;
                                    } catch (Exception ignored) {}
                                }
                                if (!due) continue;
                                try { databaseHelper.updateMaintenanceHandled(x.getId(), 0L, autoUser, autoTime, autoRemark); } catch (Exception ignored2) {}
                                maintenanceTouched = true;
                            }
                        }
                    } catch (Exception ignored) {}
                }

                boolean need8001ByConfig = false;
                try { need8001ByConfig = helper.isNeedDownlink8001(frame); } catch (Exception ignored) {}
                boolean timedMaintenanceDue = !dueMaint.isEmpty();
                boolean shouldSend = clearActivePending || timedMaintenanceDue || need8001ByConfig;
                LogUtils.e(frame.deviceId + "==>8001是否需要下行：" + need8001ByConfig + "，定时维护到期：" + timedMaintenanceDue + "，清除主动维护pending：" + clearActivePending);
                LogUtils.e(frame.deviceId + "==>是否下指令：" + shouldSend);
                if (!shouldSend) {
                    if (maintenanceTouched) {
                        try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("维护刷新:" + frame.deviceId)); } catch (Exception ignored) {}
                    }
                    return;
                }
                int intervalMin = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
                int normalizedInterval = Math.max(3, Math.min(1440, intervalMin));
                int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
                int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
                int fallbackMins = Math.max(0, Math.min(1440, h * 60 + m));
                //int sendMins = latestTimedUnsentMins >= 0 ? latestTimedUnsentMins : fallbackMins;
                int clearMask = (timedMaintenanceDue ? (1 << 1) : 0) | (clearActivePending ? (1 << 2) : 0);
                try {
                    helper.sendDownlink8001(frame.deviceId, 1, 1, depId, cartId, 0, clearMask, normalizedInterval, 1, new int[]{fallbackMins}, true);
                    if (timedMaintenanceDue) {
                        String sentTime = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                        for (com.lora.cn.ui.model.MaintenanceInfo mi : dueMaint) {
                            try { db.updateMaintenanceSent(mi.getId(), sentTime); } catch (Exception ignored) {}
                        }
                        maintenanceTouched = true;
                    }
                } catch (Exception ignored) {
                    LogUtils.e(frame.deviceId + "==>ignored：" + ignored);
                }
                if (clearActivePending) {
                    try { db.setTerminalMaintenanceClearPending(frame.deviceId, false); } catch (Exception ignored) {}
                }
                if (maintenanceTouched) {
                    try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("维护刷新:" + frame.deviceId)); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
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
            if (llAlertPending != null) llAlertPending.setVisibility(View.VISIBLE);
            return;
        }
        if (tvErrorTitle != null) tvErrorTitle.setText(currentAlert.title);
        if (tvErrorName != null) tvErrorName.setText(currentAlert.name);
        if (tvErrorCode != null) tvErrorCode.setText(currentAlert.code);
        if (tvErrorTime != null) tvErrorTime.setText(currentAlert.time);
        updatePendingBadge();
        if (llAlertPendingSmall != null) setSmallVisible(false);
        if (llAlertPending != null) llAlertPending.setVisibility(View.VISIBLE);
        allowAutoHideBig = false;
        int sc = getStatusCodeForTitle(currentAlert.title);
        Integer handled = lastHandledTypes.get(currentAlert.code);
        boolean needConfirm = handled == null || handled != sc;
        if (tvErrorComplete != null) tvErrorComplete.setVisibility(needConfirm ? View.VISIBLE : View.VISIBLE);
        refreshHandledStatusFromDBAsync(currentAlert.code, sc);
        lastShownKey = key;
        lastSmallKey = null;
    }

    

    private void minimizePending() {
        allowAutoHideBig = false;
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

    private int getStatusCodeForTitle(String title) {
        if ("异常取走".equals(title)) return com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code;
        if ("设备低电量".equals(title)) return com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code;
        if ("设备离线".equals(title)) return com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
        return 0;
    }

    public java.util.List<com.lora.cn.ui.model.LogInfo> getPendingAlertLogSnapshot() {
        java.util.ArrayList<com.lora.cn.ui.model.LogInfo> out = new java.util.ArrayList<>();
        try {
            for (AlertItem ai : alertQueue) {
                if (ai == null) continue;
                String title = ai.title;
                String devId = ai.code;
                int sc = getStatusCodeForTitle(title);
                if (sc == 0) continue;
                com.lora.cn.ui.model.LogInfo li = new com.lora.cn.ui.model.LogInfo();
                li.setId(ai.logId);
                li.setTerminalId(devId != null ? devId : "");
                li.setTerminalName(ai.name != null ? ai.name : "");
                li.setDeviceId(devId != null ? devId : "");
                li.setStatusCode(sc);
                li.setOperator("");
                li.setOperationTime(ai.time != null ? ai.time : "");
                li.setAction(title != null ? title : "");
                li.setCreateTime(ai.time != null ? ai.time : "");
                li.setHandleUser("");
                li.setHandleTime("");
                li.setHandleRemark("");
                out.add(li);
            }
        } catch (Exception ignored) {}
        return out;
    }

    private long parseMillis(String time) {
        if (time == null || time.length() == 0) return -1L;
        String s = time.trim();
        if (s.length() == 0) return -1L;
        int dot = s.indexOf('.');
        if (dot > 0) s = s.substring(0, dot);
        if (s.indexOf('/') >= 0) s = s.replace('/', '-');
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date d = sdf.parse(s);
            return d != null ? d.getTime() : -1L;
        } catch (Exception e) { return -1L; }
    }

    private void refreshHandledStatusFromDBAsync(String devId, int currentStatusCode) {
        if (devId == null) return;
        try {
            if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            android.content.Context appCtx = getApplicationContext();
            ioExecutor.execute(() -> {
                Integer handledCode = null;
                Long handledTs = null;
                try {
                    com.lora.cn.database.DatabaseHelper db = databaseHelper != null ? databaseHelper : com.lora.cn.database.DatabaseHelper.getInstance(appCtx);
                    java.util.List<com.lora.cn.ui.model.LogInfo> logs = db.getLogsByTerminalId(devId);
                    if (logs != null) {
                        long maxTs = -1L;
                        int codeAtMax = 0;
                        for (com.lora.cn.ui.model.LogInfo li : logs) {
                            if (li == null) continue;
                            String hu = li.getHandleUser();
                            String htStr = li.getHandleTime();
                            boolean handled = (hu != null && !hu.trim().isEmpty()) || (htStr != null && !htStr.trim().isEmpty());
                            if (!handled) continue;
                            long ts = -1L;
                            if (htStr != null && !htStr.trim().isEmpty()) {
                                ts = parseMillis(htStr.trim());
                            } else {
                                ts = parseMillis(li.getCreateTime());
                            }
                            if (ts > maxTs) {
                                maxTs = ts;
                                codeAtMax = li.getStatusCode();
                            }
                        }
                        if (maxTs >= 0) {
                            handledTs = maxTs;
                            handledCode = codeAtMax;
                        }
                    }
                } catch (Exception ignored) {}
                Integer finalHandledCode = handledCode;
                Long finalHandledTs = handledTs;
                mainHandler.post(() -> {
                    try {
                        if (finalHandledCode != null) lastHandledTypes.put(devId, finalHandledCode);
                        if (finalHandledTs != null) lastHandledTimes.put(devId, finalHandledTs);
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

    private int computePendingCountSync() {
        int count = 0;
        try {
            android.content.Context appCtx = getApplicationContext();
            com.lora.cn.database.DatabaseHelper db = databaseHelper != null ? databaseHelper : com.lora.cn.database.DatabaseHelper.getInstance(appCtx);
            java.util.List<com.lora.cn.ui.model.LogInfo> all = db.getAllLogsBoundToTerminals();
            java.util.Map<String, com.lora.cn.ui.model.LogInfo> latest = new java.util.HashMap<>();
            for (com.lora.cn.ui.model.LogInfo li : all) {
                if (li == null) continue;
                String hu0 = li.getHandleUser();
                String ht0 = li.getHandleTime();
                boolean unhandled0 = (hu0 == null || hu0.trim().isEmpty()) && (ht0 == null || ht0.trim().isEmpty());
                if (!unhandled0) continue;
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
                String hu1 = li.getHandleUser();
                String ht1 = li.getHandleTime();
                boolean unhandled1 = (hu1 == null || hu1.trim().isEmpty()) && (ht1 == null || ht1.trim().isEmpty());
                if (!unhandled1) continue;
                Long ht = lastHandledTime.get(li.getTerminalId());
                long at = parseMillis(li.getCreateTime());
                int s = li.getStatusCode();
                if (s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code) {
                    com.lora.cn.ui.model.Terminal t = terminalById.get(li.getTerminalId());
                    if (t != null) {
                        boolean devStillOffline = t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE;
                        boolean isLowNow = t.getBatteryLevel() <= lowTh;
                        boolean afterHandled = ht == null || at > ht;
                        if (!devStillOffline && isLowNow && afterHandled) c++;
                    }
                } else {
                    com.lora.cn.ui.model.Terminal t = terminalById.get(li.getTerminalId());
                    boolean isOfflineCase = s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
                    boolean isAbnormalCase = s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code;
                    boolean devStillOffline = t != null && t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE;
                    boolean devStillAbnormal = t != null && t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ABNORMAL_TAKEN;
                    boolean afterHandled = ht == null || at > ht;
                    if (isOfflineCase) {
                        if (devStillOffline && afterHandled) c++;
                    } else if (isAbnormalCase) {
                        if (!devStillOffline) {
                            if (devStillAbnormal) c++;
                            else if (afterHandled) c++;
                        }
                    } else {
                        if (afterHandled) c++;
                    }
                }
            }
            count = c;
        } catch (Exception ignored) {}
        return count;
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
            long now = System.currentTimeMillis();
            if (now - lastBadgeQueryMs < 3000) return;
            lastBadgeQueryMs = now;
            if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            android.content.Context appCtx = getApplicationContext();
            int token = badgeSeq.incrementAndGet();
            ioExecutor.execute(() -> {
                int count = 0;
                try {
                    com.lora.cn.database.DatabaseHelper db = databaseHelper != null ? databaseHelper : com.lora.cn.database.DatabaseHelper.getInstance(appCtx);
                    java.util.List<com.lora.cn.ui.model.LogInfo> all = db.getAllLogsBoundToTerminals();
                    java.util.Map<String, com.lora.cn.ui.model.LogInfo> latest = new java.util.HashMap<>();
                    for (com.lora.cn.ui.model.LogInfo li : all) {
                        if (li == null) continue;
                        String hu0 = li.getHandleUser();
                        String ht0 = li.getHandleTime();
                        boolean unhandled0 = (hu0 == null || hu0.trim().isEmpty()) && (ht0 == null || ht0.trim().isEmpty());
                        if (!unhandled0) continue;
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
                        String hu1 = li.getHandleUser();
                        String ht1 = li.getHandleTime();
                        boolean unhandled1 = (hu1 == null || hu1.trim().isEmpty()) && (ht1 == null || ht1.trim().isEmpty());
                        if (!unhandled1) continue;
                        Long ht = lastHandledTime.get(li.getTerminalId());
                        long at = parseMillis(li.getCreateTime());
                        int s = li.getStatusCode();
                        if (s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code) {
                            com.lora.cn.ui.model.Terminal t = terminalById.get(li.getTerminalId());
                            boolean devStillOffline = t != null && t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE;
                            boolean showByBattery = true;
                            if (t != null) {
                                int level = t.getBatteryLevel();
                                boolean levelKnown = level >= 0 && level <= 100;
                                if (levelKnown) showByBattery = level <= lowTh;
                            }
                            if (t == null || devStillOffline || showByBattery) c++;
                        } else {
                            com.lora.cn.ui.model.Terminal t = terminalById.get(li.getTerminalId());
                            boolean isOfflineCase = s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
                            boolean isAbnormalCase = s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code;
                            boolean devStillOffline = t != null && t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE;
                            boolean devStillAbnormal = t != null && t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ABNORMAL_TAKEN;
                            boolean afterHandled = ht == null || at > ht;
                            if (isOfflineCase) {
                                if (devStillOffline && afterHandled) c++;
                            } else if (isAbnormalCase) {
                                if (!devStillOffline) {
                                    if (devStillAbnormal) c++;
                                    else if (afterHandled) c++;
                                }
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
        int queueSizeLocal = alertQueue != null ? alertQueue.size() : 0;
        int displayCount = Math.max(count, queueSizeLocal);
        pendingAlertCount = displayCount;
        lastComputedPendingCount = displayCount;
        if (tvErrorNumber != null) tvErrorNumber.setText(String.valueOf(displayCount));
        if (llAlertPendingSmall != null) {
            boolean bigVisible = llAlertPending != null && llAlertPending.getVisibility() == View.VISIBLE;
            boolean shouldShowSmall = !bigVisible && displayCount > 0;
            setSmallVisible(shouldShowSmall);
        }
        int queueSize = alertQueue.size();
        if (displayCount != lastBadgeCount || queueSize != lastBadgeQueueSize) {
            lastBadgeCount = displayCount;
            lastBadgeQueueSize = queueSize;
            android.util.Log.d(TAG, "updatePendingBadge count=" + displayCount + ", queueSize=" + queueSize);
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
            long nowThrottle = System.currentTimeMillis();
            if (nowThrottle - lastMaintenanceBadgeMs < 2000) return;
            lastMaintenanceBadgeMs = nowThrottle;
            if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            long uid = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
            ioExecutor.execute(() -> {
                int count = 0;
                try {
                    com.lora.cn.database.DatabaseHelper db = databaseHelper != null ? databaseHelper : com.lora.cn.database.DatabaseHelper.getInstance(getApplicationContext());
                    java.util.List<com.lora.cn.ui.model.MaintenanceInfo> list = db.getMaintenanceRecords(uid);
                    long now = System.currentTimeMillis();
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault());
                    if (list != null) {
                        for (com.lora.cn.ui.model.MaintenanceInfo mi : list) {
                            if (mi == null) continue;
                            if (mi.getStatus() != 0) continue;
                            String c = mi.getContent();
                            boolean isAuto = "主动维护".equals(c);
                            if (isAuto) {
                                count++;
                            } else {
                                String ct = mi.getCreateTime();
                                if (ct == null || ct.trim().isEmpty()) continue;
                                try {
                                    java.util.Date dt = sdf.parse(ct.trim());
                                    if (dt != null && dt.getTime() <= now) count++;
                                } catch (Exception ignored) {}
                            }
                        }
                    }
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
            if (allowAutoHideBig && llAlertPending != null) llAlertPending.setVisibility(View.GONE);
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

    private void openAlertPendingList() {
        try {
            androidx.fragment.app.Fragment fragment = new com.lora.cn.ui.fragment.AlertPendingListFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_device_list_container, fragment)
                    .addToBackStack("alert_pending")
                    .commit();
            setSmallVisible(true);
            allowAutoHideBig = false;
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
            if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            ioExecutor.execute(() -> {
                try {
                    android.content.res.AssetFileDescriptor afd = getAssets().openFd("901028.wav");
                    final android.media.MediaPlayer mp = new android.media.MediaPlayer();
                    mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    afd.close();
                    mp.setLooping(true);
                    mp.setOnCompletionListener(p -> {
                        try { p.release(); } catch (Exception ignored) {}
                        if (alertPlayer == p) alertPlayer = null;
                    });
                    mp.setOnErrorListener((p, what, extra) -> {
                        try { p.release(); } catch (Exception ignored) {}
                        if (alertPlayer == p) alertPlayer = null;
                        return true;
                    });
                    mp.prepare();
                    if (mainHandler != null) {
                        mainHandler.post(() -> {
                            try {
                                mp.start();
                                alertPlayer = mp;
                                if (ringHandler == null) ringHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                                if (ringStopRunnable != null) ringHandler.removeCallbacks(ringStopRunnable);
                                ringStopRunnable = new java.lang.Runnable() { @Override public void run() { stopAlertRinging(); } };
                                ringHandler.postDelayed(ringStopRunnable, 30000);
                            } catch (Exception ignored) {}
                        });
                    }
                } catch (Exception ignored) {}
            });
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
        long now = System.currentTimeMillis();
        if (navInFlight.get()) return;
        if (now - lastTabSwitchMs < 250) return;
        lastTabSwitchMs = now;
        navInFlight.set(true);
        // 切换ViewPager2到指定页面
        viewPager.setCurrentItem(tabIndex, false); // false表示无动画切换
        if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler.postDelayed(() -> navInFlight.set(false), 200);
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
        try { menuTabAdapter.submitList(new java.util.ArrayList<>(menuTabs)); } catch (Exception ignored) {}
    }

    private void toggleUserInfo() {
        if (isUserInfoVisible) {
            hideUserInfo();
        } else {
            showUserInfo();
        }
    }

    public void showDeviceList() {
        if (navInFlight.get()) return;
        navInFlight.set(true);
        if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
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
        mainHandler.postDelayed(() -> navInFlight.set(false), 300);
    }

    public void hideDeviceList() {
        if (navInFlight.get()) return;
        navInFlight.set(true);
        if (!isDeviceListVisible) {
            if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.postDelayed(() -> navInFlight.set(false), 300);
            return;
        }

        fragmentDeviceListContainer.setVisibility(View.GONE);
        viewPager.setVisibility(View.VISIBLE);
        rvMenuTabs.setVisibility(View.VISIBLE);
        isDeviceListVisible = false;

        try { getSupportFragmentManager().popBackStackImmediate("device_list", 0); } catch (Exception ignored) {}

        // 清除Fragment
        if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler.postDelayed(() -> navInFlight.set(false), 300);
    }

    private void showUserInfo() {
        if (navInFlight.get()) return;
        navInFlight.set(true);
        if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
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
        mainHandler.postDelayed(() -> navInFlight.set(false), 300);
    }

    private void hideUserInfo() {
        if (navInFlight.get()) return;
        navInFlight.set(true);
        if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
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
        mainHandler.postDelayed(() -> navInFlight.set(false), 300);
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

    @Override
    protected void onDestroy() {

        super.onDestroy();
        try {
            if (mqttStateReceiver != null) {
                unregisterReceiver(mqttStateReceiver);
                mqttStateReceiver = null;
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
            try { sleepCycleStateByDev.clear(); } catch (Exception ignored) {}
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
        if (navInFlight.get()) return;
        navInFlight.set(true);
        if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        try {
            if (viewPager != null) viewPager.setCurrentItem(0, true);
            if (isUserInfoVisible) hideUserInfo();
            if (isDeviceListVisible) hideDeviceList();
            rvMenuTabs.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);
            fragmentUserInfoContainer.setVisibility(View.GONE);
            fragmentDeviceListContainer.setVisibility(View.GONE);
            lastNonHomeStartMs = 0L;
//            try {
//                if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
//                if (llAlertPendingSmall != null) setSmallVisible(false);
//                stopAlertRinging();
//            } catch (Exception ignored) {}
            try {
                org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.OperationBusyEvent(false));
            } catch (Exception ignored) {}
        } catch (Exception e) {
            Log.e(TAG, "navigateHome 异常: " + e.getMessage());
        } finally {
            mainHandler.postDelayed(() -> navInFlight.set(false), 300);
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
            if (navInFlight.get()) return;
            navInFlight.set(true);
            if (isUserInfoVisible) hideUserInfo();
            fragmentDeviceListContainer.setVisibility(View.VISIBLE);
            rvMenuTabs.setVisibility(View.INVISIBLE);
            viewPager.setVisibility(View.GONE);
            isDeviceListVisible = true;
            if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.postDelayed(() -> navInFlight.set(false), 200);
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

    /**
     * 注释异常取走 等等/
     * @param devHex
     * @param mask
     */
    public void sendHandleDownlink(String devHex, int mask) {
        try {
            if (mqttClient != null) {
                com.lora.cn.utils.DownlinkMessageHelper helper = new com.lora.cn.utils.DownlinkMessageHelper(mqttClient);
                int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
                int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
                int mins = Math.max(0, Math.min(1440, h * 60 + m));
                int interval = Math.max(3, Math.min(1440, com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3)));
//                helper.sendDownlink8001(
//                        devHex,
//                        1,
//                        1,
//                        0,
//                        0,
//                        0,
//                        mask,
//                        interval,
//                        1,
//                        new int[]{mins},
//                        true
//                );
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "下发处理下行失败 devEUI=" + devHex + ", mask=" + mask, e);
        }
    }
}
