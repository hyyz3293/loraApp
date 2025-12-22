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
import com.lora.cn.ui.adapter.DeviceListAdapter;
import com.lora.cn.ui.activity.MainActivity;
import com.lora.cn.utils.LoRaFrameParser;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import com.lora.cn.ui.model.LogInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 设备列表Fragment - 显示附近终端
 */
public class DeviceListFragment extends Fragment {

    private TextView btnSearchTerminal;
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

        // 返回按钮
        btnBack.setOnClickListener(v -> {
            // 返回上个界面，隐藏设备列表
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).hideDeviceList();
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
        List<Terminal> seed = new ArrayList<>(allTerminals);
        Set<String> seedIds = new HashSet<>(discoveredDeviceIds);
        ioExecutor.execute(() -> {
            List<Terminal> next = new ArrayList<>(seed);
            Set<String> nextIds = new HashSet<>(seedIds);
            try {
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(appCtx);
                if (!next.isEmpty()) {
                    java.util.Iterator<Terminal> it = next.iterator();
                    while (it.hasNext()) {
                        Terminal t = it.next();
                        if (t != null && !TextUtils.isEmpty(t.getDeviceId()) && dbHelper.isTerminalExists(t.getDeviceId())) {
                            it.remove();
                            nextIds.remove(t.getDeviceId());
                        }
                    }
                }

                List<LogInfo> logs = dbHelper.getAllUnboundLogs();
                if (logs != null) {
                    for (LogInfo li : logs) {
                        String action = li.getAction();
                        if (action == null) continue;
                        if (!action.startsWith("接收上行数据:")) continue;
                        int idx = action.indexOf(":");
                        if (idx == -1 || idx + 1 >= action.length()) continue;
                        String hex = action.substring(idx + 1).trim();
                        LoRaFrameParser.ParsedFrame pf = LoRaFrameParser.parseFrame(hex);
                        if (pf == null || TextUtils.isEmpty(pf.deviceId)) continue;

                        String deviceId = pf.deviceId;
                        if (dbHelper.isTerminalExists(deviceId)) continue;
                        if (nextIds.contains(deviceId)) continue;
                        nextIds.add(deviceId);

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
                        discoveredTerminal.parsedFrame = pf;
                        next.add(discoveredTerminal);
                    }
                }
            } catch (Exception ignored) {}

            List<Terminal> finalNext = next;
            Set<String> finalNextIds = nextIds;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (token != loadSeq.get()) return;
                allTerminals.clear();
                if (finalNext != null) allTerminals.addAll(finalNext);
                discoveredDeviceIds.clear();
                if (finalNextIds != null) discoveredDeviceIds.addAll(finalNextIds);
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
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    if (discoveredDeviceIds.contains(deviceId)) return;
                    discoveredDeviceIds.add(deviceId);
                    Terminal discoveredTerminal = new Terminal();
                    discoveredTerminal.setDeviceId(deviceId);
                    discoveredTerminal.setDeviceName("终端ID：" + deviceId);
                    boolean isAbnormal = (frameData.evIllegalRemoval == 1);
                    discoveredTerminal.setStatus(isAbnormal ? com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ABNORMAL_LOST : com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ONLINE);
                    discoveredTerminal.setBatteryLevel(frameData.batteryLevel);
                    discoveredTerminal.setBatteryVoltage(frameData.batteryVoltage);
                    discoveredTerminal.setRssi(frameData.rssi);
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
}
