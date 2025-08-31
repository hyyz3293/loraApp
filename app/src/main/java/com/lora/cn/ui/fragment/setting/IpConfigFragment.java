package com.lora.cn.ui.fragment.setting;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lora.cn.R;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class IpConfigFragment extends Fragment {
    
    private ImageView btnBack;
    private TextView titleText;
    private RadioGroup radioGroupIpType;
    private RadioButton radioDhcp;
    private RadioButton radioStatic;
    
    private View layoutStaticConfig;
    private EditText etIpAddress;
    private EditText etSubnetMask;
    private EditText etGateway;
    private EditText etDns1;
    private EditText etDns2;
    
    private TextView tvCurrentIp;
    private TextView tvCurrentGateway;
    private TextView tvCurrentDns;
    
    private Button btnSave;
    private Button btnRefresh;
    
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    
    public static IpConfigFragment newInstance() {
        return new IpConfigFragment();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ip_config, container, false);
        
        initViews(view);
        initListener();
        loadCurrentNetworkInfo();
        
        return view;
    }
    
    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btn_back);
        titleText = view.findViewById(R.id.title_text);
        radioGroupIpType = view.findViewById(R.id.radio_group_ip_type);
        radioDhcp = view.findViewById(R.id.radio_dhcp);
        radioStatic = view.findViewById(R.id.radio_static);
        
        layoutStaticConfig = view.findViewById(R.id.layout_static_config);
        etIpAddress = view.findViewById(R.id.et_ip_address);
        etSubnetMask = view.findViewById(R.id.et_subnet_mask);
        etGateway = view.findViewById(R.id.et_gateway);
        etDns1 = view.findViewById(R.id.et_dns1);
        etDns2 = view.findViewById(R.id.et_dns2);
        
        tvCurrentIp = view.findViewById(R.id.tv_current_ip);
        tvCurrentGateway = view.findViewById(R.id.tv_current_gateway);
        tvCurrentDns = view.findViewById(R.id.tv_current_dns);
        
        btnSave = view.findViewById(R.id.btn_save);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        
        titleText.setText("IP配置");
        
        // 设置默认值
        etSubnetMask.setText("255.255.255.0");
        etDns1.setText("8.8.8.8");
        etDns2.setText("8.8.4.4");
    }
    
    private void initListener() {
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
        
        radioGroupIpType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_dhcp) {
                layoutStaticConfig.setVisibility(View.GONE);
            } else if (checkedId == R.id.radio_static) {
                layoutStaticConfig.setVisibility(View.VISIBLE);
            }
        });
        
        btnSave.setOnClickListener(v -> saveIpConfiguration());
        
        btnRefresh.setOnClickListener(v -> {
            loadCurrentNetworkInfo();
            Toast.makeText(getContext(), "网络信息已刷新", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void loadCurrentNetworkInfo() {
        try {
            // 获取当前IP地址
            String currentIp = getCurrentIpAddress();
            tvCurrentIp.setText("当前IP: " + (currentIp != null ? currentIp : "未连接"));
            
            // 获取WiFi信息
            WifiManager wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    int gateway = wifiInfo.getIpAddress();
                    String gatewayIp = String.format("%d.%d.%d.%d",
                        (gateway & 0xff),
                        (gateway >> 8 & 0xff),
                        (gateway >> 16 & 0xff),
                        (gateway >> 24 & 0xff));
                    tvCurrentGateway.setText("当前网关: " + gatewayIp);
                }
            }
            
            tvCurrentDns.setText("DNS: 系统默认");
            
        } catch (Exception e) {
            e.printStackTrace();
            tvCurrentIp.setText("当前IP: 获取失败");
            tvCurrentGateway.setText("当前网关: 获取失败");
            tvCurrentDns.setText("DNS: 获取失败");
        }
    }
    
    private String getCurrentIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private void saveIpConfiguration() {
        if (radioDhcp.isChecked()) {
            // DHCP配置
            Toast.makeText(getContext(), "DHCP配置已保存", Toast.LENGTH_SHORT).show();
        } else if (radioStatic.isChecked()) {
            // 静态IP配置
            String ipAddress = etIpAddress.getText().toString().trim();
            String subnetMask = etSubnetMask.getText().toString().trim();
            String gateway = etGateway.getText().toString().trim();
            String dns1 = etDns1.getText().toString().trim();
            String dns2 = etDns2.getText().toString().trim();
            
            // 验证输入
            if (TextUtils.isEmpty(ipAddress) || !isValidIp(ipAddress)) {
                Toast.makeText(getContext(), "请输入有效的IP地址", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (TextUtils.isEmpty(subnetMask) || !isValidIp(subnetMask)) {
                Toast.makeText(getContext(), "请输入有效的子网掩码", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (TextUtils.isEmpty(gateway) || !isValidIp(gateway)) {
                Toast.makeText(getContext(), "请输入有效的网关地址", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!TextUtils.isEmpty(dns1) && !isValidIp(dns1)) {
                Toast.makeText(getContext(), "请输入有效的DNS1地址", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!TextUtils.isEmpty(dns2) && !isValidIp(dns2)) {
                Toast.makeText(getContext(), "请输入有效的DNS2地址", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 这里应该调用系统API设置静态IP，但需要系统权限
            Toast.makeText(getContext(), "静态IP配置已保存\n" +
                "IP: " + ipAddress + "\n" +
                "子网掩码: " + subnetMask + "\n" +
                "网关: " + gateway, Toast.LENGTH_LONG).show();
        }
    }
    
    private boolean isValidIp(String ip) {
        return IP_PATTERN.matcher(ip).matches();
    }
}