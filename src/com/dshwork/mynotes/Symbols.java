package com.dshwork.mynotes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** 元件符号库：从网页版 symbols.js 一比一移植（40 个，引脚坐标一致）。 */
public class Symbols {

    public static final String[] NAMES = {
        "sym-resistor", "sym-resistor-h", "sym-cap", "sym-cap-pol", "sym-inductor", "sym-pot", "sym-varistor", "sym-fuse", "sym-thermistor", "sym-lamp", "sym-motor", "sym-battery", "sym-vsource", "sym-isource", "sym-switch", "sym-button", "sym-diode", "sym-zener", "sym-led", "sym-photodiode", "sym-crystal", "sym-npn", "sym-pnp", "sym-nmos", "sym-opamp", "sym-optocoupler", "sym-ground", "sym-vcc", "sym-vdd", "sym-ammeter", "sym-voltmeter", "sym-transformer", "sym-buzzer", "sym-speaker", "sym-antenna", "sym-connector", "sym-switch-spdt", "sym-bridge", "sym-and", "sym-or", "sym-not"
    };

    public static final String[] LABELS = {
        "电阻", "resistor-h", "无极性电容", "电解电容", "电感", "电位器", "压敏电阻", "保险丝", "热敏电阻", "灯泡", "电机", "电池", "电压源", "电流源", "开关", "按钮", "二极管", "稳压二极管", "发光二极管", "光电二极管", "晶振", "NPN 三极管", "PNP 三极管", "N 沟道 MOS", "运算放大器", "光耦", "接地", "电源正", "VDD", "电流表", "电压表", "变压器", "蜂鸣器", "喇叭", "天线", "排针", "单刀双掷", "整流桥", "与门", "或门", "非门"
    };

    public static float[][] pins(String id) {
        if ("sym-resistor".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-resistor-h".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-cap".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-cap-pol".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-inductor".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-pot".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-varistor".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-fuse".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-thermistor".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-lamp".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-motor".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-battery".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-vsource".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-isource".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-switch".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-button".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-diode".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-zener".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-led".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-photodiode".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-crystal".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-transformer".equals(id)) return new float[][]{{-20f, -10f}, {-20f, 10f}, {20f, -10f}, {20f, 10f}};
        if ("sym-buzzer".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-speaker".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-antenna".equals(id)) return new float[][]{{0f, 12f}};
        if ("sym-connector".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-switch-spdt".equals(id)) return new float[][]{{-20f, 0f}, {20f, -10f}, {20f, 10f}};
        if ("sym-bridge".equals(id)) return new float[][]{{-30f, -10f}, {-30f, 10f}, {30f, -10f}, {30f, 10f}};
        if ("sym-and".equals(id)) return new float[][]{{-30f, -8f}, {-30f, 8f}, {20f, 0f}};
        if ("sym-or".equals(id)) return new float[][]{{-30f, -8f}, {-30f, 8f}, {20f, 0f}};
        if ("sym-not".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-npn".equals(id)) return new float[][]{{-20f, 0f}, {20f, -16f}, {20f, 16f}};
        if ("sym-pnp".equals(id)) return new float[][]{{-20f, 0f}, {20f, -16f}, {20f, 16f}};
        if ("sym-nmos".equals(id)) return new float[][]{{-20f, 0f}, {20f, -14f}, {20f, 14f}};
        if ("sym-opamp".equals(id)) return new float[][]{{-20f, -8f}, {-20f, 8f}, {20f, 0f}};
        if ("sym-optocoupler".equals(id)) return new float[][]{{-30f, -8f}, {-30f, 8f}, {30f, -8f}, {30f, 8f}};
        if ("sym-ground".equals(id)) return new float[][]{{0f, -12f}};
        if ("sym-vcc".equals(id)) return new float[][]{{0f, 12f}};
        if ("sym-vdd".equals(id)) return new float[][]{{0f, 12f}};
        if ("sym-ammeter".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        if ("sym-voltmeter".equals(id)) return new float[][]{{-20f, 0f}, {20f, 0f}};
        return new float[][]{{-20f, 0f}, {20f, 0f}};
    }

    public static void draw(Canvas c, String id, float cx, float cy, Paint base) {
        Paint p = new Paint(base);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.6f);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        c.save();
        c.translate(cx, cy);
        switch (id) {
            case "sym-resistor":
                drawPath(c, p, "M-20 0h5l5-7 5 14 5-14 5 14 5-14 5 7h5", 1.6f, false);
                break;
            case "sym-resistor-h":
                drawPath(c, p, "M-20 0h5l5-7 5 14 5-14 5 14 5-14 5 7h5", 1.6f, false);
                break;
            case "sym-cap":
                drawPath(c, p, "M-20 0h15M5 0h15", 1.6f, false);
                drawPath(c, p, "M-5-10v20M5-10v20", 2f, false);
                break;
            case "sym-cap-pol":
                drawPath(c, p, "M-20 0h15M5 0h15", 1.6f, false);
                drawPath(c, p, "M-5-10v20", 2f, false);
                drawPath(c, p, "M5-10q11 10 0 20", 2f, false);
                drawText(c, p, -16f, -6f, 9f, "+");
                break;
            case "sym-inductor":
                drawPath(c, p, "M-20 0h8", 1.6f, false);
                drawPath(c, p, "M-12 0a4 4 0 0 1 8 0a4 4 0 0 1 8 0a4 4 0 0 1 8 0h8", 1.6f, false);
                break;
            case "sym-pot":
                drawPath(c, p, "M-20 0h5l5-7 5 14 5-14 5 14 5-14 5 7h5", 1.6f, false);
                drawPath(c, p, "M0-18v9", 1.6f, false);
                drawPath(c, p, "M-4-11L0-5l4-6z", 1.6f, true);
                break;
            case "sym-varistor":
                drawPath(c, p, "M-20 0h11M9 0h11", 1.6f, false);
                drawRect(c, p, -9f, -7f, 18f, 14f, 2f, 1.8f, false);
                drawPath(c, p, "M-6 5l4-10M-1 5l4-10", 1.6f, false);
                break;
            case "sym-fuse":
                drawPath(c, p, "M-20 0h11M9 0h11", 1.6f, false);
                drawRect(c, p, -9f, -6f, 18f, 12f, 2f, 1.8f, false);
                drawPath(c, p, "M-9 0h18", 1.6f, false);
                break;
            case "sym-thermistor":
                drawPath(c, p, "M-20 0h11M9 0h11", 1.6f, false);
                drawRect(c, p, -9f, -6f, 18f, 12f, 2f, 1.8f, false);
                drawPath(c, p, "M-7-6l14 12", 1.6f, false);
                break;
            case "sym-lamp":
                drawPath(c, p, "M-20 0h10M10 0h10", 1.6f, false);
                drawCircle(c, p, 0f, 0f, 10f, 1.8f, false);
                drawPath(c, p, "M-7-7l14 14M7-7l-14 14", 1.6f, false);
                break;
            case "sym-motor":
                drawPath(c, p, "M-20 0h9M11 0h9", 1.6f, false);
                drawCircle(c, p, 0f, 0f, 11f, 1.8f, false);
                drawText(c, p, -4f, 4.5f, 12f, "M");
                break;
            case "sym-battery":
                drawPath(c, p, "M-20 0h12M8 0h12", 1.6f, false);
                drawPath(c, p, "M-8-11v22", 2f, false);
                drawPath(c, p, "M-4-6v12", 1.6f, false);
                drawPath(c, p, "M4-11v22", 2f, false);
                drawPath(c, p, "M8-6v12", 1.6f, false);
                break;
            case "sym-vsource":
                drawPath(c, p, "M-20 0h10M10 0h10", 1.6f, false);
                drawCircle(c, p, 0f, 0f, 10f, 1.8f, false);
                drawText(c, p, -5f, 4f, 11f, "+");
                drawText(c, p, 2f, 4f, 11f, "−");
                break;
            case "sym-isource":
                drawPath(c, p, "M-20 0h10M10 0h10", 1.6f, false);
                drawCircle(c, p, 0f, 0f, 10f, 1.8f, false);
                drawPath(c, p, "M-4 5L4-4", 1.6f, false);
                drawPath(c, p, "M4-4l-1 5 5-1z", 1.6f, true);
                break;
            case "sym-switch":
                drawPath(c, p, "M-20 0h12M8 0h12", 1.6f, false);
                drawPath(c, p, "M-8-2.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5", 1.6f, true);
                drawPath(c, p, "M8-2.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5", 1.6f, true);
                drawPath(c, p, "M-6 0l12-9", 1.6f, false);
                break;
            case "sym-button":
                drawPath(c, p, "M-20 0h12M8 0h12", 1.6f, false);
                drawPath(c, p, "M-8-2.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5", 1.6f, true);
                drawPath(c, p, "M8-2.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5", 1.6f, true);
                drawPath(c, p, "M-7-4h14", 2f, false);
                drawPath(c, p, "M0-4v-8", 1.6f, false);
                drawPath(c, p, "M-5-12h10", 1.6f, false);
                break;
            case "sym-diode":
                drawPath(c, p, "M-20 0h10M10 0h10", 1.6f, false);
                drawPath(c, p, "M-10-9L10 0L-10 9Z", 1.6f, true);
                drawPath(c, p, "M10-9v18", 2f, false);
                break;
            case "sym-zener":
                drawPath(c, p, "M-20 0h10M10 0h10", 1.6f, false);
                drawPath(c, p, "M-10-9L10 0L-10 9Z", 1.6f, true);
                drawPath(c, p, "M10-9v18M10-9l-4 3M10 9l4-3", 2f, false);
                break;
            case "sym-led":
                drawPath(c, p, "M-20 0h10M10 0h10", 1.6f, false);
                drawPath(c, p, "M-10-9L10 0L-10 9Z", 1.6f, true);
                drawPath(c, p, "M10-9v18", 2f, false);
                drawPath(c, p, "M-2-13l6-6M4-6l6-6", 1.6f, false);
                drawPath(c, p, "M-3-9l3-2M3-2l3-2", 1.6f, false);
                break;
            case "sym-photodiode":
                drawPath(c, p, "M-20 0h10M10 0h10", 1.6f, false);
                drawPath(c, p, "M-10-9L10 0L-10 9Z", 1.6f, true);
                drawPath(c, p, "M10-9v18", 2f, false);
                drawPath(c, p, "M-2-13l-6-6M4-13l-6-6", 1.6f, false);
                break;
            case "sym-crystal":
                drawPath(c, p, "M-20 0h10M10 0h10", 1.6f, false);
                drawPath(c, p, "M-10-10v20M10-10v20", 2f, false);
                drawRect(c, p, -4f, -9f, 8f, 18f, 1.5f, 1.4f, false);
                break;
            case "sym-npn":
                drawPath(c, p, "M-20 0h11", 1.6f, false);
                drawPath(c, p, "M-9-10v20", 2.4f, false);
                drawPath(c, p, "M-9-4L20-16M-9 4L20 16", 1.6f, false);
                drawPath(c, p, "M14 12l-6-2 4-5z", 1.6f, true);
                drawCircle(c, p, 4f, 0f, 14f, 1f, false);
                break;
            case "sym-pnp":
                drawPath(c, p, "M-20 0h11", 1.6f, false);
                drawPath(c, p, "M-9-10v20", 2.4f, false);
                drawPath(c, p, "M-9-4L20-16M-9 4L20 16", 1.6f, false);
                drawPath(c, p, "M-6 0l6-2-2-4z", 1.6f, true);
                drawCircle(c, p, 4f, 0f, 14f, 1f, false);
                break;
            case "sym-nmos":
                drawPath(c, p, "M-20 0h11", 1.6f, false);
                drawPath(c, p, "M-9-11v22", 2.4f, false);
                drawPath(c, p, "M-4-12v24", 1.4f, false);
                drawPath(c, p, "M-4-14H20M-4 14H20", 1.6f, false);
                drawPath(c, p, "M-4 0H6M6 0v-14", 1.4f, false);
                drawPath(c, p, "M2 0l5-2-1 4z", 1.6f, true);
                break;
            case "sym-opamp":
                drawPath(c, p, "M-12-14L16 0L-12 14Z", 1.6f, false);
                drawPath(c, p, "M-20-8h8M-20 8h8M16 0h4", 1.6f, false);
                drawText(c, p, -8f, -4f, 11f, "−");
                drawText(c, p, -8f, 12f, 11f, "+");
                break;
            case "sym-optocoupler":
                drawPath(c, p, "M-30-8h14M-30 8h14M16-8h14M16 8h14", 1.6f, false);
                drawRect(c, p, -16f, -12f, 32f, 24f, 3f, 1.8f, false);
                drawPath(c, p, "M-10-4L-2 0L-10 4Z", 1.6f, true);
                drawPath(c, p, "M-10-6v12", 1.8f, false);
                drawPath(c, p, "M2-6l8 6M2 6l8-6", 1.4f, false);
                drawPath(c, p, "M4-9v18", 1.6f, false);
                break;
            case "sym-ground":
                drawPath(c, p, "M0-12v6M-10-6h20M-7 0h14M-4 6h8", 1.6f, false);
                break;
            case "sym-vcc":
                drawPath(c, p, "M0 12v-6M-7 6h14", 1.6f, false);
                drawText(c, p, -3f, -2f, 9f, "V");
                break;
            case "sym-vdd":
                drawPath(c, p, "M0 12v-8M-7 4h14", 1.6f, false);
                drawCircle(c, p, 0f, 0f, 2.6f, 1.6f, true);
                break;
            case "sym-ammeter":
                drawPath(c, p, "M-20 0h10M10 0h10", 1.6f, false);
                drawCircle(c, p, 0f, 0f, 10f, 1.8f, false);
                drawText(c, p, -4f, 4f, 11f, "A");
                break;
            case "sym-voltmeter":
                drawPath(c, p, "M-20 0h10M10 0h10", 1.6f, false);
                drawCircle(c, p, 0f, 0f, 10f, 1.8f, false);
                drawText(c, p, -4f, 4f, 11f, "V");
                break;
            case "sym-transformer":
                drawPath(c, p, "M-20-10h14M-20 10h14M20-10h-14M20 10h-14", 1.6f, false);
                drawPath(c, p, "M-6-10a4 4 0 0 1 0 8a4 4 0 0 1 0 8", 1.6f, false);
                drawPath(c, p, "M6-10a4 4 0 0 0 0 8a4 4 0 0 0 0 8", 1.6f, false);
                drawPath(c, p, "M-1.5-11v22M1.5-11v22", 1.2f, false);
                break;
            case "sym-buzzer":
                drawPath(c, p, "M-20 0h10M10 0h10", 1.6f, false);
                drawPath(c, p, "M-10-8v16L8 0Z", 1.8f, false);
                drawPath(c, p, "M12-7a10 10 0 0 1 0 14", 1.6f, false);
                drawPath(c, p, "M16-11a15 15 0 0 1 0 22", 1.2f, false);
                break;
            case "sym-speaker":
                drawPath(c, p, "M-20 0h10M10 0h10", 1.6f, false);
                drawPath(c, p, "M-10-7v14L6 0Z", 1.8f, false);
                drawPath(c, p, "M10-7a11 11 0 0 1 0 14", 1.6f, false);
                break;
            case "sym-antenna":
                drawPath(c, p, "M0 12V0M0 0L-9-11M0 0L9-11", 1.6f, false);
                drawPath(c, p, "M-2.6 12a2.6 2.6 0 1 0 5.2 0a2.6 2.6 0 1 0-5.2 0", 1.6f, true);
                break;
            case "sym-connector":
                drawPath(c, p, "M-20 0h8M12 0h8", 1.6f, false);
                drawRect(c, p, -12f, -8f, 24f, 16f, 2f, 1.8f, false);
                drawPath(c, p, "M-6-2.2a2.2 2.2 0 1 0 0 4.4a2.2 2.2 0 1 0 0-4.4", 1.6f, true);
                drawPath(c, p, "M6-2.2a2.2 2.2 0 1 0 0 4.4a2.2 2.2 0 1 0 0-4.4", 1.6f, true);
                break;
            case "sym-switch-spdt":
                drawPath(c, p, "M-20 0h12", 1.6f, false);
                drawPath(c, p, "M-8-2.4a2.4 2.4 0 1 0 0 4.8a2.4 2.4 0 1 0 0-4.8", 1.6f, true);
                drawPath(c, p, "M-6 0L14-10M14-10h6M14 10h6", 1.6f, false);
                drawPath(c, p, "M14-12.4a2.4 2.4 0 1 0 0 4.8a2.4 2.4 0 1 0 0-4.8", 1.6f, true);
                drawPath(c, p, "M14 7.6a2.4 2.4 0 1 0 0 4.8a2.4 2.4 0 1 0 0-4.8", 1.6f, true);
                break;
            case "sym-bridge":
                drawPath(c, p, "M-30-10H-12M-30 10H-12M12-10H30M12 10H30", 1.6f, false);
                drawRect(c, p, -12f, -12f, 24f, 24f, 2f, 1.8f, false);
                drawPath(c, p, "M-6-6l12 12M6-6L-6 6", 1.2f, false);
                drawText(c, p, 14f, -5f, 8f, "+");
                drawText(c, p, 14f, 16f, 8f, "−");
                break;
            case "sym-and":
                drawPath(c, p, "M-12-12h8a12 12 0 0 1 0 24h-8Z", 1.6f, false);
                drawPath(c, p, "M-30-8h18M-30 8h18M12 0h8", 1.6f, false);
                drawText(c, p, -6f, 4f, 9f, "&");
                break;
            case "sym-or":
                drawPath(c, p, "M-14-12h4a14 14 0 0 1 0 24h-4c5-4 7-8 7-12s-2-8-7-12Z", 1.6f, false);
                drawPath(c, p, "M-30-8h18M-30 8h18M11 0h9", 1.6f, false);
                drawText(c, p, -8f, 4f, 9f, "≥1");
                break;
            case "sym-not":
                drawPath(c, p, "M-12-12L12 0L-12 12Z", 1.6f, false);
                drawCircle(c, p, 15f, 0f, 3f, 1.8f, false);
                drawPath(c, p, "M-20 0h8M18 0h2", 1.6f, false);
                break;
        }
        c.restore();
    }

    private static void drawPath(Canvas c, Paint p, String d, float w, boolean fill) {
        p.setStyle(fill ? Paint.Style.FILL : Paint.Style.STROKE);
        p.setStrokeWidth(w);
        Path path = SvgPath.parse(d);
        c.drawPath(path, p);
    }

    private static void drawRect(Canvas c, Paint p, float x, float y, float w, float h, float r, float sw, boolean fill) {
        p.setStyle(fill ? Paint.Style.FILL : Paint.Style.STROKE);
        p.setStrokeWidth(sw);
        if (r > 0) c.drawRoundRect(new RectF(x, y, x + w, y + h), r, r, p);
        else c.drawRect(x, y, x + w, y + h, p);
    }

    private static void drawCircle(Canvas c, Paint p, float cx, float cy, float r, float sw, boolean fill) {
        p.setStyle(fill ? Paint.Style.FILL : Paint.Style.STROKE);
        p.setStrokeWidth(sw);
        c.drawCircle(cx, cy, r, p);
    }

    private static void drawText(Canvas c, Paint p, float x, float y, float size, String s) {
        p.setStyle(Paint.Style.FILL);
        p.setTextSize(size);
        c.drawText(s, x, y, p);
    }
}
