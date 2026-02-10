package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blankj.utilcode.util.LogUtils;
import com.google.gson.Gson;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.dao.TerminalDao;
import com.lora.cn.database.entity.Category;
import com.lora.cn.ui.model.Terminal;
import com.lora.cn.event.TerminalRefreshEvent;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * 添加设备Fragment
 */
public class AddDeviceFragment extends Fragment {
    
    private TextView tvTerminalId;
    private EditText etDeviceId;
    private EditText etDeviceName;
    //private EditText etExtension; // 用作设备CODE输入框（字段映射为device_code）
    private Spinner spinnerDepartment;
    private Spinner spinnerRoom;
    private Spinner spinnerNursingGroup;
    private Spinner spinnerOther;
    private TextView btnBack;
    private TextView btnSave;
    private TextView btnCancel;
    private TextView btnConfirmPair;
    private androidx.recyclerview.widget.RecyclerView llDynamicGroups;
    private java.util.Map<Long, Long> selectedByGroup = new java.util.HashMap<>();
    
    private DatabaseManager dbManager;
    private TerminalDao terminalDao;
    private Terminal terminal;
    private String terminalId;
    private java.util.concurrent.ExecutorService ioExecutor;
    private android.os.Handler mainHandler;
    
    // 分类数据
    private List<Category> departmentCategories = new ArrayList<>();
    private List<Category> roomCategories = new ArrayList<>();
    private List<Category> nursingGroupCategories = new ArrayList<>();
    private List<Category> otherCategories = new ArrayList<>();
    private com.lora.cn.ui.adapter.DynamicGroupAdapter dgAdapter;
    private java.util.Map<java.lang.Long, java.util.List<com.lora.cn.database.entity.Category>> catsCache;
    
    // 选中的分类ID
    private Integer selectedDepartmentId = null;
    private Integer selectedRoomId = null;
    private Integer selectedNursingGroupId = null;
    private Integer selectedOtherId = null;
    private final java.util.concurrent.atomic.AtomicInteger groupsSeq = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger terminalSeq = new java.util.concurrent.atomic.AtomicInteger(0);
    private volatile boolean destroyed = false;
    
    public static AddDeviceFragment newInstance(Terminal terminalId) {
        AddDeviceFragment fragment = new AddDeviceFragment();
        Bundle args = new Bundle();
        args.putString("terminal", new Gson().toJson(terminalId));
        fragment.setArguments(args);
        return fragment;
    }

    public static AddDeviceFragment newInstance(Terminal terminalId, String mode) {
        AddDeviceFragment fragment = new AddDeviceFragment();
        Bundle args = new Bundle();
        args.putString("terminal", new Gson().toJson(terminalId));
        args.putString("mode", mode);
        fragment.setArguments(args);
        return fragment;
    }
    public static AddDeviceFragment newInstance(String deviceId, String mode) {
        AddDeviceFragment fragment = new AddDeviceFragment();
        Bundle args = new Bundle();
        args.putString("device_id", deviceId);
        args.putString("mode", mode);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String  terminalGson = getArguments().getString("terminal");
            if (terminalGson != null) {
                terminal = new Gson().fromJson(terminalGson, Terminal.class);
            }
            if (terminal != null) {
                terminalId = terminal.getTerminalId();
            }
            if (terminalId == null) {
                terminalId = getArguments().getString("device_id");
            }
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_device, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
        if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        initData();
        setupListeners();
    }
    
    private void initViews(View view) {
        tvTerminalId = view.findViewById(R.id.tv_terminal_id);
        etDeviceId = view.findViewById(R.id.et_device_id);
        etDeviceName = view.findViewById(R.id.et_device_name);
        btnBack = view.findViewById(R.id.btn_back);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnConfirmPair = view.findViewById(R.id.btn_confirm_pair);
        llDynamicGroups = view.findViewById(R.id.rv_dynamic_groups);
        try {
            if (llDynamicGroups != null) {
                llDynamicGroups.setHasFixedSize(true);
                llDynamicGroups.setItemAnimator(null);
                llDynamicGroups.setNestedScrollingEnabled(false);
            }
        } catch (Throwable ignored) {}
    }
    
    private void initData() {
        dbManager = DatabaseManager.getInstance(requireContext());
        terminalDao = new TerminalDao(DatabaseHelper.getInstance(requireContext()));
        
        // 设置终端ID（如果有传入）
        if (terminalId != null) {
            tvTerminalId.setText(terminalId);
            // 如果terminalId实际上是设备ID，则预填充到设备ID输入框
            //etDeviceId.setText(terminalId);
        }
        // 不设置默认值，让用户手动输入
        String mode = getArguments() != null ? getArguments().getString("mode", "pair") : "pair";
        if (btnBack != null) {
            btnBack.setText("pair".equals(mode) ? "设置终端" : "编辑");
        }
        if (btnConfirmPair != null) {
            btnConfirmPair.setText("pair".equals(mode) ? "完成配对" : "保存");
        }
        if ("edit".equals(mode) && terminal != null) {
            if (!TextUtils.isEmpty(terminal.getDeviceCode())) {
                etDeviceId.setText(terminal.getDeviceCode());
            }
            if (!TextUtils.isEmpty(terminal.getTerminalName())) {
                etDeviceName.setText(terminal.getTerminalName());
            }
        }
        if ("edit".equals(mode) && terminal == null && !TextUtils.isEmpty(terminalId)) {
            loadTerminalAsync();
        }
        // 隐藏旧的静态分组分类控件，使用动态分组/分类选择
        if (spinnerDepartment != null) spinnerDepartment.setVisibility(View.GONE);
        if (spinnerRoom != null) spinnerRoom.setVisibility(View.GONE);
        if (spinnerNursingGroup != null) spinnerNursingGroup.setVisibility(View.GONE);
        if (spinnerOther != null) spinnerOther.setVisibility(View.GONE);
        if (llDynamicGroups != null) {
            androidx.recyclerview.widget.LinearLayoutManager lm = new androidx.recyclerview.widget.LinearLayoutManager(requireContext());
            llDynamicGroups.setLayoutManager(lm);
            dgAdapter = new com.lora.cn.ui.adapter.DynamicGroupAdapter(dbManager, selectedByGroup, terminal, new java.util.HashMap<>());
            llDynamicGroups.setAdapter(dgAdapter);
        }
        renderDynamicGroups();
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            androidx.fragment.app.FragmentManager fm = getParentFragmentManager();
            if (fm.getBackStackEntryCount() > 0) fm.popBackStack();
            android.app.Activity a = getActivity();
            if (a instanceof com.lora.cn.ui.activity.MainActivity) {
                ((com.lora.cn.ui.activity.MainActivity) a).hideDeviceListImmediate();
            }
        });
        btnSave.setOnClickListener(v -> {
            androidx.fragment.app.FragmentManager fm = getParentFragmentManager();
            if (fm.getBackStackEntryCount() > 0) fm.popBackStack();
            android.app.Activity a = getActivity();
            if (a instanceof com.lora.cn.ui.activity.MainActivity) {
                ((com.lora.cn.ui.activity.MainActivity) a).hideDeviceListImmediate();
            }
        });
        btnCancel.setOnClickListener(v -> {
            androidx.fragment.app.FragmentManager fm = getParentFragmentManager();
            if (fm.getBackStackEntryCount() > 0) fm.popBackStack();
            android.app.Activity a = getActivity();
            if (a instanceof com.lora.cn.ui.activity.MainActivity) {
                ((com.lora.cn.ui.activity.MainActivity) a).hideDeviceListImmediate();
            }
        });
        // 底部完成配对按钮
        btnConfirmPair.setOnClickListener(v -> saveDevice());
        
        // Spinner选择监听
        setupSpinnerListeners();
    }
    
    private void setupSpinnerListeners() {
        // 动态渲染行中已设置监听，无需额外处理
    }

    private void loadGroupsDynamic() {}
    private void loadCategoriesDynamic(long groupId) {}
    
    private void loadCategoryData() {
        try {
            // 获取各个分组的分类数据
            // 科室分类 (group_id = 1)
            departmentCategories = dbManager.getCategoriesByGroupId(1);
            
            // 病房号分类 (group_id = 2)
            roomCategories = dbManager.getCategoriesByGroupId(2);
            
            // 护理组分类 (group_id = 3)
            nursingGroupCategories = dbManager.getCategoriesByGroupId(3);
            
            // 其他分类 (group_id = 4)
            otherCategories = dbManager.getCategoriesByGroupId(4);
            
            // 设置Spinner适配器
            setupSpinnerAdapters();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "加载分类数据失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupSpinnerAdapters() {
        // 科室Spinner
        List<String> departmentNames = new ArrayList<>();
        departmentNames.add("请选择科室");
        for (Category category : departmentCategories) {
            departmentNames.add(category.getCategoryName());
        }
        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<>(requireContext(), 
                R.layout.spinner_item_16dp, departmentNames);
        departmentAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_16dp);
        spinnerDepartment.setAdapter(departmentAdapter);
        
        // 病房号Spinner
        List<String> roomNames = new ArrayList<>();
        roomNames.add("请选择病房号");
        for (Category category : roomCategories) {
            roomNames.add(category.getCategoryName());
        }
        ArrayAdapter<String> roomAdapter = new ArrayAdapter<>(requireContext(), 
                R.layout.spinner_item_16dp, roomNames);
        roomAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_16dp);
        spinnerRoom.setAdapter(roomAdapter);
        
        // 护理组Spinner
        List<String> nursingGroupNames = new ArrayList<>();
        nursingGroupNames.add("请选择护理组");
        for (Category category : nursingGroupCategories) {
            nursingGroupNames.add(category.getCategoryName());
        }
        ArrayAdapter<String> nursingGroupAdapter = new ArrayAdapter<>(requireContext(), 
                R.layout.spinner_item_16dp, nursingGroupNames);
        nursingGroupAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_16dp);
        spinnerNursingGroup.setAdapter(nursingGroupAdapter);
        
        // 其他分类Spinner
        List<String> otherNames = new ArrayList<>();
        otherNames.add("请选择其他分类");
        for (Category category : otherCategories) {
            otherNames.add(category.getCategoryName());
        }
        ArrayAdapter<String> otherAdapter = new ArrayAdapter<>(requireContext(), 
                R.layout.spinner_item_16dp, otherNames);
        otherAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_16dp);
        spinnerOther.setAdapter(otherAdapter);
    }

    private void renderDynamicGroups() {
        try {
            if (llDynamicGroups == null) return;
            if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            int token = groupsSeq.incrementAndGet();
            ioExecutor.execute(() -> {
                java.util.List<com.lora.cn.database.entity.Group> groups;
                try {
                    groups = dbManager.getAllGroups();
                } catch (Exception e) {
                    groups = null;
                }
                java.util.Map<java.lang.Long, java.util.List<com.lora.cn.database.entity.Category>> categoriesByGroup = new java.util.HashMap<>();
                if (groups != null) {
                    for (com.lora.cn.database.entity.Group g : groups) {
                        if (destroyed) break;
                        try {
                            java.util.List<com.lora.cn.database.entity.Category> cats = dbManager.getCategoriesByGroupId(g.getGroupId());
                            categoriesByGroup.put(g.getGroupId(), cats);
                        } catch (Exception ignored2) {}
                    }
                }
                java.util.List<com.lora.cn.database.entity.Group> finalGroups = groups;
                java.util.Map<java.lang.Long, java.util.List<com.lora.cn.database.entity.Category>> finalCats = categoriesByGroup;
                mainHandler.post(() -> {
                    if (!isAdded() || destroyed) return;
                    if (token != groupsSeq.get()) return;
                    catsCache = finalCats;
                    if (finalGroups == null || finalGroups.isEmpty()) {
                        llDynamicGroups.setVisibility(View.GONE);
                        return;
                    }
                    llDynamicGroups.setVisibility(View.VISIBLE);
                    if (dgAdapter == null) {
                        androidx.recyclerview.widget.LinearLayoutManager lm = new androidx.recyclerview.widget.LinearLayoutManager(requireContext());
                        llDynamicGroups.setLayoutManager(lm);
                    }
                    dgAdapter = new com.lora.cn.ui.adapter.DynamicGroupAdapter(dbManager, selectedByGroup, terminal, finalCats);
                    llDynamicGroups.setAdapter(dgAdapter);
                    dgAdapter.submitList(finalGroups);
                });
            });
        } catch (Exception ignored) {
            LogUtils.e("ggg");
        }
    }

    private void loadTerminalAsync() {
        try {
            if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            int token = terminalSeq.incrementAndGet();
            ioExecutor.execute(() -> {
                com.lora.cn.ui.model.Terminal t = null;
                try {
                    TerminalDao dao = new TerminalDao(DatabaseHelper.getInstance(requireContext()));
                    t = dao.getTerminalByDeviceId(terminalId);
                } catch (Exception ignored) {}
                com.lora.cn.ui.model.Terminal finalT = t;
                mainHandler.post(() -> {
                    if (!isAdded() || destroyed) return;
                    if (token != terminalSeq.get()) return;
                    terminal = finalT;
                    if (terminal != null) {
                        if (!TextUtils.isEmpty(terminal.getDeviceCode())) etDeviceId.setText(terminal.getDeviceCode());
                        if (!TextUtils.isEmpty(terminal.getTerminalName())) etDeviceName.setText(terminal.getTerminalName());
                        if (llDynamicGroups != null) {
                            llDynamicGroups.setVisibility(View.VISIBLE);
                            if (catsCache != null) {
                                dgAdapter = new com.lora.cn.ui.adapter.DynamicGroupAdapter(dbManager, selectedByGroup, terminal, catsCache);
                                llDynamicGroups.setAdapter(dgAdapter);
                                if (dgAdapter.getItems() != null && !dgAdapter.getItems().isEmpty()) {
                                    dgAdapter.notifyDataSetChanged();
                                }
                            } else {
                                renderDynamicGroups();
                            }
                        }
                    }
                });
            });
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroyView() {
        try {
            if (ioExecutor != null) ioExecutor.shutdownNow();
        } catch (Exception ignored) {}
        ioExecutor = null;
        mainHandler = null;
        destroyed = true;
        super.onDestroyView();
    }
    
    @Override
    public void onStop() {
        destroyed = true;
        try {
            if (mainHandler != null) {
                // 暂无特定回调需要移除，这里保留占位
            }
            if (ioExecutor != null) ioExecutor.shutdownNow();
        } catch (Exception ignored) {}
        super.onStop();
    }
    
    private void saveDevice() {
        // 获取输入数据
        String deviceCode = etDeviceId.getText().toString().trim();
        String deviceName = etDeviceName.getText().toString().trim();
        //String deviceCode = etExtension.getText().toString().trim();
        
        // 验证必填字段
        if (TextUtils.isEmpty(deviceCode)) {
            Toast.makeText(getContext(), "请输入设备Code", Toast.LENGTH_SHORT).show();
            etDeviceId.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(deviceName)) {
            Toast.makeText(getContext(), "请输入设备名称", Toast.LENGTH_SHORT).show();
            etDeviceName.requestFocus();
            return;
        }
        
        // 判断模式：编辑跳过存在校验，新增需校验存在
        String mode = getArguments() != null ? getArguments().getString("mode", "pair") : "pair";
        boolean isEdit = "edit".equals(mode);
        if (!isEdit) {
            try {
                if (terminalDao.isDeviceIdExists(terminalId, 0)) {
                    Toast.makeText(getContext(), "设备ID已存在", Toast.LENGTH_SHORT).show();
                    etDeviceId.requestFocus();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "检查设备ID失败", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        // 创建终端对象
        Terminal terminal = new Terminal();
        terminal.setTerminalId(terminalId);
        terminal.setDeviceCode(deviceCode);
        terminal.setTerminalName(deviceName);
        int initStatus = com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ONLINE;
        if (this.terminal != null) {
            int st = this.terminal.getStatus();
            if (st == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE
                    || st == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ONLINE
                    || st == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ABNORMAL_TAKEN
                    || st == com.lora.cn.ui.constants.TerminalStatusConstants.CODE_NORMAL_TAKEN) {
                initStatus = st;
            }
        }
        terminal.setStatus(initStatus);
        // terminal.setSignalStrength(0); // 默认信号强度
        terminal.setDepartment(""); // 默认部门
        terminal.setLocation(""); // 默认位置
        if (this.terminal != null) {
            terminal.setSignalStrength(this.terminal.getSignalStrength());
            if (this.terminal.getBatteryLevel() >= 0) terminal.setBatteryLevel(this.terminal.getBatteryLevel());
            if (this.terminal.getBatteryVoltage() > 0) terminal.setBatteryVoltage(this.terminal.getBatteryVoltage());
            if (this.terminal.getRssi() > 0) terminal.setRssi(this.terminal.getRssi());
        }

        long depIdSel = 0L, roomIdSel = 0L, nurIdSel = 0L, othIdSel = 0L;
        try {
            if (spinnerDepartment != null && departmentCategories != null) {
                int pos = spinnerDepartment.getSelectedItemPosition();
                if (pos > 0 && pos - 1 < departmentCategories.size()) depIdSel = departmentCategories.get(pos - 1).getCategoryId();
            }
            if (spinnerRoom != null && roomCategories != null) {
                int pos = spinnerRoom.getSelectedItemPosition();
                if (pos > 0 && pos - 1 < roomCategories.size()) roomIdSel = roomCategories.get(pos - 1).getCategoryId();
            }
            if (spinnerNursingGroup != null && nursingGroupCategories != null) {
                int pos = spinnerNursingGroup.getSelectedItemPosition();
                if (pos > 0 && pos - 1 < nursingGroupCategories.size()) nurIdSel = nursingGroupCategories.get(pos - 1).getCategoryId();
            }
            if (spinnerOther != null && otherCategories != null) {
                int pos = spinnerOther.getSelectedItemPosition();
                if (pos > 0 && pos - 1 < otherCategories.size()) othIdSel = otherCategories.get(pos - 1).getCategoryId();
            }
        } catch (Exception ignored) {}
        if (depIdSel > 0) terminal.setDepartmentId(depIdSel);
        if (roomIdSel > 0) terminal.setRoomId(roomIdSel);
        if (nurIdSel > 0) terminal.setNursingGroupId(nurIdSel);
        if (othIdSel > 0) terminal.setOtherId(othIdSel);
        
        Long dep = selectedByGroup.get(1L);
        Long room = selectedByGroup.get(2L);
        Long nur = selectedByGroup.get(3L);
        Long oth = selectedByGroup.get(4L);
        if (dep != null && dep > 0) terminal.setDepartmentId(dep);
        if (room != null && room > 0) terminal.setRoomId(room);
        if (nur != null && nur > 0) terminal.setNursingGroupId(nur);
        if (oth != null && oth > 0) terminal.setOtherId(oth);
        try {
            StringBuilder idsSb = new StringBuilder();
            StringBuilder namesSb = new StringBuilder();
            com.lora.cn.database.DatabaseManager dm = com.lora.cn.database.DatabaseManager.getInstance(getContext());
            for (java.util.Map.Entry<Long, Long> e : selectedByGroup.entrySet()) {
                Long gid = e.getKey(); Long cid = e.getValue();
                if (gid == null || cid == null || gid <= 0L || cid <= 0L) continue;
                if (idsSb.length() > 0) idsSb.append(',');
                idsSb.append(gid).append(':').append(cid);
                String gname = String.valueOf(gid);
                String cname = String.valueOf(cid);
                try {
                    com.lora.cn.database.entity.Group g = dm.getGroupById(gid);
                    if (g != null && g.getGroupName() != null) gname = g.getGroupName();
                    com.lora.cn.database.entity.Category c = dm.getCategoryById(cid);
                    if (c != null && c.getCategoryName() != null) cname = c.getCategoryName();
                } catch (Exception ignored2) {}
                if (namesSb.length() > 0) namesSb.append(',');
                namesSb.append(gname).append('-').append(cname);
            }
            terminal.setGroupIdsText(idsSb.toString());
            terminal.setGroupNamesText(namesSb.toString());
        } catch (Exception ignored) {}
        
//        // 设置扩展字段
//        if (!TextUtils.isEmpty(deviceCode)) {
//            terminal.setDeviceCode(deviceCode);
//        }
        
        // 保存到数据库（编辑→更新；新增→插入）
        try {
            boolean ok;
            if (isEdit) {
                ok = terminalDao.updateTerminalByDeviceId(terminal) > 0;
                try {
                    DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
                    dbHelper.updateTerminalMetricsByDeviceId(terminalId,
                            Math.max(0, terminal.getBatteryLevel()),
                            Math.max(0, terminal.getRssi()),
                            Math.max(0, terminal.getBatteryVoltage()));
                } catch (Exception ignored) {}
            } else {
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
                ok = dbHelper.addTerminal(terminal) > 0;
            }
            if (ok) {
                // 记录添加终端的日志
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
                com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
                logInfo.setTerminalId(terminalId);
                logInfo.setTerminalName(deviceName);
                logInfo.setDeviceId(deviceCode);
                logInfo.setStatusCode(0);
                logInfo.setOperator("");
                logInfo.setAction(isEdit ? "编辑设备" : "添加设备");
                logInfo.setOperationTime("");
                logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
                
                //dbHelper.addLog(logInfo);
                
                Toast.makeText(getContext(), isEdit ? "编辑终端成功" : "添加终端成功", Toast.LENGTH_SHORT).show();
                
                if (!isEdit) {
                    new Thread(() -> {
                        try {
                            android.content.Context ctx = getContext();
                            android.content.Context appCtx = ctx != null ? ctx.getApplicationContext() : null;
                            DatabaseHelper helper = DatabaseHelper.getInstance(appCtx != null ? appCtx : ctx);
                            helper.syncUnboundLogsToBound();
                        } catch (Exception ignored3) {}
                    }).start();
                }
                
                // 发送EventBus事件通知TerminalListFragment刷新
                EventBus.getDefault().post(new TerminalRefreshEvent((isEdit ? "编辑终端: " : "新增终端: ") + deviceName));
                
                // 返回上一页
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            } else {
                // 记录添加失败的日志
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
                com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
                logInfo.setTerminalId(terminalId);
                logInfo.setTerminalName(deviceName);
                logInfo.setDeviceId(deviceCode);
                logInfo.setStatusCode(0);
                logInfo.setOperator("");
                logInfo.setAction(isEdit ? "编辑设备" : "添加设备");
                logInfo.setOperationTime("");
                logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
                
                //dbHelper.addLog(logInfo);
                
                Toast.makeText(getContext(), isEdit ? "编辑终端失败" : "添加终端失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            
            // 记录异常日志
            try {
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
                com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
                logInfo.setTerminalId(terminalId);
                logInfo.setTerminalName(deviceName);
                logInfo.setDeviceId(deviceCode);
                logInfo.setStatusCode(0);
                logInfo.setOperator("");
                logInfo.setAction(isEdit ? "编辑设备" : "添加设备");
                logInfo.setOperationTime("");
                logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
                
                //dbHelper.addLog(logInfo);
            } catch (Exception logException) {
                logException.printStackTrace();
            }
            
            Toast.makeText(getContext(), (isEdit ? "编辑终端失败: " : "添加终端失败: ") + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
