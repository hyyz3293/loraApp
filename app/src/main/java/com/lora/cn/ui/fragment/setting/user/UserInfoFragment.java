package com.lora.cn.ui.fragment.setting.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lora.cn.R;

public class UserInfoFragment extends Fragment {

    private ImageView ivUserAvatarLarge;
    private TextView tvUserNameDetail;
    private TextView tvUserRole;
    private TextView tvUserId;
    private TextView tvUserDepartment;
    private TextView tvLoginTime;
    private TextView tvPermissionLevel;
    private TextView tvLoginCount;
    private TextView tvOnlineDuration;
    private TextView btnCloseUserInfo;
    private TextView btnEditProfile;

    private OnUserInfoActionListener listener;

    public interface OnUserInfoActionListener {
        void onCloseUserInfo();
        void onEditProfile();
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
        ivUserAvatarLarge = view.findViewById(R.id.iv_user_avatar_large);
        tvUserNameDetail = view.findViewById(R.id.tv_user_name_detail);
        tvUserRole = view.findViewById(R.id.tv_user_role);
        tvUserId = view.findViewById(R.id.tv_user_id);
        tvUserDepartment = view.findViewById(R.id.tv_user_department);
        tvLoginTime = view.findViewById(R.id.tv_login_time);
        tvPermissionLevel = view.findViewById(R.id.tv_permission_level);
        tvLoginCount = view.findViewById(R.id.tv_login_count);
        tvOnlineDuration = view.findViewById(R.id.tv_online_duration);
        btnCloseUserInfo = view.findViewById(R.id.btn_close_user_info);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
    }

    private void initListeners() {
        btnCloseUserInfo.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCloseUserInfo();
            }
        });

        btnEditProfile.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditProfile();
            }
        });
    }

    private void loadUserInfo() {
        // 这里可以加载实际的用户信息
        // 目前使用默认值
        tvUserNameDetail.setText("当前用户");
        tvUserRole.setText("管理员");
        tvUserId.setText("U001");
        tvUserDepartment.setText("技术部");
        tvLoginTime.setText("2024-01-15 09:30");
        tvPermissionLevel.setText("高级权限");
        tvLoginCount.setText("156");
        tvOnlineDuration.setText("2小时30分钟");
    }

    public void updateUserInfo(String userName, String userRole) {
        if (tvUserNameDetail != null) {
            tvUserNameDetail.setText(userName);
        }
        if (tvUserRole != null) {
            tvUserRole.setText(userRole);
        }
    }
}