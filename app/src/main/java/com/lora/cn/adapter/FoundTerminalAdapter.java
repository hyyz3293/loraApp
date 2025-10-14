package com.lora.cn.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.lora.cn.R;
import com.lora.cn.utils.LoRaProtocolParser;
import java.util.List;

/**
 * 搜索到的终端设备适配器
 */
public class FoundTerminalAdapter extends RecyclerView.Adapter<FoundTerminalAdapter.ViewHolder> {
    
    private List<LoRaProtocolParser.TerminalInfo> terminals;
    private OnTerminalAddListener listener;
    
    public interface OnTerminalAddListener {
        void onTerminalAdd(LoRaProtocolParser.TerminalInfo terminalInfo);
    }
    
    public FoundTerminalAdapter(List<LoRaProtocolParser.TerminalInfo> terminals) {
        this.terminals = terminals;
    }
    
    public void setOnTerminalAddListener(OnTerminalAddListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_found_terminal, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LoRaProtocolParser.TerminalInfo terminal = terminals.get(position);
        
        holder.tvTerminalName.setText(terminal.deviceName);
        holder.tvTerminalId.setText("ID: " + terminal.deviceId);
        holder.tvSignalStrength.setText("信号强度: " + terminal.signalStrength + " dBm");
        
        holder.btnAdd.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTerminalAdd(terminal);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return terminals != null ? terminals.size() : 0;
    }
    
    public void updateTerminals(List<LoRaProtocolParser.TerminalInfo> newTerminals) {
        this.terminals = newTerminals;
        notifyDataSetChanged();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTerminalName;
        TextView tvTerminalId;
        TextView tvSignalStrength;
        TextView btnAdd;

        ViewHolder(View itemView) {
            super(itemView);
            tvTerminalName = itemView.findViewById(R.id.tv_terminal_name);
            tvTerminalId = itemView.findViewById(R.id.tv_terminal_id);
            tvSignalStrength = itemView.findViewById(R.id.tv_signal_strength);
            btnAdd = itemView.findViewById(R.id.btn_add_terminal);
        }
    }
}