package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.User;
import com.lora.cn.ui.adapter.TerminalLogAdapter;
import com.lora.cn.ui.model.TerminalLog;
import com.blankj.utilcode.util.SPUtils;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class LogInfoFragment extends Fragment {

    private RecyclerView terminalLogRecycle;
    private TerminalLogAdapter terminalLogAdapter;
    private DatabaseManager databaseManager;
    private int currentUserRoleId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log_info, container, false);

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
        
        // 检查查看日志权限
        if (hasPermission("log_info")) {
            initLogData();
        } else {
            Toast.makeText(requireContext(), "您没有查看日志的权限", Toast.LENGTH_SHORT).show();
        }
        
        return view;
    }

    private void initViews(View view) {
        terminalLogRecycle = view.findViewById(R.id.terminal_log_recycle);
    }

    private void initLogData() {
        // 创建模拟日志数据
        List<TerminalLog> logList = new ArrayList<>();
        logList.add(new TerminalLog("2024-01-15 09:30:25", "在线", "终端001", "T001", "张三", "2024-01-15 09:35:00", "查看详情"));
        logList.add(new TerminalLog("2024-01-15 09:28:15", "设备丢失", "终端002", "T002", "李四", "2024-01-15 09:40:30", "查找设备"));
        logList.add(new TerminalLog("2024-01-15 09:25:10", "离线", "终端003", "T003", "", "", "重新连接"));
        logList.add(new TerminalLog("2024-01-15 09:20:45", "低电量报警", "终端004", "T004", "王五", "2024-01-15 09:25:20", "更换电池"));
        logList.add(new TerminalLog("2024-01-15 09:18:30", "在线", "终端005", "T005", "", "", "查看详情"));
        logList.add(new TerminalLog("2024-01-15 09:15:20", "异常丢失", "终端006", "T006", "赵六", "2024-01-15 09:50:15", "查找设备"));
        logList.add(new TerminalLog("2024-01-15 09:12:10", "在线", "终端007", "T007", "", "", "查看详情"));
        logList.add(new TerminalLog("2024-01-15 09:10:05", "离线", "终端008", "T008", "孙七", "2024-01-15 09:30:45", "设备维修"));
        logList.add(new TerminalLog("2024-01-15 09:08:30", "低电量", "终端009", "T009", "", "", "充电提醒"));
        logList.add(new TerminalLog("2024-01-15 09:05:15", "设备丢失", "终端010", "T010", "周八", "2024-01-15 09:15:30", "紧急处理"));

        // 设置RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        terminalLogRecycle.setLayoutManager(layoutManager);
        
        terminalLogAdapter = new TerminalLogAdapter();
        terminalLogRecycle.setAdapter(terminalLogAdapter);
        
        // 设置点击事件监听器
        terminalLogAdapter.setOnItemClickListener((adapter, view, position) -> {
            if (hasPermission("log_info")) {
                TerminalLog log = logList.get(position);
                onLogClick(position, log);
            } else {
                Toast.makeText(requireContext(), "您没有查看日志详情的权限", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 提交数据到适配器
        terminalLogAdapter.submitList(logList);
    }

    private void onLogClick(int position, TerminalLog log) {
        // TODO: 处理日志点击事件
        // 可以显示日志详情或执行相关操作
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

    public static LogInfoFragment newInstance() {
        return new LogInfoFragment();
    }
}