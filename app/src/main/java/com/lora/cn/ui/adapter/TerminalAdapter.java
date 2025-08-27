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

        // 设置终端基本信息
        terminalTitle.setText(item.getName());
        terminalKs.setText(item.getDepartment());
        terminalBf.setText(item.getLocation());

        // 设置状态信息
        ivStatusIcon.setImageResource(item.getStatusIconResId());
        tvStatusTitle.setText(item.getStatusText());

        // 设置电池信息
        ivBatteryIcon.setImageResource(item.getBatteryIconResId());
        tvBatteryTitle.setText(item.getBatteryText());

    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
        return new QuickViewHolder(R.layout.item_terminal, viewGroup);
    }

}