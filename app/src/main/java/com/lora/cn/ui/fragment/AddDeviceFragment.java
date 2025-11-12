package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
    private EditText etExtension; // 用作设备CODE输入框（字段映射为device_code）
    private Spinner spinnerDepartment;
    private Spinner spinnerRoom;
    private Spinner spinnerNursingGroup;
    private Spinner spinnerOther;
    private TextView btnBack;
    private TextView btnSave;
    private TextView btnCancel;
    private TextView btnConfirmPair;
    
    private DatabaseManager dbManager;
    private TerminalDao terminalDao;
    private Terminal terminal;
    private String terminalId;
    
    // 分类数据
    private List<Category> departmentCategories = new ArrayList<>();
    private List<Category> roomCategories = new ArrayList<>();
    private List<Category> nursingGroupCategories = new ArrayList<>();
    private List<Category> otherCategories = new ArrayList<>();
    
    // 选中的分类ID
    private Integer selectedDepartmentId = null;
    private Integer selectedRoomId = null;
    private Integer selectedNursingGroupId = null;
    private Integer selectedOtherId = null;
    
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
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String  terminalGson = getArguments().getString("terminal");
            terminal = new Gson().fromJson(terminalGson, Terminal.class);
            terminalId = terminal.getTerminalId();
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
        initData();
        setupListeners();
        loadCategoryData();
    }
    
    private void initViews(View view) {
        tvTerminalId = view.findViewById(R.id.tv_terminal_id);
        etDeviceId = view.findViewById(R.id.et_device_id);
        etDeviceName = view.findViewById(R.id.et_device_name);
        etExtension = view.findViewById(R.id.et_extension);
        spinnerDepartment = view.findViewById(R.id.spinner_department);
        spinnerRoom = view.findViewById(R.id.spinner_room);
        spinnerNursingGroup = view.findViewById(R.id.spinner_nursing_group);
        spinnerOther = view.findViewById(R.id.spinner_other);
        btnBack = view.findViewById(R.id.btn_back);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnConfirmPair = view.findViewById(R.id.btn_confirm_pair);
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
    }
    
    private void setupListeners() {
        // 顶部右侧返回按钮
        btnSave.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
        // 底部取消按钮
        btnCancel.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
        // 底部完成配对按钮
        btnConfirmPair.setOnClickListener(v -> saveDevice());
        
        // Spinner选择监听
        setupSpinnerListeners();
    }
    
    private void setupSpinnerListeners() {
        // 科室选择监听
        spinnerDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position <= departmentCategories.size()) {
                    selectedDepartmentId = (int) departmentCategories.get(position - 1).getCategoryId();
                } else {
                    selectedDepartmentId = null;
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDepartmentId = null;
            }
        });
        
        // 病房号选择监听
        spinnerRoom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position <= roomCategories.size()) {
                    selectedRoomId = (int) roomCategories.get(position - 1).getCategoryId();
                } else {
                    selectedRoomId = null;
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedRoomId = null;
            }
        });
        
        // 护理组选择监听
        spinnerNursingGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position <= nursingGroupCategories.size()) {
                    selectedNursingGroupId = (int) nursingGroupCategories.get(position - 1).getCategoryId();
                } else {
                    selectedNursingGroupId = null;
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedNursingGroupId = null;
            }
        });
        
        // 其他分类选择监听
        spinnerOther.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position <= otherCategories.size()) {
                    selectedOtherId = (int) otherCategories.get(position - 1).getCategoryId();
                } else {
                    selectedOtherId = null;
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedOtherId = null;
            }
        });
    }
    
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
                android.R.layout.simple_spinner_item, departmentNames);
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(departmentAdapter);
        
        // 病房号Spinner
        List<String> roomNames = new ArrayList<>();
        roomNames.add("请选择病房号");
        for (Category category : roomCategories) {
            roomNames.add(category.getCategoryName());
        }
        ArrayAdapter<String> roomAdapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_spinner_item, roomNames);
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoom.setAdapter(roomAdapter);
        
        // 护理组Spinner
        List<String> nursingGroupNames = new ArrayList<>();
        nursingGroupNames.add("请选择护理组");
        for (Category category : nursingGroupCategories) {
            nursingGroupNames.add(category.getCategoryName());
        }
        ArrayAdapter<String> nursingGroupAdapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_spinner_item, nursingGroupNames);
        nursingGroupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNursingGroup.setAdapter(nursingGroupAdapter);
        
        // 其他分类Spinner
        List<String> otherNames = new ArrayList<>();
        otherNames.add("请选择其他分类");
        for (Category category : otherCategories) {
            otherNames.add(category.getCategoryName());
        }
        ArrayAdapter<String> otherAdapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_spinner_item, otherNames);
        otherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOther.setAdapter(otherAdapter);
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
        
        // 检查设备ID是否已存在
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
        
        // 创建终端对象
        Terminal terminal = new Terminal();
        terminal.setTerminalId(terminalId);
        terminal.setDeviceCode(deviceCode);
        terminal.setTerminalName(deviceName);
        terminal.setStatus(com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ONLINE); // 默认状态
        terminal.setSignalStrength(0); // 默认信号强度
        terminal.setDepartment(""); // 默认部门
        terminal.setLocation(""); // 默认位置
        
        // 设置分类ID
        terminal.setDepartmentId(selectedDepartmentId != null ? selectedDepartmentId : 0);
        terminal.setRoomId(selectedRoomId != null ? selectedRoomId : 0);
        terminal.setNursingGroupId(selectedNursingGroupId != null ? selectedNursingGroupId : 0);
        terminal.setOtherId(selectedOtherId != null ? selectedOtherId : 0);
        
//        // 设置扩展字段
//        if (!TextUtils.isEmpty(deviceCode)) {
//            terminal.setDeviceCode(deviceCode);
//        }
        
        // 保存到数据库
        try {
            long result = terminalDao.insertTerminal(terminal);
            if (result > 0) {
                // 记录添加终端的日志
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
                com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
                logInfo.setTerminalId(terminalId);
                logInfo.setTerminalName(deviceName);
                logInfo.setDeviceId(deviceCode);
                logInfo.setStatus("成功");
                logInfo.setOperator("系统管理员"); // 这里可以根据实际登录用户设置
                logInfo.setAction("添加设备");
                logInfo.setOperationTime(String.valueOf(System.currentTimeMillis()));
                logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
                
                dbHelper.addLog(logInfo);
                
                Toast.makeText(getContext(), "添加终端成功", Toast.LENGTH_SHORT).show();
                
                // 发送EventBus事件通知TerminalListFragment刷新
                EventBus.getDefault().post(new TerminalRefreshEvent("新增终端: " + deviceName));
                
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
                logInfo.setStatus("失败");
                logInfo.setOperator("系统管理员");
                logInfo.setAction("添加设备");
                logInfo.setOperationTime(String.valueOf(System.currentTimeMillis()));
                logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
                
                dbHelper.addLog(logInfo);
                
                Toast.makeText(getContext(), "添加终端失败", Toast.LENGTH_SHORT).show();
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
                logInfo.setStatus("异常");
                logInfo.setOperator("系统管理员");
                logInfo.setAction("添加设备");
                logInfo.setOperationTime(String.valueOf(System.currentTimeMillis()));
                logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
                
                dbHelper.addLog(logInfo);
            } catch (Exception logException) {
                logException.printStackTrace();
            }
            
            Toast.makeText(getContext(), "添加终端失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
