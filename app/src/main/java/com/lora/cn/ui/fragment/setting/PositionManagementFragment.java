package com.lora.cn.ui.fragment.setting;

import android.app.AlertDialog;
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
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.DatabaseManager;
import com.lora.cn.database.entity.Position;
import com.lora.cn.database.entity.User;
import com.lora.cn.ui.adapter.PositionAdapter;
import com.blankj.utilcode.util.SPUtils;
import com.lora.cn.utils.DialogUtils;

import java.util.Date;
import java.util.List;

/**
 * 职位管理Fragment
 */
public class PositionManagementFragment extends Fragment {

    private RecyclerView rvPositions;
    private PositionAdapter positionAdapter;
    private TextView btnAddPosition;
    private TextView btnBack;
    private DatabaseManager databaseManager;
    private int currentUserRoleId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_position_management, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadPositions();
        
        return view;
    }

    private void initViews(View view) {
        // 工具栏
        btnAddPosition = view.findViewById(R.id.btn_add_position);
        btnBack = view.findViewById(R.id.back);
        
        // 列表
        rvPositions = view.findViewById(R.id.rv_positions);
        
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
    }
    
    private void setupRecyclerView() {
        positionAdapter = new PositionAdapter();
        rvPositions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPositions.setAdapter(positionAdapter);
    }
    
    private void setupListeners() {
        // 返回按钮
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
        
        // 新增按钮
        btnAddPosition.setOnClickListener(v -> {
            if (hasPermission("position_add")) {
                showAddPositionDialog();
            } else {
                Toast.makeText(requireContext(), "您没有新增职位的权限", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 设置适配器点击监听器
        positionAdapter.addOnItemChildClickListener(R.id.tv_position_fz, (baseQuickAdapter, view, i) -> {
            Position position = baseQuickAdapter.getItem(i);
            if (position != null) {
                if (hasPermission("position_add")) {
                    copyPosition(position);
                } else {
                    Toast.makeText(requireContext(), "您没有复制职位的权限", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        positionAdapter.addOnItemChildClickListener(R.id.tv_position_edit, (baseQuickAdapter, view, i) -> {
            Position position = baseQuickAdapter.getItem(i);
            if (position != null) {
                if (hasPermission("position_edit")) {
                    showEditPositionDialog(position);
                } else {
                    Toast.makeText(requireContext(), "您没有编辑职位的权限", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        positionAdapter.addOnItemChildClickListener(R.id.tv_position_delete, (baseQuickAdapter, view, i) -> {
            Position position = baseQuickAdapter.getItem(i);
            if (position != null) {
                if (hasPermission("position_delete")) {
                    showDeleteConfirmDialog(position);
                } else {
                    Toast.makeText(requireContext(), "您没有删除职位的权限", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        positionAdapter.addOnItemChildClickListener(R.id.switch_position_status, (baseQuickAdapter, view, i) -> {
            Position position = baseQuickAdapter.getItem(i);
            if (position != null) {
                if (hasPermission("position_edit")) {
                    SwitchCompat switchStatus = (SwitchCompat) view;
                    togglePositionStatus(position, switchStatus.isChecked());
                } else {
                    Toast.makeText(requireContext(), "您没有修改职位状态的权限", Toast.LENGTH_SHORT).show();
                    // 恢复开关状态
                    SwitchCompat switchStatus = (SwitchCompat) view;
                    switchStatus.setChecked(!switchStatus.isChecked());
                }
            }
        });
    }



    /**
     * 加载职位列表
     */
    private void loadPositions() {
        try {
            List<Position> positions = databaseManager.getAllPositions();
            positionAdapter.submitList(positions);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "加载职位失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示新增职位对话框
     */
    private void showAddPositionDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_position, null);
        EditText etPositionName = dialogView.findViewById(R.id.et_position_name);
        EditText etSortOrder = dialogView.findViewById(R.id.et_sort_order);
        SwitchCompat switchStatus = dialogView.findViewById(R.id.switch_status);

        // 设置默认值
        etSortOrder.setText("1");
        switchStatus.setChecked(true);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("新增职位")
                .setView(dialogView)
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String positionName = etPositionName.getText().toString().trim();
                String sortOrderStr = etSortOrder.getText().toString().trim();
                boolean status = switchStatus.isChecked();

                if (TextUtils.isEmpty(positionName)) {
                    Toast.makeText(requireContext(), "请输入职位名称", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(sortOrderStr)) {
                    Toast.makeText(requireContext(), "请输入排序数字", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int sortOrder = Integer.parseInt(sortOrderStr);
                    addPosition(positionName, sortOrder, status ? 1 : 0);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "排序必须是数字", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    /**
     * 显示编辑职位对话框
     */
    private void showEditPositionDialog(Position position) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_position, null);
        EditText etPositionName = dialogView.findViewById(R.id.et_position_name);
        EditText etSortOrder = dialogView.findViewById(R.id.et_sort_order);
        SwitchCompat switchStatus = dialogView.findViewById(R.id.switch_status);

        // 设置当前值
        etPositionName.setText(position.getPositionName());
        etSortOrder.setText(String.valueOf(position.getSortOrder()));
        switchStatus.setChecked(position.getStatus() == 1);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("编辑职位")
                .setView(dialogView)
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String positionName = etPositionName.getText().toString().trim();
                String sortOrderStr = etSortOrder.getText().toString().trim();
                boolean status = switchStatus.isChecked();

                if (TextUtils.isEmpty(positionName)) {
                    Toast.makeText(requireContext(), "请输入职位名称", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(sortOrderStr)) {
                    Toast.makeText(requireContext(), "请输入排序数字", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int sortOrder = Integer.parseInt(sortOrderStr);
                    updatePosition(position.getPositionId(), positionName, sortOrder, status ? 1 : 0);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "排序必须是数字", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog(Position position) {
        DialogUtils.showConfirmDialog(getContext(), "确认删除", "确定要删除职位 \"" + position.getPositionName() + "\" 吗？", new DialogUtils.OnConfirmDialogListener() {
            @Override
            public void onConfirm() {
                deletePosition(position.getPositionId());
            }

            @Override
            public void onCancel() {
                // 取消操作
            }
        });
    }

    /**
     * 新增职位
     */
    private void addPosition(String positionName, int sortOrder, int status) {
        try {
            // 检查职位名称是否已存在
            if (databaseManager.isPositionNameExists(positionName)) {
                Toast.makeText(requireContext(), "职位名称已存在", Toast.LENGTH_SHORT).show();
                return;
            }

            Position position = new Position();
            position.setPositionName(positionName);
            position.setSortOrder(sortOrder);
            position.setStatus(status);
            position.setCreateTime(new Date());
            position.setUpdateTime(new Date());

            long result = databaseManager.insertPosition(position);
            if (result > 0) {
                Toast.makeText(requireContext(), "职位添加成功", Toast.LENGTH_SHORT).show();
                loadPositions();
            } else {
                Toast.makeText(requireContext(), "职位添加失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "添加职位失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 更新职位
     */
    private void updatePosition(long positionId, String positionName, int sortOrder, int status) {
        try {
            Position position = databaseManager.getPositionById(positionId);
            if (position != null) {
                // 如果名称有变化，检查新名称是否已存在
                if (!position.getPositionName().equals(positionName) && databaseManager.isPositionNameExists(positionName)) {
                    Toast.makeText(requireContext(), "职位名称已存在", Toast.LENGTH_SHORT).show();
                    return;
                }

                position.setPositionName(positionName);
                position.setSortOrder(sortOrder);
                position.setStatus(status);
                position.setUpdateTime(new Date());

                int result = databaseManager.updatePosition(position);
                if (result > 0) {
                    Toast.makeText(requireContext(), "职位更新成功", Toast.LENGTH_SHORT).show();
                    loadPositions();
                } else {
                    Toast.makeText(requireContext(), "职位更新失败", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "更新职位失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 删除职位
     */
    private void deletePosition(long positionId) {
        try {
            int result = databaseManager.deletePosition(positionId);
            if (result > 0) {
                Toast.makeText(requireContext(), "职位删除成功", Toast.LENGTH_SHORT).show();
                loadPositions();
            } else {
                Toast.makeText(requireContext(), "职位删除失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "删除职位失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 复制职位
     */
    private void copyPosition(Position originalPosition) {
        try {
            String newPositionName = originalPosition.getPositionName() + "_副本";

            // 确保复制的名称不重复
            int copyCount = 1;
            while (databaseManager.isPositionNameExists(newPositionName)) {
                copyCount++;
                newPositionName = originalPosition.getPositionName() + "_副本" + copyCount;
            }

            Position newPosition = new Position();
            newPosition.setPositionName(newPositionName);
            newPosition.setSortOrder(originalPosition.getSortOrder());
            newPosition.setStatus(originalPosition.getStatus());
            newPosition.setCreateTime(new Date());
            newPosition.setUpdateTime(new Date());

            long result = databaseManager.insertPosition(newPosition);
            if (result > 0) {
                Toast.makeText(requireContext(), "职位复制成功", Toast.LENGTH_SHORT).show();
                loadPositions();
            } else {
                Toast.makeText(requireContext(), "职位复制失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "复制职位失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 切换职位状态
     */
    private void togglePositionStatus(Position position, boolean isChecked) {
        int newStatus = isChecked ? 1 : 0;
        updatePosition(position.getPositionId(), position.getPositionName(), 
                       position.getSortOrder(), newStatus);
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

    public static PositionManagementFragment newInstance() {
        return new PositionManagementFragment();
    }
}