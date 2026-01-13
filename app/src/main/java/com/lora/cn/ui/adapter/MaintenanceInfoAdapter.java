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
    public enum Mode { HOME, SETTING, DETAIL }
    public interface OnConfirmClickListener { void onConfirmClick(MaintenanceInfo item); }
    public interface OnViewClickListener { void onViewClick(MaintenanceInfo item); }
    public interface OnViewRemarkClickListener { void onViewRemarkClick(MaintenanceInfo item); }
    public interface OnEditClickListener { void onEditClick(MaintenanceInfo item); }
    public interface OnDeleteClickListener { void onDeleteClick(MaintenanceInfo item); }
    private OnConfirmClickListener onConfirmClickListener;
    private OnViewClickListener onViewClickListener;
    private OnViewRemarkClickListener onViewRemarkClickListener;
    private OnEditClickListener onEditClickListener;
    private OnDeleteClickListener onDeleteClickListener;
    private final Mode mode;
    private final int itemLayoutResId;

    public MaintenanceInfoAdapter() {
        this(Mode.HOME);
    }

    public MaintenanceInfoAdapter(Mode mode) {
        this.mode = mode == null ? Mode.HOME : mode;
        this.itemLayoutResId =  R.layout.item_maintenance_home;
    }

    public void setOnConfirmClickListener(OnConfirmClickListener l) { this.onConfirmClickListener = l; }
    public void setOnViewClickListener(OnViewClickListener l) { this.onViewClickListener = l; }
    public void setOnViewRemarkClickListener(OnViewRemarkClickListener l) { this.onViewRemarkClickListener = l; }
    public void setOnEditClickListener(OnEditClickListener l) { this.onEditClickListener = l; }
    public void setOnDeleteClickListener(OnDeleteClickListener l) { this.onDeleteClickListener = l; }

    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int i, @Nullable MaintenanceInfo item) {
        View root = holder.itemView;
        TextView logTime = root.findViewById(R.id.log_time);
        TextView logStatus = root.findViewById(R.id.log_statu);
        TextView logName = root.findViewById(R.id.log_name);
        TextView logGroup = root.findViewById(R.id.log_group);
        TextView logId = root.findViewById(R.id.log_id);
        View containerName = root.findViewById(R.id.container_log_name);
        View containerGroup = root.findViewById(R.id.container_log_group);
        View containerId = root.findViewById(R.id.container_log_id);
        TextView logUser = root.findViewById(R.id.log_user);
        TextView logContent = root.findViewById(R.id.log_complute);
        TextView logHandleTime = root.findViewById(R.id.log_complute_time);
        TextView logOperation = root.findViewById(R.id.log_operation);
        View layoutOps = root.findViewById(R.id.layout_maintenance_ops);
        TextView btnDelete = root.findViewById(R.id.btn_maintenance_delete);
        TextView btnEdit = root.findViewById(R.id.btn_maintenance_edit);

        if (logTime != null)
            try {
                String createTime = item.getCreateTime();
                String[] create = createTime.split(" ");
                setTextOrDash(logTime, create[0] + "\n" + create[1]);
            }catch (Exception e) {
                e.getMessage();
                setTextOrDash(logTime, item.getCreateTime());
            }

//            setTextOrDash(logTime, item.getCreateTime());

        boolean done = item.getStatus() == 1;
        if (mode == Mode.SETTING) {
            if (logStatus != null) logStatus.setVisibility(View.GONE);
        } else {
            if (logStatus != null) {
                logStatus.setVisibility(View.VISIBLE);
                setStatusWithDot(logStatus, done ? "已维护" : "待维护", done);
            }
        }

        String name = item.getTerminalName();
        String group = item.getTerminalGroup();
        if (logName != null) setTextOrDash(logName, name == null ? "" : name);
        String displayGroup = "";
        try {
            android.content.Context ctx = holder.itemView.getContext();
            com.lora.cn.database.DatabaseHelper dbh = com.lora.cn.database.DatabaseHelper.getInstance(ctx);
            com.lora.cn.database.dao.TerminalDao tdao = new com.lora.cn.database.dao.TerminalDao(dbh);
            com.lora.cn.ui.model.Terminal t = tdao.getTerminalByDeviceId(item.getTerminalId());
            String gnames = t != null ? t.getGroupNamesText() : null;
            if (gnames != null && !gnames.trim().isEmpty()) {
                String[] arr = gnames.split(",");
                java.util.List<String> toks = new java.util.ArrayList<>();
                for (String tk : arr) {
                    if (tk == null) continue;
                    String raw = tk.trim();
                    if (raw.isEmpty()) continue;
                    int p = raw.lastIndexOf('-');
                    toks.add(p >= 0 ? raw.substring(p + 1) : raw);
                }
                StringBuilder sb = new StringBuilder();
                for (int idx2 = 0; idx2 < toks.size(); idx2++) {
                    if (idx2 > 0) sb.append("、");
                    sb.append(toks.get(idx2));
                }
                displayGroup = sb.toString();
            } else {
                displayGroup = group == null ? "" : group;
                if (displayGroup != null) {
                    int p2 = displayGroup.lastIndexOf('-');
                    displayGroup = p2 >= 0 ? displayGroup.substring(p2 + 1) : displayGroup;
                }
            }
        } catch (Exception ignored) {
            displayGroup = group == null ? "" : group;
        }
        if (logGroup != null) setTextOrDash(logGroup, displayGroup);

        String tid = item.getTerminalId();
        if (logId != null) setTextOrDash(logId, "终端ID：" + (tid == null ? "" : tid));

        if (mode == Mode.SETTING) {
            if (logContent != null) logContent.setText("");
            if (logHandleTime != null) {
                logHandleTime.setText("");
                logHandleTime.setVisibility(View.GONE);
            }
            if (logUser != null) {
                logUser.setText("");
            }
        } else {
            if (logContent != null) setTextOrDash(logContent, item.getContent());
            if (logContent != null) {
                logContent.setOnClickListener(v -> {
                    if (onViewClickListener != null) onViewClickListener.onViewClick(item);
                });
            }
            if (logUser != null) {
                setTextOrDash(logUser, item.getHandleUser());
            }
            if (logHandleTime != null) {
                logHandleTime.setVisibility(View.VISIBLE);
                if (done) {
                    try {
                        String handleTime = item.getHandleTime();
                        String[] handler = handleTime.split(" ");
                        setTextOrDash(logHandleTime, handler[0] + "\n" + handler[1]);
                    }catch (Exception e) {
                        e.getMessage();
                        setTextOrDash(logHandleTime, item.getHandleTime());
                    }

                } else {
                    logHandleTime.setText("");
                }
            }
        }

        if (mode == Mode.HOME || mode == Mode.DETAIL) {
            if (layoutOps != null) layoutOps.setVisibility(View.VISIBLE);
            if (btnEdit != null) {
                String remark = "";
                try { remark = item.getHandleRemark(); } catch (Exception ignored) {}
                boolean hasRemark = remark != null && !remark.trim().isEmpty();
                if (done && hasRemark) {
                    btnEdit.setVisibility(View.VISIBLE);
                    btnEdit.setText("查看备注");
                    btnEdit.setBackgroundResource(R.drawable.bg_btn_voice);
                    btnEdit.setTextColor(Color.parseColor("#383B40"));
                    btnEdit.setOnClickListener(v -> {
                        if (onViewRemarkClickListener != null) onViewRemarkClickListener.onViewRemarkClick(item);
                    });
                } else {
                    btnEdit.setVisibility(View.GONE);
                }
            }
            if (btnDelete != null) {
                if (done) {
                    btnDelete.setVisibility(View.GONE);
                } else {
                    btnDelete.setVisibility(View.VISIBLE);
                    btnDelete.setText("确认维护");
                    btnDelete.setBackgroundResource(R.drawable.bg_btn_now);
                    btnDelete.setTextColor(Color.WHITE);
                    btnDelete.setOnClickListener(v -> {
                        if (onConfirmClickListener != null) onConfirmClickListener.onConfirmClick(item);
                    });
                }
            }
        } else {
            if (layoutOps != null) layoutOps.setVisibility(View.VISIBLE);
            if (btnDelete != null) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> {
                    if (onDeleteClickListener != null) onDeleteClickListener.onDeleteClick(item);
                });
            }
            if (btnEdit != null) {
                btnEdit.setVisibility(View.VISIBLE);
                btnEdit.setOnClickListener(v -> {
                    if (onEditClickListener != null) onEditClickListener.onEditClick(item);
                });
            }
        }

        if (logOperation != null) {
            if (mode == Mode.HOME || mode == Mode.DETAIL) {
                logOperation.setVisibility(View.GONE);
                logOperation.setEnabled(false);
            } else {
                logOperation.setVisibility(View.VISIBLE);
                logOperation.setEnabled(true);
                if (mode == Mode.SETTING) {
                    logOperation.setText("维护内容");
                    logOperation.setBackgroundResource(R.drawable.bg_btn_voice);
                    logOperation.setTextColor(Color.parseColor("#383B40"));
                    logOperation.setOnClickListener(v -> {
                        if (onViewClickListener != null) onViewClickListener.onViewClick(item);
                    });
                } else if (done) {
                    logOperation.setText("查看");
                    logOperation.setBackgroundResource(R.drawable.bg_btn_voice);
                    logOperation.setTextColor(Color.parseColor("#383B40"));
                    logOperation.setOnClickListener(v -> {
                        if (onViewClickListener != null) onViewClickListener.onViewClick(item);
                    });
                } else {
                    logOperation.setText("确认维护");
                    logOperation.setBackgroundResource(R.drawable.bg_btn_now);
                    logOperation.setTextColor(Color.WHITE);
                    logOperation.setOnClickListener(v -> {
                        if (onConfirmClickListener != null) onConfirmClickListener.onConfirmClick(item);
                    });
                }
            }
        }
        if (mode == Mode.DETAIL) {
            if (containerName != null) containerName.setVisibility(View.GONE);
            if (containerGroup != null) containerGroup.setVisibility(View.GONE);
            if (containerId != null) containerId.setVisibility(View.GONE);
        } else {
            if (logName != null) logName.setVisibility(View.VISIBLE);
            if (logGroup != null) logGroup.setVisibility(View.VISIBLE);
            if (logId != null) logId.setVisibility(View.VISIBLE);
            if (containerId != null && mode == Mode.HOME) containerId.setVisibility(View.GONE);
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
        return new QuickViewHolder(itemLayoutResId, viewGroup);
    }
}
