package com.dshwork.mynotes;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** 备份的固定目录：App 私有外部目录下的 backups/，不用系统文件选择器。 */
public class Backups {

    public static File dir(Context c) {
        File d = null;
        try {
            d = c.getExternalFilesDir("backups");
        } catch (Exception ignored) {
        }
        if (d == null) d = new File(c.getFilesDir(), "backups");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    /** 人类可读的目录提示 */
    public static String dirHint(Context c) {
        String abs = dir(c).getAbsolutePath();
        int i = abs.indexOf("/Android/data/");
        if (i > 0) return "内部存储" + abs.substring(i)
                    + "\n（文件管理器 → Android/data/com.dshwork.mynotes/files/backups）";
        return abs;
    }

    public static String stamp() {
        return new SimpleDateFormat("yyyyMMdd-HHmm", Locale.CHINA).format(new Date());
    }

    /** 往输出流里写备份内容的回调 */
    public interface Sink {
        void write(java.io.OutputStream os) throws Exception;
    }

    /** 流式写备份文件：先写 .part 再改名，避免中断留下半个文件 */
    public static File stream(Context c, String name, Sink sink) throws Exception {
        java.io.File dir = dir(c);
        if (!dir.exists()) dir.mkdirs();
        java.io.File tmp = new java.io.File(dir, name + ".part");
        java.io.File out = new java.io.File(dir, name);
        java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp);
        try {
            sink.write(fos);
            fos.flush();
        } finally {
            fos.close();
        }
        if (out.exists()) out.delete();
        if (!tmp.renameTo(out)) {
            throw new java.io.IOException("重命名备份文件失败");
        }
        return out;
    }

    public static File save(Context c, String name, byte[] data) throws Exception {
        File f = new File(dir(c), name);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(data);
        fos.close();
        return f;
    }

    /** 目录里的备份 zip，按时间倒序 */
    public static List<File> list(Context c) {
        List<File> out = new ArrayList<>();
        File[] files = dir(c).listFiles();
        if (files == null) return out;
        Arrays.sort(files, new java.util.Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });
        for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) out.add(f);
        }
        return out;
    }

    /** 看 zip 里是原生版备份还是网页版备份 */
    public static String kindOf(File f) {
        ZipInputStream zip = null;
        try {
            zip = new ZipInputStream(new java.io.FileInputStream(f));
            ZipEntry e;
            int guard = 0;
            while ((e = zip.getNextEntry()) != null && guard++ < 400) {
                if ("manifest.json".equals(e.getName())) return "web";
                if (e.getName().endsWith("mynotes-backup.json")) return "native";
            }
        } catch (Exception ignored) {
        } finally {
            try {
                if (zip != null) zip.close();
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    public static String describe(File f) {
        String kind = kindOf(f);
        String tag = "web".equals(kind) ? "网页版备份" : "native".equals(kind) ? "本机备份" : "未知格式";
        String time = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(f.lastModified()));
        long kb = Math.max(1, f.length() / 1024);
        return tag + " · " + kb + " KB · " + time;
    }
}
