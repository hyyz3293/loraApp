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
    private static final long ROTATE_MS = 30L * 60L * 1000L;
    private static final int MAX_FILES = 96;

    public static void start(Context ctx) {
        if (running) return;
        running = true;
        worker = new Thread(() -> {
            Process proc = null;
            BufferedReader br = null;
            StreamHolder holder = new StreamHolder();
            try {
                int pid = android.os.Process.myPid();
                File base = ctx != null ? ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) : null;
                if (base == null && ctx != null) base = ctx.getExternalFilesDir(null);
                File dir = base != null ? new File(base, "LoraAppLogs") : null;
                if (dir != null && !dir.exists()) dir.mkdirs();
                long now0 = System.currentTimeMillis();
                long nextRotateAt = rotateToNewFile(ctx, base, dir, holder, now0);
                proc = new ProcessBuilder("logcat", "--pid", String.valueOf(pid), "-v", "time").redirectErrorStream(true).start();
                br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String line;
                while (running && (line = br.readLine()) != null) {
                    long now = System.currentTimeMillis();
                    if (now >= nextRotateAt) {
                        nextRotateAt = rotateToNewFile(ctx, base, dir, holder, now);
                    }
                    try {
                        if (holder.fos != null) holder.fos.write((line + "\n").getBytes("UTF-8"));
                        if (holder.publicOs != null) holder.publicOs.write((line + "\n").getBytes("UTF-8"));
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                try { android.util.Log.e("LogcatCapture", "start error: " + e.getMessage()); } catch (Exception ignore) {}
            } finally {
                try { if (br != null) br.close(); } catch (Exception ignore) {}
                try { if (holder.fos != null) holder.fos.close(); } catch (Exception ignore) {}
                try { if (holder.publicOs != null) holder.publicOs.close(); } catch (Exception ignore) {}
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

    private static final class StreamHolder {
        FileOutputStream fos;
        OutputStream publicOs;
    }

    private static long rotateToNewFile(Context ctx, File base, File dir, StreamHolder holder, long now) {
        try { if (holder.fos != null) holder.fos.close(); } catch (Exception ignored) {}
        try { if (holder.publicOs != null) holder.publicOs.close(); } catch (Exception ignored) {}
        holder.fos = null;
        holder.publicOs = null;
        try {
            String ts;
            try {
                ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date(now));
            } catch (Exception ignored) {
                ts = String.valueOf(now);
            }
            LOGCAT_TS = ts;
            String name = "app_logcat_" + ts + ".txt";
            PUBLIC_NAME = name;
            File out = dir != null ? new File(dir, name) : null;
            if (out != null && (!out.exists() || out.length() == 0)) {
                FileOutputStream init = new FileOutputStream(out, false);
                init.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
                init.flush();
                init.close();
            }
            holder.fos = out != null ? new FileOutputStream(out, true) : null;
            try { holder.publicOs = openPublicAppend(ctx); } catch (Exception ignored) { holder.publicOs = null; }
            if (dir != null) prune(dir, "app_logcat_", MAX_FILES);
            try {
                String basePath = base != null ? base.getAbsolutePath() : "null";
                String dirPath = dir != null ? dir.getAbsolutePath() : "null";
                String outPath = out != null ? out.getAbsolutePath() : "null";
                android.util.Log.i("LogcatCapture", "rotate base=" + basePath + " dir=" + dirPath + " file=" + outPath);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
        return now + ROTATE_MS;
    }

    private static void prune(File dir, String prefix, int maxFiles) {
        try {
            if (dir == null || !dir.exists() || maxFiles <= 0) return;
            File[] arr = dir.listFiles();
            if (arr == null || arr.length <= maxFiles) return;
            java.util.ArrayList<File> list = new java.util.ArrayList<>();
            for (File f : arr) {
                if (f == null) continue;
                String n = f.getName();
                if (n != null && n.startsWith(prefix) && n.endsWith(".txt")) list.add(f);
            }
            if (list.size() <= maxFiles) return;
            java.util.Collections.sort(list, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
            int remove = list.size() - maxFiles;
            for (int i = 0; i < remove; i++) {
                try { list.get(i).delete(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }
}
