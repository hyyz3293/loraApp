package com.lora.cn.ui.model;

import com.lora.cn.ui.view.PieChartView;
import java.util.List;

public class TerminalChartData {
    private String onlineTitle;                           // 在线状态标题
    private String batteryTitle;                          // 电量状态标题
    private List<PieChartView.PieData> onlinePieData;     // 在线状态饼图数据
    private List<PieChartView.PieData> batteryPieData;    // 电量状态饼图数据
    private List<ChartItem> onlineChartItems;             // 在线状态图例数据
    private List<ChartItem> batteryChartItems;            // 电量状态图例数据
    
    public TerminalChartData() {}
    
    public TerminalChartData(String onlineTitle, String batteryTitle,
                           List<PieChartView.PieData> onlinePieData,
                           List<PieChartView.PieData> batteryPieData,
                           List<ChartItem> onlineChartItems,
                           List<ChartItem> batteryChartItems) {
        this.onlineTitle = onlineTitle;
        this.batteryTitle = batteryTitle;
        this.onlinePieData = onlinePieData;
        this.batteryPieData = batteryPieData;
        this.onlineChartItems = onlineChartItems;
        this.batteryChartItems = batteryChartItems;
    }
    
    // Getters and Setters
    public String getOnlineTitle() {
        return onlineTitle;
    }
    
    public void setOnlineTitle(String onlineTitle) {
        this.onlineTitle = onlineTitle;
    }
    
    public String getBatteryTitle() {
        return batteryTitle;
    }
    
    public void setBatteryTitle(String batteryTitle) {
        this.batteryTitle = batteryTitle;
    }
    
    public List<PieChartView.PieData> getOnlinePieData() {
        return onlinePieData;
    }
    
    public void setOnlinePieData(List<PieChartView.PieData> onlinePieData) {
        this.onlinePieData = onlinePieData;
    }
    
    public List<PieChartView.PieData> getBatteryPieData() {
        return batteryPieData;
    }
    
    public void setBatteryPieData(List<PieChartView.PieData> batteryPieData) {
        this.batteryPieData = batteryPieData;
    }
    
    public List<ChartItem> getOnlineChartItems() {
        return onlineChartItems;
    }
    
    public void setOnlineChartItems(List<ChartItem> onlineChartItems) {
        this.onlineChartItems = onlineChartItems;
    }
    
    public List<ChartItem> getBatteryChartItems() {
        return batteryChartItems;
    }
    
    public void setBatteryChartItems(List<ChartItem> batteryChartItems) {
        this.batteryChartItems = batteryChartItems;
    }
}