package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

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
import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.event.TerminalRefreshEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class TerminalListFragment extends Fragment {
    
    private static final String TAG = "TerminalListFragment";

    private RecyclerView rvTerminalStatus;
    private RecyclerView terminalRecycle;
    private TerminalStatusAdapter terminalStatusAdapter;
    private TerminalAdapter adapter;
    private TextView addTerminalBtn;
    private int currentStatusIndex = 0;
    
    // 数据库管理器
    private DatabaseManager databaseManager;
    private int currentUserRoleId = -1;

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
                currentUserRoleId = (int)user.getRoleId();
            }
        }
        
        initViews(view);
        
        // 检查查看终端列表权限（修正为正确的权限码：terminal_list）
        if (hasPermission("terminal_list")) {
            initTerminalStatus();
        } else {
            Toast.makeText(requireContext(), "您没有查看终端列表的权限", Toast.LENGTH_SHORT).show();
        }
        
        return view;
    }

    private void initViews(View view) {
        rvTerminalStatus = view.findViewById(R.id.rv_terminal_status);
        terminalRecycle = view.findViewById(R.id.terminal_recycle);
        addTerminalBtn = view.findViewById(R.id.add_terminal);
        
        // 设置添加终端按钮点击事件
        addTerminalBtn.setOnClickListener(v -> {
            if (hasPermission("terminal_add")) {
                onAddTerminalClick();
            } else {
                Toast.makeText(requireContext(), "您没有添加终端的权限", Toast.LENGTH_SHORT).show();
            }
        });

        // 搜索设备：点击搜索图标展示设备列表Fragment
        EditText searchEditText = view.findViewById(R.id.et_search);
        if (searchEditText != null) {
            searchEditText.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    android.graphics.drawable.Drawable right = searchEditText.getCompoundDrawables()[2];
                    if (right != null && event.getX() >= (searchEditText.getWidth() - searchEditText.getPaddingRight() - right.getBounds().width())) {
                        // 展示设备列表
                        if (getActivity() instanceof com.lora.cn.ui.activity.MainActivity) {
                            com.lora.cn.ui.activity.MainActivity mainActivity = (com.lora.cn.ui.activity.MainActivity) getActivity();
                            mainActivity.showDeviceList();
                        }
                        // 记录日志
                        try {
                            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
                            com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
                            logInfo.setTerminalId("SYSTEM");
                            logInfo.setTerminalName("终端列表页");
                            logInfo.setDeviceId("SYSTEM");
                            logInfo.setStatus("成功");
                            logInfo.setOperator("系统管理员");
                            logInfo.setAction("打开搜索设备界面");
                            logInfo.setOperationTime(String.valueOf(System.currentTimeMillis()));
                            logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
                            dbHelper.addLog(logInfo);
                        } catch (Exception e) {
                            Log.e(TAG, "记录日志失败", e);
                        }
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void initTerminalStatus() {
        // 从数据库获取真实的终端统计数据
        updateTerminalStatusFromDatabase();
        
        // 设置状态RecyclerView
        LinearLayoutManager statusLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvTerminalStatus.setLayoutManager(statusLayoutManager);
        
        terminalStatusAdapter = new TerminalStatusAdapter();
        rvTerminalStatus.setAdapter(terminalStatusAdapter);
        
        // 初始化终端列表
        initTerminalList();
    }
    
    /**
     * 从数据库更新终端状态统计
     */
    private void updateTerminalStatusFromDatabase() {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
            List<com.lora.cn.ui.model.Terminal> allTerminals = dbHelper.getAllTerminals();
            
            // 统计各种状态的终端数量
            int favoriteCount = 0;
            int onlineCount = 0;
            int normalTakenCount = 0;
            int abnormalLostCount = 0;
            int lowBatteryCount = 0;
            int offlineCount = 0;
            
            for (com.lora.cn.ui.model.Terminal terminal : allTerminals) {
                // 统计收藏数量
                if (terminal.isFavorite()) {
                    favoriteCount++;
                }
                
                // 统计状态数量
                String status = terminal.getStatus();
                if ("在线".equals(status)) {
                    onlineCount++;
                } else if ("离线".equals(status)) {
                    offlineCount++;
                } else if ("异常".equals(status)) {
                    abnormalLostCount++;
                }
                
                // 统计电量状态
                int batteryLevel = terminal.getBatteryLevel();
                if (batteryLevel <= 20) {
                    lowBatteryCount++;
                } else if (batteryLevel > 60) {
                    normalTakenCount++;
                }
            }
            
            // 创建状态列表
            List<TerminalStatus> statusList = new ArrayList<>();
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_IMPORTANT, R.mipmap.ic_coll, favoriteCount));
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_ONLINE, R.mipmap.ic_xh_3, onlineCount));
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_NORMAL_TAKEN, R.mipmap.ic_blue_right, normalTakenCount));
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_ABNORMAL_LOST, R.mipmap.ic_ds, abnormalLostCount));
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_LOW_BATTERY, R.mipmap.ic_red_sd, lowBatteryCount));
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_OFFLINE, R.mipmap.ic_xh_no, offlineCount));
            
            // 更新适配器数据
            if (terminalStatusAdapter != null) {
                terminalStatusAdapter.submitList(statusList);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "更新终端状态统计失败", e);
            // 出错时使用默认数据
            List<TerminalStatus> statusList = TerminalStatusConstants.getDefaultStatusList();
            if (terminalStatusAdapter != null) {
                terminalStatusAdapter.submitList(statusList);
            }
        }
    }
    
    private void initTerminalList() {
        // 首先尝试从数据库加载终端数据
        loadTerminals();
    }

    @Override
    public void onStart() {
        super.onStart();
        // 注册事件总线，监听新增终端事件
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            // 返回页面时刷新终端列表和状态统计，确保展示最新添加的设备
            loadTerminals();
            updateTerminalStatusFromDatabase();
        } catch (Exception e) {
            Log.e(TAG, "onResume 刷新终端列表失败", e);
        }
    }

    @Override
    public void onStop() {
        // 取消注册事件总线
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        super.onStop();
    }

    // 订阅终端刷新事件，收到后立即刷新列表与状态
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTerminalRefreshEvent(TerminalRefreshEvent event) {
        try {
            loadTerminals();
            updateTerminalStatusFromDatabase();
            if (getContext() != null) {
                Toast.makeText(getContext(), event != null ? event.getMessage() : "终端列表已刷新", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "处理TerminalRefreshEvent失败", e);
        }
    }

    private void onTerminalClick(int position, Terminal terminal) {
        try {
            // 从数据库获取完整的终端信息
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
            com.lora.cn.ui.model.Terminal dbTerminal = null;
            
            // 通过终端ID查找数据库中的终端信息
            List<com.lora.cn.ui.model.Terminal> allTerminals = dbHelper.getAllTerminals();
            for (com.lora.cn.ui.model.Terminal t : allTerminals) {
                if (t.getTerminalId().equals(terminal.getTerminalId())) {
                    dbTerminal = t;
                    break;
                }
            }
            
            if (dbTerminal != null) {
                // 创建TerminalInfo对象用于显示详情
                LoRaProtocolParser.TerminalInfo terminalInfo = new LoRaProtocolParser.TerminalInfo();
                terminalInfo.deviceId = dbTerminal.getTerminalId();
                terminalInfo.deviceName = dbTerminal.getTerminalName();
                terminalInfo.department = dbTerminal.getDepartment();
                terminalInfo.location = dbTerminal.getLocation();
                terminalInfo.signalStrength = dbTerminal.getSignalStrength();
                terminalInfo.batteryLevel = dbTerminal.getBatteryLevel();
                terminalInfo.status = "在线".equals(dbTerminal.getStatus()) ? 1 : 0;
                terminalInfo.timestamp = System.currentTimeMillis();
                terminalInfo.payloadHex = ""; // 可以从日志中获取最新的payload
                
                // 显示终端详情对话框
                com.lora.cn.dialog.TerminalDetailDialog dialog = 
                    new com.lora.cn.dialog.TerminalDetailDialog(getContext(), terminalInfo);
                dialog.show();
            } else {
                Toast.makeText(getContext(), "未找到终端详细信息", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "显示终端详情失败", e);
            Toast.makeText(getContext(), "显示终端详情失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        
        // 展示附近设备列表（DeviceListFragment）
        if (getActivity() instanceof com.lora.cn.ui.activity.MainActivity) {
            com.lora.cn.ui.activity.MainActivity mainActivity = (com.lora.cn.ui.activity.MainActivity) getActivity();
            mainActivity.showDeviceList();
        } else {
            Toast.makeText(getContext(), "无法打开设备列表", Toast.LENGTH_SHORT).show();
        }
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
            com.lora.cn.ui.model.Terminal terminal = new com.lora.cn.ui.model.Terminal();
            terminal.setTerminalId(terminalInfo.deviceId);
            terminal.setTerminalName(terminalInfo.deviceName);
            terminal.setStatus("在线");
            terminal.setSignalStrength(terminalInfo.signalStrength);
            terminal.setDepartment(terminalInfo.department);
            terminal.setLocation(terminalInfo.location);
            
            // 添加到数据库
            long result = dbHelper.addTerminal(terminal);
            
            if (result > 0) {
                // 记录添加终端的日志
                com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
                logInfo.setTerminalId(terminalInfo.deviceId);
                logInfo.setTerminalName(terminalInfo.deviceName);
                logInfo.setDeviceId(terminalInfo.deviceId);
                logInfo.setStatus("成功");
                logInfo.setOperator("系统管理员"); // 这里可以根据实际登录用户设置
                logInfo.setAction("添加终端");
                logInfo.setOperationTime(String.valueOf(System.currentTimeMillis()));
                logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
                
                dbHelper.addLog(logInfo);
                
                Toast.makeText(getContext(), "终端添加成功", Toast.LENGTH_SHORT).show();
                // 重新加载终端列表
                loadTerminals();
            } else {
                // 记录添加失败的日志
                com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
                logInfo.setTerminalId(terminalInfo.deviceId);
                logInfo.setTerminalName(terminalInfo.deviceName);
                logInfo.setDeviceId(terminalInfo.deviceId);
                logInfo.setStatus("失败");
                logInfo.setOperator("系统管理员");
                logInfo.setAction("添加终端");
                logInfo.setOperationTime(String.valueOf(System.currentTimeMillis()));
                logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
                
                dbHelper.addLog(logInfo);
                
                Toast.makeText(getContext(), "终端添加失败", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "添加终端到数据库失败", e);
            
            // 记录异常日志
            try {
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
                com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
                logInfo.setTerminalId(terminalInfo.deviceId);
                logInfo.setTerminalName(terminalInfo.deviceName);
                logInfo.setDeviceId(terminalInfo.deviceId);
                logInfo.setStatus("异常");
                logInfo.setOperator("系统管理员");
                logInfo.setAction("添加终端");
                logInfo.setOperationTime(String.valueOf(System.currentTimeMillis()));
                logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
                
                dbHelper.addLog(logInfo);
            } catch (Exception logException) {
                Log.e(TAG, "记录日志失败", logException);
            }
            
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
     * 加载终端列表数据
     */
    private void loadTerminals() {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
            List<Terminal> terminals = dbHelper.getAllTerminals();
            
            // 设置终端列表RecyclerView（如果还没有设置）
            if (adapter == null) {
                GridLayoutManager terminalLayoutManager = new GridLayoutManager(getContext(), 4);
                terminalRecycle.setLayoutManager(terminalLayoutManager);
                
                adapter = new TerminalAdapter();
                adapter.setOnFavoriteClickListener(new TerminalAdapter.OnFavoriteClickListener() {
                    @Override
                    public void onFavoriteClick(Terminal terminal, boolean isFavorite) {
                        try {
                            // 更新数据库中的收藏状态
                            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
                            int result = dbHelper.updateTerminalFavoriteStatus(terminal.getTerminalId(), isFavorite);
                            
                            // 记录收藏状态变更日志
                            com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
                            logInfo.setTerminalId(terminal.getTerminalId());
                            logInfo.setTerminalName(terminal.getTerminalName());
                            logInfo.setDeviceId(terminal.getTerminalId());
                            logInfo.setStatus(result > 0 ? "成功" : "失败");
                            logInfo.setOperator("系统管理员"); // 这里可以根据实际登录用户设置
                            logInfo.setAction(isFavorite ? "收藏终端" : "取消收藏");
                            logInfo.setOperationTime(String.valueOf(System.currentTimeMillis()));
                logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
                            
                            dbHelper.addLog(logInfo);
                            
                            // 更新终端状态统计
                            updateTerminalStatusFromDatabase();
                        } catch (Exception e) {
                            Log.e(TAG, "更新收藏状态失败", e);
                            
                            // 记录异常日志
                            try {
                                DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
                                com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
                                logInfo.setTerminalId(terminal.getTerminalId());
                                logInfo.setTerminalName(terminal.getTerminalName());
                                logInfo.setDeviceId(terminal.getTerminalId());
                                logInfo.setStatus("异常");
                                logInfo.setOperator("系统管理员");
                                logInfo.setAction(isFavorite ? "收藏终端" : "取消收藏");
                                logInfo.setOperationTime(String.valueOf(System.currentTimeMillis()));
                logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
                                
                                dbHelper.addLog(logInfo);
                            } catch (Exception logException) {
                                Log.e(TAG, "记录日志失败", logException);
                            }
                        }
                    }
                });
                terminalRecycle.setAdapter(adapter);
                
                // 设置终端点击事件监听器
                adapter.setOnItemClickListener((adapter, view, position) -> {
                    // 查看终端详情权限（修正为正确的权限码：terminal_detail）
                    if (hasPermission("terminal_detail")) {
                        Terminal terminal = (Terminal) adapter.getItem(position);
                        onTerminalClick(position, terminal);
                    } else {
                        Toast.makeText(requireContext(), "您没有查看终端详情的权限", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            if (terminals != null && !terminals.isEmpty()) {
                // 转换数据库终端数据为UI显示格式
                List<Terminal> displayTerminals = convertToDisplayTerminals(terminals);
                adapter.submitList(displayTerminals);
            } else {
                // 如果数据库中没有数据，显示空列表
                adapter.submitList(new ArrayList<>());
            }
        } catch (Exception e) {
            Log.e(TAG, "加载终端列表失败", e);
            // 出错时显示空列表
            if (adapter != null) {
                adapter.submitList(new ArrayList<>());
            }
        }
    }
    
    /**
     * 将数据库终端数据转换为UI显示格式
     */
    private List<Terminal> convertToDisplayTerminals(List<Terminal> dbTerminals) {
        List<Terminal> displayTerminals = new ArrayList<>();
        
        for (Terminal dbTerminal : dbTerminals) {
            Terminal displayTerminal = new Terminal();
            
            // 设置基本信息
            displayTerminal.setId(dbTerminal.getId());
            displayTerminal.setTerminalId(dbTerminal.getTerminalId());
            displayTerminal.setTerminalName(dbTerminal.getTerminalName());
            displayTerminal.setName(dbTerminal.getTerminalName()); // 显示名称使用终端名称
            displayTerminal.setDepartment(dbTerminal.getDepartment());
            displayTerminal.setLocation(dbTerminal.getLocation());
            displayTerminal.setStatus(dbTerminal.getStatus());
            displayTerminal.setSignalStrength(dbTerminal.getSignalStrength());
            displayTerminal.setFavorite(dbTerminal.isFavorite());
            
            // 根据状态设置图标
            int statusIcon = getStatusIcon(dbTerminal.getStatus());
            displayTerminal.setStatusIconResId(statusIcon);
            displayTerminal.setStatusText(dbTerminal.getStatus());
            
            // 设置电量信息
            displayTerminal.setBatteryLevel(dbTerminal.getBatteryLevel());
            displayTerminal.setBatteryText(dbTerminal.getBatteryLevel() + "%");
            
            // 根据电量设置电池图标
            if (dbTerminal.getBatteryLevel() > 80) {
                displayTerminal.setBatteryIconResId(R.mipmap.ic_blue_right); // 使用蓝色对勾图标表示高电量
            } else if (dbTerminal.getBatteryLevel() > 30) {
                displayTerminal.setBatteryIconResId(R.mipmap.ic_baterery_low); // 使用低电量图标表示中等电量
            } else {
                displayTerminal.setBatteryIconResId(R.mipmap.ic_red_sd); // 红色电池表示低电量
            }
            
            // 设置重要性（收藏状态）
            displayTerminal.setImportant(dbTerminal.isFavorite());
            
            displayTerminals.add(displayTerminal);
        }
        
        return displayTerminals;
    }
    
    /**
     * 根据状态获取对应的图标资源ID
     */
    private int getStatusIcon(String status) {
        if (status == null) {
            return R.mipmap.ic_xh_no;
        }
        
        switch (status) {
            case "在线":
                return R.mipmap.ic_xh_3;
            case "异常":
                return R.mipmap.ic_ds;
            case "离线":
            default:
                return R.mipmap.ic_xh_no;
        }
    }
}