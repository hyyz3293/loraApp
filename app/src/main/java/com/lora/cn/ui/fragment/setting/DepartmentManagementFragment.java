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
import com.lora.cn.database.entity.Category;
import com.lora.cn.ui.adapter.CategoryAdapter;
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
        
        // 新增按钮 - 检查权限（统一使用设置模块权限）
        if (hasPermission("setting")) {
            btnAdd.setOnClickListener(v -> showAddDepartmentDialog());
        } else {
            btnAdd.setVisibility(View.GONE);
        }
        
        // 设置适配器点击监听器
        departmentAdapter.addOnItemChildClickListener(R.id.tv_department_edit, new BaseQuickAdapter.OnItemChildClickListener<Department>() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<Department, ?> baseQuickAdapter, @NonNull View view, int i) {
                Department dept = baseQuickAdapter.getItem(i);
                if (dept != null && hasPermission("setting")) {
                    showEditDepartmentDialog(dept);
                } else {
                    Toast.makeText(requireContext(), "您没有编辑科室的权限", Toast.LENGTH_SHORT).show();
                }
            }
        });

        departmentAdapter.addOnItemChildClickListener(R.id.tv_department_delete, new BaseQuickAdapter.OnItemChildClickListener<Department>() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<Department, ?> baseQuickAdapter, @NonNull View view, int i) {
                Department dept = baseQuickAdapter.getItem(i);
                if (dept != null && hasPermission("setting")) {
                    deleteDepartment(dept);
                } else {
                    Toast.makeText(requireContext(), "您没有删除科室的权限", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // 分类不支持状态开关，移除相关监听
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
        DialogUtils.showTextEditDialog(
                getContext(),
                "新增科室",
                "科室名称",
                "",
                "",
                newValue -> {
                    if (TextUtils.isEmpty(newValue)) {
                        Toast.makeText(requireContext(), "请输入科室名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        int sort = dbManager.getDepartmentCount() + 1;
                        long id = dbManager.addDepartment(newValue, sort, 1);
                        if (id > 0) {
                            Toast.makeText(requireContext(), "科室添加成功", Toast.LENGTH_SHORT).show();
                            loadDepartments();
                        } else {
                            Toast.makeText(requireContext(), "科室添加失败", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "添加科室失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    
    private void showEditDepartmentDialog(Department dept) {
        DialogUtils.showTextEditDialog(
                getContext(),
                "编辑科室",
                "科室名称",
                dept.getDepartmentName(),
                "",
                newValue -> {
                    if (TextUtils.isEmpty(newValue)) {
                        Toast.makeText(requireContext(), "请输入科室名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        dept.setDepartmentName(newValue);
                        int r = dbManager.updateDepartment(dept);
                        if (r > 0) {
                            Toast.makeText(requireContext(), "科室更新成功", Toast.LENGTH_SHORT).show();
                            loadDepartments();
                        } else {
                            Toast.makeText(requireContext(), "科室更新失败", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "更新科室失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    

    
    // 科室以分组-分类的“科室”分组下的分类动态展示；新增/编辑/删除均操作Category表
    
    private void deleteDepartment(Department dept) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除确认")
                .setMessage("确定要删除科室 \"" + dept.getDepartmentName() + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    try {
                        int r = dbManager.deleteDepartment(dept.getDepartmentId());
                        if (r > 0) {
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
    private void onItemClickItem(Department dept) {
        Toast.makeText(requireContext(), "点击了科室: " + dept.getDepartmentName(), Toast.LENGTH_SHORT).show();
    }
    
    // 已移除状态切换，分类不包含状态字段

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
