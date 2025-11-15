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
                // 顶部基础信息
                tvTitle.setText(!TextUtils.isEmpty(t.getTerminalName()) ? t.getTerminalName() : "-");
                tvDeviceId.setText(!TextUtils.isEmpty(t.getTerminalId()) ? t.getTerminalId() : deviceId);
                tvDepartment.setText(!TextUtils.isEmpty(t.getDepartment()) ? t.getDepartment() : "-");
                tvLocation.setText(!TextUtils.isEmpty(t.getLocation()) ? t.getLocation() : "-");
                tvStatus.setText(com.lora.cn.ui.constants.TerminalStatusConstants.codeToText(t.getStatus()));
                if (t.getStatus() == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE) {
                    tvBattery.setText("");
                } else {
                    tvBattery.setText(t.getBatteryLevel() + "%");
                }
                // 分组展示：将选择的分组与分类名用“-”连接，不同分组用“，”隔开
                StringBuilder groupText = new StringBuilder();
                com.lora.cn.database.DatabaseManager dm = com.lora.cn.database.DatabaseManager.getInstance(requireContext());
                if (t.getDepartmentId() > 0) {
                    com.lora.cn.database.entity.Category c = dm.getCategoryById(t.getDepartmentId());
                    String name = c != null ? c.getCategoryName() : String.valueOf(t.getDepartmentId());
                    if (groupText.length() > 0) groupText.append(", ");
                    groupText.append("科室-").append(name);
                }
                if (t.getRoomId() > 0) {
                    com.lora.cn.database.entity.Category c = dm.getCategoryById(t.getRoomId());
                    String name = c != null ? c.getCategoryName() : String.valueOf(t.getRoomId());
                    if (groupText.length() > 0) groupText.append(", ");
                    groupText.append("病房-").append(name);
                }
                if (t.getNursingGroupId() > 0) {
                    com.lora.cn.database.entity.Category c = dm.getCategoryById(t.getNursingGroupId());
                    String name = c != null ? c.getCategoryName() : String.valueOf(t.getNursingGroupId());
                    if (groupText.length() > 0) groupText.append(", ");
                    groupText.append("护理组-").append(name);
                }
                if (t.getOtherId() > 0) {
                    com.lora.cn.database.entity.Category c = dm.getCategoryById(t.getOtherId());
                    String name = c != null ? c.getCategoryName() : String.valueOf(t.getOtherId());
                    if (groupText.length() > 0) groupText.append(", ");
                    groupText.append("其他-").append(name);
                }
                if (terminal_detail_type != null) terminal_detail_type.setText(groupText.length() > 0 ? groupText.toString() : "-");

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
                } else {
                    if (terminal_detail_battery != null) terminal_detail_battery.setText(t.getBatteryLevel() + "%");
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
                    signalView.setSignalStrength(Math.max(0, Math.min(4, t.getSignalStrength())));
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
        ivFavorite.setOnClickListener(v -> {
            String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
            Object tag = ivFavorite.getTag();
            boolean current = tag instanceof Boolean ? (Boolean) tag : false;
            boolean target = !current;
            try {
                int result = dbHelper.updateTerminalFavoriteStatus(deviceId, target);
                if (result > 0) {
                    ivFavorite.setImageResource(target ? R.mipmap.ic_star_yeollw : R.mipmap.ic_start);
                    ivFavorite.setTag(target);
                    ///ivFavorite.setVisibility(View.GONE);
                    //Toast.makeText(requireContext(), target ? "已收藏" : "已取消收藏", Toast.LENGTH_SHORT).show();
                    // 通知列表刷新收藏状态
                    org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("更新收藏: " + deviceId));
                } else {
                    Toast.makeText(requireContext(), "更新收藏状态失败", Toast.LENGTH_SHORT).show();
                }
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
                java.util.Map<Integer, LogInfo> latest = new java.util.HashMap<>();
                for (LogInfo li : logs) {
                    int s = li.getStatusCode();
                    boolean candidate = s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                            || s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code
                            || s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
                    if (candidate) {
                        LogInfo prev = latest.get(s);
                        long prevT = prev != null ? parseMillis(prev.getCreateTime()) : -1L;
                        long curT = parseMillis(li.getCreateTime());
                        if (prev == null || curT >= prevT) latest.put(s, li);
                    }
                }
                java.util.Set<Long> allowedIds = new java.util.HashSet<>();
                for (LogInfo v : latest.values()) allowedIds.add(v.getId());
                logAdapter.setAllowedHandleIds(allowedIds);
                logAdapter.setOnHandleClickListener(item -> {
                    com.lora.cn.utils.DialogUtils.showRemarkDialog(requireContext(), "确认处理", "", new com.lora.cn.utils.DialogUtils.OnConfirmListener() {
                        @Override
                        public void onConfirm(String remark) {
                            String user = com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", "");
                            String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                            try {
                                dbHelper.updateLogHandled(item.getId(), user, time, remark);
                                loadLogs();
                            } catch (Exception ignored) {}
                        }
                    });
                });
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
