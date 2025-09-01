package com.lora.cn.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.database.entity.Category;
import com.lora.cn.database.entity.Group;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 分类列表适配器
 */
public class CategoryAdapter extends BaseQuickAdapter<Category, QuickViewHolder> {
        @Override
        protected void onBindViewHolder(@NonNull QuickViewHolder holder, int i, @Nullable Category group) {
            holder.setText(R.id.tv_group_name, group.getCategoryName());

            holder.setText(R.id.tv_group_fz, "");
        }

        @NonNull
        @Override
        protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int i) {
            return new QuickViewHolder(R.layout.item_group, viewGroup);
        }

//    private List<Category> categoryList;
//    private OnCategoryItemClickListener listener;
//    private SimpleDateFormat dateFormat;
//
//    public interface OnCategoryItemClickListener {
//        void onEditClick(Category category);
//        void onDeleteClick(Category category);
//        void onItemClick(Category category);
//    }
//
//    public CategoryAdapter() {
//        this.categoryList = new ArrayList<>();
//        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
//    }
//
//    public void setOnCategoryItemClickListener(OnCategoryItemClickListener listener) {
//        this.listener = listener;
//    }
//
//    public void setCategoryList(List<Category> categoryList) {
//        this.categoryList = categoryList != null ? categoryList : new ArrayList<>();
//        notifyDataSetChanged();
//    }
//
//    public void addCategory(Category category) {
//        if (category != null) {
//            categoryList.add(category);
//            notifyItemInserted(categoryList.size() - 1);
//        }
//    }
//
//    public void updateCategory(Category updatedCategory) {
//        if (updatedCategory != null) {
//            for (int i = 0; i < categoryList.size(); i++) {
//                if (categoryList.get(i).getCategoryId() == updatedCategory.getCategoryId()) {
//                    categoryList.set(i, updatedCategory);
//                    notifyItemChanged(i);
//                    break;
//                }
//            }
//        }
//    }
//
//    public void removeCategory(long categoryId) {
//        for (int i = 0; i < categoryList.size(); i++) {
//            if (categoryList.get(i).getCategoryId() == categoryId) {
//                categoryList.remove(i);
//                notifyItemRemoved(i);
//                break;
//            }
//        }
//    }
//
//    public void filterByGroup(long groupId) {
//        List<Category> filteredList = new ArrayList<>();
//        for (Category category : categoryList) {
//            if (category.getGroupId() == groupId) {
//                filteredList.add(category);
//            }
//        }
//        setCategoryList(filteredList);
//    }
//
//    @NonNull
//    @Override
//    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_category, parent, false);
//        return new CategoryViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
//        Category category = categoryList.get(position);
//        holder.bind(category);
//    }
//
//    @Override
//    public int getItemCount() {
//        return categoryList.size();
//    }
//
//    class CategoryViewHolder extends RecyclerView.ViewHolder {
//        private TextView tvCategoryName;
//        private TextView tvCategoryDescription;
//        private TextView tvGroupName;
//        private TextView tvCreateTime;
//        private ImageView btnEdit;
//        private ImageView btnDelete;
//
//        public CategoryViewHolder(@NonNull View itemView) {
//            super(itemView);
//            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
//            tvCategoryDescription = itemView.findViewById(R.id.tv_category_description);
//            tvGroupName = itemView.findViewById(R.id.tv_group_name);
//            tvCreateTime = itemView.findViewById(R.id.tv_create_time);
//            btnEdit = itemView.findViewById(R.id.btn_edit_category);
//            btnDelete = itemView.findViewById(R.id.btn_delete_category);
//        }
//
//        public void bind(Category category) {
//            tvCategoryName.setText(category.getCategoryName());
//            tvCategoryDescription.setText(category.getCategoryDescription());
//
//            // 显示所属分组名称
//            if (category.getGroup() != null) {
//                tvGroupName.setText(category.getGroup().getGroupName());
//            } else {
//                tvGroupName.setText("未知分组");
//            }
//
//            // 显示创建时间
//            if (category.getCreateTime() != null) {
//                tvCreateTime.setText(dateFormat.format(category.getCreateTime()));
//            } else {
//                tvCreateTime.setText("");
//            }
//
//            // 设置点击事件
//            itemView.setOnClickListener(v -> {
//                if (listener != null) {
//                    listener.onItemClick(category);
//                }
//            });
//
//            btnEdit.setOnClickListener(v -> {
//                if (listener != null) {
//                    listener.onEditClick(category);
//                }
//            });
//
//            btnDelete.setOnClickListener(v -> {
//                if (listener != null) {
//                    listener.onDeleteClick(category);
//                }
//            });
//        }
//    }
}