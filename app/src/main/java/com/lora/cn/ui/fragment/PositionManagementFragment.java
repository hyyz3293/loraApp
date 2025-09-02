package com.lora.cn.ui.fragment;

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
import com.lora.cn.ui.adapter.PositionAdapter;
import com.lora.cn.utils.DialogUtils;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_position_management, container, false);
        initViews(view);
        initData();
        return view;
    }

    private void initViews(View view) {
        rvPositions = view.findViewById(R.id.rv_positions);
        btnAddPosition = view.findViewById(R.id.btn_add_position);
        btnBack = view.findViewById(R.id.back);

        // 设置RecyclerView
        rvPositions.setLayoutManager(new LinearLayoutManager(getContext()));
        positionAdapter = new PositionAdapter();
        positionAdapter.setAnimationEnable(true);
        rvPositions.setAdapter(positionAdapter);

        // 设置点击事件
        btnAddPosition.setOnClickListener(v -> showAddPositionDialog());
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

//        // 设置列表项点击事件
//        positionAdapter.setOnItemChildClickListener(new OnItemChildClickListener() {
//            @Override
//            public void onItemChildClick(@NonNull com.chad.library.adapter.base.BaseQuickAdapter adapter, @NonNull View view, int position) {
//                Position positionItem = positionAdapter.getItem(position);
//                if (positionItem == null) return;
//
//                int viewId = view.getId();
//                if (viewId == R.id.tv_position_fz) {
//                    // 复制职位
//                    copyPosition(positionItem);
//                } else if (viewId == R.id.tv_position_edit) {
//                    // 编辑职位
//                    showEditPositionDialog(positionItem);
//                } else if (viewId == R.id.tv_position_delete) {
//                    // 删除职位
//                    showDeleteConfirmDialog(positionItem);
//                } else if (viewId == R.id.switch_position_status) {
//                    // 切换状态
//                    togglePositionStatus(positionItem);
//                }
//            }
//        });
    }

    private void initData() {
        databaseManager = DatabaseManager.getInstance(getContext());
        loadPositions();
    }

    /**
     * 加载职位列表
     */
    private void loadPositions() {
        try {
            List<Position> positions = databaseManager.getAllPositions();
            positionAdapter.submitList(positions);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "加载职位列表失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getContext(), "请输入职位名称", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(sortOrderStr)) {
                    Toast.makeText(getContext(), "请输入排序数字", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int sortOrder = Integer.parseInt(sortOrderStr);
                    addPosition(positionName, sortOrder, status ? 1 : 0);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "排序必须是数字", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getContext(), "请输入职位名称", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(sortOrderStr)) {
                    Toast.makeText(getContext(), "请输入排序数字", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int sortOrder = Integer.parseInt(sortOrderStr);
                    updatePosition(position.getPositionId(), positionName, sortOrder, status ? 1 : 0);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "排序必须是数字", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog(Position position) {
        DialogUtils.showConfirmDialog(getContext(), "确认删除", "确定要删除职位 \"" + position.getPositionName() + "\" 吗？", new DialogUtils.OnConfirmListener() {
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
                Toast.makeText(getContext(), "职位名称已存在", Toast.LENGTH_SHORT).show();
                return;
            }

            Position position = new Position();
            position.setPositionName(positionName);
            position.setSortOrder(sortOrder);
            position.setStatus(status);
            position.setCreateTime(System.currentTimeMillis());
            position.setUpdateTime(System.currentTimeMillis());

            long result = databaseManager.addPosition(position);
            if (result > 0) {
                position.setPositionId(result);
                positionAdapter.addPosition(position);
                Toast.makeText(getContext(), "职位添加成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "职位添加失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "添加职位时发生错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getContext(), "职位名称已存在", Toast.LENGTH_SHORT).show();
                    return;
                }

                position.setPositionName(positionName);
                position.setSortOrder(sortOrder);
                position.setStatus(status);
                position.setUpdateTime(System.currentTimeMillis());

                boolean result = databaseManager.updatePosition(position);
                if (result) {
                    positionAdapter.updatePosition(position);
                    Toast.makeText(getContext(), "职位更新成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "职位更新失败", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "更新职位时发生错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 删除职位
     */
    private void deletePosition(long positionId) {
        try {
            boolean result = databaseManager.deletePosition(positionId);
            if (result) {
                positionAdapter.removePosition(positionId);
                Toast.makeText(getContext(), "职位删除成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "职位删除失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "删除职位时发生错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            newPosition.setCreateTime(System.currentTimeMillis());
            newPosition.setUpdateTime(System.currentTimeMillis());

            long result = databaseManager.addPosition(newPosition);
            if (result > 0) {
                newPosition.setPositionId(result);
                positionAdapter.addPosition(newPosition);
                Toast.makeText(getContext(), "职位复制成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "职位复制失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "复制职位时发生错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 切换职位状态
     */
    private void togglePositionStatus(Position position) {
        try {
            int newStatus = position.getStatus() == 1 ? 0 : 1;
            position.setStatus(newStatus);
            position.setUpdateTime(System.currentTimeMillis());

            boolean result = databaseManager.updatePosition(position);
            if (result) {
                positionAdapter.updatePosition(position);
                String statusText = newStatus == 1 ? "启用" : "禁用";
                Toast.makeText(getContext(), "职位已" + statusText, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "状态更新失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "更新状态时发生错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}