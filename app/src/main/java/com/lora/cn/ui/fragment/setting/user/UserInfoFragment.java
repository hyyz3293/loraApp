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

    private TextView back;

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
        back = view.findViewById(R.id.back);
    }

    private void initListeners() {
        back.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCloseUserInfo();
            }
        });

//        btnEditProfile.setOnClickListener(v -> {
//            if (listener != null) {
//                listener.onEditProfile();
//            }
//        });
    }

    private void loadUserInfo() {

    }

    public void updateUserInfo(String userName, String userRole) {

    }
}