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

    private final List<Terminal> allDisplayTerminals = new ArrayList<>();
    private String statusFilterTitle = null;
    private String searchKeyword = "";

    private DatabaseManager databaseManager;
    private int currentUserRoleId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status_terminal_list, container, false);

        databaseManager = DatabaseManager.getInstance(requireContext());
        long userId = SPUtils.getInstance().getLong("current_user_id", -1);
        if (userId != -1) {
            User user = databaseManager.getUserById(userId);
            if (user != null) currentUserRoleId = (int) user.getRoleId();
        }

        initViews(view);
        if (getArguments() != null) {
            statusFilterTitle = getArguments().getString("status_filter_title", null);
        }

        if (hasPermission("terminal_list")) {
            initStatusBar();
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

        addTerminalBtn.setOnClickListener(v -> Toast.makeText(requireContext(), "请返回首页进行新增", Toast.LENGTH_SHORT).show());

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

        terminalRecycle.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        adapter = new TerminalAdapter();
        terminalRecycle.setAdapter(adapter);
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
                allDisplayTerminals.addAll(terminals);
            } else {
                adapter.submitList(new ArrayList<>());
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "加载终端失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
