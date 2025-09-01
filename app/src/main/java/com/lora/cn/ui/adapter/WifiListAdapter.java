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
import com.lora.cn.ui.model.WifiItem;

public class WifiListAdapter extends BaseQuickAdapter<WifiItem, QuickViewHolder> {
    
    public interface OnItemClickListener {
        void onItemClick(WifiItem item, int position);
    }
    
    private OnItemClickListener onItemClickListener;
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int position, @Nullable WifiItem item) {
        if (item == null) return;
        
        TextView tvSsid = holder.getView(R.id.tv_wifi_ssid);
        TextView tvSignal = holder.getView(R.id.tv_wifi_signal);
        ImageView ivSecurity = holder.getView(R.id.iv_wifi_security);
        ImageView ivSignal = holder.getView(R.id.iv_wifi_signal);
        
        tvSsid.setText(item.getSsid());
        tvSignal.setText(item.getSignalStrengthText());
        
        // 设置安全图标
        if (item.isSecured()) {
            ivSecurity.setVisibility(View.VISIBLE);
            ivSecurity.setImageResource(R.mipmap.ic_lock);
        } else {
            ivSecurity.setVisibility(View.GONE);
        }
        
        // 设置信号强度图标
//        if (item.getSignalLevel() > -50) {
//            ivSignal.setImageResource(R.mipmap.ic_wifi_strong);
//        } else if (item.getSignalLevel() > -70) {
//            ivSignal.setImageResource(R.mipmap.ic_wifi_medium);
//        } else {
//            ivSignal.setImageResource(R.mipmap.ic_wifi_weak);
//        }
        
        // 设置连接状态
        if (item.isConnected()) {
            tvSsid.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.teal_200));
        } else {
            tvSsid.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.black));
        }
//
        // 设置点击事件
//        holder.itemView.setOnClickListener(v -> {
//            if (onItemClickListener != null) {
//                onItemClickListener.onItemClick(item, position);
//            }
//        });
    }
    
    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int viewType) {
        return new QuickViewHolder(R.layout.item_wifi_list, viewGroup);
    }
}