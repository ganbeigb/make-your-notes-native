package com.dshwork.mynotes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;

/** 一点共用的界面小工具，保持三个页面风格一致。 */
public class Ui {
    public static int BG = 0xFFFBF8F1;
    public static int CARD = 0xFFFFFFFF;
    public static int INK = 0xFF23302A;
    public static int INK2 = 0xFF5A6A62;
    public static int LINE = 0xFFE6DDC8;
    public static int ACCENT = 0xFF2F5D4B;

    public static boolean dark = false;

    private static float scale = 1f;

    /** 读设置，应用字号与主题（每个 Activity 的 onCreate 先调这个） */
    public static void apply(Context c) {
        Prefs p = new Prefs(c);
        setScale(p.uiScale());
        boolean d;
        String t = p.theme();
        if ("dark".equals(t)) d = true;
        else if ("light".equals(t)) d = false;
        else {
            int mode = c.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            d = mode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }
        applyTheme(d);
    }

    /** 当设置里的主题与当前渲染用的主题不一致时返回 true */
    public static boolean themeChanged(Context c) {
        return resolvedDark(c) != dark;
    }

    public static boolean resolvedDark(Context c) {
        Prefs p = new Prefs(c);
        String t = p.theme();
        if ("dark".equals(t)) return true;
        if ("light".equals(t)) return false;
        int mode = c.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    public static void applyTheme(boolean useDark) {
        dark = useDark;
        if (useDark) {
            BG = 0xFF121815;
            CARD = 0xFF1C2420;
            INK = 0xFFE6EEE8;
            INK2 = 0xFF9BA8A0;
            LINE = 0xFF2C3730;
            ACCENT = 0xFF6FBF95;
        } else {
            BG = 0xFFFBF8F1;
            CARD = 0xFFFFFFFF;
            INK = 0xFF23302A;
            INK2 = 0xFF5A6A62;
            LINE = 0xFFE6DDC8;
            ACCENT = 0xFF2F5D4B;
        }
    }

    public static void setScale(float s) {
        scale = s <= 0 ? 1f : s;
    }

    public static float scale() {
        return scale;
    }

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
        t.setTextSize(size * scale);
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
