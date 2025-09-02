package com.lora.cn.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lora.cn.R;
import com.lora.cn.database.entity.Permission;
import com.lora.cn.database.entity.Role;
import com.lora.cn.ui.adapter.RoleAdapter;
import com.lora.cn.ui.adapter.RoleTreeAdapter;
import com.lora.cn.ui.adapter.WifiListAdapter;
import com.lora.cn.ui.model.WifiItem;

import java.util.List;

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
            //editNumber.setSelection(currentValue.length());
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
     * 显示数字编辑对话框
     * @param context 上下文
     * @param title 对话框标题
     * @param currentValue 当前数值
     * @param listener 回调监听器
     */
    public static void showTxtEditDialog(Context context, String title, String hint, String currentValue, String unit, OnNumberEditListener listener) {
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
            //editNumber.setSelection(currentValue.length());
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
//                int number = Integer.parseInt(inputValue);
//                if (number < 0 || number > 999) {
//                    Toast.makeText(context, "请输入0-999之间的数字", Toast.LENGTH_SHORT).show();
//                    return;
//                }

                dialog.dismiss();
                if (listener != null) {
                    listener.onConfirm(inputValue);
                }

            } catch (NumberFormatException e) {
                Toast.makeText(context, "请输入有效文字", Toast.LENGTH_SHORT).show();
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
     * 显示数字编辑对话框的简化版本
     * @param context 上下文
     * @param title 对话框标题
     * @param currentValue 当前数值
     * @param onConfirm 确认回调
     */
    public static void showTextEditDialog(Context context, String title, String hint, String currentValue, String unit, OnConfirmListener onConfirm) {
        showTxtEditDialog(context, title, hint, currentValue, unit, new OnNumberEditListener() {
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
    
    /**
     * 确认对话框回调接口
     */
    public interface OnConfirmDialogListener {
        void onConfirm();
        void onCancel();
    }
    
    /**
     * 显示确认对话框
     * @param context 上下文
     * @param title 对话框标题
     * @param message 对话框消息
     * @param listener 回调监听器
     */
    public static void showConfirmDialog(Context context, String title, String message, OnConfirmDialogListener listener) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("确定", (dialog, which) -> {
            if (listener != null) {
                listener.onConfirm();
            }
        });
        builder.setNegativeButton("取消", (dialog, which) -> {
            if (listener != null) {
                listener.onCancel();
            }
        });
        builder.show();
    }
    
    // 在DialogUtils类中添加以下方法
    
    /**
     * WiFi密码输入对话框回调接口
     */
    public interface OnWifiPasswordListener {
        void onPasswordEntered(String password);
    }
    
    /**
     * 显示WiFi密码输入对话框
     * @param context 上下文
     * @param title 对话框标题
     * @param listener 回调监听器
     */
    public static void showWifiPasswordDialog(Context context, String title, OnWifiPasswordListener listener) {
        // 创建对话框
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // 加载布局
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_wifi_password, null);
        dialog.setContentView(dialogView);
        
        // 设置对话框属性
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(
                (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        
        // 获取控件
        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        ImageView btnClose = dialogView.findViewById(R.id.btn_close);
        EditText editPassword = dialogView.findViewById(R.id.edit_password);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConnect = dialogView.findViewById(R.id.btn_connect);
        
        // 设置标题
        dialogTitle.setText(title);
        
        // 关闭按钮点击事件
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        // 取消按钮点击事件
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        // 连接按钮点击事件
        btnConnect.setOnClickListener(v -> {
            String password = editPassword.getText().toString().trim();
            
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(context, "请输入WiFi密码", Toast.LENGTH_SHORT).show();
                return;
            }
            
            dialog.dismiss();
            if (listener != null) {
                listener.onPasswordEntered(password);
            }
        });
        
        // 显示对话框
        dialog.show();
    }
    
    // 在DialogUtils类中添加以下方法
    
    public interface OnWifiSelectedListener {
        void onWifiSelected(WifiItem wifiItem);
    }
    
    public static void showWifiListDialog(Context context, List<WifiItem> wifiList, OnWifiSelectedListener listener) {
//        Dialog.Builder builder = new Dialog.Builder(context);
//        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_wifi_list, null);
//        builder.setView(dialogView);
//
//        Dialog dialog = builder.create();
//        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 加载布局
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_wifi_list, null);
        dialog.setContentView(dialogView);

        // 设置对话框属性
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        
        RecyclerView recyclerView = dialogView.findViewById(R.id.rv_wifi_list);
        TextView titleText = dialogView.findViewById(R.id.tv_title);
        ImageView closeButton = dialogView.findViewById(R.id.iv_close);
        
        titleText.setText("选择WiFi网络");
        
        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        WifiListAdapter adapter = new WifiListAdapter();
        recyclerView.setAdapter(adapter);
        
        adapter.setOnItemClickListener((adapterView, view, position) -> {
            WifiItem selectedWifi = wifiList.get(position);
            if (listener != null) {
                listener.onWifiSelected(selectedWifi);
            }
            dialog.dismiss();
        });
        
        adapter.submitList(wifiList);
        
        closeButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }


    public static void showRoleDialog(Context context, String title, List<Permission> allPermissions, Role role, OnConfirmListener onConfirm) {
        showRoleDialogs(context, title, allPermissions, role, new OnNumberEditListener() {
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

    public static void showRoleDialogs(Context context, String title, List<Permission> allPermissions, Role role, OnNumberEditListener listener) {
        // 创建对话框
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 加载布局
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_role, null);
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

        EditText editNumber = dialogView.findViewById(R.id.edit_number);

        RecyclerView recyclerView = dialogView.findViewById(R.id.role_recycle);
        RoleTreeAdapter adapter = new RoleTreeAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter.submitList(allPermissions);
        recyclerView.setAdapter(adapter);

        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        // 设置标题和当前值
        dialogTitle.setText(title);

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


}