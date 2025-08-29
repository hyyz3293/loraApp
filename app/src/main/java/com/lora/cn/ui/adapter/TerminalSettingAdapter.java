package com.lora.cn.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.ui.model.SettingItem;

import org.jetbrains.annotations.NotNull;

public class TerminalSettingAdapter extends BaseQuickAdapter<SettingItem, QuickViewHolder> {




    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int i, @Nullable SettingItem item) {
        ImageView settingImg = holder.getView(R.id.terminal_setting_img);
        TextView settingText = holder.getView(R.id.terminal_setting_text);

        settingImg.setImageResource(item.getIconResId());
        settingText.setText(item.getTitle());
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
        return new QuickViewHolder(R.layout.item_terminal_setting, viewGroup);
    }
}