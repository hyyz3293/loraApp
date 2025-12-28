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
        ImageView ivMaintenance = holder.getView(R.id.iv_maintenance);

        // 设置终端基本信息
        String title = item.getName();
        if (title == null || title.isEmpty()) title = item.getTerminalName();
        if (title == null || title.isEmpty()) title = item.getTerminalId();
        terminalTitle.setText(title);
        java.util.List<String> allTokens = new java.util.ArrayList<>();
        try {
            DatabaseManager dm = DatabaseManager.getInstance(holder.itemView.getContext());
            java.util.Set<String> seen = new java.util.HashSet<>();
            java.util.function.Consumer<Long> addByCategoryId = cid -> {
                if (cid == null || cid <= 0) return;
                Category c = dm.getCategoryById(cid);
                if (c == null) return;
                long gid = c.getGroupId();
                String key = gid + ":" + cid;
                if (seen.contains(key)) return;
                com.lora.cn.database.entity.Group g = dm.getGroupById(gid);
                String gname = g != null ? g.getGroupName() : String.valueOf(gid);
                String cname = c.getCategoryName();
                allTokens.add(gname + "-" + cname);
                seen.add(key);
            };
            addByCategoryId.accept(item.getDepartmentId());
            addByCategoryId.accept(item.getRoomId());
            addByCategoryId.accept(item.getNursingGroupId());
            addByCategoryId.accept(item.getOtherId());
//            String ids = item.getGroupIdsText();
//            if (ids != null && !ids.isEmpty()) {
//                String[] toks = ids.split(",");
//                for (String tk : toks) {
//                    if (tk == null || tk.trim().isEmpty()) continue;
//                    String[] pr = tk.trim().split(":");
//                    if (pr.length == 2) {
//                        long gid = 0L, cid = 0L;
//                        try { gid = Long.parseLong(pr[0]); cid = Long.parseLong(pr[1]); } catch (Exception ignored) {}
//                        String key = gid + ":" + cid;
//                        if (cid > 0 && !seen.contains(key)) {
//                            com.lora.cn.database.entity.Group g = dm.getGroupById(gid);
//                            Category c = dm.getCategoryById(cid);
//                            String gname = g != null ? g.getGroupName() : pr[0];
//                            String cname = c != null ? c.getCategoryName() : pr[1];
//                            allTokens.add(gname + "-" + cname);
//                            seen.add(key);
//                        }
//                    }
//                }
//            }
            if (allTokens.isEmpty()) {
                String names = item.getGroupNamesText();
                if (names != null && !names.isEmpty()) {
                    String[] toks = names.split(",");
                    for (String tk : toks) {
                        if (tk == null || tk.trim().isEmpty()) continue;
                        allTokens.add(tk.trim());
                    }
                }
            }
        } catch (Exception ignored) {}
        StringBuilder l1 = new StringBuilder();
        StringBuilder l2 = new StringBuilder();
        for (int idx = 0; idx < allTokens.size(); idx++) {
            String tk = allTokens.get(idx);
            if (idx % 2 == 0) { if (l1.length() > 0) l1.append("  "); l1.append(tk); }
            else { if (l2.length() > 0) l2.append("  "); l2.append(tk); }
        }
        terminalKs.setSingleLine(false); terminalKs.setEllipsize(null); terminalKs.setMaxLines(10);
        terminalBf.setSingleLine(false); terminalBf.setEllipsize(null); terminalBf.setMaxLines(10);
        terminalKs.setText(l1.toString());
        terminalBf.setText(l2.toString());

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
                int bars;
                if (rssiRaw <= 65) bars = 4;
                else if (rssiRaw <= 75) bars = 3;
                else if (rssiRaw <= 85) bars = 2;
                else if (rssiRaw <= 95) bars = 1;
                else bars = 0;
                if (signalView != null) signalView.setVisibility(View.GONE);
                if (ivStatusIcon != null) {
                    ivStatusIcon.setVisibility(View.VISIBLE);
                    int iconRes;
                    switch (bars) {
                        case 4: iconRes = R.drawable.ic_xh_signal_4; break;
                        case 3: iconRes = R.drawable.ic_xh_signal_3; break;
                        case 2: iconRes = R.drawable.ic_xh_signal_2; break;
                        case 1: iconRes = R.drawable.ic_xh_signal_1; break;
                        default: iconRes = R.drawable.ic_xh_signal_0; break;
                    }
                    ivStatusIcon.setImageResource(iconRes);
                }
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
                int bars;
                if (rssiRaw <= 65) bars = 4;
                else if (rssiRaw <= 75) bars = 3;
                else if (rssiRaw <= 85) bars = 2;
                else if (rssiRaw <= 95) bars = 1;
                else bars = 0;
                if (signalView != null) signalView.setVisibility(View.GONE);
                if (ivStatusIcon != null) {
                    ivStatusIcon.setVisibility(View.VISIBLE);
                    int iconRes;
                    switch (bars) {
                        case 4: iconRes = R.drawable.ic_xh_signal_4; break;
                        case 3: iconRes = R.drawable.ic_xh_signal_3; break;
                        case 2: iconRes = R.drawable.ic_xh_signal_2; break;
                        case 1: iconRes = R.drawable.ic_xh_signal_1; break;
                        default: iconRes = R.drawable.ic_xh_signal_0; break;
                    }
                    ivStatusIcon.setImageResource(iconRes);
                }
                tvStatusTitle.setText("正常在线");
            }
        }

        // 电量使用BatteryView，背景透明、边框+四方格显示
        if (!isOffline) {
            int level = Math.max(0, Math.min(100, item.getBatteryLevel()));
            int lowTh = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
            boolean isLow = level <= lowTh;
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
        
        // 设置维修状态
        if (item.isMaintenanceActive()) {
            if (ivMaintenance != null) {
                ivMaintenance.setVisibility(View.VISIBLE);
                android.view.animation.AlphaAnimation anim = new android.view.animation.AlphaAnimation(1.0f, 0.2f);
                anim.setDuration(800);
                anim.setRepeatCount(android.view.animation.Animation.INFINITE);
                anim.setRepeatMode(android.view.animation.Animation.REVERSE);
                ivMaintenance.startAnimation(anim);
            }
        } else {
            if (ivMaintenance != null) {
                ivMaintenance.setVisibility(View.GONE);
                ivMaintenance.clearAnimation();
            }
        }
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
        return new QuickViewHolder(R.layout.item_terminal, viewGroup);
    }

}
