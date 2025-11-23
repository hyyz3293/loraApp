package com.lora.cn.ui.fragment;

import android.os.Bundle;
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
    private boolean waitingForUplink = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_terminal_detail, container, false);
        dbHelper = DatabaseHelper.getInstance(requireContext());
        initViews(v);
        bindData();
        setupListeners();
        loadLogs();
        return v;
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

    }

    private void bindData() {
        Bundle b = getArguments();
        if (b == null) return;
        String deviceId = b.getString(ARG_DEVICE_ID, "-");

        // 从数据库补齐详情文本：分组类型、设备CODE、状态(重复显示)、电量、终端ID、信号强度
        try {
            com.lora.cn.database.dao.TerminalDao dao = new com.lora.cn.database.dao.TerminalDao(dbHelper);
            com.lora.cn.ui.model.Terminal t = dao.getTerminalByDeviceId(deviceId);
            if (t != null) {
                long nowMs = System.currentTimeMillis();
                long timeoutMs = 10 * 60 * 1000L;
                if (t.getUpdateTime() <= 0 || nowMs - t.getUpdateTime() > timeoutMs) {
                    t.setStatus(com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE);
                }
                // 顶部基础信息
                tvTitle.setText(!TextUtils.isEmpty(t.getTerminalName()) ? t.getTerminalName() : "-");
                tvDeviceId.setText(!TextUtils.isEmpty(t.getTerminalId()) ? t.getTerminalId() : deviceId);
                String depText = t.getDepartment();
                String locText = t.getLocation();
                try {
                    com.lora.cn.database.DatabaseManager dm2 = com.lora.cn.database.DatabaseManager.getInstance(requireContext());
                    if ((depText == null || depText.isEmpty()) && t.getDepartmentId() > 0) {
                        com.lora.cn.database.entity.Category cd = dm2.getCategoryById(t.getDepartmentId());
                        if (cd != null) depText = cd.getCategoryName();
                    }
                    if ((locText == null || locText.isEmpty()) && t.getRoomId() > 0) {
                        com.lora.cn.database.entity.Category cr = dm2.getCategoryById(t.getRoomId());
                        if (cr != null) locText = cr.getCategoryName();
                    }
                } catch (Exception ignored) {}
                tvDepartment.setText(!TextUtils.isEmpty(depText) ? depText : "-");
                tvLocation.setText(!TextUtils.isEmpty(locText) ? locText : "-");
                int rssiRawTop = Math.max(0, Math.min(138, 138 -t.getRssi()));
                float percentTop = (138 - rssiRawTop) * 100f / 138f;
                tvStatus.setText(String.format("%.0f%%", percentTop));
                if (t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE) {
                    tvBattery.setText("");
                } else {
                    tvBattery.setText(t.getBatteryLevel() + "%");
                }
                // 分组展示：将选择的分组与分类名用“-”连接，不同分组用“，”隔开
                StringBuilder groupText = new StringBuilder();
                com.lora.cn.database.DatabaseManager dm = com.lora.cn.database.DatabaseManager.getInstance(requireContext());
                java.util.function.Consumer<Long> addByCategoryId = cid -> {
                    if (cid == null || cid <= 0) return;
                    com.lora.cn.database.entity.Category c = dm.getCategoryById(cid);
                    if (c == null) return;
                    long gid = c.getGroupId();
                    com.lora.cn.database.entity.Group g = dm.getGroupById(gid);
                    String gname = g != null ? g.getGroupName() : String.valueOf(gid);
                    String cname = c.getCategoryName();
                    if (groupText.length() > 0) groupText.append(", ");
                    groupText.append(gname).append("-").append(cname);
                };
                addByCategoryId.accept(t.getDepartmentId());
                addByCategoryId.accept(t.getRoomId());
                addByCategoryId.accept(t.getNursingGroupId());
                addByCategoryId.accept(t.getOtherId());
                try {
                    String ids = t.getGroupIdsText();
                    if (ids != null && !ids.isEmpty()) {
                        String[] toks = ids.split(",");
                        for (String tk : toks) {
                            if (tk == null || tk.trim().isEmpty()) continue;
                            String[] pr = tk.trim().split(":");
                            if (pr.length == 2) {
                                long gid = 0L, cid = 0L;
                                try { gid = Long.parseLong(pr[0]); cid = Long.parseLong(pr[1]); } catch (Exception ignored) {}
                                com.lora.cn.database.entity.Group g = dm.getGroupById(gid);
                                com.lora.cn.database.entity.Category c = dm.getCategoryById(cid);
                                String gname = g != null ? g.getGroupName() : pr[0];
                                String cname = c != null ? c.getCategoryName() : pr[1];
                                if (groupText.length() > 0) groupText.append(", ");
                                groupText.append(gname).append("-").append(cname);
                            }
                        }
                    }
                    if (groupText.length() == 0) {
                        String names = t.getGroupNamesText();
                        if (names != null && !names.isEmpty()) {
                            String[] toks = names.split(",");
                            for (String tk : toks) {
                                if (tk == null || tk.trim().isEmpty()) continue;
                                if (groupText.length() > 0) groupText.append(", ");
                                groupText.append(tk.trim());
                            }
                        }
                    }
                } catch (Exception ignored) {}
                if (terminal_detail_type != null) {
                    terminal_detail_type.setSingleLine(false);
                    terminal_detail_type.setEllipsize(null);
                    terminal_detail_type.setMaxLines(20);
                    terminal_detail_type.setText(t.getGroupNamesText().length() > 0 ? t.getGroupNamesText().toString() : "-");
                }

                // 设备CODE：读取新字段deviceCode
                String code = t.getDeviceCode();
                if (TextUtils.isEmpty(code)) code = "-";
                if (terminal_detail_code != null) terminal_detail_code.setText(code);

                // 状态（WiFi/在线状态）与电量
                if (terminal_detail_wifi != null) terminal_detail_wifi.setText(com.lora.cn.ui.constants.TerminalStatusConstants.codeToText(t.getStatus()));
                if (t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE) {
                    if (terminal_detail_battery != null) terminal_detail_battery.setText("");
                    if (batteryView != null) batteryView.setVisibility(View.GONE);
                    if (signalView != null) signalView.setVisibility(View.GONE);
                } else if (t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ONLINE
                        || t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_NORMAL_TAKEN
                        || t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ABNORMAL_TAKEN) {
                    int lowTh = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
                    boolean isLow = t.getBatteryLevel() <= lowTh;
                    if (terminal_detail_battery != null) {
                        terminal_detail_battery.setText(t.getBatteryLevel() + "%");
                        terminal_detail_battery.setTextColor(isLow ? android.graphics.Color.parseColor("#FF9500") : android.graphics.Color.parseColor("#333333"));
                    }
                    if (batteryView != null) {
                        batteryView.setVisibility(View.VISIBLE);
                        batteryView.setBatteryLevel(t.getBatteryLevel());
                    }
                    if (signalView != null) signalView.setVisibility(View.VISIBLE);
                }

                // 异常/离线显示“立即处理”按钮
                int stCode = t.getStatus();
                boolean showHandle = (stCode == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ABNORMAL_TAKEN)
                        || (stCode == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE);
//                if (btnHandleNow != null) {
//                    btnHandleNow.setVisibility(showHandle ? View.VISIBLE : View.GONE);
//                    btnHandleNow.setText(waitingForUplink ? "处理完成" : "立即处理");
//                }

                // 终端ID
                if (terminal_detail_id != null) terminal_detail_id.setText(t.getTerminalId());

                if (signalView != null && t.getStatus() != com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE) {
                    int rssiRaw = Math.max(0, Math.min(138, 138 - t.getRssi()));
                    int bars;
                    if (rssiRaw <= 65) bars = 4;
                    else if (rssiRaw <= 75) bars = 3;
                    else if (rssiRaw <= 85) bars = 2;
                    else if (rssiRaw <= 95) bars = 1;
                    else bars = 0;
                    signalView.setSignalStrength(bars);
                    int rev = 138 - rssiRaw;
                    float percent = rev * 100f / 138f;
                    if (tvStatus != null) tvStatus.setText(String.format("%.0f%%", percent));
                }
            } else {
                // 无记录时，回退为占位符
                tvTitle.setText("-");
                tvDeviceId.setText(deviceId);
                tvDepartment.setText("-");
                tvLocation.setText("-");
                tvStatus.setText("-");
                tvBattery.setText("-");
                if (terminal_detail_type != null) terminal_detail_type.setText("-");
                if (terminal_detail_code != null) terminal_detail_code.setText("-");
                if (terminal_detail_wifi != null) terminal_detail_wifi.setText("-");
                if (terminal_detail_battery != null) terminal_detail_battery.setText("-");
                if (terminal_detail_id != null) terminal_detail_id.setText(deviceId);
                if (signalView != null) signalView.setSignalStrength(0);
                //if (btnHandleNow != null) btnHandleNow.setVisibility(View.GONE);
            }
        } catch (Exception ignored) {}

        // 设置收藏图标状态（根据数据库记录）
        try {
            com.lora.cn.database.dao.TerminalDao dao = new com.lora.cn.database.dao.TerminalDao(dbHelper);
            com.lora.cn.ui.model.Terminal t = dao.getTerminalByDeviceId(deviceId);
            boolean isFavorite = t != null && t.isFavorite();
            // 详情页的收藏图标保留逻辑：仅在收藏时显示星标
            ivFavorite.setImageResource(isFavorite ? R.mipmap.ic_star_yeollw : R.mipmap.ic_start);
            //ivFavorite.setVisibility(isFavorite ? View.VISIBLE : View.GONE);
            ivFavorite.setTag(isFavorite);
        } catch (Exception ignored) {}
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                // 先恢复主界面可见性
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
        });
        btnEdit.setOnClickListener(v -> {
            String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
            com.lora.cn.database.dao.TerminalDao dao = new com.lora.cn.database.dao.TerminalDao(dbHelper);
            com.lora.cn.ui.model.Terminal t = dao.getTerminalByDeviceId(deviceId);
            if (t == null) {
                Toast.makeText(requireContext(), "未找到终端", Toast.LENGTH_SHORT).show();
                return;
            }
            AddDeviceFragment fragment = AddDeviceFragment.newInstance(t, "edit");
            androidx.appcompat.app.AppCompatActivity a = (androidx.appcompat.app.AppCompatActivity) getActivity();
            if (a != null) {
                a.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_device_list_container, fragment)
                        .addToBackStack("edit_device")
                        .commit();
            }
        });
        btnDelete.setOnClickListener(v -> {
            final String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
            DialogUtils.showConfirmDialog(requireContext(), "删除确认", "确定删除该终端吗？", new DialogUtils.OnConfirmDialogListener() {
                @Override
                public void onConfirm() {
                    boolean ok = deleteTerminal(deviceId);
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
            try {
                long uid = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
                if (uid > 0) {
                    dbHelper.setFavoriteForUser(uid, deviceId, target);
                } else {
                    dbHelper.updateTerminalFavoriteStatus(deviceId, target);
                }
                ivFavorite.setImageResource(target ? R.mipmap.ic_star_yeollw : R.mipmap.ic_start);
                if (target) {
                    ivFavorite.setColorFilter(android.graphics.Color.parseColor("#FFD700"));
                } else {
                    ivFavorite.clearColorFilter();
                }
                ivFavorite.setTag(target);
                ///ivFavorite.setVisibility(View.GONE);
                //Toast.makeText(requireContext(), target ? "已收藏" : "已取消收藏", Toast.LENGTH_SHORT).show();
                // 通知列表刷新收藏状态
                org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("更新收藏: " + deviceId));
                ivFavorite.invalidate();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "收藏操作异常: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

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

    private void loadLogs() {
        String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
        try {
            List<LogInfo> logs = dbHelper.getLogsByTerminalId(deviceId);
            if (logs != null && !logs.isEmpty()) {
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
                java.util.Set<Long> allowedIds = new java.util.HashSet<>();
                for (LogInfo v : latestByDeviceStatus.values()) {
                    Long ht = lastHandledTime.get(v.getTerminalId());
                    long at = parseMillis(v.getCreateTime());
                    if (ht == null || at > ht) allowedIds.add(v.getId());
                }
                logAdapter.setAllowedHandleIds(allowedIds);
                logAdapter.setOnHandleClickListener(item -> {
                    com.lora.cn.utils.DialogUtils.showRemarkDialog(requireContext(), "确认处理", "", new com.lora.cn.utils.DialogUtils.OnConfirmListener() {
                        @Override
                        public void onConfirm(String remark) {
                            String user = com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", "");
                            String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                            try {
                                dbHelper.updateLogHandled(item.getId(), user, time, remark);
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
                                loadLogs();
                                try {
                                    android.app.Activity a = getActivity();
                                    if (a instanceof com.lora.cn.ui.activity.MainActivity) {
                                        ((com.lora.cn.ui.activity.MainActivity) a).handleAlertHandled(item.getDeviceId(), s);
                                    }
                                } catch (Exception ignored) {}
                            } catch (Exception ignored) {}
                        }
                    });
                });
                java.util.Map<Long, String> handledLabels = new java.util.HashMap<>();
                try {
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
                logAdapter.setHandledSourceLabels(handledLabels);
                logAdapter.submitList(logs);
                rvLogs.setVisibility(View.VISIBLE);
                tvNoLogs.setVisibility(View.GONE);
            } else {
                rvLogs.setVisibility(View.GONE);
                tvNoLogs.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            rvLogs.setVisibility(View.GONE);
            tvNoLogs.setVisibility(View.VISIBLE);
            tvNoLogs.setText("加载日志失败: " + e.getMessage());
        }
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
