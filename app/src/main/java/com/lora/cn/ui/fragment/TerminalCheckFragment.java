package com.lora.cn.ui.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.User;
import com.lora.cn.ui.adapter.TerminalChartAdapter;
import com.lora.cn.ui.model.ChartItem;
import com.lora.cn.ui.model.TerminalChartData;
import com.lora.cn.ui.view.PieChartView;
import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.database.DatabaseHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TerminalCheckFragment extends Fragment {

    private PieChartView pieChartOnline;
    private PieChartView pieChartBattery;
    private TextView terminalRemainingNumber;
    private TextView terminalClearTime;
    private TextView btnExportExcel;
    private TextView addTerminal;
    private RecyclerView terminalCheckRecycle;
    
    // 新增适配器
    private TerminalChartAdapter terminalChartAdapter;
    private java.util.List<com.lora.cn.ui.model.TerminalChartData> chartDataList = new java.util.ArrayList<>();
    
    // 数据字段
    private int remainingCount = 1;
    private boolean isChecking = false;
    private boolean isAdmin = false;
    
    // 权限相关
    private DatabaseManager databaseManager;
    private int currentUserRoleId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal_check, container, false);
        
        // 初始化数据库管理器
        databaseManager = DatabaseManager.getInstance(requireContext());
        
        // 初始化用户角色ID
        long userId = SPUtils.getInstance().getLong("current_user_id", -1);
        if (userId != -1) {
            User user = databaseManager.getUserById(userId);
            if (user != null) {
                currentUserRoleId = (int)user.getRoleId();
                try {
                    com.lora.cn.database.entity.Role role = databaseManager.getRoleById(currentUserRoleId);
                    isAdmin = (role != null && "管理员".equals(role.getRoleName()));
                } catch (Exception ignored) {}
            }
        }
        
        initViews(view);
        initData();
        initListeners();
        
        return view;
    }
    
    private void initViews(View view) {
        pieChartOnline = view.findViewById(R.id.pie_chart_online);
        pieChartBattery = view.findViewById(R.id.pie_chart_battery);
        terminalRemainingNumber = view.findViewById(R.id.terminal_remaining_number);
        terminalClearTime = view.findViewById(R.id.terminal_clear_time);
        btnExportExcel = view.findViewById(R.id.btn_export_excel);
        addTerminal = view.findViewById(R.id.add_terminal);
        terminalCheckRecycle = view.findViewById(R.id.terminal_check_recycle);
        
        // 初始化RecyclerView
        initRecyclerView();
    }
    
    private void initRecyclerView() {
        terminalChartAdapter = new TerminalChartAdapter();
        terminalCheckRecycle.setLayoutManager(new LinearLayoutManager(getContext()));
        terminalCheckRecycle.setAdapter(terminalChartAdapter);
        try { terminalCheckRecycle.setNestedScrollingEnabled(false); } catch (Throwable ignored) {}
    }
    
    private void initData() {
        // 更新UI数据（管理员不显示剩余次数）
        if (isAdmin) {
            android.view.View parent = (android.view.View) terminalRemainingNumber.getParent();
            if (parent != null) parent.setVisibility(android.view.View.GONE);
        } else {
            updateRemainingCount(remainingCount);
        }
        updateClearTime();
        
        // 初始化饼图数据
        initPieChartData();
        
        // 初始化图表适配器数据
        initChartAdapterData();
    }
    
    private void initListeners() {
        // 开始清点按钮点击事件
        addTerminal.setOnClickListener(v -> {
            if (!hasPermission("clean_start_count")) {
                Toast.makeText(getContext(), "您没有终端清点的权限", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!isAdmin && remainingCount <= 0) {
                Toast.makeText(getContext(), "今日清点次数已用完", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (isChecking) {
                Toast.makeText(getContext(), "正在清点中，请稍候...", Toast.LENGTH_SHORT).show();
                return;
            }
            
            startTerminalCheck();
        });
        btnExportExcel.setOnClickListener(v -> exportExcel());
    }
    
    private void initPieChartData() {
        // 使用数据库真实数据初始化饼图
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
            List<com.lora.cn.ui.model.Terminal> terminals = dbHelper.getAllTerminals();
            int online = 0, offline = 0, abnormal = 0;
            int batteryNormal = 0, batteryLow = 0;
            int manualTake = 0; // 正常取走
            for (com.lora.cn.ui.model.Terminal t : terminals) {
                int sc = t.getStatus();
                String st = com.lora.cn.ui.constants.TerminalStatusConstants.codeToText(sc);
                if ("在线".equals(st)) {
                    online++;
                } else if ("异常取走".equals(st)) {
                    abnormal++;
                } else {
                    offline++;
                }
                if (!"离线".equals(st)) {
                    int bl = t.getBatteryLevel();
                    if (bl <= 20) batteryLow++; else batteryNormal++;
                }

                // 解析最近一条上行，统计“正常取走”
                List<com.lora.cn.ui.model.LogInfo> logs = dbHelper.getLogsByTerminalId(t.getTerminalId());
                String hex = null;
                for (com.lora.cn.ui.model.LogInfo li : logs) {
                    String action = li.getAction();
                    if (action != null && action.startsWith("接收上行数据:")) {
                        int idx = action.indexOf(":");
                        if (idx != -1 && idx + 1 < action.length()) { hex = action.substring(idx + 1).trim(); break; }
                    }
                }
                if (hex != null) {
                    com.lora.cn.utils.LoRaFrameParser.ParsedFrame pf = com.lora.cn.utils.LoRaFrameParser.parseFrame(hex);
                    if (pf != null && pf.evManualTake == 1) manualTake++;
                }
            }
            int totalStatus = Math.max(1, online + offline + abnormal + manualTake);
            int totalBattery = Math.max(1, batteryNormal + batteryLow + offline);
            List<PieChartView.PieData> onlineData = new ArrayList<>();
            if (online > 0)
                onlineData.add(new PieChartView.PieData("在线", String.valueOf(online), (online * 100f) / totalStatus, Color.parseColor("#39E56D")));
            if (manualTake > 0)
                onlineData.add(new PieChartView.PieData("正常取走", String.valueOf(manualTake), (manualTake * 100f) / totalStatus, Color.parseColor("#5D75F7")));
            if (abnormal > 0)
                onlineData.add(new PieChartView.PieData("异常", String.valueOf(abnormal), (abnormal * 100f) / totalStatus, Color.parseColor("#D00000")));
            if (offline > 0)
                onlineData.add(new PieChartView.PieData("离线", String.valueOf(offline), (offline * 100f) / totalStatus, Color.parseColor("#CECECE")));
            pieChartOnline.setData(onlineData);

            List<PieChartView.PieData> batteryData = new ArrayList<>();
            if (batteryNormal > 0)
                batteryData.add(new PieChartView.PieData("正常电量", String.valueOf(batteryNormal), (batteryNormal * 100f) / totalBattery, Color.parseColor("#39E56D")));
            if (batteryLow > 0)
                batteryData.add(new PieChartView.PieData("低电量", String.valueOf(batteryLow), (batteryLow * 100f) / totalBattery, Color.parseColor("#FF9500")));
            if (offline > 0)
                batteryData.add(new PieChartView.PieData("离线", String.valueOf(offline), (offline * 100f) / totalBattery, Color.parseColor("#CECECE")));
            pieChartBattery.setData(batteryData);
        } catch (Exception e) {
            Log.e("TerminalCheckFragment", "初始化饼图真实数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 初始化图表适配器数据
     */
    private void initChartAdapterData() {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
            List<com.lora.cn.ui.model.Terminal> terminals = dbHelper.getAllTerminals();
            List<com.lora.cn.ui.model.TerminalChartData> list = new java.util.ArrayList<>();

            // 动态获取所有分组与分类
            java.util.List<com.lora.cn.database.entity.Group> groups = DatabaseManager.getInstance(getContext()).getAllGroups();
            if (groups == null) groups = new java.util.ArrayList<>();
            for (com.lora.cn.database.entity.Group g : groups) {
                int gid = (int) g.getGroupId();
                String prefix = (g.getGroupName() != null ? g.getGroupName() : "分组") + "-";
                java.util.List<com.lora.cn.database.entity.Category> cats = DatabaseManager.getInstance(getContext()).getCategoriesByGroupId(gid);
                if (cats == null) continue;
                for (com.lora.cn.database.entity.Category c : cats) {
                    String label = prefix + c.getCategoryName();
                    int manualTake = 0, illegalLoss = 0;
                    int batteryNormal = 0, batteryLow = 0, batteryOffline = 0;
                    int onlineCount = 0, offlineCount = 0;

                    for (com.lora.cn.ui.model.Terminal t : terminals) {
                        // 动态匹配：优先按分类ID匹配；未设置ID时回退按名称匹配
                        boolean match = (t.getDepartmentId() == c.getCategoryId())
                                || (t.getRoomId() == c.getCategoryId())
                                || (t.getNursingGroupId() == c.getCategoryId())
                                || (t.getOtherId() == c.getCategoryId());
                        if (!match) {
                            String cname = c.getCategoryName();
                            match = (cname != null && cname.equals(t.getDepartment()))
                                    || (cname != null && cname.equals(t.getLocation()));
                        }
                        if (!match) continue;

                        int sc2 = t.getStatus();
                        String st = com.lora.cn.ui.constants.TerminalStatusConstants.codeToText(sc2);
                        if ("在线".equals(st)) { onlineCount++; }
                        else if ("离线".equals(st)) { offlineCount++; batteryOffline++; }
                        else { int bl = t.getBatteryLevel(); if (bl <= 20) batteryLow++; else batteryNormal++; }

                        List<com.lora.cn.ui.model.LogInfo> logs = dbHelper.getLogsByTerminalId(t.getTerminalId());
                        String hex = null;
                        for (com.lora.cn.ui.model.LogInfo li : logs) {
                            String action = li.getAction();
                            if (action != null && action.startsWith("接收上行数据:")) {
                                int idx = action.indexOf(":");
                                if (idx != -1 && idx + 1 < action.length()) { hex = action.substring(idx + 1).trim(); break; }
                            }
                        }
                        if (hex != null) {
                            com.lora.cn.utils.LoRaFrameParser.ParsedFrame pf = com.lora.cn.utils.LoRaFrameParser.parseFrame(hex);
                            if (pf != null) {
                                if (pf.evManualTake == 1) manualTake++;
                                if (pf.evIllegalRemoval == 1) illegalLoss++;
                            }
                        }
                    }

                    com.lora.cn.ui.model.TerminalChartData data = new com.lora.cn.ui.model.TerminalChartData();
                    data.setOnlineTitle(label);
                    data.setBatteryTitle(label);

                    int totalLeft = Math.max(1, onlineCount + offlineCount + manualTake + illegalLoss);
                    java.util.List<com.lora.cn.ui.view.PieChartView.PieData> onlinePie = new java.util.ArrayList<>();
                    if (onlineCount > 0) onlinePie.add(new com.lora.cn.ui.view.PieChartView.PieData("在线", String.valueOf(onlineCount), (onlineCount * 100f) / totalLeft, android.graphics.Color.parseColor("#39E56D")));
                    if (offlineCount > 0) onlinePie.add(new com.lora.cn.ui.view.PieChartView.PieData("离线", String.valueOf(offlineCount), (offlineCount * 100f) / totalLeft, android.graphics.Color.parseColor("#CECECE")));
                    if (manualTake > 0) onlinePie.add(new com.lora.cn.ui.view.PieChartView.PieData("正常取走", String.valueOf(manualTake), (manualTake * 100f) / totalLeft, android.graphics.Color.parseColor("#5D75F7")));
                    if (illegalLoss > 0) onlinePie.add(new com.lora.cn.ui.view.PieChartView.PieData("异常丢失", String.valueOf(illegalLoss), (illegalLoss * 100f) / totalLeft, android.graphics.Color.parseColor("#D00000")));
                    data.setOnlinePieData(onlinePie);

                    int totalBattery = Math.max(1, batteryNormal + batteryLow + batteryOffline);
                    java.util.List<com.lora.cn.ui.view.PieChartView.PieData> batteryPie = new java.util.ArrayList<>();
                    if (batteryNormal > 0) batteryPie.add(new com.lora.cn.ui.view.PieChartView.PieData("电量正常", String.valueOf(batteryNormal), (batteryNormal * 100f) / totalBattery, android.graphics.Color.parseColor("#39E56D")));
                    if (batteryLow > 0) batteryPie.add(new com.lora.cn.ui.view.PieChartView.PieData("低电量", String.valueOf(batteryLow), (batteryLow * 100f) / totalBattery, android.graphics.Color.parseColor("#FF9500")));
                    if (batteryOffline > 0) batteryPie.add(new com.lora.cn.ui.view.PieChartView.PieData("离线", String.valueOf(batteryOffline), (batteryOffline * 100f) / totalBattery, android.graphics.Color.parseColor("#CECECE")));
                    data.setBatteryPieData(batteryPie);

                    java.util.List<com.lora.cn.ui.model.ChartItem> onlineItems = new java.util.ArrayList<>();
                    onlineItems.add(new com.lora.cn.ui.model.ChartItem(android.graphics.Color.parseColor("#39E56D"), "在线", onlineCount + "台"));
                    onlineItems.add(new com.lora.cn.ui.model.ChartItem(android.graphics.Color.parseColor("#CECECE"), "离线", offlineCount + "台"));
                    onlineItems.add(new com.lora.cn.ui.model.ChartItem(android.graphics.Color.parseColor("#5D75F7"), "正常取走", manualTake + "台"));
                    onlineItems.add(new com.lora.cn.ui.model.ChartItem(android.graphics.Color.parseColor("#D00000"), "异常丢失", illegalLoss + "台"));
                    data.setOnlineChartItems(onlineItems);

                    java.util.List<com.lora.cn.ui.model.ChartItem> batteryItems = new java.util.ArrayList<>();
                    batteryItems.add(new com.lora.cn.ui.model.ChartItem(android.graphics.Color.parseColor("#39E56D"), "电量正常", batteryNormal + "台"));
                    batteryItems.add(new com.lora.cn.ui.model.ChartItem(android.graphics.Color.parseColor("#FF9500"), "低电量", batteryLow + "台"));
                    batteryItems.add(new com.lora.cn.ui.model.ChartItem(android.graphics.Color.parseColor("#CECECE"), "离线", batteryOffline + "台"));
                    data.setBatteryChartItems(batteryItems);

                    list.add(data);
                }
            }
            chartDataList.clear();
            chartDataList.addAll(list);
            terminalChartAdapter.submitList(list);
        } catch (Exception e) {
            Log.e("TerminalCheckFragment", "初始化图表适配器真实数据失败: " + e.getMessage());
        }
    }

    /**
     * 开始终端清点
     */
    private void startTerminalCheck() {
        isChecking = true;
        addTerminal.setText("清点中...");
        addTerminal.setEnabled(false);

        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
            com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
            logInfo.setTerminalId("ALL");
            logInfo.setTerminalName("所有终端");
            logInfo.setDeviceId("SYSTEM");
            logInfo.setStatusCode(0);
            logInfo.setOperator("");
            logInfo.setAction("终端清点");
            logInfo.setOperationTime("");
            logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
            dbHelper.addLog(logInfo);
        } catch (Exception e) {
            Log.e("TerminalCheckFragment", "记录清点开始日志失败", e);
        }

        com.lora.cn.utils.DialogUtils.CountingProgress cp = com.lora.cn.utils.DialogUtils.showCountingProgressDialog(requireContext(), "清点中...", 0);
        final int totalSec = 60;
        android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        final int[] sec = {0};
        Runnable r = new Runnable() {
            @Override public void run() {
                try {
                    sec[0]++;
                    int percent = Math.min(100, (int) Math.round(sec[0] * 100.0 / totalSec));
                    com.lora.cn.utils.DialogUtils.updateCountingProgress(cp, percent);
                    if (sec[0] < totalSec) {
                        h.postDelayed(this, 1000);
                    } else {
                        com.lora.cn.utils.DialogUtils.dismissCountingProgress(cp);
                        isChecking = false;
                        addTerminal.setText("开始清点");
                        addTerminal.setEnabled(true);
                        if (!isAdmin) updateRemainingCount(remainingCount - 1);
                        updateClearTime();
                        refreshData();
                        Toast.makeText(getContext(), "清点完成", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    try { com.lora.cn.utils.DialogUtils.dismissCountingProgress(cp); } catch (Exception ignored) {}
                    isChecking = false;
                    addTerminal.setText("开始清点");
                    addTerminal.setEnabled(true);
                }
            }
        };
        h.postDelayed(r, 1000);
    }
    
    /**
     * 更新剩余次数
     */
    private void updateRemainingCount(int count) {
        this.remainingCount = Math.max(0, count);
        if (terminalRemainingNumber != null) {
            if (isAdmin) {
                terminalRemainingNumber.setText("不限");
            } else {
                terminalRemainingNumber.setText(String.valueOf(this.remainingCount));
            }
        }
    }
    
    /**
     * 更新清点时间
     */
    private void updateClearTime() {
        if (terminalClearTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            terminalClearTime.setText(sdf.format(new Date()));
        }
    }
    
    /**
     * 刷新数据
     */
    public void refreshData() {
        // 这里可以调用API获取最新的终端数据
        // 然后更新饼图显示
        initPieChartData();
        
        // 刷新图表适配器数据
        initChartAdapterData();
    }
    
    private void exportExcel() {
        try {
            String NL = "\r\n";
            StringBuilder sbStatus = new StringBuilder();
            StringBuilder sbBattery = new StringBuilder();
            sbStatus.append("分类,在线,离线,正常取走,异常丢失").append(NL);
            sbBattery.append("分类,电量正常,低电量,离线").append(NL);
            for (com.lora.cn.ui.model.TerminalChartData d : chartDataList) {
                String label = d.getOnlineTitle();
                java.util.Map<String, Integer> online = new java.util.HashMap<>();
                for (com.lora.cn.ui.model.ChartItem ci : d.getOnlineChartItems()) {
                    String k = ci.getKey();
                    String v = ci.getValue();
                    String num = v.replaceAll("[^0-9]", "");
                    int n = num.isEmpty() ? 0 : Integer.parseInt(num);
                    online.put(k, n);
                }
                java.util.Map<String, Integer> battery = new java.util.HashMap<>();
                for (com.lora.cn.ui.model.ChartItem ci2 : d.getBatteryChartItems()) {
                    String k = ci2.getKey();
                    String v = ci2.getValue();
                    String num = v.replaceAll("[^0-9]", "");
                    int n = num.isEmpty() ? 0 : Integer.parseInt(num);
                    battery.put(k, n);
                }
                int on = online.getOrDefault("在线", 0);
                int off = online.getOrDefault("离线", 0);
                int take = online.getOrDefault("正常取走", 0);
                int loss = online.getOrDefault("异常丢失", 0);
                int bn = battery.getOrDefault("电量正常", 0);
                int bl = battery.getOrDefault("低电量", 0);
                int boff = battery.getOrDefault("离线", 0);
                sbStatus.append(label).append(',')
                        .append(on).append(',')
                        .append(off).append(',')
                        .append(take).append(',')
                        .append(loss)
                        .append(NL);
                sbBattery.append(label).append(',')
                        .append(bn).append(',')
                        .append(bl).append(',')
                        .append(boff)
                        .append(NL);
            }

            java.io.File dir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) dir = requireContext().getExternalFilesDir(null);
            if (dir == null) dir = new java.io.File(requireContext().getFilesDir(), "exports");
            if (!dir.exists()) dir.mkdirs();
            long ts = System.currentTimeMillis();
            java.io.File fileStatus = new java.io.File(dir, "terminal_check_status_" + ts + ".csv");
            java.io.File fileBattery = new java.io.File(dir, "terminal_check_battery_" + ts + ".csv");
            java.io.FileOutputStream fos1 = new java.io.FileOutputStream(fileStatus);
            fos1.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
            fos1.write(sbStatus.toString().getBytes("UTF-8"));
            fos1.flush(); fos1.close();
            java.io.FileOutputStream fos2 = new java.io.FileOutputStream(fileBattery);
            fos2.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
            fos2.write(sbBattery.toString().getBytes("UTF-8"));
            fos2.flush(); fos2.close();
            Toast.makeText(getContext(), "导出成功: \n" + fileStatus.getAbsolutePath() + "\n" + fileBattery.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 更新在线状态数据
     */
    public void updateOnlineData(List<PieChartView.PieData> data) {
        if (pieChartOnline != null && data != null) {
            pieChartOnline.setData(data);
        }
    }
    
    /**
     * 更新电量状态数据
     */
    public void updateBatteryData(List<PieChartView.PieData> data) {
        if (pieChartBattery != null && data != null) {
            pieChartBattery.setData(data);
        }
    }
    
    /**
     * 设置剩余清点次数
     */
    public void setRemainingCount(int count) {
        updateRemainingCount(count);
    }
    
    /**
     * 获取剩余清点次数
     */
    public int getRemainingCount() {
        return remainingCount;
    }
    
    /**
     * 检查是否正在清点
     */
    public boolean isChecking() {
        return isChecking;
    }
    
    /**
     * 检查当前用户是否有指定权限
     */
    private boolean hasPermission(String permissionCode) {
        if (currentUserRoleId == -1) {
            return false;
        }
        return databaseManager.hasPermission(currentUserRoleId, permissionCode);
    }

    public static TerminalCheckFragment newInstance() {
        return new TerminalCheckFragment();
    }
}
