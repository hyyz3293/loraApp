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
    private int filterStatus = -1;
    private View rlStart;
    private View rlEnd;
    private android.widget.TextView tvStart;
    private android.widget.TextView tvEnd;
    private String selectedStartTime = "";
    private String selectedEndTime = "";

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
        adapter.setOnViewRemarkClickListener(this::showRemarkDialog);
        adapter.setOnEditClickListener(this::showEditDialog);
        adapter.setOnDeleteClickListener(this::showDeleteDialog);
        rv.setAdapter(adapter);

        rlStart = v.findViewById(R.id.time_start_time);
        rlEnd = v.findViewById(R.id.time_end_time);
        tvStart = v.findViewById(R.id.time_start_time_tv);
        tvEnd = v.findViewById(R.id.time_end_time_tv);
        android.widget.TextView btnReset = v.findViewById(R.id.btn_reset_filters);

        android.widget.Spinner spinnerStatus = v.findViewById(R.id.spinner_status);
        if (spinnerStatus != null) {
            java.util.List<String> opts = new java.util.ArrayList<>();
            opts.add("全部");
            opts.add("待维护");
            opts.add("已维护");
            android.widget.ArrayAdapter<String> adapterSpinner = new android.widget.ArrayAdapter<>(requireContext(), R.layout.spinner_item_12dp, opts);
            adapterSpinner.setDropDownViewResource(R.layout.spinner_dropdown_item_12dp);
            spinnerStatus.setAdapter(adapterSpinner);
            spinnerStatus.setSelection(0, false);
            spinnerStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view1, int position, long id) {
                    if (position == 0) filterStatus = -1;
                    else if (position == 1) filterStatus = 0;
                    else filterStatus = 1;
                    currentPage = 0;
                    loadList();
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        if (btnReset != null) {
            btnReset.setOnClickListener(v1 -> {
                try {
                    selectedStartTime = "";
                    selectedEndTime = "";
                    if (tvStart != null) tvStart.setText("开始时间");
                    if (tvEnd != null) tvEnd.setText("结束时间");
                    if (spinnerStatus != null) spinnerStatus.setSelection(0, false);
                    currentPage = 0;
                    loadList();
                    if (swipe != null) swipe.setNoMoreData(false);
                } catch (Exception ignored) {}
            });
        }
        if (rlStart != null) rlStart.setOnClickListener(v12 -> showStartPicker());
        if (rlEnd != null) rlEnd.setOnClickListener(v13 -> showEndPicker());
        if (rlStart != null) rlStart.setOnLongClickListener(v14 -> {
            selectedStartTime = "";
            if (tvStart != null) tvStart.setText("开始时间");
            loadList();
            return true;
        });
        if (rlEnd != null) rlEnd.setOnLongClickListener(v15 -> {
            selectedEndTime = "";
            if (tvEnd != null) tvEnd.setText("结束时间");
            loadList();
            return true;
        });

        if (swipe != null) {
            swipe.setEnableRefresh(true);
            swipe.setEnableLoadMore(true);
            swipe.setOnRefreshListener(layout -> {
                loadList();
            });
            swipe.setOnLoadMoreListener(layout -> {
                boolean noMore = ((currentPage + 1) * pageSize) >= allFiltered.size();
                if (noMore) {
                    layout.finishLoadMore(true);
                    layout.setEnableLoadMore(false);
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
                List<MaintenanceInfo> filtered = filterByStatusAndTime(finalList);
                allFiltered.clear();
                allFiltered.addAll(filtered);
                currentPage = 0;
                currentDisplay.clear();
                submitCurrentPage();
                autoDispatchDue(filtered);
                if (swipe != null) {
                    swipe.finishRefresh(true);
                    swipe.setNoMoreData(false);
                    swipe.setEnableLoadMore(allFiltered.size() > pageSize);
                }
            });
        });
    }

    private List<MaintenanceInfo> filterByStatusAndTime(List<MaintenanceInfo> source) {
        List<MaintenanceInfo> out = new ArrayList<>();
        long now = System.currentTimeMillis();
        long startMs = parseMillis(selectedStartTime);
        long endMs = parseMillis(selectedEndTime);
        for (MaintenanceInfo mi : (source != null ? source : new ArrayList<MaintenanceInfo>())) {
            if (mi == null) continue;
            String ct = mi.getCreateTime();
            if (ct == null || ct.trim().isEmpty()) continue;
            long tm = parseMillis(ct);
            if (tm < 0) continue;
            if (tm > now) continue;
            if (startMs > 0 && tm < startMs) continue;
            if (endMs > 0 && tm > endMs) continue;
            if (filterStatus == -1) {
                out.add(mi);
            } else if (filterStatus == 0) {
                if (mi.getStatus() == 0) out.add(mi);
            } else if (filterStatus == 1) {
                if (mi.getStatus() == 1) out.add(mi);
            }
        }
        return out;
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
            try {
                int size = allFiltered != null ? allFiltered.size() : 0;
                if (size <= 0) {
                    loadingMore = false;
                    if (swipe != null) {
                        swipe.finishLoadMore(true);
                        swipe.setEnableLoadMore(false);
                    }
                    return;
                }
                int safeNextStart = (currentPage + 1) * pageSize;
                if (safeNextStart >= size) {
                    loadingMore = false;
                    if (swipe != null) {
                        swipe.finishLoadMore(true);
                        swipe.setEnableLoadMore(false);
                    }
                    return;
                }
                int end = Math.min(size, safeNextStart + pageSize);
                if (end <= safeNextStart) {
                    loadingMore = false;
                    if (swipe != null) {
                        swipe.finishLoadMore(true);
                        swipe.setEnableLoadMore(false);
                    }
                    return;
                }
                List<MaintenanceInfo> more = new ArrayList<>(allFiltered.subList(safeNextStart, end));
                currentDisplay.addAll(more);
                adapter.submitList(new ArrayList<>(currentDisplay));
                adapter.notifyDataSetChanged();
                currentPage++;
            } catch (Exception ignored) {
                if (swipe != null) swipe.finishLoadMore(false);
            } finally {
                loadingMore = false;
            }
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
            if (mi.getSentFlag() == 1) continue;
            String dev = mi.getTerminalId();
            if (TextUtils.isEmpty(dev)) continue;
            try {
                int h = SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
                int m = SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
                int mins = Math.max(0, Math.min(1440, h * 60 + m));
                int interval = SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
//                helper.sendDownlink8001(
//                        dev,
//                        0,
//                        1,
//                        0,
//                        0,
//                        0,
//                        (1 << 1),
//                        Math.max(3, Math.min(1440, interval)),
//                        1,
//                        new int[]{mins},
//                        true
//                );
                db.updateMaintenanceSent(mi.getId(), nowStr());
                EventBus.getDefault().post(new com.lora.cn.event.TerminalRefreshEvent("maintenance_updated"));
            } catch (Exception ignored) {}
        }
    }

    private void showStartPicker() {
        android.app.DatePickerDialog dp = new android.app.DatePickerDialog(requireContext());
        dp.setOnDateSetListener((view, year, month, day) -> {
            android.app.TimePickerDialog tp = new android.app.TimePickerDialog(requireContext(), (v, hour, minute) -> {
                java.util.Calendar c = java.util.Calendar.getInstance();
                c.set(java.util.Calendar.YEAR, year);
                c.set(java.util.Calendar.MONTH, month);
                c.set(java.util.Calendar.DAY_OF_MONTH, day);
                c.set(java.util.Calendar.HOUR_OF_DAY, hour);
                c.set(java.util.Calendar.MINUTE, minute);
                c.set(java.util.Calendar.SECOND, 0);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                selectedStartTime = sdf.format(c.getTime());
                if (tvStart != null) tvStart.setText(selectedStartTime);
                currentPage = 0;
                loadList();
            }, 0, 0, true);
            tp.show();
        });
        dp.show();
    }

    private void showEndPicker() {
        android.app.DatePickerDialog dp = new android.app.DatePickerDialog(requireContext());
        dp.setOnDateSetListener((view, year, month, day) -> {
            android.app.TimePickerDialog tp = new android.app.TimePickerDialog(requireContext(), (v, hour, minute) -> {
                java.util.Calendar c = java.util.Calendar.getInstance();
                c.set(java.util.Calendar.YEAR, year);
                c.set(java.util.Calendar.MONTH, month);
                c.set(java.util.Calendar.DAY_OF_MONTH, day);
                c.set(java.util.Calendar.HOUR_OF_DAY, hour);
                c.set(java.util.Calendar.MINUTE, minute);
                c.set(java.util.Calendar.SECOND, 59);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                selectedEndTime = sdf.format(c.getTime());
                if (tvEnd != null) tvEnd.setText(selectedEndTime);
                currentPage = 0;
                loadList();
            }, 23, 59, true);
            tp.show();
        });
        dp.show();
    }

    private long parseMillis(String s) {
        if (s == null || s.trim().isEmpty()) return -1L;
        String raw = s.trim();
        java.util.List<java.text.SimpleDateFormat> formats = new java.util.ArrayList<>();
        formats.add(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()));
        formats.add(new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()));
        for (java.text.SimpleDateFormat f : formats) {
            try {
                java.util.Date d = f.parse(raw);
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
        }
        return -1L;
    }

    private void showConfirmDialog(MaintenanceInfo item) {
        if (item == null) return;
        EditText et = new EditText(requireContext());
        et.setMinLines(3);
        et.setHint("请输入备注");
        try {
            et.setText("确认维护");
            et.setSelection(et.getText().length());
        } catch (Exception ignored) {}
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("确认维护")
                .setView(et)
                .setPositiveButton("确认", (d, w) -> {
                    String content = et.getText() != null ? et.getText().toString().trim() : "";
                    if (ioExecutor == null || mainHandler == null) return;
                    ioExecutor.execute(() -> {
                        int r;
                        String devId = item.getTerminalId();
                        try {
                            r = db.updateMaintenanceHandled(item.getId(), currentUserId, currentUserName, nowStr(), content);
                        } catch (Exception e) {
                            r = 0;
                        }
                        try {
                            String c = item.getContent();
                            if (r > 0 && c != null && ("主动维护".equals(c) || c.startsWith("设备维护："))) {
                                db.setTerminalMaintenanceClearPending(devId, true);
                            }
                        } catch (Exception ignored) {}
                        if (r > 0 && devId != null && devId.length() > 0) {
                            try {
                                com.lora.cn.database.dao.TerminalDao tdao = new com.lora.cn.database.dao.TerminalDao(db);
                                com.lora.cn.ui.model.Terminal t = tdao.getTerminalByDeviceId(devId);
                                int dep = t != null ? (int) Math.max(0, Math.min(255, t.getDepartmentId())) : 0;
                                int cart = t != null ? (int) Math.max(0, Math.min(255, t.getRoomId())) : 0;
                                MainActivity a = (MainActivity) getActivity();
                                com.lora.cn.network.MqttPacketsClient client = a != null ? a.getMqttClient() : null;
                                if (client == null) client = com.lora.cn.network.MqttPacketsClient.getShared();
                                DownlinkMessageHelper helper = new DownlinkMessageHelper(client);
                                int h = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_hour", 7);
                                int m = com.blankj.utilcode.util.SPUtils.getInstance().getInt("inventory_schedule_minute", 0);
                                int mins = Math.max(0, Math.min(1440, h * 60 + m));
                                int interval = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
//                                helper.sendDownlink8001(
//                                        devId,
//                                        1,
//                                        1,
//                                        dep,
//                                        cart,
//                                        0,
//                                        (1 << 2),
//                                        Math.max(3, Math.min(1440, interval)),
//                                        1,
//                                        new int[]{mins},
//                                        true
//                                );
                            } catch (Exception ignored) {}
                        }
                        int finalR = r;
                        mainHandler.post(() -> {
                            if (!isAdded()) return;
                            if (finalR > 0) {
                                Toast.makeText(requireContext(), "已确认维护", Toast.LENGTH_SHORT).show();
                                try {
                                    android.app.Activity a = getActivity();
//                                    if (a instanceof com.lora.cn.ui.activity.MainActivity) {
//                                        ((com.lora.cn.ui.activity.MainActivity) a).goHome();
//                                    } else
                                    {
                                        loadList();
                                    }
                                } catch (Exception ignored2) {}
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

    private void showRemarkDialog(MaintenanceInfo item) {
        if (item == null) return;
        String remark = item.getHandleRemark();
        new AlertDialog.Builder(requireContext())
                .setTitle("备注")
                .setMessage(TextUtils.isEmpty(remark) ? "暂无备注" : remark)
                .setPositiveButton("确定", null)
                .create()
                .show();
    }

    private String nowStr() {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}
