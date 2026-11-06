package com.dshwork.mynotes;

import android.graphics.Path;
import android.graphics.RectF;

/** 极简 SVG path 解析：M m L l H h V v C c S s Q q T t A a Z z（弧按圆/椭圆近似）。 */
public class SvgPath {

    private final String d;
    private int i;
    private char cmd;
    private float cx, cy, sx, sy;

    private SvgPath(String d) {
        this.d = d;
    }

    public static Path parse(String d) {
        Path path = new Path();
        if (d == null || d.trim().isEmpty()) return path;
        new SvgPath(d).run(path);
        return path;
    }

    private void run(Path p) {
        while (i < d.length()) {
            char c = d.charAt(i);
            if (Character.isLetter(c)) {
                cmd = c;
                i++;
            } else if (Character.isWhitespace(c) || c == ',') {
                i++;
                continue;
            } else if (cmd == 0) {
                i++;
                continue;
            }
            switch (Character.toUpperCase(cmd)) {
                case 'M': {
                    float x = num(), y = num();
                    if (Character.isLowerCase(cmd)) { x += cx; y += cy; }
                    cx = x; cy = y; sx = x; sy = y;
                    p.moveTo(cx, cy);
                    cmd = Character.isLowerCase(cmd) ? 'l' : 'L';
                    break;
                }
                case 'L': {
                    float x = num(), y = num();
                    if (Character.isLowerCase(cmd)) { x += cx; y += cy; }
                    cx = x; cy = y;
                    p.lineTo(cx, cy);
                    break;
                }
                case 'H': {
                    float x = num();
                    if (Character.isLowerCase(cmd)) x += cx;
                    cx = x;
                    p.lineTo(cx, cy);
                    break;
                }
                case 'V': {
                    float y = num();
                    if (Character.isLowerCase(cmd)) y += cy;
                    cy = y;
                    p.lineTo(cx, cy);
                    break;
                }
                case 'C': {
                    float x1 = num(), y1 = num(), x2 = num(), y2 = num(), x = num(), y = num();
                    if (Character.isLowerCase(cmd)) {
                        x1 += cx; y1 += cy; x2 += cx; y2 += cy; x += cx; y += cy;
                    }
                    p.cubicTo(x1, y1, x2, y2, x, y);
                    cx = x; cy = y;
                    break;
                }
                case 'Q': {
                    float x1 = num(), y1 = num(), x = num(), y = num();
                    if (Character.isLowerCase(cmd)) { x1 += cx; y1 += cy; x += cx; y += cy; }
                    p.quadTo(x1, y1, x, y);
                    cx = x; cy = y;
                    break;
                }
                case 'A': {
                    float rx = num(), ry = num(), rot = num();
                    boolean large = num() != 0;
                    boolean sweep = num() != 0;
                    float x = num(), y = num();
                    if (Character.isLowerCase(cmd)) { x += cx; y += cy; }
                    arc(p, cx, cy, rx, ry, rot, large, sweep, x, y);
                    cx = x; cy = y;
                    break;
                }
                case 'Z': {
                    p.close();
                    cx = sx; cy = sy;
                    break;
                }
                default:
                    i++;
                    break;
            }
        }
    }

    private float num() {
        while (i < d.length() && (d.charAt(i) == ' ' || d.charAt(i) == ',' || d.charAt(i) == '\n')) i++;
        int start = i;
        if (i < d.length() && (d.charAt(i) == '-' || d.charAt(i) == '+')) i++;
        while (i < d.length() && (Character.isDigit(d.charAt(i)) || d.charAt(i) == '.')) i++;
        if (i < d.length() && (d.charAt(i) == 'e' || d.charAt(i) == 'E')) {
            i++;
            if (i < d.length() && (d.charAt(i) == '-' || d.charAt(i) == '+')) i++;
            while (i < d.length() && Character.isDigit(d.charAt(i))) i++;
        }
        if (start == i) {
            i++;
            return 0;
        }
        try {
            return Float.parseFloat(d.substring(start, i));
        } catch (Exception e) {
            return 0;
        }
    }

    /** SVG 端点式圆弧 → Path.arcTo */
    private static void arc(Path p, float x0, float y0, float rx, float ry,
                            float rot, boolean large, boolean sweep, float x1, float y1) {
        if (rx <= 0 || ry <= 0) {
            p.lineTo(x1, y1);
            return;
        }
        if (rot != 0) {
            p.lineTo(x1, y1);
            return;
        }
        float dx2 = (x0 - x1) / 2f, dy2 = (y0 - y1) / 2f;
        float lambda = (dx2 * dx2) / (rx * rx) + (dy2 * dy2) / (ry * ry);
        if (lambda > 1) {
            float s = (float) Math.sqrt(lambda);
            rx *= s;
            ry *= s;
        }
        float num = rx * rx * ry * ry - rx * rx * dy2 * dy2 - ry * ry * dx2 * dx2;
        float den = rx * rx * dy2 * dy2 + ry * ry * dx2 * dx2;
        float coef = (float) Math.sqrt(Math.max(0, num / Math.max(den, 0.0001f)));
        if (large == sweep) coef = -coef;
        float cxp = coef * rx * dy2 / ry;
        float cyp = -coef * ry * dx2 / rx;
        float ccx = cxp + (x0 + x1) / 2f;
        float ccy = cyp + (y0 + y1) / 2f;

        double theta1 = Math.atan2((y0 - ccy) / ry, (x0 - ccx) / rx);
        double theta2 = Math.atan2((y1 - ccy) / ry, (x1 - ccx) / rx);
        double delta = theta2 - theta1;
        if (sweep && delta < 0) delta += 2 * Math.PI;
        if (!sweep && delta > 0) delta -= 2 * Math.PI;

        RectF oval = new RectF(ccx - rx, ccy - ry, ccx + rx, ccy + ry);
        p.arcTo(oval, (float) Math.toDegrees(theta1), (float) Math.toDegrees(delta), false);
    }
}
