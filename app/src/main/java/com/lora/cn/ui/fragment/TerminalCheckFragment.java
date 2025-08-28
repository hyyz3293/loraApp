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
    //private Switch switchShowLines;

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
//        switchShowLines = view.findViewById(R.id.switch_show_lines);
//
//        // 设置开关监听器
//        switchShowLines.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            pieChartOnline.setShowLines(isChecked);
//            pieChartBattery.setShowLines(isChecked);
//        });
    }
    
    private void initPieChartData() {
        // 初始化在线状态饼状图数据
        List<PieChartView.PieData> onlineData = new ArrayList<>();
        onlineData.add(new PieChartView.PieData("正常取走", "11", 65.0f, Color.parseColor("#5D75F7")));
        onlineData.add(new PieChartView.PieData("在线", 20.0f, Color.parseColor("#39E56D")));
        onlineData.add(new PieChartView.PieData("异常丢失", 10.0f, Color.parseColor("#D00000")));
        onlineData.add(new PieChartView.PieData("离线", 5.0f, Color.parseColor("#CECECE")));

        pieChartOnline.setData(onlineData);
        
        // 初始化电量状态饼状图数据
        List<PieChartView.PieData> batteryData = new ArrayList<>();
        batteryData.add(new PieChartView.PieData("正常电量", 70.0f, Color.parseColor("#39E56D")));
        batteryData.add(new PieChartView.PieData("低电量", 25.0f, Color.parseColor("#D00000")));
        batteryData.add(new PieChartView.PieData("离线", 5.0f, Color.parseColor("#CECECE")));
        
        pieChartBattery.setData(batteryData);
    }

    public static TerminalCheckFragment newInstance() {
        return new TerminalCheckFragment();
    }
}