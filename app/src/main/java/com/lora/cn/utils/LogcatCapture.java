package com.lora.cn.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.ParcelFileDescriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class LogcatCapture {
    private static volatile boolean running = false;
    private static Thread worker;
    private static final String PUBLIC_DIR = "Download/LoraAppLogs";
    private static String PUBLIC_NAME = "app_logcat.txt";
    private static String LOGCAT_TS;

    public static void start(Context ctx) {
        if (running) return;
        running = true;
        worker = new Thread(() -> {
            Process proc = null;
            BufferedReader br = null;
            FileOutputStream fos = null;
            try {
                int pid = android.os.Process.myPid();
                if (LOGCAT_TS == null) {
                    try {
                        LOGCAT_TS = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
                    } catch (Exception ignored) {
                        LOGCAT_TS = String.valueOf(System.currentTimeMillis());
                    }
                }
                File base = ctx != null ? ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) : null;
                if (base == null && ctx != null) base = ctx.getExternalFilesDir(null);
                File dir = base != null ? new File(base, "LoraAppLogs") : null;
                if (dir != null && !dir.exists()) dir.mkdirs();
                String name = "app_logcat_" + LOGCAT_TS + ".txt";
                PUBLIC_NAME = name;
                File out = dir != null ? new File(dir, name) : null;
                try {
                    String basePath = base != null ? base.getAbsolutePath() : "null";
                    String dirPath = dir != null ? dir.getAbsolutePath() : "null";
                    String outPath = out != null ? out.getAbsolutePath() : "null";
                    android.util.Log.i("LogcatCapture", "start base=" + basePath + " dir=" + dirPath + " file=" + outPath);
                } catch (Exception ignored) {}
                if (out != null && (!out.exists() || out.length() == 0)) {
                    FileOutputStream init = new FileOutputStream(out, false);
                    init.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
                    init.flush();
                    init.close();
                }
                fos = out != null ? new FileOutputStream(out, true) : null;
                OutputStream publicOs = null;
                try {
                    publicOs = openPublicAppend(ctx);
                } catch (Exception ignored) {}
                proc = new ProcessBuilder("logcat", "--pid", String.valueOf(pid), "-v", "time").redirectErrorStream(true).start();
                br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String line;
                while (running && (line = br.readLine()) != null) {
                    try {
                        if (fos != null) fos.write((line + "\n").getBytes("UTF-8"));
                        if (publicOs != null) publicOs.write((line + "\n").getBytes("UTF-8"));
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                try { android.util.Log.e("LogcatCapture", "start error: " + e.getMessage()); } catch (Exception ignore) {}
            } finally {
                try { if (br != null) br.close(); } catch (Exception ignore) {}
                try { if (fos != null) fos.close(); } catch (Exception ignore) {}
                try { if (proc != null) proc.destroy(); } catch (Exception ignore) {}
            }
        }, "LogcatCapture");
        worker.setDaemon(true);
        try { worker.start(); } catch (Exception ignored) {}
    }

    public static void stop() {
        running = false;
        try {
            if (worker != null) {
                worker.interrupt();
                worker = null;
            }
        } catch (Exception ignored) {}
    }

    private static OutputStream openPublicAppend(Context ctx) {
        try {
            if (ctx == null) return null;
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
                if (uri != null) {
                    ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "wa");
                    if (pfd != null) {
                        return new FileOutputStream(pfd.getFileDescriptor());
                    }
                }
            } else {
                File downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                File dir = new File(downloads, "LoraAppLogs");
                if (!dir.exists()) dir.mkdirs();
                File out = new File(dir, PUBLIC_NAME);
                if (!out.exists()) {
                    FileOutputStream init = new FileOutputStream(out, false);
                    init.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
                    init.flush();
                    init.close();
                }
                return new FileOutputStream(out, true);
            }
        } catch (Exception ignored) {}
        return null;
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
}
