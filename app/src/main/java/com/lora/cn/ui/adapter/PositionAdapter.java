package com.lora.cn.ui.adapter;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;


import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.database.entity.Position;

import java.util.List;

/**
 * 职位列表适配器
 */
public class PositionAdapter extends BaseQuickAdapter<Position, QuickViewHolder> {

    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int position, @Nullable Position positionItem) {
        if (positionItem == null) return;
        
        // 设置职位名称
        holder.setText(R.id.tv_position_name, positionItem.getPositionName());
        
        // 设置排序
        holder.setText(R.id.tv_position_sort, String.valueOf(positionItem.getSortOrder()));
        
        // 设置状态开关
        SwitchCompat switchStatus = holder.getView(R.id.switch_position_status);
        switchStatus.setChecked(positionItem.getStatus() == 1);
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
        return new QuickViewHolder(R.layout.item_position, viewGroup);
    }
}