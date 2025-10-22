package com.lora.cn.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.lora.cn.R;
import com.lora.cn.ui.fragment.DeviceListFragment;

/**
 * 设备列表Activity
 */
public class DeviceListActivity extends AppCompatActivity {

    public static void start(Context context) {
        Intent intent = new Intent(context, DeviceListActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        // 加载设备列表Fragment
        if (savedInstanceState == null) {
            DeviceListFragment deviceListFragment = new DeviceListFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, deviceListFragment)
                    .commit();
        }
    }
}