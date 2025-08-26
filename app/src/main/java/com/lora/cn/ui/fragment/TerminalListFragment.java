package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.ui.adapter.TerminalStatusAdapter;
import com.lora.cn.ui.model.TerminalStatus;

import java.util.ArrayList;
import java.util.List;

public class TerminalListFragment extends Fragment {

    private RecyclerView rvTerminalStatus;
    private TerminalStatusAdapter terminalStatusAdapter;
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
    }

    private void initTerminalStatus() {
        // 创建终端状态数据
        List<TerminalStatus> statusList = new ArrayList<>();
        statusList.add(new TerminalStatus("重点关注", R.mipmap.ic_coll, 0));
        statusList.add(new TerminalStatus("在线", R.mipmap.ic_xh_3, 120));
        statusList.add(new TerminalStatus("正常取走", R.mipmap.ic_blue_right, 85));
        statusList.add(new TerminalStatus("异常丢失", R.mipmap.ic_ds, 3));
        statusList.add(new TerminalStatus("设备低电量", R.mipmap.ic_red_sd, 8));
        statusList.add(new TerminalStatus("离线", R.mipmap.ic_xh_no, 12));

        // 设置RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvTerminalStatus.setLayoutManager(layoutManager);
        
        terminalStatusAdapter = new TerminalStatusAdapter();

        rvTerminalStatus.setAdapter(terminalStatusAdapter);
        
        terminalStatusAdapter.submitList(statusList);
    }



    public static TerminalListFragment newInstance() {
        return new TerminalListFragment();
    }
}