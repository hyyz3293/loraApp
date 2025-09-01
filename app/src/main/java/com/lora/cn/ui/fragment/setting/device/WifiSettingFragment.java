package com.lora.cn.ui.fragment.setting.device;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
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
    
    private TextView wifiName;
    private EditText wifiPwd;
    private ImageView wifiPwdLock;
    private LinearLayout wifiLl;
    private TextView connectBtn;
    private TextView backBtn;
    
    private boolean isPasswordVisible = true; // 密码默认可见
    private List<WifiItem> wifiList = new ArrayList<>();
    private WifiManager wifiManager;
    private BroadcastReceiver wifiScanReceiver;
    
    public static WifiSettingFragment newInstance() {
        return new WifiSettingFragment();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wifi_setting, container, false);
        
        // 初始化WifiManager
        wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        
        initViews(view);
        initListeners();
        initWifiScanReceiver();
        initWifiData();
        
        return view;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 注销广播接收器
        if (wifiScanReceiver != null && getContext() != null) {
            getContext().unregisterReceiver(wifiScanReceiver);
        }
    }

    private void initViews(View view) {
        wifiName = view.findViewById(R.id.wifi_name);
        wifiPwd = view.findViewById(R.id.wifi_pwd);
        wifiPwdLock = view.findViewById(R.id.wifi_pwd_lock);
        wifiLl = view.findViewById(R.id.wifi_ll);
        connectBtn = view.findViewById(R.id.connect);
        backBtn = view.findViewById(R.id.back);
        
        // 设置密码默认可见
        wifiPwd.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        wifiPwdLock.setImageResource(R.mipmap.com_eye); // 需要添加眼睛图标
    }

    private void initListeners() {
        // 密码可见性切换
        wifiPwdLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });
        
        // WiFi列表点击
        wifiLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 重新扫描WiFi并显示列表
                scanWifiNetworks();
                showWifiListDialog();
            }
        });
        
        // 连接按钮
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToWifi();
            }
        });
        
        // 返回按钮
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            }
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // 隐藏密码
            wifiPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            wifiPwdLock.setImageResource(R.mipmap.com_eye_un); // 需要添加闭眼图标
            isPasswordVisible = false;
        } else {
            // 显示密码
            wifiPwd.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            wifiPwdLock.setImageResource(R.mipmap.com_eye); // 需要添加眼睛图标
            isPasswordVisible = true;
        }
        // 将光标移到文本末尾
        wifiPwd.setSelection(wifiPwd.getText().length());
    }

    private void showWifiListDialog() {
        // 创建WiFi列表对话框
        DialogUtils.showWifiListDialog(getContext(), wifiList, new DialogUtils.OnWifiSelectedListener() {
            @Override
            public void onWifiSelected(WifiItem wifiItem) {
                wifiName.setText(wifiItem.getSsid());
                wifiPwd.setText(""); 
            }
        });
    }

    private void connectToWifi() {
        String ssid = wifiName.getText().toString().trim();
        String password = wifiPwd.getText().toString().trim();
        
        if (ssid.isEmpty()) {
            Toast.makeText(getContext(), "请选择WiFi网络", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (password.isEmpty()) {
            Toast.makeText(getContext(), "请输入WiFi密码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 这里实现WiFi连接逻辑
        Toast.makeText(getContext(), "正在连接到 " + ssid + "...", Toast.LENGTH_SHORT).show();
        
        // TODO: 实际的WiFi连接代码
        connectToWifiNetwork(ssid, password);
    }

    private void connectToWifiNetwork(String ssid, String password) {
        try {
            WifiManager wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            
            if (wifiManager != null) {
                WifiConfiguration wifiConfig = new WifiConfiguration();
                wifiConfig.SSID = String.format("\"%s\"", ssid);
                wifiConfig.preSharedKey = String.format("\"%s\"", password);
                
                int networkId = wifiManager.addNetwork(wifiConfig);
                if (networkId != -1) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(networkId, true);
                    wifiManager.reconnect();
                    
                    Toast.makeText(getContext(), "WiFi连接成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "WiFi连接失败", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "连接出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initWifiScanReceiver() {
        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess();
                } else {
                    scanFailure();
                }
            }
        };
        
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getContext().registerReceiver(wifiScanReceiver, intentFilter);
    }

    private void initWifiData() {
        // 检查WiFi是否开启
        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            Toast.makeText(getContext(), "WiFi未开启，请先开启WiFi", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 开始扫描WiFi网络
        scanWifiNetworks();
    }

    private void scanWifiNetworks() {
        if (wifiManager == null) {
            Toast.makeText(getContext(), "无法获取WiFi管理器", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查位置权限
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "需要位置权限才能扫描WiFi网络", Toast.LENGTH_SHORT).show();
            return;
        }

        // 开始扫描
        boolean success = wifiManager.startScan();
        if (!success) {
            // 扫描失败，使用上次的扫描结果
            scanFailure();
        }
    }

    private void scanSuccess() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        List<ScanResult> scanResults = wifiManager.getScanResults();
        wifiList.clear();
        
        for (ScanResult scanResult : scanResults) {
            // 过滤掉空的SSID
            if (scanResult.SSID != null && !scanResult.SSID.isEmpty()) {
                // 检查是否已存在相同SSID的网络（避免重复）
                boolean exists = false;
                for (WifiItem existingItem : wifiList) {
                    if (existingItem.getSsid().equals(scanResult.SSID)) {
                        exists = true;
                        break;
                    }
                }
                
                if (!exists) {
                    // 判断是否加密
                    boolean isSecured = !scanResult.capabilities.contains("[ESS]") || 
                                       scanResult.capabilities.contains("WPA") || 
                                       scanResult.capabilities.contains("WEP");
                    
                    WifiItem wifiItem = new WifiItem(scanResult.SSID, scanResult.level, isSecured);
                    wifiList.add(wifiItem);
                }
            }
        }
        
        // 按信号强度排序（信号强度越高越靠前）
        wifiList.sort((w1, w2) -> Integer.compare(w2.getSignalLevel(), w1.getSignalLevel()));
        
        // 设置默认WiFi名称（选择信号最强的）
        if (!wifiList.isEmpty() && wifiName.getText().toString().isEmpty()) {
            wifiName.setText(wifiList.get(0).getSsid());
        }
        
        Toast.makeText(getContext(), "发现 " + wifiList.size() + " 个WiFi网络", Toast.LENGTH_SHORT).show();
    }

    private void scanFailure() {
        // 扫描失败，尝试获取上次的扫描结果
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            if (scanResults != null && !scanResults.isEmpty()) {
                scanSuccess(); // 使用上次的结果
            } else {
                Toast.makeText(getContext(), "WiFi扫描失败，请检查WiFi是否开启", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "需要位置权限才能获取WiFi列表", Toast.LENGTH_SHORT).show();
        }
    }
}