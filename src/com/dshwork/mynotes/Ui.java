package com.dshwork.mynotes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;

/** 一点共用的界面小工具，保持三个页面风格一致。 */
public class Ui {
    public static final int BG = 0xFFFBF8F1;
    public static final int CARD = 0xFFFFFFFF;
    public static final int INK = 0xFF23302A;
    public static final int INK2 = 0xFF5A6A62;
    public static final int LINE = 0xFFE6DDC8;
    public static final int ACCENT = 0xFF2F5D4B;

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable card(Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(CARD);
        d.setCornerRadius(dp(c, 12));
        d.setStroke(dp(c, 1), LINE);
        return d;
    }

    public static TextView text(Context c, String s, float size, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    public static TextView button(Context c, String s) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(14);
        t.setTextColor(0xFFFFFFFF);
        t.setGravity(android.view.Gravity.CENTER);
        t.setPadding(dp(c, 14), dp(c, 8), dp(c, 14), dp(c, 8));
        GradientDrawable d = new GradientDrawable();
        d.setColor(ACCENT);
        d.setCornerRadius(dp(c, 9));
        t.setBackground(d);
        return t;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        return v;
    }
}
