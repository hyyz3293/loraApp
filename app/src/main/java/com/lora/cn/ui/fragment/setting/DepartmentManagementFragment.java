package com.lora.cn.ui.fragment.setting;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.Department;
import com.lora.cn.database.entity.User;
import com.lora.cn.ui.adapter.DepartmentAdapter;
import com.lora.cn.utils.DialogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.constant.SpConstant;

import java.util.ArrayList;
import java.util.List;

public class DepartmentManagementFragment extends Fragment {

    private TextView back;
    private TextView btnAdd;
    private RecyclerView rvDepartments;
    
    private DepartmentAdapter departmentAdapter;
    private DatabaseManager dbManager;
    private List<Department> allDepartments = new ArrayList<>();
    private long currentUserRoleId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_department_management, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadDepartments();
        
        return view;
    }

    private void initViews(View view) {
        // 工具栏
        btnAdd = view.findViewById(R.id.btn_add_department);
        back = view.findViewById(R.id.back);
        
        // 列表
        rvDepartments = view.findViewById(R.id.rv_departments);
        
        // 初始化数据库管理器
        dbManager = DatabaseManager.getInstance(requireContext());
        allDepartments = new ArrayList<>();
        
        // 初始化用户角色ID
        long userId = SPUtils.getInstance().getLong("current_user_id", -1);
        if (userId != -1) {
            User user = dbManager.getUserById(userId);
            if (user != null) {
                currentUserRoleId = user.getRoleId();
            }
        }
    }
    
    private void setupRecyclerView() {
        departmentAdapter = new DepartmentAdapter();
        rvDepartments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDepartments.setAdapter(departmentAdapter);
    }
    
    private void setupListeners() {
        // 返回按钮
        back.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
        
        // 新增按钮 - 检查权限
        if (hasPermission("DEPARTMENT_ADD")) {
            btnAdd.setOnClickListener(v -> showAddDepartmentDialog());
        } else {
            btnAdd.setVisibility(View.GONE);
        }
        
        // 设置适配器点击监听器
        departmentAdapter.addOnItemChildClickListener(R.id.tv_department_fz, new BaseQuickAdapter.OnItemChildClickListener<Department>() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<Department, ?> baseQuickAdapter, @NonNull View view, int i) {
                Department department = baseQuickAdapter.getItem(i);
                if (department != null) {
                    onItemClickItem(department);
                }
            }
        });
        
        departmentAdapter.addOnItemChildClickListener(R.id.tv_department_edit, new BaseQuickAdapter.OnItemChildClickListener<Department>() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<Department, ?> baseQuickAdapter, @NonNull View view, int i) {
                Department department = baseQuickAdapter.getItem(i);
                if (department != null && hasPermission("DEPARTMENT_EDIT")) {
                    onEditClick(department);
                } else {
                    Toast.makeText(requireContext(), "您没有编辑科室的权限", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        departmentAdapter.addOnItemChildClickListener(R.id.tv_department_delete, new BaseQuickAdapter.OnItemChildClickListener<Department>() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<Department, ?> baseQuickAdapter, @NonNull View view, int i) {
                Department department = baseQuickAdapter.getItem(i);
                if (department != null && hasPermission("DEPARTMENT_DELETE")) {
                    onDeleteClick(department);
                } else {
                    Toast.makeText(requireContext(), "您没有删除科室的权限", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        departmentAdapter.addOnItemChildClickListener(R.id.switch_department_status, new BaseQuickAdapter.OnItemChildClickListener<Department>() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<Department, ?> baseQuickAdapter, @NonNull View view, int i) {
                Department department = baseQuickAdapter.getItem(i);
                if (department != null && hasPermission("DEPARTMENT_STATUS")) {
                    SwitchCompat switchStatus = (SwitchCompat) view;
                    onStatusChanged(department, switchStatus.isChecked());
                } else {
                    // 恢复开关状态
                    SwitchCompat switchStatus = (SwitchCompat) view;
                    switchStatus.setChecked(department.getStatus() == 1);
                    Toast.makeText(requireContext(), "您没有修改科室状态的权限", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void loadDepartments() {
        try {
            allDepartments = dbManager.getAllDepartments();
            departmentAdapter.submitList(allDepartments);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "加载科室失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    

    
    private void showAddDepartmentDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_department, null);
        
        EditText etDepartmentName = dialogView.findViewById(R.id.et_department_name);
        EditText etSortOrder = dialogView.findViewById(R.id.et_sort_order);
        SwitchCompat switchStatus = dialogView.findViewById(R.id.switch_status);
        
        // 设置默认值
        etSortOrder.setText("1");
        switchStatus.setChecked(true);
        
        new AlertDialog.Builder(getContext())
                .setTitle("新增科室")
                .setView(dialogView)
                .setPositiveButton("确定", (dialog, which) -> {
                    String name = etDepartmentName.getText().toString().trim();
                    String sortOrderStr = etSortOrder.getText().toString().trim();
                    
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(getContext(), "请输入科室名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    int sortOrder = 1;
                    if (!TextUtils.isEmpty(sortOrderStr)) {
                        try {
                            sortOrder = Integer.parseInt(sortOrderStr);
                        } catch (NumberFormatException e) {
                            sortOrder = 1;
                        }
                    }
                    
                    int status = switchStatus.isChecked() ? 1 : 0;
                    addDepartment(name, sortOrder, status);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void showEditDepartmentDialog(Department department) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_department, null);
        
        EditText etDepartmentName = dialogView.findViewById(R.id.et_department_name);
        EditText etSortOrder = dialogView.findViewById(R.id.et_sort_order);
        SwitchCompat switchStatus = dialogView.findViewById(R.id.switch_status);
        
        // 设置当前值
        etDepartmentName.setText(department.getDepartmentName());
        etSortOrder.setText(String.valueOf(department.getSortOrder()));
        switchStatus.setChecked(department.getStatus() == 1);
        
        new AlertDialog.Builder(getContext())
                .setTitle("编辑科室")
                .setView(dialogView)
                .setPositiveButton("确定", (dialog, which) -> {
                    String name = etDepartmentName.getText().toString().trim();
                    String sortOrderStr = etSortOrder.getText().toString().trim();
                    
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(getContext(), "请输入科室名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    int sortOrder = department.getSortOrder();
                    if (!TextUtils.isEmpty(sortOrderStr)) {
                        try {
                            sortOrder = Integer.parseInt(sortOrderStr);
                        } catch (NumberFormatException e) {
                            sortOrder = department.getSortOrder();
                        }
                    }
                    
                    int status = switchStatus.isChecked() ? 1 : 0;
                    updateDepartment((int)department.getDepartmentId(), name, sortOrder, status);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    

    
    private void addDepartment(String name, int sortOrder, int status) {
        try {
            Department department = new Department();
            department.setDepartmentName(name);
            department.setSortOrder(sortOrder);
            department.setStatus(status);
            
            long result = dbManager.insertDepartment(department);
            if (result > 0) {
                Toast.makeText(requireContext(), "科室添加成功", Toast.LENGTH_SHORT).show();
                loadDepartments();
            } else {
                Toast.makeText(requireContext(), "科室添加失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "添加科室失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateDepartment(int departmentId, String name, int sortOrder, int status) {
        try {
            Department department = new Department();
            department.setDepartmentId((long)departmentId);
            department.setDepartmentName(name);
            department.setSortOrder(sortOrder);
            department.setStatus(status);
            
            int result = dbManager.updateDepartment(department);
            if (result > 0) {
                Toast.makeText(requireContext(), "科室更新成功", Toast.LENGTH_SHORT).show();
                loadDepartments();
            } else {
                Toast.makeText(requireContext(), "科室更新失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "更新科室失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void deleteDepartment(Department department) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除确认")
                .setMessage("确定要删除科室 \"" + department.getDepartmentName() + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    try {
                        int result = dbManager.deleteDepartment(department.getDepartmentId());
                        if (result > 0) {
                            Toast.makeText(requireContext(), "科室删除成功", Toast.LENGTH_SHORT).show();
                            loadDepartments();
                        } else {
                            Toast.makeText(requireContext(), "科室删除失败", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "删除科室失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    // 点击事件处理方法
    private void onEditClick(Department department) {
        showEditDepartmentDialog(department);
    }
    
    private void onDeleteClick(Department department) {
        deleteDepartment(department);
    }
    
    private void onItemClickItem(Department department) {
        // 可以在这里处理科室项点击事件，比如跳转到详情页面
        Toast.makeText(requireContext(), "点击了科室: " + department.getDepartmentName(), Toast.LENGTH_SHORT).show();
    }
    
    private void onStatusChanged(Department department, boolean isChecked) {
         int newStatus = isChecked ? 1 : 0;
         updateDepartment((int)department.getDepartmentId(), department.getDepartmentName(), 
                         department.getSortOrder(), newStatus);
     }

    /**
     * 检查当前用户是否有指定权限
     */
    private boolean hasPermission(String permissionCode) {
        if (currentUserRoleId <= 0) {
            return false;
        }
        return dbManager.hasPermission((int)currentUserRoleId, permissionCode);
    }

    public static DepartmentManagementFragment newInstance() {
        return new DepartmentManagementFragment();
    }
}