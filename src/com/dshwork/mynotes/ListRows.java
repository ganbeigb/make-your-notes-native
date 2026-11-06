package com.dshwork.mynotes;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 知识点卡片行的统一构建与绑定，避免每个列表各写一份。 */
public class ListRows {

    public static class NoteRow {
        public LinearLayout card;
        public TextView tag;
        public TextView name;
        public TextView preview;
        public TextView more;
    }

    private static int dp(Context c, float v) {
        return Ui.dp(c, v);
    }

    public static NoteRow create(Context c) {
        NoteRow row = new NoteRow();
        row.card = new LinearLayout(c);
        row.card.setOrientation(LinearLayout.VERTICAL);
        row.card.setPadding(Ui.dp(c, 16), Ui.dp(c, 12), Ui.dp(c, 16), Ui.dp(c, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(c, 10);
        row.card.setLayoutParams(lp);

        row.tag = Ui.text(c, "", 11, Ui.ACCENT);
        row.card.addView(row.tag);

        row.name = Ui.text(c, "", 16, Ui.INK);
        row.name.setTypeface(Typeface.DEFAULT_BOLD);
        row.card.addView(row.name);

        row.preview = Ui.text(c, "", 12, Ui.INK2);
        row.preview.setPadding(0, Ui.dp(c, 4), 0, 0);
        row.card.addView(row.preview);

        row.more = Ui.text(c, "", 11.5f, Ui.ACCENT);
        row.more.setPadding(0, dp(c, 4), 0, 0);
        row.more.setVisibility(View.GONE);
        row.card.addView(row.more);

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
        bind(c, row, tagText, name, preview, lines, 0);
    }

    public static void bind(Context c, NoteRow row, String tagText, String name, String preview, int lines, int importance) {
        row.card.setBackground(Ui.card(c));
        row.tag.setText(tagText == null ? "" : tagText);
        row.tag.setVisibility(tagText == null || tagText.isEmpty() ? View.GONE : View.VISIBLE);
        String label = name == null ? "" : name;
        if (importance >= 4) label = "★★ " + label;
        else if (importance == 3) label = "★ " + label;
        row.name.setText(label);

        String p = preview == null ? "" : preview.replace("\n", " ").trim();
        if (lines == 0) {
            p = "";
        } else if (lines > 0) {
            int limit = lines * 24;
            if (p.length() > limit) p = p.substring(0, limit) + "…";
        }
        row.preview.setText(p);
        row.preview.setVisibility(p.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
