package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

    private DatabaseManager databaseManager;
    private int currentUserRoleId = -1;
    private android.os.Handler autoRefreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                loadTerminals();
                applyCurrentFilters();
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
        long userId = SPUtils.getInstance().getLong("current_user_id", -1);
        if (userId != -1) {
            User user = databaseManager.getUserById(userId);
            if (user != null) currentUserRoleId = (int) user.getRoleId();
        }

        initViews(view);


        if (hasPermission("terminal_list")) {
            //initStatusBar();
            initTerminalList();
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
//        toolbarLeftTitle = view.findViewById(R.id.toolbar_left_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(statusFilterTitle != null ? statusFilterTitle : "");
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

        androidx.recyclerview.widget.GridLayoutManager terminalLayoutManager = new androidx.recyclerview.widget.GridLayoutManager(getContext(), 4);
        terminalRecycle.setLayoutManager(terminalLayoutManager);
        adapter = new TerminalAdapter();
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

    private void initStatusBar() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 6);
        rvTerminalStatus.setLayoutManager(gridLayoutManager);
        terminalStatusAdapter = new TerminalStatusAdapter();
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
        loadTerminals();
        applyCurrentFilters();
    }

    private void loadTerminals() {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(requireContext());
            List<Terminal> terminals = dbHelper.getAllTerminals();
            if (terminals != null && !terminals.isEmpty()) {
                allDisplayTerminals.clear();
                allDisplayTerminals.addAll(convertToDisplayTerminals(terminals));
            } else {
                adapter.submitList(new ArrayList<>());
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "加载终端失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** 将数据库终端数据转换为UI显示格式（与首页一致） */
    private List<Terminal> convertToDisplayTerminals(List<Terminal> dbTerminals) {
        List<Terminal> displayTerminals = new ArrayList<>();
        for (Terminal dbTerminal : dbTerminals) {
            Terminal displayTerminal = new Terminal();
            displayTerminal.setId(dbTerminal.getId());
            displayTerminal.setTerminalId(dbTerminal.getTerminalId());
            displayTerminal.setTerminalName(dbTerminal.getTerminalName());
            displayTerminal.setName(dbTerminal.getTerminalName());
            String dept = dbTerminal.getDepartment();
            if (android.text.TextUtils.isEmpty(dept) && dbTerminal.getDepartmentId() > 0) {
                try {
                    com.lora.cn.database.entity.Category c = DatabaseManager.getInstance(getContext()).getCategoryById(dbTerminal.getDepartmentId());
                    if (c != null) dept = c.getCategoryName();
                } catch (Exception ignored) {}
            }
            String room = dbTerminal.getLocation();
            if (android.text.TextUtils.isEmpty(room) && dbTerminal.getRoomId() > 0) {
                try {
                    com.lora.cn.database.entity.Category c2 = DatabaseManager.getInstance(getContext()).getCategoryById(dbTerminal.getRoomId());
                    if (c2 != null) room = c2.getCategoryName();
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
            displayTerminals.add(displayTerminal);
        }
        return displayTerminals;
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
                else if (TerminalStatusConstants.STATUS_ABNORMAL_LOST.equals(statusFilterTitle)) match = t.getStatus() == TerminalStatusConstants.CODE_ABNORMAL_TAKEN;
                else if (TerminalStatusConstants.STATUS_LOW_BATTERY.equals(statusFilterTitle)) match = t.getBatteryLevel() <= 20;
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
        adapter.submitList(list);
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
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        autoRefreshHandler.postDelayed(autoRefreshRunnable, 120000);
    }

    @Override
    public void onPause() {
        super.onPause();
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    private int getStatusIcon(int statusCode) {
        switch (statusCode) {
            case TerminalStatusConstants.CODE_ONLINE:
                return R.mipmap.ic_xh_3;
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
