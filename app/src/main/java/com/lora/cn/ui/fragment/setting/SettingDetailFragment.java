package com.lora.cn.ui.fragment.setting;

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

public class SettingDetailFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_ICON = "icon";
    
    private TextView titleText;
    private ImageView backButton;
    private String title;
    private int iconResId;

    public static SettingDetailFragment newInstance(String title, int iconResId) {
        SettingDetailFragment fragment = new SettingDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putInt(ARG_ICON, iconResId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
            iconResId = getArguments().getInt(ARG_ICON);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting_detail, container, false);
        
        initViews(view);
        setupContent();
        
        return view;
    }

    private void initViews(View view) {
        titleText = view.findViewById(R.id.setting_detail_title);
        backButton = view.findViewById(R.id.setting_detail_back);
        
        // 设置返回按钮点击事件
        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void setupContent() {
        if (titleText != null && title != null) {
            titleText.setText(title);
        }
        
        // TODO: 根据不同的设置项显示不同的内容
        // 可以在这里添加具体的设置界面内容
    }
}