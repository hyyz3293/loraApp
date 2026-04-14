package com.lora.cn.ui.fragment.setting;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.LogUtils;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.Department;
import com.lora.cn.database.entity.Position;
import com.lora.cn.database.entity.Role;
import com.lora.cn.database.entity.User;
import com.lora.cn.ui.adapter.UserAdapter;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用户管理Fragment
 */
public class UserManagementFragment extends Fragment {

    private RecyclerView rvUsers;
    private UserAdapter userAdapter;
    private TextView btnAddUser;
    private TextView btnBack;

    private DatabaseManager databaseManager;
    private List<User> allUsers;
    private long currentUserRoleId = -1;
    private final Set<String> grantedPermissions = new HashSet<>();
    private ExecutorService ioExecutor;
    private Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_management, container, false);
        
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
        rvUsers = view.findViewById(R.id.rv_users);
        btnAddUser = view.findViewById(R.id.btn_add_user);
        btnBack = view.findViewById(R.id.back);
        databaseManager = DatabaseManager.getInstance(requireContext());
        allUsers = new ArrayList<>();
    }

    private void initAsync() {
        if (ioExecutor == null) ioExecutor = Executors.newSingleThreadExecutor();
        if (mainHandler == null) mainHandler = new Handler(Looper.getMainLooper());
        loadCurrentUserPermissionsAsync(this::loadUsers);
    }

    private void loadCurrentUserPermissionsAsync(@Nullable Runnable onComplete) {
        if (ioExecutor == null || mainHandler == null) return;
        final android.content.Context appContext = requireContext().getApplicationContext();
        ioExecutor.execute(() -> {
            long roleId = -1;
            Set<String> permissions = new HashSet<>();
            try {
                long uid = com.blankj.utilcode.util.SPUtils.getInstance().getLong("current_user_id", -1);
                if (uid != -1) {
                    User cur = databaseManager.getUserById(uid);
                    if (cur != null) roleId = cur.getRoleId();
                }
                if (roleId > 0) {
                    String[] permissionCodes = new String[] {
                            "user_add",
                            "user_reset_password",
                            "user_edit",
                            "user_delete",
                            "user_disable"
                    };
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
                if (onComplete != null) {
                    onComplete.run();
                }
            });
        });
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter();
        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUsers.setAdapter(userAdapter);
    }
    
    private void setupListeners() {
        // 返回按钮
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
        
        // 新增用户按钮
        btnAddUser.setOnClickListener(v -> {
            if (hasPermission("user_add")) showAddUserDialog();
            else Toast.makeText(requireContext(), "您没有新增用户的权限", Toast.LENGTH_SHORT).show();
        });

        // 设置适配器点击监听器
        userAdapter.addOnItemChildClickListener(R.id.tv_user_reset_password, (baseQuickAdapter, view, i) -> {
            User user = baseQuickAdapter.getItem(i);
            if (user != null) {
                String acc = user.getUserAccount() == null ? "" : user.getUserAccount().trim();
                if ("admin".equals(acc)) { Toast.makeText(requireContext(), "基础账户不可修改", Toast.LENGTH_SHORT).show(); return; }
                if (hasPermission("user_reset_password")) showResetPasswordDialog(user);
                else Toast.makeText(requireContext(), "您没有重置密码的权限", Toast.LENGTH_SHORT).show();
            }
        });
        
        userAdapter.addOnItemChildClickListener(R.id.tv_user_edit, (baseQuickAdapter, view, i) -> {
            User user = baseQuickAdapter.getItem(i);
            if (user != null) {
                String acc = user.getUserAccount() == null ? "" : user.getUserAccount().trim();
                if ("admin".equals(acc)) { Toast.makeText(requireContext(), "基础账户不可修改", Toast.LENGTH_SHORT).show(); return; }
                if (hasPermission("user_edit")) showEditUserDialog(user);
                else Toast.makeText(requireContext(), "您没有编辑用户的权限", Toast.LENGTH_SHORT).show();
            }
        });
        
        userAdapter.addOnItemChildClickListener(R.id.tv_user_delete, (baseQuickAdapter, view, i) -> {
            User user = baseQuickAdapter.getItem(i);
            if (user != null) {
                String acc = user.getUserAccount() == null ? "" : user.getUserAccount().trim();
                if ("admin".equals(acc)) { Toast.makeText(requireContext(), "基础账户不可删除", Toast.LENGTH_SHORT).show(); return; }
                if (hasPermission("user_delete")) showDeleteConfirmDialog(user);
                else Toast.makeText(requireContext(), "您没有删除用户的权限", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 状态开关监听器
        userAdapter.addOnItemChildClickListener(R.id.switch_user_status, (baseQuickAdapter, view, i) -> {
            User user = baseQuickAdapter.getItem(i);
            if (user != null) {
                String acc = user.getUserAccount() == null ? "" : user.getUserAccount().trim();
                if ("admin".equals(acc)) { Toast.makeText(requireContext(), "基础账户不可修改", Toast.LENGTH_SHORT).show(); return; }
                if (hasPermission("user_disable")) {
                    SwitchCompat switchCompat = (SwitchCompat) view;
                    toggleUserStatus(user, switchCompat.isChecked(), switchCompat);
                } else Toast.makeText(requireContext(), "您没有启用/禁用的权限", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean hasPermission(String code) {
        return currentUserRoleId > 0 && grantedPermissions.contains(code);
    }
    
    private void loadUsers() {
        if (ioExecutor == null || mainHandler == null) return;
        final android.content.Context appContext = requireContext().getApplicationContext();
        ioExecutor.execute(() -> {
            List<User> nextUsers = new ArrayList<>();
            Exception error = null;
            try {
                com.lora.cn.database.DatabaseHelper.getInstance(appContext).ensureDefaultAdminRoleAssigned();
                DatabaseManager dm = DatabaseManager.getInstance(appContext);
                List<User> users = dm.getAllUsers();
                if (users != null) {
                    for (User u : users) {
                        if (u == null) continue;
                        try {
                            if (u.getRole() == null) {
                                u.setRole(dm.getRoleById((int) u.getRoleId()));
                            }
                            if (u.getPosition() == null) {
                                u.setPosition(dm.getPositionById(u.getPositionId()));
                            }
                            if (u.getDepartment() == null) {
                                u.setDepartment(dm.getDepartmentById(u.getDepartmentId()));
                            }
                        } catch (Exception ignored) {}
                        nextUsers.add(u);
                    }
                }
            } catch (Exception e) {
                error = e;
            }

            final Exception finalError = error;
            final List<User> finalUsers = nextUsers;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (finalError != null) {
                    LogUtils.e("UserManagementFragment", "加载用户列表失败: " + finalError.getMessage());
                    Toast.makeText(requireContext(), "加载用户列表失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                allUsers = finalUsers;
                userAdapter.submitList(new ArrayList<>(finalUsers));
            });
        });
    }
    
    private void showAddUserDialog() {
        showUserDialog(null);
    }
    
    private void showEditUserDialog(User user) {
        showUserDialog(user);
    }
    
    private void showUserDialog(User user) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_user, null);
        
        // 获取控件
        EditText etUserName = dialogView.findViewById(R.id.et_user_name);
        EditText etUserAccount = dialogView.findViewById(R.id.et_user_account);
        EditText etUserPassword = dialogView.findViewById(R.id.et_user_password);
        EditText etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);
        Spinner spinnerRole = dialogView.findViewById(R.id.spinner_role);
        SwitchCompat switchStatus = dialogView.findViewById(R.id.switch_status);
        Spinner spinnerPosition = dialogView.findViewById(R.id.spinner_position);
        Spinner spinnerDepartment = dialogView.findViewById(R.id.spinner_department);
        EditText etUserNumber = dialogView.findViewById(R.id.et_user_number);
        RadioGroup rgGender = dialogView.findViewById(R.id.rg_gender);
        
        // 如果是编辑模式，填充数据
        boolean isEdit = user != null;
        if (isEdit) {
            etUserName.setText(user.getUserName());
            etUserAccount.setText(user.getUserAccount());
            etUserAccount.setEnabled(false); // 编辑时不允许修改账号
            
            // 隐藏密码相关字段
            dialogView.findViewById(R.id.til_password).setVisibility(View.GONE);
            dialogView.findViewById(R.id.til_confirm_password).setVisibility(View.GONE);
            
            switchStatus.setChecked(user.getStatus() == 1);
            etUserNumber.setText(user.getUserCode());
            
            // 设置性别
            if ("男".equals(user.getGender())) {
                rgGender.check(R.id.rb_male);
            } else {
                rgGender.check(R.id.rb_female);
            }
            
            // 设置下拉框选中项
            setSpinnerSelection(spinnerRole, user.getRoleId());
            setSpinnerSelection(spinnerPosition, user.getPositionId());
            setSpinnerSelection(spinnerDepartment, user.getDepartmentId());
        } else {
            switchStatus.setChecked(true);
            rgGender.check(R.id.rb_male);
            try {
                Object tag = spinnerRole.getTag();
                if (tag instanceof java.util.List) {
                    java.util.List<Role> roles = (java.util.List<Role>) tag;
                    for (int i = 0; i < roles.size(); i++) {
                        Role r = roles.get(i);
                        if (r != null && "管理员".equals(r.getRoleName())) {
                            spinnerRole.setSelection(i);
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        
        String title = isEdit ? "编辑用户" : "新增用户";
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create();
        
        // 设置确定按钮点击事件
        
        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            android.widget.Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            positiveButton.setEnabled(false);
            positiveButton.setOnClickListener(v -> {
                UserFormData formData = validateUserInput(user, etUserName, etUserAccount, etUserPassword, etConfirmPassword,
                        spinnerRole, switchStatus, spinnerPosition, spinnerDepartment, etUserNumber, rgGender);
                if (formData != null) {
                    saveUserAsync(user, formData, dialog, positiveButton, negativeButton);
                }
            });
            loadDialogSpinnerDataAsync(spinnerRole, spinnerPosition, spinnerDepartment, user, positiveButton);
        });
        
        dialog.show();
    }
    
    private void loadDialogSpinnerDataAsync(Spinner spinnerRole, Spinner spinnerPosition, Spinner spinnerDepartment,
                                            @Nullable User user, android.widget.Button positiveButton) {
        if (ioExecutor == null || mainHandler == null) return;
        final android.content.Context appContext = requireContext().getApplicationContext();
        ioExecutor.execute(() -> {
            UserDialogData data = new UserDialogData();
            Exception error = null;
            try {
                com.lora.cn.database.DatabaseHelper.getInstance(appContext).ensureDefaultAdminRoleAssigned();
                DatabaseManager dm = DatabaseManager.getInstance(appContext);
                data.roles = dm.getActiveRoles();
                data.positions = dm.getAllPositions();
                dm.ensureDefaultDepartmentsSeeded();
                data.departments = dm.getAllDepartments();
            } catch (Exception e) {
                error = e;
            }

            final Exception finalError = error;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (finalError != null) {
                    LogUtils.e("UserManagementFragment", "设置下拉框数据失败: " + finalError.getMessage());
                    Toast.makeText(requireContext(), "加载数据失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                bindSpinnerData(spinnerRole, data.roles, Role::getRoleName);
                bindSpinnerData(spinnerPosition, data.positions, Position::getPositionName);
                bindSpinnerData(spinnerDepartment, data.departments, Department::getDepartmentName);
                try {
                    spinnerDepartment.setEnabled(true);
                    spinnerDepartment.setClickable(true);
                    spinnerDepartment.setPrompt("选择科室");
                    spinnerDepartment.setOnTouchListener(null);
                } catch (Exception ignored) {}

                if (user != null) {
                    setSpinnerSelection(spinnerRole, user.getRoleId());
                    setSpinnerSelection(spinnerPosition, user.getPositionId());
                    setSpinnerSelection(spinnerDepartment, user.getDepartmentId());
                } else {
                    selectDefaultAdminRole(spinnerRole);
                    if (!data.departments.isEmpty()) {
                        spinnerDepartment.setSelection(0);
                    }
                }
                if (data.departments.isEmpty()) {
                    Toast.makeText(requireContext(), "科室列表为空，请先在科室管理中添加科室", Toast.LENGTH_SHORT).show();
                }
                positiveButton.setEnabled(true);
            });
        });
    }

    private <T> void bindSpinnerData(Spinner spinner, List<T> items, NameProvider<T> provider) {
        List<String> names = new ArrayList<>();
        if (items != null) {
            for (T item : items) {
                names.add(provider.getName(item));
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_16dp, names);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_16dp);
        spinner.setAdapter(adapter);
        spinner.setTag(items != null ? items : new ArrayList<>());
    }

    private void selectDefaultAdminRole(Spinner spinnerRole) {
        Object tag = spinnerRole.getTag();
        if (!(tag instanceof List)) return;
        List<?> items = (List<?>) tag;
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            if (item instanceof Role) {
                Role role = (Role) item;
                if (role != null && "管理员".equals(role.getRoleName())) {
                    spinnerRole.setSelection(i);
                    return;
                }
            }
        }
    }
    
    private void setSpinnerSelection(Spinner spinner, long targetId) {
        Object tag = spinner.getTag();
        if (tag instanceof List) {
            List<?> items = (List<?>) tag;
            boolean matched = false;
            for (int i = 0; i < items.size(); i++) {
                Object item = items.get(i);
                long itemId = 0;
                if (item instanceof Role) {
                    itemId = ((Role) item).getRoleId();
                } else if (item instanceof Position) {
                    itemId = ((Position) item).getPositionId();
                } else if (item instanceof Department) {
                    itemId = ((Department) item).getDepartmentId();
                }
                if (itemId == targetId) {
                    spinner.setSelection(i);
                    matched = true;
                    break;
                }
            }
            if (!matched && (items.size() > 0) && items.get(0) instanceof Role) {
                for (int i = 0; i < items.size(); i++) {
                    Role r = (Role) items.get(i);
                    if (r != null && "管理员".equals(r.getRoleName())) {
                        spinner.setSelection(i);
                        break;
                    }
                }
            }
        }
    }
    
    private UserFormData validateUserInput(User existingUser, EditText etUserName, EditText etUserAccount,
                                           EditText etUserPassword, EditText etConfirmPassword, Spinner spinnerRole,
                                           SwitchCompat switchStatus, Spinner spinnerPosition, Spinner spinnerDepartment,
                                           EditText etUserNumber, RadioGroup rgGender) {
        String userName = etUserName.getText().toString().trim();
        String userAccount = etUserAccount.getText().toString().trim();
        String password = etUserPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String userNumber = etUserNumber.getText().toString().trim();

        if (TextUtils.isEmpty(userName)) {
            Toast.makeText(requireContext(), "请输入用户姓名", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (TextUtils.isEmpty(userAccount)) {
            Toast.makeText(requireContext(), "请输入用户账号", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (existingUser == null) { // 新增用户需要验证密码
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(requireContext(), "请输入密码", Toast.LENGTH_SHORT).show();
                return null;
            }
            if (TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(requireContext(), "请确认密码", Toast.LENGTH_SHORT).show();
                return null;
            }
            if (!password.equals(confirmPassword)) {
                Toast.makeText(requireContext(), "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
                return null;
            }
            if (password.length() < 6) {
                Toast.makeText(requireContext(), "密码长度不能少于6位", Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        if (TextUtils.isEmpty(userNumber)) {
            Toast.makeText(requireContext(), "请输入用户编号", Toast.LENGTH_SHORT).show();
            return null;
        }

        Role selectedRole = getSelectedItem(spinnerRole, Role.class);
        Position selectedPosition = getSelectedItem(spinnerPosition, Position.class);
        Department selectedDepartment = getSelectedItem(spinnerDepartment, Department.class);
        if (selectedRole == null) {
            Toast.makeText(requireContext(), "请选择用户角色", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (selectedPosition == null) {
            Toast.makeText(requireContext(), "请选择职位", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (selectedDepartment == null) {
            Toast.makeText(requireContext(), "请选择科室", Toast.LENGTH_SHORT).show();
            return null;
        }

        String gender = rgGender.getCheckedRadioButtonId() == R.id.rb_male ? "男" : "女";

        UserFormData formData = new UserFormData();
        formData.userName = userName;
        formData.userAccount = userAccount;
        formData.password = password;
        formData.userNumber = userNumber;
        formData.gender = gender;
        formData.selectedRole = selectedRole;
        formData.selectedPosition = selectedPosition;
        formData.selectedDepartment = selectedDepartment;
        formData.enabled = switchStatus.isChecked();
        return formData;
    }

    private void saveUserAsync(@Nullable User existingUser, UserFormData formData, AlertDialog dialog,
                               android.widget.Button positiveButton, android.widget.Button negativeButton) {
        if (ioExecutor == null || mainHandler == null) return;
        setDialogButtonsEnabled(positiveButton, negativeButton, false);
        final android.content.Context appContext = requireContext().getApplicationContext();
        ioExecutor.execute(() -> {
            UserWriteResult result = new UserWriteResult();
            try {
                DatabaseManager dm = DatabaseManager.getInstance(appContext);
                if (existingUser == null) {
                    if (dm.isUserAccountExists(formData.userAccount)) {
                        result.message = "用户账号已存在";
                    } else {
                        User newUser = new User();
                        newUser.setUserName(formData.userName);
                        newUser.setUserAccount(formData.userAccount);
                        newUser.setUserPassword(formData.password);
                        newUser.setRoleId(formData.selectedRole.getRoleId());
                        newUser.setStatus(formData.enabled ? 1 : 0);
                        newUser.setPositionId(formData.selectedPosition.getPositionId());
                        newUser.setDepartmentId(formData.selectedDepartment.getDepartmentId());
                        newUser.setUserCode(formData.userNumber);
                        newUser.setGender(formData.gender);
                        long userId = dm.addUser(newUser);
                        if (userId > 0) {
                            newUser.setUserId(userId);
                            newUser.setRole(formData.selectedRole);
                            newUser.setPosition(formData.selectedPosition);
                            newUser.setDepartment(formData.selectedDepartment);
                            result.success = true;
                            result.isAdd = true;
                            result.user = newUser;
                            result.message = "用户添加成功";
                        } else {
                            result.message = "用户添加失败";
                        }
                    }
                } else {
                    User updatedUser = copyUser(existingUser);
                    updatedUser.setUserName(formData.userName);
                    updatedUser.setRoleId(formData.selectedRole.getRoleId());
                    updatedUser.setStatus(formData.enabled ? 1 : 0);
                    updatedUser.setPositionId(formData.selectedPosition.getPositionId());
                    updatedUser.setDepartmentId(formData.selectedDepartment.getDepartmentId());
                    updatedUser.setUserCode(formData.userNumber);
                    updatedUser.setGender(formData.gender);
                    updatedUser.setRole(formData.selectedRole);
                    updatedUser.setPosition(formData.selectedPosition);
                    updatedUser.setDepartment(formData.selectedDepartment);
                    if (dm.updateUser(updatedUser)) {
                        result.success = true;
                        result.user = updatedUser;
                        result.message = "用户更新成功";
                    } else {
                        result.message = "用户更新失败";
                    }
                }
            } catch (Exception e) {
                LogUtils.e("UserManagementFragment", "保存用户失败: " + e.getMessage());
                result.message = "保存用户失败";
            }

            mainHandler.post(() -> {
                if (!isAdded()) return;
                setDialogButtonsEnabled(positiveButton, negativeButton, true);
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show();
                if (!result.success || result.user == null) {
                    return;
                }
                if (result.isAdd) {
                    userAdapter.addUser(result.user);
                } else {
                    userAdapter.updateUser(result.user);
                }
                updateCachedUser(result.user);
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            });
        });
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getSelectedItem(Spinner spinner, Class<T> clazz) {
        Object tag = spinner.getTag();
        if (tag instanceof List) {
            List<T> items = (List<T>) tag;
            int selectedPosition = spinner.getSelectedItemPosition();
            if (selectedPosition >= 0 && selectedPosition < items.size()) {
                return items.get(selectedPosition);
            }
        }
        return null;
    }
    
    private void showResetPasswordDialog(User user) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("重置密码")
                .setMessage("确定要重置用户 \"" + user.getUserName() + "\" 的密码吗？\n重置后密码将变为：123456")
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            android.widget.Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            positiveButton.setOnClickListener(v -> resetPasswordAsync(user, positiveButton, negativeButton, dialog));
        });
        dialog.show();
    }
    
    private void showDeleteConfirmDialog(User user) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("删除用户")
                .setMessage("确定要删除用户 \"" + user.getUserName() + "\" 吗？此操作不可撤销。")
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            android.widget.Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            positiveButton.setOnClickListener(v -> deleteUserAsync(user, positiveButton, negativeButton, dialog));
        });
        dialog.show();
    }
    
    private void resetPasswordAsync(User user, android.widget.Button positiveButton,
                                    android.widget.Button negativeButton, AlertDialog dialog) {
        if (ioExecutor == null || mainHandler == null) return;
        setDialogButtonsEnabled(positiveButton, negativeButton, false);
        final android.content.Context appContext = requireContext().getApplicationContext();
        ioExecutor.execute(() -> {
            boolean success = false;
            String message = "密码重置失败";
            try {
                success = DatabaseManager.getInstance(appContext).updateUserPassword(user.getUserId(), "123456");
                message = success ? "密码重置成功" : "密码重置失败";
            } catch (Exception e) {
                LogUtils.e("UserManagementFragment", "重置密码失败: " + e.getMessage());
            }
            final boolean finalSuccess = success;
            final String finalMessage = message;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                setDialogButtonsEnabled(positiveButton, negativeButton, true);
                Toast.makeText(requireContext(), finalMessage, Toast.LENGTH_SHORT).show();
                if (finalSuccess && dialog.isShowing()) {
                    dialog.dismiss();
                }
            });
        });
    }

    private void deleteUserAsync(User user, android.widget.Button positiveButton,
                                 android.widget.Button negativeButton, AlertDialog dialog) {
        if (ioExecutor == null || mainHandler == null) return;
        setDialogButtonsEnabled(positiveButton, negativeButton, false);
        final android.content.Context appContext = requireContext().getApplicationContext();
        ioExecutor.execute(() -> {
            boolean success = false;
            String message = "删除用户失败";
            try {
                success = DatabaseManager.getInstance(appContext).deleteUser(user.getUserId());
                message = success ? "删除成功" : "删除失败";
            } catch (Exception e) {
                LogUtils.e("UserManagementFragment", "删除用户失败: " + e.getMessage());
            }
            final boolean finalSuccess = success;
            final String finalMessage = message;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                setDialogButtonsEnabled(positiveButton, negativeButton, true);
                Toast.makeText(requireContext(), finalMessage, Toast.LENGTH_SHORT).show();
                if (!finalSuccess) {
                    return;
                }
                userAdapter.removeUser(user.getUserId());
                removeCachedUser(user.getUserId());
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            });
        });
    }

    private void toggleUserStatus(User user, boolean isEnabled, SwitchCompat switchCompat) {
        if (ioExecutor == null || mainHandler == null) return;
        final int previousStatus = user.getStatus();
        switchCompat.setEnabled(false);
        final android.content.Context appContext = requireContext().getApplicationContext();
        ioExecutor.execute(() -> {
            boolean success = false;
            try {
                User updatedUser = copyUser(user);
                updatedUser.setStatus(isEnabled ? 1 : 0);
                success = DatabaseManager.getInstance(appContext).updateUser(updatedUser);
            } catch (Exception e) {
                LogUtils.e("UserManagementFragment", "更新用户状态失败: " + e.getMessage());
            }
            final boolean finalSuccess = success;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                switchCompat.setEnabled(true);
                if (finalSuccess) {
                    user.setStatus(isEnabled ? 1 : 0);
                    userAdapter.updateUser(user);
                    updateCachedUser(user);
                    String statusText = isEnabled ? "启用" : "禁用";
                    Toast.makeText(requireContext(), "用户已" + statusText, Toast.LENGTH_SHORT).show();
                } else {
                    switchCompat.setChecked(previousStatus == 1);
                    Toast.makeText(requireContext(), "状态更新失败", Toast.LENGTH_SHORT).show();
                    loadUsers();
                }
            });
        });
    }

    private void setDialogButtonsEnabled(@Nullable android.widget.Button positiveButton,
                                         @Nullable android.widget.Button negativeButton,
                                         boolean enabled) {
        if (positiveButton != null) positiveButton.setEnabled(enabled);
        if (negativeButton != null) negativeButton.setEnabled(enabled);
    }

    private User copyUser(User source) {
        User user = new User();
        user.setUserId(source.getUserId());
        user.setUserName(source.getUserName());
        user.setUserAccount(source.getUserAccount());
        user.setUserPassword(source.getUserPassword());
        user.setRoleId(source.getRoleId());
        user.setStatus(source.getStatus());
        user.setPositionId(source.getPositionId());
        user.setDepartmentId(source.getDepartmentId());
        user.setUserCode(source.getUserCode());
        user.setGender(source.getGender());
        user.setPhone(source.getPhone());
        user.setCreateTime(source.getCreateTime());
        user.setUpdateTime(source.getUpdateTime());
        user.setRole(source.getRole());
        user.setPosition(source.getPosition());
        user.setDepartment(source.getDepartment());
        return user;
    }

    private void updateCachedUser(User user) {
        if (allUsers == null) return;
        for (int i = 0; i < allUsers.size(); i++) {
            User item = allUsers.get(i);
            if (item != null && item.getUserId() == user.getUserId()) {
                allUsers.set(i, user);
                return;
            }
        }
        allUsers.add(user);
    }

    private void removeCachedUser(long userId) {
        if (allUsers == null) return;
        for (int i = allUsers.size() - 1; i >= 0; i--) {
            User item = allUsers.get(i);
            if (item != null && item.getUserId() == userId) {
                allUsers.remove(i);
                return;
            }
        }
    }

    public static UserManagementFragment newInstance() {
        return new UserManagementFragment();
    }

    private interface NameProvider<T> {
        String getName(T item);
    }

    private static class UserDialogData {
        private List<Role> roles = new ArrayList<>();
        private List<Position> positions = new ArrayList<>();
        private List<Department> departments = new ArrayList<>();
    }

    private static class UserFormData {
        private String userName;
        private String userAccount;
        private String password;
        private String userNumber;
        private String gender;
        private boolean enabled;
        private Role selectedRole;
        private Position selectedPosition;
        private Department selectedDepartment;
    }

    private static class UserWriteResult {
        private boolean success;
        private boolean isAdd;
        private String message;
        private User user;
    }
}
