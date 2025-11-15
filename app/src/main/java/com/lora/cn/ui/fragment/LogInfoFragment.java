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

import java.util.ArrayList;
import java.util.List;

public class LogInfoFragment extends Fragment {

    private RecyclerView recyclerView;
    private LogInfoAdapter logInfoAdapter;
    private DatabaseHelper databaseHelper;
    List<LogInfo> logList = new ArrayList<>();
    private android.view.View rlStart;
    private android.view.View rlEnd;
    private android.widget.TextView tvStart;
    private android.widget.TextView tvEnd;
    private String selectedStartTime = "";
    private String selectedEndTime = "";
    private List<LogInfo> baseLogs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log_info, container, false);
        initViews(view);
        initLogData();
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.terminal_log_recycle);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        databaseHelper = DatabaseHelper.getInstance(getContext());
        rlStart = view.findViewById(R.id.time_start_time);
        rlEnd = view.findViewById(R.id.time_end_time);
        tvStart = view.findViewById(R.id.time_start_time_tv);
        tvEnd = view.findViewById(R.id.time_end_time_tv);
        if (rlStart != null) rlStart.setOnClickListener(v -> showStartPicker());
        if (rlEnd != null) rlEnd.setOnClickListener(v -> showEndPicker());
    }

    private void initLogData() {
        // 先清理示例日志，避免展示假数据
        databaseHelper.cleanSampleLogData();
        
        // 从数据库获取真实日志数据
        logList = databaseHelper.getAllLogs();
        baseLogs.clear();
        if (logList != null) {
            for (LogInfo li : logList) {
                String act = li != null ? li.getAction() : null;
                if (act != null && (act.startsWith("接收上行数据") || act.startsWith("发送下行数据") || act.contains("功能码=") || act.contains("下行"))) {
                    baseLogs.add(li);
                }
            }
        }
        logInfoAdapter = new LogInfoAdapter();
        recyclerView.setAdapter(logInfoAdapter);
        applyTimeFilter();
        
        // 设置点击事件
        logInfoAdapter.setOnItemClickListener((adapter, view, position) -> {
            LogInfo log = logList.get(position);
            // 处理日志项点击事件
        });
    }

    private void showStartPicker() {
        android.app.DatePickerDialog dp = new android.app.DatePickerDialog(requireContext());
        dp.setOnDateSetListener((view, year, month, day) -> {
            android.app.TimePickerDialog tp = new android.app.TimePickerDialog(requireContext(), (v, hour, minute) -> {
                java.util.Calendar c = java.util.Calendar.getInstance();
                c.set(java.util.Calendar.YEAR, year);
                c.set(java.util.Calendar.MONTH, month);
                c.set(java.util.Calendar.DAY_OF_MONTH, day);
                c.set(java.util.Calendar.HOUR_OF_DAY, hour);
                c.set(java.util.Calendar.MINUTE, minute);
                c.set(java.util.Calendar.SECOND, 0);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                selectedStartTime = sdf.format(c.getTime());
                if (tvStart != null) tvStart.setText(selectedStartTime);
                applyTimeFilter();
            }, 0, 0, true);
            tp.show();
        });
        dp.show();
    }

    private void showEndPicker() {
        android.app.DatePickerDialog dp = new android.app.DatePickerDialog(requireContext());
        dp.setOnDateSetListener((view, year, month, day) -> {
            android.app.TimePickerDialog tp = new android.app.TimePickerDialog(requireContext(), (v, hour, minute) -> {
                java.util.Calendar c = java.util.Calendar.getInstance();
                c.set(java.util.Calendar.YEAR, year);
                c.set(java.util.Calendar.MONTH, month);
                c.set(java.util.Calendar.DAY_OF_MONTH, day);
                c.set(java.util.Calendar.HOUR_OF_DAY, hour);
                c.set(java.util.Calendar.MINUTE, minute);
                c.set(java.util.Calendar.SECOND, 59);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                selectedEndTime = sdf.format(c.getTime());
                if (tvEnd != null) tvEnd.setText(selectedEndTime);
                applyTimeFilter();
            }, 23, 59, true);
            tp.show();
        });
        dp.show();
    }

    private long parseMillis(String s) {
        if (s == null || s.isEmpty()) return -1L;
        try {
            if (s.matches("\\d+")) {
                long v = Long.parseLong(s);
                if (s.length() <= 10) return v * 1000L;
                return v;
            }
        } catch (Exception ignored) {}
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date d = sdf.parse(s);
            if (d != null) return d.getTime();
        } catch (Exception ignored) {}
        try {
            java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            java.util.Date d2 = sdf2.parse(s);
            if (d2 != null) return d2.getTime();
        } catch (Exception ignored) {}
        return -1L;
    }

    private void applyTimeFilter() {
        List<LogInfo> src = baseLogs != null ? baseLogs : new java.util.ArrayList<>();
        List<LogInfo> out = new java.util.ArrayList<>();
        long startMs = parseMillis(selectedStartTime);
        long endMs = parseMillis(selectedEndTime);
        for (LogInfo li : src) {
            String ct = li != null ? li.getCreateTime() : null;
            long t = parseMillis(ct);
            boolean keep = true;
            if (startMs > 0) keep = keep && t >= startMs;
            if (endMs > 0) keep = keep && t <= endMs;
            if (keep) out.add(li);
        }
        if (logInfoAdapter != null) logInfoAdapter.submitList(out);
    }
}
