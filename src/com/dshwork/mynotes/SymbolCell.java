package com.dshwork.mynotes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

/** 元件格里的一格：上面画符号，下面写名字。 */
public class SymbolCell extends View {

    private final String id;
    private final String label;
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean selected;

    public SymbolCell(Context c, String id, String label) {
        super(c);
        this.id = id;
        this.label = label;
        ink.setStyle(Paint.Style.STROKE);
        ink.setStrokeWidth(Ui.dp(c, 1.5f));
        ink.setStrokeCap(Paint.Cap.ROUND);
        ink.setStrokeJoin(Paint.Join.ROUND);
        ink.setColor(Ui.INK);
        text.setColor(Ui.INK2);
        text.setTextSize(Ui.dp(c, 9.5f));
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT);
        bg.setColor(Ui.CARD);
    }

    public void setSelected(boolean v) {
        selected = v;
        invalidate();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = Ui.dp(getContext(), 62);
        int h = Ui.dp(getContext(), 60);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        android.graphics.RectF r = new android.graphics.RectF(2, 2, w - 2, h - 2);
        Paint p = new Paint(bg);
        if (selected) p.setColor(Ui.ACCENT);
        c.drawRoundRect(r, Ui.dp(getContext(), 10), Ui.dp(getContext(), 10), p);

        Paint line = new Paint(ink);
        if (selected) line.setColor(0xFFFFFFFF);
        float scale = Math.min(w, h * 0.72f) / 58f;
        c.save();
        c.translate(w / 2f, h * 0.40f);
        c.scale(scale, scale);
        Symbols.draw(c, id, 0, 0, line);
        c.restore();

        Paint t = new Paint(text);
        if (selected) t.setColor(0xFFFFFFFF);
        c.drawText(label, w / 2f, h - Ui.dp(getContext(), 7), t);
    }
}
