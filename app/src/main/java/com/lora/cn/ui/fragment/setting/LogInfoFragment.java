package com.lora.cn.ui.fragment.setting;

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
import com.lora.cn.ui.adapter.TerminalLogAdapter;
import com.lora.cn.ui.model.TerminalLog;

import java.util.ArrayList;
import java.util.List;

public class LogInfoFragment extends Fragment {

    private RecyclerView terminalLogRecycle;
    private TerminalLogAdapter terminalLogAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log_info, container, false);

        initViews(view);
        initLogData();
        
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
            TerminalLog log = logList.get(position);
            onLogClick(position, log);
        });
        
        // 提交数据到适配器
        terminalLogAdapter.submitList(logList);
    }

    private void onLogClick(int position, TerminalLog log) {
        // TODO: 处理日志点击事件
        // 可以显示日志详情或执行相关操作
    }

    public static LogInfoFragment newInstance() {
        return new LogInfoFragment();
    }
}