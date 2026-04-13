package com.lora.cn.ui.fragment.setting;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
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

import com.blankj.utilcode.util.LogUtils;
import com.google.gson.Gson;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.Role;
import com.lora.cn.database.entity.Permission;
import com.lora.cn.ui.adapter.RoleAdapter;
import com.lora.cn.ui.adapter.TreePermissionCheckboxAdapter;
import com.lora.cn.utils.DialogUtils;
import com.lora.cn.utils.PermissionTreeBuilder;
import com.lora.cn.utils.PermissionTreeConverter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private long currentUserRoleId = -1;
    private final Set<String> grantedPermissions = new HashSet<>();
    private ExecutorService ioExecutor;
    private Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_role_management, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupListeners();
        initAsync();
        
        return view;
    }

    @Override
    public void onDestroyView() {
        try {
            if (ioExecutor != null) ioExecutor.shutdownNow();
        } catch (Exception ignored) {}
        ioExecutor = null;
        mainHandler = null;
        grantedPermissions.clear();
        super.onDestroyView();
    }

    private void initViews(View view) {
        rvRoles = view.findViewById(R.id.rv_roles);
        btnAddRole = view.findViewById(R.id.btn_add_role);
        btnBack = view.findViewById(R.id.back);
        databaseManager = DatabaseManager.getInstance(requireContext());
        allRoles = new ArrayList<>();
    }

    private void initAsync() {
        if (ioExecutor == null) ioExecutor = Executors.newSingleThreadExecutor();
        if (mainHandler == null) mainHandler = new Handler(Looper.getMainLooper());
        loadCurrentUserPermissionsAsync(this::loadRoles);
    }

    private void loadCurrentUserPermissionsAsync(@Nullable Runnable onComplete) {
        if (ioExecutor == null || mainHandler == null) return;
        final android.content.Context appContext = requireContext().getApplicationContext();
        ioExecutor.execute(() -> {
            long roleId = -1;
            Set<String> permissions = new HashSet<>();
            try {
                long userId = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
                if (userId != -1) {
                    com.lora.cn.database.entity.User user = databaseManager.getUserById(userId);
                    if (user != null) roleId = user.getRoleId();
                }
                if (roleId > 0) {
                    String[] permissionCodes = new String[] {"role_add", "role_edit", "role_delete"};
                    DatabaseManager dm = DatabaseManager.getInstance(appContext);
                    for (String code : permissionCodes) {
                        try {
                            if (dm.hasPermission((int) roleId, code)) {
                                permissions.add(code);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}

            final long finalRoleId = roleId;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                currentUserRoleId = finalRoleId;
                grantedPermissions.clear();
                grantedPermissions.addAll(permissions);
                if (onComplete != null) onComplete.run();
            });
        });
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
        btnAddRole.setOnClickListener(v -> {
            if (hasPermission("role_add")) showAddRoleDialogNew();
            else android.widget.Toast.makeText(requireContext(), "您没有新增角色的权限", android.widget.Toast.LENGTH_SHORT).show();
        });

        // 设置适配器点击监听器
        roleAdapter.addOnItemChildClickListener(R.id.tv_role_edit, (baseQuickAdapter, view, i) -> {
            Role role = baseQuickAdapter.getItem(i);
            if (role != null) {
                String rn = role.getRoleName() == null ? "" : role.getRoleName().trim();
                if ("管理员".equals(rn)) {
                    android.widget.Toast.makeText(requireContext(), "基础角色不可编辑", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                if (hasPermission("role_edit")) showEditRoleDialogNew(role);
                else android.widget.Toast.makeText(requireContext(), "您没有编辑角色的权限", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        
        roleAdapter.addOnItemChildClickListener(R.id.tv_role_delete, (baseQuickAdapter, view, i) -> {
            Role role = baseQuickAdapter.getItem(i);
            if (role != null) {
                String rn = role.getRoleName() == null ? "" : role.getRoleName().trim();
                if ("管理员".equals(rn)) {
                    android.widget.Toast.makeText(requireContext(), "基础角色不可删除", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                if (hasPermission("role_delete")) showDeleteConfirmDialog(role);
                else android.widget.Toast.makeText(requireContext(), "您没有删除角色的权限", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        
        roleAdapter.addOnItemChildClickListener(R.id.tv_role_permissions, (baseQuickAdapter, view, i) -> {
            Role role = baseQuickAdapter.getItem(i);
            if (role != null) {
                if (!hasPermission("role_edit")) { android.widget.Toast.makeText(requireContext(), "您没有编辑角色权限的权限", android.widget.Toast.LENGTH_SHORT).show(); return; }
                showRolePermissionsDialogAsync(role, "权限设置");
            }
        });
        
        roleAdapter.addOnItemChildClickListener(R.id.switch_role_status, (baseQuickAdapter, view, i) -> {
            Role role = baseQuickAdapter.getItem(i);
            if (role != null) {
                SwitchCompat switchStatus = (SwitchCompat) view;
                String rn = role.getRoleName() == null ? "" : role.getRoleName().trim();
                if ("管理员".equals(rn)) {
                    switchStatus.setChecked(role.getStatus() == 1);
                    android.widget.Toast.makeText(requireContext(), "基础角色状态不可更改", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                toggleRoleStatus(role, switchStatus.isChecked(), switchStatus);
            }
        });
    }

    private void loadRoles() {
        if (ioExecutor == null || mainHandler == null) return;
        final android.content.Context appContext = requireContext().getApplicationContext();
        ioExecutor.execute(() -> {
            List<Role> roles = new ArrayList<>();
            Exception error = null;
            try {
                roles = DatabaseManager.getInstance(appContext).getAllRoles();
                if (roles == null) roles = new ArrayList<>();
            } catch (Exception e) {
                error = e;
            }

            final Exception finalError = error;
            final List<Role> finalRoles = roles;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (finalError != null) {
                    Toast.makeText(requireContext(), "加载角色列表失败: " + finalError.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                allRoles = new ArrayList<>(finalRoles);
                roleAdapter.submitList(new ArrayList<>(finalRoles));
            });
        });
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
        loadRoleDialogDataAsync(null, "新增角色", data -> showRoleEditorDialog("新增角色", null, data, false));
    }
    
    // 使用新的DialogUtils方法编辑角色
    private void showEditRoleDialogNew(Role role) {
        loadRoleDialogDataAsync(role, "编辑角色", data -> showRoleEditorDialog("编辑角色", role, data, false));
    }

    private void loadRoleDialogDataAsync(@Nullable Role role, String title, RoleDialogCallback callback) {
        if (ioExecutor == null || mainHandler == null) return;
        final android.content.Context appContext = requireContext().getApplicationContext();
        ioExecutor.execute(() -> {
            RoleDialogData data = new RoleDialogData();
            Exception error = null;
            try {
                DatabaseManager dm = DatabaseManager.getInstance(appContext);
                data.allPermissions = dm.getPermissionTree();
                if (data.allPermissions == null) data.allPermissions = new ArrayList<>();
                if (role != null) {
                    data.currentPermissionIds = dm.getPermissionIdsByRoleId((int) role.getRoleId());
                    if (data.currentPermissionIds == null) data.currentPermissionIds = new ArrayList<>();
                }
            } catch (Exception e) {
                error = e;
            }

            final Exception finalError = error;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (finalError != null) {
                    Toast.makeText(requireContext(), title + "数据加载失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                callback.onLoaded(data);
            });
        });
    }

    private void showRolePermissionsDialogAsync(Role role, String title) {
        loadRoleDialogDataAsync(role, "权限设置", data -> showRoleEditorDialog(title, role, data, true));
    }

    private void showDeleteConfirmDialog(Role role) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("删除确认")
                .setMessage("确定要删除角色 \"" + role.getRoleName() + "\" 吗？\n\n注意：删除角色将同时删除该角色的所有权限关联。")
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            positiveButton.setOnClickListener(v -> deleteRoleAsync(role, positiveButton, negativeButton, dialog));
        });
        dialog.show();
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

    private void showRoleEditorDialog(String title, @Nullable Role role, RoleDialogData data, boolean permissionOnly) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_role, null);
        dialog.setContentView(dialogView);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(
                    (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.6),
                    (int) (requireContext().getResources().getDisplayMetrics().heightPixels * 0.8)
            );
        }

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        TextView editHint = dialogView.findViewById(R.id.edit_number_hint);
        EditText editRoleName = dialogView.findViewById(R.id.edit_number);
        ImageView btnClose = dialogView.findViewById(R.id.btn_close);
        RecyclerView recyclerView = dialogView.findViewById(R.id.role_recycle);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        dialogTitle.setText(title);
        editHint.setText("角色名称");
        btnCancel.setVisibility(View.VISIBLE);
        if (role != null) {
            editRoleName.setText(role.getRoleName());
        }
        if (permissionOnly) {
            editRoleName.setEnabled(false);
        } else {
            editRoleName.setHint("请输入角色名称");
        }

        TreePermissionCheckboxAdapter adapter = new TreePermissionCheckboxAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter.setPermissions(PermissionTreeBuilder.buildTree(copyPermissions(data.allPermissions)));
        List<Long> selectedIds = new ArrayList<>();
        for (Integer id : data.currentPermissionIds) {
            selectedIds.add(id.longValue());
        }
        adapter.setSelectedPermissions(selectedIds);
        recyclerView.setAdapter(adapter);

        View.OnClickListener dismissListener = v -> {
            if (btnConfirm.isEnabled()) {
                dialog.dismiss();
            }
        };
        btnClose.setOnClickListener(dismissListener);
        btnCancel.setOnClickListener(dismissListener);
        btnConfirm.setOnClickListener(v -> {
            String roleName = editRoleName.getText().toString().trim();
            if (TextUtils.isEmpty(roleName)) {
                Toast.makeText(requireContext(), "请输入角色名称", Toast.LENGTH_SHORT).show();
                return;
            }
            List<Integer> permissionIds = new ArrayList<>();
            for (Long id : adapter.getSelectedPermissionIds()) {
                permissionIds.add(id.intValue());
            }
            saveRoleAsync(role, roleName, permissionIds, permissionOnly, btnConfirm, btnCancel, btnClose, dialog);
        });

        try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.OperationBusyEvent(true)); } catch (Exception ignored) {}
        dialog.setOnDismissListener(d -> {
            try { org.greenrobot.eventbus.EventBus.getDefault().post(new com.lora.cn.event.OperationBusyEvent(false)); } catch (Exception ignored) {}
        });
        dialog.show();
    }

    private void saveRoleAsync(@Nullable Role existingRole, String roleName, List<Integer> permissionIds, boolean permissionOnly,
                               Button btnConfirm, Button btnCancel, ImageView btnClose, Dialog dialog) {
        if (ioExecutor == null || mainHandler == null) return;
        setRoleDialogEnabled(btnConfirm, btnCancel, btnClose, false);
        final android.content.Context appContext = requireContext().getApplicationContext();
        ioExecutor.execute(() -> {
            RoleWriteResult result = new RoleWriteResult();
            try {
                DatabaseManager dm = DatabaseManager.getInstance(appContext);
                if (existingRole == null) {
                    if (dm.isRoleNameExists(roleName)) {
                        result.message = "角色名称已存在";
                    } else {
                        Role newRole = new Role();
                        newRole.setRoleName(roleName);
                        newRole.setDescription(null);
                        newRole.setSortOrder(allRoles.size() + 1);
                        newRole.setStatus(1);
                        newRole.setCreateTime(new Date());
                        newRole.setUpdateTime(new Date());
                        long roleId = dm.insertRole(newRole);
                        if (roleId > 0) {
                            newRole.setRoleId(roleId);
                            boolean permissionSuccess = dm.setRolePermissions((int) roleId, permissionIds);
                            if (permissionSuccess || permissionIds.isEmpty()) {
                                result.success = true;
                                result.isAdd = true;
                                result.role = newRole;
                                result.message = "角色添加成功";
                            } else {
                                dm.deleteRole((int) roleId);
                                result.message = "角色权限保存失败";
                            }
                        } else {
                            result.message = "角色添加失败";
                        }
                    }
                } else if (permissionOnly) {
                    boolean success = dm.setRolePermissions((int) existingRole.getRoleId(), permissionIds);
                    result.success = success;
                    result.role = existingRole;
                    result.message = success ? "权限设置保存成功" : "权限设置保存失败";
                } else {
                    if (!roleName.equals(existingRole.getRoleName()) && dm.isRoleNameExists(roleName)) {
                        result.message = "角色名称已存在";
                    } else {
                        Role updatedRole = copyRole(existingRole);
                        updatedRole.setRoleName(roleName);
                        updatedRole.setUpdateTime(new Date());
                        boolean updateSuccess = dm.updateRole(updatedRole);
                        boolean permissionSuccess = updateSuccess && dm.setRolePermissions((int) updatedRole.getRoleId(), permissionIds);
                        if (updateSuccess && permissionSuccess) {
                            result.success = true;
                            result.role = updatedRole;
                            result.message = "角色更新成功";
                        } else {
                            result.message = updateSuccess ? "角色权限保存失败" : "角色更新失败";
                        }
                    }
                }
            } catch (Exception e) {
                LogUtils.e("RoleManagementFragment", "保存角色失败: " + e.getMessage());
                result.message = permissionOnly ? "权限设置保存失败" : "保存角色失败";
            }

            mainHandler.post(() -> {
                if (!isAdded()) return;
                setRoleDialogEnabled(btnConfirm, btnCancel, btnClose, true);
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show();
                if (!result.success) {
                    return;
                }
                if (result.role != null && !permissionOnly) {
                    if (result.isAdd) {
                        roleAdapter.addRole(result.role);
                    } else {
                        roleAdapter.updateRole(result.role);
                    }
                    updateCachedRole(result.role);
                }
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            });
        });
    }

    private void deleteRoleAsync(Role role, Button positiveButton, Button negativeButton, AlertDialog dialog) {
        if (ioExecutor == null || mainHandler == null) return;
        setAlertDialogButtonsEnabled(positiveButton, negativeButton, false);
        final android.content.Context appContext = requireContext().getApplicationContext();
        ioExecutor.execute(() -> {
            boolean success = false;
            String message = "角色删除失败";
            try {
                success = DatabaseManager.getInstance(appContext).deleteRole((int) role.getRoleId());
                message = success ? "角色删除成功" : "角色删除失败";
            } catch (Exception e) {
                LogUtils.e("RoleManagementFragment", "删除角色失败: " + e.getMessage());
            }
            final boolean finalSuccess = success;
            final String finalMessage = message;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                setAlertDialogButtonsEnabled(positiveButton, negativeButton, true);
                Toast.makeText(requireContext(), finalMessage, Toast.LENGTH_SHORT).show();
                if (!finalSuccess) {
                    return;
                }
                roleAdapter.removeRole((int) role.getRoleId());
                removeCachedRole(role.getRoleId());
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            });
        });
    }

    private void toggleRoleStatus(Role role, boolean isEnabled, SwitchCompat switchCompat) {
        if (ioExecutor == null || mainHandler == null) return;
        final int previousStatus = role.getStatus();
        switchCompat.setEnabled(false);
        final android.content.Context appContext = requireContext().getApplicationContext();
        ioExecutor.execute(() -> {
            boolean success = false;
            try {
                Role updatedRole = copyRole(role);
                updatedRole.setStatus(isEnabled ? 1 : 0);
                updatedRole.setUpdateTime(new Date());
                success = DatabaseManager.getInstance(appContext).updateRole(updatedRole);
            } catch (Exception e) {
                LogUtils.e("RoleManagementFragment", "更新角色状态失败: " + e.getMessage());
            }
            final boolean finalSuccess = success;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                switchCompat.setEnabled(true);
                if (finalSuccess) {
                    role.setStatus(isEnabled ? 1 : 0);
                    role.setUpdateTime(new Date());
                    updateCachedRole(role);
                    roleAdapter.updateRole(role);
                    String statusText = isEnabled ? "启用" : "禁用";
                    Toast.makeText(requireContext(), "角色已" + statusText, Toast.LENGTH_SHORT).show();
                } else {
                    switchCompat.setChecked(previousStatus == 1);
                    Toast.makeText(requireContext(), "状态更新失败", Toast.LENGTH_SHORT).show();
                    loadRoles();
                }
            });
        });
    }

    private List<Permission> copyPermissions(List<Permission> permissions) {
        List<Permission> copies = new ArrayList<>();
        if (permissions == null) return copies;
        for (Permission permission : permissions) {
            copies.add(copyPermission(permission));
        }
        return copies;
    }

    private Permission copyPermission(Permission source) {
        Permission target = new Permission();
        target.setPermissionId(source.getPermissionId());
        target.setPermissionName(source.getPermissionName());
        target.setPermissionCode(source.getPermissionCode());
        target.setDescription(source.getDescription());
        target.setCategory(source.getCategory());
        target.setSortOrder(source.getSortOrder());
        target.setStatus(source.getStatus());
        target.setParentId(source.getParentId());
        target.setLevel(source.getLevel());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.isSelect = source.isSelect;
        if (source.getChildList() != null) {
            List<Permission> children = new ArrayList<>();
            for (Permission child : source.getChildList()) {
                children.add(copyPermission(child));
            }
            target.setChildList(children);
        }
        return target;
    }

    private Role copyRole(Role source) {
        Role role = new Role();
        role.setRoleId(source.getRoleId());
        role.setRoleName(source.getRoleName());
        role.setDescription(source.getDescription());
        role.setSortOrder(source.getSortOrder());
        role.setStatus(source.getStatus());
        role.setCreateTime(source.getCreateTime());
        role.setUpdateTime(source.getUpdateTime());
        return role;
    }

    private void updateCachedRole(Role role) {
        if (allRoles == null) return;
        for (int i = 0; i < allRoles.size(); i++) {
            Role item = allRoles.get(i);
            if (item != null && item.getRoleId() == role.getRoleId()) {
                allRoles.set(i, role);
                return;
            }
        }
        allRoles.add(role);
    }

    private void removeCachedRole(long roleId) {
        if (allRoles == null) return;
        for (int i = allRoles.size() - 1; i >= 0; i--) {
            Role item = allRoles.get(i);
            if (item != null && item.getRoleId() == roleId) {
                allRoles.remove(i);
                return;
            }
        }
    }

    private void setRoleDialogEnabled(Button btnConfirm, Button btnCancel, ImageView btnClose, boolean enabled) {
        btnConfirm.setEnabled(enabled);
        btnCancel.setEnabled(enabled);
        btnClose.setEnabled(enabled);
    }

    private void setAlertDialogButtonsEnabled(@Nullable Button positiveButton, @Nullable Button negativeButton, boolean enabled) {
        if (positiveButton != null) positiveButton.setEnabled(enabled);
        if (negativeButton != null) negativeButton.setEnabled(enabled);
    }

    public static RoleManagementFragment newInstance() {
        return new RoleManagementFragment();
    }
    private boolean hasPermission(String permissionCode) {
        return currentUserRoleId > 0 && grantedPermissions.contains(permissionCode);
    }

    private interface RoleDialogCallback {
        void onLoaded(RoleDialogData data);
    }

    private static class RoleDialogData {
        private List<Permission> allPermissions = new ArrayList<>();
        private List<Integer> currentPermissionIds = new ArrayList<>();
    }

    private static class RoleWriteResult {
        private boolean success;
        private boolean isAdd;
        private String message;
        private Role role;
    }

}
