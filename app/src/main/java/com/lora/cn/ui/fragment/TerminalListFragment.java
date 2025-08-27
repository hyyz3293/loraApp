package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.ui.adapter.TerminalStatusAdapter;
import com.lora.cn.ui.adapter.TerminalAdapter;
import com.lora.cn.ui.model.TerminalStatus;
import com.lora.cn.ui.model.Terminal;
import com.lora.cn.ui.constants.TerminalStatusConstants;

import java.util.ArrayList;
import java.util.List;

public class TerminalListFragment extends Fragment {

    private RecyclerView rvTerminalStatus;
    private RecyclerView terminalRecycle;
    private TerminalStatusAdapter terminalStatusAdapter;
    private TerminalAdapter terminalAdapter;
    private int currentStatusIndex = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal_list, container, false);
        
        initViews(view);
        initTerminalStatus();
        
        return view;
    }

    private void initViews(View view) {
        rvTerminalStatus = view.findViewById(R.id.rv_terminal_status);
        terminalRecycle = view.findViewById(R.id.terminal_recycle);
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
        terminalList.add(new Terminal("终端001", "科室一", "病房101", R.mipmap.ic_xh_3, "在线", R.mipmap.ic_red_sd, "85%", true));
        terminalList.add(new Terminal("终端002", "科室一", "病房102", R.mipmap.ic_xh_3, "在线", R.mipmap.ic_red_sd, "92%", false));
        terminalList.add(new Terminal("终端003", "科室二", "病房201", R.mipmap.ic_ds, "异常", R.mipmap.ic_red_sd, "15%", true));
        terminalList.add(new Terminal("终端004", "科室二", "病房202", R.mipmap.ic_xh_no, "离线", R.mipmap.ic_red_sd, "0%", false));
        
        // 设置终端列表RecyclerView
        GridLayoutManager terminalLayoutManager = new GridLayoutManager(getContext(), 5);
        terminalRecycle.setLayoutManager(terminalLayoutManager);
        
        terminalAdapter = new TerminalAdapter();
        terminalRecycle.setAdapter(terminalAdapter);
        
        terminalAdapter.submitList(terminalList);
    }

    private void onTerminalClick(int position, Terminal terminal) {
        // TODO: 处理终端点击事件
        // 可以跳转到终端详情页面或显示更多信息
    }

    public static TerminalListFragment newInstance() {
        return new TerminalListFragment();
    }
}