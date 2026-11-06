package com.dshwork.mynotes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;

/** 一点共用的界面小工具，保持三个页面风格一致。 */
public class Ui {
    public static int BG = 0xFFF4F1EA;
    public static int CARD = 0xFFFFFDF8;
    public static int INK = 0xFF211E19;
    public static int INK2 = 0xFF4B463D;
    public static int LINE = 0xFFE5DDCC;
    public static int ACCENT = 0xFF2C5D4A;
    public static int ACCENT2 = 0xFF3D7A61;
    public static int MUTED = 0xFF8B8477;
    public static int ACCENT_SOFT = 0xFFE6EFE9;
    public static int GOLD = 0xFFA9762A;
    public static int RED = 0xFFA2453A;
    public static int LINE_STRONG = 0xFFD6CCB6;

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

    /** 顶部状态栏 / 底部导航栏留白（Android 15 强制边到边，必须自己让位） */
    public static void padForInsets(View root) {
        final int l = root.getPaddingLeft(), t0 = root.getPaddingTop();
        final int r = root.getPaddingRight(), b0 = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public android.view.WindowInsets onApplyWindowInsets(View v, android.view.WindowInsets insets) {
                int top = insets.getSystemWindowInsetTop();
                int bottom = insets.getSystemWindowInsetBottom();
                v.setPadding(l, t0 + top, r, b0 + bottom);
                return insets;
            }
        });
        root.requestApplyInsets();
    }

    /** 状态栏/导航栏底色与图标明暗跟随主题 */
    public static void tintSystemBars(android.app.Activity a) {
        try {
            android.view.Window w = a.getWindow();
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(BG));
            w.setStatusBarColor(BG);
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                w.setNavigationBarColor(BG);
            }
            View decor = w.getDecorView();
            int flags = decor.getSystemUiVisibility();
            if (dark) flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            else flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decor.setSystemUiVisibility(flags);
        } catch (Exception ignored) {
        }
    }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable card(Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(CARD);
        d.setCornerRadius(dp(c, 16));
        d.setStroke(dp(c, 1), LINE);
        return d;
    }

    public static GradientDrawable cardSelected(Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(CARD);
        d.setCornerRadius(dp(c, 16));
        d.setStroke(dp(c, 2), ACCENT);
        return d;
    }

    public static TextView text(Context c, String s, float size, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(size * scale);
        t.setTextColor(color);
        t.setLineSpacing(0, 1.16f);
        return t;
    }

    public static TextView button(Context c, String s) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(13.5f * scale);
        t.setTextColor(0xFFFFFFFF);
        t.setGravity(android.view.Gravity.CENTER);
        t.setMinHeight(dp(c, 34));
        t.setPadding(dp(c, 14), 0, dp(c, 14), 0);
        GradientDrawable d = new GradientDrawable();
        d.setColor(ACCENT);
        d.setCornerRadius(dp(c, 9));
        t.setBackground(d);
        return t;
    }

    /** 次要按钮：白底、细边、深字（网页版 .btn 默认样式） */
    public static TextView buttonGhost(Context c, String s) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(13.5f * scale);
        t.setTextColor(INK);
        t.setGravity(android.view.Gravity.CENTER);
        t.setMinHeight(dp(c, 34));
        t.setPadding(dp(c, 14), 0, dp(c, 14), 0);
        GradientDrawable d = new GradientDrawable();
        d.setColor(CARD);
        d.setCornerRadius(dp(c, 9));
        d.setStroke(dp(c, 1), LINE_STRONG);
        t.setBackground(d);
        return t;
    }

    /** 工具行里的胶囊按钮：单行、等高、不换行 */
    public static TextView chip(Context c, String label, boolean primary) {
        TextView t = new TextView(c);
        t.setText(label);
        t.setTextSize(12.5f * scale);
        t.setSingleLine(true);
        t.setMaxLines(1);
        t.setGravity(android.view.Gravity.CENTER);
        t.setMinHeight(dp(c, 32));
        t.setPadding(dp(c, 12), 0, dp(c, 12), 0);
        if (primary) {
            t.setTextColor(0xFFFFFFFF);
            t.setBackground(button(c, "").getBackground());
        } else {
            t.setTextColor(INK);
            t.setBackground(card(c));
        }
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(c, 8);
        t.setLayoutParams(lp);
        return t;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        return v;
    }
}
