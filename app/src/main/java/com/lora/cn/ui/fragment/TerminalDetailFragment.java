package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.ui.adapter.LogDetailInfoAdapter;
import com.lora.cn.ui.adapter.LogInfoAdapter;
import com.lora.cn.ui.model.LogInfo;
import com.lora.cn.ui.view.BatteryView;
import com.lora.cn.ui.view.SignalStrengthView;
import com.lora.cn.utils.LoRaFrameParser;
import com.lora.cn.utils.DialogUtils;
import com.lora.cn.events.UplinkDataEvent;
import com.lora.cn.network.MqttPacketsClient;
import com.lora.cn.utils.DownlinkMessageHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TerminalDetailFragment extends Fragment {

    private static final String ARG_DEVICE_ID = "arg_device_id";

    public static TerminalDetailFragment newInstance(String terminalId) {
        TerminalDetailFragment f = new TerminalDetailFragment();
        Bundle b = new Bundle();
        b.putString(ARG_DEVICE_ID, terminalId);
        f.setArguments(b);
        return f;
    }

    private DatabaseHelper dbHelper;

    private TextView tvTitle;
    private TextView btnBack;
    private TextView btnEdit;
    private TextView btnDelete;
    private ImageView ivFavorite;
    private TextView tvDeviceId;
    private TextView tvDepartment;
    private TextView tvLocation;
    private TextView tvStatus;
    private TextView tvBattery;
    private RecyclerView rvLogs;
    private TextView tvNoLogs;
    private LogDetailInfoAdapter logAdapter;

    private TextView terminal_detail_type;
    private TextView terminal_detail_code;
    private SignalStrengthView signalView;
    private BatteryView batteryView;
    private TextView terminal_detail_wifi;
    private TextView terminal_detail_battery;
    private TextView terminal_detail_id;
    private Button btnHandleNow;
    private TextView btnSetMaintenance;
    private boolean waitingForUplink = false;
    private ExecutorService ioExecutor;
    private Handler mainHandler;
    private final AtomicInteger bindSeq = new AtomicInteger(0);
    private final AtomicInteger logsSeq = new AtomicInteger(0);
    private final AtomicInteger maintenanceSeq = new AtomicInteger(0);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_terminal_detail, container, false);
        dbHelper = DatabaseHelper.getInstance(requireContext());
        if (ioExecutor == null) ioExecutor = Executors.newSingleThreadExecutor();
        if (mainHandler == null) mainHandler = new Handler(Looper.getMainLooper());
        initViews(v);
        setupListeners();
        bindData();
        loadLogs();
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (ioExecutor != null) ioExecutor.shutdownNow();
        } catch (Exception ignored) {}
        ioExecutor = null;
        mainHandler = null;
    }

    private void initViews(View v) {
        tvTitle = v.findViewById(R.id.tv_title);
        btnBack = v.findViewById(R.id.btn_back);
        btnEdit = v.findViewById(R.id.btn_edit);
        btnDelete = v.findViewById(R.id.btn_delete);
        ivFavorite = v.findViewById(R.id.iv_favorite);
        tvDeviceId = v.findViewById(R.id.tv_device_id);
        tvDepartment = v.findViewById(R.id.tv_department);
        tvLocation = v.findViewById(R.id.tv_location);
        tvStatus = v.findViewById(R.id.tv_status);
        tvBattery = v.findViewById(R.id.tv_battery);
        rvLogs = v.findViewById(R.id.rv_logs);
        tvNoLogs = v.findViewById(R.id.tv_no_logs);
        rvLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        logAdapter = new LogDetailInfoAdapter();
        rvLogs.setAdapter(logAdapter);

        terminal_detail_type = v.findViewById(R.id.terminal_detail_type);
        terminal_detail_code = v.findViewById(R.id.terminal_detail_code);
        signalView = v.findViewById(R.id.signalView);
        batteryView = v.findViewById(R.id.batteryView);
        terminal_detail_wifi = v.findViewById(R.id.terminal_detail_wifi);
        terminal_detail_battery = v.findViewById(R.id.terminal_detail_battery);
        terminal_detail_id = v.findViewById(R.id.terminal_detail_id);
        btnHandleNow = v.findViewById(R.id.btn_handle_now);
        btnSetMaintenance = v.findViewById(R.id.btn_set_maintenance);

    }

    private void bindData() {
        if (ioExecutor == null || mainHandler == null) return;
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        android.content.Context appCtx = ctx.getApplicationContext();
        Bundle b = getArguments();
        if (b == null) return;
        String deviceId = b.getString(ARG_DEVICE_ID, "-");
        int token = bindSeq.incrementAndGet();
        ioExecutor.execute(() -> {
            com.lora.cn.ui.model.Terminal t = null;
            try {
                com.lora.cn.database.dao.TerminalDao dao = new com.lora.cn.database.dao.TerminalDao(dbHelper);
                t = dao.getTerminalByDeviceId(deviceId);
            } catch (Exception ignored) {}

            String title = "-";
            String showDeviceId = deviceId;
            String depText = "-";
            String locText = "-";
            String statusPercent = "-";
            String topBattery = "-";
            String groupNames = "-";
            String code = "-";
            String wifiText = "-";
            String batteryText = "";
            int batteryLevel = 0;
            boolean showBatteryAndSignal = false;
            int signalBars = 0;
            boolean isFavorite = false;
            int maintenanceCount = 0;

            int st = com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE;
            int rssi = 0;
            if (t != null) {
                long nowMs = System.currentTimeMillis();
                long timeoutMs = 10 * 60 * 1000L;
                if (t.getUpdateTime() <= 0 || nowMs - t.getUpdateTime() > timeoutMs) {
                    t.setStatus(com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE);
                }

                title = !TextUtils.isEmpty(t.getTerminalName()) ? t.getTerminalName() : "-";
                showDeviceId = !TextUtils.isEmpty(t.getTerminalId()) ? t.getTerminalId() : deviceId;
                depText = t.getDepartment();
                locText = t.getLocation();
                if (TextUtils.isEmpty(depText) || TextUtils.isEmpty(locText)) {
                    try {
                        com.lora.cn.database.DatabaseManager dm2 = com.lora.cn.database.DatabaseManager.getInstance(appCtx);
                        if (TextUtils.isEmpty(depText) && t.getDepartmentId() > 0) {
                            com.lora.cn.database.entity.Category cd = dm2.getCategoryById(t.getDepartmentId());
                            if (cd != null) depText = cd.getCategoryName();
                        }
                        if (TextUtils.isEmpty(locText) && t.getRoomId() > 0) {
                            com.lora.cn.database.entity.Category cr = dm2.getCategoryById(t.getRoomId());
                            if (cr != null) locText = cr.getCategoryName();
                        }
                    } catch (Exception ignored) {}
                }
                if (TextUtils.isEmpty(depText)) depText = "-";
                if (TextUtils.isEmpty(locText)) locText = "-";

                rssi = t.getRssi();
                int rssiRawTop = Math.max(0, Math.min(138, 138 - rssi));
                float percentTop = (138 - rssiRawTop) * 100f / 138f;
                statusPercent = String.format("%.0f%%", percentTop);

                st = t.getStatus();
                batteryLevel = t.getBatteryLevel();
                if (st == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE) {
                    topBattery = "";
                } else {
                    topBattery = batteryLevel + "%";
                }

                String names = t.getGroupNamesText();
                if (!TextUtils.isEmpty(names)) groupNames = names;

                code = t.getDeviceCode();
                if (TextUtils.isEmpty(code)) code = "-";

                wifiText = com.lora.cn.ui.constants.TerminalStatusConstants.codeToText(st);
                if (st == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ONLINE
                        || st == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_NORMAL_TAKEN
                        || st == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ABNORMAL_TAKEN) {
                    showBatteryAndSignal = true;
                    int lowTh = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
                    batteryText = batteryLevel + "%";
                    boolean ignoredIsLow = batteryLevel <= lowTh;
                }

                if (st != com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE) {
                    int rssiRaw = Math.max(0, Math.min(138, 138 - rssi));
                    if (rssiRaw <= 65) signalBars = 4;
                    else if (rssiRaw <= 75) signalBars = 3;
                    else if (rssiRaw <= 85) signalBars = 2;
                    else if (rssiRaw <= 95) signalBars = 1;
                    else signalBars = 0;
                }

                isFavorite = t.isFavorite();
            }

            try {
                long uid = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
                maintenanceCount = dbHelper.getMaintenanceCountByTerminal(deviceId, uid);
            } catch (Exception ignored) {}

            String finalTitle = title;
            String finalShowDeviceId = showDeviceId;
            String finalDepText = depText;
            String finalLocText = locText;
            String finalStatusPercent = statusPercent;
            String finalTopBattery = topBattery;
            String finalGroupNames = groupNames;
            String finalCode = code;
            String finalWifiText = wifiText;
            String finalBatteryText = batteryText;
            int finalBatteryLevel = batteryLevel;
            boolean finalShowBatteryAndSignal = showBatteryAndSignal;
            int finalSignalBars = signalBars;
            boolean finalIsFavorite = isFavorite;
            int finalMaintenanceCount = maintenanceCount;
            int finalSt = st;

            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (token != bindSeq.get()) return;
                if (tvTitle != null) tvTitle.setText(finalTitle);
                if (tvDeviceId != null) tvDeviceId.setText(finalShowDeviceId);
                if (tvDepartment != null) tvDepartment.setText(finalDepText);
                if (tvLocation != null) tvLocation.setText(finalLocText);
                if (tvStatus != null) tvStatus.setText(finalStatusPercent);
                if (tvBattery != null) tvBattery.setText(finalTopBattery);
                if (terminal_detail_type != null) {
                    terminal_detail_type.setSingleLine(false);
                    terminal_detail_type.setEllipsize(null);
                    terminal_detail_type.setMaxLines(20);
                    terminal_detail_type.setText(finalGroupNames);
                }
                if (terminal_detail_code != null) terminal_detail_code.setText(finalCode);
                if (terminal_detail_wifi != null) terminal_detail_wifi.setText(finalWifiText);
                if (terminal_detail_battery != null) {
                    terminal_detail_battery.setText(finalBatteryText);
                    if (finalShowBatteryAndSignal) {
                        int lowTh = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
                        boolean isLow = finalBatteryLevel <= lowTh;
                        terminal_detail_battery.setTextColor(isLow ? android.graphics.Color.parseColor("#FF9500") : android.graphics.Color.parseColor("#333333"));
                    }
                }
                if (batteryView != null) batteryView.setVisibility(finalShowBatteryAndSignal ? View.VISIBLE : View.GONE);
                if (batteryView != null && finalShowBatteryAndSignal) batteryView.setBatteryLevel(finalBatteryLevel);
                if (signalView != null) {
                    if (finalSt == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE) {
                        signalView.setVisibility(View.GONE);
                    } else {
                        signalView.setVisibility(View.VISIBLE);
                        signalView.setSignalStrength(finalSignalBars);
                    }
                }
                if (terminal_detail_id != null) terminal_detail_id.setText(finalShowDeviceId);
                if (ivFavorite != null) {
                    ivFavorite.setImageResource(finalIsFavorite ? R.mipmap.ic_star_yeollw : R.mipmap.ic_start);
                    ivFavorite.setTag(finalIsFavorite);
                }
                if (btnSetMaintenance != null) {
                    btnSetMaintenance.setText("设置维护(" + finalMaintenanceCount + ")");
                }
            });
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            android.app.Activity a = getActivity();
            if (a instanceof com.lora.cn.ui.activity.MainActivity) {
                ((com.lora.cn.ui.activity.MainActivity) a).hideDeviceList();
                return;
            }
            if (getParentFragmentManager().getBackStackEntryCount() > 0) getParentFragmentManager().popBackStack();
        });
        btnEdit.setOnClickListener(v -> {
            String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
            if (ioExecutor == null || mainHandler == null) return;
            ioExecutor.execute(() -> {
                com.lora.cn.ui.model.Terminal t = null;
                try {
                    com.lora.cn.database.dao.TerminalDao dao = new com.lora.cn.database.dao.TerminalDao(dbHelper);
                    t = dao.getTerminalByDeviceId(deviceId);
                } catch (Exception ignored) {}
                com.lora.cn.ui.model.Terminal finalT = t;
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    if (finalT == null) {
                        Toast.makeText(requireContext(), "未找到终端", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AddDeviceFragment fragment = AddDeviceFragment.newInstance(finalT, "edit");
                    androidx.appcompat.app.AppCompatActivity a = (androidx.appcompat.app.AppCompatActivity) getActivity();
                    if (a != null) {
                        a.getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_device_list_container, fragment)
                                .addToBackStack("edit_device")
                                .commit();
                    }
                });
            });
        });
        btnDelete.setOnClickListener(v -> {
            final String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
            DialogUtils.showConfirmDialog(requireContext(), "删除确认", "确定删除该终端吗？", new DialogUtils.OnConfirmDialogListener() {
                @Override
                public void onConfirm() {
                    if (ioExecutor == null || mainHandler == null) return;
                    ioExecutor.execute(() -> {
                        boolean ok = deleteTerminal(deviceId);
                        mainHandler.post(() -> {
                            if (!isAdded()) return;
                            if (ok) {
                                Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                                if (getActivity() != null) {
                                    android.view.View rvTabs = getActivity().findViewById(R.id.rv_menu_tabs);
                                    android.view.View vp = getActivity().findViewById(R.id.view_pager);
                                    android.view.View container = getActivity().findViewById(R.id.fragment_device_list_container);
                                    if (rvTabs != null) rvTabs.setVisibility(android.view.View.VISIBLE);
                                    if (vp != null) vp.setVisibility(android.view.View.VISIBLE);
                                    if (container != null) container.setVisibility(android.view.View.GONE);
                                }
                                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                                    getParentFragmentManager().popBackStack();
                                }
                            } else {
                                Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                }
                @Override
                public void onCancel() {}
            });
        });

        // 收藏点击切换
        ivFavorite.setClickable(true);
        ivFavorite.setOnClickListener(v -> {
            String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
            Object tag = ivFavorite.getTag();
            boolean current = tag instanceof Boolean ? (Boolean) tag : false;
            boolean target = !current;
            if (ioExecutor == null || mainHandler == null) return;
            ioExecutor.execute(() -> {
                try {
                    long uid = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
                    if (uid > 0) {
                        dbHelper.setFavoriteForUser(uid, deviceId, target);
                    } else {
                        dbHelper.updateTerminalFavoriteStatus(deviceId, target);
                    }
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        ivFavorite.setImageResource(target ? R.mipmap.ic_star_yeollw : R.mipmap.ic_start);
                        if (target) {
                            ivFavorite.setColorFilter(android.graphics.Color.parseColor("#FFD700"));
                        } else {
                            ivFavorite.clearColorFilter();
                        }
                        ivFavorite.setTag(target);
                        org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("更新收藏: " + deviceId));
                        ivFavorite.invalidate();
                    });
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "收藏操作异常: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        if (btnSetMaintenance != null) {
            btnSetMaintenance.setOnClickListener(v -> {
                String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
                MaintenanceSettingListFragment fragment = MaintenanceSettingListFragment.newInstance(deviceId);
                androidx.appcompat.app.AppCompatActivity a = (androidx.appcompat.app.AppCompatActivity) getActivity();
                if (a != null) {
                    a.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_device_list_container, fragment)
                            .addToBackStack("maintenance_list")
                            .commit();
                }
            });
        }

//        // 立即处理按钮：下发查询并等待上行回复
//        if (btnHandleNow != null) {
//            btnHandleNow.setOnClickListener(v -> {
//                String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
//                waitingForUplink = true;
//                btnHandleNow.setText("处理完成");
//                Toast.makeText(requireContext(), "已下发查询，等待设备上行回复", Toast.LENGTH_SHORT).show();
//                try {
//                    MqttPacketsClient mqttClient = new MqttPacketsClient();
//                    DownlinkMessageHelper helper = new DownlinkMessageHelper(mqttClient);
//                    helper.sendQueryStatusDownlink(deviceId);
//                } catch (Exception e) {
//                    // 下发失败不影响等待流程，仅提示
//                    Toast.makeText(requireContext(), "下发失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
    }

    private void updateMaintenanceCount(String deviceId) {
        if (ioExecutor == null || mainHandler == null) return;
        if (btnSetMaintenance == null) return;
        int token = maintenanceSeq.incrementAndGet();
        ioExecutor.execute(() -> {
            long uid = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
            int count = 0;
            try {
                count = dbHelper.getMaintenanceCountByTerminal(deviceId, uid);
            } catch (Exception ignored) {}
            int finalCount = count;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (token != maintenanceSeq.get()) return;
                btnSetMaintenance.setText("设置维护(" + finalCount + ")");
            });
        });
    }

    private void loadLogs() {
        if (ioExecutor == null || mainHandler == null) return;
        String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
        int token = logsSeq.incrementAndGet();
        ioExecutor.execute(() -> {
            List<LogInfo> logs;
            try {
                logs = dbHelper.getLogsByTerminalId(deviceId);
            } catch (Exception e) {
                logs = null;
            }

            java.util.Set<Long> allowedIds = new java.util.HashSet<>();
            java.util.Map<Long, String> handledLabels = new java.util.HashMap<>();
            String err = null;

            if (logs != null && !logs.isEmpty()) {
                try {
                    java.util.Map<String, Long> lastHandledTime = new java.util.HashMap<>();
                    java.util.Map<String, LogInfo> latestByDeviceStatus = new java.util.HashMap<>();
                    for (LogInfo li : logs) {
                        long t = parseMillis(li.getCreateTime());
                        int s = li.getStatusCode();
                        if (s == com.lora.cn.ui.constants.LogStatus.HANDLED.code) {
                            Long prev = lastHandledTime.get(li.getTerminalId());
                            if (prev == null || t >= prev) lastHandledTime.put(li.getTerminalId(), t);
                            continue;
                        }
                        boolean candidate = s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                                || s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code
                                || s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
                        if (candidate) {
                            String key = (li.getTerminalId() == null ? "" : li.getTerminalId()) + ":" + s;
                            LogInfo prevLog = latestByDeviceStatus.get(key);
                            long prevT = prevLog != null ? parseMillis(prevLog.getCreateTime()) : -1L;
                            if (prevLog == null || t >= prevT) latestByDeviceStatus.put(key, li);
                        }
                    }
                    for (LogInfo v : latestByDeviceStatus.values()) {
                        Long ht = lastHandledTime.get(v.getTerminalId());
                        long at = parseMillis(v.getCreateTime());
                        if (ht == null || at > ht) allowedIds.add(v.getId());
                    }
                    for (LogInfo li : logs) {
                        if (li.getStatusCode() == com.lora.cn.ui.constants.LogStatus.HANDLED.code) {
                            long ref = parseMillis(li.getHandleTime());
                            if (ref <= 0) ref = parseMillis(li.getCreateTime());
                            LogInfo src = null;
                            for (LogInfo x : logs) {
                                int s2 = x.getStatusCode();
                                boolean candidate2 = s2 == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                                        || s2 == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code
                                        || s2 == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
                                if (!candidate2) continue;
                                long tt2 = parseMillis(x.getCreateTime());
                                if (tt2 > 0 && tt2 <= ref) { if (src == null || tt2 >= parseMillis(src.getCreateTime())) src = x; }
                            }
                            if (src != null) handledLabels.put(li.getId(), com.lora.cn.ui.constants.LogStatus.toText(src.getStatusCode()));
                        }
                    }
                } catch (Exception ignored) {}
            } else if (logs == null) {
                err = "加载日志失败";
            }

            List<LogInfo> finalLogs = logs;
            java.util.Set<Long> finalAllowedIds = allowedIds;
            java.util.Map<Long, String> finalHandledLabels = handledLabels;
            String finalErr = err;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (token != logsSeq.get()) return;
                if (finalErr != null) {
                    rvLogs.setVisibility(View.GONE);
                    tvNoLogs.setVisibility(View.VISIBLE);
                    tvNoLogs.setText(finalErr);
                    return;
                }
                if (finalLogs != null && !finalLogs.isEmpty()) {
                    logAdapter.setAllowedHandleIds(finalAllowedIds);
                    logAdapter.setHandledSourceLabels(finalHandledLabels);
                    logAdapter.setOnHandleClickListener(item -> DialogUtils.showRemarkDialog(requireContext(), "确认处理", "已处理", remark -> {
                        if (ioExecutor == null || mainHandler == null) return;
                        ioExecutor.execute(() -> {
                            String user = com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", "");
                            String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                            try {
                                dbHelper.updateLogHandled(item.getId(), user, time, remark);
                            } catch (Exception ignored) {}
                            int s = item.getStatusCode();
                            if (s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code) {
                                int mask = 0;
                                if (s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code) mask |= 0x00000001;
                                String devHex = item.getDeviceId() != null ? item.getDeviceId() : "";
                                int finalMask = mask;
                                String finalDevHex = devHex;
                                mainHandler.post(() -> {
                                    try {
                                        android.app.Activity a = getActivity();
                                        if (a instanceof com.lora.cn.ui.activity.MainActivity) {
                                            ((com.lora.cn.ui.activity.MainActivity) a).sendHandleDownlink(finalDevHex, finalMask);
                                        }
                                    } catch (Exception ignored) {}
                                });
                            }
                            mainHandler.post(() -> {
                                loadLogs();
                                try {
                                    android.app.Activity a = getActivity();
                                    if (a instanceof com.lora.cn.ui.activity.MainActivity) {
                                        ((com.lora.cn.ui.activity.MainActivity) a).handleAlertHandled(item.getDeviceId(), s);
                                    }
                                } catch (Exception ignored) {}
                            });
                        });
                    }));
                    logAdapter.submitList(finalLogs);
                    rvLogs.setVisibility(View.VISIBLE);
                    tvNoLogs.setVisibility(View.GONE);
                } else {
                    rvLogs.setVisibility(View.GONE);
                    tvNoLogs.setVisibility(View.VISIBLE);
                }
            });
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

    private boolean deleteTerminal(String deviceId) {
        try {
            return dbHelper.deleteTerminalByDeviceId(deviceId) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
            updateMaintenanceCount(deviceId);
        } catch (Exception ignored) {}
    }

    @Override
    public void onStop() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        super.onStop();
    }

    // 订阅上行事件：匹配当前设备且处于等待状态时，刷新数据并结束等待
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUplinkDataEvent(UplinkDataEvent event) {
        if (event == null) return;
        try {
            LoRaFrameParser.ParsedFrame frame = LoRaFrameParser.parseFrame(event.getHex());
            if (frame == null || frame.deviceId == null) return;
            String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
            if (waitingForUplink && deviceId.equalsIgnoreCase(frame.deviceId)) {
                waitingForUplink = false;
                //if (btnHandleNow != null) btnHandleNow.setVisibility(View.GONE);
                // 刷新数据以反映最新状态与电量
                bindData();
            }
        } catch (Exception ignored) {}
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTerminalRefreshEvent(com.lora.cn.event.TerminalRefreshEvent event) {
        try {
            bindData();
        } catch (Exception ignored) {}
    }
}
