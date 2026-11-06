package com.dshwork.mynotes;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** 用量统计：总览 + 按用途 + 按天。 */
public class UsageActivity extends Activity {

    private Db db;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Ui.setScale(new Prefs(this).uiScale());
        db = new Db(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 10));
        TextView back = Ui.text(this, "‹", 26, Ui.ACCENT);
        back.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 12), 0);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bar.addView(back);
        TextView title = Ui.text(this, "用量", 19, Ui.INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(bar);

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 24));

        long[] total = db.usageTotals();
        body.addView(card("总览", new String[]{
                "调用次数：" + total[2] + " 次",
                "输入 tokens：" + fmt(total[0]),
                "输出 tokens：" + fmt(total[1])
        }));

        List<String[]> kinds = db.usageByKind();
        if (!kinds.isEmpty()) {
            String[] lines = new String[kinds.size()];
            for (int i = 0; i < kinds.size(); i++) {
                String[] r = kinds.get(i);
                lines[i] = labelOf(r[0]) + "：" + r[1] + " 次，输入 " + fmt(parse(r[2])) + "，输出 " + fmt(parse(r[3]));
            }
            body.addView(card("按用途", lines));
        }

        List<String[]> days = db.usageByDay(30);
        if (!days.isEmpty()) {
            String[] lines = new String[days.size()];
            for (int i = 0; i < days.size(); i++) {
                String[] r = days.get(i);
                lines[i] = r[0] + "　" + r[1] + " 次　输入 " + fmt(parse(r[2])) + "　输出 " + fmt(parse(r[3]));
            }
            body.addView(card("最近 30 天", lines));
        } else {
            TextView empty = Ui.text(this, "还没有调用记录。导入课件或做 AI 整理后会记在这里。", 13, Ui.INK2);
            empty.setPadding(0, Ui.dp(this, 20), 0, 0);
            body.addView(empty);
        }

        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    private long parse(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private String labelOf(String kind) {
        if ("extract".equals(kind)) return "识图提取";
        if ("organize".equals(kind)) return "自动整理";
        if ("title".equals(kind)) return "标题生成";
        return kind == null ? "其他" : kind;
    }

    private String fmt(long n) {
        if (n < 1000) return String.valueOf(n);
        if (n < 1000000) return String.format(java.util.Locale.CHINA, "%.1fk", n / 1000.0);
        return String.format(java.util.Locale.CHINA, "%.1fM", n / 1000000.0);
    }

    private View card(String title, String[] lines) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Ui.card(this));
        box.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(this, 12);
        box.setLayoutParams(lp);

        TextView h = Ui.text(this, title, 14, Ui.INK);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(h);

        for (String line : lines) {
            TextView v = Ui.text(this, line, 13, Ui.INK2);
            v.setPadding(0, Ui.dp(this, 8), 0, 0);
            box.addView(v);
        }
        return box;
    }
}
