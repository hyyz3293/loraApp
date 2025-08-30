package com.lora.cn.ui.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.viewholder.QuickViewHolder;
import com.lora.cn.R;
import com.lora.cn.ui.model.SettingItem;

public class TerminalSettingDeviceAdapter extends BaseQuickAdapter<SettingItem, QuickViewHolder> {

    @Override
    protected void onBindViewHolder(@NonNull QuickViewHolder holder, int position, @Nullable SettingItem item) {
        if (item == null) return;

        // 获取所有布局
        LinearLayout layoutNormal = holder.getView(R.id.layout_normal);
        LinearLayout layoutVolume = holder.getView(R.id.layout_volume);
        LinearLayout layoutNumber = holder.getView(R.id.layout_number);

        // 隐藏所有布局
        layoutNormal.setVisibility(View.GONE);
        layoutVolume.setVisibility(View.GONE);
        layoutNumber.setVisibility(View.GONE);

        // 根据viewType显示对应布局
        switch (item.getViewType()) {
            case 0: // 普通文本显示
                layoutNormal.setVisibility(View.VISIBLE);
                TextView settingText = holder.getView(R.id.terminal_setting_text);
                settingText.setText(item.getTitle());
                break;

            case 1: // 音量调节滑动条
                layoutVolume.setVisibility(View.VISIBLE);
                setupVolumeControl(holder, item);
                break;

            case 2: // 数字显示和弹窗编辑
                layoutNumber.setVisibility(View.VISIBLE);
                setupNumberControl(holder, item, position);
                break;
        }
    }

    private void setupVolumeControl(QuickViewHolder holder, SettingItem item) {
        TextView volumeTitle = holder.getView(R.id.volume_title);
        SeekBar volumeSeekBar = holder.getView(R.id.volume_seekbar);
        TextView volumeValue = holder.getView(R.id.volume_value);

        volumeTitle.setText(item.getTitle());
        
        // 设置当前音量值
        int currentVolume = 50; // 默认值
        if (item.getVolume() != null && !item.getVolume().isEmpty()) {
            try {
                currentVolume = Integer.parseInt(item.getVolume());
            } catch (NumberFormatException e) {
                currentVolume = 50;
            }
        }
        
        volumeSeekBar.setProgress(currentVolume);
        volumeValue.setText(String.valueOf(currentVolume) + "%");

        // 设置滑动监听
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    volumeValue.setText(String.valueOf(progress));
                    item.setVolume(String.valueOf(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 可以在这里保存音量设置
                Context context = seekBar.getContext();
                Toast.makeText(context, "音量设置为: " + seekBar.getProgress(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNumberControl(QuickViewHolder holder, SettingItem item, int position) {
        TextView numberTitle = holder.getView(R.id.number_title);
        TextView numberValue = holder.getView(R.id.number_value);
        LinearLayout layoutNumber = holder.getView(R.id.layout_number);

        numberTitle.setText(item.getTitle());
        
        // 设置当前数值
        String currentNum = "2"; // 默认值
        if (item.getNum() != null && !item.getNum().isEmpty()) {
            currentNum = item.getNum();
        }
        numberValue.setText(currentNum + "次");

        // 设置点击监听，弹出编辑对话框
        layoutNumber.setOnClickListener(v -> showNumberEditDialog(v.getContext(), item, numberValue));
    }

    private void showNumberEditDialog(Context context, SettingItem item, TextView numberValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("修改 " + item.getTitle());

        // 创建输入框
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(item.getNum() != null ? item.getNum() : "5");
        input.setSelection(input.getText().length());
        builder.setView(input);

        // 设置按钮
        builder.setPositiveButton("确定", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (!newValue.isEmpty()) {
                try {
                    int num = Integer.parseInt(newValue);
                    if (num >= 0 && num <= 999) {
                        item.setNum(newValue);
                        numberValue.setText(newValue);
                        Toast.makeText(context, "设置成功: " + newValue, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "请输入0-999之间的数字", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "请输入有效数字", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int viewType) {
        return new QuickViewHolder(R.layout.item_terminal_setting_device, viewGroup);
    }
}