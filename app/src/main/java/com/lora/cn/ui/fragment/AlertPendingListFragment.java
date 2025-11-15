package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.ui.adapter.LogInfoAdapter;
import com.lora.cn.ui.model.LogInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 报警待处理列表（简单用日志筛选实现）
 */
public class AlertPendingListFragment extends Fragment {

    private RecyclerView rv;
    private LogInfoAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_alert_pending_list, container, false);
        rv = v.findViewById(R.id.rv_alerts);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LogInfoAdapter();
        rv.setAdapter(adapter);
        loadAlerts();
        return v;
    }

    private void loadAlerts() {
        try {
            DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
            List<LogInfo> all = db.getAllLogsBoundToTerminals();
            List<LogInfo> pending = new ArrayList<>();
            for (LogInfo li : all) {
                int s = li.getStatusCode();
                if (s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                        || s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code
                        || s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code) {
                    pending.add(li);
                }
            }
            adapter.submitList(pending);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "加载报警列表失败", Toast.LENGTH_SHORT).show();
        }
    }
}
