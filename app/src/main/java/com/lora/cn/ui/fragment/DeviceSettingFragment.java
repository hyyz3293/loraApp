package com.lora.cn.ui.fragment;

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

public class DeviceSettingFragment extends Fragment {

//    private TextView titleText;
//    private ImageView backButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_setting, container, false);
        
//        initViews(view);
//        setupContent();
//
        return view;
    }

//    private void initViews(View view) {
//        titleText = view.findViewById(R.id.setting_title);
//        backButton = view.findViewById(R.id.setting_back);
//
//        backButton.setOnClickListener(v -> {
//            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
//                getParentFragmentManager().popBackStack();
//            }
//        });
//    }
//
//    private void setupContent() {
//        if (titleText != null) {
//            titleText.setText("设备设置");
//        }
//    }

    public static DeviceSettingFragment newInstance() {
        return new DeviceSettingFragment();
    }
}