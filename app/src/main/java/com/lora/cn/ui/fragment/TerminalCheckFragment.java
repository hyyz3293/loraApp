package com.lora.cn.ui.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lora.cn.R;
import com.lora.cn.ui.view.PieChartView;

import java.util.ArrayList;
import java.util.List;

public class TerminalCheckFragment extends Fragment {

    private PieChartView pieChartOnline;
    private PieChartView pieChartBattery;
    private Switch switchShowLines;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal_check, container, false);
        
        initViews(view);
        initPieChartData();
        
        return view;
    }
    
    private void initViews(View view) {
        pieChartOnline = view.findViewById(R.id.pie_chart_online);
        pieChartBattery = view.findViewById(R.id.pie_chart_battery);
        switchShowLines = view.findViewById(R.id.switch_show_lines);
        
        // 设置开关监听器
        switchShowLines.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pieChartOnline.setShowLines(isChecked);
            pieChartBattery.setShowLines(isChecked);
        });
    }
    
    private void initPieChartData() {
        // 初始化在线状态饼状图数据
        List<PieChartView.PieData> onlineData = new ArrayList<>();
        onlineData.add(new PieChartView.PieData("在线", 65.0f, Color.parseColor("#4CAF50")));
        onlineData.add(new PieChartView.PieData("离线", 20.0f, Color.parseColor("#F44336")));
        onlineData.add(new PieChartView.PieData("异常", 15.0f, Color.parseColor("#FF9800")));
        
        pieChartOnline.setData(onlineData);
        
        // 初始化电量状态饼状图数据
        List<PieChartView.PieData> batteryData = new ArrayList<>();
        batteryData.add(new PieChartView.PieData("正常", 70.0f, Color.parseColor("#4CAF50")));
        batteryData.add(new PieChartView.PieData("低电", 25.0f, Color.parseColor("#FF9F0F")));
        batteryData.add(new PieChartView.PieData("电量", 5.0f, Color.parseColor("#D30000")));
        
        pieChartBattery.setData(batteryData);
    }

    public static TerminalCheckFragment newInstance() {
        return new TerminalCheckFragment();
    }
}