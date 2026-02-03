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
        String termVer = "终端版本：" + "Ver-20260203-1.7.7";
        if (tvAppVersion != null) tvAppVersion.setText(appVer);
        if (tvTerminalVersion != null) tvTerminalVersion.setText(termVer);
        return view;
    }
}
