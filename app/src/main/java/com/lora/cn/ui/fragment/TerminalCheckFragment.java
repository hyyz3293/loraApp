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

public class TerminalCheckFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal_check, container, false);
        
        // 初始化视图
        TextView tvTitle = view.findViewById(R.id.tv_fragment_title);
        tvTitle.setText("清点终端");
        
        // TODO: 在此添加清点终端的具体实现
        
        return view;
    }

    public static TerminalCheckFragment newInstance() {
        return new TerminalCheckFragment();
    }
}