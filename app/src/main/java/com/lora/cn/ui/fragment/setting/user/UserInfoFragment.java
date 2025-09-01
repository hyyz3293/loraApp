package com.lora.cn.ui.fragment.setting.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lora.cn.R;
import com.lora.cn.ui.fragment.setting.user.UserInfoEditFragment;
import com.lora.cn.ui.fragment.setting.user.PasswordChangeFragment;

public class UserInfoFragment extends Fragment {

    private TextView back;
    private RelativeLayout userInfo;
    private RelativeLayout userPwd;

    private OnUserInfoActionListener listener;

    public interface OnUserInfoActionListener {
        void onCloseUserInfo();
    }

    public void setOnUserInfoActionListener(OnUserInfoActionListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        initListeners();
        loadUserInfo();
    }

    private void initViews(View view) {
        back = view.findViewById(R.id.back);
        userInfo = view.findViewById(R.id.user_info);
        userPwd = view.findViewById(R.id.user_pwd);
    }

    private void initListeners() {
        back.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCloseUserInfo();
            }
        });

        userInfo.setOnClickListener(v -> {
            // 直接跳转到用户信息编辑Fragment，保留顶部
            UserInfoEditFragment editFragment = new UserInfoEditFragment();
            editFragment.setOnUserInfoEditListener(new UserInfoEditFragment.OnUserInfoEditListener() {
                @Override
                public void onCancelEdit() {
                    // 返回到当前Fragment
                    getParentFragmentManager().popBackStack();
                }

                @Override
                public void onSaveUserInfo(UserInfoEditFragment.UserInfo userInfo) {
                    // 保存用户信息逻辑
                    getParentFragmentManager().popBackStack();
                }
            });
            
            getParentFragmentManager().beginTransaction()
                    .replace(getId(), editFragment)
                    .addToBackStack(null)
                    .commit();
        });

        userPwd.setOnClickListener(v -> {
            // 直接跳转到密码修改Fragment，保留顶部
            PasswordChangeFragment passwordFragment = new PasswordChangeFragment();
            passwordFragment.setOnPasswordChangeListener(new PasswordChangeFragment.OnPasswordChangeListener() {
                @Override
                public void onCancelPasswordChange() {
                    // 返回到当前Fragment
                    getParentFragmentManager().popBackStack();
                }

                @Override
                public void onSavePassword(String oldPassword, String newPassword) {
                    // 保存密码逻辑
                    getParentFragmentManager().popBackStack();
                }
            });
            
            getParentFragmentManager().beginTransaction()
                    .replace(getId(), passwordFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void loadUserInfo() {

    }

    public void updateUserInfo(String userName, String userRole) {

    }
}