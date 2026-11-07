package com.dshwork.mynotes;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 崩溃日志：把未捕获异常的堆栈写到 files/crash.log，方便装机后回传定位。 */
public class CrashLog {

    private static boolean installed = false;
    private static File logFile;

    public static void install(final Context ctx) {
        final Context app = ctx.getApplicationContext();
        logFile = new File(app.getFilesDir(), "crash.log");
        if (installed) return;
        installed = true;
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                try {
                    save(app, e);
                } catch (Throwable ignored) {
                }
                if (prev != null) prev.uncaughtException(thread, e);
            }
        });
    }

    private static void save(Context ctx, Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
        String body = "时间：" + time + "\n\n" + sw.toString();
        File f = new File(ctx.getFilesDir(), "crash.log");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f, false);
            fos.write(body.getBytes("UTF-8"));
        } catch (Exception ignored) {
        } finally {
            try {
                if (fos != null) fos.close();
            } catch (Exception ignored) {
            }
        }
    }

    /** 读最近的崩溃日志，没有就返回空串 */
    public static String read(Context ctx) {
        try {
            File f = new File(ctx.getFilesDir(), "crash.log");
            if (!f.exists()) return "";
            byte[] buf = new byte[(int) f.length()];
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            int n = in.read(buf);
            in.close();
            return n > 0 ? new String(buf, 0, n, "UTF-8") : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static void clear(Context ctx) {
        try {
            new File(ctx.getFilesDir(), "crash.log").delete();
        } catch (Exception ignored) {
        }
    }
}
