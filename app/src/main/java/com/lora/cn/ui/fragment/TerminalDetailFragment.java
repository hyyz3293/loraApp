package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.ui.adapter.LogInfoAdapter;
import com.lora.cn.ui.model.LogInfo;
import com.lora.cn.ui.view.SignalStrengthView;
import com.lora.cn.utils.LoRaProtocolParser;
import com.lora.cn.utils.DialogUtils;

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
    private LogInfoAdapter logAdapter;

    private TextView terminal_detail_type;
    private TextView terminal_detail_code;
    private SignalStrengthView signalView;
    private TextView terminal_detail_wifi;
    private TextView terminal_detail_battery;
    private TextView terminal_detail_id;

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
        logAdapter = new LogInfoAdapter();
        rvLogs.setAdapter(logAdapter);

        terminal_detail_type = v.findViewById(R.id.terminal_detail_type);
        terminal_detail_code = v.findViewById(R.id.terminal_detail_code);
        signalView = v.findViewById(R.id.signalView);
        terminal_detail_wifi = v.findViewById(R.id.terminal_detail_wifi);
        terminal_detail_battery = v.findViewById(R.id.terminal_detail_battery);
        terminal_detail_id = v.findViewById(R.id.terminal_detail_id);

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
                tvStatus.setText(!TextUtils.isEmpty(t.getStatus()) ? t.getStatus() : "-");
                tvBattery.setText(t.getBatteryLevel() + "%");
                // 分组类型：根据哪个分类ID非零判定
                String type = "-";
                if (t.getDepartmentId() > 0) type = "科室";
                else if (t.getRoomId() > 0) type = "病房";
                else if (t.getNursingGroupId() > 0) type = "护理组";
                else if (t.getOtherId() > 0) type = "其他";
                if (terminal_detail_type != null) terminal_detail_type.setText(type);

                // 设备CODE：读取新字段deviceCode
                String code = t.getDeviceCode();
                if (TextUtils.isEmpty(code)) code = "-";
                if (terminal_detail_code != null) terminal_detail_code.setText(code);

                // 状态（WiFi/在线状态）与电量
                if (terminal_detail_wifi != null) terminal_detail_wifi.setText(!TextUtils.isEmpty(t.getStatus()) ? t.getStatus() : "-");
                if (terminal_detail_battery != null) terminal_detail_battery.setText(t.getBatteryLevel() + "%");

                // 终端ID
                if (terminal_detail_id != null) terminal_detail_id.setText(t.getTerminalId());

                // 信号强度视图：将信号强度映射到0-4
                int strength = t.getSignalStrength();
                int level;
                if (strength >= 75) level = 4;
                else if (strength >= 50) level = 3;
                else if (strength >= 25) level = 2;
                else if (strength > 0) level = 1;
                else level = 0;
                if (signalView != null) signalView.setSignalStrength(4);
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
                if (signalView != null) signalView.setSignalStrength(4);
            }
        } catch (Exception ignored) {}

        // 设置收藏图标状态（根据数据库记录）
        try {
            com.lora.cn.database.dao.TerminalDao dao = new com.lora.cn.database.dao.TerminalDao(dbHelper);
            com.lora.cn.ui.model.Terminal t = dao.getTerminalByDeviceId(deviceId);
            boolean isFavorite = t != null && t.isFavorite();
            // 详情页的收藏图标保留逻辑：仅在收藏时显示星标
            ivFavorite.setVisibility(isFavorite ? View.VISIBLE : View.GONE);
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
            final String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
            DialogUtils.showTextEditDialog(requireContext(), "编辑名称", "设备名称", tvTitle.getText().toString(), "", new DialogUtils.OnConfirmListener() {
                @Override
                public void onConfirm(String newValue) {
                    if (TextUtils.isEmpty(newValue)) {
                        Toast.makeText(requireContext(), "名称不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    boolean ok = dbHelper.updateTerminalName(deviceId, newValue);
                    if (ok) {
                        tvTitle.setText(newValue);
                        Toast.makeText(requireContext(), "更新成功", Toast.LENGTH_SHORT).show();
                        // 记录日志（统一格式与外层日志使用同一LogInfoAdapter）
                        try {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                            String now = sdf.format(new java.util.Date());
                            LogInfo logInfo = new LogInfo();
                            logInfo.setTerminalId(deviceId);
                            logInfo.setTerminalName(newValue);
                            logInfo.setDeviceId(deviceId);
                            // 状态按枚举之一占位：设备打开（后续可根据具体业务改为更精确状态）
                            logInfo.setStatus("设备打开");
                            logInfo.setOperator("系统管理员");
                            logInfo.setOperationTime(now);
                            logInfo.setCreateTime(now);
                            logInfo.setAction("编辑名称为: " + newValue);
                            dbHelper.addLog(logInfo);
                            loadLogs();
                        } catch (Exception ignored) {}
                    } else {
                        Toast.makeText(requireContext(), "更新失败", Toast.LENGTH_SHORT).show();
                        try {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                            String now = sdf.format(new java.util.Date());
                            com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
                            logInfo.setTerminalId(deviceId);
                            logInfo.setTerminalName(tvTitle.getText().toString());
                            logInfo.setDeviceId(deviceId);
                            logInfo.setStatus("设备离线");
                            logInfo.setOperator("系统管理员");
                            logInfo.setOperationTime(now);
                            logInfo.setCreateTime(now);
                            logInfo.setAction("编辑名称失败");
                            dbHelper.addLog(logInfo);
                            loadLogs();
                        } catch (Exception ignored) {}
                    }
                }
            });
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
                    //ivFavorite.setImageResource(target ? R.mipmap.ic_coll : R.mipmap.ic_cw);
                    ivFavorite.setTag(target);
                    ivFavorite.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), target ? "已收藏" : "已取消收藏", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "更新收藏状态失败", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "收藏操作异常: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLogs() {
        String deviceId = getArguments() != null ? getArguments().getString(ARG_DEVICE_ID, "") : "";
        try {
            List<LogInfo> logs = dbHelper.getLogsByTerminalId(deviceId);
            if (logs != null && !logs.isEmpty()) {
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

    private boolean deleteTerminal(String deviceId) {
        try {
            return dbHelper.deleteTerminalByDeviceId(deviceId) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}