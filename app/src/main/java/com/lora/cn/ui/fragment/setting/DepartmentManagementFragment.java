package com.lora.cn.ui.fragment.setting;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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

import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.dao.DepartmentDao;
import com.lora.cn.database.entity.Department;
import com.lora.cn.ui.adapter.DepartmentAdapter;

import java.util.ArrayList;
import java.util.List;

public class DepartmentManagementFragment extends Fragment implements DepartmentAdapter.OnDepartmentItemClickListener {

    private TextView titleText;
    private ImageView backButton;
    private TextView btnAdd;
    private EditText etSearch;
    private RecyclerView rvDepartments;
    
    private DepartmentAdapter departmentAdapter;
    private DepartmentDao departmentDao;
    private List<Department> allDepartments = new ArrayList<>();

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
        titleText = view.findViewById(R.id.setting_title);
        backButton = view.findViewById(R.id.setting_back);
        btnAdd = view.findViewById(R.id.btn_add_department);
        etSearch = view.findViewById(R.id.et_search);
        rvDepartments = view.findViewById(R.id.rv_departments);
        
        if (titleText != null) {
            titleText.setText("科室管理");
        }
        
        // 初始化数据库操作类
        DatabaseManager databaseManager = DatabaseManager.getInstance(requireContext());
        departmentDao = new DepartmentDao(databaseManager.getReadableDatabase());
    }
    
    private void setupRecyclerView() {
        departmentAdapter = new DepartmentAdapter();
        departmentAdapter.setOnDepartmentItemClickListener(this);
        
        rvDepartments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDepartments.setAdapter(departmentAdapter);
    }
    
    private void setupListeners() {
        // 返回按钮
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }
        
        // 新增按钮
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> showAddDepartmentDialog());
        }
        
        // 搜索功能
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchDepartments(s.toString());
                }
                
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }
    
    private void loadDepartments() {
        allDepartments = departmentDao.getAllDepartments();
        departmentAdapter.submitList(new ArrayList<>(allDepartments));
    }
    
    private void searchDepartments(String query) {
        if (TextUtils.isEmpty(query)) {
            departmentAdapter.submitList(new ArrayList<>(allDepartments));
        } else {
            List<Department> filteredList = new ArrayList<>();
            for (Department department : allDepartments) {
                if (department.getDepartmentName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(department);
                }
            }
            departmentAdapter.submitList(filteredList);
        }
    }
    
    private void showAddDepartmentDialog() {
        showDepartmentDialog(null);
    }
    
    private void showEditDepartmentDialog(Department department) {
        showDepartmentDialog(department);
    }
    
    private void showDepartmentDialog(Department department) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_department, null);
        
        EditText etDepartmentName = dialogView.findViewById(R.id.et_department_name);
        EditText etSortOrder = dialogView.findViewById(R.id.et_sort_order);
        SwitchCompat switchStatus = dialogView.findViewById(R.id.switch_status);
        
        boolean isEdit = department != null;
        if (isEdit) {
            etDepartmentName.setText(department.getDepartmentName());
            etSortOrder.setText(String.valueOf(department.getSortOrder()));
            switchStatus.setChecked(department.getStatus() == 1);
        }
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(isEdit ? "编辑科室" : "新增科室")
                .setView(dialogView)
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create();
        
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = etDepartmentName.getText().toString().trim();
                String sortStr = etSortOrder.getText().toString().trim();
                
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(getContext(), "请输入科室名称", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (TextUtils.isEmpty(sortStr)) {
                    Toast.makeText(getContext(), "请输入排序", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                int sortOrder;
                try {
                    sortOrder = Integer.parseInt(sortStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "排序必须是数字", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                int status = switchStatus.isChecked() ? 1 : 0;
                
                if (isEdit) {
                    updateDepartment(department.getDepartmentId(), name, sortOrder, status);
                } else {
                    addDepartment(name, sortOrder, status);
                }
                
                dialog.dismiss();
            });
        });
        
        dialog.show();
    }
    
    private void addDepartment(String name, int sortOrder, int status) {
        // 检查名称是否已存在
        if (departmentDao.isDepartmentNameExists(name)) {
            Toast.makeText(getContext(), "科室名称已存在", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Department department = new Department();
        department.setDepartmentName(name);
        department.setSortOrder(sortOrder);
        department.setStatus(status);
        
        long id = departmentDao.insertDepartment(department);
        if (id > 0) {
            department.setDepartmentId(id);
            departmentAdapter.addDepartment(department);
            allDepartments.add(department);
            Toast.makeText(getContext(), "添加成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "添加失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateDepartment(long departmentId, String name, int sortOrder, int status) {
        Department department = new Department();
        department.setDepartmentId(departmentId);
        department.setDepartmentName(name);
        department.setSortOrder(sortOrder);
        department.setStatus(status);
        
        boolean success = departmentDao.updateDepartment(department);
        if (success) {
            departmentAdapter.updateDepartment(department);
            // 更新本地列表
            for (int i = 0; i < allDepartments.size(); i++) {
                if (allDepartments.get(i).getDepartmentId() == departmentId) {
                    allDepartments.set(i, department);
                    break;
                }
            }
            Toast.makeText(getContext(), "更新成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "更新失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void deleteDepartment(Department department) {
        new AlertDialog.Builder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除科室 \"" + department.getDepartmentName() + "\" 吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    boolean success = departmentDao.deleteDepartment(department.getDepartmentId());
                    if (success) {
                        departmentAdapter.removeDepartment(department.getDepartmentId());
                        allDepartments.removeIf(d -> d.getDepartmentId() == department.getDepartmentId());
                        Toast.makeText(getContext(), "删除成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "删除失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    @Override
    public void onEditClick(Department department) {
        showEditDepartmentDialog(department);
    }
    
    @Override
    public void onDeleteClick(Department department) {
        deleteDepartment(department);
    }
    
    @Override
    public void onItemClick(Department department) {
        // 可以在这里处理科室项点击事件
        Toast.makeText(getContext(), "点击了科室: " + department.getDepartmentName(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onStatusChanged(Department department, boolean isEnabled) {
        // 更新科室状态
        int newStatus = isEnabled ? 1 : 0;
        updateDepartment(department.getDepartmentId(), department.getDepartmentName(), 
                        department.getSortOrder(), newStatus);
    }

    public static DepartmentManagementFragment newInstance() {
        return new DepartmentManagementFragment();
    }
}