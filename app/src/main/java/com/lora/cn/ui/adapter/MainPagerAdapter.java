package com.lora.cn.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.lora.cn.ui.fragment.TerminalListFragment;
import com.lora.cn.ui.fragment.LogInfoFragment;
import com.lora.cn.ui.fragment.TerminalCheckFragment;
import com.lora.cn.ui.fragment.SettingsFragment;
import com.lora.cn.ui.fragment.DownlinkTestFragment;

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
                return new SettingsFragment();
            case 4:
                return new DownlinkTestFragment();
            default:
                return new TerminalListFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
