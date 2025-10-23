package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.DatabaseHelper;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.dao.TerminalDao;
import com.lora.cn.database.entity.Terminal;
import com.lora.cn.ui.adapter.DeviceListAdapter;
import com.lora.cn.ui.activity.MainActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备列表Fragment - 显示附近终端
 */
public class DeviceListFragment extends Fragment {

    private TextView btnSearchTerminal;
    private TextView btnBack;
    private EditText etSearch;
    private TextView btnSearch;
    private RecyclerView rvTerminals;
    private TextView tvEmpty;

    private DeviceListAdapter deviceListAdapter;
    private DatabaseManager databaseManager;
    private TerminalDao terminalDao;
    private List<Terminal> allTerminals = new ArrayList<>();

    public static DeviceListFragment newInstance() {
        return new DeviceListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_list, container, false);
        initViews(view);
        initData();
        setupRecyclerView();
        setupClickListeners();
        loadTerminals();
        return view;
    }

    private void initViews(View view) {
        btnSearchTerminal = view.findViewById(R.id.btn_search_terminal);
        btnBack = view.findViewById(R.id.btn_back);
        etSearch = view.findViewById(R.id.et_search);
        btnSearch = view.findViewById(R.id.btn_search);
        rvTerminals = view.findViewById(R.id.rv_terminals);
        tvEmpty = view.findViewById(R.id.tv_empty);
    }

    private void initData() {
        databaseManager = DatabaseManager.getInstance(requireContext());
        terminalDao = new TerminalDao(DatabaseHelper.getInstance(requireContext()));
    }

    private void setupRecyclerView() {
        deviceListAdapter = new DeviceListAdapter();
        rvTerminals.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTerminals.setAdapter(deviceListAdapter);

        // 设置添加终端点击事件
        deviceListAdapter.setOnItemClickListener(terminal -> {
            // 跳转到添加设备Fragment
            AddDeviceFragment addDeviceFragment = AddDeviceFragment.newInstance(terminal.getDeviceId());
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, addDeviceFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });
    }

    private void setupClickListeners() {
        // 搜索终端按钮
        btnSearchTerminal.setOnClickListener(v -> {
            // 这里可以实现搜索终端的功能，暂时显示提示
            Toast.makeText(getContext(), "搜索终端功能", Toast.LENGTH_SHORT).show();
        });

        // 返回按钮
        btnBack.setOnClickListener(v -> {
            // 返回上个界面，隐藏设备列表
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).hideDeviceList();
            } else if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        // 搜索按钮
        btnSearch.setOnClickListener(v -> {
            String searchText = etSearch.getText().toString().trim();
            searchTerminals(searchText);
        });
    }

    private void loadTerminals() {
        try {
            // 获取所有终端数据
            allTerminals = terminalDao.getAllTerminals();
            updateUI();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "加载终端数据失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void searchTerminals(String searchText) {
        if (TextUtils.isEmpty(searchText)) {
            // 如果搜索文本为空，显示所有终端
            deviceListAdapter.setTerminals(allTerminals);
        } else {
            // 根据设备ID或设备名称搜索
            List<Terminal> filteredTerminals = new ArrayList<>();
            for (Terminal terminal : allTerminals) {
                if (terminal.getDeviceId().toLowerCase().contains(searchText.toLowerCase()) ||
                    terminal.getDeviceName().toLowerCase().contains(searchText.toLowerCase())) {
                    filteredTerminals.add(terminal);
                }
            }
            deviceListAdapter.setTerminals(filteredTerminals);
        }
        updateEmptyView();
    }

    private void updateUI() {
        deviceListAdapter.setTerminals(allTerminals);
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (deviceListAdapter.getItemCount() == 0) {
            rvTerminals.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rvTerminals.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 重新加载数据，以防有新的终端添加
        loadTerminals();
    }
}