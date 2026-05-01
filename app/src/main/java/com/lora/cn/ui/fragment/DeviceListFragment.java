package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.google.gson.Gson;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.events.UplinkDataEvent;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.dao.TerminalDao;
import com.lora.cn.database.entity.Terminal;
import com.lora.cn.event.TerminalRefreshEvent;
import com.lora.cn.ui.adapter.DeviceListAdapter;
import com.lora.cn.ui.activity.MainActivity;
import com.lora.cn.utils.LoRaFrameParser;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import com.lora.cn.ui.model.LogInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 设备列表Fragment - 显示附近终端
 */
public class DeviceListFragment extends Fragment {

    private TextView btnSearchTerminal;
    private TextView btnAddTestDevice;
    private TextView btnAddLowBatteryTestDevice;
    private TextView btnBack;
    private EditText etSearch;
    private TextView btnSearch;
    private RecyclerView rvTerminals;
    private TextView tvEmpty;
    private LinearLayout llEmpty;
    private DeviceListAdapter deviceListAdapter;
    private DatabaseManager databaseManager;
    private TerminalDao terminalDao;
    private List<Terminal> allTerminals = new ArrayList<>();
    private Set<String> discoveredDeviceIds = new HashSet<>(); // 用于存储已发现的设备ID，避免重复显示
    private Map<String, Long> discoveredDeviceTimes = new HashMap<>();
    private java.util.concurrent.ExecutorService ioExecutor;
    private android.os.Handler mainHandler;
    private final java.util.concurrent.atomic.AtomicInteger loadSeq = new java.util.concurrent.atomic.AtomicInteger();

    public static DeviceListFragment newInstance() {
        return new DeviceListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_list, container, false);
        initViews(view);
        initData();
        if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
        if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        setupRecyclerView();
        setupClickListeners();
        loadTerminals();
        return view;
    }

    @Override
    public void onDestroyView() {
        try {
            if (ioExecutor != null) ioExecutor.shutdownNow();
        } catch (Exception ignored) {}
        ioExecutor = null;
        mainHandler = null;
        super.onDestroyView();
    }

    private void initViews(View view) {
        btnSearchTerminal = view.findViewById(R.id.btn_search_terminal);
        btnAddTestDevice = view.findViewById(R.id.btn_add_test_device);
        btnAddLowBatteryTestDevice = view.findViewById(R.id.btn_add_low_battery_test_device);
        btnBack = view.findViewById(R.id.btn_back);
        etSearch = view.findViewById(R.id.et_search);
        btnSearch = view.findViewById(R.id.btn_search);
        rvTerminals = view.findViewById(R.id.rv_terminals);
        tvEmpty = view.findViewById(R.id.tv_empty);
        llEmpty = view.findViewById(R.id.ll_empty);
    }

    private void initData() {
        databaseManager = DatabaseManager.getInstance(requireContext());
        terminalDao = new TerminalDao(DatabaseHelper.getInstance(requireContext()));
    }

    private void setupRecyclerView() {
        rvTerminals.setLayoutManager(new LinearLayoutManager(getContext()));
        deviceListAdapter = new DeviceListAdapter();
//        deviceListAdapter.setOnItemClickListener(new DeviceListAdapter.OnItemClickListener() {
//            @Override
//            public void onAddTerminalClick(Terminal terminal) {
//                // 点击添加终端，传入设备ID
//                if (getActivity() instanceof MainActivity) {
//                    MainActivity mainActivity = (MainActivity) getActivity();
//                    mainActivity.showAddDeviceFragment(terminal.getDeviceId());
//                }
//            }
//        });。
        deviceListAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener<Terminal>() {
            @Override
            public void onClick(@NonNull BaseQuickAdapter<Terminal, ?> baseQuickAdapter, @NonNull View view, int i) {
                if (getActivity() instanceof MainActivity) {
                    Terminal terminal = allTerminals.get(i);
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.showAddDeviceFragment(terminal);
                }
            }
        });
        deviceListAdapter.submitList(allTerminals);
        rvTerminals.setAdapter(deviceListAdapter);
    }

    private void setupClickListeners() {
        // 搜索终端按钮
        btnSearchTerminal.setOnClickListener(v -> {
            startSearchingTerminals();
        });

        if (btnAddTestDevice != null) {
            btnAddTestDevice.setOnClickListener(v -> addTestDevice());
        }
        if (btnAddLowBatteryTestDevice != null) {
            btnAddLowBatteryTestDevice.setOnClickListener(v -> addLowBatteryTestDevice());
        }

        // 返回按钮
        btnBack.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).hideDeviceListImmediate();
            } else if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        // 搜索按钮
        btnSearch.setOnClickListener(v -> {
            String searchText = etSearch.getText().toString().trim();
            searchTerminals(searchText);
        });
    }

    private void addTestDevice() {
        if (ioExecutor == null || mainHandler == null) return;
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        android.content.Context appCtx = ctx.getApplicationContext();
        ioExecutor.execute(() -> {
            String newId = generateUniqueTestTerminalId();
            String suffix = newId != null && newId.length() >= 4 ? newId.substring(newId.length() - 4) : String.valueOf(System.currentTimeMillis() % 10000);
            com.lora.cn.ui.model.Terminal t = new com.lora.cn.ui.model.Terminal();
            t.setTerminalId(newId);
            t.setTerminalName("测试设备-" + suffix);
            t.setDeviceCode("TEST" + suffix);
            t.setStatus(com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ONLINE);
            t.setSignalStrength(0);
            t.setBatteryLevel(100);
            t.setBatteryVoltage(420);
            t.setRssi(0);
            t.setDepartment("");
            t.setLocation("");
            boolean ok = false;
            try {
                DatabaseHelper db = DatabaseHelper.getInstance(appCtx);
                ok = db.addTerminal(t) > 0;
            } catch (Exception ignored) {}
            boolean finalOk = ok;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (finalOk) {
                    try { EventBus.getDefault().post(new TerminalRefreshEvent("新增测试终端:" + newId)); } catch (Exception ignored) {}
                    Toast.makeText(getContext(), "已添加测试设备: " + newId, Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).hideDeviceListImmediate();
                    }
                } else {
                    Toast.makeText(getContext(), "添加测试设备失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void addLowBatteryTestDevice() {
        if (ioExecutor == null || mainHandler == null) return;
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        android.content.Context appCtx = ctx.getApplicationContext();
        ioExecutor.execute(() -> {
            String newId = generateUniqueTestTerminalId();
            String suffix = newId != null && newId.length() >= 4 ? newId.substring(newId.length() - 4) : String.valueOf(System.currentTimeMillis() % 10000);
            com.lora.cn.ui.model.Terminal t = new com.lora.cn.ui.model.Terminal();
            t.setTerminalId(newId);
            t.setTerminalName("低电量测试-" + suffix);
            t.setDeviceCode("LOW" + suffix);
            t.setStatus(com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ONLINE);
            t.setSignalStrength(0);
            t.setBatteryLevel(5);
            t.setBatteryVoltage(300);
            t.setRssi(0);
            t.setDepartment("");
            t.setLocation("");
            boolean ok = false;
            try {
                DatabaseHelper db = DatabaseHelper.getInstance(appCtx);
                ok = db.addTerminal(t) > 0;
            } catch (Exception ignored) {}
            boolean finalOk = ok;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (finalOk) {
                    try { EventBus.getDefault().post(new TerminalRefreshEvent("新增低电量测试终端:" + newId)); } catch (Exception ignored) {}
                    Toast.makeText(getContext(), "已添加低电量测试设备: " + newId, Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).hideDeviceListImmediate();
                    }
                } else {
                    Toast.makeText(getContext(), "添加低电量测试设备失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String generateUniqueTestTerminalId() {
        try {
            if (terminalDao == null) terminalDao = new TerminalDao(DatabaseHelper.getInstance(requireContext()));
            for (int i = 0; i < 20; i++) {
                String id = randomHex16();
                try {
                    if (!terminalDao.isDeviceIdExists(id, 0)) return id;
                } catch (Exception ignored) {
                    return id;
                }
            }
        } catch (Exception ignored) {}
        return randomHex16();
    }

    private String randomHex16() {
        try {
            String s = java.util.UUID.randomUUID().toString().replace("-", "");
            if (s.length() >= 16) return s.substring(0, 16).toUpperCase(java.util.Locale.getDefault());
            return (s + "0000000000000000").substring(0, 16).toUpperCase(java.util.Locale.getDefault());
        } catch (Exception ignored) {
            return String.valueOf(System.currentTimeMillis());
        }
    }

    /**
     * 开始搜索终端功能
     */
    private void startSearchingTerminals() {
        // 显示loading动画和提示
        btnSearchTerminal.setEnabled(false);
        btnSearchTerminal.setText("搜索中...");
        
        Toast.makeText(getContext(), "正在搜索附近终端，请等待上行数据...", Toast.LENGTH_LONG).show();
        // 开始一次新的搜索时，清空已发现列表，避免返回后数据丢失问题
        try {
            allTerminals.clear();
            discoveredDeviceIds.clear();
            discoveredDeviceTimes.clear();
            deviceListAdapter.submitList(allTerminals);
            deviceListAdapter.notifyDataSetChanged();
            updateUI();
        } catch (Exception ignored) {}

        loadTerminals();
        
        // 3秒后恢复按钮状态
        btnSearchTerminal.postDelayed(() -> {
            btnSearchTerminal.setEnabled(true);
            btnSearchTerminal.setText("搜索终端");
        }, 3000);
    }

    private void loadTerminals() {
        if (ioExecutor == null || mainHandler == null) return;
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        android.content.Context appCtx = ctx.getApplicationContext();
        int token = loadSeq.incrementAndGet();
        final android.os.Handler handler = mainHandler;
        List<Terminal> seed = new ArrayList<>(allTerminals);
        Set<String> seedIds = new HashSet<>(discoveredDeviceIds);
        Map<String, Long> seedTimes = new HashMap<>(discoveredDeviceTimes);
        ioExecutor.execute(() -> {
            LinkedHashMap<String, Terminal> nextMap = new LinkedHashMap<>();
            Map<String, Long> nextTimes = new HashMap<>();
            try {
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(appCtx);
                long nowMs = System.currentTimeMillis();
                long filterWindowMs = getFilterWindowMs();
                if (!seed.isEmpty()) {
                    for (Terminal t : seed) {
                        if (t == null || TextUtils.isEmpty(t.getDeviceId())) continue;
                        String deviceId = t.getDeviceId();
                        long seenTime = seedTimes.containsKey(deviceId) ? seedTimes.get(deviceId) : -1L;
                        if (dbHelper.isTerminalExists(deviceId)) continue;
                        if (!isWithinFilterWindow(nowMs, seenTime, filterWindowMs)) continue;
                        nextMap.put(deviceId, t);
                        nextTimes.put(deviceId, seenTime);
                    }
                }

                List<LogInfo> logs = dbHelper.getAllUnboundLogs();
                if (logs != null) {
                    for (LogInfo li : logs) {
                        String action = li.getAction();
                        if (action == null) continue;
                        if (!action.startsWith("接收上行数据:")) continue;
                        long logTime = parseLogTime(li);
                        if (!isWithinFilterWindow(nowMs, logTime, filterWindowMs)) continue;
                        int idx = action.indexOf(":");
                        if (idx == -1 || idx + 1 >= action.length()) continue;
                        String hex = action.substring(idx + 1).trim();
                        LoRaFrameParser.ParsedFrame pf = LoRaFrameParser.parseFrame(hex);
                        if (pf == null || TextUtils.isEmpty(pf.deviceId)) continue;

                        String deviceId = pf.deviceId;
                        if (dbHelper.isTerminalExists(deviceId)) continue;
                        Long existingTime = nextTimes.get(deviceId);
                        if (existingTime != null && existingTime >= logTime) continue;

                        Terminal discoveredTerminal = new Terminal();
                        discoveredTerminal.setDeviceId(deviceId);
                        discoveredTerminal.setDeviceName("终端ID：" + deviceId);
                        boolean anyLayerInPlace = pf.stLayer1NotInPlace == 0 || pf.stLayer2NotInPlace == 0 || pf.stLayer3NotInPlace == 0 || pf.stLayer4NotInPlace == 0 || pf.stLayer5NotInPlace == 0;
                        boolean isOnline = (pf.stPowerLockOn == 0 && anyLayerInPlace);
                        boolean isAbnormal = (pf.stPowerLockOn == 0) && (pf.stLayer1NotInPlace == 1 || pf.stLayer2NotInPlace == 1 || pf.stLayer3NotInPlace == 1 || pf.stLayer4NotInPlace == 1 || pf.stLayer5NotInPlace == 1);
                        discoveredTerminal.setStatus(isAbnormal ? com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ABNORMAL_LOST : (isOnline ? com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ONLINE : com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_OFFLINE));
                        discoveredTerminal.setBatteryLevel(pf.batteryLevel);
                        discoveredTerminal.setBatteryVoltage(pf.batteryVoltage);
                        discoveredTerminal.setRssi(pf.rssi);
                        discoveredTerminal.setTermVerYY(pf.termVerYY);
                        discoveredTerminal.setTermVerMM(pf.termVerMM);
                        discoveredTerminal.setTermVerDD(pf.termVerDD);
                        discoveredTerminal.setLoraModuleVersionCode(pf.loraModuleVersionCode);
                        discoveredTerminal.setFirmwareVersionString(pf.firmwareVersionString);
                        discoveredTerminal.parsedFrame = pf;
                        nextMap.put(deviceId, discoveredTerminal);
                        nextTimes.put(deviceId, logTime);
                    }
                }
            } catch (Exception ignored) {}

            List<Terminal> finalNext = new ArrayList<>(nextMap.values());
            Set<String> finalNextIds = new HashSet<>(nextMap.keySet());
            Map<String, Long> finalNextTimes = new HashMap<>(nextTimes);
            if (handler == null) return;
            handler.post(() -> {
                if (!isAdded()) return;
                if (token != loadSeq.get()) return;
                allTerminals.clear();
                if (finalNext != null) allTerminals.addAll(finalNext);
                discoveredDeviceIds.clear();
                if (finalNextIds != null) discoveredDeviceIds.addAll(finalNextIds);
                discoveredDeviceTimes.clear();
                discoveredDeviceTimes.putAll(finalNextTimes);
                if (deviceListAdapter != null) {
                    deviceListAdapter.submitList(new ArrayList<>(allTerminals));
                    deviceListAdapter.notifyDataSetChanged();
                }
                updateUI();
            });
        });
    }

    private void searchTerminals(String searchText) {
//        if (TextUtils.isEmpty(searchText)) {
//            // 如果搜索文本为空，显示所有终端
//            deviceListAdapter.setTerminals(allTerminals);
//        } else {
//            // 根据设备ID或设备名称搜索
//            List<Terminal> filteredTerminals = new ArrayList<>();
//            for (Terminal terminal : allTerminals) {
//                if (terminal.getDeviceId().toLowerCase().contains(searchText.toLowerCase()) ||
//                    terminal.getDeviceName().toLowerCase().contains(searchText.toLowerCase())) {
//                    filteredTerminals.add(terminal);
//                }
//            }
//            deviceListAdapter.setTerminals(filteredTerminals);
//        }
//        updateEmptyView();
    }

    private void updateUI() {
        //deviceListAdapter.setTerminals(allTerminals);
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (allTerminals.isEmpty()) {
            rvTerminals.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            llEmpty.setVisibility(View.VISIBLE);
        } else {
            rvTerminals.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            llEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTerminals();
        // 注册EventBus
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 注销EventBus
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    /**
     * 接收上行数据事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUplinkDataReceived(UplinkDataEvent event) {
        if (event == null) return;
        if (ioExecutor == null || mainHandler == null) return;
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        android.content.Context appCtx = ctx.getApplicationContext();
        String hex = event.getHex();
        ioExecutor.execute(() -> {
            try {
                LoRaFrameParser.ParsedFrame frameData = LoRaFrameParser.parseFrame(hex);
                if (frameData == null || TextUtils.isEmpty(frameData.deviceId)) return;
                String deviceId = frameData.deviceId;
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(appCtx);
                if (dbHelper.isTerminalExists(deviceId)) return;
                long eventTime = parseTime(event.getTime());
                long nowMs = System.currentTimeMillis();
                long filterWindowMs = getFilterWindowMs();
                if (!isWithinFilterWindow(nowMs, eventTime, filterWindowMs)) return;
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    if (discoveredDeviceIds.contains(deviceId)) return;
                    discoveredDeviceIds.add(deviceId);
                    discoveredDeviceTimes.put(deviceId, eventTime > 0 ? eventTime : nowMs);
                    Terminal discoveredTerminal = new Terminal();
                    discoveredTerminal.setDeviceId(deviceId);
                    discoveredTerminal.setDeviceName("终端ID：" + deviceId);
                    boolean isAbnormal = (frameData.evIllegalRemoval == 1);
                    discoveredTerminal.setStatus(isAbnormal ? com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ABNORMAL_LOST : com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ONLINE);
                    discoveredTerminal.setBatteryLevel(frameData.batteryLevel);
                    discoveredTerminal.setBatteryVoltage(frameData.batteryVoltage);
                    discoveredTerminal.setRssi(frameData.rssi);
                    discoveredTerminal.setTermVerYY(frameData.termVerYY);
                    discoveredTerminal.setTermVerMM(frameData.termVerMM);
                    discoveredTerminal.setTermVerDD(frameData.termVerDD);
                    discoveredTerminal.setLoraModuleVersionCode(frameData.loraModuleVersionCode);
                    discoveredTerminal.setFirmwareVersionString(frameData.firmwareVersionString);
                    discoveredTerminal.parsedFrame = frameData;
                    allTerminals.add(discoveredTerminal);
                    updateUI();
                    if (deviceListAdapter != null) {
                        deviceListAdapter.submitList(new ArrayList<>(allTerminals));
                        deviceListAdapter.notifyDataSetChanged();
                    }
                });
            } catch (Exception ignored) {}
        });
    }

    private long getFilterWindowMs() {
        int sleepMin = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
        if (sleepMin <= 0) sleepMin = 3;
        return Math.max(60_000L, sleepMin * 2L * 60_000L);
    }

    private long parseLogTime(LogInfo logInfo) {
        if (logInfo == null) return -1L;
        long createTime = parseTime(logInfo.getCreateTime());
        if (createTime > 0) return createTime;
        return parseTime(logInfo.getOperationTime());
    }

    private boolean isWithinFilterWindow(long nowMs, long seenTimeMs, long filterWindowMs) {
        return seenTimeMs > 0 && nowMs - seenTimeMs <= filterWindowMs;
    }

    private long parseTime(String rawTime) {
        if (rawTime == null || rawTime.trim().isEmpty()) return -1L;
        String value = rawTime.trim();
        java.util.List<java.text.SimpleDateFormat> formats = new ArrayList<>();
        formats.add(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()));
        formats.add(new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()));
        for (java.text.SimpleDateFormat format : formats) {
            try {
                java.util.Date parsed = format.parse(value);
                if (parsed != null) return parsed.getTime();
            } catch (Exception ignored) {}
        }
        return -1L;
    }
}
