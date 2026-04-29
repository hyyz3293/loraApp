package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blankj.utilcode.util.LogUtils;
import com.lora.cn.R;
import com.lora.cn.network.MqttPacketsClient;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.ui.model.Terminal;
import com.lora.cn.ui.activity.MainActivity;
import com.lora.cn.utils.DownlinkMessageHelper;

public class DownlinkTestFragment extends Fragment {
    private EditText etDevId;
    private Spinner spTerminal;
    private EditText etDepartmentId;
    private EditText etCartId;
    private EditText etIntervalMin;
    private EditText etClearMask;
    private EditText etAlarmCount;
    private EditText etAlarmMinutes;
    private EditText etRawHex;
    private EditText etBcdTime;
    private EditText etRes2_4b;
    private EditText etRes3_4b;
    private EditText etRes4_2b;
    private EditText etLowBattery;
    private EditText etRes9_1b;
    private EditText etRes10_1b;
    private Spinner spAckResult;
    private Spinner spQueryOp;
    private Spinner spRegisterResult;

    private DownlinkMessageHelper helper;
    private java.util.List<Terminal> terminalList = new java.util.ArrayList<>();
    private java.util.concurrent.ExecutorService ioExecutor;
    private android.os.Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_downlink_test, container, false);
        etDevId = v.findViewById(R.id.et_dev_id);
        spTerminal = v.findViewById(R.id.sp_terminal);
        etDepartmentId = v.findViewById(R.id.et_department_id);
        etCartId = v.findViewById(R.id.et_cart_id);
        etIntervalMin = v.findViewById(R.id.et_interval_min);
        etClearMask = v.findViewById(R.id.et_clear_mask);
        etAlarmCount = v.findViewById(R.id.et_alarm_count);
        etAlarmMinutes = v.findViewById(R.id.et_alarm_minutes);
        etRawHex = v.findViewById(R.id.et_raw_hex);
        etBcdTime = v.findViewById(R.id.et_bcd_time);
        etRes2_4b = v.findViewById(R.id.et_res2_4b);
        etRes3_4b = v.findViewById(R.id.et_res3_4b);
        etRes4_2b = v.findViewById(R.id.et_res4_2b);
        etLowBattery = v.findViewById(R.id.et_low_battery);
        etRes9_1b = v.findViewById(R.id.et_res9_1b);
        etRes10_1b = v.findViewById(R.id.et_res10_1b);
        spAckResult = v.findViewById(R.id.sp_ack_result);
        spQueryOp = v.findViewById(R.id.sp_query_op);
        spRegisterResult = v.findViewById(R.id.sp_register_result);

        MqttPacketsClient client = null;
        if (getActivity() instanceof MainActivity) {
            client = ((MainActivity) getActivity()).getMqttClient();
        }
        if (client == null) {
            client = new MqttPacketsClient();
        }
        helper = new DownlinkMessageHelper(client);
        if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
        if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        try {
            DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
            terminalList = db.getAllTerminals();
            java.util.List<String> names = new java.util.ArrayList<>();
            for (Terminal t : terminalList) {
                names.add((t.getTerminalName() == null ? "" : t.getTerminalName()) + " (" + (t.getTerminalId() == null ? "" : t.getTerminalId()) + ")");
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_12dp, names);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_12dp);
            spTerminal.setAdapter(adapter);
            spTerminal.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    Terminal sel = position >= 0 && position < terminalList.size() ? terminalList.get(position) : null;
                    if (sel != null) {
                        String dev = sel.getTerminalId() != null ? sel.getTerminalId().trim().replace(" ", "") : "";
                        if (etDevId != null) etDevId.setText(dev);
                        int dep = (int) Math.max(0, Math.min(255, sel.getDepartmentId()));
                        int cart = (int) Math.max(0, Math.min(255, sel.getRoomId()));
                        etDepartmentId.setText(String.valueOf(dep));
                        etCartId.setText(String.valueOf(cart));
                        int intervalSet = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
                        if (TextUtils.isEmpty(etIntervalMin.getText())) etIntervalMin.setText(String.valueOf(Math.max(3, Math.min(1440, intervalSet))));
                        if (TextUtils.isEmpty(etClearMask.getText())) etClearMask.setText("00000000");
                        if (spAckResult != null) spAckResult.setSelection(0);
                        if (spQueryOp != null) spQueryOp.setSelection(0);
                        if (spRegisterResult != null) spRegisterResult.setSelection(1);
                        if (TextUtils.isEmpty(etAlarmCount.getText())) etAlarmCount.setText("1");
                        int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
                        int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
                        int mins = Math.max(0, Math.min(1440, h * 60 + m));
                        if (TextUtils.isEmpty(etAlarmMinutes.getText())) etAlarmMinutes.setText(String.valueOf(mins));
                    }
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        } catch (Exception ignored) {}

        try {
            String[] ackOpts = getResources().getStringArray(R.array.ack_result_options);
            ArrayAdapter<String> ackAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_12dp, java.util.Arrays.asList(ackOpts));
            ackAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_12dp);
            spAckResult.setAdapter(ackAdapter);

            String[] queryOpts = getResources().getStringArray(R.array.query_op_options);
            ArrayAdapter<String> queryAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_12dp, java.util.Arrays.asList(queryOpts));
            queryAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_12dp);
            spQueryOp.setAdapter(queryAdapter);

            String[] regOpts = getResources().getStringArray(R.array.register_result_options);
            ArrayAdapter<String> regAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_12dp, java.util.Arrays.asList(regOpts));
            regAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_12dp);
            spRegisterResult.setAdapter(regAdapter);
        } catch (Exception ignored) {}

        prefillDefaults();

        View btnAck = v.findViewById(R.id.btn_send_ack);
        View btnQuery = v.findViewById(R.id.btn_send_query);
        View btnConfig = v.findViewById(R.id.btn_send_config);
        View btnClear = v.findViewById(R.id.btn_send_clear);
        View btnPresetAck = v.findViewById(R.id.btn_preset_ack_success);
        View btnPresetQuery = v.findViewById(R.id.btn_preset_query_routine);
        View btnPresetConfig = v.findViewById(R.id.btn_preset_config_60_two_alarms);
        View btnPresetClear = v.findViewById(R.id.btn_preset_clear_all);
        View btnSendRawHex = v.findViewById(R.id.btn_send_raw_hex);
        View btnBuildHex = v.findViewById(R.id.btn_build_hex);

        btnAck.setOnClickListener(view -> {
            String dev = getDevId();
            if (dev == null) return;
            int ack = getSpinnerInt(spAckResult);
            int[] alarms = parseAlarms();
            int ac = getAlarmCount(alarms);
            ioExecutor.execute(() -> {
                try {
                    int interval = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
                    helper.sendDownlink8001(
                            dev,
                            ack,
                            0,
                            0,
                            0,
                            0,
                            0,
                            Math.max(3, Math.min(1440, interval)),
                            ac,
                            alarms,
                            true
                    );
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "已发送应答下行", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "发送失败", Toast.LENGTH_SHORT).show());
                }
            });
        });
        btnQuery.setOnClickListener(view -> {
            String dev = getDevId();
            if (dev == null) return;
            int[] alarms = parseAlarms();
            int ac = getAlarmCount(alarms);
            ioExecutor.execute(() -> {
                try {
                    int interval = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
                    helper.sendDownlink8001(
                            dev,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            Math.max(3, Math.min(1440, interval)),
                            ac,
                            alarms,
                            true
                    );
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "已发送查询下行", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "发送失败", Toast.LENGTH_SHORT).show());
                }
            });
        });
        btnConfig.setOnClickListener(view -> {
            String dev = getDevId();
            if (dev == null) return;
            int dep = parseInt(etDepartmentId, 0);
            int cart = parseInt(etCartId, 0);
            int interval = clamp(parseInt(etIntervalMin, 60), 5, 1440);
            int[] alarms = parseAlarms();
            int ac = getAlarmCount(alarms);
            ioExecutor.execute(() -> {
                try {
                    helper.sendDownlink8001(
                            dev,
                            1,
                            0,
                            dep,
                            cart,
                            0,
                            0,
                            interval,
                            ac,
                            alarms,
                            true
                    );
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "已发送配置下行", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "发送失败", Toast.LENGTH_SHORT).show());
                }
            });
        });
        btnClear.setOnClickListener(view -> {
            String dev = getDevId();
            if (dev == null) return;
            int clearMask = parseHexInt(etClearMask, 0);
            int[] alarms = parseAlarms();
            int ac = getAlarmCount(alarms);
            ioExecutor.execute(() -> {
                try {
                    helper.sendDownlink8001(
                            dev,
                            1,
                            1,
                            0,
                            0,
                            0,
                            clearMask,
                            Math.max(3, Math.min(1440, com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3))),
                            ac,
                            alarms,
                            true
                    );
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "已发送清除下行", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "发送失败", Toast.LENGTH_SHORT).show());
                }
            });
        });
        btnPresetAck.setOnClickListener(view -> {
            String dev = getDevId();
            if (dev == null) return;
            ioExecutor.execute(() -> {
                try {
                    int interval = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
                    helper.sendDownlink8001(
                            dev,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            Math.max(3, Math.min(1440, interval)),
                            0,
                            null,
                            true
                    );
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "预设应答成功已发送", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "发送失败", Toast.LENGTH_SHORT).show());
                }
            });
        });
        btnPresetQuery.setOnClickListener(view -> {
            String dev = getDevId();
            if (dev == null) return;
            int[] alarms = new int[]{480};
            ioExecutor.execute(() -> {
                try {
                    int interval = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
                    helper.sendDownlink8001(
                            dev,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            Math.max(3, Math.min(1440, interval)),
                            1,
                            alarms,
                            true
                    );
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "预设例行查询已发送", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "发送失败", Toast.LENGTH_SHORT).show());
                }
            });
        });
        btnPresetConfig.setOnClickListener(view -> {
            String dev = getDevId();
            if (dev == null) return;
            int[] alarms = new int[]{480, 1215};
            ioExecutor.execute(() -> {
                try {
                    helper.sendDownlink8001(
                            dev,
                            1,
                            0,
                            0,
                            0,
                            0,
                            0,
                            60,
                            2,
                            alarms,
                            true
                    );
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "预设配置60分钟+两闹钟已发送", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "发送失败", Toast.LENGTH_SHORT).show());
                }
            });
        });
        btnPresetClear.setOnClickListener(view -> {
            String dev = getDevId();
            if (dev == null) return;
            int clearMask = 0x00000005;
            ioExecutor.execute(() -> {
                try {
                    helper.sendDownlink8001(
                            dev,
                            1,
                            1,
                            0,
                            0,
                            0,
                            clearMask,
                            Math.max(3, Math.min(1440, com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3))),
                            0,
                            null,
                            true
                    );
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "预设清除全部告警已发送", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "发送失败", Toast.LENGTH_SHORT).show());
                }
            });
        });
        btnBuildHex.setOnClickListener(view -> {
            String dev = getDevId();
            if (dev == null) return;
            long utcMs = System.currentTimeMillis();
            try {
                String t = etBcdTime.getText() != null ? etBcdTime.getText().toString().trim() : "";
                if (!TextUtils.isEmpty(t) && t.matches("\\d{14}")) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault());
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    java.util.Date d = sdf.parse(t);
                    if (d != null) utcMs = d.getTime();
                }
            } catch (Exception ignored) {}
            final long utcMsFinal = utcMs;
            int ack = getSpinnerInt(spAckResult);
            int qop = getSpinnerInt(spQueryOp);
            int dep = parseInt(etDepartmentId, 0);
            int cart = parseInt(etCartId, 0);
            int clearMask = parseHexInt(etClearMask, 0);
            int interval = clamp(parseInt(etIntervalMin, 60), 5, 1440);
            int[] alarms = parseAlarms();
            int ac = getAlarmCount(alarms);
            int res2 = parseHexInt(etRes2_4b, 0xFFFFFFFF);
            int res3 = parseHexInt(etRes3_4b, 0xFFFFFFFF);
            int res4 = parseHexInt(etRes4_2b, 0xFFFF);
            int lowBat = clamp(parseInt(etLowBattery, 0), 0, 100);
            int res9 = parseHexInt(etRes9_1b, 0xFF) & 0xFF;
            int res10 = parseHexInt(etRes10_1b, 0xFF) & 0xFF;
            ioExecutor.execute(() -> {
                try {
                    byte[] frame = com.lora.cn.utils.LoRaProtocolParser.buildDownlink8001Full(
                            dev,
                            (byte) (System.currentTimeMillis() & 0xFF),
                            utcMsFinal,
                            res2,
                            res3,
                            res4,
                            lowBat,
                            ack,
                            dep,
                            cart,
                            res9,
                            res10,
                            qop,
                            clearMask,
                            interval,
                            ac,
                            alarms
                    );
                    String hex = com.lora.cn.utils.LoRaProtocolParser.bytesToHex(frame).replaceAll("\\s+", "");
                    if (mainHandler != null) mainHandler.post(() -> {
                        etRawHex.setText(hex);
                        LogUtils.e("生成成功::=====" + hex);

                        Toast.makeText(requireContext(), "已生成HEX", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    LogUtils.e("生成失败:::=====" + e);
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "生成失败", Toast.LENGTH_SHORT).show());
                }
            });
        });
        btnSendRawHex.setOnClickListener(view -> {
            String dev = getDevId();
            if (dev == null) return;
            String hex = etRawHex.getText() != null ? etRawHex.getText().toString().trim() : "";
            if (TextUtils.isEmpty(hex)) {
                Toast.makeText(requireContext(), "请输入原始HEX数据", Toast.LENGTH_SHORT).show();
                return;
            }
            ioExecutor.execute(() -> {
                try {
                    helper.sendRawHexDownlink(dev, hex);
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "已发送原始HEX下行", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    if (mainHandler != null) mainHandler.post(() -> Toast.makeText(requireContext(), "发送失败", Toast.LENGTH_SHORT).show());
                }
            });
        });
        return v;
    }

    private void prefillDefaults() {
        try {
            if (spTerminal != null && terminalList != null && !terminalList.isEmpty()) {
                spTerminal.setSelection(0);
            }
            int intervalSet = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
            if (etIntervalMin != null && TextUtils.isEmpty(etIntervalMin.getText())) {
                etIntervalMin.setText(String.valueOf(Math.max(3, Math.min(1440, intervalSet))));
            }
            if (etClearMask != null && TextUtils.isEmpty(etClearMask.getText())) {
                etClearMask.setText("00000000");
            }
            if (spAckResult != null) spAckResult.setSelection(0);
            if (spQueryOp != null) spQueryOp.setSelection(0);
            if (spRegisterResult != null) spRegisterResult.setSelection(1);
            int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
            int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
            int mins = Math.max(0, Math.min(1440, h * 60 + m));
            if (etAlarmCount != null && TextUtils.isEmpty(etAlarmCount.getText())) {
                etAlarmCount.setText("1");
            }
            if (etAlarmMinutes != null && TextUtils.isEmpty(etAlarmMinutes.getText())) {
                etAlarmMinutes.setText(String.valueOf(mins));
            }
            int low = com.lora.cn.utils.DownlinkMessageHelper.getLowBatteryThresholdPercent();
            if (etLowBattery != null && TextUtils.isEmpty(etLowBattery.getText())) {
                etLowBattery.setText(String.valueOf(low));
            }
            if (etRes2_4b != null && TextUtils.isEmpty(etRes2_4b.getText())) etRes2_4b.setText("FFFFFFFF");
            if (etRes3_4b != null && TextUtils.isEmpty(etRes3_4b.getText())) etRes3_4b.setText("FFFFFFFF");
            if (etRes4_2b != null && TextUtils.isEmpty(etRes4_2b.getText())) etRes4_2b.setText("FFFF");
            if (etRes9_1b != null && TextUtils.isEmpty(etRes9_1b.getText())) etRes9_1b.setText("FF");
            if (etRes10_1b != null && TextUtils.isEmpty(etRes10_1b.getText())) etRes10_1b.setText("FF");
            try {
                String nowUtcBcd = new java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault())
                        .format(new java.util.Date());
                if (etBcdTime != null && TextUtils.isEmpty(etBcdTime.getText())) {
                    etBcdTime.setText(nowUtcBcd);
                }
            } catch (Exception ignored) {}
            if (etRawHex != null && TextUtils.isEmpty(etRawHex.getText())) etRawHex.setText("");
        } catch (Exception ignored) {}
    }

    private String getDevId() {
        String dev = etDevId.getText() != null ? etDevId.getText().toString().trim().replace(" ", "") : "";
        if (TextUtils.isEmpty(dev) || dev.length() != 16) {
            Toast.makeText(requireContext(), "请输入16位HEX设备ID", Toast.LENGTH_SHORT).show();
            return null;
        }
        return dev;
    }

    private int getSpinnerInt(Spinner s) {
        Object item = s.getSelectedItem();
        if (item == null) return 0;
        String v = String.valueOf(item).replaceAll("[^0-9-]", "");
        if (TextUtils.isEmpty(v)) return 0;
        try { return Integer.parseInt(v); } catch (Exception ignored) { return 0; }
    }

    private int parseInt(EditText et, int def) {
        String t = et.getText() != null ? et.getText().toString().trim() : "";
        if (TextUtils.isEmpty(t)) return def;
        try { return Integer.parseInt(t); } catch (Exception ignored) { return def; }
    }

    private int parseHexInt(EditText et, int def) {
        String t = et.getText() != null ? et.getText().toString().trim() : "";
        if (TextUtils.isEmpty(t)) return def;
        try { return Integer.parseInt(t.replaceAll("[^0-9A-Fa-f]", ""), 16); } catch (Exception ignored) { return def; }
    }

    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private int[] parseAlarms() {
        String t = etAlarmMinutes.getText() != null ? etAlarmMinutes.getText().toString().trim() : "";
        if (TextUtils.isEmpty(t)) return new int[0];
        String[] parts = t.split("[,\\s]+");
        int[] out = new int[Math.min(2, parts.length)];
        int idx = 0;
        for (String p : parts) {
            if (idx >= 2) break;
            try {
                int m = Integer.parseInt(p);
                out[idx++] = clamp(m, 0, 1440);
            } catch (Exception ignored) {}
        }
        if (idx < out.length) {
            int[] res = new int[idx];
            System.arraycopy(out, 0, res, 0, idx);
            return res;
        }
        return out;
    }

    private int getAlarmCount(int[] arr) {
        int c = arr == null ? 0 : Math.min(2, arr.length);
        int inputCount = parseInt(etAlarmCount, c);
        return Math.max(0, Math.min(2, inputCount));
    }
}
