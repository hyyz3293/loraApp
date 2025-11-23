package com.lora.cn.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashLogger implements Thread.UncaughtExceptionHandler {
    private final Context context;
    private final Thread.UncaughtExceptionHandler previous;

    public static void install(Context context) {
        Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new CrashLogger(context, prev));
    }

    private CrashLogger(Context context, Thread.UncaughtExceptionHandler previous) {
        this.context = context.getApplicationContext();
        this.previous = previous;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            LogUtils.e("Crash", "Uncaught: " + e.getMessage(), e);
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File logs = new File(dir, "LoraAppLogs");
            if (!logs.exists()) logs.mkdirs();
            String name = "crash_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".txt";
            File f = new File(logs, name);
            String content = android.util.Log.getStackTraceString(e);
            FileOutputStream fos = new FileOutputStream(f, false);
            fos.write(content.getBytes("UTF-8"));
            fos.flush();
            fos.close();
        } catch (Exception ignored) {}
        if (previous != null) previous.uncaughtException(t, e);
    }
}