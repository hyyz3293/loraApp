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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限选择适配器
 */
public class PermissionCheckboxAdapter extends RecyclerView.Adapter<PermissionCheckboxAdapter.ViewHolder> {

    private List<Permission> permissions;
    private Set<Long> selectedPermissionIds;

    public PermissionCheckboxAdapter() {
        this.permissions = new ArrayList<>();
        this.selectedPermissionIds = new HashSet<>();
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions != null ? permissions : new ArrayList<>();
        notifyDataSetChanged();
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
                .inflate(R.layout.item_permission_checkbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Permission permission = permissions.get(position);
        holder.bind(permission);
    }

    @Override
    public int getItemCount() {
        return permissions.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private CheckBox cbPermission;
        private TextView tvPermissionName;
        private TextView tvPermissionDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbPermission = itemView.findViewById(R.id.cb_permission);
            tvPermissionName = itemView.findViewById(R.id.tv_permission_name);
            tvPermissionDescription = itemView.findViewById(R.id.tv_permission_description);

            // 设置点击监听器
            itemView.setOnClickListener(v -> {
                cbPermission.setChecked(!cbPermission.isChecked());
                togglePermissionSelection();
            });

            cbPermission.setOnCheckedChangeListener((buttonView, isChecked) -> {
                togglePermissionSelection();
            });
        }

        public void bind(Permission permission) {
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

        private void togglePermissionSelection() {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                Permission permission = permissions.get(position);
                if (cbPermission.isChecked()) {
                    selectedPermissionIds.add(permission.getPermissionId());
                } else {
                    selectedPermissionIds.remove(permission.getPermissionId());
                }
            }
        }
    }
}