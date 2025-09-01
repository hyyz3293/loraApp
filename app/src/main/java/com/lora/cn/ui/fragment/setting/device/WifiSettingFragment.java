package com.lora.cn.ui.fragment.setting.device;

import android.content.Context;
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
    
    public static WifiSettingFragment newInstance() {
        return new WifiSettingFragment();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wifi_setting, container, false);
        
        initViews(view);
        initListeners();
        initWifiData();
        
        return view;
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
            wifiPwdLock.setImageResource(R.mipmap.com_eye); // 需要添加睁眼图标
            isPasswordVisible = true;
        }
        // 保持光标位置
        wifiPwd.setSelection(wifiPwd.getText().length());
    }
    
    private void showWifiListDialog() {
        // 创建WiFi列表对话框
        DialogUtils.showWifiListDialog(getContext(), wifiList, new DialogUtils.OnWifiSelectedListener() {
            @Override
            public void onWifiSelected(WifiItem wifiItem) {
                wifiName.setText(wifiItem.getSsid());
                wifiPwd.setText(""); // 清空密码
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
    
    private void initWifiData() {
        // 初始化WiFi列表数据（模拟数据）
        wifiList.clear();
        wifiList.add(new WifiItem("Office_WiFi", -30, true));
        wifiList.add(new WifiItem("Home_Network", -45, true));
        wifiList.add(new WifiItem("Guest_WiFi", -60, false));
        wifiList.add(new WifiItem("Mobile_Hotspot", -70, true));
        
        // 设置默认WiFi名称
        if (!wifiList.isEmpty()) {
            wifiName.setText(wifiList.get(0).getSsid());
        }
    }
}