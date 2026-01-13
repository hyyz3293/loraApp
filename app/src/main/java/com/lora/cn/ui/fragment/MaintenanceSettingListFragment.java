package com.lora.cn.ui.fragment;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.ui.adapter.MaintenanceInfoDetailAdapter;
import com.lora.cn.ui.model.MaintenanceInfo;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MaintenanceSettingListFragment extends Fragment {

    private static final String ARG_TERMINAL_ID = "arg_terminal_id";

    private RecyclerView rv;
    private MaintenanceInfoDetailAdapter adapter;
    private DatabaseHelper db;
    private long currentUserId = -1;
    private String currentUserName = "";
    private String terminalId;
    private ExecutorService ioExecutor;
    private Handler mainHandler;
    private final AtomicInteger loadSeq = new AtomicInteger(0);
    private final Runnable dueRefreshRunnable = () -> {
        try { loadList(); } catch (Exception ignored) {}
    };

    public static MaintenanceSettingListFragment newInstance(String terminalId) {
        MaintenanceSettingListFragment f = new MaintenanceSettingListFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TERMINAL_ID, terminalId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        terminalId = getArguments() != null ? getArguments().getString(ARG_TERMINAL_ID, null) : null;
        View v = inflater.inflate(R.layout.fragment_maintenance_list, container, false);
        rv = v.findViewById(R.id.rv_maintenance);
        View btnAdd = v.findViewById(R.id.btn_add_maintenance);
        View btnBack = v.findViewById(R.id.back);

        db = DatabaseHelper.getInstance(requireContext());
        currentUserId = SPUtils.getInstance().getLong("current_user_id", -1);
        currentUserName = SPUtils.getInstance().getString("current_user_name", "");
        if (ioExecutor == null) ioExecutor = Executors.newSingleThreadExecutor();
        if (mainHandler == null) mainHandler = new Handler(Looper.getMainLooper());

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MaintenanceInfoDetailAdapter(MaintenanceInfoDetailAdapter.Mode.SETTING);
        adapter.setOnConfirmClickListener(this::showConfirmDialog);
        adapter.setOnViewClickListener(this::showViewDialog);
        adapter.setOnEditClickListener(this::showEditDialog);
        adapter.setOnDeleteClickListener(this::showDeleteDialog);
        rv.setAdapter(adapter);

        if (btnBack != null) {
            btnBack.setOnClickListener(view -> {
                try {
                    getParentFragmentManager().popBackStack();
                } catch (Exception ignored) {}
            });
        }
        if (btnAdd != null) {
            btnAdd.setOnClickListener(view -> {
                if (!TextUtils.isEmpty(terminalId)) showAddDialogForTerminal(terminalId);
            });
        }

        loadList();
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (mainHandler != null) mainHandler.removeCallbacks(dueRefreshRunnable);
            if (ioExecutor != null) ioExecutor.shutdownNow();
        } catch (Exception ignored) {}
        ioExecutor = null;
        mainHandler = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
        } catch (Exception ignored) {}
    }

    @Override
    public void onStop() {
        try {
            if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        } catch (Exception ignored) {}
        try {
            if (mainHandler != null) mainHandler.removeCallbacks(dueRefreshRunnable);
        } catch (Exception ignored) {}
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadList();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTerminalRefreshEvent(com.lora.cn.event.TerminalRefreshEvent event) {
        if (event == null) return;
        String msg = null;
        try { msg = event.getMessage(); } catch (Exception ignored) {}
        if (msg == null || !msg.startsWith("维护刷新")) return;
        if (TextUtils.isEmpty(terminalId)) return;
        String target = null;
        int idx = msg.indexOf(':');
        if (idx >= 0 && idx + 1 < msg.length()) target = msg.substring(idx + 1).trim();
        if (!TextUtils.isEmpty(target) && !terminalId.equalsIgnoreCase(target)) return;
        loadList();
    }

    private void loadList() {
        if (ioExecutor == null || mainHandler == null) return;
        int token = loadSeq.incrementAndGet();
        ioExecutor.execute(() -> {
            List<MaintenanceInfo> list;
            long nextDueMs = -1L;
            try {
                if (TextUtils.isEmpty(terminalId)) list = new ArrayList<>();
                else list = db.getMaintenanceRecordsByTerminal(terminalId, currentUserId);
                if (list == null) list = new ArrayList<>();
                List<MaintenanceInfo> filtered = new ArrayList<>();
                for (MaintenanceInfo mi : list) {
                    String c = mi != null ? mi.getContent() : null;
                    boolean isAuto = "主动维护".equals(c);
                    if (!isAuto) filtered.add(mi);
                }
                long now = System.currentTimeMillis();
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault());
                List<MaintenanceInfo> futureOnly = new ArrayList<>();
                long minFutureMs = Long.MAX_VALUE;
                for (MaintenanceInfo mi : filtered) {
                    String ct = mi != null ? mi.getCreateTime() : null;
                    if (ct == null || ct.trim().isEmpty()) continue;
                    try {
                        java.util.Date dt = sdf.parse(ct.trim());
                        if (dt != null && dt.getTime() > now && mi.getStatus() == 0) {
                            futureOnly.add(mi);
                            if (dt.getTime() < minFutureMs) minFutureMs = dt.getTime();
                        }
                    } catch (Exception ignored) {}
                }
                list = futureOnly;
                if (minFutureMs != Long.MAX_VALUE) nextDueMs = minFutureMs;
            } catch (Exception e) {
                list = null;
            }
            List<MaintenanceInfo> finalList = list;
            long finalNextDueMs = nextDueMs;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (token != loadSeq.get()) return;
                if (finalList == null) {
                    Toast.makeText(requireContext(), "加载维护列表失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                adapter.submitList(finalList);
                adapter.notifyDataSetChanged();
                scheduleNextDueRefresh(finalNextDueMs);
            });
        });
    }

    private void scheduleNextDueRefresh(long dueAtMs) {
        if (mainHandler == null) return;
        try { mainHandler.removeCallbacks(dueRefreshRunnable); } catch (Exception ignored) {}
        if (dueAtMs <= 0) return;
        long now = System.currentTimeMillis();
        long delay = Math.max(0L, dueAtMs - now) + 300L;
        try { mainHandler.postDelayed(dueRefreshRunnable, delay); } catch (Exception ignored) {}
    }

    private void showAddDialogForTerminal(String terminalId) {
        if (ioExecutor == null || mainHandler == null) return;
        ioExecutor.execute(() -> {
            com.lora.cn.ui.model.Terminal t = null;
            try {
                com.lora.cn.database.dao.TerminalDao dao = new com.lora.cn.database.dao.TerminalDao(db);
                t = dao.getTerminalByDeviceId(terminalId);
            } catch (Exception ignored) {}
            com.lora.cn.ui.model.Terminal finalT = t;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (finalT == null) {
                    Toast.makeText(requireContext(), "未找到终端", Toast.LENGTH_SHORT).show();
                    return;
                }
                showAddDialogForTerminalInternal(finalT);
            });
        });
    }

    private void showAddDialogForTerminalInternal(com.lora.cn.ui.model.Terminal t) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_maintenance, null);
        Spinner spinner = view.findViewById(R.id.spinner_terminal);
        View terminalLabel = view.findViewById(R.id.tv_select_terminal_label);
        EditText etContent = view.findViewById(R.id.et_content);
        TextView tvTime = view.findViewById(R.id.tv_maintenance_time);

        if (terminalLabel != null) terminalLabel.setVisibility(View.GONE);
        if (spinner != null) spinner.setVisibility(View.GONE);

        if (etContent != null && TextUtils.isEmpty(etContent.getText())) {
            etContent.setText("");
            etContent.setSelection(etContent.getText().length());
        }
        if (tvTime != null) {
            tvTime.setText(nowStr());
            tvTime.setOnClickListener(v -> pickDateTimeInto(tvTime));
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("新增维护")
                .setView(view)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .create();

        dialog.setOnShowListener(dlg -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String content = etContent != null ? etContent.getText().toString().trim() : "";
            if (TextUtils.isEmpty(content)) {
                Toast.makeText(requireContext(), "请输入维护内容", Toast.LENGTH_SHORT).show();
                return;
            }

            MaintenanceInfo mi = new MaintenanceInfo();
            mi.setTerminalId(t.getTerminalId());
            mi.setTerminalName(t.getTerminalName());
            String group = t.getDepartment();
            if (TextUtils.isEmpty(group)) group = t.getLocation();
            mi.setTerminalGroup(group);
            mi.setStatus(0);
            mi.setContent(content);
            mi.setCreateUserId(currentUserId);
            mi.setCreateUser(currentUserName);
            String createTime = tvTime != null && tvTime.getText() != null ? tvTime.getText().toString().trim() : "";
            mi.setCreateTime(TextUtils.isEmpty(createTime) ? nowStr() : createTime);
            mi.setHandleUserId(0);
            mi.setHandleUser("");
            mi.setHandleTime("");
            if (ioExecutor == null || mainHandler == null) return;
            ioExecutor.execute(() -> {
                long r;
                try {
                    r = db.addMaintenanceRecord(mi);
                } catch (Exception e) {
                    r = -1;
                }
                long finalR = r;
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    if (finalR > 0) {
                        dialog.dismiss();
                        loadList();
                        Toast.makeText(requireContext(), "已添加", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "添加失败", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }));
        dialog.show();
    }

    private void showConfirmDialog(MaintenanceInfo item) {
        if (item == null) return;
        EditText et = new EditText(requireContext());
        et.setMinLines(3);
        et.setText(item.getContent() == null ? "" : item.getContent());
        et.setSelection(et.getText().length());
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
                        try {
                            String c = item.getContent();
                            if (r > 0 && c != null && ("主动维护".equals(c) || c.startsWith("设备维护："))) {
                                db.setTerminalMaintenanceClearPending(item.getTerminalId(), true);
                            }
                        } catch (Exception ignored) {}
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
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_maintenance, null);
        Spinner spinner = view.findViewById(R.id.spinner_terminal);
        View terminalLabel = view.findViewById(R.id.tv_select_terminal_label);
        EditText etContent = view.findViewById(R.id.et_content);
        TextView tvTime = view.findViewById(R.id.tv_maintenance_time);

        if (terminalLabel != null) terminalLabel.setVisibility(View.GONE);
        if (spinner != null) spinner.setVisibility(View.GONE);

        if (etContent != null) {
            etContent.setText(item.getContent() == null ? "" : item.getContent());
            etContent.setSelection(etContent.getText().length());
        }
        if (tvTime != null) {
            String ct = item.getCreateTime();
            tvTime.setText(TextUtils.isEmpty(ct) ? nowStr() : ct);
            tvTime.setOnClickListener(v -> pickDateTimeInto(tvTime));
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("编辑维护")
                .setView(view)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.setOnShowListener(dlg -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String content = etContent != null && etContent.getText() != null ? etContent.getText().toString().trim() : "";
            if (TextUtils.isEmpty(content)) {
                Toast.makeText(requireContext(), "请输入维护内容", Toast.LENGTH_SHORT).show();
                return;
            }
            String createTime = tvTime != null && tvTime.getText() != null ? tvTime.getText().toString().trim() : "";
            if (TextUtils.isEmpty(createTime)) {
                createTime = nowStr();
            }
            final String createTimeFinal = createTime;
            if (ioExecutor == null || mainHandler == null) return;
            ioExecutor.execute(() -> {
                int r;
                try {
                    r = db.updateMaintenanceCreateTimeAndContent(item.getId(), content, createTimeFinal);
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
        String msg = item.getContent() == null ? "" : item.getContent();
        new AlertDialog.Builder(requireContext())
                .setTitle("维护内容")
                .setMessage(msg)
                .setPositiveButton("确定", null)
                .create()
                .show();
    }

    private String nowStr() {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private void pickDateTimeInto(TextView tv) {
        if (tv == null) return;
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar cal2 = Calendar.getInstance();
            cal2.set(Calendar.YEAR, year);
            cal2.set(Calendar.MONTH, month);
            cal2.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            TimePickerDialog tp = new TimePickerDialog(requireContext(), (tpView, hourOfDay, minute) -> {
                cal2.set(Calendar.HOUR_OF_DAY, hourOfDay);
                cal2.set(Calendar.MINUTE, minute);
                cal2.set(Calendar.SECOND, 0);
                String s = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(cal2.getTime());
                tv.setText(s);
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
            tp.show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dp.show();
    }
}
