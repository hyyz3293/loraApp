package com.lora.cn.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
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
import com.lora.cn.utils.DialogUtils;

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
        android.media.AudioManager am = (android.media.AudioManager) holder.itemView.getContext().getSystemService(Context.AUDIO_SERVICE);
        int sysMax = am != null ? am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) : 15;
        int sysVol = am != null ? am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) : 8;
        int normalized = Math.round(sysVol * 100f / Math.max(1, sysMax));
        volumeSeekBar.setMax(100);
        volumeSeekBar.setProgress(normalized);
        volumeValue.setText(String.valueOf(normalized));

        // 设置滑动监听
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    volumeValue.setText(String.valueOf(progress));
                    item.setVolume(String.valueOf(progress));
                    if (am != null) {
                        int newVol = Math.round(progress * Math.max(1, sysMax) / 100f);
                        newVol = Math.max(0, Math.min(sysMax, newVol));
                        am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVol, 0);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Context context = seekBar.getContext();
                if (am != null) {
                    int newVol = Math.round(seekBar.getProgress() * Math.max(1, sysMax) / 100f);
                    newVol = Math.max(0, Math.min(sysMax, newVol));
                    am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVol, 0);
                }
                Toast.makeText(context, "音量设置为: " + seekBar.getProgress(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNumberControl(QuickViewHolder holder, SettingItem item, int position) {
        TextView numberTitle = holder.getView(R.id.number_title);
        TextView numberValue = holder.getView(R.id.number_value);
        LinearLayout layoutNumber = holder.getView(R.id.layout_number);

        numberTitle.setText(item.getTitle());
        
        // 设置当前数值（从SP读取为准）
        String currentNum = "";
        if (item.getIndex() == 3) {
            int cc = com.blankj.utilcode.util.SPUtils.getInstance().getInt("terminal_check_count", 2);
            currentNum = String.valueOf(cc);
        } else if (item.getIndex() == 4) {
            int lb = com.blankj.utilcode.util.SPUtils.getInstance().getInt("low_battery_threshold_percent", 20);
            currentNum = String.valueOf(lb);
        } else if (item.getIndex() == 5) {
            long sec = com.blankj.utilcode.util.SPUtils.getInstance().getLong("home_auto_return_timeout_sec", 60);
            currentNum = String.valueOf(sec);
        } else if (item.getIndex() == 7) {
            int sleepMin = com.blankj.utilcode.util.SPUtils.getInstance().getInt("device_sleep_interval_min", 3);
            currentNum = String.valueOf(sleepMin);
        } else {
            currentNum = item.getValue() != null ? item.getValue() : "";
        }
        item.setValue(currentNum);
        if (item.getIndex() == 4) {
            numberValue.setText(currentNum + "%");
        } else {
            numberValue.setText(currentNum);
        }

        // 设置点击监听，使用工具类显示对话框
        layoutNumber.setOnClickListener(v -> {
            String title = "";
            String hint = "";
            String value = "";
            String unit = "";
            switch (item.getIndex()) {
                case 3:
                    title = "清点次数设置";
                    hint = "清点次数";
                    value = item.getValue() != null ? item.getValue() : "2";
                    break;
                case 4:
                    title = "低电量报警值";
                    hint = "电量值(0-100)";
                    value = item.getValue() != null ? item.getValue() : "20";
                    unit = "%";
                    break;
                case 5:
                    title = "回到首页时间设置";
                    hint = "回到首页时间";
                    value = item.getValue() != null ? item.getValue() : "60";
                    unit = "秒";
                    break;
                case 7:
                    title = "设备休眠间隔(分钟)";
                    hint = "休眠间隔(3-1440)";
                    value = item.getValue() != null ? item.getValue() : "60";
                    unit = "分钟";
                    break;
            }

            DialogUtils.showNumberEditDialog(
                v.getContext(),
                    title,
                    hint,
                    value,
                    unit,
                    newValue -> {
                        String out = newValue;
                        if (item.getIndex() == 3) {
                            try {
                                int n = Integer.parseInt(newValue);
                                if (n < 1) n = 1;
                                out = String.valueOf(n);
                                com.blankj.utilcode.util.SPUtils.getInstance().put("terminal_check_count", n);
                                try {
                                    com.lora.cn.database.DatabaseHelper db = com.lora.cn.database.DatabaseHelper.getInstance(v.getContext());
                                    com.lora.cn.ui.model.LogInfo li = new com.lora.cn.ui.model.LogInfo();
                                    li.setTerminalId("SYS");
                                    li.setTerminalName("系统设置");
                                    li.setDeviceId("SYS");
                                    li.setStatusCode(0);
                                    li.setOperator(com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", ""));
                                    li.setOperationTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                                    li.setAction("设置: 清点次数=" + out);
                                    li.setCreateTime(li.getOperationTime());
                                    db.addLog(li);
                                } catch (Exception ignored) {}
                            } catch (Exception ignored) {}
                        } else if (item.getIndex() == 4) {
                            try {
                                int n = Integer.parseInt(newValue);
                                out = String.valueOf(n);
                                com.blankj.utilcode.util.SPUtils.getInstance().put("low_battery_threshold_percent", n);
                                try {
                                    com.lora.cn.database.DatabaseHelper db = com.lora.cn.database.DatabaseHelper.getInstance(v.getContext());
                                    com.lora.cn.ui.model.LogInfo li = new com.lora.cn.ui.model.LogInfo();
                                    li.setTerminalId("SYS");
                                    li.setTerminalName("系统设置");
                                    li.setDeviceId("SYS");
                                    li.setStatusCode(0);
                                    li.setOperator(com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", ""));
                                    li.setOperationTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                                    li.setAction("设置: 低电量报警值=" + out + "%");
                                    li.setCreateTime(li.getOperationTime());
                                    db.addLog(li);
                                } catch (Exception ignored) {}
                            } catch (Exception ignored) {}
                        } else if (item.getIndex() == 5) {
                            try {
                                long n = Long.parseLong(newValue);
                                if (n < 0) n = 0;
                                out = String.valueOf(n);
                                com.blankj.utilcode.util.SPUtils.getInstance().put("home_auto_return_timeout_sec", n);
                                try {
                                    com.lora.cn.database.DatabaseHelper db = com.lora.cn.database.DatabaseHelper.getInstance(v.getContext());
                                    com.lora.cn.ui.model.LogInfo li = new com.lora.cn.ui.model.LogInfo();
                                    li.setTerminalId("SYS");
                                    li.setTerminalName("系统设置");
                                    li.setDeviceId("SYS");
                                    li.setStatusCode(0);
                                    li.setOperator(com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", ""));
                                    li.setOperationTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                                    li.setAction("设置: 回到首页时间=" + out + "秒");
                                    li.setCreateTime(li.getOperationTime());
                                    db.addLog(li);
                                } catch (Exception ignored) {}
                            } catch (Exception ignored) {}
                        } else if (item.getIndex() == 7) {
                            try {
                                int n = Integer.parseInt(newValue);
                                if (n < 3) n = 3;
                                if (n > 1440) n = 1440;
                                out = String.valueOf(n);
                                com.blankj.utilcode.util.SPUtils.getInstance().put("device_sleep_interval_min", n);
                                try {
                                    com.lora.cn.database.DatabaseHelper db = com.lora.cn.database.DatabaseHelper.getInstance(v.getContext());
                                    com.lora.cn.ui.model.LogInfo li = new com.lora.cn.ui.model.LogInfo();
                                    li.setTerminalId("SYS");
                                    li.setTerminalName("系统设置");
                                    li.setDeviceId("SYS");
                                    li.setStatusCode(0);
                                    li.setOperator(com.blankj.utilcode.util.SPUtils.getInstance().getString("current_user_name", ""));
                                    li.setOperationTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                                    li.setAction("设置: 设备休眠间隔=" + out + "分钟");
                                    li.setCreateTime(li.getOperationTime());
                                    db.addLog(li);
                                } catch (Exception ignored) {}
                            } catch (Exception ignored) {}
                        }
                        item.setValue(out);
                        if (item.getIndex() == 4) {
                            numberValue.setText(out + "%");
                        } else {
                            numberValue.setText(out);
                        }
                        Toast.makeText(v.getContext(), "设置成功: " + out + (item.getIndex()==4?"%":""), Toast.LENGTH_SHORT).show();
                    }
            );
        });
    }

    @NonNull
    @Override
    protected QuickViewHolder onCreateViewHolder(@NonNull Context context, @NonNull ViewGroup viewGroup, int viewType) {
        return new QuickViewHolder(R.layout.item_terminal_setting_device, viewGroup);
    }
}
