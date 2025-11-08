package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.database.entity.User;
import com.lora.cn.ui.adapter.TerminalStatusAdapter;
import com.lora.cn.ui.adapter.TerminalAdapter;
import com.lora.cn.ui.model.TerminalStatus;
import com.lora.cn.ui.model.Terminal;
import com.lora.cn.ui.constants.TerminalStatusConstants;
import com.lora.cn.utils.LoRaProtocolParser;
import com.lora.cn.dialog.AddTerminalDialog;
import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.event.TerminalRefreshEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class TerminalListFragment extends Fragment {

    private static final String TAG = "TerminalListFragment";

    private RecyclerView rvTerminalStatus;
    private RecyclerView terminalRecycle;
    private TerminalStatusAdapter terminalStatusAdapter;
    private TerminalAdapter adapter;
    private TextView addTerminalBtn;
    private int currentStatusIndex = 0;
    // 过滤相关
    private TextView tvGroupCategory;
    private long selectedGroupId = -1; // -1: 全部分组
    private long selectedCategoryId = -1; // -1: 全部分类
    private List<Terminal> allDisplayTerminals = new ArrayList<>();

    // 数据库管理器
    private DatabaseManager databaseManager;
    private int currentUserRoleId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal_list, container, false);

        // 初始化数据库管理器
        databaseManager = DatabaseManager.getInstance(requireContext());

        // 初始化用户角色ID
        long userId = SPUtils.getInstance().getLong("current_user_id", -1);
        if (userId != -1) {
            User user = databaseManager.getUserById(userId);
            if (user != null) {
                currentUserRoleId = (int)user.getRoleId();
            }
        }

        initViews(view);

        // 检查查看终端列表权限（修正为正确的权限码：terminal_list）
        if (hasPermission("terminal_list")) {
            initTerminalStatus();
        } else {
            Toast.makeText(requireContext(), "您没有查看终端列表的权限", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void initViews(View view) {
        rvTerminalStatus = view.findViewById(R.id.rv_terminal_status);
        terminalRecycle = view.findViewById(R.id.terminal_recycle);
        addTerminalBtn = view.findViewById(R.id.add_terminal);
        tvGroupCategory = view.findViewById(R.id.tv_group_category);

        // 设置添加终端按钮点击事件
        addTerminalBtn.setOnClickListener(v -> {
            if (hasPermission("terminal_add")) {
                onAddTerminalClick();
            } else {
                Toast.makeText(requireContext(), "您没有添加终端的权限", Toast.LENGTH_SHORT).show();
            }
        });

        // 搜索设备：点击搜索图标展示设备列表Fragment
        EditText searchEditText = view.findViewById(R.id.et_search);
        if (searchEditText != null) {
            searchEditText.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    android.graphics.drawable.Drawable right = searchEditText.getCompoundDrawables()[2];
                    if (right != null && event.getX() >= (searchEditText.getWidth() - searchEditText.getPaddingRight() - right.getBounds().width())) {
                        // 展示设备列表
                        if (getActivity() instanceof com.lora.cn.ui.activity.MainActivity) {
                            com.lora.cn.ui.activity.MainActivity mainActivity = (com.lora.cn.ui.activity.MainActivity) getActivity();
                            mainActivity.showDeviceList();
                        }
                        // 记录日志
                        try {
                            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                            String now = sdf.format(new java.util.Date());
                            com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
                            logInfo.setTerminalId("SYSTEM");
                            logInfo.setTerminalName("终端列表");
                            logInfo.setDeviceId("SYSTEM");
                            logInfo.setStatus("设备打开");
                            logInfo.setOperator("系统管理员");
                            logInfo.setAction("打开附近设备搜索界面");
                            logInfo.setOperationTime(now);
                            logInfo.setCreateTime(now);
                            dbHelper.addLog(logInfo);
                        } catch (Exception e) {
                            Log.e(TAG, "记录日志失败", e);
                        }
                        return true;
                    }
                }
                return false;
            });
        }

        // 初始化两级选择器入口（文本点击弹出选择）
        if (tvGroupCategory != null) {
            tvGroupCategory.setOnClickListener(v -> showGroupCategoryPicker());
        }
    }

    private void initTerminalStatus() {
        // 从数据库获取真实的终端统计数据
        updateTerminalStatusFromDatabase();

        // 设置状态RecyclerView
        LinearLayoutManager statusLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvTerminalStatus.setLayoutManager(statusLayoutManager);

        terminalStatusAdapter = new TerminalStatusAdapter();
        rvTerminalStatus.setAdapter(terminalStatusAdapter);

        // 初始化终端列表
        initTerminalList();
    }

    private void showGroupCategoryPicker() {
        try {
            android.app.Dialog dialog = new android.app.Dialog(requireContext());
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
            android.view.View root = android.view.LayoutInflater.from(requireContext()).inflate(R.layout.dialog_group_category_picker, null);
            dialog.setContentView(root);
            android.widget.ListView lvGroups = root.findViewById(R.id.lv_groups);
            android.widget.ListView lvCategories = root.findViewById(R.id.lv_categories);
            android.widget.Button btnConfirm = root.findViewById(R.id.btn_confirm);
            android.widget.Button btnCancel = root.findViewById(R.id.btn_cancel);

            // 加载分组
            List<com.lora.cn.database.entity.Group> groups = databaseManager.getAllGroups();
            List<com.lora.cn.database.entity.Group> displayGroups = new ArrayList<>();
            com.lora.cn.database.entity.Group allG = new com.lora.cn.database.entity.Group();
            allG.setGroupId(-1);
            allG.setGroupName("全部分组");
            displayGroups.add(allG);
            if (groups != null) displayGroups.addAll(groups);
            List<String> groupNames = new ArrayList<>();
            for (com.lora.cn.database.entity.Group g : displayGroups) groupNames.add(g.getGroupName());
            android.widget.ArrayAdapter<String> gAdapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, groupNames);
            lvGroups.setAdapter(gAdapter);

            final long[] tempSelectedGroupId = {selectedGroupId};
            final long[] tempSelectedCategoryId = {selectedCategoryId};

            lvGroups.setOnItemClickListener((parent, view, position, id) -> {
                tempSelectedGroupId[0] = displayGroups.get(position).getGroupId();
                // 加载分类
                List<com.lora.cn.database.entity.Category> categories = tempSelectedGroupId[0] == -1 ? databaseManager.getAllCategories() : databaseManager.getCategoriesByGroupId(tempSelectedGroupId[0]);
                List<com.lora.cn.database.entity.Category> displayCategories = new ArrayList<>();
                com.lora.cn.database.entity.Category allC = new com.lora.cn.database.entity.Category();
                allC.setCategoryId(-1);
                allC.setCategoryName("全部分类");
                displayCategories.add(allC);
                if (categories != null) displayCategories.addAll(categories);
                List<String> categoryNames = new ArrayList<>();
                for (com.lora.cn.database.entity.Category c : displayCategories) categoryNames.add(c.getCategoryName());
                android.widget.ArrayAdapter<String> cAdapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, categoryNames);
                lvCategories.setAdapter(cAdapter);
                lvCategories.setOnItemClickListener((p, v, pos, i2) -> tempSelectedCategoryId[0] = displayCategories.get(pos).getCategoryId());
            });

            btnConfirm.setOnClickListener(v -> {
                selectedGroupId = tempSelectedGroupId[0];
                selectedCategoryId = tempSelectedCategoryId[0];
                // 更新显示文本
                try {
                    String gName = "全部分组";
                    String cName = "全部分类";
                    if (selectedGroupId != -1) {
                        List<com.lora.cn.database.entity.Group> gs = databaseManager.getAllGroups();
                        for (com.lora.cn.database.entity.Group g : gs) if (g.getGroupId() == selectedGroupId) { gName = g.getGroupName(); break; }
                    }
                    if (selectedCategoryId != -1) {
                        List<com.lora.cn.database.entity.Category> cs = selectedGroupId == -1 ? databaseManager.getAllCategories() : databaseManager.getCategoriesByGroupId(selectedGroupId);
                        for (com.lora.cn.database.entity.Category c : cs) if (c.getCategoryId() == selectedCategoryId) { cName = c.getCategoryName(); break; }
                    }
                    tvGroupCategory.setText(gName + "/" + cName);
                } catch (Exception ignored) {}

                applyFilters();
                dialog.dismiss();
            });
            btnCancel.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "显示分组分类选择器失败", e);
        }
    }

    /**
     * 从数据库更新终端状态统计
     */
    private void updateTerminalStatusFromDatabase() {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
            List<com.lora.cn.ui.model.Terminal> allTerminals = dbHelper.getAllTerminals();

            // 统计各种状态的终端数量
            int favoriteCount = 0;
            int onlineCount = 0;
            int normalTakenCount = 0;
            int abnormalLostCount = 0;
            int lowBatteryCount = 0;
            int offlineCount = 0;

            for (com.lora.cn.ui.model.Terminal terminal : allTerminals) {
                // 统计收藏数量
                if (terminal.isFavorite()) {
                    favoriteCount++;
                }

                // 统计状态数量
                String status = terminal.getStatus();
                if ("在线".equals(status)) {
                    onlineCount++;
                } else if ("离线".equals(status)) {
                    offlineCount++;
                } else if ("异常".equals(status)) {
                    abnormalLostCount++;
                }

                // 统计电量状态
                int batteryLevel = terminal.getBatteryLevel();
                if (batteryLevel <= 20) {
                    lowBatteryCount++;
                } else if (batteryLevel > 60) {
                    normalTakenCount++;
                }
            }

            // 创建状态列表
            List<TerminalStatus> statusList = new ArrayList<>();
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_IMPORTANT, R.mipmap.ic_coll, favoriteCount));
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_ONLINE, R.mipmap.ic_xh_3, onlineCount));
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_NORMAL_TAKEN, R.mipmap.ic_blue_right, normalTakenCount));
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_ABNORMAL_LOST, R.mipmap.ic_ds, abnormalLostCount));
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_LOW_BATTERY, R.mipmap.ic_red_sd, lowBatteryCount));
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_OFFLINE, R.mipmap.ic_xh_no, offlineCount));

            // 更新适配器数据
            if (terminalStatusAdapter != null) {
                terminalStatusAdapter.submitList(statusList);
            }

        } catch (Exception e) {
            Log.e(TAG, "更新终端状态统计失败", e);
            // 出错时使用默认数据
            List<TerminalStatus> statusList = TerminalStatusConstants.getDefaultStatusList();
            if (terminalStatusAdapter != null) {
                terminalStatusAdapter.submitList(statusList);
            }
        }
    }

    private void initTerminalList() {
        // 首先尝试从数据库加载终端数据
        loadTerminals();
    }

    @Override
    public void onStart() {
        super.onStart();
        // 注册事件总线，监听新增终端事件
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            // 返回页面时刷新终端列表和状态统计，确保展示最新添加的设备
            loadTerminals();
            updateTerminalStatusFromDatabase();
        } catch (Exception e) {
            Log.e(TAG, "onResume 刷新终端列表失败", e);
        }
    }

    @Override
    public void onStop() {
        // 取消注册事件总线
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        super.onStop();
    }

    // 订阅终端刷新事件，收到后立即刷新列表与状态
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTerminalRefreshEvent(TerminalRefreshEvent event) {
        try {
            loadTerminals();
            updateTerminalStatusFromDatabase();
            if (getContext() != null) {
                Toast.makeText(getContext(), event != null ? event.getMessage() : "终端列表已刷新", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "处理TerminalRefreshEvent失败", e);
        }
    }

    private void onTerminalClick(int position, Terminal terminal) {
        try {
            // 从数据库获取完整的终端信息
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
            com.lora.cn.ui.model.Terminal dbTerminal = null;

            // 通过终端ID查找数据库中的终端信息
            List<com.lora.cn.ui.model.Terminal> allTerminals = dbHelper.getAllTerminals();
            for (com.lora.cn.ui.model.Terminal t : allTerminals) {
                if (t.getTerminalId().equals(terminal.getTerminalId())) {
                    dbTerminal = t;
                    break;
                }
            }

            if (dbTerminal != null) {
                // 创建TerminalInfo对象用于显示详情
                LoRaProtocolParser.TerminalInfo terminalInfo = new LoRaProtocolParser.TerminalInfo();
                terminalInfo.deviceId = dbTerminal.getTerminalId();
                terminalInfo.deviceName = dbTerminal.getTerminalName();
                terminalInfo.department = dbTerminal.getDepartment();
                terminalInfo.location = dbTerminal.getLocation();
                terminalInfo.signalStrength = dbTerminal.getSignalStrength();
                terminalInfo.batteryLevel = dbTerminal.getBatteryLevel();
                terminalInfo.status = "在线".equals(dbTerminal.getStatus()) ? 1 : 0;
                terminalInfo.timestamp = System.currentTimeMillis();
                terminalInfo.payloadHex = ""; // 可以从日志中获取最新的payload

                // 导航到终端详情Fragment（根据terminalId从数据库加载）
                com.lora.cn.ui.fragment.TerminalDetailFragment fragment = com.lora.cn.ui.fragment.TerminalDetailFragment.newInstance(dbTerminal.getTerminalId());
                if (getActivity() != null) {
                    android.app.Activity a = getActivity();
                    android.view.View container = a.findViewById(R.id.fragment_device_list_container);
                    if (container != null) {
                        container.setVisibility(android.view.View.VISIBLE);
                        android.view.View rvTabs = a.findViewById(R.id.rv_menu_tabs);
                        if (rvTabs != null) rvTabs.setVisibility(android.view.View.INVISIBLE);
                        android.view.View vp = a.findViewById(R.id.view_pager);
                        if (vp != null) vp.setVisibility(android.view.View.GONE);
                        ((androidx.appcompat.app.AppCompatActivity)a).getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_device_list_container, fragment)
                                .addToBackStack("terminal_detail")
                                .commit();
                    } else {
                        Toast.makeText(getContext(), "未找到详情容器", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(getContext(), "未找到终端详细信息", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "显示终端详情失败", e);
            Toast.makeText(getContext(), "显示终端详情失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 添加终端按钮点击事件
     */
    private void onAddTerminalClick() {
        // 检查权限
        if (!hasPermission("terminal_add")) {
            Toast.makeText(getContext(), "您没有添加终端的权限", Toast.LENGTH_SHORT).show();
            return;
        }

        // 展示附近设备列表（DeviceListFragment）
        if (getActivity() instanceof com.lora.cn.ui.activity.MainActivity) {
            com.lora.cn.ui.activity.MainActivity mainActivity = (com.lora.cn.ui.activity.MainActivity) getActivity();
            mainActivity.showDeviceList();
        } else {
            Toast.makeText(getContext(), "无法打开设备列表", Toast.LENGTH_SHORT).show();
        }
    }

//    /**
//     * 将新终端添加到数据库
//     */
//    private void addNewTerminalToDatabase(LoRaProtocolParser.TerminalInfo terminalInfo) {
//        try {
//            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
//
//            // 检查终端是否已存在
//            if (dbHelper.isTerminalExists(terminalInfo.deviceId)) {
//                Toast.makeText(getContext(), "终端设备已存在", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // 创建Terminal对象
//            com.lora.cn.ui.model.Terminal terminal = new com.lora.cn.ui.model.Terminal();
//            terminal.setTerminalId(terminalInfo.deviceId);
//            terminal.setTerminalName(terminalInfo.deviceName);
//            terminal.setStatus("在线");
//            terminal.setSignalStrength(terminalInfo.signalStrength);
//            terminal.setDepartment(terminalInfo.department);
//            terminal.setLocation(terminalInfo.location);
//
//            // 添加到数据库
//            long result = dbHelper.addTerminal(terminal);
//
//            if (result > 0) {
//                // 记录添加终端的日志
//                com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
//                logInfo.setTerminalId(terminalInfo.deviceId);
//                logInfo.setTerminalName(terminalInfo.deviceName);
//                logInfo.setDeviceId(terminalInfo.deviceId);
//                logInfo.setStatus("成功");
//                logInfo.setOperator("系统管理员"); // 这里可以根据实际登录用户设置
//                logInfo.setAction("添加终端");
//                logInfo.setOperationTime(String.valueOf(System.currentTimeMillis()));
//                logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
//
//                dbHelper.addLog(logInfo);
//
//                Toast.makeText(getContext(), "终端添加成功", Toast.LENGTH_SHORT).show();
//                // 重新加载终端列表
//                loadTerminals();
//            } else {
//                // 记录添加失败的日志
//                com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
//                logInfo.setTerminalId(terminalInfo.deviceId);
//                logInfo.setTerminalName(terminalInfo.deviceName);
//                logInfo.setDeviceId(terminalInfo.deviceId);
//                logInfo.setStatus("失败");
//                logInfo.setOperator("系统管理员");
//                logInfo.setAction("添加终端");
//                logInfo.setOperationTime(String.valueOf(System.currentTimeMillis()));
//                logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
//
//                dbHelper.addLog(logInfo);
//
//                Toast.makeText(getContext(), "终端添加失败", Toast.LENGTH_SHORT).show();
//            }
//
//        } catch (Exception e) {
//            Log.e(TAG, "添加终端到数据库失败", e);
//
//            // 记录异常日志
//            try {
//                DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
//                com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
//                logInfo.setTerminalId(terminalInfo.deviceId);
//                logInfo.setTerminalName(terminalInfo.deviceName);
//                logInfo.setDeviceId(terminalInfo.deviceId);
//                logInfo.setStatus("异常");
//                logInfo.setOperator("系统管理员");
//                logInfo.setAction("添加终端");
//                logInfo.setOperationTime(String.valueOf(System.currentTimeMillis()));
//                logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
//
//                dbHelper.addLog(logInfo);
//            } catch (Exception logException) {
//                Log.e(TAG, "记录日志失败", logException);
//            }
//
//            Toast.makeText(getContext(), "添加终端失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//        }
//    }

    /**
     * 检查当前用户是否有指定权限
     */
    private boolean hasPermission(String permissionCode) {
        if (currentUserRoleId == -1) {
            return false;
        }
        return databaseManager.hasPermission(currentUserRoleId, permissionCode);
    }

    public static TerminalListFragment newInstance() {
        return new TerminalListFragment();
    }

    /**
     * 加载终端列表数据
     */
    private void loadTerminals() {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
            List<Terminal> terminals = dbHelper.getAllTerminals();

            // 设置终端列表RecyclerView（如果还没有设置）
            if (adapter == null) {
                GridLayoutManager terminalLayoutManager = new GridLayoutManager(getContext(), 4);
                terminalRecycle.setLayoutManager(terminalLayoutManager);

                adapter = new TerminalAdapter();
                adapter.setOnFavoriteClickListener(new TerminalAdapter.OnFavoriteClickListener() {
                    @Override
                    public void onFavoriteClick(Terminal terminal, boolean isFavorite) {
                        try {
                            // 更新数据库中的收藏状态
                            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
                            int result = dbHelper.updateTerminalFavoriteStatus(terminal.getTerminalId(), isFavorite);

                            // 记录收藏状态变更日志
                            com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
                            logInfo.setTerminalId(terminal.getTerminalId());
                            logInfo.setTerminalName(terminal.getTerminalName());
                            logInfo.setDeviceId(terminal.getTerminalId());
                            logInfo.setStatus(result > 0 ? "成功" : "失败");
                            logInfo.setOperator("系统管理员"); // 这里可以根据实际登录用户设置
                            logInfo.setAction(isFavorite ? "收藏终端" : "取消收藏");
                            logInfo.setOperationTime(String.valueOf(System.currentTimeMillis()));
                            logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));

                            dbHelper.addLog(logInfo);

                            // 更新终端状态统计
                            updateTerminalStatusFromDatabase();
                        } catch (Exception e) {
                            Log.e(TAG, "更新收藏状态失败", e);

                            // 记录异常日志
                            try {
                                DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
                                com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
                                logInfo.setTerminalId(terminal.getTerminalId());
                                logInfo.setTerminalName(terminal.getTerminalName());
                                logInfo.setDeviceId(terminal.getTerminalId());
                                logInfo.setStatus("异常");
                                logInfo.setOperator("系统管理员");
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                                String now = sdf.format(new java.util.Date());
                                logInfo.setAction(isFavorite ? "收藏终端" : "取消收藏");
                                logInfo.setOperationTime(now);
                                logInfo.setCreateTime(now);

                                dbHelper.addLog(logInfo);
                            } catch (Exception logException) {
                                Log.e(TAG, "记录日志失败", logException);
                            }
                        }
                    }
                });
                terminalRecycle.setAdapter(adapter);

                // 设置终端点击事件监听器
                adapter.setOnItemClickListener((adapter, view, position) -> {
                    // 查看终端详情权限（修正为正确的权限码：terminal_detail）
                    if (hasPermission("terminal_detail")) {
                        Terminal terminal = (Terminal) adapter.getItem(position);
                        onTerminalClick(position, terminal);
                    } else {
                        Toast.makeText(requireContext(), "您没有查看终端详情的权限", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (terminals != null && !terminals.isEmpty()) {
                // 转换数据库终端数据为UI显示格式
                allDisplayTerminals = convertToDisplayTerminals(terminals);
                adapter.submitList(new ArrayList<>(allDisplayTerminals));
            } else {
                // 如果数据库中没有数据，显示空列表
                adapter.submitList(new ArrayList<>());
            }
        } catch (Exception e) {
            Log.e(TAG, "加载终端列表失败", e);
            // 出错时显示空列表
            if (adapter != null) {
                adapter.submitList(new ArrayList<>());
            }
        }
    }

    /**
     * 将数据库终端数据转换为UI显示格式
     */
    private List<Terminal> convertToDisplayTerminals(List<Terminal> dbTerminals) {
        List<Terminal> displayTerminals = new ArrayList<>();

        for (Terminal dbTerminal : dbTerminals) {
            Terminal displayTerminal = new Terminal();

            // 设置基本信息
            displayTerminal.setId(dbTerminal.getId());
            displayTerminal.setTerminalId(dbTerminal.getTerminalId());
            displayTerminal.setTerminalName(dbTerminal.getTerminalName());
            displayTerminal.setName(dbTerminal.getTerminalName()); // 显示名称使用终端名称
            // 科室与病房：优先使用字符串；为空则根据ID查询分类名称
            String dept = dbTerminal.getDepartment();
            if (TextUtils.isEmpty(dept) && dbTerminal.getDepartmentId() > 0) {
                try {
                    com.lora.cn.database.entity.Category c = DatabaseManager.getInstance(getContext()).getCategoryById(dbTerminal.getDepartmentId());
                    if (c != null) dept = c.getCategoryName();
                } catch (Exception ignored) {}
            }
            String room = dbTerminal.getLocation();
            if (TextUtils.isEmpty(room) && dbTerminal.getRoomId() > 0) {
                try {
                    com.lora.cn.database.entity.Category c2 = DatabaseManager.getInstance(getContext()).getCategoryById(dbTerminal.getRoomId());
                    if (c2 != null) room = c2.getCategoryName();
                } catch (Exception ignored) {}
            }
            displayTerminal.setDepartment(dept);
            displayTerminal.setLocation(room);
            displayTerminal.setStatus(dbTerminal.getStatus());
            displayTerminal.setSignalStrength(dbTerminal.getSignalStrength());
            displayTerminal.setFavorite(dbTerminal.isFavorite());

            // 根据状态设置图标
            int statusIcon = getStatusIcon(dbTerminal.getStatus());
            displayTerminal.setStatusIconResId(statusIcon);
            displayTerminal.setStatusText(dbTerminal.getStatus());

            // 设置电量信息
            displayTerminal.setBatteryLevel(dbTerminal.getBatteryLevel());
            displayTerminal.setBatteryText(dbTerminal.getBatteryLevel() + "%");

            // 根据电量设置电池图标
            if (dbTerminal.getBatteryLevel() > 80) {
                displayTerminal.setBatteryIconResId(R.drawable.ic_green_sd); // 高电量-绿色电池
            } else if (dbTerminal.getBatteryLevel() > 30) {
                displayTerminal.setBatteryIconResId(R.drawable.ic_yellow_sd); // 中电量-黄色电池
            } else {
                displayTerminal.setBatteryIconResId(R.mipmap.ic_red_sd); // 低电量-红色电池（PNG保留在mipmap）
            }

            // 设置重要性（收藏状态）
            displayTerminal.setImportant(dbTerminal.isFavorite());

            displayTerminals.add(displayTerminal);
        }

        return displayTerminals;
    }

    private void applyFilters() {
        try {
            if (adapter == null) return;
            List<Terminal> base = new ArrayList<>(allDisplayTerminals);
            // 只有当选择了具体分类时才过滤，否则显示全部
            if (selectedCategoryId != -1 && selectedGroupId != -1) {
                List<Terminal> filtered = new ArrayList<>();
                for (Terminal t : base) {
                    boolean match = false;
                    if (selectedGroupId == 1) {
                        match = (t.getDepartmentId() == selectedCategoryId);
                    } else if (selectedGroupId == 2) {
                        match = (t.getRoomId() == selectedCategoryId);
                    } else if (selectedGroupId == 3) {
                        match = (t.getNursingGroupId() == selectedCategoryId);
                    } else if (selectedGroupId == 4) {
                        match = (t.getOtherId() == selectedCategoryId);
                    } else {
                        // 暂不支持其他自定义分组过滤
                        match = true;
                    }
                    if (match) filtered.add(t);
                }
                adapter.submitList(filtered);
            } else {
                adapter.submitList(new ArrayList<>(base));
            }
        } catch (Exception e) {
            Log.e(TAG, "应用过滤失败", e);
        }
    }

    /**
     * 根据状态获取对应的图标资源ID
     */
    private int getStatusIcon(String status) {
        if (status == null) {
            return R.mipmap.ic_xh_no;
        }

        switch (status) {
            case "在线":
                return R.mipmap.ic_xh_3;
            case "异常":
                return R.mipmap.ic_ds;
            case "离线":
            default:
                return R.mipmap.ic_xh_no;
        }
    }
}