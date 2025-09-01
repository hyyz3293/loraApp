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

import com.lora.cn.R;

public class PasswordChangeFragment extends Fragment {

    private EditText etOldPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private TextView btnCancelPassword;
    private TextView btnSavePassword;

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

        if (listener != null) {
            listener.onSavePassword(oldPassword, newPassword);
        }

        Toast.makeText(getContext(), "密码修改成功", Toast.LENGTH_SHORT).show();
        
        // 清空输入框
        clearInputs();
    }

    private void clearInputs() {
        etOldPassword.setText("");
        etNewPassword.setText("");
        etConfirmPassword.setText("");
    }

    public void resetForm() {
        clearInputs();
    }
}