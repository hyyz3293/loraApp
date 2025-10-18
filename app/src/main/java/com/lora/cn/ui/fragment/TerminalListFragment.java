package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
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
import com.blankj.utilcode.util.SPUtils;

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
        
        // 检查查看终端列表权限
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