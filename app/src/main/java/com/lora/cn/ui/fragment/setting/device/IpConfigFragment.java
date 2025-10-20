package com.lora.cn.ui.fragment.setting.device;

import android.content.Context;
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
import com.blankj.utilcode.util.SPUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class IpConfigFragment extends Fragment {

    public static IpConfigFragment newInstance() {
        return new IpConfigFragment();
    }

    private TextView backBtn;
    private TextView connectBtn;
    private TextView autoFillBtn;
    private EditText ip1;
    private EditText ip2;
    private EditText ip3;
    private EditText ip4;
    private TextView loginGatewayBtn;
    private EditText etGatewayUsername;
    private EditText etGatewayPassword;

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
        backBtn = view.findViewById(R.id.back);
        connectBtn = view.findViewById(R.id.connect);
        autoFillBtn = view.findViewById(R.id.auto_fill);
        ip1 = view.findViewById(R.id.ip1);
        ip2 = view.findViewById(R.id.ip2);
        ip3 = view.findViewById(R.id.ip3);
        ip4 = view.findViewById(R.id.ip4);
        loginGatewayBtn = view.findViewById(R.id.login_gateway);
        etGatewayUsername = view.findViewById(R.id.et_gateway_username);
        etGatewayPassword = view.findViewById(R.id.et_gateway_password);
    }

    private void initListener() {
        // 返回按钮
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            }
        });

        // 连接按钮：校验并保存网关IP
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s1 = safeText(ip1);
                String s2 = safeText(ip2);
                String s3 = safeText(ip3);
                String s4 = safeText(ip4);

                if (!isValidOctet(s1) || !isValidOctet(s2) || !isValidOctet(s3) || !isValidOctet(s4)) {
                    Toast.makeText(requireContext(), "请输入有效的IP地址", Toast.LENGTH_SHORT).show();
                    return;
                }

                String gatewayIp = s1 + "." + s2 + "." + s3 + "." + s4;
                SPUtils.getInstance().put("gateway_ip", gatewayIp);

                // 同时保存账号密码（如已填写）
                String username = safeText(etGatewayUsername);
                String password = safeText(etGatewayPassword);
                if (!TextUtils.isEmpty(username)) {
                    SPUtils.getInstance().put("gateway_username", username);
                }
                if (!TextUtils.isEmpty(password)) {
                    SPUtils.getInstance().put("gateway_password", password);
                }

                Toast.makeText(requireContext(), "网关IP已保存: " + gatewayIp, Toast.LENGTH_SHORT).show();
            }
        });

        // 登录网关按钮：打开WebView进行登录并保存Cookie
        loginGatewayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s1 = safeText(ip1);
                String s2 = safeText(ip2);
                String s3 = safeText(ip3);
                String s4 = safeText(ip4);
                if (!isValidOctet(s1) || !isValidOctet(s2) || !isValidOctet(s3) || !isValidOctet(s4)) {
                    Toast.makeText(requireContext(), "请先填写有效的网关IP", Toast.LENGTH_SHORT).show();
                    return;
                }
                String gatewayIp = s1 + "." + s2 + "." + s3 + "." + s4;
                SPUtils.getInstance().put("gateway_ip", gatewayIp);

                String url = "http://" + gatewayIp + "/#networkserver/packets";
                com.lora.cn.ui.activity.WebViewActivity.start(requireContext(), url, "网关抓包页");
            }
        });

        // 自动获取按钮：主动获取并填充网关IP
        autoFillBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoFillGatewayIpIfAvailable();
            }
        });
    }

    private void loadCurrentNetworkInfo() {
        // 加载已保存的网关IP到输入框
        String savedIp = SPUtils.getInstance().getString("gateway_ip", "");
        if (!TextUtils.isEmpty(savedIp) && savedIp.contains(".")) {
            String[] parts = savedIp.split("\\.");
            if (parts.length == 4) {
                ip1.setText(parts[0]);
                ip2.setText(parts[1]);
                ip3.setText(parts[2]);
                ip4.setText(parts[3]);
            }
        }

        // 加载已保存的账号密码
        String savedUsername = SPUtils.getInstance().getString("gateway_username", "");
        String savedPassword = SPUtils.getInstance().getString("gateway_password", "");
        if (!TextUtils.isEmpty(savedUsername)) etGatewayUsername.setText(savedUsername);
        if (!TextUtils.isEmpty(savedPassword)) etGatewayPassword.setText(savedPassword);

        // 若未保存或当前输入为空，尝试自动获取WiFi网关并填充
        boolean inputsEmpty = TextUtils.isEmpty(safeText(ip1))
                && TextUtils.isEmpty(safeText(ip2))
                && TextUtils.isEmpty(safeText(ip3))
                && TextUtils.isEmpty(safeText(ip4));
        if (inputsEmpty) {
            autoFillGatewayIpIfAvailable();
        }
    }

    private String safeText(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private boolean isValidOctet(String s) {
        if (TextUtils.isEmpty(s)) return false;
        try {
            int v = Integer.parseInt(s);
            return v >= 0 && v <= 255;
        } catch (Exception e) {
            return false;
        }
    }

    // 自动获取WiFi的网关IP（优先DHCP网关，其次推断为X.Y.Z.1）
    private void autoFillGatewayIpIfAvailable() {
        try {
            String gateway = getWifiGateway();
            if (!TextUtils.isEmpty(gateway) && gateway.contains(".")) {
                String[] parts = gateway.split("\\.");
                if (parts.length == 4) {
                    ip1.setText(parts[0]);
                    ip2.setText(parts[1]);
                    ip3.setText(parts[2]);
                    ip4.setText(parts[3]);
                    Toast.makeText(requireContext(), "已自动获取WiFi网关IP: " + gateway, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String getWifiGateway() {
        try {
            WifiManager wm = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return null;
            android.net.DhcpInfo dhcp = wm.getDhcpInfo();
            if (dhcp != null && dhcp.gateway != 0) {
                return intToIp(dhcp.gateway);
            }
            WifiInfo wifiInfo = wm.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getIpAddress() != 0) {
                String ip = intToIp(wifiInfo.getIpAddress());
                String[] p = ip.split("\\.");
                if (p.length == 4) return p[0] + "." + p[1] + "." + p[2] + ".1";
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String intToIp(int ip) {
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
    }

}