package com.lora.cn.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.ui.model.Terminal;
import com.lora.cn.utils.LoRaProtocolParser;
import com.lora.cn.network.GatewayClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AddTerminalDialog extends Dialog {
    
    private Context context;
    private LinearLayout llSearchStatus;
    private ProgressBar pbSearching;
    private TextView tvSearchStatus;
    private TextView tvFoundTerminalsTitle;
    private RecyclerView rvFoundTerminals;
    private TextView tvNoDevices;
    private TextView btnCancel;
    private TextView btnSearch;
    
    private FoundTerminalAdapter adapter;
    private List<LoRaProtocolParser.TerminalInfo> foundTerminals;
    private Handler handler;
    private OnTerminalAddedListener listener;
    private GatewayClient gatewayClient;
    
    public interface OnTerminalAddedListener {
        void onTerminalAdded(LoRaProtocolParser.TerminalInfo terminalInfo);
    }
    
    public AddTerminalDialog(@NonNull Context context) {
        super(context);
        this.context = context;
        this.foundTerminals = new ArrayList<>();
        this.handler = new Handler(Looper.getMainLooper());
        this.gatewayClient = new GatewayClient();
        initDialog();
    }
    
    private void initDialog() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_terminal, null);
        setContentView(view);
        
        initViews(view);
        initListeners();
        setupRecyclerView();
    }
    
    private void initViews(View view) {
        llSearchStatus = view.findViewById(R.id.ll_search_status);
        pbSearching = view.findViewById(R.id.pb_searching);
        tvSearchStatus = view.findViewById(R.id.tv_search_status);
        tvFoundTerminalsTitle = view.findViewById(R.id.tv_found_terminals_title);
        rvFoundTerminals = view.findViewById(R.id.rv_found_terminals);
        tvNoDevices = view.findViewById(R.id.tv_no_devices);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnSearch = view.findViewById(R.id.btn_search);
    }
    
    private void initListeners() {
        btnCancel.setOnClickListener(v -> dismiss());
        
        btnSearch.setOnClickListener(v -> {
            if (btnSearch.getText().toString().equals("开始搜索")) {
                startSearching();
            } else {
                stopSearching();
            }
        });
    }
    
    private void setupRecyclerView() {
        adapter = new FoundTerminalAdapter(foundTerminals, this::addTerminalToDatabase);
        rvFoundTerminals.setLayoutManager(new LinearLayoutManager(context));
        rvFoundTerminals.setAdapter(adapter);
    }
    
    private void startSearching() {
        // 显示搜索状态
        llSearchStatus.setVisibility(View.VISIBLE);
        tvFoundTerminalsTitle.setVisibility(View.GONE);
        rvFoundTerminals.setVisibility(View.GONE);
        tvNoDevices.setVisibility(View.GONE);
        
        btnSearch.setText("停止搜索");
        tvSearchStatus.setText("正在检查网络连接...");
        
        // 清空之前的搜索结果
        foundTerminals.clear();
        adapter.notifyDataSetChanged();
        
        // 真实网关搜索
        gatewayClient.startScan(new GatewayClient.ScanListener() {
            @Override
            public void onDeviceFound(LoRaProtocolParser.TerminalInfo info) {
                handler.post(() -> {
                    if (llSearchStatus.getVisibility() == View.VISIBLE) {
                        foundTerminals.add(info);
                        adapter.notifyItemInserted(foundTerminals.size() - 1);
                        tvSearchStatus.setText("发现 " + foundTerminals.size() + " 个设备");
                    }
                });
            }

            @Override
            public void onStatus(String msg) {
                handler.post(() -> {
                    tvSearchStatus.setText(msg);
                    Log.d("AddTerminalDialog", "扫描状态: " + msg);
                });
            }

            @Override
            public void onError(String error) {
                Log.e("AddTerminalDialog", "扫描错误: " + error);
                handler.post(() -> {
                    // 显示详细的错误信息和解决建议
                    showDetailedError(error);
                    stopSearching();
                });
            }

            @Override
            public void onComplete() {
                handler.post(() -> stopSearching());
            }
        });
    }
    
    private void showDetailedError(String error) {
        // 创建详细的错误对话框
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("网关连接失败");
        
        String detailedMessage = error;
        String suggestions = "";
        
        // 根据错误类型提供具体的解决建议
        if (error.contains("Connection refused") || error.contains("连接被拒绝")) {
            suggestions = "\n\n解决建议：\n" +
                    "1. 检查网关设备是否开机并正常运行\n" +
                    "2. 确认网关服务是否在端口6000上运行\n" +
                    "3. 检查网关设备的防火墙设置\n" +
                    "4. 尝试重启网关设备";
        } else if (error.contains("timeout") || error.contains("超时")) {
            suggestions = "\n\n解决建议：\n" +
                    "1. 检查网络连接是否稳定\n" +
                    "2. 确认设备与网关在同一网络中\n" +
                    "3. 检查网关设备负载是否过高\n" +
                    "4. 尝试移动到网关设备附近";
        } else if (error.contains("无法ping通") || error.contains("网络不可达")) {
            suggestions = "\n\n解决建议：\n" +
                    "1. 检查网关IP地址是否正确\n" +
                    "2. 确认设备已连接到正确的WiFi网络\n" +
                    "3. 检查路由器设置，确保设备间可以通信\n" +
                    "4. 尝试重新连接WiFi网络";
        } else if (error.contains("HTTP明文流量")) {
            suggestions = "\n\n解决建议：\n" +
                    "1. 应用已更新网络安全配置\n" +
                    "2. 请重新安装应用或清除应用数据\n" +
                    "3. 如问题持续，请联系技术支持";
        } else {
            suggestions = "\n\n通用解决建议：\n" +
                    "1. 检查网关设备状态和网络连接\n" +
                    "2. 确认IP地址和端口配置正确\n" +
                    "3. 重启网关设备和移动设备\n" +
                    "4. 如问题持续，请联系技术支持";
        }
        
        builder.setMessage(detailedMessage + suggestions);
        builder.setPositiveButton("确定", null);
        builder.setNeutralButton("重试", (dialog, which) -> {
            dialog.dismiss();
            startSearching();
        });
        
        builder.create().show();
    }
    
    private void stopSearching() {
        llSearchStatus.setVisibility(View.GONE);
        btnSearch.setText("开始搜索");
        gatewayClient.stopScan();
        
        if (foundTerminals.isEmpty()) {
            tvNoDevices.setVisibility(View.VISIBLE);
        } else {
            tvFoundTerminalsTitle.setVisibility(View.VISIBLE);
            rvFoundTerminals.setVisibility(View.VISIBLE);
        }
    }
    
    private void addTerminalToDatabase(LoRaProtocolParser.TerminalInfo terminalInfo) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        
        // 检查终端是否已存在
        if (dbHelper.isTerminalExists(terminalInfo.deviceId)) {
            Toast.makeText(context, "终端设备已存在", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 创建新终端对象
        com.lora.cn.ui.model.Terminal terminal = new com.lora.cn.ui.model.Terminal();
        terminal.setTerminalId(terminalInfo.deviceId);
        terminal.setTerminalName(terminalInfo.deviceName);
        terminal.setSignalStrength(terminalInfo.signalStrength);
        terminal.setStatus("在线");
        terminal.setCreateTime(System.currentTimeMillis());
        
        // 添加到数据库
        long result = dbHelper.addTerminal(terminal);
        if (result > 0) {
            Toast.makeText(context, "终端添加成功", Toast.LENGTH_SHORT).show();
            
            // 通知监听器
            if (listener != null) {
                listener.onTerminalAdded(terminalInfo);
            }
            
            // 从列表中移除已添加的终端
            foundTerminals.remove(terminalInfo);
            adapter.notifyDataSetChanged();
            
            // 如果没有更多设备，显示提示
            if (foundTerminals.isEmpty()) {
                tvFoundTerminalsTitle.setVisibility(View.GONE);
                rvFoundTerminals.setVisibility(View.GONE);
                tvNoDevices.setVisibility(View.VISIBLE);
                tvNoDevices.setText("所有发现的设备已添加完成");
            }
        } else {
            Toast.makeText(context, "终端添加失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    public void setOnTerminalAddedListener(OnTerminalAddedListener listener) {
        this.listener = listener;
    }
    
    // 搜索到的终端适配器
    private static class FoundTerminalAdapter extends RecyclerView.Adapter<FoundTerminalAdapter.ViewHolder> {
        
        private List<LoRaProtocolParser.TerminalInfo> terminals;
        private OnAddTerminalClickListener listener;
        
        public interface OnAddTerminalClickListener {
            void onAddTerminalClick(LoRaProtocolParser.TerminalInfo terminalInfo);
        }
        
        public FoundTerminalAdapter(List<LoRaProtocolParser.TerminalInfo> terminals, 
                                  OnAddTerminalClickListener listener) {
            this.terminals = terminals;
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
            
            holder.btnAddTerminal.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddTerminalClick(terminal);
                }
            });

            holder.btnDetail.setOnClickListener(v -> {
                Context ctx = holder.itemView.getContext();
                TerminalDetailDialog dialog = new TerminalDetailDialog(ctx, terminal);
                dialog.show();
            });
        }
        
        @Override
        public int getItemCount() {
            return terminals.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTerminalName;
            TextView tvTerminalId;
            TextView tvSignalStrength;
            TextView btnAddTerminal;
            TextView btnDetail;
            
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTerminalName = itemView.findViewById(R.id.tv_terminal_name);
                tvTerminalId = itemView.findViewById(R.id.tv_terminal_id);
                tvSignalStrength = itemView.findViewById(R.id.tv_signal_strength);
                btnAddTerminal = itemView.findViewById(R.id.btn_add_terminal);
                btnDetail = itemView.findViewById(R.id.btn_detail);
            }
        }
    }
}