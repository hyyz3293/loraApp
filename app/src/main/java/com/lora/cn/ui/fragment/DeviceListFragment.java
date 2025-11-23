package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import androidx.fragment.app.FragmentTransaction;
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

    public static DeviceListFragment newInstance() {
        return new DeviceListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_list, container, false);
        initViews(view);
        initData();
        setupRecyclerView();
        setupClickListeners();
        loadTerminals();
        return view;
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
        
        // 3秒后恢复按钮状态
        btnSearchTerminal.postDelayed(() -> {
            btnSearchTerminal.setEnabled(true);
            btnSearchTerminal.setText("搜索终端");
        }, 3000);
    }

    private void loadTerminals() {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
            // 从本地日志库中提取最近上行的设备，作为“附近终端”来源
            List<LogInfo> logs = dbHelper.getAllLogs();
            // 清理已添加到终端表的设备，不再展示在发现列表中
            if (allTerminals != null && !allTerminals.isEmpty()) {
                java.util.Iterator<com.lora.cn.database.entity.Terminal> it = allTerminals.iterator();
                while (it.hasNext()) {
                    com.lora.cn.database.entity.Terminal t = it.next();
                    if (t != null && !TextUtils.isEmpty(t.getDeviceId()) && dbHelper.isTerminalExists(t.getDeviceId())) {
                        it.remove();
                        discoveredDeviceIds.remove(t.getDeviceId());
                    }
                }
            }

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
                    // 只展示“未添加过”的设备；添加过的跳过
                    if (dbHelper.isTerminalExists(deviceId)) continue;
                    // 同一设备只展示一次
                    if (discoveredDeviceIds.contains(deviceId)) continue;
                    discoveredDeviceIds.add(deviceId);

                    com.lora.cn.database.entity.Terminal discoveredTerminal = new com.lora.cn.database.entity.Terminal();
                    discoveredTerminal.setDeviceId(deviceId);
                    discoveredTerminal.setDeviceName("终端ID：" + deviceId);
                    // 在线规则：电源锁关且任一层板在位；否则不认为在线
                    boolean anyLayerInPlace = pf.stLayer1NotInPlace == 0 || pf.stLayer2NotInPlace == 0 || pf.stLayer3NotInPlace == 0 || pf.stLayer4NotInPlace == 0 || pf.stLayer5NotInPlace == 0;
                    boolean isOnline = (pf.stPowerLockOn == 0 && anyLayerInPlace);
                    boolean isAbnormal = (pf.stPowerLockOn == 0) && (pf.stLayer1NotInPlace == 1 || pf.stLayer2NotInPlace == 1 || pf.stLayer3NotInPlace == 1 || pf.stLayer4NotInPlace == 1 || pf.stLayer5NotInPlace == 1);
                    discoveredTerminal.setStatus(isAbnormal ? com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ABNORMAL_LOST : (isOnline ? com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ONLINE : com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_OFFLINE));
                    // 同步展示关键指标（电量、电压、RSSI）
                    discoveredTerminal.setBatteryLevel(pf.batteryLevel);
                    discoveredTerminal.setBatteryVoltage(pf.batteryVoltage);
                    discoveredTerminal.setRssi(pf.rssi);
                    discoveredTerminal.parsedFrame = pf;

                    allTerminals.add(discoveredTerminal);
                }
            }

            deviceListAdapter.submitList(allTerminals);
            deviceListAdapter.notifyDataSetChanged();
            updateUI();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "加载终端数据失败", Toast.LENGTH_SHORT).show();
        }
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
        try {
            // 解析hex数据
            LoRaFrameParser.ParsedFrame frameData = LoRaFrameParser.parseFrame(event.getHex());

            android.util.Log.d("DeviceListFragment", "发现新设备: " + new Gson().toJson(frameData));

            if (frameData != null && frameData.deviceId != null) {
                String deviceId = frameData.deviceId;
                
                // 检查设备ID是否已经在终端表中存在
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
                if (!dbHelper.isTerminalExists(deviceId)) {
                    // 检查是否已经在发现列表中显示
                    if (!discoveredDeviceIds.contains(deviceId)) {
                        discoveredDeviceIds.add(deviceId);
                        
                        // 创建一个临时的Terminal对象用于显示
                        Terminal discoveredTerminal = new Terminal();
                        discoveredTerminal.setDeviceId(deviceId);
                        discoveredTerminal.setDeviceName("终端ID：" + deviceId);
                        // 上行事件解析后，按“不是离线/异常即在线”的规则设置状态
                        boolean isAbnormal = (frameData.evIllegalRemoval == 1);
                        discoveredTerminal.setStatus(isAbnormal ? com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ABNORMAL_LOST : com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ONLINE);
                        // 同步展示关键指标（电量、电压、RSSI）
                        discoveredTerminal.setBatteryLevel(frameData.batteryLevel);
                        discoveredTerminal.setBatteryVoltage(frameData.batteryVoltage);
                        discoveredTerminal.setRssi(frameData.rssi);
                        discoveredTerminal.parsedFrame = frameData;
                        // 添加到列表并更新UI
                        allTerminals.add(discoveredTerminal); // 添加到列表顶部

                        updateUI();

                        deviceListAdapter.submitList(allTerminals);
                        deviceListAdapter.notifyDataSetChanged();
                        // 显示发现新设备的提示
                        //Toast.makeText(getContext(), "发现新设备: " + deviceId.substring(deviceId.length() - 4), Toast.LENGTH_SHORT).show();

                        android.util.Log.d("DeviceListFragment", "发现新设备: " + deviceId);
                    }
                } else {
                    // 设备已存在于终端表中
                    android.util.Log.d("DeviceListFragment", "设备 " + deviceId + " 已存在于终端表中");
                }
            }
        } catch (Exception e) {
            android.util.Log.e("DeviceListFragment", "处理上行数据失败", e);
        }
    }
}
