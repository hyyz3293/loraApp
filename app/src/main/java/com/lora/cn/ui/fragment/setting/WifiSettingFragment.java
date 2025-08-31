package com.lora.cn.ui.fragment.setting;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.ui.adapter.WifiListAdapter;
import com.lora.cn.ui.model.WifiItem;
import com.lora.cn.utils.DialogUtils;

import java.util.ArrayList;
import java.util.List;

public class WifiSettingFragment extends Fragment {
    
    private ImageView btnBack;
    private TextView titleText;
    private Switch wifiSwitch;
    private Button btnRefresh;
    private RecyclerView recyclerWifiList;
    private TextView tvCurrentWifi;
    private TextView tvWifiStatus;
    
    private WifiManager wifiManager;
    private WifiListAdapter wifiAdapter;
    private List<WifiItem> wifiList;
    
    public static WifiSettingFragment newInstance() {
        return new WifiSettingFragment();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wifi_setting, container, false);
        
        initViews(view);
        initWifiManager();
        initListener();
        setupRecyclerView();
        updateWifiStatus();
        
        return view;
    }
    
    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btn_back);
        titleText = view.findViewById(R.id.title_text);
        wifiSwitch = view.findViewById(R.id.wifi_switch);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        recyclerWifiList = view.findViewById(R.id.recycler_wifi_list);
        tvCurrentWifi = view.findViewById(R.id.tv_current_wifi);
        tvWifiStatus = view.findViewById(R.id.tv_wifi_status);
        
        titleText.setText("WiFi设置");
    }
    
    private void initWifiManager() {
        wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiList = new ArrayList<>();
    }
    
    private void initListener() {
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
        
        wifiSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (wifiManager != null) {
                wifiManager.setWifiEnabled(isChecked);
                updateWifiStatus();
                if (isChecked) {
                    scanWifiNetworks();
                } else {
                    wifiList.clear();
                    wifiAdapter.notifyDataSetChanged();
                }
            }
        });
        
        btnRefresh.setOnClickListener(v -> {
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                scanWifiNetworks();
                Toast.makeText(getContext(), "正在刷新WiFi列表...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "请先开启WiFi", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupRecyclerView() {
        wifiAdapter = new WifiListAdapter();
        recyclerWifiList.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerWifiList.setAdapter(wifiAdapter);
        
        wifiAdapter.setOnItemClickListener((item, position) -> {
            if (item.isSecured()) {
                showWifiPasswordDialog(item);
            } else {
                connectToWifi(item, null);
            }
        });
    }
    
    private void updateWifiStatus() {
        if (wifiManager != null) {
            boolean isEnabled = wifiManager.isWifiEnabled();
            wifiSwitch.setChecked(isEnabled);
            
            if (isEnabled) {
                tvWifiStatus.setText("已开启");
                tvWifiStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                
                // 获取当前连接的WiFi
                android.net.wifi.WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null && wifiInfo.getSSID() != null) {
                    String ssid = wifiInfo.getSSID().replace("\"", "");
                    tvCurrentWifi.setText("当前连接: " + ssid);
                } else {
                    tvCurrentWifi.setText("未连接到WiFi网络");
                }
            } else {
                tvWifiStatus.setText("已关闭");
                tvWifiStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                tvCurrentWifi.setText("WiFi已关闭");
            }
        }
    }
    
    private void scanWifiNetworks() {
        if (wifiManager != null) {
            wifiManager.startScan();
            List<ScanResult> scanResults = wifiManager.getScanResults();
            
            wifiList.clear();
            for (ScanResult result : scanResults) {
                if (result.SSID != null && !result.SSID.isEmpty()) {
                    WifiItem wifiItem = new WifiItem(
                        result.SSID,
                        result.level,
                        result.capabilities.contains("WPA") || result.capabilities.contains("WEP")
                    );
                    wifiList.add(wifiItem);
                }
            }
            
            wifiAdapter.submitList(new ArrayList<>(wifiList));
        }
    }
    
    private void showWifiPasswordDialog(WifiItem wifiItem) {
        DialogUtils.showWifiPasswordDialog(
            getContext(),
            "连接到 " + wifiItem.getSsid(),
            password -> connectToWifi(wifiItem, password)
        );
    }
    
    private void connectToWifi(WifiItem wifiItem, String password) {
        if (wifiManager != null) {
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = "\"" + wifiItem.getSsid() + "\"";
            
            if (wifiItem.isSecured() && password != null) {
                wifiConfig.preSharedKey = "\"" + password + "\"";
            } else {
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            }
            
            int networkId = wifiManager.addNetwork(wifiConfig);
            if (networkId != -1) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(networkId, true);
                wifiManager.reconnect();
                
                Toast.makeText(getContext(), "正在连接到 " + wifiItem.getSsid(), Toast.LENGTH_SHORT).show();
                
                // 延迟更新状态
                recyclerWifiList.postDelayed(this::updateWifiStatus, 3000);
            } else {
                Toast.makeText(getContext(), "连接失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
}