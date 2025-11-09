package com.lora.cn.ui.adapter;

import android.content.Context;
import android.view.View;
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
        ImageView ivStatusIcon = holder.getView(R.id.iv_status_icon);
        TextView tvStatusTitle = holder.getView(R.id.tv_status_title);
        ImageView ivBatteryIcon = holder.getView(R.id.iv_battery_icon);
        TextView tvBatteryTitle = holder.getView(R.id.tv_battery_title);
        ImageView terminalColl = holder.getView(R.id.terminal_coll);

        // 设置终端基本信息
        terminalTitle.setText(item.getName());
        terminalKs.setText(item.getDepartment());
        terminalBf.setText(item.getLocation());

        // 设置状态信息
        ivStatusIcon.setImageResource(item.getStatusIconResId());
        tvStatusTitle.setText(item.getStatusText());

        // 设置电池信息（显示电量）
        // 显示电量图标（由列表转换逻辑根据电量选择不同资源）
        ivBatteryIcon.setImageResource(item.getBatteryIconResId());
        tvBatteryTitle.setText(item.getBatteryText());
        
        // 设置收藏状态
        terminalColl.setVisibility(View.GONE);
        if (item.isFavorite()) {
            terminalColl.setVisibility(View.VISIBLE);
            terminalColl.setImageResource(R.mipmap.ic_coll); // 已收藏图标
        }
//        else {
//            terminalColl.setImageResource(R.mipmap.ic_start); // 未收藏图标
//        }
//
        // 设置收藏点击事件
        terminalColl.setOnClickListener(v -> {
            if (onFavoriteClickListener != null) {
                boolean newFavoriteState = !item.isFavorite();
                item.setFavorite(newFavoriteState);
                item.setImportant(newFavoriteState);
                
                // 更新图标
                terminalColl.setVisibility(View.GONE);
                if (newFavoriteState) {
                    terminalColl.setVisibility(View.VISIBLE);
                    terminalColl.setImageResource(R.mipmap.ic_coll);
                } else {
                    terminalColl.setImageResource(R.mipmap.ic_start);
                }
                
                onFavoriteClickListener.onFavoriteClick(item, newFavoriteState);
            }
        });
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
        return new QuickViewHolder(R.layout.item_terminal, viewGroup);
    }

}