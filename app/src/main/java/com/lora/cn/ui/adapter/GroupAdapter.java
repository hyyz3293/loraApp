package com.lora.cn.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.entity.Group;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 分组列表适配器
 */
public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private List<Group> groupList;
    private OnGroupItemClickListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnGroupItemClickListener {
        void onEditClick(Group group);
        void onDeleteClick(Group group);
        void onItemClick(Group group);
    }

    public GroupAdapter() {
        this.groupList = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }

    public void setOnGroupItemClickListener(OnGroupItemClickListener listener) {
        this.listener = listener;
    }

    public void setGroupList(List<Group> groupList) {
        this.groupList = groupList != null ? groupList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addGroup(Group group) {
        if (group != null) {
            groupList.add(group);
            notifyItemInserted(groupList.size() - 1);
        }
    }

    public void updateGroup(Group updatedGroup) {
        if (updatedGroup != null) {
            for (int i = 0; i < groupList.size(); i++) {
                if (groupList.get(i).getGroupId() == updatedGroup.getGroupId()) {
                    groupList.set(i, updatedGroup);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    public void removeGroup(long groupId) {
        for (int i = 0; i < groupList.size(); i++) {
            if (groupList.get(i).getGroupId() == groupId) {
                groupList.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groupList.get(position);
        holder.bind(group);
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        private TextView tvGroupName;
        private TextView tvGroupDescription;
        private TextView tvCategoryCount;
        private TextView tvCreateTime;
        private ImageView btnEdit;
        private ImageView btnDelete;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tv_group_name);
            tvGroupDescription = itemView.findViewById(R.id.tv_group_description);
            tvCategoryCount = itemView.findViewById(R.id.tv_category_count);
            tvCreateTime = itemView.findViewById(R.id.tv_create_time);
            btnEdit = itemView.findViewById(R.id.btn_edit_group);
            btnDelete = itemView.findViewById(R.id.btn_delete_group);
        }

        public void bind(Group group) {
            tvGroupName.setText(group.getGroupName());
            tvGroupDescription.setText(group.getGroupDescription());
            
            // 显示分类数量
            int categoryCount = group.getCategories() != null ? group.getCategories().size() : 0;
            tvCategoryCount.setText(String.valueOf(categoryCount));
            
            // 显示创建时间
            if (group.getCreateTime() != null) {
                tvCreateTime.setText(dateFormat.format(group.getCreateTime()));
            } else {
                tvCreateTime.setText("");
            }

            // 设置点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(group);
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(group);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(group);
                }
            });
        }
    }
}