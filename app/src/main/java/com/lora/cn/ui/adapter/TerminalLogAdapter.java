package com.lora.cn.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.ui.model.Terminal;
import com.lora.cn.ui.model.TerminalLog;

public class TerminalLogAdapter extends BaseQuickAdapter<TerminalLog, QuickViewHolder> {


    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int i, @Nullable TerminalLog item) {
        TextView logTime = holder.getView(R.id.log_time);
        TextView logStatus = holder.getView(R.id.log_statu);
        TextView logName = holder.getView(R.id.log_name);
        TextView logId = holder.getView(R.id.log_id);
        TextView logComplete = holder.getView(R.id.log_complute);
        TextView logCompleteTime = holder.getView(R.id.log_complute_time);
        TextView logOperation = holder.getView(R.id.log_operation);

        // 设置时间字段
        setTextOrPlaceholder(logTime, item.getLogTime());

        // 设置状态字段，包含特殊状态的圆点和颜色
        setStatusWithDot(logStatus, item.getLogStatus());

        // 设置名称字段
        setTextOrPlaceholder(logName, item.getLogName());

        // 设置ID字段
        setTextOrPlaceholder(logId, item.getLogId());

        // 设置完成字段
        setTextOrPlaceholder(logComplete, item.getLogComplete());

        // 设置完成时间字段
        setTextOrPlaceholder(logCompleteTime, item.getLogCompleteTime());

        // 设置操作字段
        setTextOrPlaceholder(logOperation, item.getLogOperation());
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
        return new QuickViewHolder(R.layout.item_terminal_log, viewGroup);
    }


    /**
     * 设置文本或占位符
     * 如果文本为空，显示占位符背景；否则显示文本内容
     */
    private void setTextOrPlaceholder(TextView textView, String text) {
        if (TextUtils.isEmpty(text)) {
            textView.setText("");
            textView.setBackground(ContextCompat.getDrawable(textView.getContext(), R.drawable.placeholder_empty));
        } else {
            textView.setText(text);
            textView.setBackground(null);
        }
    }

    /**
     * 设置状态字段，包含特殊状态的圆点和颜色
     */
    private void setStatusWithDot(TextView textView, String status) {
        if (TextUtils.isEmpty(status)) {
            textView.setText("");
            textView.setBackground(ContextCompat.getDrawable(textView.getContext(), R.drawable.placeholder_empty));
            textView.setCompoundDrawables(null, null, null, null);
            return;
        }

        textView.setText(status);
        textView.setBackground(null);
        
        // 根据状态设置圆点和文字颜色
        Drawable dotDrawable = null;
        int textColor = Color.parseColor("#666666"); // 默认颜色
        
        if ("设备丢失".equals(status) || "异常丢失".equals(status)) {
            dotDrawable = ContextCompat.getDrawable(textView.getContext(), R.drawable.dot_red);
            textColor = Color.parseColor("#D30000");
        } else if ("低电量报警".equals(status) || "低电量".equals(status)) {
            dotDrawable = ContextCompat.getDrawable(textView.getContext(), R.drawable.dot_orange);
            textColor = Color.parseColor("#FF9F0F");
        }
        
        // 设置文字颜色
        textView.setTextColor(textColor);
        
        // 设置左侧圆点
        if (dotDrawable != null) {
            dotDrawable.setBounds(0, 0, dotDrawable.getIntrinsicWidth(), dotDrawable.getIntrinsicHeight());
            textView.setCompoundDrawables(dotDrawable, null, null, null);
            textView.setCompoundDrawablePadding(8); // 设置圆点与文字的间距
        } else {
            textView.setCompoundDrawables(null, null, null, null);
        }
    }


}