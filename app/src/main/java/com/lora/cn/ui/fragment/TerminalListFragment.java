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

public class TerminalListFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal_list, container, false);
        
        // 初始化视图
        TextView tvTitle = view.findViewById(R.id.tv_fragment_title);
        tvTitle.setText("终端列表");
        
        // TODO: 在此添加终端列表的具体实现
        
        return view;
    }

    public static TerminalListFragment newInstance() {
        return new TerminalListFragment();
    }
}