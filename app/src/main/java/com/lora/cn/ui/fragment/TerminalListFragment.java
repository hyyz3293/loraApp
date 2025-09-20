package com.lora.cn.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.User;
import com.lora.cn.ui.adapter.TerminalStatusAdapter;
import com.lora.cn.ui.adapter.TerminalAdapter;
import com.lora.cn.ui.model.TerminalStatus;
import com.lora.cn.ui.model.Terminal;
import com.lora.cn.ui.constants.TerminalStatusConstants;
import com.blankj.utilcode.util.SPUtils;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class TerminalListFragment extends Fragment {

    private RecyclerView rvTerminalStatus;
    private RecyclerView terminalRecycle;
    private TerminalStatusAdapter terminalStatusAdapter;
    private TerminalAdapter terminalAdapter;
    private int currentStatusIndex = 0;
    
    // 权限相关
    private DatabaseManager databaseManager;
    private int currentUserRoleId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal_list, container, false);
        
        // 初始化数据库管理器
        databaseManager = DatabaseManager.getInstance(requireContext());
        
        // 初始化用户角色ID
        long userId = SPUtils.getInstance().getLong("current_user_id", -1);
        if (userId != -1) {
            User user = databaseManager.getUserById(userId);
            if (user != null) {
                currentUserRoleId = (int)user.getRoleId();
            }
        }
        
        initViews(view);
        
        // 检查查看终端列表权限
        if (hasPermission("terminal_view")) {
            initTerminalStatus();
        } else {
            Toast.makeText(requireContext(), "您没有查看终端列表的权限", Toast.LENGTH_SHORT).show();
        }
        
        return view;
    }

    private void initViews(View view) {
        rvTerminalStatus = view.findViewById(R.id.rv_terminal_status);
        terminalRecycle = view.findViewById(R.id.terminal_recycle);
    }

    private void initTerminalStatus() {
        // 使用常量类获取终端状态数据
        List<TerminalStatus> statusList = TerminalStatusConstants.getDefaultStatusList();

        // 设置状态RecyclerView
        LinearLayoutManager statusLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvTerminalStatus.setLayoutManager(statusLayoutManager);
        
        terminalStatusAdapter = new TerminalStatusAdapter();
        rvTerminalStatus.setAdapter(terminalStatusAdapter);
        
        terminalStatusAdapter.submitList(statusList);
        
        // 初始化终端列表
        initTerminalList();
    }
    
    private void initTerminalList() {
        // 创建示例终端数据
        List<Terminal> terminalList = new ArrayList<>();
        terminalList.add(new Terminal("终端001", "科室一", "病房101", R.mipmap.ic_xh_3, "在线", R.mipmap.ic_red_sd, "85%", true));
        terminalList.add(new Terminal("终端002", "科室一", "病房102", R.mipmap.ic_xh_3, "在线", R.mipmap.ic_red_sd, "92%", false));
        terminalList.add(new Terminal("终端003", "科室二", "病房201", R.mipmap.ic_ds, "异常", R.mipmap.ic_red_sd, "15%", true));
        terminalList.add(new Terminal("终端004", "科室二", "病房202", R.mipmap.ic_xh_no, "离线", R.mipmap.ic_red_sd, "0%", false));
        terminalList.add(new Terminal("终端006", "科室二", "病房202", R.mipmap.ic_xh_no, "离线", R.mipmap.ic_red_sd, "0%", false));
        terminalList.add(new Terminal("终端009", "科室二", "病房202", R.mipmap.ic_xh_no, "离线", R.mipmap.ic_red_sd, "0%", false));
        terminalList.add(new Terminal("终端001", "科室一", "病房101", R.mipmap.ic_xh_3, "在线", R.mipmap.ic_red_sd, "85%", true));
        terminalList.add(new Terminal("终端002", "科室一", "病房102", R.mipmap.ic_xh_3, "在线", R.mipmap.ic_red_sd, "92%", false));
        terminalList.add(new Terminal("终端003", "科室二", "病房201", R.mipmap.ic_ds, "异常", R.mipmap.ic_red_sd, "15%", true));
        terminalList.add(new Terminal("终端004", "科室二", "病房202", R.mipmap.ic_xh_no, "离线", R.mipmap.ic_red_sd, "0%", false));
        terminalList.add(new Terminal("终端006", "科室二", "病房202", R.mipmap.ic_xh_no, "离线", R.mipmap.ic_red_sd, "0%", false));
        terminalList.add(new Terminal("终端009", "科室二", "病房202", R.mipmap.ic_xh_no, "离线", R.mipmap.ic_red_sd, "0%", false));

        // 设置终端列表RecyclerView
        GridLayoutManager terminalLayoutManager = new GridLayoutManager(getContext(), 4);
        terminalRecycle.setLayoutManager(terminalLayoutManager);
        
        terminalAdapter = new TerminalAdapter();
        terminalRecycle.setAdapter(terminalAdapter);
        
        terminalAdapter.submitList(terminalList);
        
        // 设置终端点击事件监听器
        terminalAdapter.setOnItemClickListener((adapter, view, position) -> {
            if (hasPermission("terminal_view")) {
                Terminal terminal = terminalList.get(position);
                onTerminalClick(position, terminal);
            } else {
                Toast.makeText(requireContext(), "您没有查看终端详情的权限", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onTerminalClick(int position, Terminal terminal) {
        // TODO: 处理终端点击事件
        // 可以跳转到终端详情页面或显示更多信息
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
}