package com.dshwork.mynotes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** 用量折线图：横轴日期，纵轴 token 总量。 */
public class UsageChartView extends View {

    private final List<Float> values = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();

    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    public UsageChartView(Context c) {
        super(c);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Ui.dp(c, 2));
        line.setColor(Ui.ACCENT);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0x332C5D4A);
        grid.setStyle(Paint.Style.STROKE);
        grid.setStrokeWidth(1);
        grid.setColor(Ui.LINE);
        text.setColor(Ui.MUTED);
        text.setTextSize(Ui.dp(c, 10));
    }

    /** rows: {日期, 次数, 输入, 输出}，按日期倒序传入，这里翻成从左到右。 */
    public void setRows(List<String[]> rows) {
        values.clear();
        labels.clear();
        for (int i = rows.size() - 1; i >= 0; i--) {
            String[] r = rows.get(i);
            long inTok = parse(r[2]);
            long outTok = parse(r[3]);
            values.add((float) (inTok + outTok));
            String d = r[0] == null ? "" : r[0];
            labels.add(d.length() >= 10 ? d.substring(5) : d);
        }
        invalidate();
    }

    private long parse(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float padL = Ui.dp(getContext(), 38);
        float padR = Ui.dp(getContext(), 10);
        float padT = Ui.dp(getContext(), 12);
        float padB = Ui.dp(getContext(), 22);
        float w = getWidth() - padL - padR;
        float h = getHeight() - padT - padB;
        if (w <= 0 || h <= 0) return;

        c.drawLine(padL, padT + h, padL + w, padT + h, grid);
        c.drawLine(padL, padT, padL, padT + h, grid);

        if (values.isEmpty()) {
            c.drawText("还没有数据", padL + Ui.dp(getContext(), 8), padT + h / 2, text);
            return;
        }

        float max = 1;
        for (float v : values) max = Math.max(max, v);
        c.drawText(fmt(max), Ui.dp(getContext(), 2), padT + Ui.dp(getContext(), 10), text);
        c.drawText("0", Ui.dp(getContext(), 2), padT + h, text);

        float stepX = values.size() > 1 ? w / (values.size() - 1) : 0;
        Path p = new Path();
        Path area = new Path();
        for (int i = 0; i < values.size(); i++) {
            float x = padL + stepX * i;
            float y = padT + h - (values.get(i) / max) * h;
            if (i == 0) {
                p.moveTo(x, y);
                area.moveTo(x, padT + h);
                area.lineTo(x, y);
            } else {
                p.lineTo(x, y);
                area.lineTo(x, y);
            }
        }
        if (values.size() > 1) {
            area.lineTo(padL + w, padT + h);
            area.close();
            c.drawPath(area, fill);
        }
        c.drawPath(p, line);

        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setColor(Ui.ACCENT);
        for (int i = 0; i < values.size(); i++) {
            float x = padL + stepX * i;
            float y = padT + h - (values.get(i) / max) * h;
            c.drawCircle(x, y, Ui.dp(getContext(), 2.2f), dot);
        }

        int skip = Math.max(1, values.size() / 6);
        for (int i = 0; i < labels.size(); i += skip) {
            float x = padL + stepX * i;
            c.drawText(labels.get(i), x - Ui.dp(getContext(), 10), padT + h + Ui.dp(getContext(), 14), text);
        }
    }

    private String fmt(float v) {
        if (v < 1000) return String.valueOf((int) v);
        if (v < 1000000) return String.format(java.util.Locale.CHINA, "%.0fk", v / 1000);
        return String.format(java.util.Locale.CHINA, "%.1fM", v / 1000000);
    }
}
