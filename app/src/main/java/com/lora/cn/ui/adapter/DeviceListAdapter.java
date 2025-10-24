package com.lora.cn.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.database.entity.Terminal;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备列表适配器
 */
public class DeviceListAdapter extends BaseQuickAdapter<Terminal, QuickViewHolder> {
    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int i, @Nullable Terminal terminal) {
        holder.setText(R.id.tv_terminal_info, terminal.getDeviceName());
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
        return new QuickViewHolder(R.layout.item_device_list, viewGroup);
    }

//    private List<Terminal> terminals = new ArrayList<>();
//    private OnItemClickListener onItemClickListener;
//
//    public interface OnItemClickListener {
//        void onAddTerminalClick(Terminal terminal);
//    }
//
//    public void setOnItemClickListener(OnItemClickListener listener) {
//        this.onItemClickListener = listener;
//    }
//
//    public void setTerminals(List<Terminal> terminals) {
//        this.terminals = terminals != null ? terminals : new ArrayList<>();
//        notifyDataSetChanged();
//    }
//
//    @NonNull
//    @Override
//    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_device_list, parent, false);
//        return new ViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//        Terminal terminal = terminals.get(position);
//        holder.bind(terminal);
//    }
//
//    @Override
//    public int getItemCount() {
//        return terminals.size();
//    }
//
//    class ViewHolder extends RecyclerView.ViewHolder {
//        private TextView tvTerminalInfo;
//        private TextView btnAddTerminal;
//
//        public ViewHolder(@NonNull View itemView) {
//            super(itemView);
//            tvTerminalInfo = itemView.findViewById(R.id.tv_terminal_info);
//            btnAddTerminal = itemView.findViewById(R.id.btn_add_terminal);
//        }
//
//        public void bind(Terminal terminal) {
//            // 显示设备ID信息
//            String deviceInfo = "设备ID: " + (terminal.getDeviceId() != null ? terminal.getDeviceId() : "未知");
//            if (terminal.getDeviceName() != null) {
//                deviceInfo += "\n设备名称: " + terminal.getDeviceName();
//            }
//            if (terminal.getStatus() != null) {
//                deviceInfo += "\n状态: " + terminal.getStatus();
//            }
//            tvTerminalInfo.setText(deviceInfo);
//
//            // 设置添加终端按钮点击事件
//            btnAddTerminal.setOnClickListener(v -> {
//                if (onItemClickListener != null) {
//                    onItemClickListener.onAddTerminalClick(terminal);
//                }
//            });
//        }
//    }
}