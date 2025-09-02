package com.lora.cn.ui.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.database.entity.Permission;
import com.lora.cn.ui.model.ChartItem;

public class RoleTreeAdapter extends BaseQuickAdapter<Permission, QuickViewHolder> {

    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int position, @Nullable Permission item) {
        holder.setText(R.id.tv_permission_name, item.getPermissionName());
        RecyclerView recyclerView = holder.getView(R.id.role_recycle_tree);
        recyclerView.setVisibility(View.GONE);
        if (item.getChild() != null) {
            recyclerView.setVisibility(View.VISIBLE);
            RoleTreeAdapter adapter = new RoleTreeAdapter();
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter.submitList(item.getChild());
            recyclerView.setAdapter(adapter);
        }


    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup parent, int viewType) {
        return new QuickViewHolder(R.layout.item_role_tree, parent);
    }
}