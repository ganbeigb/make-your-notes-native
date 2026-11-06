package com.dshwork.mynotes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Base64;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** 导出：Markdown / HTML / 公式合集 / 完整备份 zip。全部在本地生成。 */
public class Exporter {

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String ymd() {
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA);
        return f.format(new java.util.Date());
    }

    private static Map<String, List<Db.Note>> groupByCategory(Db db, String subjectId) {
        Map<String, List<Db.Note>> map = new LinkedHashMap<>();
        for (Db.Note n : db.notes(subjectId)) {
            String path = n.categoryId == null ? "" : db.categoryPath(n.categoryId);
            if (path.isEmpty()) path = "未分类";
            List<Db.Note> list = map.get(path);
            if (list == null) {
                list = new ArrayList<>();
                map.put(path, list);
            }
            list.add(n);
        }
        return map;
    }

    /* ---------------- Markdown ---------------- */

    public static String markdown(Db db, String subjectId) {
        Db.Subject s = db.subject(subjectId);
        Map<String, List<Db.Note>> groups = groupByCategory(db, subjectId);
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(s == null ? "学习笔记" : s.name).append("\n\n");
        sb.append("> 导出时间 ").append(ymd()).append("\n\n");

        int count = 0;
        for (Map.Entry<String, List<Db.Note>> e : groups.entrySet()) {
            sb.append("## ").append(e.getKey()).append("\n\n");
            for (Db.Note n : e.getValue()) {
                count++;
                sb.append("### ").append(n.name).append("\n\n");
                if (n.content != null && !n.content.trim().isEmpty()) {
                    sb.append(n.content.trim()).append("\n\n");
                }
                if (n.formula != null && !n.formula.trim().isEmpty()) {
                    sb.append("$$").append(n.formula.trim()).append("$$\n\n");
                }
                if (n.quote != null && !n.quote.trim().isEmpty()) {
                    sb.append("> 原文：").append(n.quote.trim()).append("\n\n");
                }
                if (n.tags != null && !n.tags.trim().isEmpty()) {
                    sb.append("标签：").append(n.tags).append("\n\n");
                }
            }
        }
        if (count == 0) sb.append("（还没有知识点）\n");
        return sb.toString();
    }

    /* ---------------- HTML（公式转内联 PNG，离线可看） ---------------- */

    public static String html(Context ctx, Db db, String subjectId) {
        Db.Subject s = db.subject(subjectId);
        Map<String, List<Db.Note>> groups = groupByCategory(db, subjectId);
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">");
        sb.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
        sb.append("<title>").append(esc(s == null ? "学习笔记" : s.name)).append("</title>");
        sb.append("<style>");
        sb.append("body{max-width:820px;margin:0 auto;padding:24px 20px 60px;background:#fff;color:#28302b;");
        sb.append("font:16px/1.8 -apple-system,\"PingFang SC\",\"Microsoft YaHei\",sans-serif}");
        sb.append("h1{font-size:26px;margin:0 0 6px}h2{margin:30px 0 12px;font-size:19px;border-bottom:1px solid #e6ddca;padding-bottom:6px}");
        sb.append("h3{font-size:16px;margin:20px 0 6px}.meta{color:#8a8272;font-size:13px}");
        sb.append(".body{white-space:pre-wrap}.fx{text-align:center;margin:10px 0}.fx img{max-width:100%;height:auto}");
        sb.append(".tag{color:#8a8272;font-size:12px}@media print{h2{page-break-after:avoid}.item{page-break-inside:avoid}}");
        sb.append("</style></head><body>");
        sb.append("<h1>").append(esc(s == null ? "学习笔记" : s.name)).append("</h1>");
        sb.append("<div class=\"meta\">导出时间 ").append(esc(ymd())).append("</div>");

        int count = 0;
        for (Map.Entry<String, List<Db.Note>> e : groups.entrySet()) {
            sb.append("<h2>").append(esc(e.getKey())).append("</h2>");
            for (Db.Note n : e.getValue()) {
                count++;
                sb.append("<div class=\"item\"><h3>").append(esc(n.name)).append("</h3>");
                if (n.content != null && !n.content.trim().isEmpty()) {
                    sb.append("<div class=\"body\">").append(esc(n.content.trim())).append("</div>");
                }
                if (n.formula != null && !n.formula.trim().isEmpty()) {
                    String png = formulaPng(ctx, n.formula.trim());
                    if (png != null) {
                        sb.append("<div class=\"fx\"><img alt=\"formula\" src=\"data:image/png;base64,").append(png).append("\"></div>");
                    } else {
                        sb.append("<div class=\"fx\">").append(esc(n.formula.trim())).append("</div>");
                    }
                }
                if (n.quote != null && !n.quote.trim().isEmpty()) {
                    sb.append("<div class=\"tag\" style=\"border-left:3px solid #d8cfb8;padding-left:8px;margin-top:6px\">原文：")
                            .append(esc(n.quote.trim())).append("</div>");
                }
                if (n.tags != null && !n.tags.trim().isEmpty()) {
                    sb.append("<div class=\"tag\">标签：").append(esc(n.tags)).append("</div>");
                }
                sb.append("</div>");
            }
        }
        if (count == 0) sb.append("<p class=\"meta\">（还没有知识点）</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /* ---------------- 公式合集 ---------------- */

    public static String formulas(Context ctx, Db db, String subjectId) {
        Db.Subject s = db.subject(subjectId);
        Map<String, List<Db.Note>> groups = groupByCategory(db, subjectId);
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">");
        sb.append("<title>").append(esc(s == null ? "" : s.name)).append(" · 公式合集</title>");
        sb.append("<style>@page{size:A4;margin:16mm 14mm}");
        sb.append("body{max-width:820px;margin:0 auto;padding:24px 20px 60px;color:#28302b;");
        sb.append("font:16px/1.8 -apple-system,\"PingFang SC\",\"Microsoft YaHei\",sans-serif}");
        sb.append("h1{font-size:24px;margin:0 0 6px}h2{font-size:18px;margin:28px 0 10px;border-bottom:1px solid #e6ddca;padding-bottom:6px}");
        sb.append(".meta{color:#8a8272;font-size:13px;margin-bottom:20px}");
        sb.append(".entry{margin:0 0 16px;padding-bottom:12px;border-bottom:1px dashed #eee6d5;page-break-inside:avoid}");
        sb.append(".name{font-size:14px;color:#42524a;font-weight:600;margin-bottom:4px}");
        sb.append(".fx{text-align:center;margin:6px 0}.fx img{max-width:100%;height:auto}");
        sb.append(".note{font-size:13px;color:#5d6a63;white-space:pre-wrap;margin-top:6px}");
        sb.append("</style></head><body>");
        sb.append("<h1>").append(esc(s == null ? "" : s.name)).append(" · 公式合集</h1>");
        sb.append("<div class=\"meta\">导出时间 ").append(esc(ymd())).append("</div>");

        int total = 0;
        for (Map.Entry<String, List<Db.Note>> e : groups.entrySet()) {
            List<Db.Note> withFormula = new ArrayList<>();
            for (Db.Note n : e.getValue()) {
                if (n.formula != null && !n.formula.trim().isEmpty()) withFormula.add(n);
            }
            if (withFormula.isEmpty()) continue;
            sb.append("<h2>").append(esc(e.getKey())).append("</h2>");
            for (Db.Note n : withFormula) {
                total++;
                sb.append("<div class=\"entry\"><div class=\"name\">").append(esc(n.name)).append("</div>");
                String png = formulaPng(ctx, n.formula.trim());
                if (png != null) {
                    sb.append("<div class=\"fx\"><img alt=\"formula\" src=\"data:image/png;base64,").append(png).append("\"></div>");
                } else {
                    sb.append("<div class=\"fx\">").append(esc(n.formula.trim())).append("</div>");
                }
                if (n.content != null && !n.content.trim().isEmpty()) {
                    sb.append("<div class=\"note\">").append(esc(n.content.trim())).append("</div>");
                }
                sb.append("</div>");
            }
        }
        if (total == 0) sb.append("<p class=\"meta\">这个科目还没有带公式的知识点。</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /* ---------------- 备份 zip ---------------- */

    public static byte[] backup(Db db, Context ctx) throws Exception {
        Prefs prefs = new Prefs(ctx);
        JSONObject root = new JSONObject();
        root.put("app", "MakeYourNotes-native");
        root.put("version", 2);
        root.put("exportedAt", System.currentTimeMillis());

        JSONArray subjects = new JSONArray();
        JSONArray categories = new JSONArray();
        JSONArray notes = new JSONArray();

        for (Db.Subject s : db.subjects()) {
            JSONObject o = new JSONObject();
            o.put("id", s.id);
            o.put("name", s.name);
            o.put("createdAt", s.createdAt);
            subjects.put(o);

            for (Db.Category c : db.categories(s.id)) {
                JSONObject co = new JSONObject();
                co.put("id", c.id);
                co.put("subjectId", c.subjectId);
                co.put("parentId", c.parentId);
                co.put("name", c.name);
                co.put("orderNo", c.orderNo);
                categories.put(co);
            }
            for (Db.Note n : db.notes(s.id)) {
                JSONObject no = new JSONObject();
                no.put("id", n.id);
                no.put("subjectId", n.subjectId);
                no.put("categoryId", n.categoryId);
                no.put("name", n.name);
                no.put("content", n.content);
                no.put("formula", n.formula);
                no.put("tags", n.tags);
                no.put("source", n.source);
                no.put("importance", n.importance);
                no.put("createdAt", n.createdAt);
                notes.put(no);
            }
        }

        root.put("settings", prefs.snapshot());
        root.put("usage", db.exportUsage());
        root.put("subjects", subjects);
        root.put("categories", categories);
        root.put("notes", notes);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bos);
        zip.putNextEntry(new ZipEntry("mynotes-backup.json"));
        zip.write(root.toString(2).getBytes("UTF-8"));
        zip.closeEntry();
        zip.putNextEntry(new ZipEntry("README.txt"));
        zip.write(("Make Your Notes 备份\n导出时间: " + ymd() + "\n包含: subjects / categories / notes / settings / usage\n").getBytes("UTF-8"));
        zip.closeEntry();
        zip.close();
        return bos.toByteArray();
    }

    /* ---------------- 公式 → PNG ---------------- */

    public static String formulaPng(Context ctx, String tex) {
        try {
            FormulaView v = new FormulaView(ctx);
            v.setTex(tex);
            int ws = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            v.measure(ws, ws);
            int w = Math.max(1, v.getMeasuredWidth());
            int h = Math.max(1, v.getMeasuredHeight());
            v.layout(0, 0, w, h);
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            c.drawColor(0xFFFFFFFF);
            v.draw(c);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
            bmp.recycle();
            return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }
}
