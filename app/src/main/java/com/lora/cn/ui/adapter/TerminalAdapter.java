package com.lora.cn.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blankj.utilcode.util.LogUtils;
import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.ui.model.Terminal;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.Category;
import com.lora.cn.ui.view.SignalStrengthView;

public class TerminalAdapter extends BaseQuickAdapter<Terminal, QuickViewHolder> {

    private OnFavoriteClickListener onFavoriteClickListener;
    
    public interface OnFavoriteClickListener {
        void onFavoriteClick(Terminal terminal, boolean isFavorite);
    }
    
    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.onFavoriteClickListener = listener;
    }

    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int i, @Nullable Terminal item) {
        TextView terminalTitle = holder.getView(R.id.terminal_title);
        TextView terminalKs = holder.getView(R.id.terminal_ks);
        TextView terminalBf = holder.getView(R.id.terminal_bf);
        SignalStrengthView signalView = holder.getView(R.id.signal_view);
        ImageView ivStatusIcon = holder.getView(R.id.iv_status_icon);
        TextView tvStatusTitle = holder.getView(R.id.tv_status_title);
        com.lora.cn.ui.view.BatteryView batteryView = holder.getView(R.id.battery_view);
        ImageView ivBatteryIcon = holder.getView(R.id.iv_battery_icon);
        TextView tvBatteryTitle = holder.getView(R.id.tv_battery_title);
        ImageView terminalColl = holder.getView(R.id.terminal_coll);

        // 设置终端基本信息
        terminalTitle.setText(item.getName());
        String dept = item.getDepartment();
        String room = item.getLocation();
        String ngName = null;
        String otherName = null;
        try {
            DatabaseManager dm = DatabaseManager.getInstance(holder.itemView.getContext());
            if (item.getNursingGroupId() > 0) {
                Category c = dm.getCategoryById(item.getNursingGroupId());
                if (c != null) ngName = c.getCategoryName();
            }
            if (item.getOtherId() > 0) {
                Category c2 = dm.getCategoryById(item.getOtherId());
                if (c2 != null) otherName = c2.getCategoryName();
            }
        } catch (Exception ignored) {}
        String line1 = "";
        if (dept != null && !dept.isEmpty()) line1 += "科室-" + dept;
        if (ngName != null && !ngName.isEmpty()) {
            if (!line1.isEmpty()) line1 += "  ";
            line1 += "护理组-" + ngName;
        }
        String line2 = "";
        if (room != null && !room.isEmpty()) line2 += "病房-" + room;
        if (otherName != null && !otherName.isEmpty()) {
            if (!line2.isEmpty()) line2 += "  ";
            line2 += "其他-" + otherName;
        }
        terminalKs.setText(line1);
        terminalBf.setText(line2);

        // 信号强度使用SignalStrengthView，按-138~0对应138~0反向计算百分比
        boolean isOffline = com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE == item.getStatus();
        boolean isAbnormal = com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ABNORMAL_TAKEN == item.getStatus();
        boolean isNormalTaken = com.lora.cn.ui.constants.TerminalStatusConstants.CODE_NORMAL_TAKEN == item.getStatus();
        boolean isOnline = com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ONLINE == item.getStatus();
        if (isOffline) {
            if (signalView != null) signalView.setVisibility(View.GONE);
            if (ivStatusIcon != null) {
                ivStatusIcon.setVisibility(View.VISIBLE);
                ivStatusIcon.setImageResource(R.mipmap.ic_xh_no);
            }
            if (batteryView != null) batteryView.setVisibility(View.GONE);
            if (ivBatteryIcon != null) ivBatteryIcon.setVisibility(View.GONE);
            tvStatusTitle.setText(com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_OFFLINE);
            tvBatteryTitle.setText("");
        } else {
            if (isAbnormal) {
                if (signalView != null) signalView.setVisibility(View.GONE);
                if (ivStatusIcon != null) {
                    ivStatusIcon.setVisibility(View.VISIBLE);
                    ivStatusIcon.setImageResource(R.mipmap.ic_ds);
                }
                tvStatusTitle.setText(com.lora.cn.ui.constants.TerminalStatusConstants.STATUS_ABNORMAL_LOST);
            } else if (isOnline) {
                int rssiRaw = Math.max(0, Math.min(138, item.getRssi()));
                float percent = (rssiRaw) * 100f / 138f;
                int bars = Math.max(0, Math.min(4, Math.round(percent * 4f / 100f)));
                if (signalView != null) {
                    LogUtils.e("setSignalStrength===bars=" + bars+ "+=======" + item.getRssi() + "====percent=" + percent);

                    signalView.setVisibility(View.VISIBLE);
                    signalView.setSignalStrength(bars);
                }
                if (ivStatusIcon != null) ivStatusIcon.setVisibility(View.GONE);
                tvStatusTitle.setText("正常在线");
            } else if (isNormalTaken) {
                if (signalView != null) signalView.setVisibility(View.GONE);
                if (ivStatusIcon != null) {
                    ivStatusIcon.setVisibility(View.VISIBLE);
                    ivStatusIcon.setImageResource(R.mipmap.ic_blue_right);
                }
                tvStatusTitle.setText("正常取走");
            } else {
                int rssiRaw = Math.max(0, Math.min(138, item.getRssi()));
                float percent = (138 - rssiRaw) * 100f / 138f;
                int bars = Math.max(0, Math.min(4, Math.round(percent * 4f / 100f)));
                LogUtils.e("setSignalStrength===bars=" + bars+ "+=======" + item.getRssi() + "====percent=" + percent);
                if (signalView != null) {
                    signalView.setVisibility(View.VISIBLE);
                    signalView.setSignalStrength(bars);
                }
                if (ivStatusIcon != null) ivStatusIcon.setVisibility(View.GONE);
                tvStatusTitle.setText("正常在线");
            }
        }

        // 电量使用BatteryView，背景透明、边框+四方格显示
        if (!isOffline) {
            int level = Math.max(0, Math.min(100, item.getBatteryLevel()));
            boolean isLow = level <= 20;
            if (isLow) {
                if (batteryView != null) batteryView.setVisibility(View.GONE);
                if (ivBatteryIcon != null) {
                    ivBatteryIcon.setVisibility(View.VISIBLE);
                    ivBatteryIcon.setImageResource(R.mipmap.ic_baterery_low);
                }
            } else {
                if (ivBatteryIcon != null) ivBatteryIcon.setVisibility(View.GONE);
                if (batteryView != null) {
                    batteryView.setVisibility(View.VISIBLE);
                    batteryView.setBatteryLevel(level);
                }
            }
            tvBatteryTitle.setText(level + "%");
        }
        
        // 设置收藏状态
        terminalColl.setVisibility(View.GONE);
        if (item.isFavorite()) {
            terminalColl.setVisibility(View.VISIBLE);
            terminalColl.setImageResource(R.mipmap.ic_coll); // 已收藏图标
        }
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
        return new QuickViewHolder(R.layout.item_terminal, viewGroup);
    }

}
