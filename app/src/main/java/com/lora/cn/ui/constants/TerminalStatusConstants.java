package com.lora.cn.ui.constants;

import com.lora.cn.R;
import com.lora.cn.ui.model.TerminalStatus;

import java.util.ArrayList;
import java.util.List;

public class TerminalStatusConstants {
    
    public static final String STATUS_IMPORTANT = "重点关注";
    public static final String STATUS_ONLINE = "在线";
    public static final String STATUS_NORMAL_TAKEN = "正常取走";
    public static final String STATUS_ABNORMAL_LOST = "异常丢失";
    public static final String STATUS_LOW_BATTERY = "设备低电量";
    public static final String STATUS_OFFLINE = "离线";
    
    public static List<TerminalStatus> getDefaultStatusList() {
        List<TerminalStatus> statusList = new ArrayList<>();
        statusList.add(new TerminalStatus(STATUS_IMPORTANT, R.mipmap.ic_coll, 0));
        statusList.add(new TerminalStatus(STATUS_ONLINE, R.mipmap.ic_xh_3, 120));
        statusList.add(new TerminalStatus(STATUS_NORMAL_TAKEN, R.mipmap.ic_blue_right, 85));
        statusList.add(new TerminalStatus(STATUS_ABNORMAL_LOST, R.mipmap.ic_ds, 3));
        statusList.add(new TerminalStatus(STATUS_LOW_BATTERY, R.mipmap.ic_red_sd, 8));
        statusList.add(new TerminalStatus(STATUS_OFFLINE, R.mipmap.ic_xh_no, 12));
        return statusList;
    }
}