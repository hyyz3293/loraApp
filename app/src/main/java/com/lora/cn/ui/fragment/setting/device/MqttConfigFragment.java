package com.lora.cn.ui.fragment.setting.device;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.R;
import com.lora.cn.service.MqttBrokerService;

/**
 * MQTT 设置页：配置 broker、主题过滤、用户名/密码、证书信任选项。
 * 值保存到 SharedPreferences，键：
 * - mqtt_broker_url
 * - mqtt_topic_filter
 * - mqtt_username
 * - mqtt_password
 * - mqtt_trust_all_certs (boolean)
 */
public class MqttConfigFragment extends Fragment {

    public static MqttConfigFragment newInstance() {
        return new MqttConfigFragment();
    }

    private TextView backBtn;
    private TextView saveBtn;
    private EditText etBrokerUrl;
    private EditText etTopicFilter;
    private EditText etUsername;
    private EditText etPassword;
    private CheckBox cbTrustAll;
    private CheckBox cbLocalBrokerEnabled;
    private EditText etLocalBrokerPort;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mqtt_config, container, false);
        initViews(view);
        loadSavedValues();
        initListeners();
        return view;
    }

    private void initViews(@NonNull View view) {
        backBtn = view.findViewById(R.id.back);
        saveBtn = view.findViewById(R.id.save);
        etBrokerUrl = view.findViewById(R.id.et_broker_url);
        etTopicFilter = view.findViewById(R.id.et_topic_filter);
        etUsername = view.findViewById(R.id.et_username);
        etPassword = view.findViewById(R.id.et_password);
        cbTrustAll = view.findViewById(R.id.cb_trust_all);
        cbLocalBrokerEnabled = view.findViewById(R.id.cb_local_broker_enabled);
        etLocalBrokerPort = view.findViewById(R.id.et_local_broker_port);
    }

    private void loadSavedValues() {
        SPUtils sp = SPUtils.getInstance();
        String broker = sp.getString("mqtt_broker_url", "");
        String topic = sp.getString("mqtt_topic_filter", "/milesight/uplink/#");
        String user = sp.getString("mqtt_username", "");
        String pass = sp.getString("mqtt_password", "");
        boolean trust = sp.getBoolean("mqtt_trust_all_certs", false);
        boolean localEnabled = sp.getBoolean("mqtt_local_broker_enabled", true);
        int localPort = sp.getInt("mqtt_local_broker_port", 1883);

        if (TextUtils.isEmpty(broker)) {
            // 尝试根据网关IP填充默认 broker（局域网）
            String gw = sp.getString("gateway_ip", "");
            if (!TextUtils.isEmpty(gw)) {
                broker = "tcp://" + gw + ":1883";
            }
        }

        etBrokerUrl.setText(broker);
        etTopicFilter.setText(TextUtils.isEmpty(topic) ? "/milesight/uplink/#" : topic);
        etUsername.setText(user);
        etPassword.setText(pass);
        cbTrustAll.setChecked(trust);
        cbLocalBrokerEnabled.setChecked(localEnabled);
        etLocalBrokerPort.setText(String.valueOf(localPort));
    }

    private void initListeners() {
        backBtn.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });
        saveBtn.setOnClickListener(v -> saveValues());
    }

    private void saveValues() {
        String broker = safeText(etBrokerUrl);
        String topic = safeText(etTopicFilter);
        String user = safeText(etUsername);
        String pass = safeText(etPassword);
        boolean trust = cbTrustAll.isChecked();
        boolean localEnabled = cbLocalBrokerEnabled != null && cbLocalBrokerEnabled.isChecked();
        int localPort = 1883;
        try {
            localPort = Integer.parseInt(safeText(etLocalBrokerPort));
        } catch (Exception ignored) {}

        if (TextUtils.isEmpty(broker)) {
            Toast.makeText(requireContext(), "请填写MQTT Broker地址", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!(broker.startsWith("tcp://") || broker.startsWith("ssl://"))) {
            Toast.makeText(requireContext(), "Broker需以tcp://或ssl://开头", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(topic)) {
            topic = "/milesight/uplink/#";
        }

        SPUtils sp = SPUtils.getInstance();
        sp.put("mqtt_broker_url", broker);
        sp.put("mqtt_topic_filter", topic);
        sp.put("mqtt_username", user);
        sp.put("mqtt_password", pass);
        sp.put("mqtt_trust_all_certs", trust);
        sp.put("mqtt_local_broker_enabled", localEnabled);
        sp.put("mqtt_local_broker_port", localPort > 0 ? localPort : 1883);

        try {
            android.content.Intent svc = new android.content.Intent(requireContext(), com.lora.cn.service.MqttBrokerService.class);
            svc.putExtra("port", localPort > 0 ? localPort : 1883);
            androidx.core.content.ContextCompat.startForegroundService(requireContext(), svc);
            if (localEnabled) {
                String ipSummary = getIpSummary();
                Toast.makeText(requireContext(), "本地MQTT服务端已启动: IP=" + (ipSummary.isEmpty()?"未知":ipSummary) + ", 端口=" + (localPort>0?localPort:1883), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), "使用外部Broker，服务已保持订阅接收", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {}

        Toast.makeText(requireContext(), "MQTT设置已保存", Toast.LENGTH_SHORT).show();
    }

    private String safeText(EditText et) {
        return et == null ? "" : et.getText().toString().trim();
    }

    private String getIpSummary() {
        try {
            java.util.List<String> ips = new java.util.LinkedList<>();
            java.util.Enumeration<java.net.NetworkInterface> en = java.net.NetworkInterface.getNetworkInterfaces();
            for (java.net.NetworkInterface nif : java.util.Collections.list(en)) {
                if (!nif.isUp() || nif.isLoopback()) continue;
                for (java.net.InetAddress addr : java.util.Collections.list(nif.getInetAddresses())) {
                    if (addr.isLoopbackAddress()) continue;
                    String host = addr.getHostAddress();
                    if (host != null && host.indexOf(':') < 0) {
                        ips.add(host);
                    }
                }
            }
            return String.join(",", ips);
        } catch (Exception e) {
            return "";
        }
    }
}
