package com.dshwork.mynotes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** 元件符号：用 Canvas 直接画，引脚固定在 (-20,0) 与 (20,0) 这类位置。 */
public class Symbols {

    public static final String[] NAMES = {"resistor", "cap", "inductor", "diode", "battery", "ground", "switch", "led"};
    public static final String[] LABELS = {"电阻", "电容", "电感", "二极管", "电池", "接地", "开关", "LED"};

    /** 引脚（相对中心） */
    public static float[][] pins(String name) {
        if ("ground".equals(name)) return new float[][]{{0, -12}};
        return new float[][]{{-22, 0}, {22, 0}};
    }

    public static void draw(Canvas c, String name, float cx, float cy, Paint p) {
        Path path = new Path();
        switch (name) {
            case "resistor": {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx - 22, cy, cx - 12, cy, p);
                path.moveTo(cx - 12, cy);
                path.lineTo(cx - 8, cy - 8);
                path.lineTo(cx - 2, cy + 8);
                path.lineTo(cx + 4, cy - 8);
                path.lineTo(cx + 10, cy + 8);
                path.lineTo(cx + 12, cy);
                c.drawPath(path, p);
                c.drawLine(cx + 12, cy, cx + 22, cy, p);
                break;
            }
            case "cap": {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx - 22, cy, cx - 4, cy, p);
                c.drawLine(cx + 4, cy, cx + 22, cy, p);
                c.drawLine(cx - 4, cy - 11, cx - 4, cy + 11, p);
                c.drawLine(cx + 4, cy - 11, cx + 4, cy + 11, p);
                break;
            }
            case "inductor": {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx - 22, cy, cx - 14, cy, p);
                RectF r = new RectF(cx - 14, cy - 7, cx - 2, cy + 7);
                c.drawArc(r, 180, 180, false, p);
                r = new RectF(cx - 2, cy - 7, cx + 10, cy + 7);
                c.drawArc(r, 180, 180, false, p);
                c.drawLine(cx + 10, cy, cx + 22, cy, p);
                break;
            }
            case "diode": {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx - 22, cy, cx - 8, cy, p);
                c.drawLine(cx + 8, cy, cx + 22, cy, p);
                Path tri = new Path();
                tri.moveTo(cx - 8, cy - 9);
                tri.lineTo(cx + 8, cy);
                tri.lineTo(cx - 8, cy + 9);
                tri.close();
                Paint fill = new Paint(p);
                fill.setStyle(Paint.Style.FILL);
                c.drawPath(tri, fill);
                c.drawLine(cx + 8, cy - 10, cx + 8, cy + 10, p);
                break;
            }
            case "battery": {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx - 22, cy, cx - 10, cy, p);
                c.drawLine(cx - 10, cy - 10, cx - 10, cy + 10, p);
                c.drawLine(cx - 5, cy - 6, cx - 5, cy + 6, p);
                c.drawLine(cx + 1, cy - 10, cx + 1, cy + 10, p);
                c.drawLine(cx + 6, cy - 6, cx + 6, cy + 6, p);
                c.drawLine(cx + 10, cy, cx + 22, cy, p);
                break;
            }
            case "ground": {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx, cy - 12, cx, cy - 2, p);
                c.drawLine(cx - 10, cy - 2, cx + 10, cy - 2, p);
                c.drawLine(cx - 6, cy + 4, cx + 6, cy + 4, p);
                c.drawLine(cx - 3, cy + 10, cx + 3, cy + 10, p);
                break;
            }
            case "switch": {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx - 22, cy, cx - 10, cy, p);
                c.drawLine(cx + 10, cy, cx + 22, cy, p);
                c.drawCircle(cx - 10, cy, 2.5f, p);
                c.drawCircle(cx + 10, cy, 2.5f, p);
                c.drawLine(cx - 9, cy, cx + 8, cy - 10, p);
                break;
            }
            case "led": {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx - 22, cy, cx - 8, cy, p);
                c.drawLine(cx + 8, cy, cx + 22, cy, p);
                Path tri = new Path();
                tri.moveTo(cx - 8, cy - 9);
                tri.lineTo(cx + 8, cy);
                tri.lineTo(cx - 8, cy + 9);
                tri.close();
                Paint fill = new Paint(p);
                fill.setStyle(Paint.Style.FILL);
                c.drawPath(tri, fill);
                c.drawLine(cx + 8, cy - 10, cx + 8, cy + 10, p);
                c.drawLine(cx - 2, cy - 13, cx + 5, cy - 20, p);
                c.drawLine(cx + 4, cy - 13, cx + 11, cy - 20, p);
                break;
            }
            default:
                c.drawCircle(cx, cy, 18, p);
        }
    }
}
