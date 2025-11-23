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
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import com.lora.cn.R;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.ui.adapter.LogInfoAdapter;
import com.lora.cn.ui.model.LogInfo;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.User;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class LogInfoFragment extends Fragment {

    private RecyclerView recyclerView;
    private LogInfoAdapter logInfoAdapter;
    private DatabaseHelper databaseHelper;
    private DatabaseManager databaseManager;
    private int currentUserRoleId = -1;
    private SmartRefreshLayout refreshLayout;
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
    private int pageSize = 20;
    private int currentPage = 0;
    private java.util.List<LogInfo> filteredPageBase = new java.util.ArrayList<>();
    private int totalFilteredCount = 0;
    private java.util.List<LogInfo> displayedLogs = new java.util.ArrayList<>();
    private boolean isAutoLoading = false;
    private android.widget.Spinner spinnerTs;
    private android.widget.ImageView sxLeft;
    private android.widget.ImageView sxRight;
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
        refreshLayout = view.findViewById(R.id.refreshLayout);
        recyclerView = view.findViewById(R.id.terminal_log_recycle);
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(lm);
        databaseHelper = DatabaseHelper.getInstance(getContext());
        databaseManager = DatabaseManager.getInstance(requireContext());
        try {
            long uid = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
            if (uid != -1) {
                User u = databaseManager.getUserById(uid);
                if (u != null) currentUserRoleId = (int) u.getRoleId();
            }
        } catch (Exception ignored) {}
        rlStart = view.findViewById(R.id.time_start_time);
        rlEnd = view.findViewById(R.id.time_end_time);
        tvStart = view.findViewById(R.id.time_start_time_tv);
        tvEnd = view.findViewById(R.id.time_end_time_tv);
        spinnerLogType = view.findViewById(R.id.log_type);
        spinnerPolice = view.findViewById(R.id.spinner_police);
        btnExport = view.findViewById(R.id.btn_export_log_excel);
        android.widget.TextView btnReset = view.findViewById(R.id.btn_reset_filters);
        spinnerTs = view.findViewById(R.id.spinner_ts);
        sxLeft = view.findViewById(R.id.sx_left);
        sxRight = view.findViewById(R.id.sx_right);
        try {
            String[] typeOpts = getResources().getStringArray(R.array.log_type_options);
            android.widget.ArrayAdapter<String> typeAdapter = new android.widget.ArrayAdapter<>(requireContext(), R.layout.spinner_item_12dp, java.util.Arrays.asList(typeOpts));
            typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_12dp);
            if (spinnerLogType != null) spinnerLogType.setAdapter(typeAdapter);

            String[] policeOpts = getResources().getStringArray(R.array.log_police_options);
            android.widget.ArrayAdapter<String> policeAdapter = new android.widget.ArrayAdapter<>(requireContext(), R.layout.spinner_item_12dp, java.util.Arrays.asList(policeOpts));
            policeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_12dp);
            if (spinnerPolice != null) spinnerPolice.setAdapter(policeAdapter);

            String[] pageOpts = getResources().getStringArray(R.array.page_size_options);
            android.widget.ArrayAdapter<String> pageAdapter = new android.widget.ArrayAdapter<>(requireContext(), R.layout.spinner_item_12dp, java.util.Arrays.asList(pageOpts));
            pageAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_12dp);
            if (spinnerTs != null) spinnerTs.setAdapter(pageAdapter);
        } catch (Exception ignored) {}

        if (btnExport != null) {
            if (!hasPermission("log_export")) btnExport.setVisibility(android.view.View.GONE);
            else btnExport.setOnClickListener(v -> exportLogs());
        }
        if (btnReset != null) btnReset.setOnClickListener(v -> {
            try {
                selectedStartTime = "";
                selectedEndTime = "";
                if (tvStart != null) tvStart.setText("开始时间");
                if (tvEnd != null) tvEnd.setText("结束时间");
                if (spinnerLogType != null) spinnerLogType.setSelection(0, false);
                if (spinnerPolice != null) spinnerPolice.setSelection(0, false);
                currentPage = 0;
                applyTimeFilter();
                if (refreshLayout != null) refreshLayout.setNoMoreData(false);
            } catch (Exception ignored) {}
        });
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
        if (refreshLayout != null) {
            refreshLayout.setEnableRefresh(true);
            refreshLayout.setEnableLoadMore(true);
            refreshLayout.setOnRefreshListener(layout -> {
                try {
                    currentPage = 0;
                    String startStr = selectedStartTime;
                    String endStr = selectedEndTime;
                    int typeSel = spinnerLogType != null ? spinnerLogType.getSelectedItemPosition() : 0;
                    int policeSel = spinnerPolice != null ? spinnerPolice.getSelectedItemPosition() : 0;
                    totalFilteredCount = databaseHelper.queryLogsCount(startStr, endStr, typeSel, policeSel, false);
                    java.util.List<LogInfo> first = databaseHelper.queryLogsPaged(startStr, endStr, typeSel, policeSel, false, pageSize, currentPage);
                    displayedLogs.clear();
                    if (first != null) displayedLogs.addAll(first);
                    recalcAndSubmit(displayedLogs);
                    refreshLayout.finishRefresh(true);
                    boolean noMore = (currentPage + 1) * pageSize >= totalFilteredCount || (first == null || first.size() < pageSize);
                    refreshLayout.setNoMoreData(noMore);
                } catch (Exception e) {
                    refreshLayout.finishRefresh(false);
                }
            });
            refreshLayout.setOnLoadMoreListener(layout -> {
                try {
                    String startStr = selectedStartTime;
                    String endStr = selectedEndTime;
                    int typeSel = spinnerLogType != null ? spinnerLogType.getSelectedItemPosition() : 0;
                    int policeSel = spinnerPolice != null ? spinnerPolice.getSelectedItemPosition() : 0;
                    boolean canNext = (currentPage + 1) * pageSize < totalFilteredCount;
                    if (!canNext) {
                        refreshLayout.finishLoadMoreWithNoMoreData();
                        return;
                    }
                    currentPage++;
                    java.util.List<LogInfo> next = databaseHelper.queryLogsPaged(startStr, endStr, typeSel, policeSel, false, pageSize, currentPage);
                    if (next != null && !next.isEmpty()) {
                        displayedLogs.addAll(next);
                        recalcAndSubmit(displayedLogs);
                        refreshLayout.finishLoadMore(true);
                    } else {
                        refreshLayout.finishLoadMoreWithNoMoreData();
                    }
                } catch (Exception e) {
                    refreshLayout.finishLoadMore(false);
                }
            });
        }
        if (spinnerTs != null) {
            spinnerTs.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View v, int position, long id) {
                    Object item = parent.getItemAtPosition(position);
                    int newSize = pageSize;
                    if (item != null) {
                        String s = String.valueOf(item).replaceAll("[^0-9]", "");
                        if (!s.isEmpty()) {
                            try { newSize = Integer.parseInt(s); } catch (Exception ignored) {}
                        }
                    }
                    pageSize = newSize > 0 ? newSize : 20;
                    currentPage = 0;
                    applyTimeFilter();
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }
        if (sxLeft != null) {
            sxLeft.setOnClickListener(v -> {
                if (currentPage > 0) {
                    currentPage--;
                    submitCurrentPage();
                    updatePaginationControls();
                }
            });
        }
        if (sxRight != null) {
            sxRight.setOnClickListener(v -> {
                if ((currentPage + 1) * pageSize < totalFilteredCount) {
                    currentPage++;
                    submitCurrentPage();
                    updatePaginationControls();
                }
            });
        }
        try {
            if (spinnerLogType != null) spinnerLogType.setSelection(0, false);
            if (spinnerPolice != null) spinnerPolice.setSelection(0, false);
            selectedStartTime = "";
            selectedEndTime = "";
        } catch (Exception ignored) {}
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
        new Thread(() -> {
            java.util.List<LogInfo> all = new java.util.ArrayList<>();
            try {
                java.util.List<LogInfo> a = databaseHelper.getAllLogs();
                java.util.List<LogInfo> b = databaseHelper.getAllUnboundLogs();
                if (a != null) all.addAll(a);
                if (b != null) all.addAll(b);
            } catch (Exception ignored) {}
            java.util.Collections.sort(all, (o1, o2) -> {
                long t1 = parseMillis(o1 != null ? o1.getCreateTime() : null);
                long t2 = parseMillis(o2 != null ? o2.getCreateTime() : null);
                return Long.compare(t2, t1);
            });
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                logList = all;
                baseLogs.clear();
                if (logList != null) baseLogs.addAll(logList);
                if (logInfoAdapter == null) {
                    logInfoAdapter = new LogInfoAdapter();
                    logInfoAdapter.setOnHandleClickListener(item -> showHandleDialogForLog(item));
                    recyclerView.setAdapter(logInfoAdapter);
                    logInfoAdapter.setOnItemClickListener((adapter, view, position) -> {
                        LogInfo log = logList.get(position);
                    });
                }
                applyTimeFilter();
                try {
                    android.app.Activity a = getActivity();
                    if (a instanceof com.lora.cn.ui.activity.MainActivity) {
                        ((com.lora.cn.ui.activity.MainActivity) a).updatePendingBadge();
                    }
                } catch (Exception ignored) {}
            });
        }).start();
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
        if (logInfoAdapter == null) return;
        if (!isAdded() || getActivity() == null) return;
        List<LogInfo> out = new java.util.ArrayList<>();
        long startMs = parseMillis(selectedStartTime);
        long endMs = parseMillis(selectedEndTime);
        int typeSel = spinnerLogType != null ? spinnerLogType.getSelectedItemPosition() : 0;
        int policeSel = spinnerPolice != null ? spinnerPolice.getSelectedItemPosition() : 0;
        String startStr = selectedStartTime;
        String endStr = selectedEndTime;
        currentPage = 0;
        totalFilteredCount = databaseHelper.queryLogsCount(startStr, endStr, typeSel, policeSel, false);
        out = databaseHelper.queryLogsPaged(startStr, endStr, typeSel, policeSel, false, pageSize, currentPage);
        displayedLogs.clear();
        if (out != null) displayedLogs.addAll(out);
        recalcAndSubmit(displayedLogs);
        if (refreshLayout != null) {
            boolean noMore = (currentPage + 1) * pageSize >= totalFilteredCount || (out == null || out.size() < pageSize);
            refreshLayout.setNoMoreData(noMore);
        }
    }

    private void submitCurrentPage() {
        try {
            if (logInfoAdapter != null) logInfoAdapter.submitList(filteredPageBase);
        } catch (Exception ignored) {}
    }

    private void updatePaginationControls() {
        try {
            boolean canLeft = currentPage > 0;
            boolean canRight = (currentPage + 1) * pageSize < totalFilteredCount;
            if (sxLeft != null) sxLeft.setEnabled(canLeft);
            if (sxRight != null) sxRight.setEnabled(canRight);
        } catch (Exception ignored) {}
    }

    private void recalcAndSubmit(java.util.List<LogInfo> list) {
        java.util.Map<String, Long> lastHandledTime = new java.util.HashMap<>();
        java.util.Map<String, LogInfo> latestByDeviceStatus = new java.util.HashMap<>();
        for (LogInfo li : list) {
            String ct = li != null ? li.getCreateTime() : null;
            long t = parseMillis(ct);
            int s = li.getStatusCode();
            if (s == com.lora.cn.ui.constants.LogStatus.HANDLED.code) {
                Long prevH = lastHandledTime.get(li.getTerminalId());
                if (prevH == null || t >= prevH) lastHandledTime.put(li.getTerminalId(), t);
                continue;
            }
            boolean candidate = s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                    || s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code
                    || s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
            if (candidate) {
                String key = (li.getTerminalId() == null ? "" : li.getTerminalId()) + ":" + s;
                LogInfo prev = latestByDeviceStatus.get(key);
                long prevT = prev != null ? parseMillis(prev.getCreateTime()) : -1L;
                if (prev == null || t >= prevT) latestByDeviceStatus.put(key, li);
            }
        }
        java.util.Set<Long> allowedIds = new java.util.HashSet<>();
        for (LogInfo v : latestByDeviceStatus.values()) {
            Long ht = lastHandledTime.get(v.getTerminalId());
            long at = parseMillis(v.getCreateTime());
            boolean afterHandled = ht == null || at > ht;
            if (afterHandled) allowedIds.add(v.getId());
        }
        if (!hasPermission("log_confirm")) allowedIds.clear();
        if (logInfoAdapter != null) logInfoAdapter.setAllowedHandleIds(allowedIds);
        java.util.Map<Long, String> handledLabels = new java.util.HashMap<>();
        try {
            com.lora.cn.database.DatabaseHelper db = com.lora.cn.database.DatabaseHelper.getInstance(requireContext());
            java.util.Map<String, java.util.List<LogInfo>> byDev = new java.util.HashMap<>();
            for (LogInfo li : list) {
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
        if (logInfoAdapter != null) logInfoAdapter.setHandledSourceLabels(handledLabels);
        filteredPageBase = new java.util.ArrayList<>(list);
        submitCurrentPage();
    }

    private boolean hasPermission(String code) {
        if (currentUserRoleId == -1) return false;
        try { return databaseManager.hasPermission(currentUserRoleId, code); } catch (Exception e) { return false; }
    }

    private void exportLogs() {
        try {
            java.util.List<com.lora.cn.ui.model.LogInfo> all = databaseHelper.getAllLogs();
            StringBuilder sb = new StringBuilder();
            sb.append("<html><head><meta charset=\"UTF-8\"></head><body>");
            sb.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"4\">");
            sb.append("<tr>")
              .append("<th>时间</th><th>状态</th><th>终端列表</th><th>终端ID</th><th>处理人</th><th>处理时间</th><th>操作</th>")
              .append("</tr>");
            if (all != null) {
                for (com.lora.cn.ui.model.LogInfo li : all) {
                    String time = safe(li.getCreateTime());
                    String status = com.lora.cn.ui.constants.LogStatus.toText(li.getStatusCode());
                    String name = safe(li.getTerminalName());
                    String id = safe(li.getTerminalId());
                    String user = safe(li.getHandleUser());
                    String htime = safe(li.getHandleTime());
                    String op = safe(li.getAction());
                    sb.append("<tr>")
                      .append("<td>").append(escape(time)).append("</td>")
                      .append("<td>").append(escape(status)).append("</td>")
                      .append("<td>").append(escape(name)).append("</td>")
                      .append("<td>").append(escape(id)).append("</td>")
                      .append("<td>").append(escape(user)).append("</td>")
                      .append("<td>").append(escape(htime)).append("</td>")
                      .append("<td>").append(escape(op)).append("</td>")
                      .append("</tr>");
                }
            }
            sb.append("</table></body></html>");
            java.io.File dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            java.io.File folder = new java.io.File(dir, "LoraAppLogs");
            if (!folder.exists()) folder.mkdirs();
            String name = "logs_export_" + System.currentTimeMillis() + ".xls";
            java.io.File file = new java.io.File(folder, name);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
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
