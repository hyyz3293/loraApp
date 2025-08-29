package com.lora.cn.ui.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.ui.model.ChartItem;

public class ChartItemAdapter extends BaseQuickAdapter<ChartItem, QuickViewHolder> {

    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int position, @Nullable ChartItem item) {
        ImageView img = holder.getView(R.id.img);
        TextView chartKey = holder.getView(R.id.chart_key);
        TextView chartValue = holder.getView(R.id.chart_value);
        
        // 设置圆形颜色图标
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(item.getColor());
        img.setBackground(drawable);
        
        // 设置键名和值
        chartKey.setText(item.getKey());
        chartValue.setText(item.getValue());
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup parent, int viewType) {
        return new QuickViewHolder(R.layout.item_terminal_chart_child, parent);
    }
}