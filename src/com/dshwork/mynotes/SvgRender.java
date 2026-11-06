package com.dshwork.mynotes;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 把网页版画图导出的 SVG 子集渲染成 PNG（path / line / rect / circle / text）。 */
public class SvgRender {

    /** 渲染成位图；解析不了返回 null */
    public static Bitmap render(String svg, int maxPx) {
        if (svg == null || svg.trim().isEmpty()) return null;
        try {
            float vw = 400, vh = 300;
            Matcher vb = Pattern.compile("viewBox=\"([-\\d.]+)[ ,]+([-\\d.]+)[ ,]+([\\d.]+)[ ,]+([\\d.]+)\"").matcher(svg);
            if (vb.find()) {
                vw = Float.parseFloat(vb.group(3)) - Float.parseFloat(vb.group(1));
                vh = Float.parseFloat(vb.group(4)) - Float.parseFloat(vb.group(2));
            } else {
                Matcher wm = Pattern.compile("width=\"([\\d.]+)").matcher(svg);
                if (wm.find()) vw = Float.parseFloat(wm.group(1));
                Matcher hm = Pattern.compile("height=\"([\\d.]+)").matcher(svg);
                if (hm.find()) vh = Float.parseFloat(hm.group(1));
            }
            if (vw <= 1 || vh <= 1) return null;
            float scale = Math.min(1f, maxPx / Math.max(vw, vh));
            int bw = Math.max(1, Math.round(vw * scale));
            int bh = Math.max(1, Math.round(vh * scale));

            Bitmap bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            c.drawColor(0xFFFFFDF8);
            c.scale(scale, scale);

            renderTags(c, svg);
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    private static void renderTags(Canvas c, String svg) {
        Matcher m = Pattern.compile("<(path|line|rect|circle|text)\\b([^>]*?)/?>").matcher(svg);
        while (m.find()) {
            String tag = m.group(1);
            String attrs = m.group(2);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);

            String stroke = attr(attrs, "stroke");
            String fill = attr(attrs, "fill");
            float sw = num(attrs, "stroke-width", 2f);
            p.setStrokeWidth(sw);
            p.setColor(parseColor(stroke, 0xFF243029));
            boolean filled = fill != null && !"none".equals(fill);
            if (filled) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(parseColor(fill, 0xFF243029));
            }

            if ("path".equals(tag)) {
                String d = attr(attrs, "d");
                if (d != null) {
                    Path path = SvgPath.parse(d);
                    c.drawPath(path, p);
                }
            } else if ("line".equals(tag)) {
                c.drawLine(num(attrs, "x1", 0), num(attrs, "y1", 0),
                        num(attrs, "x2", 0), num(attrs, "y2", 0), p);
            } else if ("rect".equals(tag)) {
                float x = num(attrs, "x", 0), y = num(attrs, "y", 0);
                float w = num(attrs, "width", 0), h = num(attrs, "height", 0);
                float r = num(attrs, "rx", 0);
                if (r > 0) c.drawRoundRect(new RectF(x, y, x + w, y + h), r, r, p);
                else c.drawRect(x, y, x + w, y + h, p);
            } else if ("circle".equals(tag)) {
                c.drawCircle(num(attrs, "cx", 0), num(attrs, "cy", 0), num(attrs, "r", 0), p);
            } else if ("text".equals(tag)) {
                int start = m.end();
                int end = svg.indexOf("</text>", start);
                String text = end > start ? svg.substring(start, end) : "";
                text = text.replaceAll("<[^>]+>", "").replace("&amp;", "&").trim();
                if (text.isEmpty()) continue;
                p.setStyle(Paint.Style.FILL);
                p.setColor(parseColor(fill == null ? stroke : fill, 0xFF243029));
                p.setTextSize(num(attrs, "font-size", 12f));
                c.drawText(text, num(attrs, "x", 0), num(attrs, "y", 0), p);
            }
        }
    }

    private static String attr(String attrs, String key) {
        Matcher m = Pattern.compile(key + "=\"([^\"]*)\"").matcher(attrs);
        return m.find() ? m.group(1) : null;
    }

    private static float num(String attrs, String key, float def) {
        String v = attr(attrs, key);
        if (v == null) return def;
        try {
            return Float.parseFloat(v.replaceAll("[^\\d.\\-]", ""));
        } catch (Exception e) {
            return def;
        }
    }

    private static int parseColor(String v, int def) {
        if (v == null) return def;
        try {
            return Color.parseColor(v.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
