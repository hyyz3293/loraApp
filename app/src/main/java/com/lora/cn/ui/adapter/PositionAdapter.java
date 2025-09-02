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
        
        // 为子视图添加点击监听器支持
        holder.addOnClickListener(R.id.tv_position_fz);
        holder.addOnClickListener(R.id.tv_position_edit);
        holder.addOnClickListener(R.id.tv_position_delete);
        holder.addOnClickListener(R.id.switch_position_status);
    }
    
    /**
     * 添加职位
     */
    public void addPosition(Position position) {
        if (position != null) {
            add(position);
        }
    }
    
    /**
     * 更新职位
     */
    public void updatePosition(Position updatedPosition) {
        if (updatedPosition != null) {
            for (int i = 0; i < getItemCount(); i++) {
                Position position = getItem(i);
                if (position != null && position.getPositionId() == updatedPosition.getPositionId()) {
                    set(i, updatedPosition);
                    break;
                }
            }
        }
    }
    
    /**
     * 删除职位
     */
    public void removePosition(long positionId) {
        for (int i = 0; i < getItemCount(); i++) {
            Position position = getItem(i);
            if (position != null && position.getPositionId() == positionId) {
                removeAt(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
        return new QuickViewHolder(R.layout.item_position, viewGroup);
    }
}