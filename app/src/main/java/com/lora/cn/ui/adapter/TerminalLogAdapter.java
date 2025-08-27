package com.lora.cn.ui.adapter;

import android.content.Context;
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

        // 设置状态字段
        setTextOrPlaceholder(logStatus, item.getLogStatus());

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


}