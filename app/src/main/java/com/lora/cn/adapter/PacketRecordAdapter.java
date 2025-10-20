package com.lora.cn.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.network.GatewayPacketsClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PacketRecordAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    static class Row {
        boolean header;
        String title; // DevEUI
        GatewayPacketsClient.PacketRecord record;
    }

    private final List<Row> rows = new ArrayList<>();

    public void setRecords(List<GatewayPacketsClient.PacketRecord> newRecords) {
        rows.clear();
        if (newRecords != null) {
            for (GatewayPacketsClient.PacketRecord r : newRecords) {
                Row row = new Row();
                row.header = false;
                row.record = r;
                rows.add(row);
            }
        }
        notifyDataSetChanged();
    }

    public void setGroupedRecords(List<GatewayPacketsClient.PacketRecord> newRecords) {
        rows.clear();
        if (newRecords != null && !newRecords.isEmpty()) {
            // 使用LinkedHashMap保持分组顺序
            Map<String, List<GatewayPacketsClient.PacketRecord>> groups = new LinkedHashMap<>();
            for (GatewayPacketsClient.PacketRecord r : newRecords) {
                String key = r.deviceId != null ? r.deviceId : "未知设备";
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
            }
            for (Map.Entry<String, List<GatewayPacketsClient.PacketRecord>> e : groups.entrySet()) {
                Row header = new Row();
                header.header = true;
                header.title = "DevEUI: " + e.getKey();
                rows.add(header);
                for (GatewayPacketsClient.PacketRecord r : e.getValue()) {
                    Row item = new Row();
                    item.header = false;
                    item.record = r;
                    rows.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).header ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_packet_group_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_packet_record, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (row.header) {
            HeaderViewHolder hv = (HeaderViewHolder) holder;
            hv.tvTitle.setText(row.title);
            // 保证高亮颜色
            hv.tvTitle.setTextColor(ContextCompat.getColor(hv.tvTitle.getContext(), R.color.colorAccent));
        } else {
            ItemViewHolder iv = (ItemViewHolder) holder;
            GatewayPacketsClient.PacketRecord r = row.record;
            iv.tvTime.setText(r.time != null ? r.time : "");
            iv.tvDevEui.setText("DevEUI: " + (r.deviceId != null ? r.deviceId : "-"));
            iv.tvDevAddr.setText("DevAddr: " + (r.devAddr != null ? r.devAddr : "-"));
            iv.tvSignal.setText(String.format("RSSI: %s SNR: %s DR: %s Freq: %s",
                    r.rssi != null ? r.rssi : "-",
                    r.snr != null ? r.snr : "-",
                    r.dr != null ? r.dr : "-",
                    r.freq != null ? r.freq : "-"));
            iv.tvFrame.setText(String.format("FCnt: %s FPort: %s",
                    r.fcnt != null ? r.fcnt : "-",
                    r.fport != null ? r.fport : "-"));
            String payload = r.payloadHex;
            if (payload != null && payload.length() > 200) payload = payload.substring(0, 200) + "...";
            iv.tvPayload.setText("Payload: " + (payload != null ? payload : "-"));
            // 高亮 DevEUI 文本颜色
            iv.tvDevEui.setTextColor(ContextCompat.getColor(iv.tvDevEui.getContext(), R.color.colorAccent));
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        HeaderViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_group_title);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        TextView tvDevEui;
        TextView tvDevAddr;
        TextView tvSignal;
        TextView tvFrame;
        TextView tvPayload;

        ItemViewHolder(View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvDevEui = itemView.findViewById(R.id.tv_dev_eui);
            tvDevAddr = itemView.findViewById(R.id.tv_dev_addr);
            tvSignal = itemView.findViewById(R.id.tv_signal);
            tvFrame = itemView.findViewById(R.id.tv_frame);
            tvPayload = itemView.findViewById(R.id.tv_payload);
        }
    }
}