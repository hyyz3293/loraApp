package com.lora.cn.ui.fragment.setting.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.User;
import com.lora.cn.database.entity.Position;
import com.lora.cn.database.entity.Department;
import com.blankj.utilcode.util.LogUtils;

public class UserInfoEditFragment extends Fragment {

    private EditText etUserName;
    private Spinner spGender;
    private EditText etPosition;
    private EditText etDepartment;
    private EditText etUserId;
    private EditText etPhone;
    private TextView btnCancel;
    private TextView btnSave;

    private DatabaseManager databaseManager;
    private User currentUser;
    private OnUserInfoEditListener listener;

    public interface OnUserInfoEditListener {
        void onCancelEdit();
        void onSaveUserInfo(UserInfo userInfo);
    }

    public static class UserInfo {
        public String userName;
        public String gender;
        public String position;
        public String department;
        public String userId;
        public String phone;

        public UserInfo(String userName, String gender, String position, String department, String userId, String phone) {
            this.userName = userName;
            this.gender = gender;
            this.position = position;
            this.department = department;
            this.userId = userId;
            this.phone = phone;
        }
    }

    public void setOnUserInfoEditListener(OnUserInfoEditListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_info_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        initListeners();
        loadDefaultData();
    }

    private void initViews(View view) {
        etUserName = view.findViewById(R.id.et_user_name);
        spGender = view.findViewById(R.id.sp_gender);
        etPosition = view.findViewById(R.id.et_position);
        etDepartment = view.findViewById(R.id.et_department);
        etUserId = view.findViewById(R.id.et_user_id);
        etPhone = view.findViewById(R.id.et_phone);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnSave = view.findViewById(R.id.btn_save);
        
        // 初始化数据库管理器
        databaseManager = DatabaseManager.getInstance(getContext());
        
        // 设置性别Spinner
        String[] genderOptions = {"男", "女"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, genderOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(adapter);
    }

    private void initListeners() {
        btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancelEdit();
            }
        });

        btnSave.setOnClickListener(v -> {
            if (validateInput()) {
                saveUserInfo();
            }
        });
    }

    private void loadDefaultData() {
        try {
            // 从SharedPreferences获取当前登录用户ID
            long currentUserId = SPUtils.getInstance().getLong("current_user_id", -1);
            if (currentUserId != -1) {
                // 从数据库获取用户信息
                currentUser = databaseManager.getUserById(currentUserId);
                if (currentUser != null) {
                    // 填充用户信息
                    etUserName.setText(currentUser.getUserName());
                    
                    // 设置性别
                    if ("男".equals(currentUser.getGender())) {
                        spGender.setSelection(0);
                    } else {
                        spGender.setSelection(1);
                    }
                    
                    // 获取并设置职位信息
                    if (currentUser.getPositionId() > 0) {
                        Position position = databaseManager.getPositionById((int) currentUser.getPositionId());
                        if (position != null) {
                            etPosition.setText(position.getPositionName());
                        }
                    }
                    
                    // 获取并设置科室信息
                    if (currentUser.getDepartmentId() > 0) {
                        Department department = databaseManager.getDepartmentById((int) currentUser.getDepartmentId());
                        if (department != null) {
                            etDepartment.setText(department.getDepartmentName());
                        }
                    }
                    
                    etUserId.setText(currentUser.getUserCode());
                    etPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
                    return;
                }
            }
        } catch (Exception e) {
            LogUtils.e("UserInfoEditFragment", "加载用户数据失败: " + e.getMessage());
        }
        
        // 如果获取失败，使用默认数据
        etUserName.setText("管理员A");
        spGender.setSelection(1);
        etPosition.setText("护士长");
        etDepartment.setText("内1科");
        etUserId.setText("HS0001");
        etPhone.setText("13896981378");
    }

    private boolean validateInput() {
        if (etUserName.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "请输入用户姓名", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (etPosition.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "请输入职务", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (etDepartment.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "请输入科室", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (etUserId.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "请输入编号", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (etPhone.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "请输入联系电话", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void saveUserInfo() {
        try {
            if (currentUser != null) {
                String userName = etUserName.getText().toString().trim();
                String gender = spGender.getSelectedItem().toString();
                String phone = etPhone.getText().toString().trim();
                
                // 更新用户信息
                currentUser.setUserName(userName);
                currentUser.setGender(gender);
                currentUser.setPhone(phone);
                
                // 保存到数据库
                boolean success = databaseManager.updateUser(currentUser);
                if (success) {
                    UserInfo userInfo = new UserInfo(userName, gender, 
                        etPosition.getText().toString().trim(),
                        etDepartment.getText().toString().trim(),
                        etUserId.getText().toString().trim(), phone);
                    
                    if (listener != null) {
                        listener.onSaveUserInfo(userInfo);
                    }
                    
                    Toast.makeText(getContext(), "用户信息保存成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "用户信息保存失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "用户信息获取失败，无法保存", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            LogUtils.e("UserInfoEditFragment", "保存用户信息失败: " + e.getMessage());
            Toast.makeText(getContext(), "用户信息保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    public void setUserInfo(UserInfo userInfo) {
        if (userInfo != null) {
            etUserName.setText(userInfo.userName);
            if ("男".equals(userInfo.gender)) {
                spGender.setSelection(0);
            } else {
                spGender.setSelection(1);
            }
            etPosition.setText(userInfo.position);
            etDepartment.setText(userInfo.department);
            etUserId.setText(userInfo.userId);
            etPhone.setText(userInfo.phone);
        }
    }
}