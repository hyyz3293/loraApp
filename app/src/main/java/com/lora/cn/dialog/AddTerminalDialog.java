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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.ui.model.Terminal;
import com.lora.cn.utils.LoRaProtocolParser;

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
    
    public interface OnTerminalAddedListener {
        void onTerminalAdded(LoRaProtocolParser.TerminalInfo terminalInfo);
    }
    
    public AddTerminalDialog(@NonNull Context context) {
        super(context);
        this.context = context;
        this.foundTerminals = new ArrayList<>();
        this.handler = new Handler(Looper.getMainLooper());
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
        tvSearchStatus.setText("正在搜索终端设备...");
        
        // 清空之前的搜索结果
        foundTerminals.clear();
        adapter.notifyDataSetChanged();
        
        // 模拟LoRa搜索过程
        simulateLoRaSearch();
    }
    
    private void stopSearching() {
        llSearchStatus.setVisibility(View.GONE);
        btnSearch.setText("开始搜索");
        
        if (foundTerminals.isEmpty()) {
            tvNoDevices.setVisibility(View.VISIBLE);
        } else {
            tvFoundTerminalsTitle.setVisibility(View.VISIBLE);
            rvFoundTerminals.setVisibility(View.VISIBLE);
        }
    }
    
    private void simulateLoRaSearch() {
        // 模拟搜索过程，实际应用中这里会调用真实的LoRa通信
        handler.postDelayed(() -> {
            if (llSearchStatus.getVisibility() == View.VISIBLE) {
                // 模拟发现新设备
                LoRaProtocolParser.TerminalInfo terminalInfo = generateRandomTerminal();
                foundTerminals.add(terminalInfo);
                adapter.notifyItemInserted(foundTerminals.size() - 1);
                
                tvSearchStatus.setText("发现 " + foundTerminals.size() + " 个设备");
                
                // 继续搜索
                if (foundTerminals.size() < 5) { // 最多搜索5个设备
                    simulateLoRaSearch();
                } else {
                    stopSearching();
                }
            }
        }, 2000 + new Random().nextInt(3000)); // 2-5秒随机间隔
    }
    
    private LoRaProtocolParser.TerminalInfo generateRandomTerminal() {
        Random random = new Random();
        String terminalId = String.format("%08d", random.nextInt(100000000));
        String terminalName = "终端设备-" + terminalId.substring(4);
        int signalStrength = -30 - random.nextInt(50); // -30 到 -80 dBm
        
        LoRaProtocolParser.TerminalInfo terminalInfo = new LoRaProtocolParser.TerminalInfo();
        terminalInfo.deviceId = terminalId;
        terminalInfo.deviceName = terminalName;
        terminalInfo.signalStrength = signalStrength;
        terminalInfo.status = 1; // 1表示在线
        terminalInfo.timestamp = System.currentTimeMillis();
        
        return terminalInfo;
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
            
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTerminalName = itemView.findViewById(R.id.tv_terminal_name);
                tvTerminalId = itemView.findViewById(R.id.tv_terminal_id);
                tvSignalStrength = itemView.findViewById(R.id.tv_signal_strength);
                btnAddTerminal = itemView.findViewById(R.id.btn_add_terminal);
            }
        }
    }
}