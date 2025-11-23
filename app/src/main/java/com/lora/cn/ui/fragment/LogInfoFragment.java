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
    private android.widget.Spinner spinnerLogType;
    private android.widget.Spinner spinnerPolice;
    private android.widget.TextView btnExport;
    List<LogInfo> logList = new ArrayList<>();
    private android.view.View rlStart;
    private android.view.View rlEnd;
    private android.widget.TextView tvStart;
    private android.widget.TextView tvEnd;
    private String selectedStartTime = "";
    private String selectedEndTime = "";
    private List<LogInfo> baseLogs = new ArrayList<>();
    @Override
    public void onStart() {
        super.onStart();
        if (!org.greenrobot.eventbus.EventBus.getDefault().isRegistered(this)) {
            org.greenrobot.eventbus.EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onStop() {
        if (org.greenrobot.eventbus.EventBus.getDefault().isRegistered(this)) {
            org.greenrobot.eventbus.EventBus.getDefault().unregister(this);
        }
        super.onStop();
    }

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    public void onTerminalRefreshEvent(com.lora.cn.event.TerminalRefreshEvent event) {
        try { initLogData(); } catch (Exception ignored) {}
    }

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
        spinnerLogType = view.findViewById(R.id.log_type);
        spinnerPolice = view.findViewById(R.id.spinner_police);
        btnExport = view.findViewById(R.id.btn_export_log_excel);
        if (btnExport != null) btnExport.setOnClickListener(v -> exportLogs());
        if (rlStart != null) rlStart.setOnClickListener(v -> showStartPicker());
        if (rlEnd != null) rlEnd.setOnClickListener(v -> showEndPicker());
        if (spinnerLogType != null) spinnerLogType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View v, int pos, long id) { applyTimeFilter(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        if (spinnerPolice != null) spinnerPolice.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View v, int pos, long id) { applyTimeFilter(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        if (rlStart != null) rlStart.setOnLongClickListener(v -> {
            selectedStartTime = "";
            if (tvStart != null) tvStart.setText("开始时间");
            applyTimeFilter();
            return true;
        });
        if (rlEnd != null) rlEnd.setOnLongClickListener(v -> {
            selectedEndTime = "";
            if (tvEnd != null) tvEnd.setText("结束时间");
            applyTimeFilter();
            return true;
        });
    }

    private void initLogData() {
        // 先清理示例日志，避免展示假数据
        databaseHelper.cleanSampleLogData();
        
        // 从数据库获取真实日志数据（仅展示已添加终端的日志）
        logList = databaseHelper.getAllLogsBoundToTerminals();
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
        logInfoAdapter.setOnHandleClickListener(item -> showHandleDialogForLog(item));
        recyclerView.setAdapter(logInfoAdapter);
        applyTimeFilter();
        try {
            android.app.Activity a = getActivity();
            if (a instanceof com.lora.cn.ui.activity.MainActivity) {
                ((com.lora.cn.ui.activity.MainActivity) a).updatePendingBadge();
            }
        } catch (Exception ignored) {}
        
        // 设置点击事件
        logInfoAdapter.setOnItemClickListener((adapter, view, position) -> {
            LogInfo log = logList.get(position);
            // 处理日志项点击事件
        });
    }

    private void showHandleDialogForLog(LogInfo item) {
        if (item == null) return;
        com.lora.cn.utils.DialogUtils.showRemarkDialog(requireContext(), "确认处理", "", new com.lora.cn.utils.DialogUtils.OnConfirmListener() {
            @Override
            public void onConfirm(String remark) {
                String user = com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", "");
                String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                try {
                    databaseHelper.updateLogHandled(item.getId(), user, time, remark);
                    int s = item.getStatusCode();
                    if (s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code || s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code) {
                        int mask = 0;
                        if (s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code) mask |= 0x00000001;
                        if (s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code) mask |= 0x00000002;
                        String devHex = item.getDeviceId() != null ? item.getDeviceId() : "";
                        try {
                            android.app.Activity a = getActivity();
                            if (a instanceof com.lora.cn.ui.activity.MainActivity) {
                                ((com.lora.cn.ui.activity.MainActivity) a).sendHandleDownlink(devHex, mask);
                            }
                        } catch (Exception ignored) {}
                    }
                    initLogData();
                    try {
                        android.app.Activity a = getActivity();
                        if (a instanceof com.lora.cn.ui.activity.MainActivity) {
                            ((com.lora.cn.ui.activity.MainActivity) a).handleAlertHandled(item.getDeviceId(), s);
                        }
                    } catch (Exception ignored) {}
                    try {
                        android.app.Activity a = getActivity();
                        if (a instanceof com.lora.cn.ui.activity.MainActivity) {
                            ((com.lora.cn.ui.activity.MainActivity) a).updatePendingBadge();
                        }
                    } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            }
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
        int typeSel = spinnerLogType != null ? spinnerLogType.getSelectedItemPosition() : 0;
        int policeSel = spinnerPolice != null ? spinnerPolice.getSelectedItemPosition() : 0;
        java.util.Map<String, LogInfo> latestHandleMap = new java.util.HashMap<>();
        java.util.Map<String, Long> lastNormalTime = new java.util.HashMap<>();
        java.util.Map<String, Long> lastHandledTime = new java.util.HashMap<>();
        for (LogInfo li : src) {
            String ct = li != null ? li.getCreateTime() : null;
            long t = parseMillis(ct);
            boolean keep = true;
            if (startMs > 0) keep = keep && t >= startMs;
            if (endMs > 0) keep = keep && t <= endMs;
            if (keep) {
                int sCode = li.getStatusCode();
                boolean isAlarm = sCode == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                        || sCode == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code
                        || sCode == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
                boolean isHandled = sCode == com.lora.cn.ui.constants.LogStatus.HANDLED.code;
                if (typeSel == 1 && !isAlarm) keep = false; // 报警日志
                else if (typeSel == 2 && (isAlarm || isHandled)) keep = false; // 普通日志（非报警且非处理）
                else if (typeSel == 3 && !isHandled) keep = false; // 处理日志
                if (policeSel == 1 && !isHandled) keep = false; // 已处理
                else if (policeSel == 2 && isHandled) keep = false; // 未处理
            }
            if (keep) out.add(li);
            int s = li.getStatusCode();
            boolean candidate = s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                    || s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code
                    || s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
            if (candidate) {
                String key = (li.getTerminalId() == null ? "" : li.getTerminalId()) + ":" + s;
                LogInfo prev = latestHandleMap.get(key);
                long prevT = prev != null ? parseMillis(prev.getCreateTime()) : -1L;
                if (prev == null || t >= prevT) latestHandleMap.put(key, li);
            } else {
                boolean normal = s == com.lora.cn.ui.constants.LogStatus.ONLINE.code
                        || s == com.lora.cn.ui.constants.LogStatus.LOCK_OPEN.code
                        || s == com.lora.cn.ui.constants.LogStatus.LOCK_CLOSE.code
                        || s == com.lora.cn.ui.constants.LogStatus.DEVICE_ON.code
                        || s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFF.code;
                if (normal) {
                    Long prev = lastNormalTime.get(li.getTerminalId());
                    if (prev == null || t >= prev) lastNormalTime.put(li.getTerminalId(), t);
                }
                if (s == com.lora.cn.ui.constants.LogStatus.HANDLED.code) {
                    Long prevH = lastHandledTime.get(li.getTerminalId());
                    if (prevH == null || t >= prevH) lastHandledTime.put(li.getTerminalId(), t);
                }
            }
        }
        java.util.Set<Long> allowedIds = new java.util.HashSet<>();
        for (LogInfo v : latestHandleMap.values()) {
            Long nt = lastNormalTime.get(v.getTerminalId());
            Long ht = lastHandledTime.get(v.getTerminalId());
            long at = parseMillis(v.getCreateTime());
            boolean afterNormal = nt == null || at >= nt;
            boolean afterHandled = ht == null || at > ht;
            if (afterNormal && afterHandled) allowedIds.add(v.getId());
        }
        logInfoAdapter.setAllowedHandleIds(allowedIds);
        java.util.Map<Long, String> handledLabels = new java.util.HashMap<>();
        try {
            com.lora.cn.database.DatabaseHelper db = com.lora.cn.database.DatabaseHelper.getInstance(requireContext());
            java.util.Map<String, java.util.List<LogInfo>> byDev = new java.util.HashMap<>();
            for (LogInfo li : out) {
                String dev = li.getTerminalId();
                java.util.List<LogInfo> lst = byDev.get(dev);
                if (lst == null) { lst = new java.util.ArrayList<>(); byDev.put(dev, lst); }
                lst.add(li);
            }
            for (java.util.List<LogInfo> lst : byDev.values()) {
                for (LogInfo li : lst) {
                    if (li.getStatusCode() == com.lora.cn.ui.constants.LogStatus.HANDLED.code) {
                        long ref = parseMillis(li.getHandleTime());
                        if (ref <= 0) ref = parseMillis(li.getCreateTime());
                        java.util.List<LogInfo> logs = db.getLogsByTerminalId(li.getTerminalId());
                        LogInfo origin = null;
                        for (LogInfo x : logs) {
                            int s = x.getStatusCode();
                            boolean candidate = s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                                    || s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code
                                    || s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
                            if (!candidate) continue;
                            long tt = parseMillis(x.getCreateTime());
                            if (tt > 0 && tt <= ref) { if (origin == null || tt >= parseMillis(origin.getCreateTime())) origin = x; }
                        }
                        if (origin != null) handledLabels.put(li.getId(), com.lora.cn.ui.constants.LogStatus.toText(origin.getStatusCode()));
                    }
                }
            }
        } catch (Exception ignored) {}
        logInfoAdapter.setHandledSourceLabels(handledLabels);
        if (logInfoAdapter != null) logInfoAdapter.submitList(out);
    }

    private void exportLogs() {
        try {
            java.util.List<com.lora.cn.ui.model.LogInfo> all = databaseHelper.getAllLogs();
            String NL = "\r\n";
            StringBuilder sb = new StringBuilder();
            sb.append("时间,状态,终端列表,终端ID,处理人,处理时间,操作").append(NL);
            if (all != null) {
                for (com.lora.cn.ui.model.LogInfo li : all) {
                    String time = safe(li.getCreateTime());
                    String status = com.lora.cn.ui.constants.LogStatus.toText(li.getStatusCode());
                    String name = safe(li.getTerminalName());
                    String id = safe(li.getTerminalId());
                    String user = safe(li.getHandleUser());
                    String htime = safe(li.getHandleTime());
                    String op = safe(li.getAction());
                    sb.append(escape(time)).append(',')
                      .append(escape(status)).append(',')
                      .append(escape(name)).append(',')
                      .append(escape(id)).append(',')
                      .append(escape(user)).append(',')
                      .append(escape(htime)).append(',')
                      .append(escape(op)).append(NL);
                }
            }
            java.io.File dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            java.io.File folder = new java.io.File(dir, "LoraAppLogs");
            if (!folder.exists()) folder.mkdirs();
            String name = "logs_export_" + System.currentTimeMillis() + ".csv";
            java.io.File file = new java.io.File(folder, name);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            fos.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
            fos.write(sb.toString().getBytes("UTF-8"));
            fos.flush(); fos.close();
            android.widget.Toast.makeText(requireContext(), "导出成功: " + file.getAbsolutePath(), android.widget.Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            android.widget.Toast.makeText(requireContext(), "导出失败: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String escape(String s) {
        if (s == null) return "";
        String t = s.replace("\"", "\"\"");
        if (t.contains(",") || t.contains("\n") || t.contains("\r")) return '"' + t + '"';
        return t;
    }
}
