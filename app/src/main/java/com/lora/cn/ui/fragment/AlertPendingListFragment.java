package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.ui.adapter.LogInfoAlertAdapter;
import com.lora.cn.ui.model.LogInfo;
import com.lora.cn.utils.DialogUtils;
import com.lora.cn.network.MqttPacketsClient;
import com.lora.cn.utils.DownlinkMessageHelper;
import java.util.List;

/**
 * 报警待处理列表（简单用日志筛选实现）
 */
public class AlertPendingListFragment extends Fragment {

    private RecyclerView rv;
    private LogInfoAlertAdapter adapter;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_alert_pending_list, container, false);
        rv = v.findViewById(R.id.rv_alerts);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LogInfoAlertAdapter();
        adapter.setOnHandleClickListener(this::showHandleDialogForLog);
        rv.setAdapter(adapter);
        View btnBack = v.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(view -> {
                try {
                    androidx.appcompat.app.AppCompatActivity a = (androidx.appcompat.app.AppCompatActivity) getActivity();
                    if (a != null) {
                        a.getSupportFragmentManager().popBackStack();
                        android.view.View containerView = a.findViewById(R.id.fragment_device_list_container);
                        if (containerView != null) containerView.setVisibility(View.GONE);
                        android.view.View rvTabs = a.findViewById(R.id.rv_menu_tabs);
                        if (rvTabs != null) rvTabs.setVisibility(View.VISIBLE);
                        android.view.View vp = a.findViewById(R.id.view_pager);
                        if (vp != null) vp.setVisibility(View.VISIBLE);
                    }
                } catch (Exception ignored) {}
            });
        }
        loadAlerts();
        return v;
    }

    private void loadAlerts() {
        try {
            DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
            try { db.syncLowBatteryFlags(); } catch (Exception ignored) {}
            List<LogInfo> all = db.getAllLogsBoundToTerminals();
            java.util.Map<String, LogInfo> latest = new java.util.HashMap<>();
            for (LogInfo li : all) {
                int s = li.getStatusCode();
                if (s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                        || s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code
                        || s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code) {
                    String key = (li.getTerminalId() == null ? "" : li.getTerminalId()) + ":" + s;
                    LogInfo prev = latest.get(key);
                    long prevT = prev != null ? parseMillis(prev.getCreateTime()) : -1L;
                    long curT = parseMillis(li.getCreateTime());
                    if (prev == null || curT >= prevT) latest.put(key, li);
                }
            }
            java.util.Map<String, Long> lastNormalTime = new java.util.HashMap<>();
            java.util.Map<String, Long> lastHandledTime = new java.util.HashMap<>();
            for (LogInfo li : all) {
                int s = li.getStatusCode();
                boolean normal = s == com.lora.cn.ui.constants.LogStatus.ONLINE.code
                        || s == com.lora.cn.ui.constants.LogStatus.LOCK_OPEN.code
                        || s == com.lora.cn.ui.constants.LogStatus.LOCK_CLOSE.code
                        || s == com.lora.cn.ui.constants.LogStatus.DEVICE_ON.code
                        || s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFF.code;
                if (normal) {
                    long t = parseMillis(li.getCreateTime());
                    String key = li.getTerminalId();
                    Long prev = lastNormalTime.get(key);
                    if (prev == null || t >= prev) lastNormalTime.put(key, t);
                }
                String hu = li.getHandleUser();
                String htStr = li.getHandleTime();
                if ((hu != null && !hu.trim().isEmpty()) || (htStr != null && !htStr.trim().isEmpty())) {
                    long t = parseMillis(li.getCreateTime());
                    String key = li.getTerminalId();
                    Long prev = lastHandledTime.get(key);
                    if (prev == null || t >= prev) lastHandledTime.put(key, t);
                }
            }
            java.util.List<LogInfo> pending = new java.util.ArrayList<>(latest.values());
            java.util.Map<String, com.lora.cn.ui.model.Terminal> terminalById = new java.util.HashMap<>();
            for (com.lora.cn.ui.model.Terminal t : db.getAllTerminals()) {
                terminalById.put(t.getTerminalId(), t);
            }
            int lowTh = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
            java.util.List<LogInfo> filtered = new java.util.ArrayList<>();
            for (LogInfo li : pending) {
                String huLi = li.getHandleUser();
                String htLi = li.getHandleTime();
                boolean unhandledLi = (huLi == null || huLi.trim().isEmpty()) && (htLi == null || htLi.trim().isEmpty());
                if (!unhandledLi) continue;
                Long ht = lastHandledTime.get(li.getTerminalId());
                long at = parseMillis(li.getCreateTime());
                if (li.getStatusCode() == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code) {
                    com.lora.cn.ui.model.Terminal t = terminalById.get(li.getTerminalId());
                    if (t != null) {
                        boolean devStillOffline = t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE;
                        boolean isLowNow = t.getBatteryLevel() <= lowTh;
                        boolean afterHandled = ht == null || at > ht;
                        if (!devStillOffline && isLowNow && afterHandled) filtered.add(li);
                    }
                } else {
                    com.lora.cn.ui.model.Terminal t = terminalById.get(li.getTerminalId());
                    boolean isOfflineCase = li.getStatusCode() == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
                    boolean devStillOffline = t != null && t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE;
                    boolean devStillAbnormal = t != null && t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ABNORMAL_TAKEN;
                    boolean afterHandled = ht == null || at > ht;
                    if (isOfflineCase) {
                        if (devStillOffline && afterHandled) filtered.add(li);
                    } else if (li.getStatusCode() == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code) {
                        if (!devStillOffline) {
                            if (devStillAbnormal) filtered.add(li);
                            else if (afterHandled) filtered.add(li);
                        }
                    } else {
                        if (afterHandled) filtered.add(li);
                    }
                }
            }
            adapter.submitList(filtered);
            try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.AlertPendingCountEvent(filtered.size())); } catch (Exception ignored) {}
            java.util.Set<Long> allowedIds = new java.util.HashSet<>();
            for (LogInfo li : filtered) {
                int s = li.getStatusCode();
                boolean candidate = s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                        || s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code
                        || s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
                if (!candidate) continue;
                Long ht2 = lastHandledTime.get(li.getTerminalId());
                long at2 = parseMillis(li.getCreateTime());
                com.lora.cn.ui.model.Terminal t = terminalById.get(li.getTerminalId());
                boolean isOfflineCase2 = s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
                boolean devStillOffline2 = t != null && t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE;
                boolean devStillAbnormal2 = t != null && t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ABNORMAL_TAKEN;
                boolean canHandle = (ht2 == null || at2 > ht2) && (!isOfflineCase2 || devStillOffline2);
                if (s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code) {
                    canHandle = (!devStillOffline2) && (devStillAbnormal2 || canHandle);
                } else if (s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code) {
                    canHandle = (!devStillOffline2) && canHandle;
                }
                if (canHandle) allowedIds.add(li.getId());
            }
            adapter.setAllowedHandleIds(allowedIds);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "加载报警列表失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showHandleDialogForLog(LogInfo item) {
        if (item == null) return;
        DialogUtils.showRemarkDialog(requireContext(), "确认处理", "已处理", new com.lora.cn.utils.DialogUtils.OnConfirmListener() {
            @Override
            public void onConfirm(String remark) {
                String user = com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", "");
                String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                try {
                    DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
                    db.updateLogHandled(item.getId(), user, time, remark);
                    try {
                        java.util.List<LogInfo> devLogs = db.getLogsByTerminalId(item.getDeviceId());
                        if (devLogs != null) {
                            for (LogInfo li : devLogs) {
                                if (li == null) continue;
                                boolean unhandled = (li.getHandleUser() == null || li.getHandleUser().trim().isEmpty())
                                        && (li.getHandleTime() == null || li.getHandleTime().trim().isEmpty());
                                if (unhandled && li.getStatusCode() == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code) {
                                    db.updateLogHandled(li.getId(), user, time, remark);
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    int s = item.getStatusCode();
                    if (s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code) {
                        int mask = 0;
                        if (s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code) mask |= 0x00000001;
                        String devHex = item.getDeviceId() != null ? item.getDeviceId() : "";
                        try {
                            android.app.Activity a = getActivity();
                            MqttPacketsClient client = null;
                            if (a instanceof com.lora.cn.ui.activity.MainActivity) {
                                client = ((com.lora.cn.ui.activity.MainActivity) a).getMqttClient();
                            }
                            if (client == null) client = new MqttPacketsClient();
                            DownlinkMessageHelper helper = new DownlinkMessageHelper(client);
                            int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
                            int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
                            int mins = Math.max(0, Math.min(1440, h * 60 + m));
                            int interval = Math.max(3, Math.min(1440, com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3)));
//                            helper.sendDownlink8001(
//                                    devHex,
//                                    1,
//                                    1,
//                                    0,
//                                    0,
//                                    0,
//                                    mask,
//                                    interval,
//                                    1,
//                                    new int[]{mins},
//                                    true
//                            );
                        } catch (Exception ignored) {}
                    }
                    loadAlerts();
                    try {
                        androidx.appcompat.app.AppCompatActivity a = (androidx.appcompat.app.AppCompatActivity) getActivity();
                        if (a instanceof com.lora.cn.ui.activity.MainActivity) {
                            ((com.lora.cn.ui.activity.MainActivity) a).updatePendingBadge();
                        }
                    } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            }
        });
    }

    private long parseMillis(String time) {
        if (time == null || time.length() == 0) return -1L;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date d = sdf.parse(time);
            return d != null ? d.getTime() : -1L;
        } catch (Exception e) {
            return -1L;
        }
    }
}
