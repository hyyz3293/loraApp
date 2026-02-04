package com.lora.cn.ui.fragment.setting.group;

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

import com.chad.library.adapter4.BaseQuickAdapter;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.Category;
import com.lora.cn.database.entity.Group;
import com.lora.cn.ui.adapter.CategoryAdapter;
import com.lora.cn.utils.DialogUtils;

import java.util.ArrayList;
import java.util.List;

public class CategoryManagementFragment extends Fragment{

    private TextView back;
    private TextView btnAddTv;

    private TextView toolbarTitle;
    private ImageView btnBack;
    private ImageView btnAdd;
    private Spinner spinnerGroups;
    private Spinner spinnerCategories;
    private EditText etSearch;
    private Button btnSearch;
    private RecyclerView rvCategories;
    
    private CategoryAdapter categoryAdapter;
    private DatabaseManager dbManager;
    private List<Category> allCategories;
    //private List<Group> allGroups;
    //private ArrayAdapter<Group> groupSpinnerAdapter;
    private long selectedGroupId = -1; // -1表示显示所有分组的分类

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_management, container, false);

        // 处理从分组管理页面传递过来的参数
        handleArgumentsFromGroupManagement();

        initViews(view);
        setupRecyclerView();

        setupListeners();
        loadData();
        

        
        return view;
    }
    
    private void handleArgumentsFromGroupManagement() {
        Bundle args = getArguments();
        if (args != null) {
            long selectedGroupId = args.getLong("selected_group_id", -1);
            String selectedGroupName = args.getString("selected_group_name");
            this.selectedGroupId = selectedGroupId;
//            if (selectedGroupId != -1 && selectedGroupName != null) {
//                // 在Spinner中选择对应的分组
//                for (int i = 0; i < allGroups.size(); i++) {
//                    if (allGroups.get(i).getGroupId() == selectedGroupId) {
//                        spinnerGroups.setSelection(i); // 直接设置位置，因为allGroups已经包含了"全部分组"选项
//                        this.selectedGroupId = selectedGroupId;
//                        filterCategoriesByGroup();
//                        break;
//                    }
//                }
//            }
        }
    }

    private void initViews(View view) {

        btnAddTv = view.findViewById(R.id.add_group);
        back = view.findViewById(R.id.back);

        // 工具栏
        toolbarTitle = view.findViewById(R.id.toolbar_title);
        btnBack = view.findViewById(R.id.btn_back);
        btnAdd = view.findViewById(R.id.btn_add);
        
        // 分组选择器
        spinnerGroups = view.findViewById(R.id.spinner_groups);
        spinnerCategories = view.findViewById(R.id.spinner_categories);
        
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
        // 分组列表
        // 真实数据由数据库加载
        //allGroups = new ArrayList<>();
    }

    private void setupRecyclerView() {
        categoryAdapter = new CategoryAdapter();

        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategories.setAdapter(categoryAdapter);
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
        
        // 分组选择监听（真实数据）
        spinnerGroups.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object tag = parent.getTag();
                if (tag instanceof java.util.List) {
                    java.util.List<Group> groups = (java.util.List<Group>) tag;
                    if (position >= 0 && position < groups.size()) {
                        Group selectedGroup = groups.get(position);
                        selectedGroupId = selectedGroup.getGroupId();
                        // 加载分类列表和分类下拉
                        loadCategories();
                        loadCategoriesIntoSpinner();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedGroupId = -1;
                loadCategories();
            }
        });

        // 分类选择监听：按具体分类过滤展示
        spinnerCategories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object tag = parent.getTag();
                if (tag instanceof java.util.List) {
                    java.util.List<Category> categories = (java.util.List<Category>) tag;
                    if (position >= 0 && position < categories.size()) {
                        Category selected = categories.get(position);
                        java.util.List<Category> filtered = new java.util.ArrayList<>();
                        if (selected.getCategoryId() == -1) {
                            // 全部分类
                            filtered.addAll(allCategories);
                        } else {
                            for (Category c : allCategories) {
                                if (c.getCategoryId() == selected.getCategoryId()) {
                                    filtered.add(c);
                                }
                            }
                        }
                        categoryAdapter.submitList(filtered);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                categoryAdapter.submitList(allCategories);
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

        // 返回按钮
        back.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        // 新增按钮
        btnAddTv.setOnClickListener(v -> showAddCategoryDialog());

        categoryAdapter.addOnItemChildClickListener(R.id.tv_group_edit, new BaseQuickAdapter.OnItemChildClickListener<Category>() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<Category, ?> baseQuickAdapter, @NonNull View view, int i) {
                onEditClick(allCategories.get(i));
            }
        });
        categoryAdapter.addOnItemChildClickListener(R.id.tv_group_delete, new BaseQuickAdapter.OnItemChildClickListener<Category>() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<Category, ?> baseQuickAdapter, @NonNull View view, int i) {
                onDeleteClick(allCategories.get(i));
            }
        });
//        categoryAdapter.addOnItemChildClickListener(R.id.tv_group_edit, new BaseQuickAdapter.OnItemChildClickListener<Group>() {
//            @Override
//            public void onItemClick(@NonNull BaseQuickAdapter<Group, ?> baseQuickAdapter, @NonNull View view, int i) {
//                onEditClick(allCategories.get(i));
//            }
//        });
//        categoryAdapter.addOnItemChildClickListener(R.id.tv_group_delete, new BaseQuickAdapter.OnItemChildClickListener<Group>() {
//            @Override
//            public void onItemClick(@NonNull BaseQuickAdapter<Group, ?> baseQuickAdapter, @NonNull View view, int i) {
//                onDeleteClick(allCategories.get(i));
//            }
//        });
    }

    private void loadData() {
        // 加载分组列表到Spinner
        loadGroupsIntoSpinner();
        // 加载分类列表
        loadCategories();
    }


    private void loadCategories() {
        try {
            // 当选择“全部分组”时，加载全部分类；否则按分组ID加载
            if (selectedGroupId == -1) {
                allCategories = dbManager.getAllCategories();
            } else {
                allCategories = dbManager.getCategoriesByGroupId(selectedGroupId);
            }
            categoryAdapter.submitList(allCategories);
            categoryAdapter.notifyDataSetChanged();
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
        
        categoryAdapter.submitList(filteredCategories);
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
        categoryAdapter.submitList(filteredCategories);
    }

    private void loadGroupsIntoSpinner() {
        try {
            java.util.List<Group> groups = dbManager.getAllGroups();
            // 为Spinner构造显示名称列表
            java.util.List<Group> displayGroups = new java.util.ArrayList<>();
            // 添加“全部分组”占位项
            Group all = new Group();
            all.setGroupId(-1);
            all.setGroupName("全部分组");
            displayGroups.add(all);
            if (groups != null) {
                displayGroups.addAll(groups);
            }
            java.util.List<String> names = new java.util.ArrayList<>();
            for (Group g : displayGroups) {
                names.add(g.getGroupName());
            }
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(), R.layout.spinner_item_16dp, names);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_16dp);
            spinnerGroups.setAdapter(adapter);
            spinnerGroups.setTag(displayGroups);
            spinnerGroups.setVisibility(View.VISIBLE);

            // 如果从分组页面带入了selectedGroupId，设置选中项
            if (selectedGroupId != -1) {
                for (int i = 0; i < displayGroups.size(); i++) {
                    if (displayGroups.get(i).getGroupId() == selectedGroupId) {
                        spinnerGroups.setSelection(i);
                        break;
                    }
                }
            }
            // 初始加载分类下拉
            loadCategoriesIntoSpinner();
        } catch (Exception e) {
            spinnerGroups.setVisibility(View.GONE);
        }
    }

    private void loadCategoriesIntoSpinner() {
        try {
            java.util.List<Category> categories = selectedGroupId == -1 ? dbManager.getAllCategories() : dbManager.getCategoriesByGroupId(selectedGroupId);
            java.util.List<Category> displayCategories = new java.util.ArrayList<>();
            Category all = new Category();
            all.setCategoryId(-1);
            all.setCategoryName("全部分类");
            displayCategories.add(all);
            if (categories != null) displayCategories.addAll(categories);

            java.util.List<String> names = new java.util.ArrayList<>();
            for (Category c : displayCategories) names.add(c.getCategoryName());
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(), R.layout.spinner_item_16dp, names);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_16dp);
            spinnerCategories.setAdapter(adapter);
            spinnerCategories.setTag(displayCategories);
            spinnerCategories.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            spinnerCategories.setVisibility(View.GONE);
        }
    }

//    private void showAddCategoryDialog() {
//        if (allGroups.size() <= 1) { // 只有"所有分组"选项
//            Toast.makeText(requireContext(), "请先创建分组", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        showCategoryDialog(null, "新增分类");
//    }

    private void showEditCategoryDialog(Category category) {
        //showCategoryDialog(category, "编辑分类");
        DialogUtils.showTextEditDialog(
                getContext(),
                "编辑分组",
                "分组名称",
                category.getCategoryName(),
                "",
                newValue -> {
                    updateCategory(category.getCategoryId(), newValue, "", selectedGroupId);
                }
        );
    }

//    private void showCategoryDialog(Category category, String title) {
//        View dialogView = LayoutInflater.from(requireContext())
//                .inflate(R.layout.dialog_add_category, null);
//
//        EditText etName = dialogView.findViewById(R.id.et_category_name);
//        EditText etDescription = dialogView.findViewById(R.id.et_category_description);
//        Spinner spinnerGroup = dialogView.findViewById(R.id.spinner_group);
//
//        // 设置分组选择器
//        List<Group> availableGroups = new ArrayList<>();
//        for (Group group : allGroups) {
//            if (group.getGroupId() != -1) { // 排除"所有分组"选项
//                availableGroups.add(group);
//            }
//        }
//
//        ArrayAdapter<Group> dialogGroupAdapter = new ArrayAdapter<Group>(requireContext(),
//                android.R.layout.simple_spinner_item, availableGroups) {
//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//                View view = super.getView(position, convertView, parent);
//                TextView textView = (TextView) view;
//                Group group = getItem(position);
//                if (group != null) {
//                    textView.setText(group.getGroupName());
//                }
//                return view;
//            }
//
//            @Override
//            public View getDropDownView(int position, View convertView, ViewGroup parent) {
//                View view = super.getDropDownView(position, convertView, parent);
//                TextView textView = (TextView) view;
//                Group group = getItem(position);
//                if (group != null) {
//                    textView.setText(group.getGroupName());
//                }
//                return view;
//            }
//        };
//        dialogGroupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinnerGroup.setAdapter(dialogGroupAdapter);
//
//        if (category != null) {
//            etName.setText(category.getCategoryName());
//            etDescription.setText(category.getCategoryDescription());
//
//            // 设置分组选择
//            for (int i = 0; i < availableGroups.size(); i++) {
//                if (availableGroups.get(i).getGroupId() == category.getGroupId()) {
//                    spinnerGroup.setSelection(i);
//                    break;
//                }
//            }
//        }
//
//        new AlertDialog.Builder(requireContext())
//                .setTitle(title)
//                .setView(dialogView)
//                .setPositiveButton("确定", (dialog, which) -> {
//                    String name = etName.getText().toString().trim();
//                    String description = etDescription.getText().toString().trim();
//
//                    if (TextUtils.isEmpty(name)) {
//                        Toast.makeText(requireContext(), "请输入分类名称", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    if (spinnerGroup.getSelectedItem() == null) {
//                        Toast.makeText(requireContext(), "请选择分组", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    Group selectedGroup = (Group) spinnerGroup.getSelectedItem();
//                    long groupId = selectedGroup.getGroupId();
//
//                    if (category == null) {
//                        addCategory(name, description, groupId);
//                    } else {
//                        updateCategory(category.getCategoryId(), name, description, groupId);
//                    }
//                })
//                .setNegativeButton("取消", null)
//                .show();
//    }

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

    private void showAddCategoryDialog() {
        //showGroupDialog(null, "新增分组");
        DialogUtils.showTextEditDialog(
                getContext(),
                "新增分组",
                "分组名称",
                "",
                "",
                newValue -> {
                    addCategory(newValue, "", selectedGroupId);
                }
        );

    }


    public void onEditClick(Category category) {
        showEditCategoryDialog(category);
    }


    public void onDeleteClick(Category category) {
        deleteCategory(category);
    }


    public void onItemClick(Category category) {
        // 可以在这里实现点击分类查看详情的功能
        Toast.makeText(requireContext(), "点击了分类: " + category.getCategoryName(), Toast.LENGTH_SHORT).show();
    }

    public static CategoryManagementFragment newInstance() {
        return new CategoryManagementFragment();
    }
}
