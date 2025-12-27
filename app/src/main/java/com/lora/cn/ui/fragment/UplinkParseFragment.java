package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.events.UplinkDataEvent;
import com.lora.cn.utils.LoRaFrameParser;

public class UplinkParseFragment extends Fragment {
    private TextView tvTitle;
    private android.widget.EditText etHexInput;
    private Button btnParseHex;
    private TextView tvDeviceId;
    private TextView tvTime;
    private TextView tvHex;
    private RecyclerView rvList;
    private ItemsAdapter adapter;
    private java.util.List<Item> items = new java.util.ArrayList<>();
    private Button btnBuildDownlinkHex;
    private TextView tvDownlinkHex;
    private java.util.concurrent.ExecutorService ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_uplink_parse, container, false);
        tvTitle = v.findViewById(R.id.tv_title);
        etHexInput = v.findViewById(R.id.et_hex_input);
        btnParseHex = v.findViewById(R.id.btn_parse_hex);
        tvDeviceId = v.findViewById(R.id.tv_device_id);
        tvTime = v.findViewById(R.id.tv_time);
        tvHex = v.findViewById(R.id.tv_hex);
        rvList = v.findViewById(R.id.rv_list);
        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvList.setHasFixedSize(true);
        adapter = new ItemsAdapter(items);
        rvList.setAdapter(adapter);
        btnBuildDownlinkHex = v.findViewById(R.id.btn_build_downlink_hex);
        tvDownlinkHex = v.findViewById(R.id.tv_downlink_hex);
        if (btnParseHex != null) {
            btnParseHex.setOnClickListener(view -> {
                String hex = etHexInput.getText() != null ? etHexInput.getText().toString().trim() : "";
                if (TextUtils.isEmpty(hex)) return;
                LoRaFrameParser.ParsedFrame frame = LoRaFrameParser.parseFrame(hex);
                if (frame == null) return;
                tvDeviceId.setText(frame.deviceId != null ? frame.deviceId : "");
                tvTime.setText(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                tvHex.setText(hex);
                java.util.List<Item> list = buildItemsFromFrame(frame);
                java.util.List<Item> downlink = buildDownlinkSummaryFromFrame(frame);
                items.clear();
                items.add(new Item("—— 基本解析 ——", ""));
                items.addAll(list);
                items.add(new Item("—— 下行字段 ——", ""));
                items.addAll(downlink);
                adapter.notifyDataSetChanged();
                if (btnBuildDownlinkHex != null) {
                    btnBuildDownlinkHex.setOnClickListener(v1 -> {
                        ioExecutor.execute(() -> {
                            try {
                                String dev = frame.deviceId != null ? frame.deviceId : "";
                                long utcMs = frame.dataTime != null ? frame.dataTime.getTime() : System.currentTimeMillis();
                                int res2 = 0xFFFFFFFF;
                                int res3 = 0xFFFFFFFF;
                                int res4 = 0xFFFF;
                                int low = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
                                int ack = 1;
                                int dep = 0xFF;
                                int cart = 0xFF;
                                int r9 = 0xFF;
                                int r10 = 0xFF;
                                int qop = 0;
                                int clearMask = 0x00000000;
                                int interval = Math.max(5, Math.min(1440, frame.sleepIntervalMin > 0 ? frame.sleepIntervalMin : com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 60)));
                                int alarmCount = Math.max(0, Math.min(2, frame.alarmCount));
                                int[] alarmMinutes = frame.alarmMinutes != null && frame.alarmMinutes.length > 0
                                        ? frame.alarmMinutes
                                        : new int[]{getGlobalAlarmMinute()};
                                byte[] bytes = com.lora.cn.utils.LoRaProtocolParser.buildDownlink8001Full(
                                        dev,
                                        (byte) (System.currentTimeMillis() & 0xFF),
                                        utcMs,
                                        res2,
                                        res3,
                                        res4,
                                        low,
                                        ack,
                                        dep,
                                        cart,
                                        r9,
                                        r10,
                                        qop,
                                        clearMask,
                                        interval,
                                        alarmCount,
                                        alarmMinutes
                                );
                                String out = com.lora.cn.utils.LoRaProtocolParser.bytesToHex(bytes).replaceAll("\\s+", "");
                                if (mainHandler != null && tvDownlinkHex != null) {
                                    mainHandler.post(() -> tvDownlinkHex.setText(out));
                                }
                            } catch (Exception ignored) {}
                        });
                    });
                }
            });
        }
        return v;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        prefillLatestUplink();
    }
    
    private void prefillLatestUplink() {
        try {
            android.content.Context ctx = getContext();
            if (ctx == null) return;
            android.content.Context appCtx = ctx.getApplicationContext();
            ioExecutor.execute(() -> {
                String hex = null;
                try {
                    com.lora.cn.database.DatabaseHelper db = com.lora.cn.database.DatabaseHelper.getInstance(appCtx);
                    java.util.List<com.lora.cn.ui.model.LogInfo> logs = db.getAllLogs();
                    if (logs != null) {
                        for (com.lora.cn.ui.model.LogInfo li : logs) {
                            String a = li.getAction();
                            if (a != null && a.startsWith("接收上行数据")) {
                                int idx = a.indexOf(":");
                                if (idx >= 0 && idx + 1 < a.length()) {
                                    hex = a.substring(idx + 1).trim();
                                    break;
                                }
                            }
                        }
                    }
                    if (hex == null) {
                        java.util.List<com.lora.cn.ui.model.LogInfo> unbound = db.getAllUnboundLogs();
                        if (unbound != null) {
                            for (com.lora.cn.ui.model.LogInfo li : unbound) {
                                String a = li.getAction();
                                if (a != null && a.startsWith("接收上行数据")) {
                                    int idx = a.indexOf(":");
                                    if (idx >= 0 && idx + 1 < a.length()) {
                                        hex = a.substring(idx + 1).trim();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
                final String finalHex = hex;
                if (finalHex != null && mainHandler != null) {
                    mainHandler.post(() -> {
                        try {
                            if (etHexInput != null) etHexInput.setText(finalHex);
                            if (btnParseHex != null) btnParseHex.performClick();
                        } catch (Exception ignored) {}
                    });
                }
            });
        } catch (Exception ignored) {}
    }

    private java.util.List<Item> buildItemsFromFrame(LoRaFrameParser.ParsedFrame frame) {
        java.util.List<Item> list = new java.util.ArrayList<>();
        list.add(new Item("功能码", frame.functionCode));
        list.add(new Item("流水号", String.valueOf(frame.sequenceNumber)));
        list.add(new Item("数据长度", String.valueOf(frame.dataLength)));
        String t = frame.dataTime != null ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(frame.dataTime) : "";
        list.add(new Item("数据产生时间(UTC)", t));
        String evDesc = com.lora.cn.utils.LoRaFrameParser.getDeviceEventDescription(frame.deviceEvent);
        list.add(new Item("设备事件", evDesc));
        StringBuilder evBits = new StringBuilder();
        evBits.append("Bit0=").append(((frame.deviceEvent & 0x01L) != 0) ? "1" : "0").append(" 电源开锁-开； ");
        evBits.append("Bit1=").append(((frame.deviceEvent & 0x02L) != 0) ? "1" : "0").append(" 电源开锁-关； ");
        evBits.append("Bit2=").append(((frame.deviceEvent & 0x04L) != 0) ? "1" : "0").append(" 定期主动上报； ");
        evBits.append("Bit3=").append(((frame.deviceEvent & 0x08L) != 0) ? "1" : "0").append(" 主动取走； ");
        evBits.append("Bit4=").append(((frame.deviceEvent & 0x10L) != 0) ? "1" : "0").append(" 主动放入； ");
        evBits.append("Bit5=").append(((frame.deviceEvent & 0x20L) != 0) ? "1" : "0").append(" 非法移走报警； ");
        evBits.append("Bit6=").append(((frame.deviceEvent & 0x40L) != 0) ? "1" : "0").append(" 低电量<20%； ");
        evBits.append("Bit7=").append(((frame.deviceEvent & 0x80L) != 0) ? "1" : "0").append(" 护士站查询事件");
        list.add(new Item("设备事件位", evBits.toString()));
        StringBuilder stBits = new StringBuilder();
        stBits.append("Bit0=").append(((frame.deviceStatus & 0x01L) != 0) ? "1" : "0").append(" 电源锁:开； ");
        stBits.append("Bit1=").append(((frame.deviceStatus & 0x02L) != 0) ? "1" : "0").append(" 层1不在位； ");
        stBits.append("Bit2=").append(((frame.deviceStatus & 0x04L) != 0) ? "1" : "0").append(" 层2不在位； ");
        stBits.append("Bit3=").append(((frame.deviceStatus & 0x08L) != 0) ? "1" : "0").append(" 层3不在位； ");
        stBits.append("Bit4=").append(((frame.deviceStatus & 0x10L) != 0) ? "1" : "0").append(" 层4不在位； ");
        stBits.append("Bit5=").append(((frame.deviceStatus & 0x20L) != 0) ? "1" : "0").append(" 层5不在位； ");
        stBits.append("Bit6=").append(((frame.deviceStatus & 0x40L) != 0) ? "1" : "0").append(" 维护需要");
        list.add(new Item("设备状态位", stBits.toString()));
        double volt = frame.batteryVoltage * 0.01d;
        list.add(new Item("电池电压(V)", String.format(java.util.Locale.getDefault(), "%.2f", volt)));
        list.add(new Item("电量(%)", String.valueOf(frame.batteryLevel)));
        int rssiDbm = -Math.max(0, Math.min(138, frame.rssi));
        list.add(new Item("RSSI(dBm)", String.valueOf(rssiDbm)));
        list.add(new Item("科室编号", String.valueOf(frame.departmentNumber)));
        list.add(new Item("台车编号", String.valueOf(frame.cartNumber)));
        list.add(new Item("放置的设备数量", String.valueOf(frame.deviceCount)));
        list.add(new Item("台架编号", String.valueOf(frame.rackNumber)));
        list.add(new Item("应答护士站操作指令", frame.nurseAckOp == 1 ? "1(清除报警)" : "0(无操作)"));
        StringBuilder ackParams = new StringBuilder();
        ackParams.append("Bit0=").append(((frame.nurseAckParams & 0x01L) != 0) ? "1" : "0").append(" 已清除非法移走； ");
        ackParams.append("Bit1=").append(((frame.nurseAckParams & 0x02L) != 0) ? "1" : "0").append(" 未使用； ");
        ackParams.append("Bit2=").append(((frame.nurseAckParams & 0x04L) != 0) ? "1" : "0").append(" 已清除维护标识");
        list.add(new Item("应答护士站参数位", ackParams.toString()));
        int interval = Math.max(5, Math.min(1440, frame.sleepIntervalMin));
        list.add(new Item("当前休眠间隔(min)", String.valueOf(interval)));
        list.add(new Item("当前闹钟数量", String.valueOf(frame.alarmCount)));
        if (frame.alarmMinutes != null && frame.alarmMinutes.length > 0) {
            StringBuilder sb3 = new StringBuilder();
            for (int i = 0; i < frame.alarmMinutes.length; i++) {
                if (sb3.length() > 0) sb3.append("，");
                sb3.append(frame.alarmMinutes[i]).append("min");
            }
            list.add(new Item("闹钟时刻点列表", sb3.toString()));
        }
        return list;
    }

    private java.util.List<Item> buildDownlinkSummaryFromFrame(LoRaFrameParser.ParsedFrame frame) {
        java.util.List<Item> downlink = new java.util.ArrayList<>();
        String timeStr = frame.dataTime != null ? new java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(frame.dataTime) : "";
        downlink.add(new Item("下行-时间BCD", timeStr));
        downlink.add(new Item("下行-保留2(4B)", "FFFFFFFF"));
        downlink.add(new Item("下行-保留3(4B)", "FFFFFFFF"));
        downlink.add(new Item("下行-保留4(2B)", "FFFF"));
        int lowTh = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
        downlink.add(new Item("下行-低电量阈值(%)", String.valueOf(lowTh)));
        downlink.add(new Item("下行-应答结果", "1"));
        downlink.add(new Item("下行-科室ID", "FF"));
        downlink.add(new Item("下行-台车ID", "FF"));
        downlink.add(new Item("下行-保留9(1B)", "FF"));
        downlink.add(new Item("下行-保留10(1B)", "FF"));
        downlink.add(new Item("下行-查询操作指令", "0"));
        downlink.add(new Item("下行-操作指令参数(4B)", "00000000"));
        int interval = Math.max(5, Math.min(1440, frame.sleepIntervalMin));
        downlink.add(new Item("下行-上报间隔(分钟)", String.valueOf(interval)));
        downlink.add(new Item("下行-闹钟数量", String.valueOf(frame.alarmCount)));
        if (frame.alarmMinutes != null && frame.alarmMinutes.length > 0) {
            StringBuilder sb4 = new StringBuilder();
            for (int i = 0; i < frame.alarmMinutes.length; i++) {
                if (sb4.length() > 0) sb4.append("，");
                sb4.append(frame.alarmMinutes[i]).append("min");
            }
            downlink.add(new Item("下行-闹钟时刻表", sb4.toString()));
        }
        return downlink;
    }

    private int getGlobalAlarmMinute() {
        int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
        int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
        int minutes = h * 60 + m;
        if (minutes < 0) minutes = 0;
        if (minutes > 1440) minutes = 1440;
        return minutes;
    }

    @Override
    public void onStart() {
        super.onStart();
        try { org.greenrobot.eventbus.EventBus.getDefault().register(this); } catch (Exception ignored) {}
    }

    @Override
    public void onStop() {
        super.onStop();
        try { org.greenrobot.eventbus.EventBus.getDefault().unregister(this); } catch (Exception ignored) {}
    }

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    public void onUplinkDataEvent(UplinkDataEvent event) {
        if (event == null) return;
        String hex = event.getHex();
        if (TextUtils.isEmpty(hex)) return;
        LoRaFrameParser.ParsedFrame frame = LoRaFrameParser.parseFrame(hex);
        if (frame == null) return;
        tvDeviceId.setText(frame.deviceId != null ? frame.deviceId : "");
        tvTime.setText(event.getTime() != null ? event.getTime() : "");
        tvHex.setText(hex);
        java.util.List<Item> list = new java.util.ArrayList<>();
        list.add(new Item("功能码", frame.functionCode));
        list.add(new Item("流水号", String.valueOf(frame.sequenceNumber)));
        list.add(new Item("数据长度", String.valueOf(frame.dataLength)));
        String t = frame.dataTime != null ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(frame.dataTime) : "";
        list.add(new Item("数据时间", t));
        list.add(new Item("设备事件", com.lora.cn.utils.LoRaFrameParser.getDeviceEventDescription(frame.deviceEvent)));
        if (frame.eventFlags != null) {
            StringBuilder sb = new StringBuilder();
            for (LoRaFrameParser.DeviceEventFlag f : frame.eventFlags) {
                if (sb.length() > 0) sb.append("，");
                sb.append(f.label());
            }
            list.add(new Item("事件标记", sb.toString()));
        }
        if (frame.statusFlags != null) {
            StringBuilder sb2 = new StringBuilder();
            for (LoRaFrameParser.DeviceStatusFlag f : frame.statusFlags) {
                if (sb2.length() > 0) sb2.append("，");
                sb2.append(f.label());
            }
            list.add(new Item("状态标记", sb2.toString()));
        }
        list.add(new Item("电池电压", String.valueOf(frame.batteryVoltage)));
        list.add(new Item("电量", String.valueOf(frame.batteryLevel)));
        list.add(new Item("RSSI", String.valueOf(frame.rssi)));
        list.add(new Item("科室编号", String.valueOf(frame.departmentNumber)));
        list.add(new Item("台车编号", String.valueOf(frame.cartNumber)));
        list.add(new Item("设备数量", String.valueOf(frame.deviceCount)));
        list.add(new Item("台架编号", String.valueOf(frame.rackNumber)));
        list.add(new Item("护士站应答指令", String.valueOf(frame.nurseAckOp)));
        list.add(new Item("护士站应答参数", String.valueOf(frame.nurseAckParams)));
        list.add(new Item("休眠间隔(分钟)", String.valueOf(frame.sleepIntervalMin)));
        list.add(new Item("闹钟数量", String.valueOf(frame.alarmCount)));
        if (frame.alarmMinutes != null && frame.alarmMinutes.length > 0) {
            StringBuilder sb3 = new StringBuilder();
            for (int i = 0; i < frame.alarmMinutes.length; i++) {
                if (sb3.length() > 0) sb3.append("，");
                sb3.append(frame.alarmMinutes[i]);
            }
            list.add(new Item("闹钟时刻表", sb3.toString()));
        }
        items.clear();
        items.addAll(list);
        adapter.notifyDataSetChanged();
    }

    private static class Item {
        final String title;
        final String value;
        Item(String t, String v) { this.title = t; this.value = v == null ? "" : v; }
    }

    private static class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.VH> {
        private final java.util.List<Item> data;
        ItemsAdapter(java.util.List<Item> d) { this.data = d; }
        static class VH extends RecyclerView.ViewHolder {
            TextView t1;
            TextView t2;
            VH(@NonNull View itemView) {
                super(itemView);
                this.t1 = itemView.findViewById(android.R.id.text1);
                this.t2 = itemView.findViewById(android.R.id.text2);
            }
        }
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Item it = data.get(position);
            holder.t1.setText(it.title);
            holder.t2.setText(it.value);
        }
        @Override
        public int getItemCount() { return data == null ? 0 : data.size(); }
    }
}
