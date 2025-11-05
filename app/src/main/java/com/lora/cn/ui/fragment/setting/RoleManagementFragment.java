package com.lora.cn.ui.fragment.setting;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.LogUtils;
import com.google.gson.Gson;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.Role;
import com.lora.cn.database.entity.Permission;
import com.lora.cn.ui.adapter.RoleAdapter;
import com.lora.cn.ui.adapter.TreePermissionCheckboxAdapter;
import com.lora.cn.utils.DialogUtils;
import com.lora.cn.utils.PermissionTreeConverter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 角色管理Fragment
 */
public class RoleManagementFragment extends Fragment {

    private RecyclerView rvRoles;
    private RoleAdapter roleAdapter;
    private TextView btnAddRole;
    private TextView btnBack;

    private DatabaseManager databaseManager;
    private List<Role> allRoles;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_role_management, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadRoles();
        
        return view;
    }

    private void initViews(View view) {
        rvRoles = view.findViewById(R.id.rv_roles);
        btnAddRole = view.findViewById(R.id.btn_add_role);
        btnBack = view.findViewById(R.id.back);
        databaseManager = DatabaseManager.getInstance(requireContext());
        allRoles = new ArrayList<>();
    }

    private void setupRecyclerView() {
        roleAdapter = new RoleAdapter();
        rvRoles.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRoles.setAdapter(roleAdapter);
    }
    
    private void setupListeners() {
        // 返回按钮
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
        
        // 新增按钮
        btnAddRole.setOnClickListener(v -> showAddRoleDialogNew());

        // 设置适配器点击监听器
        roleAdapter.addOnItemChildClickListener(R.id.tv_role_edit, (baseQuickAdapter, view, i) -> {
            Role role = baseQuickAdapter.getItem(i);
            if (role != null) {
                showEditRoleDialogNew(role);
            }
        });
        
        roleAdapter.addOnItemChildClickListener(R.id.tv_role_delete, (baseQuickAdapter, view, i) -> {
            Role role = baseQuickAdapter.getItem(i);
            if (role != null) {
                showDeleteConfirmDialog(role);
            }
        });
        
        roleAdapter.addOnItemChildClickListener(R.id.tv_role_permissions, (baseQuickAdapter, view, i) -> {
            Role role = baseQuickAdapter.getItem(i);
            if (role != null) {
                List<Permission> allPermissions = databaseManager.getPermissionTree();

                // 获取角色当前权限
                List<Integer> currentPermissionIds = databaseManager.getPermissionIdsByRoleId((int)role.getRoleId());
                
                DialogUtils.showRoleDialog(requireContext(), "", allPermissions, role, currentPermissionIds, new DialogUtils.OnConfirmListener() {
                    @Override
                    public void onConfirm(String selectedPermissionIds) {
                        // 解析选中的权限ID字符串
                        List<Integer> permissionIds = new ArrayList<>();
                        if (!selectedPermissionIds.isEmpty()) {
                            String[] idArray = selectedPermissionIds.split(",");
                            for (String idStr : idArray) {
                                try {
                                    permissionIds.add(Integer.parseInt(idStr.trim()));
                                } catch (NumberFormatException e) {
                                    // 忽略无效的ID
                                }
                            }
                        }
                        // 保存角色权限到数据库
                        boolean success = databaseManager.setRolePermissions((int)role.getRoleId(), permissionIds);
                        if (success) {
                            Toast.makeText(requireContext(), "权限设置保存成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "权限设置保存失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        
        roleAdapter.addOnItemChildClickListener(R.id.switch_role_status, (baseQuickAdapter, view, i) -> {
            Role role = baseQuickAdapter.getItem(i);
            if (role != null) {
                SwitchCompat switchStatus = (SwitchCompat) view;
                toggleRoleStatus(role, switchStatus.isChecked());
            }
        });
    }

    private void loadRoles() {
        try {
            allRoles = databaseManager.getAllRoles();
            roleAdapter.submitList(new ArrayList<>(allRoles));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "加载角色列表失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

//    private void showAddRoleDialog() {
//        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_role, null);
//        EditText etRoleName = dialogView.findViewById(R.id.et_role_name);
//        EditText etRoleDescription = dialogView.findViewById(R.id.et_role_description);
//        EditText etSortOrder = dialogView.findViewById(R.id.et_sort_order);
//        SwitchCompat switchStatus = dialogView.findViewById(R.id.switch_status);
//        RecyclerView rvPermissions = dialogView.findViewById(R.id.rv_permissions);
//
//        // 设置默认排序号
//        etSortOrder.setText(String.valueOf(allRoles.size() + 1));
//        switchStatus.setChecked(true);
//
//        // 设置权限选择列表
//        TreePermissionCheckboxAdapter permissionAdapter = new TreePermissionCheckboxAdapter();
//        rvPermissions.setLayoutManager(new LinearLayoutManager(requireContext()));
//        rvPermissions.setAdapter(permissionAdapter);
//
//        // 加载所有权限（使用树形权限获取方法）
//        List<Permission> flatPermissions = databaseManager.getPermissionTree();
//        List<Permission> treePermissions = PermissionTreeConverter.convertToTree(flatPermissions);
//        permissionAdapter.setPermissions(treePermissions);
//
//        new AlertDialog.Builder(requireContext())
//                .setTitle("新增角色")
//                .setView(dialogView)
//                .setPositiveButton("确定", (dialog, which) -> {
//                    String roleName = etRoleName.getText().toString().trim();
//                    String description = etRoleDescription.getText().toString().trim();
//                    String sortOrderStr = etSortOrder.getText().toString().trim();
//
//                    if (TextUtils.isEmpty(roleName)) {
//                        Toast.makeText(requireContext(), "请输入角色名称", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    // 检查角色名称是否已存在
//                    if (databaseManager.isRoleNameExists(roleName)) {
//                        Toast.makeText(requireContext(), "角色名称已存在", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    int sortOrder = 1;
//                    try {
//                        if (!TextUtils.isEmpty(sortOrderStr)) {
//                            sortOrder = Integer.parseInt(sortOrderStr);
//                        }
//                    } catch (NumberFormatException e) {
//                        sortOrder = allRoles.size() + 1;
//                    }
//
//                    Role newRole = new Role();
//                    newRole.setRoleName(roleName);
//                    newRole.setDescription(description.isEmpty() ? null : description);
//                    newRole.setSortOrder(sortOrder);
//                    newRole.setStatus(switchStatus.isChecked() ? 1 : 0);
//                    newRole.setCreateTime(new Date());
//                    newRole.setUpdateTime(new Date());
//
//                    long roleId = databaseManager.insertRole(newRole);
//                    if (roleId > 0) {
//                        newRole.setRoleId(roleId);
//
//                        // 设置角色权限
//                        List<Long> selectedPermissionIds = permissionAdapter.getSelectedPermissionIds();
//                        if (!selectedPermissionIds.isEmpty()) {
//                            List<Integer> permissionIds = new ArrayList<>();
//                            for (Long id : selectedPermissionIds) {
//                                permissionIds.add(id.intValue());
//                            }
//                            databaseManager.setRolePermissions((int)roleId, permissionIds);
//                        }
//
//                        allRoles.add(newRole);
//                        roleAdapter.addRole(newRole);
//                        Toast.makeText(requireContext(), "角色添加成功", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(requireContext(), "角色添加失败", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .setNegativeButton("取消", null)
//                .show();
//    }
//
//    private void showEditRoleDialog(Role role) {
//        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_role, null);
//        EditText etRoleName = dialogView.findViewById(R.id.et_role_name);
//        EditText etRoleDescription = dialogView.findViewById(R.id.et_role_description);
//        EditText etSortOrder = dialogView.findViewById(R.id.et_sort_order);
//        SwitchCompat switchStatus = dialogView.findViewById(R.id.switch_status);
//        RecyclerView rvPermissions = dialogView.findViewById(R.id.rv_permissions);
//
//        // 填充现有数据
//        etRoleName.setText(role.getRoleName());
//        etRoleDescription.setText(role.getDescription() != null ? role.getDescription() : "");
//        etSortOrder.setText(String.valueOf(role.getSortOrder()));
//        switchStatus.setChecked(role.getStatus() == 1);
//
//        // 设置权限选择列表
//        TreePermissionCheckboxAdapter permissionAdapter = new TreePermissionCheckboxAdapter();
//        rvPermissions.setLayoutManager(new LinearLayoutManager(requireContext()));
//        rvPermissions.setAdapter(permissionAdapter);
//
//        // 加载所有权限（使用树形权限获取方法）
//        List<Permission> flatPermissions = databaseManager.getPermissionTree();
//        List<Permission> treePermissions = PermissionTreeConverter.convertToTree(flatPermissions);
//        permissionAdapter.setPermissions(treePermissions);
//
//        // 加载角色当前权限
//        List<Integer> currentPermissionIds = databaseManager.getPermissionIdsByRoleId((int)role.getRoleId());
//        List<Long> longPermissionIds = new ArrayList<>();
//        for (Integer id : currentPermissionIds) {
//            longPermissionIds.add(id.longValue());
//        }
//        permissionAdapter.setSelectedPermissions(longPermissionIds);
//
//        new AlertDialog.Builder(requireContext())
//                .setTitle("编辑角色")
//                .setView(dialogView)
//                .setPositiveButton("确定", (dialog, which) -> {
//                    String roleName = etRoleName.getText().toString().trim();
//                    String description = etRoleDescription.getText().toString().trim();
//                    String sortOrderStr = etSortOrder.getText().toString().trim();
//
//                    if (TextUtils.isEmpty(roleName)) {
//                        Toast.makeText(requireContext(), "请输入角色名称", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    // 检查角色名称是否已存在（排除当前角色）
//                    if (!roleName.equals(role.getRoleName()) && databaseManager.isRoleNameExists(roleName)) {
//                        Toast.makeText(requireContext(), "角色名称已存在", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    int sortOrder = role.getSortOrder();
//                    try {
//                        if (!TextUtils.isEmpty(sortOrderStr)) {
//                            sortOrder = Integer.parseInt(sortOrderStr);
//                        }
//                    } catch (NumberFormatException e) {
//                        // 保持原有排序
//                    }
//
//                    role.setRoleName(roleName);
//                    role.setDescription(description.isEmpty() ? null : description);
//                    role.setSortOrder(sortOrder);
//                    role.setStatus(switchStatus.isChecked() ? 1 : 0);
//                    role.setUpdateTime(new Date());
//
//                    boolean success = databaseManager.updateRole(role);
//                    if (success) {
//                        // 更新角色权限
//                        List<Long> selectedPermissionIds = permissionAdapter.getSelectedPermissionIds();
//                        List<Integer> permissionIds = new ArrayList<>();
//                        for (Long id : selectedPermissionIds) {
//                            permissionIds.add(id.intValue());
//                        }
//                        databaseManager.setRolePermissions((int)role.getRoleId(), permissionIds);
//
//                        roleAdapter.updateRole(role);
//                        Toast.makeText(requireContext(), "角色更新成功", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(requireContext(), "角色更新失败", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .setNegativeButton("取消", null)
//                .show();
//    }

    // 使用新的DialogUtils方法新增角色
    private void showAddRoleDialogNew() {
        // 获取所有权限
        List<Permission> allPermissions = databaseManager.getPermissionTree();
        
        DialogUtils.showRoleDialog(requireContext(), "新增角色", allPermissions, null, new ArrayList<>(), new DialogUtils.OnConfirmListener() {
            @Override
            public void onConfirm(String result) {
                // 解析返回数据：角色名称|权限ID列表
                String[] parts = result.split("\\|", 2);
                if (parts.length < 1) {
                    Toast.makeText(requireContext(), "数据格式错误", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String roleName = parts[0].trim();
                String permissionIdsStr = parts.length > 1 ? parts[1] : "";
                
                // 检查角色名称是否已存在
                if (databaseManager.isRoleNameExists(roleName)) {
                    Toast.makeText(requireContext(), "角色名称已存在", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 创建新角色
                Role newRole = new Role();
                newRole.setRoleName(roleName);
                newRole.setDescription(null);
                newRole.setSortOrder(allRoles.size() + 1);
                newRole.setStatus(1); // 默认启用
                newRole.setCreateTime(new Date());
                newRole.setUpdateTime(new Date());
                
                long roleId = databaseManager.insertRole(newRole);
                if (roleId > 0) {
                    newRole.setRoleId(roleId);
                    
                    // 设置角色权限
                    if (!permissionIdsStr.isEmpty()) {
                        List<Integer> permissionIds = new ArrayList<>();
                        String[] idArray = permissionIdsStr.split(",");
                        for (String idStr : idArray) {
                            try {
                                permissionIds.add(Integer.parseInt(idStr.trim()));
                            } catch (NumberFormatException e) {
                                // 忽略无效的ID
                            }
                        }
                        databaseManager.setRolePermissions((int)roleId, permissionIds);
                    }
                    
                    allRoles.add(newRole);
                    roleAdapter.addRole(newRole);
                    Toast.makeText(requireContext(), "角色添加成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "角色添加失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    // 使用新的DialogUtils方法编辑角色
    private void showEditRoleDialogNew(Role role) {
        // 获取所有权限
        List<Permission> allPermissions = databaseManager.getPermissionTree();
        
        // 获取角色当前权限
        List<Integer> currentPermissionIds = databaseManager.getPermissionIdsByRoleId((int)role.getRoleId());
        
        DialogUtils.showRoleDialog(requireContext(), "编辑角色", allPermissions, role, currentPermissionIds, new DialogUtils.OnConfirmListener() {
            @Override
            public void onConfirm(String result) {
                // 解析返回数据：角色名称|权限ID列表
                String[] parts = result.split("\\|", 2);
                if (parts.length < 1) {
                    Toast.makeText(requireContext(), "数据格式错误", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String roleName = parts[0].trim();
                String permissionIdsStr = parts.length > 1 ? parts[1] : "";
                
                // 检查角色名称是否已存在（排除当前角色）
                if (!roleName.equals(role.getRoleName()) && databaseManager.isRoleNameExists(roleName)) {
                    Toast.makeText(requireContext(), "角色名称已存在", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 更新角色信息
                role.setRoleName(roleName);
                role.setUpdateTime(new Date());
                
                boolean success = databaseManager.updateRole(role);
                if (success) {
                    // 更新角色权限
                    List<Integer> permissionIds = new ArrayList<>();
                    if (!permissionIdsStr.isEmpty()) {
                        String[] idArray = permissionIdsStr.split(",");
                        for (String idStr : idArray) {
                            try {
                                permissionIds.add(Integer.parseInt(idStr.trim()));
                            } catch (NumberFormatException e) {
                                // 忽略无效的ID
                            }
                        }
                    }
                    databaseManager.setRolePermissions((int)role.getRoleId(), permissionIds);
                    
                    roleAdapter.updateRole(role);
                    Toast.makeText(requireContext(), "角色更新成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "角色更新失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showDeleteConfirmDialog(Role role) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除确认")
                .setMessage("确定要删除角色 \"" + role.getRoleName() + "\" 吗？\n\n注意：删除角色将同时删除该角色的所有权限关联。")
                .setPositiveButton("确定", (dialog, which) -> {
                    boolean success = databaseManager.deleteRole((int)role.getRoleId());
                    if (success) {
                        allRoles.remove(role);
                        roleAdapter.removeRole((int)role.getRoleId());
                        Toast.makeText(requireContext(), "角色删除成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "角色删除失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showRolePermissionsDialog(Role role) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_role_permissions, null);
        RecyclerView rvPermissions = dialogView.findViewById(R.id.rv_permissions);
        TextView tvRoleName = dialogView.findViewById(R.id.tv_role_name);
        
        tvRoleName.setText("角色：" + role.getRoleName());

        // 设置权限选择列表
        TreePermissionCheckboxAdapter permissionAdapter = new TreePermissionCheckboxAdapter();
        rvPermissions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPermissions.setAdapter(permissionAdapter);

        LogUtils.e("---showRolePermissionsDialog" );

        // 加载所有权限（使用树形权限获取方法）
        List<Permission> flatPermissions = databaseManager.getPermissionTree();
        List<Permission> treePermissions = PermissionTreeConverter.convertToTree(flatPermissions);
        
        if (treePermissions.size() > 0) {
            for (Permission permission : treePermissions) {
                LogUtils.e("---Tree Permission: " + new Gson().toJson(permission));
            }
        }

        LogUtils.e("---Tree Permissions: " + new Gson().toJson(treePermissions));

        permissionAdapter.setPermissions(treePermissions);

        // 加载角色当前权限
        List<Integer> currentPermissionIds = databaseManager.getPermissionIdsByRoleId((int)role.getRoleId());
        List<Long> longPermissionIds = new ArrayList<>();
        for (Integer id : currentPermissionIds) {
            longPermissionIds.add(id.longValue());
        }
        permissionAdapter.setSelectedPermissions(longPermissionIds);

        new AlertDialog.Builder(requireContext())
                .setTitle("权限设置")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    // 更新角色权限
                    List<Long> selectedPermissionIds = permissionAdapter.getSelectedPermissionIds();
                    List<Integer> permissionIds = new ArrayList<>();
                    for (Long id : selectedPermissionIds) {
                        permissionIds.add(id.intValue());
                    }
                    boolean success = databaseManager.setRolePermissions((int)role.getRoleId(), permissionIds);
                    
                    if (success) {
                        Toast.makeText(requireContext(), "权限设置保存成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "权限设置保存失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void toggleRoleStatus(Role role, boolean isEnabled) {
        role.setStatus(isEnabled ? 1 : 0);
        role.setUpdateTime(new Date());
        
        boolean success = databaseManager.updateRole(role);
        if (success) {
            String statusText = isEnabled ? "启用" : "禁用";
            Toast.makeText(requireContext(), "角色已" + statusText, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "状态更新失败", Toast.LENGTH_SHORT).show();
            // 恢复开关状态
            roleAdapter.notifyDataSetChanged();
        }
    }

    public static RoleManagementFragment newInstance() {
        return new RoleManagementFragment();
    }
}