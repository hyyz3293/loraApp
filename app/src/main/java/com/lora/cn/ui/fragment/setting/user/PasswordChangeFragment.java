package com.lora.cn.ui.fragment.setting.user;

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
import androidx.fragment.app.Fragment;

import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.User;
import com.blankj.utilcode.util.LogUtils;

public class PasswordChangeFragment extends Fragment {

    private EditText etOldPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private TextView btnCancelPassword;
    private TextView btnSavePassword;
    private View ivToggleOld;
    private View ivToggleNew;
    private View ivToggleConfirm;

    private DatabaseManager databaseManager;
    private User currentUser;
    private OnPasswordChangeListener listener;

    public interface OnPasswordChangeListener {
        void onCancelPasswordChange();
        void onSavePassword(String oldPassword, String newPassword);
    }

    public void setOnPasswordChangeListener(OnPasswordChangeListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_password_change, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        initListeners();
    }

    private void initViews(View view) {
        etOldPassword = view.findViewById(R.id.et_old_password);
        etNewPassword = view.findViewById(R.id.et_new_password);
        etConfirmPassword = view.findViewById(R.id.et_confirm_password);
        btnCancelPassword = view.findViewById(R.id.btn_cancel_password);
        btnSavePassword = view.findViewById(R.id.btn_save_password);
        ivToggleOld = view.findViewById(R.id.iv_toggle_old_password);
        ivToggleNew = view.findViewById(R.id.iv_toggle_new_password);
        ivToggleConfirm = view.findViewById(R.id.iv_toggle_confirm_password);
        
        // 初始化数据库管理器
        databaseManager = DatabaseManager.getInstance(getContext());
        loadCurrentUser();
    }

    private void initListeners() {
        btnCancelPassword.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancelPasswordChange();
            }
        });

        btnSavePassword.setOnClickListener(v -> {
            if (validatePasswordInput()) {
                changePassword();
            }
        });
        if (ivToggleOld != null) ivToggleOld.setOnClickListener(v -> togglePasswordVisibility(etOldPassword));
        if (ivToggleNew != null) ivToggleNew.setOnClickListener(v -> togglePasswordVisibility(etNewPassword));
        if (ivToggleConfirm != null) ivToggleConfirm.setOnClickListener(v -> togglePasswordVisibility(etConfirmPassword));
    }

    private void togglePasswordVisibility(EditText editText) {
        if (editText == null) return;
        int currentType = editText.getInputType();
        boolean isPassword = (currentType & android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;
        if (isPassword) {
            editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            editText.setTransformationMethod(null);
        } else {
            editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            editText.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
        }
        editText.setSelection(editText.getText() != null ? editText.getText().length() : 0);
    }

    private boolean validatePasswordInput() {
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(oldPassword)) {
            Toast.makeText(getContext(), "请输入原密码", Toast.LENGTH_SHORT).show();
            etOldPassword.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(getContext(), "请输入新密码", Toast.LENGTH_SHORT).show();
            etNewPassword.requestFocus();
            return false;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(getContext(), "新密码长度不能少于6位", Toast.LENGTH_SHORT).show();
            etNewPassword.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(getContext(), "请输入确认密码", Toast.LENGTH_SHORT).show();
            etConfirmPassword.requestFocus();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(getContext(), "新密码与确认密码不一致", Toast.LENGTH_SHORT).show();
            etConfirmPassword.requestFocus();
            return false;
        }

        if (oldPassword.equals(newPassword)) {
            Toast.makeText(getContext(), "新密码不能与原密码相同", Toast.LENGTH_SHORT).show();
            etNewPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void changePassword() {
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        try {
            if (currentUser == null) {
                Toast.makeText(getContext(), "用户信息获取失败", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 验证原密码
            if (!oldPassword.equals(currentUser.getUserPassword())) {
                Toast.makeText(getContext(), "原密码不正确", Toast.LENGTH_SHORT).show();
                etOldPassword.requestFocus();
                return;
            }
            
            // 更新密码
            boolean success = databaseManager.updateUserPassword(currentUser.getUserId(), newPassword);
            if (success) {
                // 更新当前用户对象的密码
                currentUser.setUserPassword(newPassword);
                
                if (listener != null) {
                    listener.onSavePassword(oldPassword, newPassword);
                }
                
                Toast.makeText(getContext(), "密码修改成功", Toast.LENGTH_SHORT).show();
                
                // 清空输入框
                clearInputs();
            } else {
                Toast.makeText(getContext(), "密码修改失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            LogUtils.e("PasswordChangeFragment", "修改密码失败: " + e.getMessage());
            Toast.makeText(getContext(), "密码修改失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearInputs() {
        etOldPassword.setText("");
        etNewPassword.setText("");
        etConfirmPassword.setText("");
    }

    public void resetForm() {
        clearInputs();
    }
    
    private void loadCurrentUser() {
        try {
            // 从SharedPreferences获取当前登录用户ID
            long currentUserId = SPUtils.getInstance().getLong("current_user_id", -1);
            if (currentUserId != -1) {
                // 从数据库获取用户信息
                currentUser = databaseManager.getUserById(currentUserId);
                if (currentUser == null) {
                    LogUtils.e("PasswordChangeFragment", "无法获取当前用户信息");
                }
            } else {
                LogUtils.e("PasswordChangeFragment", "未找到当前登录用户ID");
            }
        } catch (Exception e) {
            LogUtils.e("PasswordChangeFragment", "加载用户信息失败: " + e.getMessage());
        }
    }
}
