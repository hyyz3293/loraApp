package com.lora.cn.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.R;
import com.lora.cn.adapter.FoundTerminalAdapter;
import com.lora.cn.adapter.PacketRecordAdapter;
// 删除网关HTTP/TCP扫描客户端
// import com.lora.cn.network.GatewayClient;
import com.lora.cn.network.GatewayPacketsClient;
import com.lora.cn.network.MqttPacketsClient;
import com.lora.cn.utils.LoRaProtocolParser;

import java.util.ArrayList;
import java.util.List;

public class AddTerminalDialog extends Dialog {

    private final Context context;

    private RecyclerView rvFoundTerminals;
    private TextView tvFoundTitle;
    private TextView tvNoDevices;

    private RecyclerView rvPackets;
    private TextView tvPacketsTitle;
    private View llPacketsHeader;
    private CheckBox cbGroupByDevEui;
    private TextView tvNoPackets;

    private ProgressBar pbSearching;
    private TextView tvSearchStatus;
    private View llSearchStatus;

    private FoundTerminalAdapter foundTerminalAdapter;
    private PacketRecordAdapter packetRecordAdapter;

    // private GatewayClient gatewayClient; // 改为MQTT，不再使用网关扫描
    private MqttPacketsClient mqttClient;
    private Handler uiHandler;

    private final List<LoRaProtocolParser.TerminalInfo> foundTerminals = new ArrayList<>();
    private final List<GatewayPacketsClient.PacketRecord> packets = new ArrayList<>();

    // 对外回调：添加终端
    public interface OnTerminalAddedListener {
        void onTerminalAdded(LoRaProtocolParser.TerminalInfo terminalInfo);
    }

    private OnTerminalAddedListener onTerminalAddedListener;

    public void setOnTerminalAddedListener(OnTerminalAddedListener listener) {
        this.onTerminalAddedListener = listener;
    }

    public AddTerminalDialog(@NonNull Context context) {
        super(context);
        this.context = context;
        initDialog();
    }

    private void initDialog() {
        View root = LayoutInflater.from(context).inflate(R.layout.dialog_add_terminal, null);
        setContentView(root);

        rvFoundTerminals = root.findViewById(R.id.rv_found_terminals);
        tvFoundTitle = root.findViewById(R.id.tv_found_terminals_title);
        tvNoDevices = root.findViewById(R.id.tv_no_devices);

        rvPackets = root.findViewById(R.id.rv_packets);
        tvPacketsTitle = root.findViewById(R.id.tv_packets_title);
        llPacketsHeader = root.findViewById(R.id.ll_packets_header);
        cbGroupByDevEui = root.findViewById(R.id.cb_group_by_dev_eui);
        tvNoPackets = root.findViewById(R.id.tv_no_packets);

        pbSearching = root.findViewById(R.id.pb_searching);
        tvSearchStatus = root.findViewById(R.id.tv_search_status);
        llSearchStatus = root.findViewById(R.id.ll_search_status);

        rvFoundTerminals.setLayoutManager(new LinearLayoutManager(context));
        foundTerminalAdapter = new FoundTerminalAdapter(foundTerminals);
        rvFoundTerminals.setAdapter(foundTerminalAdapter);
        foundTerminalAdapter.setOnTerminalAddListener(info -> {
            if (onTerminalAddedListener != null) {
                onTerminalAddedListener.onTerminalAdded(info);
            }
            // 停止连接并关闭弹窗
            if (mqttClient != null) mqttClient.disconnect();
            dismiss();
        });

        rvPackets.setLayoutManager(new LinearLayoutManager(context));
        packetRecordAdapter = new PacketRecordAdapter();
        rvPackets.setAdapter(packetRecordAdapter);

        cbGroupByDevEui.setChecked(true);
        cbGroupByDevEui.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (packets.isEmpty()) return;
            if (isChecked) {
                packetRecordAdapter.setGroupedRecords(packets);
            } else {
                packetRecordAdapter.setRecords(packets);
            }
        });

        uiHandler = new Handler(Looper.getMainLooper());

        root.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            if (mqttClient != null) mqttClient.disconnect();
            dismiss();
        });
        root.findViewById(R.id.btn_search).setOnClickListener(v -> startSearching());

        setOnDismissListener(d -> {
            if (mqttClient != null) mqttClient.disconnect();
        });
    }

    private void startSearching() {
        tvFoundTitle.setVisibility(View.GONE);
        rvFoundTerminals.setVisibility(View.GONE);
        tvNoDevices.setVisibility(View.GONE);

        tvPacketsTitle.setVisibility(View.GONE);
        rvPackets.setVisibility(View.GONE);
        tvNoPackets.setVisibility(View.GONE);

        // 清空上次结果
        foundTerminals.clear();
        foundTerminalAdapter.updateTerminals(foundTerminals);
        packets.clear();
        packetRecordAdapter.setRecords(new ArrayList<>());

        llSearchStatus.setVisibility(View.VISIBLE);
        pbSearching.setVisibility(View.VISIBLE);
        tvSearchStatus.setText("正在通过MQTT订阅上行数据...");

        // 改为MQTT采集：连接并订阅 Milesight uplink
        if (mqttClient == null) mqttClient = new MqttPacketsClient();
        String brokerUrl = "tcp://broker.emqx.io:1883";
        final String clientId = "android-" + System.currentTimeMillis();
        final String topicFilter = "/milesight/uplink/#";

        mqttClient.connectAndSubscribe(context, brokerUrl, clientId, topicFilter,
                null, null, false,
                new GatewayPacketsClient.PacketsListener() {
                    @Override
                    public void onStatus(String msg) {
                        uiHandler.post(() -> {
                            tvSearchStatus.setText(msg);
                            android.util.Log.d("AddTerminalDialog", msg);
                            // 订阅成功后隐藏进度
                            if (msg != null && msg.contains("订阅成功")) {
                                pbSearching.setVisibility(View.GONE);
                                llSearchStatus.setVisibility(View.GONE);
                            }
                        });
                    }



                    @Override
                    public void onPackets(List<GatewayPacketsClient.PacketRecord> records) {
                        uiHandler.post(() -> {
                            if (records == null || records.isEmpty()) {
                                tvNoPackets.setVisibility(View.VISIBLE);
                                return;
                            }
                            llPacketsHeader.setVisibility(View.VISIBLE);
                            rvPackets.setVisibility(View.VISIBLE);
                            tvNoPackets.setVisibility(View.GONE);
                            packets.clear();
                            packets.addAll(records);
                            if (cbGroupByDevEui.isChecked()) {
                                packetRecordAdapter.setGroupedRecords(packets);
                            } else {
                                packetRecordAdapter.setRecords(packets);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        uiHandler.post(() -> {
                            tvNoPackets.setText("MQTT连接/订阅失败：" + error);
                            tvNoPackets.setVisibility(View.VISIBLE);
                        });
                    }

                    @Override
                    public void onComplete() {
                        // ignore
                    }
                });

        // 旧代码：通过网关HTTP抓取packets，已停用
        // new Thread(() -> {
        //     String ip = SPUtils.getInstance().getString("gateway_ip", "");
        //     if (ip == null || ip.isEmpty()) {
        //         uiHandler.post(() -> {
        //             tvNoPackets.setText("未配置网关IP，请先在设置中配置");
        //             tvNoPackets.setVisibility(View.VISIBLE);
        //         });
        //         return;
        //     }
        //     GatewayPacketsClient client = new GatewayPacketsClient();
        //     client.fetchPackets(ip, new GatewayPacketsClient.PacketsListener() { /* ... */ });
        // }).start();
    }
}