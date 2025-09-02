package com.lora.cn.ui.adapter;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.database.entity.Department;

/**
 * 科室列表适配器
 */
public class DepartmentAdapter extends BaseQuickAdapter<Department, QuickViewHolder> {

    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int position, @Nullable Department department) {
        if (department == null) return;
        
        // 设置科室名称
        holder.setText(R.id.tv_department_name, department.getDepartmentName());
        
        // 设置排序
        holder.setText(R.id.tv_department_sort, String.valueOf(department.getSortOrder()));
        
        // 设置状态开关
        SwitchCompat switchStatus = holder.getView(R.id.switch_department_status);
        switchStatus.setChecked(department.getStatus() == 1);

    }
    
    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull android.view.ViewGroup viewGroup, int viewType) {
        return new QuickViewHolder(R.layout.item_department, viewGroup);
    }
    
    /**
     * 添加科室
     */
    public void addDepartment(Department department) {
        if (department != null) {
            add(department);
        }
    }
    
    /**
     * 更新科室
     */
    public void updateDepartment(Department updatedDepartment) {
        if (updatedDepartment != null) {
            for (int i = 0; i < getItemCount(); i++) {
                Department department = getItem(i);
                if (department != null && department.getDepartmentId() == updatedDepartment.getDepartmentId()) {
                    set(i, updatedDepartment);
                    break;
                }
            }
        }
    }
    
    /**
     * 删除科室
     */
    public void removeDepartment(long departmentId) {
        for (int i = 0; i < getItemCount(); i++) {
            Department department = getItem(i);
            if (department != null && department.getDepartmentId() == departmentId) {
                removeAt(i);
                break;
            }
        }
    }
}