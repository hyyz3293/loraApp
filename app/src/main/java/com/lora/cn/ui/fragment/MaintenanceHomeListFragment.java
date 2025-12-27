package com.lora.cn.ui.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.ui.adapter.MaintenanceInfoAdapter;
import com.lora.cn.ui.model.MaintenanceInfo;
import com.lora.cn.ui.activity.MainActivity;
import com.lora.cn.utils.DownlinkMessageHelper;
import com.lora.cn.network.MqttPacketsClient;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.greenrobot.eventbus.EventBus;

public class MaintenanceHomeListFragment extends Fragment {

    private SmartRefreshLayout swipe;
    private RecyclerView rv;
    private MaintenanceInfoAdapter adapter;
    private DatabaseHelper db;
    private long currentUserId = -1;
    private String currentUserName = "";
    private ExecutorService ioExecutor;
    private Handler mainHandler;
    private final AtomicInteger loadSeq = new AtomicInteger(0);
    private final List<MaintenanceInfo> allFiltered = new ArrayList<>();
    private final List<MaintenanceInfo> currentDisplay = new ArrayList<>();
    private int pageSize = 20;
    private int currentPage = 0;
    private boolean loadingMore = false;

    public static MaintenanceHomeListFragment newInstance() {
        return new MaintenanceHomeListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_maintenance_list_home, container, false);
        swipe = v.findViewById(R.id.swipe_maintenance);
        rv = v.findViewById(R.id.rv_maintenance);

        db = DatabaseHelper.getInstance(requireContext());
        currentUserId = SPUtils.getInstance().getLong("current_user_id", -1);
        currentUserName = SPUtils.getInstance().getString("current_user_name", "");
        if (ioExecutor == null) ioExecutor = Executors.newSingleThreadExecutor();
        if (mainHandler == null) mainHandler = new Handler(Looper.getMainLooper());

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MaintenanceInfoAdapter(MaintenanceInfoAdapter.Mode.HOME);
        adapter.setOnConfirmClickListener(this::showConfirmDialog);
        adapter.setOnViewClickListener(this::showViewDialog);
        adapter.setOnEditClickListener(this::showEditDialog);
        adapter.setOnDeleteClickListener(this::showDeleteDialog);
        rv.setAdapter(adapter);

        if (swipe != null) {
            swipe.setEnableRefresh(true);
            swipe.setEnableLoadMore(true);
            swipe.setOnRefreshListener(layout -> {
                loadList();
            });
            swipe.setOnLoadMoreListener(layout -> {
                boolean noMore = ((currentPage + 1) * pageSize) >= allFiltered.size();
                if (noMore) {
                    layout.finishLoadMoreWithNoMoreData();
                    swipe.setEnableLoadMore(false);
                } else {
                    loadMorePage();
                    layout.finishLoadMore(true);
                }
            });
        }
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && !recyclerView.canScrollVertically(1)) {
                    loadMorePage();
                }
            }
        });

        loadList();
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (ioExecutor != null) ioExecutor.shutdownNow();
        } catch (Exception ignored) {}
        ioExecutor = null;
        mainHandler = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadList();
    }

    private void loadList() {
        if (ioExecutor == null || mainHandler == null) return;
        int token = loadSeq.incrementAndGet();
        ioExecutor.execute(() -> {
            List<MaintenanceInfo> list;
            try {
                list = db.getMaintenanceRecords(currentUserId);
                if (list == null) list = new ArrayList<>();
            } catch (Exception e) {
                list = null;
            }
            List<MaintenanceInfo> finalList = list;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (token != loadSeq.get()) return;
                if (finalList == null) {
                    Toast.makeText(requireContext(), "加载维护列表失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<MaintenanceInfo> filtered = new ArrayList<>();
                long now = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
                for (MaintenanceInfo mi : finalList) {
                    String ct = mi.getCreateTime();
                    if (ct == null || ct.trim().isEmpty()) continue;
                    try {
                        Date dt = sdf.parse(ct.trim());
                        if (dt != null && dt.getTime() <= now) filtered.add(mi);
                    } catch (Exception ignored) {}
                }
                allFiltered.clear();
                allFiltered.addAll(filtered);
                currentPage = 0;
                currentDisplay.clear();
                submitCurrentPage();
                autoDispatchDue(filtered);
                if (swipe != null) swipe.finishRefresh(true);
            });
        });
    }

    private void submitCurrentPage() {
        int start = currentPage * pageSize;
        int end = Math.min(allFiltered.size(), start + pageSize);
        if (start >= allFiltered.size()) return;
        currentDisplay.clear();
        currentDisplay.addAll(allFiltered.subList(0, end));
        adapter.submitList(new ArrayList<>(currentDisplay));
        adapter.notifyDataSetChanged();
    }

    private void loadMorePage() {
        if (loadingMore) return;
        int nextStart = (currentPage + 1) * pageSize;
        if (nextStart >= allFiltered.size()) return;
        loadingMore = true;
        mainHandler.post(() -> {
            int end = Math.min(allFiltered.size(), nextStart + pageSize);
            List<MaintenanceInfo> more = allFiltered.subList(nextStart, end);
            currentDisplay.addAll(more);
            adapter.submitList(new ArrayList<>(currentDisplay));
            adapter.notifyDataSetChanged();
            currentPage++;
            loadingMore = false;
        });
    }

    private void autoDispatchDue(List<MaintenanceInfo> list) {
        if (list == null || list.isEmpty()) return;
        MainActivity a = (MainActivity) getActivity();
        MqttPacketsClient client = a != null ? a.getMqttClient() : null;
        if (client == null) return;
        DownlinkMessageHelper helper = new DownlinkMessageHelper(client);
        for (MaintenanceInfo mi : list) {
            if (mi == null) continue;
            if (mi.getStatus() != 0) continue;
            String ct = mi.getCreateTime();
            if (TextUtils.isEmpty(ct)) continue;
            String key = "maintenance_sent_" + mi.getId();
            boolean sent = SPUtils.getInstance().getBoolean(key, false);
            if (sent) continue;
            String dev = mi.getTerminalId();
            if (TextUtils.isEmpty(dev)) continue;
            try {
                helper.sendQueryStatusDownlink(dev);
                SPUtils.getInstance().put(key, true);
                EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("maintenance_updated"));
            } catch (Exception ignored) {}
        }
    }

    private void showConfirmDialog(MaintenanceInfo item) {
        if (item == null) return;
        EditText et = new EditText(requireContext());
        et.setMinLines(3);
        et.setHint("请输入备注");
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("确认维护")
                .setView(et)
                .setPositiveButton("确认", (d, w) -> {
                    String content = et.getText() != null ? et.getText().toString().trim() : "";
                    if (ioExecutor == null || mainHandler == null) return;
                    ioExecutor.execute(() -> {
                        int r;
                        try {
                            r = db.updateMaintenanceHandled(item.getId(), currentUserId, currentUserName, nowStr(), content);
                        } catch (Exception e) {
                            r = 0;
                        }
                        int finalR = r;
                        mainHandler.post(() -> {
                            if (!isAdded()) return;
                            if (finalR > 0) {
                                loadList();
                                Toast.makeText(requireContext(), "已确认维护", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), "操作失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("取消", null)
                .create();
        dialog.show();
    }

    private void showEditDialog(MaintenanceInfo item) {
        if (item == null) return;
        EditText et = new EditText(requireContext());
        et.setMinLines(3);
        et.setText(item.getContent() == null ? "" : item.getContent());
        et.setSelection(et.getText().length());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("编辑维护")
                .setView(et)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.setOnShowListener(dlg -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String content = et.getText() != null ? et.getText().toString().trim() : "";
            if (TextUtils.isEmpty(content)) {
                Toast.makeText(requireContext(), "请输入维护内容", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ioExecutor == null || mainHandler == null) return;
            ioExecutor.execute(() -> {
                int r;
                try {
                    r = db.updateMaintenanceContent(item.getId(), content);
                } catch (Exception e) {
                    r = 0;
                }
                int finalR = r;
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    if (finalR > 0) {
                        dialog.dismiss();
                        loadList();
                        Toast.makeText(requireContext(), "已保存", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "保存失败", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }));
        dialog.show();
    }

    private void showDeleteDialog(MaintenanceInfo item) {
        if (item == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("删除确认")
                .setMessage("确定删除这条维护记录吗？")
                .setPositiveButton("删除", (d, w) -> {
                    if (ioExecutor == null || mainHandler == null) return;
                    ioExecutor.execute(() -> {
                        int r;
                        try {
                            r = db.deleteMaintenanceRecord(item.getId());
                        } catch (Exception e) {
                            r = 0;
                        }
                        int finalR = r;
                        mainHandler.post(() -> {
                            if (!isAdded()) return;
                            if (finalR > 0) {
                                loadList();
                                Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("取消", null)
                .create()
                .show();
    }

    private void showViewDialog(MaintenanceInfo item) {
        if (item == null) return;
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(item.getContent())) sb.append(item.getContent());
        if (!TextUtils.isEmpty(item.getHandleUser()) || !TextUtils.isEmpty(item.getHandleTime())) {
            if (sb.length() > 0) sb.append("\n\n");
            if (!TextUtils.isEmpty(item.getHandleUser())) sb.append("维护人：").append(item.getHandleUser()).append("\n");
            if (!TextUtils.isEmpty(item.getHandleTime())) sb.append("维护时间：").append(item.getHandleTime());
        }
        if (!TextUtils.isEmpty(item.getHandleRemark())) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("备注：").append(item.getHandleRemark());
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("维护内容")
                .setMessage(sb.toString())
                .setPositiveButton("确定", null)
                .create()
                .show();
    }

    private String nowStr() {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}
