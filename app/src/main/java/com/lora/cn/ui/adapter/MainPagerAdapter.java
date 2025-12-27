package com.lora.cn.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.lora.cn.ui.fragment.MaintenanceHomeListFragment;
import com.lora.cn.ui.fragment.TerminalListFragment;
import com.lora.cn.ui.fragment.LogInfoFragment;
import com.lora.cn.ui.fragment.TerminalCheckFragment;
import com.lora.cn.ui.fragment.SettingsFragment;
import com.lora.cn.ui.fragment.DownlinkTestFragment;
import com.lora.cn.ui.fragment.UplinkParseFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new TerminalListFragment();
            case 1:
                return new TerminalCheckFragment();
            case 2:
                return new LogInfoFragment();
            case 3:
                return new MaintenanceHomeListFragment();
            case 4:
                return new SettingsFragment();
            case 5:
                return new DownlinkTestFragment();
            case 6:
                return new UplinkParseFragment();
            default:
                return new TerminalListFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 7;
    }
}
