package com.lora.cn.ui.adapter;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.ui.model.Terminal;

import java.util.List;

public class TerminalAdapter extends BaseQuickAdapter<Terminal, QuickViewHolder> {

    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int i, @Nullable Terminal item) {
        TextView terminalTitle = holder.getView(R.id.terminal_title);
        TextView terminalKs = holder.getView(R.id.terminal_ks);
        TextView terminalBf = holder.getView(R.id.terminal_bf);
        ImageView ivStatusIcon = holder.getView(R.id.iv_status_icon);
        TextView tvStatusTitle = holder.getView(R.id.tv_status_title);
        ImageView ivBatteryIcon = holder.getView(R.id.iv_battery_icon);
        TextView tvBatteryTitle = holder.getView(R.id.tv_battery_title);

        if (item == null) {
            return;
        }

        // 设置终端基本信息
        terminalTitle.setText(getDisplayText(item.getName(), item.getTerminalName(), "未知终端"));
        
        // 设置科室信息，优先显示部门编号，其次显示部门名称
        String departmentText = "";
        if (item.getDepartmentNumber() != null) {
            departmentText = "科室: " + item.getDepartmentNumber();
        } else if (item.getDepartment() != null && !item.getDepartment().isEmpty()) {
            departmentText = item.getDepartment();
        } else {
            departmentText = "未分配科室";
        }
        terminalKs.setText(departmentText);
        
        // 设置位置信息，包含台车和台架信息
        String locationText = "";
        if (item.getCartNumber() != null && item.getRackNumber() != null) {
            locationText = "台车" + item.getCartNumber() + "-台架" + item.getRackNumber();
        } else if (item.getLocation() != null && !item.getLocation().isEmpty()) {
            locationText = item.getLocation();
        } else {
            locationText = "位置未知";
        }
        terminalBf.setText(locationText);

        // 设置状态信息
        setupStatusDisplay(holder, item);

        // 设置电池信息
        setupBatteryDisplay(holder, item);
    }

    /**
     * 设置状态显示
     */
    private void setupStatusDisplay(QuickViewHolder holder, Terminal item) {
        ImageView ivStatusIcon = holder.getView(R.id.iv_status_icon);
        TextView tvStatusTitle = holder.getView(R.id.tv_status_title);
        
        // 根据设备状态设置图标和文本
        String statusText = "";
        int statusIconRes = R.mipmap.ic_xh_no; // 使用现有的图标作为默认未知状态图标
        
        if (item.getDeviceStatus() != null) {
            switch (item.getDeviceStatus().intValue()) {
                case 0:
                    statusText = "正常";
                    statusIconRes = R.mipmap.ic_xh;
                    break;
                case 1:
                    statusText = "异常";
                    statusIconRes = R.mipmap.ic_alarm;
                    break;
                case 2:
                    statusText = "离线";
                    statusIconRes = R.mipmap.ic_xh_no;
                    break;
                default:
                    statusText = "未知";
                    break;
            }
        } else if (item.getStatus() != null && !item.getStatus().isEmpty()) {
            statusText = item.getStatus();
            // 根据状态文本设置图标
            if ("在线".equals(item.getStatus()) || "正常".equals(item.getStatus())) {
                statusIconRes = R.mipmap.ic_xh;
            } else if ("离线".equals(item.getStatus())) {
                statusIconRes = R.mipmap.ic_xh_no;
            } else if ("异常".equals(item.getStatus())) {
                statusIconRes = R.mipmap.ic_alarm;
            }
        } else {
            statusText = "未知";
        }
        
        // 添加信号强度信息
        if (item.getRssi() != null) {
            statusText += " (" + item.getRssi() + "dBm)";
        } else if (item.getSignalStrength() != 0) {
            statusText += " (" + item.getSignalStrength() + "dBm)";
        }
        
        // 如果有自定义状态图标和文本，优先使用
        if (item.getStatusIconResId() != 0) {
            statusIconRes = item.getStatusIconResId();
        }
        if (item.getStatusText() != null && !item.getStatusText().isEmpty()) {
            statusText = item.getStatusText();
        }
        
        ivStatusIcon.setImageResource(statusIconRes);
        tvStatusTitle.setText(statusText);
    }

    /**
     * 设置电池显示
     */
    private void setupBatteryDisplay(QuickViewHolder holder, Terminal item) {
        ImageView ivBatteryIcon = holder.getView(R.id.iv_battery_icon);
        TextView tvBatteryTitle = holder.getView(R.id.tv_battery_title);
        
        String batteryText = "";
        int batteryIconRes = R.mipmap.ic_baterery_low; // 使用现有的电池图标
        
        // 优先使用电池电量百分比
        if (item.getBatteryLevel() != null) {
            int level = item.getBatteryLevel();
            batteryText = level + "%";
            
            // 根据电量设置电池图标（使用现有图标）
            if (level >= 20) {
                batteryIconRes = R.mipmap.ic_ds; // 使用现有图标表示正常电量
            } else {
                batteryIconRes = R.mipmap.ic_baterery_low; // 低电量图标
            }
        } 
        // 其次使用电池电压
        else if (item.getBatteryVoltage() != null) {
            float voltage = item.getBatteryVoltage() / 1000.0f; // 假设电压以毫伏为单位
            batteryText = String.format("%.2fV", voltage);
            
            // 根据电压估算电量
            if (voltage >= 3.4f) {
                batteryIconRes = R.mipmap.ic_ds; // 正常电压
            } else {
                batteryIconRes = R.mipmap.ic_baterery_low; // 低电压
            }
        } else {
            batteryText = "未知";
            batteryIconRes = R.mipmap.ic_ds; // 默认图标
        }
        
        // 如果有自定义电池图标和文本，优先使用
        if (item.getBatteryIconResId() != 0) {
            batteryIconRes = item.getBatteryIconResId();
        }
        if (item.getBatteryText() != null && !item.getBatteryText().isEmpty()) {
            batteryText = item.getBatteryText();
        }
        
        ivBatteryIcon.setImageResource(batteryIconRes);
        tvBatteryTitle.setText(batteryText);
    }

    /**
     * 获取显示文本，按优先级返回非空值
     */
    private String getDisplayText(String... texts) {
        for (String text : texts) {
            if (text != null && !text.trim().isEmpty()) {
                return text;
            }
        }
        return "未知";
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
        return new QuickViewHolder(R.layout.item_terminal, viewGroup);
    }

    /**
     * 更新终端列表数据
     */
    public void updateTerminals(List<Terminal> terminals) {
        submitList(terminals);
    }

}