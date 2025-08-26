package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lora.cn.R;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        // 初始化视图
        TextView tvTitle = view.findViewById(R.id.tv_fragment_title);
        tvTitle.setText("设置");
        
        // TODO: 在此添加设置页面的具体实现
        
        return view;
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }
}