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
import com.lora.cn.utils.LoRaProtocolParser;
import com.lora.cn.utils.DialogUtils;

import java.util.List;

public class TerminalDetailFragment extends Fragment {

    private static final String ARG_DEVICE_ID = "arg_device_id";
    private static final String ARG_DEVICE_NAME = "arg_device_name";
    private static final String ARG_DEPARTMENT = "arg_department";
    private static final String ARG_LOCATION = "arg_location";
    private static final String ARG_STATUS = "arg_status";
    private static final String ARG_BATTERY = "arg_battery";

    public static TerminalDetailFragment newInstance(LoRaProtocolParser.TerminalInfo info) {
        TerminalDetailFragment f = new TerminalDetailFragment();
        Bundle b = new Bundle();
        b.putString(ARG_DEVICE_ID, info.deviceId);
        b.putString(ARG_DEVICE_NAME, info.deviceName);
        b.putString(ARG_DEPARTMENT, info.department);
        b.putString(ARG_LOCATION, info.location);
        b.putString(ARG_STATUS, info.status == 1 ? "在线" : (info.status == 0 ? "离线" : "异常"));
        b.putInt(ARG_BATTERY, info.batteryLevel);
        f.setArguments(b);
        return f;
    }

    private DatabaseHelper dbHelper;

    private TextView tvTitle;
    private ImageView btnBack;
    private ImageView btnEdit;
    private ImageView btnDelete;
    private TextView tvDeviceId;
    private TextView tvDepartment;
    private TextView tvLocation;
    private TextView tvStatus;
    private TextView tvBattery;
    private RecyclerView rvLogs;
    private TextView tvNoLogs;
    private LogInfoAdapter logAdapter;

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
    }

    private void bindData() {
        Bundle b = getArguments();
        if (b == null) return;
        String deviceId = b.getString(ARG_DEVICE_ID, "-");
        String deviceName = b.getString(ARG_DEVICE_NAME, "-");
        String dept = b.getString(ARG_DEPARTMENT, "-");
        String loc = b.getString(ARG_LOCATION, "-");
        String st = b.getString(ARG_STATUS, "-");
        int battery = b.getInt(ARG_BATTERY, 0);

        tvTitle.setText(deviceName);
        tvDeviceId.setText(deviceId);
        tvDepartment.setText(dept);
        tvLocation.setText(loc);
        tvStatus.setText(st);
        tvBattery.setText(battery + "%");
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
                            com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
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