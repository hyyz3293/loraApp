package com.lora.cn.ui.fragment.setting;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lora.cn.R;

public class VersionInfoFragment extends Fragment {

    public static VersionInfoFragment newInstance() {
        return new VersionInfoFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_version_info, container, false);
        View back = view.findViewById(R.id.back);
        TextView tvAppName = view.findViewById(R.id.tv_app_name);
        TextView tvAppVersion = view.findViewById(R.id.tv_app_version);
        TextView tvTerminalVersion = view.findViewById(R.id.tv_terminal_version);
        if (back != null) {
            back.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }
        if (tvAppName != null) {
            tvAppName.setText(getString(R.string.app_name));
        }
        String appVer = "App版本：" + com.lora.cn.BuildConfig.VERSION_NAME + " (" + com.lora.cn.BuildConfig.VERSION_CODE + ")";
        String fw = com.lora.cn.utils.LoRaFrameParser.normalizeFirmwareVersionString(
                com.blankj.utilcode.util.SPUtils.getInstance().getString("terminal_firmware_version", "")
        );
        if (tvAppVersion != null) tvAppVersion.setText(appVer);
        if (tvTerminalVersion != null) {
            if (fw.isEmpty()) {
                tvTerminalVersion.setText("");
                tvTerminalVersion.setVisibility(View.GONE);
            } else {
                tvTerminalVersion.setText("终端版本：" + fw);
                tvTerminalVersion.setVisibility(View.GONE);
            }
        }
        return view;
    }
}
