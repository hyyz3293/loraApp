package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.User;
import com.lora.cn.ui.adapter.TerminalAdapter;
import com.lora.cn.ui.adapter.TerminalStatusAdapter;
import com.lora.cn.ui.constants.TerminalStatusConstants;
import com.lora.cn.ui.model.Terminal;
import com.lora.cn.ui.model.TerminalStatus;

import java.util.ArrayList;
import java.util.List;

public class TerminalStatusListFragment extends Fragment {

    private RecyclerView rvTerminalStatus;
    private RecyclerView terminalRecycle;
    private TerminalStatusAdapter terminalStatusAdapter;
    private TerminalAdapter adapter;
    private TextView addTerminalBtn;
    private TextView tvGroupCategory;
    private View btnBack;
    private TextView toolbarTitle;
//    private TextView toolbarLeftTitle;

    private final List<Terminal> allDisplayTerminals = new ArrayList<>();
    private String statusFilterTitle = null;
    private String searchKeyword = "";
    private android.widget.Spinner spinnerTs;
    private android.widget.ImageView sxLeft;
    private android.widget.ImageView sxRight;
    private int pageSize = 20;
    private int currentPage = 0;
    private List<Terminal> filteredPageBase = new ArrayList<>();
    private java.util.Set<String> pendingAbnormalIds = new java.util.HashSet<>();
    private java.util.Set<String> pendingOfflineIds = new java.util.HashSet<>();

    private DatabaseManager databaseManager;
    private int currentUserRoleId = -1;
    private android.os.Handler autoRefreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private java.util.concurrent.ExecutorService ioExecutor;
    private android.os.Handler mainHandler;
    private final java.util.concurrent.atomic.AtomicInteger loadSeq = new java.util.concurrent.atomic.AtomicInteger();
    private boolean firstVisible = true;
    private final Runnable resumeRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            try { refreshTerminals(); } catch (Exception ignored) {}
        }
    };
    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                refreshTerminals();
            } finally {
                autoRefreshHandler.postDelayed(this, 120000);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status_terminal_list, container, false);
        if (getArguments() != null) {
            statusFilterTitle = getArguments().getString("status_filter_title", null);
        }
        databaseManager = DatabaseManager.getInstance(requireContext());
        if (ioExecutor == null) ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
        if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        long userId = SPUtils.getInstance().getLong("current_user_id", -1);
        if (userId != -1) {
            User user = databaseManager.getUserById(userId);
            if (user != null) currentUserRoleId = (int) user.getRoleId();
        }

        initViews(view);
        initStatusBar();


        if (hasPermission("terminal_list")) {
            //initStatusBar();
            initTerminalList();
        } else {
            Toast.makeText(requireContext(), "您没有查看终端列表的权限", Toast.LENGTH_SHORT).show();
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        try {
            if (ioExecutor != null) ioExecutor.shutdownNow();
            autoRefreshHandler.removeCallbacks(resumeRefreshRunnable);
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        } catch (Exception ignored) {}
        ioExecutor = null;
        super.onDestroyView();
    }

    private void initViews(View view) {
        rvTerminalStatus = view.findViewById(R.id.rv_terminal_status);
        terminalRecycle = view.findViewById(R.id.terminal_recycle);
        addTerminalBtn = view.findViewById(R.id.add_terminal);
        tvGroupCategory = view.findViewById(R.id.tv_group_category);
        spinnerTs = view.findViewById(R.id.spinner_ts);
        sxLeft = view.findViewById(R.id.sx_left);
        sxRight = view.findViewById(R.id.sx_right);

        btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            if (btnBack instanceof android.widget.ImageView) {
                ((android.widget.ImageView) btnBack).setImageResource(android.R.drawable.ic_menu_revert);
            }
            btnBack.setOnClickListener(v -> {
                if (getActivity() instanceof com.lora.cn.ui.activity.MainActivity) {
                    ((com.lora.cn.ui.activity.MainActivity) getActivity()).goHome();
                } else {
                    androidx.appcompat.app.AppCompatActivity a = (androidx.appcompat.app.AppCompatActivity) getActivity();
                    if (a != null) a.getSupportFragmentManager().popBackStack();
                }
            });
        }
//
        toolbarTitle = view.findViewById(R.id.status_terminal_tile);
        ImageView statusIcon = view.findViewById(R.id.status_terminal_icon);
//        toolbarLeftTitle = view.findViewById(R.id.toolbar_left_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(statusFilterTitle != null ? statusFilterTitle : "");
        }
        if (statusIcon != null) {
            int res = 0;
            if (statusFilterTitle != null) {
                if (TerminalStatusConstants.STATUS_ONLINE.equals(statusFilterTitle)) res = R.drawable.ic_xh_signal_4;
                else if (TerminalStatusConstants.STATUS_ABNORMAL_LOST.equals(statusFilterTitle)) res = R.mipmap.ic_ds;
                else if (TerminalStatusConstants.STATUS_NORMAL_TAKEN.equals(statusFilterTitle)) res = R.mipmap.ic_blue_right;
                else if (TerminalStatusConstants.STATUS_OFFLINE.equals(statusFilterTitle)) res = R.mipmap.ic_xh_no;
                else if (TerminalStatusConstants.STATUS_LOW_BATTERY.equals(statusFilterTitle)) res = R.mipmap.ic_red_sd;
            }
            if (res != 0) {
                statusIcon.setImageResource(res);
                statusIcon.setVisibility(View.VISIBLE);
            } else {
                statusIcon.setVisibility(View.GONE);
            }
        }



        EditText searchEditText = view.findViewById(R.id.et_search);
        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(android.text.Editable s) {
                    searchKeyword = s != null ? s.toString().trim() : "";
                    applyCurrentFilters();
                }
            });
        }
        android.widget.Spinner spinnerFilter = view.findViewById(R.id.spinner_filter);
        if (spinnerFilter != null) {
            spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                    applyCurrentFilters();
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }
        if (spinnerTs != null) {
            spinnerTs.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                    Object item = parent.getItemAtPosition(position);
                    int newSize = pageSize;
                    if (item != null) {
                        String s = String.valueOf(item).replaceAll("[^0-9]", "");
                        if (!s.isEmpty()) {
                            try { newSize = Integer.parseInt(s); } catch (Exception ignored) {}
                        }
                    }
                    pageSize = newSize > 0 ? newSize : 20;
                    currentPage = 0;
                    submitCurrentPage();
                    updatePaginationControls();
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }
        if (sxLeft != null) {
            sxLeft.setOnClickListener(v -> {
                if (currentPage > 0) {
                    currentPage--;
                    submitCurrentPage();
                    updatePaginationControls();
                }
            });
        }
        if (sxRight != null) {
            sxRight.setOnClickListener(v -> {
                int total = filteredPageBase != null ? filteredPageBase.size() : 0;
                if ((currentPage + 1) * pageSize < total) {
                    currentPage++;
                    submitCurrentPage();
                    updatePaginationControls();
                }
            });
        }

        if (terminalRecycle != null) {
            androidx.recyclerview.widget.GridLayoutManager terminalLayoutManager = new androidx.recyclerview.widget.GridLayoutManager(getContext(), 4);
            terminalRecycle.setLayoutManager(terminalLayoutManager);
            adapter = new TerminalAdapter();
            try {
                terminalRecycle.setHasFixedSize(true);
                terminalRecycle.setItemAnimator(null);
                terminalRecycle.setNestedScrollingEnabled(false);
            } catch (Throwable ignored) {}
            terminalRecycle.setAdapter(adapter);
            adapter.setOnItemClickListener((adapter1, v1, position) -> {
                if (hasPermission("terminal_detail")) {
                    Terminal terminal = (Terminal) adapter.getItem(position);
                    onTerminalClick(position, terminal);
                } else {
                    Toast.makeText(requireContext(), "您没有查看终端详情的权限", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void initStatusBar() {
        if (rvTerminalStatus == null) {
            return;
        }
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 6);
        rvTerminalStatus.setLayoutManager(gridLayoutManager);
        terminalStatusAdapter = new TerminalStatusAdapter();
        try {
            rvTerminalStatus.setHasFixedSize(true);
            rvTerminalStatus.setItemAnimator(null);
            rvTerminalStatus.setNestedScrollingEnabled(false);
        } catch (Throwable ignored) {}
        rvTerminalStatus.setAdapter(terminalStatusAdapter);
        terminalStatusAdapter.setOnItemClickListener((adapter1, view1, position1) -> {
            TerminalStatus item = (TerminalStatus) terminalStatusAdapter.getItem(position1);
            if (item == null) return;
            TerminalStatusListFragment fragment = TerminalStatusListFragment.newInstance(item.getTitle());
            androidx.appcompat.app.AppCompatActivity a = (androidx.appcompat.app.AppCompatActivity) getActivity();
            if (a != null) {
                a.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_device_list_container, fragment)
                        .addToBackStack("terminal_status_filter")
                        .commit();
            }
        });
    }

    private void initTerminalList() {
        refreshTerminals();
    }

    private void loadTerminals() {
        refreshTerminals();
    }

    /** 将数据库终端数据转换为UI显示格式（与首页一致） */
    private List<Terminal> convertToDisplayTerminals(List<Terminal> dbTerminals, java.util.Map<Long, String> categoryNameById) {
        List<Terminal> displayTerminals = new ArrayList<>();
        for (Terminal dbTerminal : dbTerminals) {
            Terminal displayTerminal = new Terminal();
            displayTerminal.setId(dbTerminal.getId());
            displayTerminal.setTerminalId(dbTerminal.getTerminalId());
            displayTerminal.setTerminalName(dbTerminal.getTerminalName());
            displayTerminal.setName(dbTerminal.getTerminalName());
            displayTerminal.setDepartmentId(dbTerminal.getDepartmentId());
            displayTerminal.setRoomId(dbTerminal.getRoomId());
            displayTerminal.setNursingGroupId(dbTerminal.getNursingGroupId());
            displayTerminal.setOtherId(dbTerminal.getOtherId());
            displayTerminal.setExtension(dbTerminal.getExtension());
            String dept = dbTerminal.getDepartment();
            if (android.text.TextUtils.isEmpty(dept) && dbTerminal.getDepartmentId() > 0) {
                try {
                    if (categoryNameById != null) dept = categoryNameById.get(dbTerminal.getDepartmentId());
                } catch (Exception ignored) {}
            }
            String room = dbTerminal.getLocation();
            if (android.text.TextUtils.isEmpty(room) && dbTerminal.getRoomId() > 0) {
                try {
                    if (categoryNameById != null) room = categoryNameById.get(dbTerminal.getRoomId());
                } catch (Exception ignored) {}
            }
            displayTerminal.setDepartment(dept);
            displayTerminal.setLocation(room);
            displayTerminal.setStatus(dbTerminal.getStatus());
            displayTerminal.setSignalStrength(dbTerminal.getSignalStrength());
            displayTerminal.setFavorite(dbTerminal.isFavorite());
            int statusIcon = getStatusIcon(dbTerminal.getStatus());
            displayTerminal.setStatusIconResId(statusIcon);
            displayTerminal.setStatusText(TerminalStatusConstants.codeToText(dbTerminal.getStatus()));
            displayTerminal.setBatteryLevel(dbTerminal.getBatteryLevel());
            displayTerminal.setBatteryText(dbTerminal.getBatteryLevel() + "%");
            displayTerminal.setImportant(dbTerminal.isFavorite());
            displayTerminal.setGroupIdsText(dbTerminal.getGroupIdsText());
            displayTerminal.setGroupNamesText(dbTerminal.getGroupNamesText());

            displayTerminals.add(displayTerminal);
        }
        return displayTerminals;
    }

    private void refreshTerminals() {
        if (ioExecutor == null) return;
        if (mainHandler == null) mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        final android.os.Handler handler = mainHandler;
        if (handler == null) return;
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        android.content.Context appCtx = ctx.getApplicationContext();
        int token = loadSeq.incrementAndGet();
        ioExecutor.execute(() -> {
            List<Terminal> terminals = null;
            try {
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(appCtx);
                try { dbHelper.checkAndLogOfflineDevices(); } catch (Exception ignored) {}
                terminals = dbHelper.getAllTerminals();
            } catch (Exception ignored) {}

            java.util.Map<Long, String> categoryNameById = new java.util.HashMap<>();
            try {
                DatabaseManager dm = DatabaseManager.getInstance(appCtx);
                List<com.lora.cn.database.entity.Category> categories = dm.getAllCategories();
                if (categories != null) {
                    for (com.lora.cn.database.entity.Category c : categories) {
                        if (c == null) continue;
                        categoryNameById.put(c.getCategoryId(), c.getCategoryName());
                    }
                }
            } catch (Exception ignored) {}

            List<Terminal> display = new ArrayList<>();
            if (terminals != null && !terminals.isEmpty()) {
                try {
                    display = convertToDisplayTerminals(terminals, categoryNameById);
                } catch (Exception ignored) {}
            }
            java.util.Set<String> pa = new java.util.HashSet<>();
            java.util.Set<String> po = new java.util.HashSet<>();
            try {
                com.lora.cn.database.DatabaseHelper db = com.lora.cn.database.DatabaseHelper.getInstance(appCtx);
                java.util.List<com.lora.cn.ui.model.LogInfo> logs = db.getAllLogsBoundToTerminals();
                java.util.Map<String, com.lora.cn.ui.model.LogInfo> latestAbnormal = new java.util.HashMap<>();
                java.util.Map<String, com.lora.cn.ui.model.LogInfo> latestOffline = new java.util.HashMap<>();
                java.util.Map<String, String> lastHandledTime = new java.util.HashMap<>();
                if (logs != null) {
                    for (com.lora.cn.ui.model.LogInfo li : logs) {
                        if (li == null) continue;
                        String tid = li.getTerminalId();
                        int s = li.getStatusCode();
                        if (tid == null || tid.isEmpty()) continue;
                        if (s == com.lora.cn.ui.constants.LogStatus.DEVICE_LOST.code) {
                            com.lora.cn.ui.model.LogInfo prev = latestAbnormal.get(tid);
                            String pt = prev != null ? prev.getCreateTime() : null;
                            String ct = li.getCreateTime();
                            if (prev == null || (ct != null && pt != null && ct.compareTo(pt) >= 0) || (ct != null && (pt == null || pt.trim().isEmpty()))) {
                                latestAbnormal.put(tid, li);
                            }
                        } else if (s == com.lora.cn.ui.constants.LogStatus.DEVICE_OFFLINE.code) {
                            com.lora.cn.ui.model.LogInfo prev2 = latestOffline.get(tid);
                            String pt2 = prev2 != null ? prev2.getCreateTime() : null;
                            String ct2 = li.getCreateTime();
                            if (prev2 == null || (ct2 != null && pt2 != null && ct2.compareTo(pt2) >= 0) || (ct2 != null && (pt2 == null || pt2.trim().isEmpty()))) {
                                latestOffline.put(tid, li);
                            }
                        }
                        String hu2 = li.getHandleUser();
                        String ht2 = li.getHandleTime();
                        if ((hu2 != null && !hu2.trim().isEmpty()) || (ht2 != null && !ht2.trim().isEmpty())) {
                            String ctH2 = li.getCreateTime();
                            if (ctH2 != null && !ctH2.trim().isEmpty()) {
                                String prevH2 = lastHandledTime.get(tid);
                                if (prevH2 == null || prevH2.trim().isEmpty() || ctH2.compareTo(prevH2) >= 0) lastHandledTime.put(tid, ctH2);
                            }
                        }
                    }
                    for (java.util.Map.Entry<String, com.lora.cn.ui.model.LogInfo> e : latestAbnormal.entrySet()) {
                        String tid = e.getKey();
                        com.lora.cn.ui.model.LogInfo li = e.getValue();
                        String at = li != null ? li.getCreateTime() : null;
                        String ht = lastHandledTime.get(tid);
                        boolean pending = ht == null || ht.trim().isEmpty() || (at != null && !at.trim().isEmpty() && at.compareTo(ht) > 0);
                        if (pending) pa.add(tid);
                    }
                    for (java.util.Map.Entry<String, com.lora.cn.ui.model.LogInfo> e : latestOffline.entrySet()) {
                        String tid = e.getKey();
                        com.lora.cn.ui.model.LogInfo li = e.getValue();
                        String at = li != null ? li.getCreateTime() : null;
                        String ht = lastHandledTime.get(tid);
                        boolean pending = ht == null || ht.trim().isEmpty() || (at != null && !at.trim().isEmpty() && at.compareTo(ht) > 0);
                        if (pending) po.add(tid);
                    }
                }
            } catch (Exception ignored) {}

            List<Terminal> finalDisplay = display;
            handler.post(() -> {
                if (!isAdded()) return;
                if (token != loadSeq.get()) return;
                allDisplayTerminals.clear();
                if (finalDisplay != null) allDisplayTerminals.addAll(finalDisplay);
                pendingAbnormalIds = pa;
                pendingOfflineIds = po;
                applyCurrentFilters();
            });
        });
    }

    private void onTerminalClick(int position, Terminal terminal) {
        com.lora.cn.ui.fragment.TerminalDetailFragment fragment = com.lora.cn.ui.fragment.TerminalDetailFragment.newInstance(terminal.getTerminalId());
        androidx.appcompat.app.AppCompatActivity a = (androidx.appcompat.app.AppCompatActivity) getActivity();
        if (a != null) {
            a.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_device_list_container, fragment)
                    .addToBackStack("terminal_detail")
                    .commit();
        }
    }

    private void applyCurrentFilters() {
        if (adapter == null) return;
        List<Terminal> list = new ArrayList<>(allDisplayTerminals);
        if (statusFilterTitle != null && !statusFilterTitle.isEmpty()) {
            List<Terminal> filtered = new ArrayList<>();
            for (Terminal t : list) {
                boolean match = false;
                if (TerminalStatusConstants.STATUS_IMPORTANT.equals(statusFilterTitle)) match = t.isFavorite();
                else if (TerminalStatusConstants.STATUS_ONLINE.equals(statusFilterTitle)) match = t.getStatus() == TerminalStatusConstants.CODE_ONLINE;
                else if (TerminalStatusConstants.STATUS_OFFLINE.equals(statusFilterTitle)) match = t.getStatus() == TerminalStatusConstants.CODE_OFFLINE;
                else if (TerminalStatusConstants.STATUS_NORMAL_TAKEN.equals(statusFilterTitle)) match = t.getStatus() == TerminalStatusConstants.CODE_NORMAL_TAKEN;
                else if (TerminalStatusConstants.STATUS_ABNORMAL_LOST.equals(statusFilterTitle)) {
                    String tid = t.getTerminalId();
                    match = t.getStatus() == TerminalStatusConstants.CODE_ABNORMAL_TAKEN || (tid != null && pendingAbnormalIds.contains(tid));
                }
                else if (TerminalStatusConstants.STATUS_LOW_BATTERY.equals(statusFilterTitle)) {
                    int lowTh = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
                    match = t.getStatus() != TerminalStatusConstants.CODE_OFFLINE && t.getBatteryLevel() <= lowTh;
                }
                if (match) filtered.add(t);
            }
            list = filtered;
        }
        if (searchKeyword != null && !searchKeyword.isEmpty()) {
            List<Terminal> filtered = new ArrayList<>();
            for (Terminal t : list) {
                String name = t.getTerminalName();
                if (name != null && name.contains(searchKeyword)) filtered.add(t);
            }
            list = filtered;
        }
        View root = getView();
        if (root != null) {
            android.widget.Spinner spinnerFilter = (android.widget.Spinner) root.findViewById(R.id.spinner_filter);
            if (spinnerFilter != null && spinnerFilter.getSelectedItem() != null) {
                String opt = String.valueOf(spinnerFilter.getSelectedItem());
                if (!"全部".equals(opt)) {
                    List<Terminal> filtered = new ArrayList<>();
                    for (Terminal t : list) {
                        boolean keep = true;
                        String st = TerminalStatusConstants.codeToText(t.getStatus());
                        if (opt.contains("只显示重点关注")) keep = t.isFavorite();
                        else if (opt.contains("只显示异常关注")) keep = TerminalStatusConstants.STATUS_ABNORMAL_LOST.equals(st);
                        else if (opt.contains("只显示没有信号")) keep = TerminalStatusConstants.STATUS_OFFLINE.equals(st);
                        if (keep) filtered.add(t);
                    }
                    list = filtered;
                }
            }
        }
        filteredPageBase = list;
        currentPage = 0;
        submitCurrentPage();
        updatePaginationControls();
    }

    private void submitCurrentPage() {
        if (adapter == null) return;
        int total = filteredPageBase != null ? filteredPageBase.size() : 0;
        int start = currentPage * pageSize;
        if (start < 0) start = 0;
        if (start > total) start = total;
        int end = Math.min(start + pageSize, total);
        List<Terminal> page = new ArrayList<>();
        if (start < end) page = new ArrayList<>(filteredPageBase.subList(start, end));
        adapter.submitList(page);
    }

    private void updatePaginationControls() {
        int total = filteredPageBase != null ? filteredPageBase.size() : 0;
        boolean canPrev = currentPage > 0;
        boolean canNext = (currentPage + 1) * pageSize < total;
        if (sxLeft != null) {
            sxLeft.setEnabled(canPrev);
            sxLeft.setAlpha(canPrev ? 1f : 0.4f);
        }
        if (sxRight != null) {
            sxRight.setEnabled(canNext);
            sxRight.setAlpha(canNext ? 1f : 0.4f);
        }
    }

    private boolean hasPermission(String permissionCode) {
        if (currentUserRoleId == -1) return false;
        return databaseManager.hasPermission(currentUserRoleId, permissionCode);
    }

    public static TerminalStatusListFragment newInstance(String statusFilterTitle) {
        TerminalStatusListFragment f = new TerminalStatusListFragment();
        Bundle b = new Bundle();
        b.putString("status_filter_title", statusFilterTitle);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onResume() {
        super.onResume();
        autoRefreshHandler.removeCallbacks(resumeRefreshRunnable);
        long delay = firstVisible ? 500L : 250L;
        firstVisible = false;
        autoRefreshHandler.postDelayed(resumeRefreshRunnable, delay);
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        autoRefreshHandler.postDelayed(autoRefreshRunnable, 120000);
    }

    @Override
    public void onPause() {
        super.onPause();
        autoRefreshHandler.removeCallbacks(resumeRefreshRunnable);
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    private int getStatusIcon(int statusCode) {
        switch (statusCode) {
            case TerminalStatusConstants.CODE_ONLINE:
                return R.drawable.ic_xh_signal_4;
            case TerminalStatusConstants.CODE_ABNORMAL_TAKEN:
                return R.mipmap.ic_ds;
            case TerminalStatusConstants.CODE_NORMAL_TAKEN:
                return R.mipmap.ic_blue_right;
            case TerminalStatusConstants.CODE_OFFLINE:
            default:
                return R.mipmap.ic_xh_no;
        }
    }
}
