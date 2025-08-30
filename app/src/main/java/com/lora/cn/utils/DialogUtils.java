package com.lora.cn.utils;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lora.cn.R;

public class DialogUtils {

    /**
     * 数字编辑对话框回调接口
     */
    public interface OnNumberEditListener {
        void onConfirm(String newValue);
        void onCancel();
    }

    /**
     * 显示数字编辑对话框
     * @param context 上下文
     * @param title 对话框标题
     * @param currentValue 当前数值
     * @param listener 回调监听器
     */
    public static void showNumberEditDialog(Context context, String title, String hint, String currentValue, String unit, OnNumberEditListener listener) {
        // 创建对话框
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // 加载布局
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_number_edit, null);
        dialog.setContentView(dialogView);
        
        // 设置对话框属性
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(
                (int) (context.getResources().getDisplayMetrics().widthPixels * 0.5),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        
        // 获取控件
        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        ImageView btnClose = dialogView.findViewById(R.id.btn_close);
        TextView editNumberHint = dialogView.findViewById(R.id.edit_number_hint);
        TextView editNumberUnit = dialogView.findViewById(R.id.edit_number_unit);
        EditText editNumber = dialogView.findViewById(R.id.edit_number);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);


        editNumberHint.setText(hint);
        // 设置标题和当前值
        dialogTitle.setText(title);
        if (!TextUtils.isEmpty(currentValue)) {
            editNumber.setText(currentValue);
            editNumber.setSelection(currentValue.length());
        }
        editNumberUnit.setVisibility(View.GONE);
        if (!TextUtils.isEmpty(unit)) {
            editNumberUnit.setText(unit);
            editNumberUnit.setVisibility(View.VISIBLE);
        }
        
        // 关闭按钮点击事件
        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onCancel();
            }
        });
        
        // 取消按钮点击事件
        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onCancel();
            }
        });
        
        // 确定按钮点击事件
        btnConfirm.setOnClickListener(v -> {
            String inputValue = editNumber.getText().toString().trim();
            
            if (TextUtils.isEmpty(inputValue)) {
                Toast.makeText(context, "请输入数值", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                int number = Integer.parseInt(inputValue);
                if (number < 0 || number > 999) {
                    Toast.makeText(context, "请输入0-999之间的数字", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                dialog.dismiss();
                if (listener != null) {
                    listener.onConfirm(inputValue);
                }
                
            } catch (NumberFormatException e) {
                Toast.makeText(context, "请输入有效数字", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 显示对话框
        dialog.show();
    }
    
    /**
     * 显示数字编辑对话框的简化版本
     * @param context 上下文
     * @param title 对话框标题
     * @param currentValue 当前数值
     * @param onConfirm 确认回调
     */
    public static void showNumberEditDialog(Context context, String title, String hint, String currentValue, String unit, OnConfirmListener onConfirm) {
        showNumberEditDialog(context, title, hint, currentValue, unit, new OnNumberEditListener() {
            @Override
            public void onConfirm(String newValue) {
                if (onConfirm != null) {
                    onConfirm.onConfirm(newValue);
                }
            }
            
            @Override
            public void onCancel() {
                // 默认不处理取消事件
            }
        });
    }
    
    /**
     * 简化的确认回调接口
     */
    public interface OnConfirmListener {
        void onConfirm(String newValue);
    }
}