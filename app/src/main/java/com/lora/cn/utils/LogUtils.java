package com.lora.cn.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogUtils {
    private static File logFile;
    private static final Object lock = new Object();

    public static void init(Context context) {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File logs = new File(dir, "LoraAppLogs");
        if (!logs.exists()) logs.mkdirs();
        logFile = new File(logs, "app_log.txt");
        try {
            if (!logFile.exists()) {
                FileOutputStream fos = new FileOutputStream(logFile, false);
                fos.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
                fos.flush();
                fos.close();
            }
        } catch (Exception ignored) {}
    }

    public static void d(String msg) {
        android.util.Log.d("LogUtils", msg);
        write("D", "LogUtils", msg, null);
    }

    public static void d(String tag, String msg) {
        android.util.Log.d(tag, msg);
        write("D", tag, msg, null);
    }

    public static void i(String msg) {
        android.util.Log.i("LogUtils", msg);
        write("I", "LogUtils", msg, null);
    }

    public static void i(String tag, String msg) {
        android.util.Log.i(tag, msg);
        write("I", tag, msg, null);
    }

    public static void e(String msg) {
        android.util.Log.e("LogUtils", msg);
        write("E", "LogUtils", msg, null);
    }

    public static void e(String tag, String msg) {
        android.util.Log.e(tag, msg);
        write("E", tag, msg, null);
    }

    public static void e(String tag, String msg, Throwable tr) {
        android.util.Log.e(tag, msg, tr);
        write("E", tag, msg, tr);
    }

    public static File getLogFile() { return logFile; }

    private static void write(String level, String tag, String msg, Throwable tr) {
        try {
            if (logFile == null) return;
            if (!logFile.exists()) {
                try {
                    FileOutputStream init = new FileOutputStream(logFile, false);
                    init.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
                    init.flush();
                    init.close();
                } catch (Exception ignored) {}
            }
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
            StringBuilder sb = new StringBuilder();
            sb.append(time).append(" ").append(level).append("/").append(tag).append(": ").append(msg);
            if (tr != null) sb.append("\n").append(android.util.Log.getStackTraceString(tr));
            sb.append("\n");
            byte[] data = sb.toString().getBytes("UTF-8");
            synchronized (lock) {
                FileOutputStream fos = new FileOutputStream(logFile, true);
                fos.write(data);
                fos.flush();
                fos.close();
            }
        } catch (Exception ignored) {}
    }
}
