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
import com.lora.cn.ui.adapter.LogInfoAlertAdapter;
import com.lora.cn.ui.model.LogInfo;
import com.lora.cn.utils.DialogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 报警待处理列表（简单用日志筛选实现）
 */
public class AlertPendingListFragment extends Fragment {

    private RecyclerView rv;
    private LogInfoAlertAdapter adapter;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_alert_pending_list, container, false);
        rv = v.findViewById(R.id.rv_alerts);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LogInfoAlertAdapter();
        adapter.setOnHandleClickListener(this::showHandleDialogForLog);
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
            java.util.Set<Long> ids = new java.util.HashSet<>();
            for (LogInfo li : pending) ids.add(li.getId());
            adapter.setAllowedHandleIds(ids);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "加载报警列表失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showHandleDialogForLog(LogInfo item) {
        if (item == null) return;
        DialogUtils.showRemarkDialog(requireContext(), "确认处理", "", new com.lora.cn.utils.DialogUtils.OnConfirmListener() {
            @Override
            public void onConfirm(String remark) {
                String user = com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", "");
                String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                try {
                    DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
                    db.updateLogHandled(item.getId(), user, time, remark);
                    loadAlerts();
                } catch (Exception ignored) {}
            }
        });
    }
}
