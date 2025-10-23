package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.database.entity.User;
import com.lora.cn.ui.adapter.TerminalStatusAdapter;
import com.lora.cn.ui.adapter.TerminalAdapter;
import com.lora.cn.ui.model.TerminalStatus;
import com.lora.cn.ui.model.Terminal;
import com.lora.cn.ui.constants.TerminalStatusConstants;
import com.lora.cn.utils.LoRaProtocolParser;
import com.lora.cn.dialog.AddTerminalDialog;
import com.lora.cn.dialog.TerminalDetailDialog;
import com.lora.cn.ui.activity.DeviceListActivity;
import com.lora.cn.ui.activity.MainActivity;
import com.blankj.utilcode.util.SPUtils;

import java.util.ArrayList;
import java.util.List;

import com.lora.cn.network.MqttPacketsClient;
import com.lora.cn.network.GatewayPacketsClient;
import com.lora.cn.event.TerminalRefreshEvent;
import com.lora.cn.events.UplinkDataEvent;
import com.lora.cn.utils.LoRaFrameParser;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class TerminalListFragment extends Fragment {
    
    private static final String TAG = "TerminalListFragment";

    private RecyclerView rvTerminalStatus;
    private RecyclerView terminalRecycle;
    private TerminalStatusAdapter terminalStatusAdapter;
    private TerminalAdapter adapter;
    private TextView addTerminalBtn;
    private int currentStatusIndex = 0;
    
    // 异常提示窗口相关
    private LinearLayout llErrorNotification;
    private TextView tvErrorMessage;
    private ImageView ivCloseError;
    
    // 数据库管理器
    private DatabaseManager databaseManager;
    private int currentUserRoleId = -1;
    
    // 全局MQTT客户端（用于输出连接与订阅日志）
    private MqttPacketsClient mqttClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal_list, container, false);
        // 初始化数据库管理器
        databaseManager = DatabaseManager.getInstance(requireContext());
        // 初始化用户角色ID
        long userId = SPUtils.getInstance().getLong("current_user_id", -1);
        if (userId != -1) {
            User user = databaseManager.getUserById(userId);
            if (user != null) {
                currentUserRoleId = (int) user.getRoleId();
            }
        }
        initViews(view);
        initTerminalStatus();
        // 避免与 MainActivity 重复连接（MainActivity 已启动全局 MQTT）
        if (getActivity() instanceof com.lora.cn.ui.activity.MainActivity) {
            Log.d(TAG, "MQTT 由 MainActivity 管理，Fragment 不再重复启动");
        } else {
            // 启动全局MQTT连接以输出连接/订阅日志
            startGlobalMqttLogging();
        }
        return view;
    }

    private void startGlobalMqttLogging() {
        try {
            if (mqttClient == null) mqttClient = new MqttPacketsClient();
            final String brokerUrl = "tcp://broker.emqx.io:1883";
            final String clientId = "android-" + System.currentTimeMillis();
            final String topicFilter = "/milesight/uplink/#";
            mqttClient.connectAndSubscribe(requireContext(), brokerUrl, clientId, topicFilter,
                    null, null, false,
                    new GatewayPacketsClient.PacketsListener() {
                        @Override
                        public void onStatus(String msg) {
                            Log.d(TAG, "MQTT状态TerminalList: " + msg);
                        }



                        @Override
                        public void onPackets(java.util.List<com.lora.cn.network.GatewayPacketsClient.PacketRecord> records) {
                            if (records == null || records.isEmpty()) {
                                android.util.Log.d(TAG, "收到上行数据条数: 0");
                                return;
                            }
                            android.util.Log.d(TAG, "收到上行数据条数: " + records.size());
                            for (com.lora.cn.network.GatewayPacketsClient.PacketRecord r : records) {
                                String devEui = r.deviceId != null ? r.deviceId : "-";
                                String devAddr = r.devAddr != null ? r.devAddr : "-";
                                String hex = r.payloadHex != null ? r.payloadHex : "-";
                                String dr = r.dr != null ? r.dr : "-";
                                String time = r.time != null ? r.time : "-";
                                String freq = r.freq != null ? String.valueOf(r.freq) : "-";
                                String rssi = r.rssi != null ? String.valueOf(r.rssi) : "-";
                                String snr = r.snr != null ? String.valueOf(r.snr) : "-";
                                String fport = r.fport != null ? String.valueOf(r.fport) : "-";
                                String fcnt = r.fcnt != null ? String.valueOf(r.fcnt) : "-";
                                android.util.Log.i(TAG,
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

    @Override
    public void onResume() {
        super.onResume();
        // 注册EventBus
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        // 加载终端列表
        loadTerminals();
    }

    @Override
    public void onPause() {
        super.onPause();
        // 注销EventBus
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (mqttClient != null) {
                mqttClient.disconnect();
            }
        } catch (Exception e) {
            Log.w(TAG, "断开MQTT时异常: " + e.getMessage());
        }
    }

    private void initViews(View view) {
        rvTerminalStatus = view.findViewById(R.id.rv_terminal_status);
        terminalRecycle = view.findViewById(R.id.terminal_recycle);
        addTerminalBtn = view.findViewById(R.id.add_terminal);
        
        // 初始化异常提示窗口控件
        llErrorNotification = view.findViewById(R.id.ll_error_notification);
        tvErrorMessage = view.findViewById(R.id.tv_error_message);
        ivCloseError = view.findViewById(R.id.iv_close_error);
        
        //TextView nearbyDevicesBtn = view.findViewById(R.id.nearby_devices);
        
        // 设置添加终端按钮点击事件
        addTerminalBtn.setOnClickListener(v -> {
            if (hasPermission("terminal_add")) {
                // 跳转到MainActivity的设备列表页面
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showDeviceList();
                }
            } else {
                Toast.makeText(requireContext(), "您没有添加终端的权限", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 设置异常提示窗口关闭按钮点击事件
        ivCloseError.setOnClickListener(v -> {
            hideErrorNotification();
        });
        
//        // 设置附近终端按钮点击事件
//        nearbyDevicesBtn.setOnClickListener(v -> {
//            // 跳转到设备列表Activity
//            DeviceListActivity.start(requireContext());
//        });
    }

    private void initTerminalStatus() {
        // 使用常量类获取终端状态数据
        List<TerminalStatus> statusList = TerminalStatusConstants.getDefaultStatusList();

        // 设置状态RecyclerView
        LinearLayoutManager statusLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvTerminalStatus.setLayoutManager(statusLayoutManager);
        
        terminalStatusAdapter = new TerminalStatusAdapter();
        rvTerminalStatus.setAdapter(terminalStatusAdapter);
        
        terminalStatusAdapter.submitList(statusList);
        
        // 初始化终端列表
        initTerminalList();
    }
    
    private void initTerminalList() {
        // 创建示例终端数据
        List<Terminal> terminalList = new ArrayList<>();


        // 设置终端列表RecyclerView
        GridLayoutManager terminalLayoutManager = new GridLayoutManager(getContext(), 4);
        terminalRecycle.setLayoutManager(terminalLayoutManager);
        
        adapter = new TerminalAdapter();
        terminalRecycle.setAdapter(adapter);
        
        adapter.submitList(terminalList);
        
        // 设置终端点击事件监听器
        adapter.setOnItemClickListener((adapter, view, position) -> {
            if (hasPermission("terminal_detail")) {
                Terminal terminal = terminalList.get(position);
                onTerminalClick(position, terminal);
            } else {
                Toast.makeText(requireContext(), "您没有查看终端详情的权限", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onTerminalClick(int position, Terminal terminal) {
        // 将UI层的 Terminal 映射为协议层的 TerminalInfo，用于详情弹窗
        LoRaProtocolParser.TerminalInfo info = new LoRaProtocolParser.TerminalInfo();
        info.deviceId = terminal.getTerminalId();
        // 优先使用数据库中的 terminalName，其次回退示例数据的 name
        info.deviceName = terminal.getTerminalName() != null && !terminal.getTerminalName().isEmpty()
                ? terminal.getTerminalName() : terminal.getName();
        info.department = terminal.getDepartment();
        info.location = terminal.getLocation();
        info.signalStrength = terminal.getSignalStrength();
        info.status = mapStatusToInt(terminal.getStatus(), terminal.getStatusText());
        info.batteryLevel = parseBatteryPercent(terminal.getBatteryText());
        info.timestamp = System.currentTimeMillis();
        info.payloadHex = ""; // 列表数据无原始HEX，详情支持刷新获取

        TerminalDetailDialog dialog = new TerminalDetailDialog(requireContext(), info);
        dialog.show();
    }

    private int parseBatteryPercent(String batteryText) {
        if (batteryText == null) return 0;
        try {
            String t = batteryText.trim();
            if (t.endsWith("%")) {
                t = t.substring(0, t.length() - 1);
            }
            return Math.max(0, Math.min(100, Integer.parseInt(t)));
        } catch (Exception e) {
            return 0;
        }
    }

    private int mapStatusToInt(String status, String statusText) {
        String s = status != null ? status : statusText;
        if (s == null) return 1;
        switch (s) {
            case "在线":
                return 1;
            case "离线":
                return 0;
            case "异常":
                return 2;
            default:
                return 1;
        }
    }
    
    /**
     * 添加终端按钮点击事件
     */
    private void onAddTerminalClick() {
        // 检查权限
        if (!hasPermission("terminal_add")) {
            Toast.makeText(getContext(), "您没有添加终端的权限", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示添加终端对话框
        AddTerminalDialog dialog = new AddTerminalDialog(getContext());
        dialog.setOnTerminalAddedListener(this::addNewTerminalToDatabase);
        dialog.show();
    }
    
    /**
     * 将新终端添加到数据库
     */
    private void addNewTerminalToDatabase(LoRaProtocolParser.TerminalInfo terminalInfo) {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
            
            // 检查终端是否已存在
            if (dbHelper.isTerminalExists(terminalInfo.deviceId)) {
                Toast.makeText(getContext(), "终端设备已存在", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 创建Terminal对象
            Terminal terminal = new Terminal();
            terminal.setTerminalId(terminalInfo.deviceId);
            terminal.setTerminalName(terminalInfo.deviceName);
            terminal.setStatus("在线");
            terminal.setSignalStrength(terminalInfo.signalStrength);
            terminal.setDepartment(terminalInfo.department);
            terminal.setLocation(terminalInfo.location);
            
            // 添加到数据库
            long result = dbHelper.addTerminal(terminal);
            
            if (result > 0) {
                Toast.makeText(getContext(), "终端添加成功", Toast.LENGTH_SHORT).show();
                // 重新加载终端列表
                loadTerminals();
            } else {
                Toast.makeText(getContext(), "终端添加失败", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "添加终端到数据库失败", e);
            Toast.makeText(getContext(), "添加终端失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 检查当前用户是否有指定权限
     */
    private boolean hasPermission(String permissionCode) {
        if (currentUserRoleId == -1) {
            return false;
        }
        return databaseManager.hasPermission(currentUserRoleId, permissionCode);
    }

    public static TerminalListFragment newInstance() {
        return new TerminalListFragment();
    }
    
    /**
     * 显示异常提示窗口
     */
    private void showErrorNotification(String message) {
        if (llErrorNotification != null && tvErrorMessage != null) {
            tvErrorMessage.setText(message);
            llErrorNotification.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 隐藏异常提示窗口
     */
    private void hideErrorNotification() {
        if (llErrorNotification != null) {
            llErrorNotification.setVisibility(View.GONE);
        }
    }
    
    /**
     * 检查异常情况并显示提示
     */
    private void checkForErrors() {
        // 检查是否有异常丢失的code情况
        // 这里可以根据实际业务逻辑来判断异常情况
        // 例如：检查终端连接状态、数据传输异常等
        
        // 示例：检查是否有离线终端或数据异常
        List<Terminal> terminals = adapter != null ? adapter.getItems() : new ArrayList<>();
        int offlineCount = 0;
        int errorCount = 0;
        
        for (Terminal terminal : terminals) {
            if ("离线".equals(terminal.getStatus()) || "0".equals(terminal.getStatus())) { // 检查离线状态
                offlineCount++;
            } else if ("异常".equals(terminal.getStatus()) || "2".equals(terminal.getStatus())) { // 检查异常状态
                errorCount++;
            }
        }
        
        if (errorCount > 0) {
            showErrorNotification("检测到 " + errorCount + " 个终端异常丢失code");
        } else if (offlineCount > 3) { // 如果离线终端超过3个也显示提示
            showErrorNotification("检测到 " + offlineCount + " 个终端离线，请检查连接");
        } else {
            hideErrorNotification();
        }
    }

    /**
     * EventBus接收上行数据事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUplinkDataReceived(UplinkDataEvent event) {
        try {
            // 解析hex数据
            LoRaFrameParser.ParsedFrame frameData = LoRaFrameParser.parseFrame(event.getHex());
            if (frameData != null && frameData.deviceId != null) {
                String deviceId = frameData.deviceId;
                
                // 检查设备ID是否已经在终端表中存在
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
                if (dbHelper.isTerminalExists(deviceId)) {
                    // 如果终端已存在，更新终端表中的上行数据相关字段
                    updateTerminalUplinkData(deviceId, frameData);
                    
                    // 刷新终端列表显示
                    loadTerminals();
                    
                    // 检查是否有异常情况需要提示
                    checkForErrors();
                    
                    Log.d(TAG, "更新终端 " + deviceId + " 的上行数据");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "处理上行数据失败", e);
        }
    }

    /**
     * 更新终端表中的上行数据相关字段
     */
    private void updateTerminalUplinkData(String deviceId, LoRaFrameParser.ParsedFrame frameData) {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
            
            // 获取现有终端信息
            Terminal terminal = dbHelper.getTerminalByDeviceId(deviceId);
            if (terminal != null) {
                // 更新上行数据相关字段
                terminal.setDataTime(frameData.dataTime != null ? frameData.dataTime : new java.util.Date());
                terminal.setDeviceEvent((long) frameData.deviceEvent);
                terminal.setDeviceStatus((long) frameData.deviceStatus);
                terminal.setBatteryVoltage(frameData.batteryVoltage);
                terminal.setBatteryLevel(frameData.batteryLevel);
                terminal.setRssi(frameData.rssi);
                terminal.setDepartmentNumber(frameData.departmentNumber);
                terminal.setCartNumber(frameData.cartNumber);
                terminal.setDeviceCount(frameData.deviceCount);
                terminal.setRackNumber(frameData.rackNumber);
                
                // 更新新增的字段
                terminal.setFunctionCode(frameData.functionCode);
                terminal.setSequenceNumber(frameData.sequenceNumber);
                terminal.setDataLength(frameData.dataLength);
                terminal.setDataContent(frameData.dataContent);
                terminal.setChecksum(frameData.checksum);
                
                // 更新数据库
                boolean success = dbHelper.updateTerminal(terminal);
                if (success) {
                    Log.d(TAG, "成功更新终端 " + deviceId + " 的上行数据");
                } else {
                    Log.e(TAG, "更新终端 " + deviceId + " 的上行数据失败");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "更新终端上行数据时发生异常", e);
        }
    }

    /**
     * EventBus接收终端刷新事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTerminalRefreshEvent(TerminalRefreshEvent event) {
        Log.d(TAG, "收到终端刷新事件: " + event.getMessage());
        // 刷新终端列表
        loadTerminals();
        // 显示提示信息
        if (getContext() != null) {
            Toast.makeText(getContext(), "终端列表已刷新", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 加载终端列表数据
     */
    private void loadTerminals() {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
            List<Terminal> terminals = dbHelper.getAllTerminals();
            
            if (terminals != null && !terminals.isEmpty()) {
                // 更新适配器数据
                if (adapter != null) {
                    adapter.updateTerminals(terminals);
                    
                    // 加载终端后检查异常情况
                    checkForErrors();
                }
            } else {
                // 如果数据库中没有数据，显示示例数据
                initTerminalList();
            }
        } catch (Exception e) {
            Log.e(TAG, "加载终端列表失败", e);
            // 出错时显示示例数据
            initTerminalList();
        }
    }
}