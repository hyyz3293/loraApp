package com.lora.cn.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.ui.model.LogInfo;

public class LogDetailInfoAdapter extends BaseQuickAdapter<LogInfo, QuickViewHolder> {
    public interface OnHandleClickListener { void onHandleClick(LogInfo item); }
    private OnHandleClickListener onHandleClickListener;
    public void setOnHandleClickListener(OnHandleClickListener l) { this.onHandleClickListener = l; }
    private java.util.Set<Long> allowedHandleIds = new java.util.HashSet<>();
    public void setAllowedHandleIds(java.util.Set<Long> ids) { this.allowedHandleIds = ids != null ? ids : new java.util.HashSet<>(); }
    private java.util.Map<Long, String> handledSourceLabels = new java.util.HashMap<>();
    public void setHandledSourceLabels(java.util.Map<Long, String> map) { this.handledSourceLabels = map != null ? map : new java.util.HashMap<>(); }

    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int i, @Nullable LogInfo item) {
        TextView logTime = holder.getView(R.id.log_time);
        TextView logStatus = holder.getView(R.id.log_statu);
        TextView logName = holder.getView(R.id.log_name);
        TextView logId = holder.getView(R.id.log_id);
        TextView logComplete = holder.getView(R.id.log_complute);
        TextView logCompleteTime = holder.getView(R.id.log_complute_time);
        TextView logOperation = holder.getView(R.id.log_operation);

        // 设置时间字段 - 使用创建时间
        setTextOrPlaceholder(logTime, item.getCreateTime());

        String displayStatus = com.lora.cn.ui.constants.LogStatus.toText(item.getStatusCode());
        if (item.getStatusCode() == com.lora.cn.ui.constants.LogStatus.HANDLED.code) {
            String src = handledSourceLabels.get(item.getId());
            if (src != null && src.length() > 0) displayStatus = src;
        }
        setStatusWithDot(logStatus, displayStatus);

        // 设置名称字段 - 使用终端名称
        setTextOrPlaceholder(logName, item.getTerminalName());

        // 设置ID字段 - 使用设备ID
        setTextOrPlaceholder(logId, item.getDeviceId());

        if (item.getStatusCode() == com.lora.cn.ui.constants.LogStatus.HANDLED.code) {
            setTextOrPlaceholder(logComplete, item.getHandleUser());
            setTextOrPlaceholder(logCompleteTime, item.getHandleTime());
        } else {
            setTextOrPlaceholder(logComplete, item.getOperator());
            setTextOrPlaceholder(logCompleteTime, item.getOperationTime());
        }

        String act = item.getAction();
        logOperation.setVisibility(android.view.View.GONE);
        logOperation.setBackground(null);
        logOperation.setEnabled(true);
        logOperation.setTextColor(android.graphics.Color.parseColor("#333333"));
        boolean canHandle = item.getStatusCode() == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                || item.getStatusCode() == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code
                || item.getStatusCode() == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code;
        boolean isLatestAllowed = allowedHandleIds.contains(item.getId());
        if (item.getStatusCode() == com.lora.cn.ui.constants.LogStatus.HANDLED.code) {
            logOperation.setText("查看备注");
            logOperation.setBackgroundResource(R.drawable.bg_btn_voice);
            logOperation.setTextColor(android.graphics.Color.parseColor("#383B40"));
            logOperation.setOnClickListener(v -> {
                android.app.AlertDialog dlg = new android.app.AlertDialog.Builder(logOperation.getContext())
                        .setTitle("处理备注")
                        .setMessage(item.getHandleRemark() == null ? "" : item.getHandleRemark())
                        .setPositiveButton("确定", null)
                        .create();
                dlg.show();
            });
            logOperation.setVisibility(android.view.View.VISIBLE);
        } else if (canHandle && isLatestAllowed) {
            logOperation.setText("确认处理");
            logOperation.setBackgroundResource(R.drawable.bg_btn_now);
            logOperation.setTextColor(android.graphics.Color.WHITE);
            logOperation.setOnClickListener(v -> { if (onHandleClickListener != null) onHandleClickListener.onHandleClick(item); });
            logOperation.setVisibility(android.view.View.VISIBLE);
        } else if (act != null && (act.startsWith("发送下行数据") || act.contains("下行"))) {
            setTextOrPlaceholder(logOperation, act);
            logOperation.setOnClickListener(null);
            logOperation.setVisibility(android.view.View.VISIBLE);
        } else {
            setTextOrPlaceholder(logOperation, "");
            logOperation.setOnClickListener(null);
            logOperation.setVisibility(android.view.View.GONE);
        }
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
        return new QuickViewHolder(R.layout.item_terminal_detail_log, viewGroup);
    }

    /**
     * 设置文本或占位符
     * 如果文本为空，显示占位符背景；否则显示文本内容
     */
    private void setTextOrPlaceholder(TextView textView, String text) {
        if (TextUtils.isEmpty(text)) {
            textView.setText("-");
            textView.setBackground(null);
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
        
        if ("设备丢失".equals(status) || "设备离线".equals(status) || status.contains("设备丢失") || status.contains("设备离线")) {
            dotDrawable = ContextCompat.getDrawable(textView.getContext(), R.drawable.dot_red);
            textColor = Color.parseColor("#D30000");
        } else if ("低电量报警".equals(status) || status.contains("低电量报警")) {
            dotDrawable = ContextCompat.getDrawable(textView.getContext(), R.drawable.dot_orange);
            textColor = Color.parseColor("#FF9F0F");
        } else if ("在线".equals(status)) {
            dotDrawable = ContextCompat.getDrawable(textView.getContext(), R.drawable.dot_green);
            textColor = Color.parseColor("#00C851");
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
