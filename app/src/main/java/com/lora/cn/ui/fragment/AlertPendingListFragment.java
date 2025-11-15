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
        View btnBack = v.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(view -> {
                try {
                    androidx.appcompat.app.AppCompatActivity a = (androidx.appcompat.app.AppCompatActivity) getActivity();
                    if (a != null) {
                        a.getSupportFragmentManager().popBackStack();
                        android.view.View containerView = a.findViewById(R.id.fragment_device_list_container);
                        if (containerView != null) containerView.setVisibility(View.GONE);
                        android.view.View rvTabs = a.findViewById(R.id.rv_menu_tabs);
                        if (rvTabs != null) rvTabs.setVisibility(View.VISIBLE);
                        android.view.View vp = a.findViewById(R.id.view_pager);
                        if (vp != null) vp.setVisibility(View.VISIBLE);
                    }
                } catch (Exception ignored) {}
            });
        }
        loadAlerts();
        return v;
    }

    private void loadAlerts() {
        try {
            DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
            List<LogInfo> all = db.getAllLogsBoundToTerminals();
            java.util.Map<String, LogInfo> latest = new java.util.HashMap<>();
            for (LogInfo li : all) {
                int s = li.getStatusCode();
                if (s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code
                        || s == com.lora.cn.ui.constants.LogStatus.LOW_BATTERY.code
                        || s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code) {
                    String key = (li.getTerminalId() == null ? "" : li.getTerminalId()) + ":" + s;
                    LogInfo prev = latest.get(key);
                    long prevT = prev != null ? parseMillis(prev.getCreateTime()) : -1L;
                    long curT = parseMillis(li.getCreateTime());
                    if (prev == null || curT >= prevT) latest.put(key, li);
                }
            }
            java.util.List<LogInfo> pending = new java.util.ArrayList<>(latest.values());
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

    private long parseMillis(String time) {
        if (time == null || time.length() == 0) return -1L;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date d = sdf.parse(time);
            return d != null ? d.getTime() : -1L;
        } catch (Exception e) {
            return -1L;
        }
    }
}
