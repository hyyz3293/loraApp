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
        // 更新UI数据（管理员不显示剩余次数；普通用户按账号与日期初始化剩余次数）
        if (isAdmin) {
            android.view.View parent = (android.view.View) terminalRemainingNumber.getParent();
            if (parent != null) parent.setVisibility(android.view.View.GONE);
        } else {
            initDailyRemainingForUser();
        }
        updateClearTime();
        
        // 初始化饼图数据
        initPieChartData();
        
        // 初始化图表适配器数据
        initChartAdapterData();

        // 默认不清点，恢复按钮状态
        isChecking = false;
        if (addTerminal != null) {
            addTerminal.setText("开始清点");
            addTerminal.setEnabled(true);
        }
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
            int lowThreshold = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
            for (com.lora.cn.ui.model.Terminal t : terminals) {
                int sc = t.getStatus();
                String st = com.lora.cn.ui.constants.TerminalStatusConstants.codeToText(sc);
                if ("正常在线".equals(st)) {
                    online++;
                } else if ("正常取走".equals(st)) {
                    manualTake++;
                } else if ("异常取走".equals(st)) {
                    abnormal++;
                } else {
                    offline++;
                }
                int bl = t.getBatteryLevel();
                if ("设备离线".equals(st)) {
                    // 离线计入电量离线
                } else {
                    if (bl <= lowThreshold) batteryLow++; else batteryNormal++;
                }
            }
            int totalStatus = Math.max(1, online + offline + abnormal + manualTake);
            int totalBattery = Math.max(1, batteryNormal + batteryLow + offline);
            List<PieChartView.PieData> onlineData = new ArrayList<>();
            if (online > 0)
                onlineData.add(new PieChartView.PieData("正常在线", String.valueOf(online), (online * 100f) / totalStatus, Color.parseColor("#39E56D")));
            if (manualTake > 0)
                onlineData.add(new PieChartView.PieData("正常取走", String.valueOf(manualTake), (manualTake * 100f) / totalStatus, Color.parseColor("#5D75F7")));
            if (abnormal > 0)
                onlineData.add(new PieChartView.PieData("异常取走", String.valueOf(abnormal), (abnormal * 100f) / totalStatus, Color.parseColor("#D00000")));
            if (offline > 0)
                onlineData.add(new PieChartView.PieData("设备离线", String.valueOf(offline), (offline * 100f) / totalStatus, Color.parseColor("#CECECE")));
            pieChartOnline.setData(onlineData);

            List<PieChartView.PieData> batteryData = new ArrayList<>();
            if (batteryNormal > 0)
                batteryData.add(new PieChartView.PieData("电量正常", String.valueOf(batteryNormal), (batteryNormal * 100f) / totalBattery, Color.parseColor("#39E56D")));
            if (batteryLow > 0)
                batteryData.add(new PieChartView.PieData("低电量", String.valueOf(batteryLow), (batteryLow * 100f) / totalBattery, Color.parseColor("#FF9500")));
            if (offline > 0)
                batteryData.add(new PieChartView.PieData("设备离线", String.valueOf(offline), (offline * 100f) / totalBattery, Color.parseColor("#CECECE")));
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
            DatabaseManager dm = DatabaseManager.getInstance(getContext());
            java.util.List<com.lora.cn.database.entity.Group> groups = dm.getAllGroups();
            if (groups == null) groups = new java.util.ArrayList<>();
            long depGroupId = 0L, roomGroupId = 0L, nursingGroupId = 0L, otherGroupId = 0L;
            try {
                com.lora.cn.database.entity.Group gDep = dm.getGroupByName("科室");
                com.lora.cn.database.entity.Group gRoom = dm.getGroupByName("病房号");
                com.lora.cn.database.entity.Group gNur = dm.getGroupByName("护理组");
                com.lora.cn.database.entity.Group gOth = dm.getGroupByName("其他分类");
                if (gDep != null) depGroupId = gDep.getGroupId();
                if (gRoom != null) roomGroupId = gRoom.getGroupId();
                if (gNur != null) nursingGroupId = gNur.getGroupId();
                if (gOth != null) otherGroupId = gOth.getGroupId();
            } catch (Exception ignored) {}
            for (com.lora.cn.database.entity.Group g : groups) {
                long gid = g.getGroupId();
                String gname = g.getGroupName() != null ? g.getGroupName() : "分组";
                String prefix = gname + "-";
                java.util.List<com.lora.cn.database.entity.Category> cats = DatabaseManager.getInstance(getContext()).getCategoriesByGroupId(gid);
                if (cats == null) continue;
                for (com.lora.cn.database.entity.Category c : cats) {
                    String label = prefix + c.getCategoryName();
                    int manualTake = 0, illegalLoss = 0;
                    int batteryNormal = 0, batteryLow = 0, batteryOffline = 0;
                    int onlineCount = 0, offlineCount = 0;

                    int lowThreshold2 = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
                    for (com.lora.cn.ui.model.Terminal t : terminals) {
                        boolean match = (t.getDepartmentId() == (int) c.getCategoryId())
                                || (t.getRoomId() == (int) c.getCategoryId())
                                || (t.getNursingGroupId() == (int) c.getCategoryId())
                                || (t.getOtherId() == (int) c.getCategoryId());
                        if (!match) {
                            long val = 0L;
                            try {
                                String ext = t.getExtension();
                                if (ext != null && !ext.isEmpty()) {
                                    org.json.JSONObject obj = new org.json.JSONObject(ext);
                                    if (obj.has("extra_groups")) {
                                        org.json.JSONObject ex = obj.getJSONObject("extra_groups");
                                        if (ex.has(String.valueOf(gid))) {
                                            val = ex.optLong(String.valueOf(gid), 0L);
                                        }
                                    }
                                }
                            } catch (Exception ignored) {}
                            match = (val == c.getCategoryId());
                        }
                        if (!match) continue;

                        int sc2 = t.getStatus();
                        String st = com.lora.cn.ui.constants.TerminalStatusConstants.codeToText(sc2);
                        if ("正常在线".equals(st)) { onlineCount++; }
                        else if ("设备离线".equals(st)) { offlineCount++; batteryOffline++; }
                        else if ("正常取走".equals(st)) { manualTake++; }
                        else if ("异常取走".equals(st)) { illegalLoss++; }
                        int bl = t.getBatteryLevel();
                        if (!"设备离线".equals(st)) { if (bl <= lowThreshold2) batteryLow++; else batteryNormal++; }
                    }

                    com.lora.cn.ui.model.TerminalChartData data = new com.lora.cn.ui.model.TerminalChartData();
                    data.setOnlineTitle(label);
                    data.setBatteryTitle(label);

                    int totalLeft = Math.max(1, onlineCount + offlineCount + manualTake + illegalLoss);
                    java.util.List<com.lora.cn.ui.view.PieChartView.PieData> onlinePie = new java.util.ArrayList<>();
                    if (onlineCount > 0) onlinePie.add(new com.lora.cn.ui.view.PieChartView.PieData("正常在线", String.valueOf(onlineCount), (onlineCount * 100f) / totalLeft, android.graphics.Color.parseColor("#39E56D")));
                    if (offlineCount > 0) onlinePie.add(new com.lora.cn.ui.view.PieChartView.PieData("设备离线", String.valueOf(offlineCount), (offlineCount * 100f) / totalLeft, android.graphics.Color.parseColor("#CECECE")));
                    if (manualTake > 0) onlinePie.add(new com.lora.cn.ui.view.PieChartView.PieData("正常取走", String.valueOf(manualTake), (manualTake * 100f) / totalLeft, android.graphics.Color.parseColor("#5D75F7")));
                    if (illegalLoss > 0) onlinePie.add(new com.lora.cn.ui.view.PieChartView.PieData("异常取走", String.valueOf(illegalLoss), (illegalLoss * 100f) / totalLeft, android.graphics.Color.parseColor("#D00000")));
                    data.setOnlinePieData(onlinePie);

                    int totalBattery = Math.max(1, batteryNormal + batteryLow + batteryOffline);
                    java.util.List<com.lora.cn.ui.view.PieChartView.PieData> batteryPie = new java.util.ArrayList<>();
                    if (batteryNormal > 0) batteryPie.add(new com.lora.cn.ui.view.PieChartView.PieData("电量正常", String.valueOf(batteryNormal), (batteryNormal * 100f) / totalBattery, android.graphics.Color.parseColor("#39E56D")));
                    if (batteryLow > 0) batteryPie.add(new com.lora.cn.ui.view.PieChartView.PieData("低电量", String.valueOf(batteryLow), (batteryLow * 100f) / totalBattery, android.graphics.Color.parseColor("#FF9500")));
                    if (batteryOffline > 0) batteryPie.add(new com.lora.cn.ui.view.PieChartView.PieData("设备离线", String.valueOf(batteryOffline), (batteryOffline * 100f) / totalBattery, android.graphics.Color.parseColor("#CECECE")));
                    data.setBatteryPieData(batteryPie);

                    java.util.List<com.lora.cn.ui.model.ChartItem> onlineItems = new java.util.ArrayList<>();
                    onlineItems.add(new com.lora.cn.ui.model.ChartItem(android.graphics.Color.parseColor("#39E56D"), "正常在线", onlineCount + "台"));
                    onlineItems.add(new com.lora.cn.ui.model.ChartItem(android.graphics.Color.parseColor("#CECECE"), "设备离线", offlineCount + "台"));
                    onlineItems.add(new com.lora.cn.ui.model.ChartItem(android.graphics.Color.parseColor("#5D75F7"), "正常取走", manualTake + "台"));
                    onlineItems.add(new com.lora.cn.ui.model.ChartItem(android.graphics.Color.parseColor("#D00000"), "异常取走", illegalLoss + "台"));
                    data.setOnlineChartItems(onlineItems);

                    java.util.List<com.lora.cn.ui.model.ChartItem> batteryItems = new java.util.ArrayList<>();
                    batteryItems.add(new com.lora.cn.ui.model.ChartItem(android.graphics.Color.parseColor("#39E56D"), "电量正常", batteryNormal + "台"));
                    batteryItems.add(new com.lora.cn.ui.model.ChartItem(android.graphics.Color.parseColor("#FF9500"), "低电量", batteryLow + "台"));
                    batteryItems.add(new com.lora.cn.ui.model.ChartItem(android.graphics.Color.parseColor("#CECECE"), "设备离线", batteryOffline + "台"));
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
        try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.OperationBusyEvent(true)); } catch (Exception ignored) {}
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
                        try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.OperationBusyEvent(false)); } catch (Exception ignored) {}
                        addTerminal.setText("开始清点");
                        addTerminal.setEnabled(true);
                        if (!isAdmin) decrementRemainingForUser();
                        updateClearTime();
                        refreshData();
                        Toast.makeText(getContext(), "清点完成", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    try { com.lora.cn.utils.DialogUtils.dismissCountingProgress(cp); } catch (Exception ignored) {}
                    isChecking = false;
                    try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.OperationBusyEvent(false)); } catch (Exception ignored) {}
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
            StringBuilder sbStatus = new StringBuilder();
            StringBuilder sbBattery = new StringBuilder();
            sbStatus.append("<html><head><meta charset=\"UTF-8\"></head><body><table border=\"1\" cellspacing=\"0\" cellpadding=\"4\">")
                    .append("<tr><th>分类</th><th>正常在线</th><th>设备离线</th><th>正常取走</th><th>异常取走</th></tr>");
            sbBattery.append("<html><head><meta charset=\"UTF-8\"></head><body><table border=\"1\" cellspacing=\"0\" cellpadding=\"4\">")
                    .append("<tr><th>分类</th><th>电量正常</th><th>低电量</th><th>设备离线</th></tr>");
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
                int on = online.getOrDefault("正常在线", 0);
                int off = online.getOrDefault("设备离线", 0);
                int take = online.getOrDefault("正常取走", 0);
                int loss = online.getOrDefault("异常取走", 0);
                int bn = battery.getOrDefault("电量正常", 0);
                int bl = battery.getOrDefault("低电量", 0);
                int boff = battery.getOrDefault("设备离线", 0);
                sbStatus.append("<tr><td>").append(label).append("</td><td>")
                        .append(on).append("</td><td>")
                        .append(off).append("</td><td>")
                        .append(take).append("</td><td>")
                        .append(loss)
                        .append("</td></tr>");
                sbBattery.append("<tr><td>").append(label).append("</td><td>")
                        .append(bn).append("</td><td>")
                        .append(bl).append("</td><td>")
                        .append(boff)
                        .append("</td></tr>");
            }
            sbStatus.append("</table></body></html>");
            sbBattery.append("</table></body></html>");
            java.io.File dir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) dir = requireContext().getExternalFilesDir(null);
            if (dir == null) dir = new java.io.File(requireContext().getFilesDir(), "exports");
            if (!dir.exists()) dir.mkdirs();
            long ts = System.currentTimeMillis();
            java.io.File fileStatus = new java.io.File(dir, "terminal_check_status_" + ts + ".xls");
            java.io.File fileBattery = new java.io.File(dir, "terminal_check_battery_" + ts + ".xls");
            java.io.FileOutputStream fos1 = new java.io.FileOutputStream(fileStatus);
            fos1.write(sbStatus.toString().getBytes("UTF-8"));
            fos1.flush(); fos1.close();
            java.io.FileOutputStream fos2 = new java.io.FileOutputStream(fileBattery);
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

    private long currentUserIdCache = -1L;
    private void initDailyRemainingForUser() {
        try {
            int defaultCnt = com.blankj.utilcode.util.SPUtils.getInstance().getInt("terminal_check_count", 2);
            long uid = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1L);
            currentUserIdCache = uid;
            String dateKey = "check_last_date_user_" + uid;
            String remainKey = "check_remaining_user_" + uid;
            String today = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(new java.util.Date());
            String last = com.blankj.utilcode.util.SPUtils.getInstance().getString(dateKey);
            int remain = com.blankj.utilcode.util.SPUtils.getInstance().getInt(remainKey, -1);
            if (!today.equals(last) || remain < 0) {
                com.blankj.utilcode.util.SPUtils.getInstance().put(dateKey, today);
                com.blankj.utilcode.util.SPUtils.getInstance().put(remainKey, defaultCnt);
                updateRemainingCount(defaultCnt);
            } else {
                updateRemainingCount(remain);
            }
        } catch (Exception ignored) {}
    }

    private void decrementRemainingForUser() {
        try {
            long uid = currentUserIdCache > 0 ? currentUserIdCache : com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1L);
            String remainKey = "check_remaining_user_" + uid;
            int newRemain = Math.max(0, remainingCount - 1);
            com.blankj.utilcode.util.SPUtils.getInstance().put(remainKey, newRemain);
            updateRemainingCount(newRemain);
        } catch (Exception ignored) {}
    }
}
