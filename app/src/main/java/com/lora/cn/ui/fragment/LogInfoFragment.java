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
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.ui.adapter.LogInfoAdapter;
import com.lora.cn.ui.model.LogInfo;

import java.util.List;

public class LogInfoFragment extends Fragment {

    private RecyclerView recyclerView;
    private LogInfoAdapter logInfoAdapter;
    private DatabaseHelper databaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log_info, container, false);
        initViews(view);
        initLogData();
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        databaseHelper = new DatabaseHelper(getContext());
    }

    private void initLogData() {
        // 初始化示例日志数据
        databaseHelper.initSampleLogData();
        
        // 从数据库获取真实日志数据
        List<LogInfo> logList = databaseHelper.getAllLogs();
        
        logInfoAdapter = new LogInfoAdapter();
        logInfoAdapter.submitList(logList);
        recyclerView.setAdapter(logInfoAdapter);
        
        // 设置点击事件
        logInfoAdapter.setOnItemClickListener((adapter, view, position) -> {
            LogInfo log = logList.get(position);
            // 处理日志项点击事件
        });
    }
}