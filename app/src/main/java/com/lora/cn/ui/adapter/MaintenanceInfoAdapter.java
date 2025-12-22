package com.lora.cn.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.ui.model.MaintenanceInfo;

public class MaintenanceInfoAdapter extends BaseQuickAdapter<MaintenanceInfo, QuickViewHolder> {
    public interface OnConfirmClickListener { void onConfirmClick(MaintenanceInfo item); }
    public interface OnViewClickListener { void onViewClick(MaintenanceInfo item); }
    public interface OnEditClickListener { void onEditClick(MaintenanceInfo item); }
    public interface OnDeleteClickListener { void onDeleteClick(MaintenanceInfo item); }
    private OnConfirmClickListener onConfirmClickListener;
    private OnViewClickListener onViewClickListener;
    private OnEditClickListener onEditClickListener;
    private OnDeleteClickListener onDeleteClickListener;

    public void setOnConfirmClickListener(OnConfirmClickListener l) { this.onConfirmClickListener = l; }
    public void setOnViewClickListener(OnViewClickListener l) { this.onViewClickListener = l; }
    public void setOnEditClickListener(OnEditClickListener l) { this.onEditClickListener = l; }
    public void setOnDeleteClickListener(OnDeleteClickListener l) { this.onDeleteClickListener = l; }

    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int i, @Nullable MaintenanceInfo item) {
        TextView logTime = holder.getView(R.id.log_time);
        TextView logStatus = holder.getView(R.id.log_statu);
        TextView logName = holder.getView(R.id.log_name);
        TextView logId = holder.getView(R.id.log_id);
        TextView logContent = holder.getView(R.id.log_complute);
        TextView logHandleTime = holder.getView(R.id.log_complute_time);
        TextView logOperation = holder.getView(R.id.log_operation);
        View layoutOps = holder.getView(R.id.layout_maintenance_ops);
        TextView btnDelete = holder.getView(R.id.btn_maintenance_delete);
        TextView btnEdit = holder.getView(R.id.btn_maintenance_edit);

        setTextOrDash(logTime, item.getCreateTime());

        boolean done = item.getStatus() == 1;
        setStatusWithDot(logStatus, done ? "已维护" : "待维护", done);

        String name = item.getTerminalName();
        String group = item.getTerminalGroup();
        String displayName;
        if (!TextUtils.isEmpty(group)) {
            displayName = "终端：" + (name == null ? "" : name) + "（" + group + "）";
        } else {
            displayName = "终端：" + (name == null ? "" : name);
        }
        setTextOrDash(logName, displayName);

        String tid = item.getTerminalId();
        setTextOrDash(logId, "终端ID：" + (tid == null ? "" : tid));

        setTextOrDash(logContent, item.getContent());

        if (done) {
            setTextOrDash(logHandleTime, item.getHandleTime());
        } else {
            logHandleTime.setText("");
        }

        if (layoutOps != null) layoutOps.setVisibility(View.VISIBLE);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                if (onDeleteClickListener != null) onDeleteClickListener.onDeleteClick(item);
            });
        }
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                if (onEditClickListener != null) onEditClickListener.onEditClick(item);
            });
        }

        if (done) {
            logOperation.setVisibility(View.VISIBLE);
            logOperation.setText("查看");
            logOperation.setEnabled(true);
            logOperation.setBackgroundResource(R.drawable.bg_btn_voice);
            logOperation.setTextColor(Color.parseColor("#383B40"));
            logOperation.setOnClickListener(v -> {
                if (onViewClickListener != null) onViewClickListener.onViewClick(item);
            });
        } else {
            logOperation.setVisibility(View.VISIBLE);
            logOperation.setText("确认维护");
            logOperation.setEnabled(true);
            logOperation.setBackgroundResource(R.drawable.bg_btn_now);
            logOperation.setTextColor(Color.WHITE);
            logOperation.setOnClickListener(v -> {
                if (onConfirmClickListener != null) onConfirmClickListener.onConfirmClick(item);
            });
        }
    }

    private void setTextOrDash(TextView textView, String text) {
        if (TextUtils.isEmpty(text)) textView.setText("-"); else textView.setText(text);
        textView.setBackground(null);
    }

    private void setStatusWithDot(TextView textView, String status, boolean done) {
        textView.setText(status == null ? "" : status);
        textView.setBackground(null);
        Drawable dotDrawable;
        int textColor;
        if (done) {
            dotDrawable = ContextCompat.getDrawable(textView.getContext(), R.drawable.dot_green);
            textColor = Color.parseColor("#00C851");
        } else {
            dotDrawable = ContextCompat.getDrawable(textView.getContext(), R.drawable.dot_red);
            textColor = Color.parseColor("#D30000");
        }
        textView.setTextColor(textColor);
        if (dotDrawable != null) {
            dotDrawable.setBounds(0, 0, dotDrawable.getIntrinsicWidth(), dotDrawable.getIntrinsicHeight());
            textView.setCompoundDrawables(dotDrawable, null, null, null);
            textView.setCompoundDrawablePadding(8);
        } else {
            textView.setCompoundDrawables(null, null, null, null);
        }
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull android.content.Context context, @NonNull ViewGroup viewGroup, int i) {
        return new QuickViewHolder(R.layout.item_terminal_log, viewGroup);
    }
}

