package com.lora.cn.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.ParcelFileDescriptor;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogUtils {
    private static File logFile;
    private static final Object lock = new Object();
    private static final String PUBLIC_DIR = "Download/LoraAppLogs";
    private static String PUBLIC_NAME = "app_log.txt";
    private static String LOG_TS;

    public static void init(Context context) {
        if (LOG_TS == null) {
            try {
                LOG_TS = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            } catch (Exception ignored) {
                LOG_TS = String.valueOf(System.currentTimeMillis());
            }
        }
        File base = context != null ? context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) : null;
        if (base == null && context != null) base = context.getExternalFilesDir(null);
        File logs = base != null ? new File(base, "LoraAppLogs") : null;
        if (logs != null && !logs.exists()) logs.mkdirs();
        String name = "app_log_" + LOG_TS + ".txt";
        PUBLIC_NAME = name;
        logFile = logs != null ? new File(logs, name) : null;
        try {
            String basePath = base != null ? base.getAbsolutePath() : "null";
            String logsPath = logs != null ? logs.getAbsolutePath() : "null";
            String filePath = logFile != null ? logFile.getAbsolutePath() : "null";
            android.util.Log.i("LogUtils", "init base=" + basePath + " logs=" + logsPath + " file=" + filePath);
        } catch (Exception ignored) {}
        try {
            if (logFile != null && !logFile.exists()) {
                FileOutputStream fos = new FileOutputStream(logFile, false);
                fos.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
                fos.flush();
                fos.close();
            }
        } catch (Exception e) {
            try { android.util.Log.e("LogUtils", "init create file error: " + e.getMessage()); } catch (Exception ignored) {}
        }
        try { ensurePublicFile(context); } catch (Exception ignored) {}
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
            if (logFile == null) {
                try { init(com.blankj.utilcode.util.Utils.getApp()); } catch (Exception ignored) {}
                try { android.util.Log.e("LogUtils", "write logFile null, attempted reinit"); } catch (Exception ignored) {}
                if (logFile == null) return;
            }
            if (!logFile.exists()) {
                try {
                    FileOutputStream init = new FileOutputStream(logFile, false);
                    init.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
                    init.flush();
                    init.close();
                } catch (Exception e) {
                    try { android.util.Log.e("LogUtils", "write init file error: " + e.getMessage()); } catch (Exception ignored) {}
                }
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
            try { appendPublic(sb.toString()); } catch (Exception ignored) {}
        } catch (Exception e) {
            try { android.util.Log.e("LogUtils", "write error: " + e.getMessage()); } catch (Exception ignored) {}
        }
    }

    private static void ensurePublicFile(Context ctx) {
        try {
            if (ctx == null) return;
            if (Build.VERSION.SDK_INT >= 29) {
                ContentResolver cr = ctx.getContentResolver();
                Uri uri = findPublicUri(cr);
                if (uri == null) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME, PUBLIC_NAME);
                    v.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                    v.put(MediaStore.Downloads.RELATIVE_PATH, PUBLIC_DIR);
                    uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (uri != null) {
                        OutputStream os = cr.openOutputStream(uri, "wt");
                        if (os != null) {
                            os.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
                            os.close();
                        }
                    }
                }
            } else {
                File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File dir = new File(downloads, "LoraAppLogs");
                if (!dir.exists()) dir.mkdirs();
                File out = new File(dir, PUBLIC_NAME);
                if (!out.exists()) {
                    FileOutputStream fos = new FileOutputStream(out, false);
                    fos.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
                    fos.flush();
                    fos.close();
                }
            }
        } catch (Exception ignored) {}
    }

    private static Uri findPublicUri(ContentResolver cr) {
        try {
            String sel = MediaStore.Downloads.DISPLAY_NAME + "=? AND " + MediaStore.Downloads.RELATIVE_PATH + "=?";
            String[] args = new String[]{PUBLIC_NAME, PUBLIC_DIR + "/"};
            android.database.Cursor c = cr.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Downloads._ID}, sel, args, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        long id = c.getLong(0);
                        return Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, String.valueOf(id));
                    }
                } finally {
                    c.close();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void appendPublic(String text) {
        try {
            Context ctx = com.blankj.utilcode.util.Utils.getApp();
            if (ctx == null) return;
            if (Build.VERSION.SDK_INT >= 29) {
                ContentResolver cr = ctx.getContentResolver();
                Uri uri = findPublicUri(cr);
                if (uri == null) {
                    ensurePublicFile(ctx);
                    uri = findPublicUri(cr);
                }
                if (uri != null) {
                    ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "wa");
                    if (pfd != null) {
                        FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
                        fos.write(text.getBytes("UTF-8"));
                        fos.flush();
                        fos.close();
                        pfd.close();
                    }
                }
            } else {
                File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File dir = new File(downloads, "LoraAppLogs");
                if (!dir.exists()) dir.mkdirs();
                File out = new File(dir, PUBLIC_NAME);
                FileOutputStream fos = new FileOutputStream(out, true);
                fos.write(text.getBytes("UTF-8"));
                fos.flush();
                fos.close();
            }
        } catch (Exception ignored) {}
    }
}
