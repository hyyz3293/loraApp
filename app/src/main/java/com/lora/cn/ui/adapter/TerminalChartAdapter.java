package com.lora.cn.ui.adapter;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.ui.model.TerminalChartData;
import com.lora.cn.ui.view.PieChartView;

public class TerminalChartAdapter extends BaseQuickAdapter<TerminalChartData, QuickViewHolder> {

    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int position, @Nullable TerminalChartData item) {
        // 获取视图组件
        TextView chartOnlineTitle = holder.getView(R.id.chart_online_title);
        TextView chartBatteryTitle = holder.getView(R.id.chart_battery_title);
        PieChartView chartOnline = holder.getView(R.id.chart_online);
        PieChartView chartBattery = holder.getView(R.id.chart_battery);
        RecyclerView chartOnlineRecycle = holder.getView(R.id.chart_online_recycle);
        RecyclerView chartBatteryRecycle = holder.getView(R.id.chart_battery_recycle);
        
        // 设置标题
        chartOnlineTitle.setText(item.getOnlineTitle());
        chartBatteryTitle.setText(item.getBatteryTitle());
        
        // 设置饼图数据
        chartOnline.setData(item.getOnlinePieData());
        chartBattery.setData(item.getBatteryPieData());
        
        // 设置在线状态图例RecyclerView
        ChartItemAdapter onlineAdapter = new ChartItemAdapter();
        chartOnlineRecycle.setLayoutManager(new LinearLayoutManager(getContext()));
        chartOnlineRecycle.setAdapter(onlineAdapter);
        onlineAdapter.submitList(item.getOnlineChartItems());
        
        // 设置电量状态图例RecyclerView
        ChartItemAdapter batteryAdapter = new ChartItemAdapter();
        chartBatteryRecycle.setLayoutManager(new LinearLayoutManager(getContext()));
        chartBatteryRecycle.setAdapter(batteryAdapter);
        batteryAdapter.submitList(item.getBatteryChartItems());
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup parent, int viewType) {
        return new QuickViewHolder(R.layout.item_terminal_chart, parent);
    }
}