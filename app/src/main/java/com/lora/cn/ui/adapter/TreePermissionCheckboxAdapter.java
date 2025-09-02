package com.lora.cn.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.entity.Permission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 树形权限选择适配器
 */
public class TreePermissionCheckboxAdapter extends RecyclerView.Adapter<TreePermissionCheckboxAdapter.ViewHolder> {

    private List<Permission> displayPermissions; // 显示的权限列表（按层级排序）
    private List<Permission> allPermissions; // 所有权限列表
    private Map<Long, List<Permission>> childrenMap; // 父权限ID -> 子权限列表
    private Set<Long> selectedPermissionIds;
    private Set<Long> expandedPermissionIds; // 展开的权限ID

    public TreePermissionCheckboxAdapter() {
        this.displayPermissions = new ArrayList<>();
        this.allPermissions = new ArrayList<>();
        this.childrenMap = new HashMap<>();
        this.selectedPermissionIds = new HashSet<>();
        this.expandedPermissionIds = new HashSet<>();
    }

    public void setPermissions(List<Permission> allPermissions) {
        if (allPermissions == null) {
            allPermissions = new ArrayList<>();
        }
        
        this.allPermissions = new ArrayList<>(allPermissions);
        
        // 构建父子关系映射
        buildChildrenMap(allPermissions);
        
        // 构建显示列表（只显示根权限和已展开的子权限）
        buildDisplayList();
        
        notifyDataSetChanged();
    }

    private void buildChildrenMap(List<Permission> allPermissions) {
        childrenMap.clear();
        
        for (Permission permission : allPermissions) {
            Long parentId = permission.getParentId();
            if (parentId != null && parentId > 0) {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(permission);
            }
        }
        
        // 对每个父权限的子权限按sortOrder排序
        for (List<Permission> children : childrenMap.values()) {
            children.sort((p1, p2) -> Integer.compare(p1.getSortOrder(), p2.getSortOrder()));
        }
    }

    private void buildDisplayList() {
        displayPermissions.clear();
        
        // 查找根权限（parentId为null或0的权限）
        List<Permission> rootPermissions = new ArrayList<>();
        for (Permission permission : allPermissions) {
            if (permission.getParentId() == null || permission.getParentId() == 0) {
                rootPermissions.add(permission);
            }
        }
        
        // 按sortOrder排序根权限
        rootPermissions.sort((p1, p2) -> Integer.compare(p1.getSortOrder(), p2.getSortOrder()));
        
        // 递归添加权限到显示列表
        for (Permission rootPermission : rootPermissions) {
            addPermissionToDisplayList(rootPermission, 0);
        }
    }

    private void addPermissionToDisplayList(Permission permission, int level) {
        displayPermissions.add(permission);
        
        // 如果权限已展开，添加其子权限
        if (expandedPermissionIds.contains(permission.getPermissionId())) {
            List<Permission> children = childrenMap.get(permission.getPermissionId());
            if (children != null) {
                for (Permission child : children) {
                    addPermissionToDisplayList(child, level + 1);
                }
            }
        }
    }

    public void setSelectedPermissions(List<Long> selectedIds) {
        this.selectedPermissionIds.clear();
        if (selectedIds != null) {
            this.selectedPermissionIds.addAll(selectedIds);
        }
        notifyDataSetChanged();
    }

    public List<Long> getSelectedPermissionIds() {
        return new ArrayList<>(selectedPermissionIds);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tree_permission_checkbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Permission permission = displayPermissions.get(position);
        holder.bind(permission);
    }

    @Override
    public int getItemCount() {
        return displayPermissions.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private View indentView;
        private TextView tvExpandIcon;
        private CheckBox cbPermission;
        private TextView tvPermissionName;
        private TextView tvPermissionDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            indentView = itemView.findViewById(R.id.view_indent);
            tvExpandIcon = itemView.findViewById(R.id.tv_expand_icon);
            cbPermission = itemView.findViewById(R.id.cb_permission);
            tvPermissionName = itemView.findViewById(R.id.tv_permission_name);
            tvPermissionDescription = itemView.findViewById(R.id.tv_permission_description);

            // 设置展开/收起点击监听器
            tvExpandIcon.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Permission permission = displayPermissions.get(position);
                    toggleExpansion(permission);
                }
            });

            // 设置权限选择点击监听器
            itemView.setOnClickListener(v -> {
                cbPermission.setChecked(!cbPermission.isChecked());
                togglePermissionSelection();
            });

            cbPermission.setOnCheckedChangeListener((buttonView, isChecked) -> {
                togglePermissionSelection();
            });
        }

        public void bind(Permission permission) {
            // 设置缩进（根据层级）
            int level = permission.getLevel() != null ? permission.getLevel() : 0;
            ViewGroup.LayoutParams params = indentView.getLayoutParams();
            params.width = level * 40; // 每级缩进40dp
            indentView.setLayoutParams(params);

            // 设置展开/收起图标
            List<Permission> children = childrenMap.get(permission.getPermissionId());
            if (children != null && !children.isEmpty()) {
                tvExpandIcon.setVisibility(View.VISIBLE);
                boolean isExpanded = expandedPermissionIds.contains(permission.getPermissionId());
                tvExpandIcon.setText(isExpanded ? "▼" : "▶");
            } else {
                tvExpandIcon.setVisibility(View.INVISIBLE);
            }

            // 设置权限信息
            tvPermissionName.setText(permission.getPermissionName());
            tvPermissionDescription.setText(permission.getDescription() != null ? 
                    permission.getDescription() : "");
            
            // 设置选中状态
            cbPermission.setOnCheckedChangeListener(null); // 临时移除监听器
            cbPermission.setChecked(selectedPermissionIds.contains(permission.getPermissionId()));
            cbPermission.setOnCheckedChangeListener((buttonView, isChecked) -> {
                togglePermissionSelection();
            });
        }

        private void toggleExpansion(Permission permission) {
            Long permissionId = permission.getPermissionId();
            if (expandedPermissionIds.contains(permissionId)) {
                expandedPermissionIds.remove(permissionId);
            } else {
                expandedPermissionIds.add(permissionId);
            }
            
            // 重新构建显示列表
            buildDisplayList();
            notifyDataSetChanged();
        }

        private void togglePermissionSelection() {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                Permission permission = displayPermissions.get(position);
                boolean isChecked = cbPermission.isChecked();
                
                if (isChecked) {
                    selectedPermissionIds.add(permission.getPermissionId());
                    // 选中父权限时，自动选中所有子权限
                    selectAllChildren(permission.getPermissionId());
                } else {
                    selectedPermissionIds.remove(permission.getPermissionId());
                    // 取消选中父权限时，自动取消选中所有子权限
                    deselectAllChildren(permission.getPermissionId());
                }
                
                notifyDataSetChanged();
            }
        }

        private void selectAllChildren(Long parentId) {
            List<Permission> children = childrenMap.get(parentId);
            if (children != null) {
                for (Permission child : children) {
                    selectedPermissionIds.add(child.getPermissionId());
                    selectAllChildren(child.getPermissionId()); // 递归选中子权限
                }
            }
        }

        private void deselectAllChildren(Long parentId) {
            List<Permission> children = childrenMap.get(parentId);
            if (children != null) {
                for (Permission child : children) {
                    selectedPermissionIds.remove(child.getPermissionId());
                    deselectAllChildren(child.getPermissionId()); // 递归取消选中子权限
                }
            }
        }
    }
}