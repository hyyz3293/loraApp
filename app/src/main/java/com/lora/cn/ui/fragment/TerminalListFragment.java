package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.LogUtils;
import com.google.gson.Gson;
import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.database.entity.User;
import com.lora.cn.ui.adapter.TerminalStatusAdapter;
import com.lora.cn.ui.adapter.TerminalAdapter;
import com.lora.cn.ui.model.TerminalStatus;
import com.lora.cn.ui.model.Terminal;
import com.lora.cn.ui.constants.TerminalStatusConstants;
import com.lora.cn.utils.LoRaProtocolParser;
import com.lora.cn.dialog.AddTerminalDialog;
import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.event.TerminalRefreshEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class TerminalListFragment extends Fragment {

    private static final String TAG = "TerminalListFragment";

    private RecyclerView rvTerminalStatus;
    private RecyclerView terminalRecycle;
    private TerminalStatusAdapter terminalStatusAdapter;
    private TerminalAdapter adapter;
    private TextView addTerminalBtn;
    private int currentStatusIndex = 0;
    // 过滤相关
    private TextView tvGroupCategory;
    private long selectedGroupId = -1; // -1: 全部分组
    private long selectedCategoryId = -1; // -1: 全部分类
    private List<Terminal> allDisplayTerminals = new ArrayList<>();
    private String statusFilterTitle = null;
    private String searchKeyword = "";
    private android.widget.Spinner spinnerTs;
    private android.widget.ImageView sxLeft;
    private android.widget.ImageView sxRight;
    private int pageSize = 20;
    private int currentPage = 0;
    private List<Terminal> filteredPageBase = new ArrayList<>();

    // 数据库管理器
    private DatabaseManager databaseManager;
    private int currentUserRoleId = -1;
    private android.os.Handler autoRefreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private java.util.concurrent.ExecutorService ioExecutor;
    private final java.util.concurrent.atomic.AtomicInteger loadSeq = new java.util.concurrent.atomic.AtomicInteger();
    private final Runnable terminalRefreshDebounceRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                refreshTerminalsAndStatus();
                evaluateAlertOverlay();
                if (adapter != null) adapter.notifyDataSetChanged();
            } catch (Exception ignored) {}
        }
    };
    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                refreshTerminalsAndStatus();
            } finally {
                autoRefreshHandler.postDelayed(this, 120000);
            }
        }
    };

    // 报警浮层与待处理入口
    private View llAlertOverlay;
    private TextView tvAlertText;
    private TextView btnAlertMute;
    private TextView btnAlertMinimize;
    private TextView btnAlertHandle;
    private View llAlertPending;
    private View llAlertPendingSmall;
    private android.widget.TextView tvErrorNumber;
    private android.widget.TextView tvErrorTitle;
    private android.widget.TextView tvErrorName;
    private android.widget.TextView tvErrorCode;
    private android.widget.TextView tvErrorTime;
    private android.widget.ImageView ivErrorSmall;
    private android.widget.ImageView ivErrorClose;
    private android.widget.TextView tvErrorVoiceNo;
    private android.widget.TextView tvErrorComplete;

    private int pendingAlertCount = 0;
    private boolean alertMuted = false;
    private final java.util.Deque<AlertItem> alertQueue = new java.util.ArrayDeque<>();
    private AlertItem currentAlert = null;
    private android.media.MediaPlayer alertPlayer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal_list, container, false);

        // 初始化数据库管理器
        databaseManager = DatabaseManager.getInstance(requireContext());
        if (ioExecutor == null) {
            ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
        }

        // 初始化用户角色ID
        long userId = SPUtils.getInstance().getLong("current_user_id", -1);
        if (userId != -1) {
            User user = databaseManager.getUserById(userId);
            if (user != null) {
                currentUserRoleId = (int)user.getRoleId();
            }
        }

        initViews(view);
        if (getArguments() != null) {
            statusFilterTitle = getArguments().getString("status_filter_title", null);
        }

        // 检查查看终端列表权限（修正为正确的权限码：terminal_list）
        if (hasPermission("terminal_list")) {
            initTerminalStatus();
        } else {
            Toast.makeText(requireContext(), "您没有查看终端列表的权限", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void initViews(View view) {
        rvTerminalStatus = view.findViewById(R.id.rv_terminal_status);
        terminalRecycle = view.findViewById(R.id.terminal_recycle);
        addTerminalBtn = view.findViewById(R.id.add_terminal);
        tvGroupCategory = view.findViewById(R.id.tv_group_category);
        spinnerTs = view.findViewById(R.id.spinner_ts);
        sxLeft = view.findViewById(R.id.sx_left);
        sxRight = view.findViewById(R.id.sx_right);
        llAlertOverlay = view.findViewById(R.id.ll_alert_overlay);
        tvAlertText = view.findViewById(R.id.tv_alert_text);
        btnAlertMute = view.findViewById(R.id.btn_alert_mute);
        btnAlertMinimize = view.findViewById(R.id.btn_alert_minimize);
        btnAlertHandle = view.findViewById(R.id.btn_alert_handle);
        llAlertPending = view.findViewById(R.id.ll_alert_pending);
        llAlertPendingSmall = view.findViewById(R.id.ll_alert_pending_small);
        tvErrorNumber = view.findViewById(R.id.error_number);
        tvErrorTitle = view.findViewById(R.id.error_title);
        tvErrorName = view.findViewById(R.id.error_name);
        tvErrorCode = view.findViewById(R.id.error_code);
        tvErrorTime = view.findViewById(R.id.error_time);
        ivErrorSmall = view.findViewById(R.id.error_small);
        ivErrorClose = view.findViewById(R.id.error_close);
        tvErrorVoiceNo = view.findViewById(R.id.error_voice_no);
        tvErrorComplete = view.findViewById(R.id.error_complte);
//        btnAlertPending = view.findViewById(R.id.btn_alert_pending);
//        tvAlertCount = view.findViewById(R.id.tv_alert_count);

        // 设置添加终端按钮点击事件
        addTerminalBtn.setOnClickListener(v -> {
            if (hasPermission("terminal_add")) {
                onAddTerminalClick();
            } else {
                Toast.makeText(requireContext(), "您没有添加终端的权限", Toast.LENGTH_SHORT).show();
            }
        });

        // 终端名称搜索（就地过滤当前界面）
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

        // 初始化两级选择器入口（文本点击弹出选择）
        if (tvGroupCategory != null) {
            tvGroupCategory.setOnClickListener(v -> showGroupCategoryPicker());
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
        // 报警浮层按钮行为
        if (btnAlertMute != null) {
            btnAlertMute.setOnClickListener(v -> {
                alertMuted = !alertMuted;
                Toast.makeText(requireContext(), alertMuted ? "已静音" : "已取消静音", Toast.LENGTH_SHORT).show();
            });
        }
        if (btnAlertMinimize != null) {
            btnAlertMinimize.setOnClickListener(v -> {
                minimizePending();
            });
        }
        if (btnAlertHandle != null) {
            btnAlertHandle.setOnClickListener(v -> {
                handleCurrentAlert();
            });
        }
//        if (btnAlertPending != null) {
//            btnAlertPending.setOnClickListener(v -> openAlertPendingList());
//        }
        if (ivErrorSmall != null) {
            ivErrorSmall.setOnClickListener(v -> minimizePending());
        }
        if (llAlertPendingSmall != null) {
            llAlertPendingSmall.setOnClickListener(v -> expandPending());
        }
        if (ivErrorClose != null) {
            ivErrorClose.setOnClickListener(v -> {
                if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
                if (llAlertPendingSmall != null) llAlertPendingSmall.setVisibility(View.VISIBLE);
            });
        }
        if (tvErrorVoiceNo != null) {
            tvErrorVoiceNo.setOnClickListener(v -> {
                alertMuted = !alertMuted;
                Toast.makeText(requireContext(), alertMuted ? "已静音" : "已取消静音", Toast.LENGTH_SHORT).show();
            });
        }
        if (tvErrorComplete != null) {
            tvErrorComplete.setOnClickListener(v -> handleCurrentAlert());
        }
    }

    private void initTerminalStatus() {
        // 设置状态RecyclerView
        //LinearLayoutManager statusLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 6);
        rvTerminalStatus.setLayoutManager(gridLayoutManager);

        terminalStatusAdapter = new TerminalStatusAdapter();
        rvTerminalStatus.setAdapter(terminalStatusAdapter);
        terminalStatusAdapter.setOnItemClickListener((adapter1, view1, position1) -> {
            TerminalStatus item = (TerminalStatus) terminalStatusAdapter.getItem(position1);
            if (item == null) return;
            TerminalStatusListFragment fragment = TerminalStatusListFragment.newInstance(item.getTitle());
            if (getActivity() instanceof com.lora.cn.ui.activity.MainActivity) {
                com.lora.cn.ui.activity.MainActivity mainActivity = (com.lora.cn.ui.activity.MainActivity) getActivity();
                mainActivity.showOverlayOnly();
            }
            androidx.appcompat.app.AppCompatActivity a = (androidx.appcompat.app.AppCompatActivity) getActivity();
            if (a != null) {
                a.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_device_list_container, fragment)
                        .addToBackStack("terminal_status_filter")
                        .commit();
            }
        });

        // 初始化终端列表
        initTerminalList();
    }

    private void showGroupCategoryPicker() {
        try {
            android.app.Dialog dialog = new android.app.Dialog(requireContext());
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
            android.view.View root = android.view.LayoutInflater.from(requireContext()).inflate(R.layout.dialog_group_category_picker, null);
            dialog.setContentView(root);
            android.widget.ListView lvGroups = root.findViewById(R.id.lv_groups);
            android.widget.ListView lvCategories = root.findViewById(R.id.lv_categories);
            android.widget.Button btnConfirm = root.findViewById(R.id.btn_confirm);
            android.widget.Button btnCancel = root.findViewById(R.id.btn_cancel);

            // 加载分组
            List<com.lora.cn.database.entity.Group> groups = databaseManager.getAllGroups();
            List<com.lora.cn.database.entity.Group> displayGroups = new ArrayList<>();
            com.lora.cn.database.entity.Group allG = new com.lora.cn.database.entity.Group();
            allG.setGroupId(-1);
            allG.setGroupName("全部分组");
            displayGroups.add(allG);
            if (groups != null) displayGroups.addAll(groups);
            List<String> groupNames = new ArrayList<>();
            for (com.lora.cn.database.entity.Group g : displayGroups) groupNames.add(g.getGroupName());
            android.widget.ArrayAdapter<String> gAdapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, groupNames);
            lvGroups.setAdapter(gAdapter);

            final long[] tempSelectedGroupId = {selectedGroupId};
            final long[] tempSelectedCategoryId = {selectedCategoryId};
            final android.view.View[] lastGroupSelectedView = new android.view.View[1];
            final android.view.View[] lastCategorySelectedView = new android.view.View[1];

            lvGroups.setOnItemClickListener((parent, view, position, id) -> {
                try {
                    if (lastGroupSelectedView[0] != null) lastGroupSelectedView[0].setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    view.setBackgroundColor(android.graphics.Color.parseColor("#E6F0FF"));
                    lastGroupSelectedView[0] = view;
                } catch (Exception ignored) {}
                tempSelectedGroupId[0] = displayGroups.get(position).getGroupId();
                // 加载分类
                List<com.lora.cn.database.entity.Category> categories = tempSelectedGroupId[0] == -1 ? databaseManager.getAllCategories() : databaseManager.getCategoriesByGroupId(tempSelectedGroupId[0]);
                List<com.lora.cn.database.entity.Category> displayCategories = new ArrayList<>();
                com.lora.cn.database.entity.Category allC = new com.lora.cn.database.entity.Category();
                allC.setCategoryId(-1);
                allC.setCategoryName("全部分类");
                displayCategories.add(allC);
                if (categories != null) displayCategories.addAll(categories);
                List<String> categoryNames = new ArrayList<>();
                for (com.lora.cn.database.entity.Category c : displayCategories) categoryNames.add(c.getCategoryName());
                android.widget.ArrayAdapter<String> cAdapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, categoryNames);
                lvCategories.setAdapter(cAdapter);
                lvCategories.setOnItemClickListener((p, v, pos, i2) -> {
                    try {
                        if (lastCategorySelectedView[0] != null) lastCategorySelectedView[0].setBackgroundColor(android.graphics.Color.TRANSPARENT);
                        v.setBackgroundColor(android.graphics.Color.parseColor("#E6F0FF"));
                        lastCategorySelectedView[0] = v;
                    } catch (Exception ignored) {}
                    tempSelectedCategoryId[0] = displayCategories.get(pos).getCategoryId();
                });
            });

            btnConfirm.setOnClickListener(v -> {
                selectedGroupId = tempSelectedGroupId[0];
                selectedCategoryId = tempSelectedCategoryId[0];
                // 更新显示文本
                try {
                    String gName = "全部分组";
                    String cName = "全部分类";
                    if (selectedGroupId != -1) {
                        List<com.lora.cn.database.entity.Group> gs = databaseManager.getAllGroups();
                        for (com.lora.cn.database.entity.Group g : gs) if (g.getGroupId() == selectedGroupId) { gName = g.getGroupName(); break; }
                    }
                    if (selectedCategoryId != -1) {
                        List<com.lora.cn.database.entity.Category> cs = selectedGroupId == -1 ? databaseManager.getAllCategories() : databaseManager.getCategoriesByGroupId(selectedGroupId);
                        for (com.lora.cn.database.entity.Category c : cs) if (c.getCategoryId() == selectedCategoryId) { cName = c.getCategoryName(); break; }
                    }
                    tvGroupCategory.setText(gName + "/" + cName);
                } catch (Exception ignored) {}

                applyFilters();
                dialog.dismiss();
            });
            btnCancel.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "显示分组分类选择器失败", e);
        }
    }

    /**
     * 从数据库更新终端状态统计
     */
    private void updateTerminalStatusFromDatabase() {
        refreshTerminalsAndStatus();
    }

    private void initTerminalList() {
        // 首先尝试从数据库加载终端数据
        loadTerminals();
    }

    @Override
    public void onStart() {
        super.onStart();
        // 注册事件总线，监听新增终端事件
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            // 返回页面时刷新终端列表和状态统计，确保展示最新添加的设备
            refreshTerminalsAndStatus();
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
            autoRefreshHandler.postDelayed(autoRefreshRunnable, 120000);
            evaluateAlertOverlay();
        } catch (Exception e) {
            Log.e(TAG, "onResume 刷新终端列表失败", e);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTerminalRefreshEvent(com.lora.cn.event.TerminalRefreshEvent event) {
        try {
            autoRefreshHandler.removeCallbacks(terminalRefreshDebounceRunnable);
            autoRefreshHandler.postDelayed(terminalRefreshDebounceRunnable, 300);
        } catch (Exception ignored) {}
    }

    @Override
    public void onStop() {
        // 取消注册事件总线
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        super.onStop();
    }

    

    

    private void showAlertOverlay(String text) {
        if (tvAlertText != null) tvAlertText.setText(text);
        if (llAlertOverlay != null) llAlertOverlay.setVisibility(View.VISIBLE);
        if (llAlertPending != null) llAlertPending.setVisibility(View.VISIBLE);
    }

    private void updatePendingBadge() {
        if (tvErrorNumber != null) tvErrorNumber.setText(String.valueOf(pendingAlertCount));
        if (llAlertPendingSmall != null) llAlertPendingSmall.setVisibility(pendingAlertCount > 0 ? View.VISIBLE : View.GONE);
    }

    private void evaluateAlertOverlay() {
        try {
            // 全局弹窗由 MainActivity 负责，这里不再弹窗
        } catch (Exception ignored) {}
    }

//    private void openAlertPendingList() {
//        try {
//            androidx.fragment.app.Fragment fragment = new com.lora.cn.ui.fragment.AlertPendingListFragment();
//            if (getActivity() != null) {
//                if (getActivity() instanceof com.lora.cn.ui.activity.MainActivity) {
//                    ((com.lora.cn.ui.activity.MainActivity) getActivity()).showDeviceList();
//                }
//                androidx.appcompat.app.AppCompatActivity a = (androidx.appcompat.app.AppCompatActivity) getActivity();
//                android.view.View container = a.findViewById(R.id.fragment_device_list_container);
//                if (container != null) {
//                    container.setVisibility(View.VISIBLE);
//                    android.view.View rvTabs = a.findViewById(R.id.rv_menu_tabs);
//                    if (rvTabs != null) rvTabs.setVisibility(View.INVISIBLE);
//                    android.view.View vp = a.findViewById(R.id.view_pager);
//                    if (vp != null) vp.setVisibility(View.GONE);
//                }
//                a.getSupportFragmentManager().beginTransaction()
//                        .replace(R.id.fragment_device_list_container, fragment)
//                        .addToBackStack("alert_pending")
//                        .commit();
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "打开报警待处理列表失败", e);
//        }
//    }

    private void showLatestPending() {
        currentAlert = alertQueue.peekLast();
        if (currentAlert == null) return;
        if (tvErrorTitle != null) tvErrorTitle.setText(currentAlert.title);
        if (tvErrorName != null) tvErrorName.setText(currentAlert.name);
        if (tvErrorCode != null) tvErrorCode.setText(currentAlert.code);
        if (tvErrorTime != null) tvErrorTime.setText(currentAlert.time);
        pendingAlertCount = alertQueue.size();
        updatePendingBadge();
        if (llAlertPendingSmall != null) llAlertPendingSmall.setVisibility(View.GONE);
        if (llAlertPending != null) llAlertPending.setVisibility(View.VISIBLE);
    }

    private void minimizePending() {
        if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
        updatePendingBadge();
    }

    private void expandPending() {
        if (!alertQueue.isEmpty()) {
            showLatestPending();
        } else {
            if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
            if (llAlertPendingSmall != null) llAlertPendingSmall.setVisibility(View.GONE);
        }
    }

    private void handleCurrentAlert() {
        if (currentAlert != null) {
            alertQueue.remove(currentAlert);
            currentAlert = null;
        } else if (!alertQueue.isEmpty()) {
            alertQueue.removeLast();
        }
        pendingAlertCount = alertQueue.size();
        if (!alertQueue.isEmpty()) {
            if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
            if (llAlertPendingSmall != null) llAlertPendingSmall.setVisibility(View.VISIBLE);
            updatePendingBadge();
        } else {
            if (llAlertPending != null) llAlertPending.setVisibility(View.GONE);
            if (llAlertPendingSmall != null) llAlertPendingSmall.setVisibility(View.GONE);
            if (llAlertOverlay != null) llAlertOverlay.setVisibility(View.GONE);
        }
    }

//    private AlertItem buildAlertItem(com.lora.cn.utils.LoRaFrameParser.ParsedFrame frame, String msg) {
//        String name = "";
//        String code = frame != null ? frame.deviceId : "";
//        String time = "";
//        try {
//            DatabaseHelper dbHelper = DatabaseHelper.getInstance(getContext());
//            java.util.List<Terminal> terminals = dbHelper.getAllTerminals();
//            if (terminals != null) {
//                for (Terminal t : terminals) {
//                    if (t.getTerminalId() != null && t.getTerminalId().equalsIgnoreCase(code)) {
//                        name = t.getTerminalName();
//                        break;
//                    }
//                }
//            }
//        } catch (Exception ignored) {}
//        if (frame != null && frame.dataTime != null) {
//            try {
//                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
//                time = sdf.format(frame.dataTime);
//            } catch (Exception ignored) {}
//        }
//        AlertItem item = new AlertItem();
//        item.title = msg;
//        item.name = name;
//        item.code = code;
//        item.time = time;
//        return item;
//    }

    private static class AlertItem {
        String title;
        String name;
        String code;
        String time;
    }

    private void playAlertSoundOnce() {
        try {
            if (alertPlayer != null) {
                if (alertPlayer.isPlaying()) return;
                try { alertPlayer.release(); } catch (Exception ignored) {}
                alertPlayer = null;
            }
            android.content.res.AssetFileDescriptor afd = requireContext().getAssets().openFd("901028.wav");
            android.media.MediaPlayer mp = new android.media.MediaPlayer();
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.setOnCompletionListener(p -> {
                try { p.release(); } catch (Exception ignored) {}
                if (alertPlayer == p) alertPlayer = null;
            });
            mp.prepare();
            mp.start();
            alertPlayer = mp;
        } catch (Exception ignored) {}
    }

    private void onTerminalClick(int position, Terminal terminal) {
        try {
            if (terminal == null || TextUtils.isEmpty(terminal.getTerminalId())) {
                Toast.makeText(getContext(), "终端ID为空，无法打开详情", Toast.LENGTH_SHORT).show();
                return;
            }

            com.lora.cn.ui.fragment.TerminalDetailFragment fragment =
                    com.lora.cn.ui.fragment.TerminalDetailFragment.newInstance(terminal.getTerminalId());
            if (getActivity() != null) {
                if (getActivity() instanceof com.lora.cn.ui.activity.MainActivity) {
                    ((com.lora.cn.ui.activity.MainActivity) getActivity()).showOverlayOnly();
                }
                android.app.Activity a = getActivity();
                android.view.View container = a.findViewById(R.id.fragment_device_list_container);
                if (container != null) {
                    container.setVisibility(android.view.View.VISIBLE);
                    android.view.View rvTabs = a.findViewById(R.id.rv_menu_tabs);
                    if (rvTabs != null) rvTabs.setVisibility(android.view.View.INVISIBLE);
                    android.view.View vp = a.findViewById(R.id.view_pager);
                    if (vp != null) vp.setVisibility(android.view.View.GONE);
                    ((androidx.appcompat.app.AppCompatActivity) a).getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_device_list_container, fragment)
                            .addToBackStack("terminal_detail")
                            .commit();
                } else {
                    Toast.makeText(getContext(), "未找到详情容器", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "显示终端详情失败", e);
            Toast.makeText(getContext(), "显示终端详情失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 添加终端按钮点击事件
     */
    private void onAddTerminalClick() {
        // 检查权限
        if (!hasPermission("terminal_add")) {
            Toast.makeText(getContext(), "您没有添加终端的权限", Toast.LENGTH_SHORT).show();
            return;
        }

        // 展示附近设备列表（DeviceListFragment）
        if (getActivity() instanceof com.lora.cn.ui.activity.MainActivity) {
            com.lora.cn.ui.activity.MainActivity mainActivity = (com.lora.cn.ui.activity.MainActivity) getActivity();
            mainActivity.showDeviceList();
        } else {
            Toast.makeText(getContext(), "无法打开设备列表", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 检查当前用户是否有指定权限
     */
    private boolean hasPermission(String permissionCode) {
        if (currentUserRoleId == -1) {
            return false;
        }
        return databaseManager.hasPermission(currentUserRoleId, permissionCode);
    }

    public static TerminalListFragment newInstance() {
        return new TerminalListFragment();
    }

    

    public static TerminalListFragment newInstance(String statusFilterTitle) {
        TerminalListFragment f = new TerminalListFragment();
        Bundle b = new Bundle();
        b.putString("status_filter_title", statusFilterTitle);
        f.setArguments(b);
        return f;
    }

    /**
     * 加载终端列表数据
     */
    private void loadTerminals() {
        refreshTerminalsAndStatus();
    }

    private void ensureTerminalListAdapter() {
        if (adapter != null) return;
        if (terminalRecycle == null) return;
        GridLayoutManager terminalLayoutManager = new GridLayoutManager(getContext(), 4);
        terminalRecycle.setLayoutManager(terminalLayoutManager);
        adapter = new TerminalAdapter();
        adapter.setOnFavoriteClickListener((terminal, isFavorite) -> {
            if (terminal == null || TextUtils.isEmpty(terminal.getTerminalId())) return;
            if (ioExecutor == null) return;
            android.content.Context ctx = getContext();
            if (ctx == null) return;
            android.content.Context appCtx = ctx.getApplicationContext();
            ioExecutor.execute(() -> {
                try {
                    DatabaseHelper dbHelper = DatabaseHelper.getInstance(appCtx);
                    dbHelper.updateTerminalFavoriteStatus(terminal.getTerminalId(), isFavorite);
                    com.lora.cn.ui.model.LogInfo logInfo = new com.lora.cn.ui.model.LogInfo();
                    logInfo.setTerminalId(terminal.getTerminalId());
                    logInfo.setTerminalName(terminal.getTerminalName());
                    logInfo.setDeviceId(terminal.getTerminalId());
                    logInfo.setStatusCode(0);
                    logInfo.setOperator("");
                    logInfo.setAction(isFavorite ? "收藏终端" : "取消收藏");
                    logInfo.setOperationTime("");
                    logInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
                    dbHelper.addLog(logInfo);
                } catch (Exception e) {
                    Log.e(TAG, "更新收藏状态失败", e);
                }
                autoRefreshHandler.post(this::refreshTerminalsAndStatus);
            });
        });
        terminalRecycle.setAdapter(adapter);
        adapter.setOnItemClickListener((adapter, view, position) -> {
            if (hasPermission("terminal_detail")) {
                Terminal terminal = (Terminal) adapter.getItem(position);
                onTerminalClick(position, terminal);
            } else {
                Toast.makeText(requireContext(), "您没有查看终端详情的权限", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshTerminalsAndStatus() {
        if (ioExecutor == null) return;
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        android.content.Context appCtx = ctx.getApplicationContext();
        int token = loadSeq.incrementAndGet();
        ioExecutor.execute(() -> {
            List<Terminal> terminals = null;
            try {
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(appCtx);
                terminals = dbHelper.getAllTerminals();
            } catch (Exception e) {
                Log.e(TAG, "获取终端列表失败", e);
            }

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

            List<Terminal> displayTerminals = new ArrayList<>();
            if (terminals != null && !terminals.isEmpty()) {
                try {
                    displayTerminals = convertToDisplayTerminals(terminals, categoryNameById);
                } catch (Exception e) {
                    Log.e(TAG, "转换终端列表失败", e);
                }
            }
            List<TerminalStatus> statusList = buildStatusList(terminals);

            List<Terminal> finalDisplayTerminals = displayTerminals;
            autoRefreshHandler.post(() -> {
                if (!isAdded()) return;
                if (token != loadSeq.get()) return;
                ensureTerminalListAdapter();
                if (terminalStatusAdapter != null) {
                    terminalStatusAdapter.submitList(statusList);
                }
                allDisplayTerminals = finalDisplayTerminals != null ? finalDisplayTerminals : new ArrayList<>();
                applyCurrentFilters();
            });
        });
    }

    /**
     * 将数据库终端数据转换为UI显示格式
     */
    private List<Terminal> convertToDisplayTerminals(List<Terminal> dbTerminals, java.util.Map<Long, String> categoryNameById) {
        List<Terminal> displayTerminals = new ArrayList<>();

        for (Terminal dbTerminal : dbTerminals) {
            Terminal displayTerminal = new Terminal();

            // 设置基本信息
            displayTerminal.setId(dbTerminal.getId());
            displayTerminal.setTerminalId(dbTerminal.getTerminalId());
            displayTerminal.setTerminalName(dbTerminal.getTerminalName());
            displayTerminal.setName(dbTerminal.getTerminalName()); // 显示名称使用终端名称
            displayTerminal.setDepartmentId(dbTerminal.getDepartmentId());
            displayTerminal.setRoomId(dbTerminal.getRoomId());
            displayTerminal.setNursingGroupId(dbTerminal.getNursingGroupId());
            displayTerminal.setOtherId(dbTerminal.getOtherId());
            displayTerminal.setExtension(dbTerminal.getExtension());
            // 科室与病房：优先使用字符串；为空则根据ID查询分类名称
            String dept = dbTerminal.getDepartment();
            if (TextUtils.isEmpty(dept) && dbTerminal.getDepartmentId() > 0) {
                try {
                    if (categoryNameById != null) dept = categoryNameById.get(dbTerminal.getDepartmentId());
                } catch (Exception ignored) {}
            }
            String room = dbTerminal.getLocation();
            if (TextUtils.isEmpty(room) && dbTerminal.getRoomId() > 0) {
                try {
                    if (categoryNameById != null) room = categoryNameById.get(dbTerminal.getRoomId());
                } catch (Exception ignored) {}
            }
            displayTerminal.setRssi(dbTerminal.getRssi());

            displayTerminal.setGroupIdsText(dbTerminal.getGroupIdsText());
            displayTerminal.setGroupNamesText(dbTerminal.getGroupNamesText());

            displayTerminal.setDepartment(dept);
            displayTerminal.setLocation(room);
            displayTerminal.setStatus(dbTerminal.getStatus());
            displayTerminal.setSignalStrength(dbTerminal.getSignalStrength());
            displayTerminal.setFavorite(dbTerminal.isFavorite());

            // 根据状态设置图标
            int statusIcon = getStatusIcon(dbTerminal.getStatus());
            displayTerminal.setStatusIconResId(statusIcon);
            displayTerminal.setStatusText(com.lora.cn.ui.constants.TerminalStatusConstants.codeToText(dbTerminal.getStatus()));

            // 设置电量信息
            displayTerminal.setBatteryLevel(dbTerminal.getBatteryLevel());
            displayTerminal.setBatteryText(dbTerminal.getBatteryLevel() + "%");

            // 根据电量设置电池图标
            if (dbTerminal.getBatteryLevel() > 80) {
                displayTerminal.setBatteryIconResId(R.drawable.ic_green_sd); // 高电量-绿色电池
            } else if (dbTerminal.getBatteryLevel() > 30) {
                displayTerminal.setBatteryIconResId(R.drawable.ic_yellow_sd); // 中电量-黄色电池
            } else {
                displayTerminal.setBatteryIconResId(R.mipmap.ic_red_sd); // 低电量-红色电池（PNG保留在mipmap）
            }

            // 设置重要性（收藏状态）
            displayTerminal.setImportant(dbTerminal.isFavorite());

            displayTerminals.add(displayTerminal);
        }

        return displayTerminals;
    }

    private List<TerminalStatus> buildStatusList(List<Terminal> allTerminals) {
        try {
            int favoriteCount = 0;
            int onlineCount = 0;
            int normalTakenCount = 0;
            int abnormalLostCount = 0;
            int lowBatteryCount = 0;
            int offlineCount = 0;

            int lowTh = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
            if (allTerminals != null) {
                for (Terminal terminal : allTerminals) {
                    if (terminal == null) continue;
                    if (terminal.isFavorite()) favoriteCount++;

                    int statusCode = terminal.getStatus();
                    if (statusCode == TerminalStatusConstants.CODE_ONLINE) onlineCount++;
                    else if (statusCode == TerminalStatusConstants.CODE_OFFLINE) offlineCount++;
                    else if (statusCode == TerminalStatusConstants.CODE_ABNORMAL_TAKEN) abnormalLostCount++;
                    else if (statusCode == TerminalStatusConstants.CODE_NORMAL_TAKEN) normalTakenCount++;

                    int batteryLevel = terminal.getBatteryLevel();
                    if (statusCode != TerminalStatusConstants.CODE_OFFLINE && batteryLevel <= lowTh) {
                        lowBatteryCount++;
                    }
                }
            }

            List<TerminalStatus> statusList = new ArrayList<>();
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_IMPORTANT, R.mipmap.ic_coll, favoriteCount));
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_ONLINE, R.mipmap.ic_xh_3, onlineCount));
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_NORMAL_TAKEN, R.mipmap.ic_blue_right, normalTakenCount));
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_ABNORMAL_LOST, R.mipmap.ic_ds, abnormalLostCount));
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_LOW_BATTERY, R.mipmap.ic_red_sd, lowBatteryCount));
            statusList.add(new TerminalStatus(TerminalStatusConstants.STATUS_OFFLINE, R.mipmap.ic_xh_no, offlineCount));
            return statusList;
        } catch (Exception e) {
            Log.e(TAG, "构建状态统计失败", e);
            return TerminalStatusConstants.getDefaultStatusList();
        }
    }

    @Override
    public void onDestroy() {
        try {
            autoRefreshHandler.removeCallbacks(terminalRefreshDebounceRunnable);
        } catch (Exception ignored) {}
        if (ioExecutor != null) {
            try {
                ioExecutor.shutdownNow();
            } catch (Exception ignored) {}
            ioExecutor = null;
        }
        super.onDestroy();
    }

    private void applyFilters() {
        try {
            if (adapter == null) return;
            List<Terminal> base = new ArrayList<>(allDisplayTerminals);
            // 只有当选择了具体分类时才过滤，否则显示全部
            if (selectedCategoryId != -1 && selectedGroupId != -1) {
                List<Terminal> filtered = new ArrayList<>();
                for (Terminal t : base) {
                    boolean match = false;
                    if (selectedGroupId == 1) {
                        match = (t.getDepartmentId() == selectedCategoryId);
                    } else if (selectedGroupId == 2) {
                        match = (t.getRoomId() == selectedCategoryId);
                    } else if (selectedGroupId == 3) {
                        match = (t.getNursingGroupId() == selectedCategoryId);
                    } else if (selectedGroupId == 4) {
                        match = (t.getOtherId() == selectedCategoryId);
                    } else {
                        // 暂不支持其他自定义分组过滤
                        match = true;
                    }
                    if (match) filtered.add(t);
                }
                java.util.Collections.sort(filtered, new java.util.Comparator<Terminal>() {
                    @Override
                    public int compare(Terminal a, Terminal b) {
                        int ra = a.isFavorite() ? 0 : ((a.getStatus() == TerminalStatusConstants.CODE_ABNORMAL_TAKEN || a.getStatus() == TerminalStatusConstants.CODE_OFFLINE) ? 1 : 2);
                        int rb = b.isFavorite() ? 0 : ((b.getStatus() == TerminalStatusConstants.CODE_ABNORMAL_TAKEN || b.getStatus() == TerminalStatusConstants.CODE_OFFLINE) ? 1 : 2);
                        if (ra != rb) return ra - rb;
                        String an = a.getTerminalName();
                        String bn = b.getTerminalName();
                        if (an == null) an = "";
                        if (bn == null) bn = "";
                        return an.compareTo(bn);
                    }
                });
                filteredPageBase = filtered;
                currentPage = 0;
                submitCurrentPage();
                updatePaginationControls();
            } else {
                java.util.Collections.sort(base, new java.util.Comparator<Terminal>() {
                    @Override
                    public int compare(Terminal a, Terminal b) {
                        int ra = a.isFavorite() ? 0 : ((a.getStatus() == TerminalStatusConstants.CODE_ABNORMAL_TAKEN || a.getStatus() == TerminalStatusConstants.CODE_OFFLINE) ? 1 : 2);
                        int rb = b.isFavorite() ? 0 : ((b.getStatus() == TerminalStatusConstants.CODE_ABNORMAL_TAKEN || b.getStatus() == TerminalStatusConstants.CODE_OFFLINE) ? 1 : 2);
                        if (ra != rb) return ra - rb;
                        String an = a.getTerminalName();
                        String bn = b.getTerminalName();
                        if (an == null) an = "";
                        if (bn == null) bn = "";
                        return an.compareTo(bn);
                    }
                });
                filteredPageBase = new ArrayList<>(base);
                currentPage = 0;
                submitCurrentPage();
                updatePaginationControls();
            }
        } catch (Exception e) {
            Log.e(TAG, "应用过滤失败", e);
        }
    }

    /**
     * 根据状态获取对应的图标资源ID
     */
    private int getStatusIcon(int statusCode) {
        switch (statusCode) {
            case com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ONLINE:
                return R.mipmap.ic_xh_3;
            case com.lora.cn.ui.constants.TerminalStatusConstants.CODE_ABNORMAL_TAKEN:
                return R.mipmap.ic_ds;
            case com.lora.cn.ui.constants.TerminalStatusConstants.CODE_NORMAL_TAKEN:
                return R.mipmap.ic_blue_right;
            case com.lora.cn.ui.constants.TerminalStatusConstants.CODE_OFFLINE:
            default:
                return R.mipmap.ic_xh_no;
        }
    }
    private void applyCurrentFilters() {
        if (adapter == null) return;
        List<Terminal> list = new ArrayList<>(allDisplayTerminals);
        // 状态筛选标题
        if (statusFilterTitle != null && !statusFilterTitle.isEmpty()) {
            List<Terminal> filtered = new ArrayList<>();
            for (Terminal t : list) {
                boolean match = false;
                if (TerminalStatusConstants.STATUS_IMPORTANT.equals(statusFilterTitle)) match = t.isFavorite();
                else if (TerminalStatusConstants.STATUS_ONLINE.equals(statusFilterTitle)) match = t.getStatus() == TerminalStatusConstants.CODE_ONLINE;
                else if (TerminalStatusConstants.STATUS_OFFLINE.equals(statusFilterTitle)) match = t.getStatus() == TerminalStatusConstants.CODE_OFFLINE;
                else if (TerminalStatusConstants.STATUS_NORMAL_TAKEN.equals(statusFilterTitle)) match = t.getStatus() == TerminalStatusConstants.CODE_NORMAL_TAKEN;
                else if (TerminalStatusConstants.STATUS_ABNORMAL_LOST.equals(statusFilterTitle)) match = t.getStatus() == TerminalStatusConstants.CODE_ABNORMAL_TAKEN;
                else if (TerminalStatusConstants.STATUS_LOW_BATTERY.equals(statusFilterTitle)) {
                    int lowTh = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
                    match = t.getStatus() != TerminalStatusConstants.CODE_OFFLINE && t.getBatteryLevel() <= lowTh;
                }
                if (match) filtered.add(t);
            }
            list = filtered;
            Log.d(TAG, "applyCurrentFilters after status filter size=" + list.size());
        }
        // 终端名称关键词
        if (searchKeyword != null && !searchKeyword.isEmpty()) {
            List<Terminal> filtered = new ArrayList<>();
            for (Terminal t : list) {
                String name = t.getTerminalName();
                if (name != null && name.contains(searchKeyword)) filtered.add(t);
            }
            list = filtered;
            Log.d(TAG, "applyCurrentFilters after keyword filter size=" + list.size());
        }
        // 下拉筛选
        android.view.View root = getView();
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
        java.util.Collections.sort(list, new java.util.Comparator<Terminal>() {
            @Override
            public int compare(Terminal a, Terminal b) {
                int ra = a.isFavorite() ? 0 : ((a.getStatus() == TerminalStatusConstants.CODE_ABNORMAL_TAKEN || a.getStatus() == TerminalStatusConstants.CODE_OFFLINE) ? 1 : 2);
                int rb = b.isFavorite() ? 0 : ((b.getStatus() == TerminalStatusConstants.CODE_ABNORMAL_TAKEN || b.getStatus() == TerminalStatusConstants.CODE_OFFLINE) ? 1 : 2);
                if (ra != rb) return ra - rb;
                String an = a.getTerminalName();
                String bn = b.getTerminalName();
                if (an == null) an = "";
                if (bn == null) bn = "";
                return an.compareTo(bn);
            }
        });
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
        evaluateAlertOverlay();
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
}
