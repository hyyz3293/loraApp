package com.lora.cn.ui.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.ui.model.MenuTab;

public class MenuTabAdapter extends BaseQuickAdapter<MenuTab, QuickViewHolder> {

    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int i, @Nullable MenuTab menuTab) {
        TextView tvTitle = holder.getView(R.id.tv_tab_title);
        ImageView imgTitle = holder.getView(R.id.img_tab_title);
        tvTitle.setText(menuTab.getTitle());
        imgTitle.setVisibility(View.INVISIBLE);
        tvTitle.setTextColor(holder.itemView.getResources().getColor(R.color.tv_gray));
        tvTitle.setTypeface(null, Typeface.NORMAL);
        if (menuTab.isSelected()) {
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setTextColor(holder.itemView.getResources().getColor(R.color.black));
            imgTitle.setVisibility(View.VISIBLE);
        }
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
        return new QuickViewHolder(R.layout.item_menu_tab, viewGroup);
    }
}