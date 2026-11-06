package com.dshwork.mynotes;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 知识点卡片：结构对照网页版 itemCard（kindex / 标题 / ƒ·图 / 重要度点 / 正文 / 展开 / 引用）。 */
public class ListRows {

    public static class NoteRow {
        public LinearLayout card;
        public TextView kindex;
        public TextView name;
        public TextView badges;
        public TextView imp;
        public TextView preview;
        public TextView more;
        public TextView quote;
        public View divider;
        public LinearLayout foot;
        public TextView tags;
        public TextView src;
    }

    public static NoteRow create(Context c) {
        NoteRow row = new NoteRow();
        row.card = new LinearLayout(c);
        row.card.setOrientation(LinearLayout.VERTICAL);
        row.card.setPadding(Ui.dp(c, 17), Ui.dp(c, 16), Ui.dp(c, 17), Ui.dp(c, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(c, 14);
        row.card.setLayoutParams(lp);

        LinearLayout head = new LinearLayout(c);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);

        row.kindex = Ui.text(c, "", 10.5f, Ui.ACCENT);
        row.kindex.setTypeface(Typeface.DEFAULT_BOLD);
        android.graphics.drawable.GradientDrawable kb = new android.graphics.drawable.GradientDrawable();
        kb.setColor(Ui.ACCENT_SOFT);
        kb.setCornerRadius(Ui.dp(c, 5));
        row.kindex.setBackground(kb);
        row.kindex.setPadding(Ui.dp(c, 6), Ui.dp(c, 1), Ui.dp(c, 6), Ui.dp(c, 1));
        LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        klp.rightMargin = Ui.dp(c, 8);
        head.addView(row.kindex, klp);

        row.name = Ui.text(c, "", 15.5f, Ui.INK);
        row.name.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(row.name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.badges = Ui.text(c, "", 11, Ui.MUTED);
        head.addView(row.badges);

        row.imp = Ui.text(c, "", 9, Ui.MUTED);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ilp.leftMargin = Ui.dp(c, 8);
        head.addView(row.imp, ilp);

        row.card.addView(head);

        row.preview = Ui.text(c, "", 12.5f, Ui.INK2);
        row.preview.setPadding(0, Ui.dp(c, 6), 0, 0);
        row.card.addView(row.preview);

        row.more = Ui.text(c, "", 12f, Ui.ACCENT2);
        row.more.setPadding(0, Ui.dp(c, 6), 0, 0);
        row.more.setVisibility(View.GONE);
        row.card.addView(row.more);

        row.quote = Ui.text(c, "", 12f, Ui.MUTED);
        row.quote.setPadding(0, Ui.dp(c, 6), 0, 0);
        row.quote.setVisibility(View.GONE);
        row.card.addView(row.quote);

        row.divider = new View(c);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 1));
        dlp.topMargin = Ui.dp(c, 11);
        android.graphics.drawable.GradientDrawable lineBg = new android.graphics.drawable.GradientDrawable();
        lineBg.setColor(Ui.LINE);
        row.divider.setBackground(lineBg);
        row.divider.setVisibility(View.GONE);
        row.card.addView(row.divider, dlp);

        row.foot = new LinearLayout(c);
        row.foot.setOrientation(LinearLayout.HORIZONTAL);
        row.foot.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.foot.setPadding(0, Ui.dp(c, 8), 0, 0);
        row.tags = Ui.text(c, "", 10.5f, Ui.GOLD);
        row.foot.addView(row.tags, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.src = Ui.text(c, "", 11, Ui.MUTED);
        row.foot.addView(row.src);
        row.foot.setVisibility(View.GONE);
        row.card.addView(row.foot);

        row.card.setTag(row);
        return row;
    }

    public static NoteRow obtain(Context c, View convertView) {
        if (convertView != null && convertView.getTag() instanceof NoteRow) {
            return (NoteRow) convertView.getTag();
        }
        return create(c);
    }

    public static void bind(Context c, NoteRow row, String tagText, String name, String preview, int lines) {
        bind(c, row, tagText, name, preview, lines, 0, false, false, null);
    }

    public static void bind(Context c, NoteRow row, String tagText, String name, String preview, int lines, int importance) {
        bind(c, row, tagText, name, preview, lines, importance, false, false, null);
    }

    /** 完整版：kindex 文本（P3 / 拓展）、重要度 1-5、是否有公式/画图、原文引用 */
    public static void bind(Context c, NoteRow row, String indexLabel, String name, String preview, int lines,
                            int importance, boolean hasFormula, boolean hasFigure, String quote) {
        bind(c, row, indexLabel, name, preview, lines, importance, hasFormula, hasFigure, quote, null, null, false);
    }

    public static void bind(Context c, NoteRow row, String indexLabel, String name, String preview, int lines,
                            int importance, boolean hasFormula, boolean hasFigure, String quote,
                            String tags, String srcLabel, boolean isExt) {
        android.graphics.drawable.GradientDrawable bg = Ui.card(c);
        bg.setCornerRadius(Ui.dp(c, 14));
        if (isExt) {
            bg.setColor(0xFFFFFDF6);
            bg.setStroke(Ui.dp(c, 1), 0xFFECDFC4);
        }
        row.card.setBackground(bg);

        row.kindex.setText(indexLabel == null ? "" : indexLabel);
        row.kindex.setVisibility(indexLabel == null || indexLabel.isEmpty() ? View.GONE : View.VISIBLE);

        row.name.setText(name == null ? "" : name);

        StringBuilder badge = new StringBuilder();
        if (hasFormula) badge.append("ƒ");
        if (hasFigure) badge.append(badge.length() > 0 ? " 图" : "图");
        row.badges.setText(badge.toString());
        row.badges.setVisibility(badge.length() == 0 ? View.GONE : View.VISIBLE);

        if (importance > 0) {
            StringBuilder dots = new StringBuilder();
            for (int i = 0; i < 5; i++) dots.append(i < importance ? "●" : "○");
            row.imp.setText(dots.toString());
            row.imp.setVisibility(View.VISIBLE);
        } else {
            row.imp.setVisibility(View.GONE);
        }

        String p = preview == null ? "" : preview.replace("\n", " ").trim();
        if (lines == 0) {
            p = "";
        } else if (lines > 0) {
            int limit = lines * 24;
            if (p.length() > limit) p = p.substring(0, limit) + "…";
        }
        row.preview.setText(p);
        row.preview.setVisibility(p.isEmpty() ? View.GONE : View.VISIBLE);

        StringBuilder footText = new StringBuilder();
        if (isExt) footText.append("AI 补充 · 请核对");
        if (tags != null && !tags.trim().isEmpty()) {
            if (footText.length() > 0) footText.append("   ");
            footText.append(tags.replace(",", " · "));
        }
        row.tags.setText(footText.toString());
        row.tags.setVisibility(footText.length() == 0 ? View.GONE : View.VISIBLE);
        row.src.setText(srcLabel == null ? "" : srcLabel);
        boolean showFoot = footText.length() > 0 || (srcLabel != null && !srcLabel.isEmpty());
        row.divider.setVisibility(showFoot ? View.VISIBLE : View.GONE);
        row.foot.setVisibility(showFoot ? View.VISIBLE : View.GONE);

        if (quote != null && !quote.trim().isEmpty()) {
            row.quote.setText("「" + quote.trim() + "」");
            row.quote.setVisibility(View.VISIBLE);
        } else {
            row.quote.setVisibility(View.GONE);
        }
    }
}
