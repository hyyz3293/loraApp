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
import android.widget.LinearLayout;

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
    private com.lora.cn.database.DatabaseManager databaseManager;
    private int currentUserRoleId = -1;

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
    private com.scwang.smart.refresh.layout.SmartRefreshLayout refreshLayout;
    private int pageSize = 20;
    private int currentPage = 0;
    private int totalLogCount = 0;
    private java.util.List<com.lora.cn.ui.model.LogInfo> displayedLogs = new java.util.ArrayList<>();
    private boolean noMoreData = false;
    private androidx.recyclerview.widget.RecyclerView rvMaintenanceLogs;
    private android.widget.TextView tvNoMaintenance;
    private com.lora.cn.ui.adapter.MaintenanceInfoAdapter maintenanceAdapter;
    private final java.util.concurrent.atomic.AtomicInteger maintenanceLogsSeq = new java.util.concurrent.atomic.AtomicInteger(0);

    private TextView terminal_detail_type;
    private TextView terminal_detail_code;
    private SignalStrengthView signalView;
    private BatteryView batteryView;
    private TextView terminal_detail_wifi;
    private TextView terminal_detail_battery;
    private TextView terminal_detail_id;
    private TextView terminal_detail_version;
    private Button btnHandleNow;
    private TextView btnSetMaintenance;
    private boolean waitingForUplink = false;
    private ExecutorService ioExecutor;
    private Handler mainHandler;
    private final AtomicInteger bindSeq = new AtomicInteger(0);
    private final AtomicInteger logsSeq = new AtomicInteger(0);
    private final AtomicInteger maintenanceSeq = new AtomicInteger(0);
    private final Runnable deferBindRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded()) bindData();
        }
    };
    private final Runnable deferMaintenanceCountRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) return;
            String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
            updateMaintenanceCount(deviceId);
        }
    };
    private View layoutMaintenanceBlock;
    private View layoutLogsBlock;
    private View tab_maintenance, tab_logs;


    private ImageView btnToggleMaintenance;
    private ImageView btnToggleLogs;
    private View maintenanceHeaderRow;
    private View logsHeaderRow;
    private boolean maintenanceCollapsed = false;
    private boolean logsCollapsed = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_terminal_detail, container, false);
        dbHelper = DatabaseHelper.getInstance(requireContext());
        databaseManager = com.lora.cn.database.DatabaseManager.getInstance(requireContext());
        long userId = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
        if (userId != -1) {
            com.lora.cn.database.entity.User user = databaseManager.getUserById(userId);
            if (user != null) currentUserRoleId = (int) user.getRoleId();
        }
        if (ioExecutor == null) ioExecutor = Executors.newSingleThreadExecutor();
        if (mainHandler == null) mainHandler = new Handler(Looper.getMainLooper());
        initViews(v);
        setupListeners();
        if (mainHandler != null) {
            mainHandler.postDelayed(() -> {
                if (!isAdded()) return;
                bindData();
                loadLogs();
                loadMaintenanceLogs();
            }, 500);
        }
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (ioExecutor != null) ioExecutor.shutdownNow();
            if (mainHandler != null) {
                mainHandler.removeCallbacks(deferBindRunnable);
                mainHandler.removeCallbacks(deferMaintenanceCountRunnable);
            }
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
        refreshLayout = v.findViewById(R.id.refreshLayout);
        rvLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        logAdapter = new LogDetailInfoAdapter();
        rvLogs.setAdapter(logAdapter);
        rvMaintenanceLogs = v.findViewById(R.id.rv_maintenance_logs);
        tvNoMaintenance = v.findViewById(R.id.tv_no_maintenance);
        if (rvMaintenanceLogs != null) {
            rvMaintenanceLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
            maintenanceAdapter = new com.lora.cn.ui.adapter.MaintenanceInfoAdapter(com.lora.cn.ui.adapter.MaintenanceInfoAdapter.Mode.DETAIL);
            maintenanceAdapter.setOnConfirmClickListener(this::showMaintenanceConfirmDialog);
            maintenanceAdapter.setOnViewClickListener(item -> {
                android.app.AlertDialog.Builder b1 = new android.app.AlertDialog.Builder(requireContext());
                b1.setTitle("维护内容");
                b1.setMessage(item.getContent() == null ? "" : item.getContent());
                b1.setPositiveButton("关闭", (d, w) -> d.dismiss());
                b1.show();
            });
            maintenanceAdapter.setOnViewRemarkClickListener(item -> {
                android.app.AlertDialog.Builder b1 = new android.app.AlertDialog.Builder(requireContext());
                b1.setTitle("备注");
                String remark = item.getHandleRemark();
                b1.setMessage(android.text.TextUtils.isEmpty(remark) ? "暂无备注" : remark);
                b1.setPositiveButton("关闭", (d, w) -> d.dismiss());
                b1.show();
            });
            rvMaintenanceLogs.setAdapter(maintenanceAdapter);
        }

        terminal_detail_type = v.findViewById(R.id.terminal_detail_type);
        terminal_detail_code = v.findViewById(R.id.terminal_detail_code);
        signalView = v.findViewById(R.id.signalView);
        batteryView = v.findViewById(R.id.batteryView);
        terminal_detail_wifi = v.findViewById(R.id.terminal_detail_wifi);
        terminal_detail_battery = v.findViewById(R.id.terminal_detail_battery);
        terminal_detail_id = v.findViewById(R.id.terminal_detail_id);
        terminal_detail_version = v.findViewById(R.id.terminal_detail_version);
        btnHandleNow = v.findViewById(R.id.btn_handle_now);
        btnSetMaintenance = v.findViewById(R.id.btn_set_maintenance);
        if (btnEdit != null) btnEdit.setVisibility(hasPermission("terminal_edit") ? View.VISIBLE : View.GONE);
        if (btnDelete != null) btnDelete.setVisibility(hasPermission("terminal_delete") ? View.VISIBLE : View.GONE);
        if (ivFavorite != null) ivFavorite.setVisibility(hasPermission("terminal_mark") ? View.VISIBLE : View.GONE);
        if (btnSetMaintenance != null) btnSetMaintenance.setVisibility(hasPermission("terminal_confirm") ? View.VISIBLE : View.GONE);

        tab_maintenance = v.findViewById(R.id.tab_maintenance);
        tab_logs = v.findViewById(R.id.tab_logs);


        layoutMaintenanceBlock = v.findViewById(R.id.layout_maintenance_block);
        layoutLogsBlock = v.findViewById(R.id.layout_logs_block);
        btnToggleMaintenance = v.findViewById(R.id.btn_toggle_maintenance);
        btnToggleLogs = v.findViewById(R.id.btn_toggle_logs);
        maintenanceHeaderRow = v.findViewById(R.id.maintenance_header_row);
        logsHeaderRow = v.findViewById(R.id.logs_header_row);
        if (btnToggleMaintenance != null) {
            btnToggleMaintenance.setOnClickListener(view -> {
                maintenanceCollapsed = !maintenanceCollapsed;
                applyToggleUi();
            });
        }
        if (btnToggleLogs != null) {
            btnToggleLogs.setOnClickListener(view -> {
                logsCollapsed = !logsCollapsed;
                applyToggleUi();
            });
        }
        applyToggleUi();

        tab_maintenance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutMaintenanceBlock.setVisibility(View.VISIBLE);
                layoutLogsBlock.setVisibility(View.GONE);
                if (tab_maintenance instanceof android.widget.TextView) {
                    ((android.widget.TextView) tab_maintenance).setTextColor(android.graphics.Color.parseColor("#383B40"));
                }
                if (tab_logs instanceof android.widget.TextView) {
                    ((android.widget.TextView) tab_logs).setTextColor(android.graphics.Color.parseColor("#8291A9"));
                }
                tab_maintenance.setSelected(true);
                tab_logs.setSelected(false);
            }
        });

        tab_logs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutMaintenanceBlock.setVisibility(View.GONE);
                layoutLogsBlock.setVisibility(View.VISIBLE);
                if (tab_logs instanceof android.widget.TextView) {
                    ((android.widget.TextView) tab_logs).setTextColor(android.graphics.Color.parseColor("#383B40"));
                }
                if (tab_maintenance instanceof android.widget.TextView) {
                    ((android.widget.TextView) tab_maintenance).setTextColor(android.graphics.Color.parseColor("#8291A9"));
                }
                tab_logs.setSelected(true);
                tab_maintenance.setSelected(false);
            }
        });
        if (tab_maintenance instanceof android.widget.TextView && tab_logs instanceof android.widget.TextView) {
            ((android.widget.TextView) tab_maintenance).setTextColor(android.graphics.Color.parseColor("#383B40"));
            ((android.widget.TextView) tab_logs).setTextColor(android.graphics.Color.parseColor("#8291A9"));
        }
        tab_maintenance.setSelected(true);
        tab_logs.setSelected(false);
        if (terminal_detail_version != null) terminal_detail_version.setText("");
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
            String firmwareVersion = LoRaFrameParser.normalizeFirmwareVersionString(
                    com.blankj.utilcode.util.SPUtils.getInstance().getString("terminal_firmware_version", "")
            );
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
                java.util.List<com.lora.cn.ui.model.MaintenanceInfo> list = dbHelper.getMaintenanceRecordsByTerminal(deviceId, uid);
                int cnt = 0;
                if (list != null) {
                    long now = System.currentTimeMillis();
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault());
                    for (com.lora.cn.ui.model.MaintenanceInfo mi : list) {
                        String c = mi != null ? mi.getContent() : null;
                        String ct = mi != null ? mi.getCreateTime() : null;
                        if (ct == null || ct.trim().isEmpty()) continue;
                        boolean isAuto = "主动维护".equals(c);
                        try {
                            java.util.Date dt = sdf.parse(ct.trim());
                            if (dt != null && dt.getTime() > now)
                                if (!isAuto && mi.getStatus() == 0) cnt++;
                        } catch (Exception ignored) {}
                    }
                }
                maintenanceCount = cnt;
            } catch (Exception ignored) {}

            String finalTitle = title;
            String finalShowDeviceId = showDeviceId;
            String finalDepText = depText;
            String finalLocText = locText;
            String finalStatusPercent = statusPercent;
            String finalTopBattery = topBattery;
            String finalGroupNames = groupNames;
            String finalGroupChildren;
            {
                java.util.List<String> toks = new java.util.ArrayList<>();
                if (finalGroupNames != null && !finalGroupNames.trim().isEmpty()) {
                    String[] arr = finalGroupNames.split(",");
                    for (String tk : arr) {
                        if (tk == null) continue;
                        String raw = tk.trim();
                        if (raw.isEmpty()) continue;
                        int p = raw.lastIndexOf('-');
                        toks.add(p >= 0 ? raw.substring(p + 1) : raw);
                    }
                }
                StringBuilder sb = new StringBuilder();
                for (int i2 = 0; i2 < toks.size(); i2++) {
                    if (i2 > 0) sb.append("、");
                    sb.append(toks.get(i2));
                }
                finalGroupChildren = sb.toString();
            }
            String finalCode = code;
            String finalWifiText = wifiText;
            String finalBatteryText = batteryText;
            String finalFirmwareVersion = firmwareVersion;
            int finalBatteryLevel = batteryLevel;
            boolean finalShowBatteryAndSignal = showBatteryAndSignal;
            int finalSignalBars = signalBars;
            boolean finalIsFavorite = isFavorite;
            int finalMaintenanceCount = maintenanceCount;
            int finalSt = st;

            Handler h = mainHandler;
            if (h == null) return;
            h.post(() -> {
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
                    terminal_detail_type.setText(finalGroupChildren);
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
                if (terminal_detail_version != null) terminal_detail_version.setText(finalFirmwareVersion);
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
                ((com.lora.cn.ui.activity.MainActivity) a).hideDeviceListImmediate();
                return;
            }
            if (getParentFragmentManager().getBackStackEntryCount() > 0) getParentFragmentManager().popBackStack();
        });
        btnEdit.setOnClickListener(v -> {
            if (!hasPermission("terminal_edit")) {
                Toast.makeText(requireContext(), "您没有编辑终端的权限", Toast.LENGTH_SHORT).show();
                return;
            }
            String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
            AddDeviceFragment fragment = AddDeviceFragment.newInstance(deviceId, "edit");
            androidx.appcompat.app.AppCompatActivity a = (androidx.appcompat.app.AppCompatActivity) getActivity();
            if (a != null) {
                a.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_device_list_container, fragment)
                        .addToBackStack("edit_device")
                        .commit();
            }
        });
        btnDelete.setOnClickListener(v -> {
            if (!hasPermission("terminal_delete")) {
                Toast.makeText(requireContext(), "您没有删除终端的权限", Toast.LENGTH_SHORT).show();
                return;
            }
            final String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
            DialogUtils.showConfirmDialog(requireContext(), "删除确认", "确定删除该终端吗？", new DialogUtils.OnConfirmDialogListener() {
                @Override
                public void onConfirm() {
                    if (ioExecutor == null || mainHandler == null) return;
                    ioExecutor.execute(() -> {
                        boolean ok = deleteTerminal(deviceId);
                        Handler h3 = mainHandler;
                        if (h3 == null) return;
                        h3.post(() -> {
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
            if (!hasPermission("terminal_mark")) {
                Toast.makeText(requireContext(), "您没有收藏终端的权限", Toast.LENGTH_SHORT).show();
                return;
            }
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
                    Handler h4 = mainHandler;
                    if (h4 == null) return;
                    h4.post(() -> {
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
                    Handler h5 = mainHandler;
                    if (h5 == null) return;
                    h5.post(() -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "收藏操作异常: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        if (btnSetMaintenance != null) {
            btnSetMaintenance.setOnClickListener(v -> {
                if (!hasPermission("terminal_confirm")) {
                    Toast.makeText(requireContext(), "您没有维护操作的权限", Toast.LENGTH_SHORT).show();
                    return;
                }
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
                java.util.List<com.lora.cn.ui.model.MaintenanceInfo> list = dbHelper.getMaintenanceRecordsByTerminal(deviceId, uid);
                if (list != null) {
                    long now = System.currentTimeMillis();
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault());
                    for (com.lora.cn.ui.model.MaintenanceInfo mi : list) {
                        String c = mi != null ? mi.getContent() : null;
                        String ct = mi != null ? mi.getCreateTime() : null;
                        if (ct == null || ct.trim().isEmpty()) continue;
                        boolean isAuto = "主动维护".equals(c);
                        try {
                            java.util.Date dt = sdf.parse(ct.trim());
                            if (dt != null && dt.getTime() > now)
                                if (!isAuto && mi.getStatus() == 0) count++;;
                        } catch (Exception ignored) {}

                    }
                }
            } catch (Exception ignored) {}
            int finalCount = count;
            Handler h6 = mainHandler;
            if (h6 == null) return;
            h6.post(() -> {
                if (!isAdded()) return;
                if (token != maintenanceSeq.get()) return;
                btnSetMaintenance.setText("设置维护(" + finalCount + ")");
            });
        });
        if (refreshLayout != null) {
            refreshLayout.setEnableRefresh(true);
            refreshLayout.setEnableLoadMore(true);
            refreshLayout.setOnRefreshListener(layout -> {
                currentPage = 0;
                noMoreData = false;
                loadLogs();
                layout.finishRefresh(true);
            });
            refreshLayout.setOnLoadMoreListener(layout -> {
                //String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
                boolean canNext = (currentPage + 1) * pageSize < totalLogCount && !noMoreData;
                if (!canNext) {
                    layout.finishLoadMoreWithNoMoreData();
                    noMoreData = true;
                    refreshLayout.setEnableLoadMore(false);
                    return;
                }
                int nextPage = currentPage + 1;
                if (ioExecutor == null || mainHandler == null) {
                    layout.finishLoadMore(false);
                    return;
                }
                ioExecutor.execute(() -> {
                    java.util.List<com.lora.cn.ui.model.LogInfo> next = null;
                    try {
                        next = dbHelper.queryLogsByTerminalPaged(deviceId, pageSize, nextPage);
                    } catch (Exception ignored) {}
                    java.util.List<com.lora.cn.ui.model.LogInfo> finalNext = next;
                    Handler h = mainHandler;
                    if (h == null) return;
                    h.post(() -> {
                        if (!isAdded()) return;
                        if (finalNext != null && !finalNext.isEmpty()) {
                            displayedLogs.addAll(finalNext);
                            logAdapter.submitList(new java.util.ArrayList<>(displayedLogs));
                            layout.finishLoadMore(true);
                            currentPage = nextPage;
                            boolean noMore = (currentPage + 1) * pageSize >= totalLogCount || finalNext.size() < pageSize;
                            noMoreData = noMore;
                            refreshLayout.setEnableLoadMore(!noMoreData);
                        } else {
                            layout.finishLoadMoreWithNoMoreData();
                            noMoreData = true;
                            refreshLayout.setEnableLoadMore(false);
                        }
                    });
                });
            });
        }
    }

    private void loadLogs() {
        if (ioExecutor == null || mainHandler == null) return;
        String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
        int token = logsSeq.incrementAndGet();
        ioExecutor.execute(() -> {
            List<LogInfo> logs;
            try {
                totalLogCount = dbHelper.queryLogsByTerminalCount(deviceId);
                logs = dbHelper.queryLogsByTerminalPaged(deviceId, pageSize, 0);
            } catch (Exception e) {
                logs = null;
            }

            java.util.Set<Long> allowedIds = new java.util.HashSet<>();
            java.util.Map<Long, String> handledLabels = new java.util.HashMap<>();
            String err = null;

            if (logs != null && !logs.isEmpty()) {
                try {
                    java.util.List<LogInfo> allLogs = dbHelper.getLogsByTerminalId(deviceId);
                    java.util.Map<String, Long> lastHandledTime = new java.util.HashMap<>();
                    java.util.Map<String, LogInfo> latestByDeviceStatus = new java.util.HashMap<>();
                    for (LogInfo li : allLogs) {
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
                    for (LogInfo li : allLogs) {
                        if (li.getStatusCode() == com.lora.cn.ui.constants.LogStatus.HANDLED.code) {
                            long ref = parseMillis(li.getHandleTime());
                            if (ref <= 0) ref = parseMillis(li.getCreateTime());
                            LogInfo src = null;
                            for (LogInfo x : allLogs) {
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
            Handler h7 = mainHandler;
            if (h7 == null) return;
            h7.post(() -> {
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
                                Handler h8 = mainHandler;
                                if (h8 == null) return;
                                h8.post(() -> {
                                    try {
                                        android.app.Activity a = getActivity();
                                        if (a instanceof com.lora.cn.ui.activity.MainActivity) {
                                            ((com.lora.cn.ui.activity.MainActivity) a).sendHandleDownlink(finalDevHex, finalMask);
                                        }
                                    } catch (Exception ignored) {}
                                });
                            }
                            Handler h9 = mainHandler;
                            if (h9 == null) return;
                            h9.post(() -> {
                                currentPage = 0;
                                noMoreData = false;
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
                    displayedLogs.clear();
                    displayedLogs.addAll(finalLogs);
                    logAdapter.submitList(new java.util.ArrayList<>(displayedLogs));
                    rvLogs.setVisibility(View.VISIBLE);
                    tvNoLogs.setVisibility(View.GONE);
                    boolean noMore = (currentPage + 1) * pageSize >= totalLogCount || finalLogs.size() < pageSize;
                    noMoreData = noMore;
                    if (refreshLayout != null) refreshLayout.setEnableLoadMore(!noMoreData);
                } else {
                    rvLogs.setVisibility(View.GONE);
                    tvNoLogs.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void loadMaintenanceLogs() {
        if (ioExecutor == null || mainHandler == null) return;
        String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
        int token = maintenanceLogsSeq.incrementAndGet();
        ioExecutor.execute(() -> {
            java.util.List<com.lora.cn.ui.model.MaintenanceInfo> list = null;
            String err = null;
            try {
                long uid = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
                java.util.List<com.lora.cn.ui.model.MaintenanceInfo> all = dbHelper.getMaintenanceRecords(uid);
                java.util.ArrayList<com.lora.cn.ui.model.MaintenanceInfo> filtered = new java.util.ArrayList<>();
                long now = System.currentTimeMillis();
                if (all != null) {
                    for (com.lora.cn.ui.model.MaintenanceInfo mi : all) {
                        if (mi == null) continue;
                        String tid = mi.getTerminalId();
                        if (tid == null || !tid.equalsIgnoreCase(deviceId)) continue;
                        String ct = mi.getCreateTime();
                        if (ct == null || ct.trim().isEmpty()) continue;
                        long tm = parseMillis(ct);
                        if (tm < 0) continue;
                        if (tm > now) continue;
                        filtered.add(mi);
                    }
                }
                list = filtered;
            } catch (Exception e) {
                err = "加载维护日志失败";
            }
            java.util.List<com.lora.cn.ui.model.MaintenanceInfo> finalList = list;
            String finalErr = err;
            Handler h = mainHandler;
            if (h == null) return;
            h.post(() -> {
                if (!isAdded()) return;
                if (token != maintenanceLogsSeq.get()) return;
                if (finalErr != null) {
                    if (rvMaintenanceLogs != null) rvMaintenanceLogs.setVisibility(View.GONE);
                    if (tvNoMaintenance != null) {
                        tvNoMaintenance.setVisibility(View.VISIBLE);
                        tvNoMaintenance.setText(finalErr);
                    }
                    return;
                }
                if (finalList != null && !finalList.isEmpty()) {
                    if (rvMaintenanceLogs != null) rvMaintenanceLogs.setVisibility(View.VISIBLE);
                    if (tvNoMaintenance != null) tvNoMaintenance.setVisibility(View.GONE);
                    if (maintenanceAdapter != null) {
                        maintenanceAdapter.submitList(new java.util.ArrayList<>(finalList));
                    }
                } else {
                    if (rvMaintenanceLogs != null) rvMaintenanceLogs.setVisibility(View.GONE);
                    if (tvNoMaintenance != null) {
                        tvNoMaintenance.setVisibility(View.VISIBLE);
                        tvNoMaintenance.setText("暂无维护记录");
                    }
                }
            });
        });
    }

    private void showMaintenanceConfirmDialog(com.lora.cn.ui.model.MaintenanceInfo item) {
        if (item == null) return;
        android.widget.EditText et = new android.widget.EditText(requireContext());
        et.setMinLines(3);
        et.setHint("请输入备注");
        try {
            et.setText("确认维护");
            et.setSelection(et.getText().length());
        } catch (Exception ignored) {}
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("确认维护")
                .setView(et)
                .setPositiveButton("确认", (d, w) -> {
                    String remark = et.getText() != null ? et.getText().toString().trim() : "";
                    if (ioExecutor == null || mainHandler == null) return;
                    ioExecutor.execute(() -> {
                        long uid = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
                        String user = com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", "");
                        String time = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                        int r;
                        try {
                            r = dbHelper.updateMaintenanceHandled(item.getId(), uid, user, time, remark);
                        } catch (Exception ignored) {
                            r = 0;
                        }
                        try {
                            String c = item.getContent();
                            if (r > 0 && c != null && c.startsWith("设备维护：")) {
                                dbHelper.setTerminalMaintenanceClearPending(item.getTerminalId(), true);
                            }
                        } catch (Exception ignored) {}
                        Handler h = mainHandler;
                        if (h == null) return;
                        h.post(this::loadMaintenanceLogs);
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void applyToggleUi() {
        try {
            if (btnToggleMaintenance != null) btnToggleMaintenance.setImageResource(maintenanceCollapsed ? R.drawable.ic_chevron_down : R.drawable.ic_chevron_up);
            if (btnToggleLogs != null) btnToggleLogs.setImageResource(logsCollapsed ? R.drawable.ic_chevron_down : R.drawable.ic_chevron_up);
            if (maintenanceHeaderRow != null) maintenanceHeaderRow.setVisibility(maintenanceCollapsed ? View.GONE : View.VISIBLE);
            if (rvMaintenanceLogs != null) rvMaintenanceLogs.setVisibility(maintenanceCollapsed ? View.GONE : View.VISIBLE);
            if (tvNoMaintenance != null) tvNoMaintenance.setVisibility(maintenanceCollapsed ? View.GONE : tvNoMaintenance.getVisibility());
            if (logsHeaderRow != null) logsHeaderRow.setVisibility(logsCollapsed ? View.GONE : View.VISIBLE);
            if (refreshLayout != null) refreshLayout.setVisibility(logsCollapsed ? View.GONE : View.VISIBLE);
            if (tvNoLogs != null) tvNoLogs.setVisibility(logsCollapsed ? View.GONE : tvNoLogs.getVisibility());
            if (layoutMaintenanceBlock != null) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) layoutMaintenanceBlock.getLayoutParams();
                lp.height = maintenanceCollapsed ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
                lp.weight = maintenanceCollapsed ? 0f : 1f;
                layoutMaintenanceBlock.setLayoutParams(lp);
            }
            if (layoutLogsBlock != null) {
                LinearLayout.LayoutParams lp2 = (LinearLayout.LayoutParams) layoutLogsBlock.getLayoutParams();
                lp2.height = logsCollapsed ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
                lp2.weight = logsCollapsed ? 0f : 1f;
                layoutLogsBlock.setLayoutParams(lp2);
            }
        } catch (Exception ignored) {}
    }

    private long parseMillis(String time) {
        if (time == null || time.trim().isEmpty()) return -1L;
        String raw = time.trim();
        java.util.List<java.text.SimpleDateFormat> formats = new java.util.ArrayList<>();
        formats.add(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()));
        formats.add(new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()));
        for (java.text.SimpleDateFormat f : formats) {
            try {
                java.util.Date d = f.parse(raw);
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
        }
        return -1L;
    }

    private boolean deleteTerminal(String deviceId) {
        try {
            return dbHelper.deleteTerminalByDeviceId(deviceId) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasPermission(String permissionCode) {
        if (currentUserRoleId == -1) {
            return false;
        }
        try {
            return databaseManager.hasPermission(currentUserRoleId, permissionCode);
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
            if (mainHandler == null) mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.removeCallbacks(deferMaintenanceCountRunnable);
            mainHandler.postDelayed(deferMaintenanceCountRunnable, 300);
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
                if (mainHandler == null) mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.removeCallbacks(deferBindRunnable);
                mainHandler.postDelayed(deferBindRunnable, 200);
            }
        } catch (Exception ignored) {}
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTerminalRefreshEvent(com.lora.cn.event.TerminalRefreshEvent event) {
        try {
            if (!isResumed()) return;
            if (mainHandler == null) mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.removeCallbacks(deferBindRunnable);
            mainHandler.postDelayed(deferBindRunnable, 500);
        } catch (Exception ignored) {}
    }
}
