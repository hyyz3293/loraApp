package com.lora.cn.ui.fragment.setting;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.Group;
import com.lora.cn.database.entity.User;
import com.lora.cn.ui.adapter.GroupAdapter;
import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.ui.fragment.setting.group.CategoryManagementFragment;
import com.lora.cn.utils.DialogUtils;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class GroupManagementFragment extends Fragment  {


    private TextView back;
    private TextView btnAdd;
    private EditText etSearch;
    private Button btnSearch;
    private RecyclerView rvGroups;
    
    private GroupAdapter groupAdapter;
    private DatabaseManager dbManager;
    private List<Group> allGroups;
    private long currentUserRoleId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_management, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadGroups();
        
        return view;
    }

    private void initViews(View view) {
        // 工具栏
        btnAdd = view.findViewById(R.id.add_group);
        back = view.findViewById(R.id.back);
        
        // 搜索
        etSearch = view.findViewById(R.id.et_search);
        btnSearch = view.findViewById(R.id.btn_search);
        
        // 列表
        rvGroups = view.findViewById(R.id.rv_groups);
        

        // 初始化数据库管理器
        dbManager = DatabaseManager.getInstance(requireContext());
        allGroups = new ArrayList<>();
        
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
        groupAdapter = new GroupAdapter();
        rvGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvGroups.setAdapter(groupAdapter);
    }

    private void setupListeners() {
        // 返回按钮
        back.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
        
        // 新增按钮
        btnAdd.setOnClickListener(v -> {
            if (hasPermission("group_add")) {
                showAddGroupDialog();
            } else {
                Toast.makeText(requireContext(), "您没有新增分组的权限", Toast.LENGTH_SHORT).show();
            }
        });

        // 搜索按钮
        btnSearch.setOnClickListener(v -> performSearch());
        
        // 搜索框文本变化监听
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(s)) {
                    groupAdapter.submitList(allGroups);
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        groupAdapter.addOnItemChildClickListener(R.id.tv_group_fz, new BaseQuickAdapter.OnItemChildClickListener<Group>() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<Group, ?> baseQuickAdapter, @NonNull View view, int i) {
                onItemClickItem(allGroups.get(i));
            }
        });
        groupAdapter.addOnItemChildClickListener(R.id.tv_group_edit, new BaseQuickAdapter.OnItemChildClickListener<Group>() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<Group, ?> baseQuickAdapter, @NonNull View view, int i) {
                onEditClick(allGroups.get(i));
            }
        });
        groupAdapter.addOnItemChildClickListener(R.id.tv_group_delete, new BaseQuickAdapter.OnItemChildClickListener<Group>() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<Group, ?> baseQuickAdapter, @NonNull View view, int i) {
                onDeleteClick(allGroups.get(i));
            }
        });
    }

    private void loadGroups() {
        try {
            allGroups = dbManager.getAllGroups();
            groupAdapter.submitList(allGroups);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "加载分组失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void performSearch() {
        String keyword = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            groupAdapter.submitList(allGroups);
            return;
        }
        
        List<Group> filteredGroups = new ArrayList<>();
        for (Group group : allGroups) {
            if (group.getGroupName().toLowerCase().contains(keyword.toLowerCase()) ||
                group.getGroupDescription().toLowerCase().contains(keyword.toLowerCase())) {
                filteredGroups.add(group);
            }
        }
        groupAdapter.submitList(filteredGroups);
    }

    private void showAddGroupDialog() {
        //showGroupDialog(null, "新增分组");
        DialogUtils.showTextEditDialog(
                getContext(),
                "新增分类",
                "分类名称",
                "",
                "",
                newValue -> {
                    addGroup(newValue, "");
                }
        );

    }

    private void showEditGroupDialog(Group group) {
        //showGroupDialog(group, "编辑分组");
        DialogUtils.showTextEditDialog(
                getContext(),
                "编辑分类",
                "分类名称",
                group.getGroupName(),
                "",
                newValue -> {
                    updateGroup(group.getGroupId(), newValue, "");
                }
        );

    }



    private void showGroupDialog(Group group, String title) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_group, null);
        
        EditText etName = dialogView.findViewById(R.id.et_group_name);
        EditText etDescription = dialogView.findViewById(R.id.et_group_description);
        
        if (group != null) {
            etName.setText(group.getGroupName());
            etDescription.setText(group.getGroupDescription());
        }
        
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("确定", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();
                    
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(requireContext(), "请输入分组名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (group == null) {
                        addGroup(name, description);
                    } else {
                        updateGroup(group.getGroupId(), name, description);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void addGroup(String name, String description) {
        try {
            long groupId = dbManager.addGroup(name, description);
            if (groupId > 0) {
                Toast.makeText(requireContext(), "添加成功", Toast.LENGTH_SHORT).show();
                loadGroups();
            } else {
                Toast.makeText(requireContext(), "添加失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "添加失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateGroup(long groupId, String name, String description) {
        try {
            boolean success = dbManager.updateGroup(groupId, name, description);
            if (success) {
                Toast.makeText(requireContext(), "更新成功", Toast.LENGTH_SHORT).show();
                loadGroups();
            } else {
                Toast.makeText(requireContext(), "更新失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "更新失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteGroup(Group group) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除确认")
                .setMessage("确定要删除分组 \"" + group.getGroupName() + "\" 吗？\n删除分组将同时删除其下所有分类。")
                .setPositiveButton("删除", (dialog, which) -> {
                    try {
                        boolean success = dbManager.deleteGroup(group.getGroupId());
                        if (success) {
                            Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show();
                            loadGroups();
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


    public void onEditClick(Group group) {
        if (hasPermission("group_edit")) {
            showEditGroupDialog(group);
        } else {
            Toast.makeText(requireContext(), "您没有编辑分组的权限", Toast.LENGTH_SHORT).show();
        }
    }

    public void onDeleteClick(Group group) {
        if (hasPermission("group_delete")) {
            deleteGroup(group);
        } else {
            Toast.makeText(requireContext(), "您没有删除分组的权限", Toast.LENGTH_SHORT).show();
        }
    }

    public void onItemClickItem(Group group) {
        // 跳转到分类管理页面，并传递分组ID进行过滤
        CategoryManagementFragment categoryFragment = CategoryManagementFragment.newInstance();
        Bundle args = new Bundle();
        args.putLong("selected_group_id", group.getGroupId());
        args.putString("selected_group_name", group.getGroupName());
        categoryFragment.setArguments(args);
        
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.settings_fragment_container, categoryFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * 检查当前用户是否有指定权限
     */
    private boolean hasPermission(String permissionCode) {
        if (currentUserRoleId == -1) {
            return false;
        }
        return dbManager.hasPermission((int)currentUserRoleId, permissionCode);
    }

    public static GroupManagementFragment newInstance() {
        return new GroupManagementFragment();
    }
}