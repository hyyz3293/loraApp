package com.lora.cn.ui.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lora.cn.R;
import com.lora.cn.ui.view.PieChartView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TerminalCheckFragment extends Fragment {

    private PieChartView pieChartOnline;
    private PieChartView pieChartBattery;
    private TextView terminalRemainingNumber;
    private TextView terminalClearTime;
    private TextView addTerminal;
    
    // 数据字段
    private int remainingCount = 1;
    private boolean isChecking = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal_check, container, false);
        
        initViews(view);
        initData();
        initListeners();
        
        return view;
    }
    
    private void initViews(View view) {
        pieChartOnline = view.findViewById(R.id.pie_chart_online);
        pieChartBattery = view.findViewById(R.id.pie_chart_battery);
        terminalRemainingNumber = view.findViewById(R.id.terminal_remaining_number);
        terminalClearTime = view.findViewById(R.id.terminal_clear_time);
        addTerminal = view.findViewById(R.id.add_terminal);
    }
    
    private void initData() {
        // 更新UI数据
        updateRemainingCount(remainingCount);
        updateClearTime();
        
        // 初始化饼图数据
        initPieChartData();
    }
    
    private void initListeners() {
        // 开始清点按钮点击事件
        addTerminal.setOnClickListener(v -> {
            if (remainingCount <= 0) {
                Toast.makeText(getContext(), "今日清点次数已用完", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (isChecking) {
                Toast.makeText(getContext(), "正在清点中，请稍候...", Toast.LENGTH_SHORT).show();
                return;
            }
            
            startTerminalCheck();
        });
    }
    
    private void initPieChartData() {
        // 初始化在线状态饼状图数据
        List<PieChartView.PieData> onlineData = new ArrayList<>();
        onlineData.add(new PieChartView.PieData("正常取走", "11", 65.0f, Color.parseColor("#5D75F7")));
        onlineData.add(new PieChartView.PieData("在线", "20", 20.0f, Color.parseColor("#39E56D")));
        onlineData.add(new PieChartView.PieData("异常丢失", "10", 10.0f, Color.parseColor("#D00000")));
        onlineData.add(new PieChartView.PieData("离线", "5", 5.0f, Color.parseColor("#CECECE")));

        pieChartOnline.setData(onlineData);
        
        // 初始化电量状态饼状图数据
        List<PieChartView.PieData> batteryData = new ArrayList<>();
        batteryData.add(new PieChartView.PieData("正常电量", "70", 70.0f, Color.parseColor("#39E56D")));
        batteryData.add(new PieChartView.PieData("低电量", "25", 25.0f, Color.parseColor("#D00000")));
        batteryData.add(new PieChartView.PieData("离线", "5", 5.0f, Color.parseColor("#CECECE")));
        
        pieChartBattery.setData(batteryData);
    }
    
    /**
     * 开始终端清点
     */
    private void startTerminalCheck() {
        isChecking = true;
        addTerminal.setText("清点中...");
        addTerminal.setEnabled(false);
        
        // 模拟清点过程（实际项目中这里应该调用相应的API）
        new android.os.Handler().postDelayed(() -> {
            // 清点完成
            isChecking = false;
            addTerminal.setText("开始清点");
            addTerminal.setEnabled(true);
            
            // 减少剩余次数
            updateRemainingCount(remainingCount - 1);
            
            // 更新清点时间
            updateClearTime();
            
            // 刷新数据（这里可以调用API获取最新数据）
            refreshData();
            
            Toast.makeText(getContext(), "清点完成", Toast.LENGTH_SHORT).show();
        }, 2000); // 模拟2秒的清点时间
    }
    
    /**
     * 更新剩余次数
     */
    private void updateRemainingCount(int count) {
        this.remainingCount = Math.max(0, count);
        if (terminalRemainingNumber != null) {
            terminalRemainingNumber.setText(String.valueOf(this.remainingCount));
        }
    }
    
    /**
     * 更新清点时间
     */
    private void updateClearTime() {
        if (terminalClearTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            terminalClearTime.setText(sdf.format(new Date()));
        }
    }
    
    /**
     * 刷新数据
     */
    public void refreshData() {
        // 这里可以调用API获取最新的终端数据
        // 然后更新饼图显示
        initPieChartData();
    }
    
    /**
     * 更新在线状态数据
     */
    public void updateOnlineData(List<PieChartView.PieData> data) {
        if (pieChartOnline != null && data != null) {
            pieChartOnline.setData(data);
        }
    }
    
    /**
     * 更新电量状态数据
     */
    public void updateBatteryData(List<PieChartView.PieData> data) {
        if (pieChartBattery != null && data != null) {
            pieChartBattery.setData(data);
        }
    }
    
    /**
     * 设置剩余清点次数
     */
    public void setRemainingCount(int count) {
        updateRemainingCount(count);
    }
    
    /**
     * 获取剩余清点次数
     */
    public int getRemainingCount() {
        return remainingCount;
    }
    
    /**
     * 检查是否正在清点
     */
    public boolean isChecking() {
        return isChecking;
    }

    public static TerminalCheckFragment newInstance() {
        return new TerminalCheckFragment();
    }
}