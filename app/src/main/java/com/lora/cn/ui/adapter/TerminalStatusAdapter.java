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
import com.lora.cn.ui.model.TerminalStatus;

public class TerminalStatusAdapter extends BaseQuickAdapter<TerminalStatus, QuickViewHolder> {

    private final java.util.WeakHashMap<View, android.animation.ObjectAnimator> animators = new java.util.WeakHashMap<>();

    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int i, @Nullable TerminalStatus item) {
        ImageView ivStatusIcon = holder.getView(R.id.iv_status_icon);
        TextView tvStatusTitle = holder.getView(R.id.tv_status_title);
        TextView tvStatusCount = holder.getView(R.id.tv_status_count);

        // 设置图标
        ivStatusIcon.setImageResource(item.getIconResId());

        // 设置标题
        tvStatusTitle.setText(item.getTitle());

        // 设置数量
        tvStatusCount.setText(String.valueOf(item.getCount()));

        // 设置选中状态
        holder.itemView.setSelected(item.isSelected());

        // 设置选中状态的视觉效果
        if (item.isSelected()) {
            tvStatusTitle.setTextColor(0xFF2196F3);
            tvStatusCount.setTextColor(0xFF2196F3);
        } else {
            tvStatusTitle.setTextColor(0xFF333333);
            tvStatusCount.setTextColor(0xFF666666);
        }

        // 闪烁逻辑
        stopFlashing(ivStatusIcon);
        stopFlashing(tvStatusTitle);
        stopFlashing(tvStatusCount);
        if (item.getCount() > 0 && isAlertStatus(item.getTitle())) {
            startFlashing(ivStatusIcon);
            startFlashing(tvStatusTitle);
            startFlashing(tvStatusCount);
        }
    }

    private boolean isAlertStatus(String title) {
        return "异常取走".equals(title) || "设备低电量".equals(title) || "设备离线".equals(title);
    }

    private void startFlashing(View view) {
        android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        animator.setDuration(800);
        animator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        animator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        animator.start();
        animators.put(view, animator);
    }

    private void stopFlashing(View view) {
        android.animation.ObjectAnimator animator = animators.remove(view);
        if (animator != null) {
            animator.cancel();
        }
        view.setAlpha(1f);
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
        return new QuickViewHolder(R.layout.item_terminal_status, viewGroup);
    }
}
