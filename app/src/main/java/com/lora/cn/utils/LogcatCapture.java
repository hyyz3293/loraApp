package com.lora.cn.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class LogcatCapture {
    private static volatile boolean running = false;
    private static Thread worker;

    public static void start(Context ctx) {
        if (running) return;
        running = true;
        worker = new Thread(() -> {
            Process proc = null;
            BufferedReader br = null;
            FileOutputStream fos = null;
            try {
                int pid = android.os.Process.myPid();
                File downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                File dir = new File(downloads, "LoraAppLogs");
                if (!dir.exists()) dir.mkdirs();
                File out = new File(dir, "app_logcat.txt");
                if (!out.exists() || out.length() == 0) {
                    FileOutputStream init = new FileOutputStream(out, false);
                    init.write(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF});
                    init.flush();
                    init.close();
                }
                fos = new FileOutputStream(out, true);
                proc = new ProcessBuilder("logcat", "--pid", String.valueOf(pid), "-v", "time").redirectErrorStream(true).start();
                br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String line;
                while (running && (line = br.readLine()) != null) {
                    try {
                        fos.write((line + "\n").getBytes("UTF-8"));
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {
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
}
