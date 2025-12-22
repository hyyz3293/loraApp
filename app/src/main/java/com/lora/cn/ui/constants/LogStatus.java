package com.lora.cn.ui.constants;

public enum LogStatus {
    LOCK_CLOSE(1, "上锁"),
    LOCK_OPEN(2, "开锁"),
    DEVICE_OFF(3, "设备关闭"),
    DEVICE_ON(4, "正常取走"),
    DEVICE_LOST(5, "异常取走"),
    DEVICE_OFFLINE(6, "设备离线"),
    LOW_BATTERY(7, "设备低电量"),
    HANDLED(8, "处理"),
    ONLINE(9, "正常在线"),
    TIMED_MAINTENANCE(10, "定时维护报警");


    public final int code;
    private final String label;
    LogStatus(int code, String label) { this.code = code; this.label = label; }
    public String label() { return label; }
    public static String toText(int code) {
        for (LogStatus s : values()) if (s.code == code) return s.label;
        return "";
    }
    public static int fromText(String text) {
        if (text == null) return 0;
        String t = text.trim();
        for (LogStatus s : values()) if (s.label.equals(t)) return s.code;
        return 0;
    }
}
