package com.lora.cn.ui.adapter;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.database.entity.User;

/**
 * 用户列表适配器
 */
public class UserAdapter extends BaseQuickAdapter<User, QuickViewHolder> {

    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int position, @Nullable User user) {
        if (user == null) return;
        
        // 设置用户姓名
        holder.setText(R.id.tv_user_name, user.getUserName());
        
        // 设置用户账号
        holder.setText(R.id.tv_user_account, user.getUserAccount());
        
        // 设置用户角色
        if (user.getRole() != null) {
            holder.setText(R.id.tv_user_role, user.getRole().getRoleName());
        } else {
            holder.setText(R.id.tv_user_role, "未分配角色");
        }
        
        // 设置状态开关
        SwitchCompat switchStatus = holder.getView(R.id.switch_user_status);
        switchStatus.setChecked(user.getStatus() == 1);
        String acc = user.getUserAccount() == null ? "" : user.getUserAccount().trim();
        if ("admin".equals(acc)) {
            holder.setVisible(R.id.tv_user_edit, false);
            holder.setVisible(R.id.tv_user_delete, false);
            holder.setVisible(R.id.tv_user_reset_password, false);
            holder.setVisible(R.id.switch_user_status, false);
        }
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int viewType) {
        return new QuickViewHolder(R.layout.item_user, viewGroup);
    }
    
    /**
     * 添加用户
     */
    public void addUser(User user) {
        if (user != null) {
            add(user);
        }
    }
    
    /**
     * 更新用户
     */
    public void updateUser(User updatedUser) {
        if (updatedUser != null) {
            for (int i = 0; i < getItemCount(); i++) {
                User user = getItem(i);
                if (user != null && user.getUserId() == updatedUser.getUserId()) {
                    set(i, updatedUser);
                    break;
                }
            }
        }
    }
    
    /**
     * 删除用户
     */
    public void removeUser(long userId) {
        for (int i = 0; i < getItemCount(); i++) {
            User user = getItem(i);
            if (user != null && user.getUserId() == userId) {
                removeAt(i);
                break;
            }
        }
    }
}
