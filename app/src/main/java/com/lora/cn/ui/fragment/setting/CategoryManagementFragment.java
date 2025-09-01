package com.lora.cn.ui.fragment.setting;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.Category;
import com.lora.cn.database.entity.Group;
import com.lora.cn.ui.adapter.CategoryAdapter;

import java.util.ArrayList;
import java.util.List;

public class CategoryManagementFragment extends Fragment implements CategoryAdapter.OnCategoryItemClickListener {

    private TextView toolbarTitle;
    private ImageView btnBack;
    private ImageView btnAdd;
    private Spinner spinnerGroups;
    private EditText etSearch;
    private Button btnSearch;
    private RecyclerView rvCategories;
    
    private CategoryAdapter categoryAdapter;
    private DatabaseManager dbManager;
    private List<Category> allCategories;
    private List<Group> allGroups;
    private ArrayAdapter<Group> groupSpinnerAdapter;
    private long selectedGroupId = -1; // -1表示显示所有分组的分类

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_management, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupSpinner();
        setupListeners();
        loadData();
        
        return view;
    }

    private void initViews(View view) {
        // 工具栏
        toolbarTitle = view.findViewById(R.id.toolbar_title);
        btnBack = view.findViewById(R.id.btn_back);
        btnAdd = view.findViewById(R.id.btn_add);
        
        // 分组选择器
        spinnerGroups = view.findViewById(R.id.spinner_groups);
        
        // 搜索
        etSearch = view.findViewById(R.id.et_search);
        btnSearch = view.findViewById(R.id.btn_search);
        
        // 列表
        rvCategories = view.findViewById(R.id.rv_categories);
        
        // 设置标题
        toolbarTitle.setText("分类管理");
        
        // 初始化数据库管理器
        dbManager = DatabaseManager.getInstance(requireContext());
        allCategories = new ArrayList<>();
        allGroups = new ArrayList<>();
    }

    private void setupRecyclerView() {
        categoryAdapter = new CategoryAdapter();
        categoryAdapter.setOnCategoryItemClickListener(this);
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupSpinner() {
        groupSpinnerAdapter = new ArrayAdapter<Group>(requireContext(), android.R.layout.simple_spinner_item, allGroups) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                Group group = getItem(position);
                if (group != null) {
                    if (group.getGroupId() == -1) {
                        textView.setText(group.getGroupName());
                    } else {
                        textView.setText(group.getGroupName());
                    }
                }
                return view;
            }
            
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                Group group = getItem(position);
                if (group != null) {
                    textView.setText(group.getGroupName());
                }
                return view;
            }
        };
        groupSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGroups.setAdapter(groupSpinnerAdapter);
    }

    private void setupListeners() {
        // 返回按钮
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
        
        // 新增按钮
        btnAdd.setOnClickListener(v -> showAddCategoryDialog());
        
        // 搜索按钮
        btnSearch.setOnClickListener(v -> performSearch());
        
        // 分组选择监听
        spinnerGroups.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Group selectedGroup = allGroups.get(position);
                selectedGroupId = selectedGroup.getGroupId();
                filterCategoriesByGroup();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedGroupId = -1;
                filterCategoriesByGroup();
            }
        });
        
        // 搜索框文本变化监听
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(s)) {
                    filterCategoriesByGroup();
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadData() {
        loadGroups();
        loadCategories();
    }

    private void loadGroups() {
        try {
            List<Group> groups = dbManager.getAllGroups();
            allGroups.clear();
            
            // 添加"所有分组"选项
            Group allGroupsOption = new Group();
            allGroupsOption.setGroupId(-1);
            allGroupsOption.setGroupName("所有分组");
            allGroups.add(allGroupsOption);
            
            allGroups.addAll(groups);
            groupSpinnerAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "加载分组失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadCategories() {
        try {
            allCategories = dbManager.getCategoriesWithGroup();
            filterCategoriesByGroup();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "加载分类失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void filterCategoriesByGroup() {
        List<Category> filteredCategories = new ArrayList<>();
        
        if (selectedGroupId == -1) {
            // 显示所有分类
            filteredCategories.addAll(allCategories);
        } else {
            // 按分组过滤
            for (Category category : allCategories) {
                if (category.getGroupId() == selectedGroupId) {
                    filteredCategories.add(category);
                }
            }
        }
        
        categoryAdapter.setCategoryList(filteredCategories);
    }

    private void performSearch() {
        String keyword = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            filterCategoriesByGroup();
            return;
        }
        
        List<Category> baseList = new ArrayList<>();
        if (selectedGroupId == -1) {
            baseList.addAll(allCategories);
        } else {
            for (Category category : allCategories) {
                if (category.getGroupId() == selectedGroupId) {
                    baseList.add(category);
                }
            }
        }
        
        List<Category> filteredCategories = new ArrayList<>();
        for (Category category : baseList) {
            if (category.getCategoryName().toLowerCase().contains(keyword.toLowerCase()) ||
                category.getCategoryDescription().toLowerCase().contains(keyword.toLowerCase())) {
                filteredCategories.add(category);
            }
        }
        categoryAdapter.setCategoryList(filteredCategories);
    }

    private void showAddCategoryDialog() {
        if (allGroups.size() <= 1) { // 只有"所有分组"选项
            Toast.makeText(requireContext(), "请先创建分组", Toast.LENGTH_SHORT).show();
            return;
        }
        showCategoryDialog(null, "新增分类");
    }

    private void showEditCategoryDialog(Category category) {
        showCategoryDialog(category, "编辑分类");
    }

    private void showCategoryDialog(Category category, String title) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_category, null);
        
        EditText etName = dialogView.findViewById(R.id.et_category_name);
        EditText etDescription = dialogView.findViewById(R.id.et_category_description);
        Spinner spinnerGroup = dialogView.findViewById(R.id.spinner_group);
        
        // 设置分组选择器
        List<Group> availableGroups = new ArrayList<>();
        for (Group group : allGroups) {
            if (group.getGroupId() != -1) { // 排除"所有分组"选项
                availableGroups.add(group);
            }
        }
        
        ArrayAdapter<Group> dialogGroupAdapter = new ArrayAdapter<Group>(requireContext(), 
                android.R.layout.simple_spinner_item, availableGroups) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                Group group = getItem(position);
                if (group != null) {
                    textView.setText(group.getGroupName());
                }
                return view;
            }
            
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                Group group = getItem(position);
                if (group != null) {
                    textView.setText(group.getGroupName());
                }
                return view;
            }
        };
        dialogGroupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGroup.setAdapter(dialogGroupAdapter);
        
        if (category != null) {
            etName.setText(category.getCategoryName());
            etDescription.setText(category.getCategoryDescription());
            
            // 设置分组选择
            for (int i = 0; i < availableGroups.size(); i++) {
                if (availableGroups.get(i).getGroupId() == category.getGroupId()) {
                    spinnerGroup.setSelection(i);
                    break;
                }
            }
        }
        
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("确定", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();
                    
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(requireContext(), "请输入分类名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (spinnerGroup.getSelectedItem() == null) {
                        Toast.makeText(requireContext(), "请选择分组", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    Group selectedGroup = (Group) spinnerGroup.getSelectedItem();
                    long groupId = selectedGroup.getGroupId();
                    
                    if (category == null) {
                        addCategory(name, description, groupId);
                    } else {
                        updateCategory(category.getCategoryId(), name, description, groupId);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void addCategory(String name, String description, long groupId) {
        try {
            long categoryId = dbManager.addCategory(name, description, groupId);
            if (categoryId > 0) {
                Toast.makeText(requireContext(), "添加成功", Toast.LENGTH_SHORT).show();
                loadCategories();
            } else {
                Toast.makeText(requireContext(), "添加失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "添加失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCategory(long categoryId, String name, String description, long groupId) {
        try {
            boolean success = dbManager.updateCategory(categoryId, name, description, groupId);
            if (success) {
                Toast.makeText(requireContext(), "更新成功", Toast.LENGTH_SHORT).show();
                loadCategories();
            } else {
                Toast.makeText(requireContext(), "更新失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "更新失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteCategory(Category category) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除确认")
                .setMessage("确定要删除分类 \"" + category.getCategoryName() + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    try {
                        boolean success = dbManager.deleteCategory(category.getCategoryId());
                        if (success) {
                            Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show();
                            loadCategories();
                        } else {
                            Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onEditClick(Category category) {
        showEditCategoryDialog(category);
    }

    @Override
    public void onDeleteClick(Category category) {
        deleteCategory(category);
    }

    @Override
    public void onItemClick(Category category) {
        // 可以在这里实现点击分类查看详情的功能
        Toast.makeText(requireContext(), "点击了分类: " + category.getCategoryName(), Toast.LENGTH_SHORT).show();
    }

    public static CategoryManagementFragment newInstance() {
        return new CategoryManagementFragment();
    }
}