package com.lora.cn.ui.adapter;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.database.entity.Role;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * 角色列表适配器
 */
public class RoleAdapter extends BaseQuickAdapter<Role, QuickViewHolder> {
    
    private SimpleDateFormat dateFormat;
    
    public RoleAdapter() {
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }

    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int position, @Nullable Role role) {
        if (role == null) return;
        
        // 设置角色名称
        holder.setText(R.id.tv_role_name, role.getRoleName());
        if (role.getRoleName() != null && role.getRoleName().trim().equals("管理员")) {
            holder.setVisible(R.id.tv_role_edit, false);
            holder.setVisible(R.id.tv_role_delete, false);
        }
        
        // 设置角色描述
        if (role.getDescription() != null && !role.getDescription().isEmpty()) {
            holder.setText(R.id.tv_role_description, role.getDescription());
            holder.setVisible(R.id.tv_role_description, true);
        } else {
            holder.setVisible(R.id.tv_role_description, false);
        }
        
        // 设置创建时间
        if (role.getCreateTime() != null) {
            holder.setText(R.id.tv_role_create_time, dateFormat.format(role.getCreateTime()));
        }
        
        // 设置状态开关
        SwitchCompat switchStatus = holder.getView(R.id.switch_role_status);
        switchStatus.setChecked(role.getStatus() == 1);
        
        // 设置排序
        holder.setText(R.id.tv_role_sort, String.valueOf(role.getSortOrder()));
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int viewType) {
        return new QuickViewHolder(R.layout.item_role, viewGroup);
    }
    
    /**
     * 添加角色
     */
    public void addRole(Role role) {
        if (role != null) {
            add(role);
        }
    }
    
    /**
     * 更新角色
     */
    public void updateRole(Role updatedRole) {
        if (updatedRole != null) {
            for (int i = 0; i < getItemCount(); i++) {
                Role role = getItem(i);
                if (role != null && role.getRoleId() == updatedRole.getRoleId()) {
                    set(i, updatedRole);
                    break;
                }
            }
        }
    }
    
    /**
     * 删除角色
     */
    public void removeRole(long roleId) {
        for (int i = 0; i < getItemCount(); i++) {
            Role role = getItem(i);
            if (role != null && role.getRoleId() == roleId) {
                removeAt(i);
                break;
            }
        }
    }
}
