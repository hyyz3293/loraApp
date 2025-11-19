package com.lora.cn.ui.fragment.setting;

import android.app.AlertDialog;
import android.os.Bundle;
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
import java.util.List;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_management, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadUsers();
        
        return view;
    }

    private void initViews(View view) {
        rvUsers = view.findViewById(R.id.rv_users);
        btnAddUser = view.findViewById(R.id.btn_add_user);
        btnBack = view.findViewById(R.id.back);
        databaseManager = DatabaseManager.getInstance(requireContext());
        allUsers = new ArrayList<>();
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
        btnAddUser.setOnClickListener(v -> showAddUserDialog());

        // 设置适配器点击监听器
        userAdapter.addOnItemChildClickListener(R.id.tv_user_reset_password, (baseQuickAdapter, view, i) -> {
            User user = baseQuickAdapter.getItem(i);
            if (user != null) {
                showResetPasswordDialog(user);
            }
        });
        
        userAdapter.addOnItemChildClickListener(R.id.tv_user_edit, (baseQuickAdapter, view, i) -> {
            User user = baseQuickAdapter.getItem(i);
            if (user != null) {
                showEditUserDialog(user);
            }
        });
        
        userAdapter.addOnItemChildClickListener(R.id.tv_user_delete, (baseQuickAdapter, view, i) -> {
            User user = baseQuickAdapter.getItem(i);
            if (user != null) {
                showDeleteConfirmDialog(user);
            }
        });
        
        // 状态开关监听器
        userAdapter.addOnItemChildClickListener(R.id.switch_user_status, (baseQuickAdapter, view, i) -> {
            User user = baseQuickAdapter.getItem(i);
            if (user != null) {
                SwitchCompat switchCompat = (SwitchCompat) view;
                toggleUserStatus(user, switchCompat.isChecked());
            }
        });
    }
    
    private void loadUsers() {
        try {
            com.lora.cn.database.DatabaseHelper.getInstance(requireContext()).ensureDefaultAdminRoleAssigned();
            allUsers = databaseManager.getAllUsers();
            userAdapter.submitList(allUsers);
        } catch (Exception e) {
            LogUtils.e("UserManagementFragment", "加载用户列表失败: " + e.getMessage());
            Toast.makeText(requireContext(), "加载用户列表失败", Toast.LENGTH_SHORT).show();
        }
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
        
        // 设置下拉框数据
        setupSpinners(spinnerRole, spinnerPosition, spinnerDepartment);
        try {
            spinnerDepartment.setEnabled(true);
            spinnerDepartment.setClickable(true);
            spinnerDepartment.setPrompt("选择科室");
            spinnerDepartment.setOnTouchListener(null);
        } catch (Exception ignored) {}
        
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
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (validateAndSaveUser(user, etUserName, etUserAccount, etUserPassword, etConfirmPassword,
                        spinnerRole, switchStatus, spinnerPosition, spinnerDepartment, etUserNumber, rgGender)) {
                    dialog.dismiss();
                }
            });
        });
        
        dialog.show();
    }
    
    private void setupSpinners(Spinner spinnerRole, Spinner spinnerPosition, Spinner spinnerDepartment) {
        try {
            com.lora.cn.database.DatabaseHelper.getInstance(requireContext()).ensureDefaultAdminRoleAssigned();

            // 设置角色下拉框
            List<Role> roles = databaseManager.getActiveRoles();
            List<String> roleNames = new ArrayList<>();
            for (Role role : roles) {
                roleNames.add(role.getRoleName());
            }
            ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, roleNames);
            roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRole.setAdapter(roleAdapter);
            spinnerRole.setTag(roles);
            
            // 设置职位下拉框
            List<Position> positions = databaseManager.getAllPositions();
            List<String> positionNames = new ArrayList<>();
            for (Position position : positions) {
                positionNames.add(position.getPositionName());
            }
            ArrayAdapter<String> positionAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, positionNames);
            positionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerPosition.setAdapter(positionAdapter);
            spinnerPosition.setTag(positions);
            
            // 设置科室下拉框
            //allDepartments = dbManager.getCategoriesByGroupId(1);
            databaseManager.ensureDefaultDepartmentsSeeded();
            List<Department> departments = databaseManager.getAllDepartments();
            List<String> departmentNames = new ArrayList<>();
            for (Department department : departments) {
                departmentNames.add(department.getDepartmentName());
            }
            ArrayAdapter<String> departmentAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, departmentNames);
            departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDepartment.setAdapter(departmentAdapter);
            spinnerDepartment.setTag(departments);
            if (!departments.isEmpty()) {
                spinnerDepartment.setSelection(0);
            }
            if (departments.isEmpty()) {
                Toast.makeText(requireContext(), "科室列表为空，请先在科室管理中添加科室", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            LogUtils.e("UserManagementFragment", "设置下拉框数据失败: " + e.getMessage());
            Toast.makeText(requireContext(), "加载数据失败", Toast.LENGTH_SHORT).show();
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
    
    private boolean validateAndSaveUser(User existingUser, EditText etUserName, EditText etUserAccount,
                                       EditText etUserPassword, EditText etConfirmPassword, Spinner spinnerRole,
                                       SwitchCompat switchStatus, Spinner spinnerPosition, Spinner spinnerDepartment,
                                       EditText etUserNumber, RadioGroup rgGender) {
        
        // 获取输入值
        String userName = etUserName.getText().toString().trim();
        String userAccount = etUserAccount.getText().toString().trim();
        String password = etUserPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String userNumber = etUserNumber.getText().toString().trim();
        
        // 验证必填字段
        if (TextUtils.isEmpty(userName)) {
            Toast.makeText(requireContext(), "请输入用户姓名", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (TextUtils.isEmpty(userAccount)) {
            Toast.makeText(requireContext(), "请输入用户账号", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (existingUser == null) { // 新增用户需要验证密码
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(requireContext(), "请输入密码", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            if (TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(requireContext(), "请确认密码", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            if (!password.equals(confirmPassword)) {
                Toast.makeText(requireContext(), "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            if (password.length() < 6) {
                Toast.makeText(requireContext(), "密码长度不能少于6位", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        
        if (TextUtils.isEmpty(userNumber)) {
            Toast.makeText(requireContext(), "请输入用户编号", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // 获取选中的角色、职位、科室
        Role selectedRole = getSelectedItem(spinnerRole, Role.class);
        if (selectedRole == null) {
            try {
                List<Role> roles = databaseManager.getActiveRoles();
                for (Role r : roles) {
                    if (r != null && "管理员".equals(r.getRoleName())) { selectedRole = r; break; }
                }
            } catch (Exception ignored) {}
        }
        Position selectedPosition = getSelectedItem(spinnerPosition, Position.class);
        Department selectedDepartment = getSelectedItem(spinnerDepartment, Department.class);
        if (selectedDepartment == null) {
            try {
                Object tagDept = spinnerDepartment.getTag();
                java.util.List<Department> deptList;
                if (tagDept instanceof java.util.List) {
                    deptList = (java.util.List<Department>) tagDept;
                } else {
                    deptList = databaseManager.getAllDepartments();
                }
                int idx = spinnerDepartment.getSelectedItemPosition();
                if (deptList != null && !deptList.isEmpty()) {
                    if (idx >= 0 && idx < deptList.size()) selectedDepartment = deptList.get(idx);
                    else selectedDepartment = deptList.get(0);
                }
            } catch (Exception ignored) {}
        }
        
        if (selectedRole == null) {
            Toast.makeText(requireContext(), "请选择用户角色", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (selectedPosition == null) {
            Toast.makeText(requireContext(), "请选择职位", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (selectedDepartment == null) {
            Toast.makeText(requireContext(), "请选择科室", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // 获取性别
        String gender = rgGender.getCheckedRadioButtonId() == R.id.rb_male ? "男" : "女";
        
        try {
            if (existingUser == null) {
                // 检查账号是否已存在
                if (databaseManager.isUserAccountExists(userAccount)) {
                    Toast.makeText(requireContext(), "用户账号已存在", Toast.LENGTH_SHORT).show();
                    return false;
                }
                
                // 新增用户
                User newUser = new User();
                newUser.setUserName(userName);
                newUser.setUserAccount(userAccount);
                newUser.setUserPassword(password);
                newUser.setRoleId(selectedRole.getRoleId());
                newUser.setStatus(switchStatus.isChecked() ? 1 : 0);
                newUser.setPositionId(selectedPosition.getPositionId());
                newUser.setDepartmentId(selectedDepartment.getDepartmentId());
                newUser.setUserCode(userNumber);
                newUser.setGender(gender);
                
                long userId = databaseManager.addUser(newUser);
                if (userId > 0) {
                    newUser.setUserId(userId);
                    newUser.setRole(selectedRole);
                    newUser.setPosition(selectedPosition);
                    newUser.setDepartment(selectedDepartment);
                    userAdapter.addUser(newUser);
                    Toast.makeText(requireContext(), "用户添加成功", Toast.LENGTH_SHORT).show();
                    return true;
                } else {
                    Toast.makeText(requireContext(), "用户添加失败", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else {
                // 更新用户
                existingUser.setUserName(userName);
                existingUser.setRoleId(selectedRole.getRoleId());
                existingUser.setStatus(switchStatus.isChecked() ? 1 : 0);
                existingUser.setPositionId(selectedPosition.getPositionId());
                existingUser.setDepartmentId(selectedDepartment.getDepartmentId());
                existingUser.setUserCode(userNumber);
                existingUser.setGender(gender);
                existingUser.setRole(selectedRole);
                existingUser.setPosition(selectedPosition);
                existingUser.setDepartment(selectedDepartment);
                
                boolean success = databaseManager.updateUser(existingUser);
                if (success) {
                    userAdapter.updateUser(existingUser);
                    Toast.makeText(requireContext(), "用户更新成功", Toast.LENGTH_SHORT).show();
                    return true;
                } else {
                    Toast.makeText(requireContext(), "用户更新失败", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        } catch (Exception e) {
            LogUtils.e("UserManagementFragment", "保存用户失败: " + e.getMessage());
            Toast.makeText(requireContext(), "保存用户失败", Toast.LENGTH_SHORT).show();
            return false;
        }
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
        new AlertDialog.Builder(requireContext())
                .setTitle("重置密码")
                .setMessage("确定要重置用户 \"" + user.getUserName() + "\" 的密码吗？\n重置后密码将变为：123456")
                .setPositiveButton("确定", (dialog, which) -> {
                    try {
                        boolean success = databaseManager.updateUserPassword(user.getUserId(), "123456");
                        if (success) {
                            Toast.makeText(requireContext(), "密码重置成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "密码重置失败", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        LogUtils.e("UserManagementFragment", "重置密码失败: " + e.getMessage());
                        Toast.makeText(requireContext(), "重置密码失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void showDeleteConfirmDialog(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除用户")
                .setMessage("确定要删除用户 \"" + user.getUserName() + "\" 吗？此操作不可撤销。")
                .setPositiveButton("确定", (dialog, which) -> {
                    try {
                        boolean success = databaseManager.deleteUser(user.getUserId());
                        if (success) {
                            userAdapter.removeUser(user.getUserId());
                            Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        LogUtils.e("UserManagementFragment", "删除用户失败: " + e.getMessage());
                        Toast.makeText(requireContext(), "删除用户失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void toggleUserStatus(User user, boolean isEnabled) {
        try {
            user.setStatus(isEnabled ? 1 : 0);
            boolean success = databaseManager.updateUser(user);
            if (success) {
                userAdapter.updateUser(user);
                String statusText = isEnabled ? "启用" : "禁用";
                Toast.makeText(requireContext(), "用户已" + statusText, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "状态更新失败", Toast.LENGTH_SHORT).show();
                loadUsers(); // 重新加载数据
            }
        } catch (Exception e) {
            LogUtils.e("UserManagementFragment", "更新用户状态失败: " + e.getMessage());
            Toast.makeText(requireContext(), "状态更新失败", Toast.LENGTH_SHORT).show();
            loadUsers(); // 重新加载数据
        }
    }

    public static UserManagementFragment newInstance() {
        return new UserManagementFragment();
    }
}