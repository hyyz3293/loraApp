package com.lora.cn.ui.constants;

import com.lora.cn.R;
import com.lora.cn.ui.model.TerminalStatus;

import java.util.ArrayList;
import java.util.List;

public class TerminalStatusConstants {
    public static final String STATUS_IMPORTANT = "重点关注";
    public static final String STATUS_ONLINE = "在线";
    public static final String STATUS_NORMAL_TAKEN = "正常取走";
    public static final String STATUS_ABNORMAL_LOST = "异常取走";
    public static final String STATUS_LOW_BATTERY = "设备低电量";
    public static final String STATUS_OFFLINE = "离线";

    public static final int CODE_OFFLINE = 0;
    public static final int CODE_ONLINE = 1;
    public static final int CODE_ABNORMAL_TAKEN = 2;
    public static final int CODE_NORMAL_TAKEN = 3;

    public static String codeToText(int code) {
        switch (code) {
            case CODE_ONLINE: return STATUS_ONLINE;
            case CODE_NORMAL_TAKEN: return STATUS_NORMAL_TAKEN;
            case CODE_ABNORMAL_TAKEN: return STATUS_ABNORMAL_LOST;
            case CODE_OFFLINE:
            default: return STATUS_OFFLINE;
        }
    }

    public static int textToCode(String text) {
        if (text == null) return CODE_OFFLINE;
        switch (text) {
            case STATUS_ONLINE: return CODE_ONLINE;
            case STATUS_NORMAL_TAKEN: return CODE_NORMAL_TAKEN;
            case STATUS_ABNORMAL_LOST: return CODE_ABNORMAL_TAKEN;
            case STATUS_OFFLINE:
            default: return CODE_OFFLINE;
        }
    }

    public static List<TerminalStatus> getDefaultStatusList() {
        List<TerminalStatus> statusList = new ArrayList<>();
        statusList.add(new TerminalStatus(STATUS_IMPORTANT, R.mipmap.ic_coll, 0));
        statusList.add(new TerminalStatus(STATUS_ONLINE, R.mipmap.ic_xh_3, 0));
        statusList.add(new TerminalStatus(STATUS_NORMAL_TAKEN, R.mipmap.ic_blue_right, 0));
        statusList.add(new TerminalStatus(STATUS_ABNORMAL_LOST, R.mipmap.ic_ds, 0));
        statusList.add(new TerminalStatus(STATUS_LOW_BATTERY, R.mipmap.ic_red_sd, 0));
        statusList.add(new TerminalStatus(STATUS_OFFLINE, R.mipmap.ic_xh_no, 0));
        return statusList;
    }
}
