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

import java.util.List;

/** 用量统计：总览 + 按用途 + 按天。 */
public class UsageActivity extends BaseActivity {

    private Db db;
    private LinearLayout body;
    private int dayRange = 30;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Ui.apply(this);
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
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 24));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        render();
    }

    private void render() {
        body.removeAllViews();

        LinearLayout ranges = new LinearLayout(this);
        ranges.setOrientation(LinearLayout.HORIZONTAL);
        ranges.setPadding(0, 0, 0, Ui.dp(this, 10));
        String[] rangeLabels = {"7 天", "30 天", "90 天", "全部"};
        final int[] rangeValues = {7, 30, 90, 3650};
        for (int i = 0; i < rangeLabels.length; i++) {
            final int days = rangeValues[i];
            TextView b = Ui.chip(this, rangeLabels[i], dayRange == days);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dayRange = days;
                    render();
                }
            });
            ranges.addView(b);
        }
        body.addView(ranges);

        UsageChartView chart = new UsageChartView(this);
        chart.setRows(db.usageByDay(dayRange));
        LinearLayout chartBox = new LinearLayout(this);
        chartBox.setOrientation(LinearLayout.VERTICAL);
        chartBox.setBackground(Ui.card(this));
        chartBox.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 6));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 190));
        clp.bottomMargin = Ui.dp(this, 12);
        chartBox.addView(Ui.text(this, dayRange >= 3650 ? "全部时间 tokens" : "最近 " + dayRange + " 天 tokens", 13, Ui.INK2));
        chartBox.addView(chart, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        body.addView(chartBox, clp);

        long[] total = db.usageTotals();
        Prefs prefs = new Prefs(this);
        float priceIn = prefs.priceIn();
        float priceOut = prefs.priceOut();
        java.util.List<String> overview = new java.util.ArrayList<>();
        overview.add("调用次数：" + total[2] + " 次");
        overview.add("输入 tokens：" + fmt(total[0]));
        overview.add("输出 tokens：" + fmt(total[1]));
        if (priceIn > 0 || priceOut > 0) {
            double cost = total[0] / 1000000.0 * priceIn + total[1] / 1000000.0 * priceOut;
            overview.add(String.format(java.util.Locale.CHINA, "预估费用：¥%.4f", cost));
        }
        body.addView(card("总览", overview.toArray(new String[0])));

        List<String[]> kinds = db.usageByKind();
        if (!kinds.isEmpty()) {
            String[] lines = new String[kinds.size()];
            for (int i = 0; i < kinds.size(); i++) {
                String[] r = kinds.get(i);
                lines[i] = labelOf(r[0]) + "：" + r[1] + " 次，输入 " + fmt(parse(r[2])) + "，输出 " + fmt(parse(r[3]));
            }
            body.addView(card("按用途", lines));
        }

        List<String[]> bySubject = db.usageBySubject();
        if (!bySubject.isEmpty()) {
            String[] lines = new String[bySubject.size()];
            for (int i = 0; i < bySubject.size(); i++) {
                String[] r = bySubject.get(i);
                lines[i] = r[0] + "：" + r[1] + " 次，输入 " + fmt(parse(r[2])) + "，输出 " + fmt(parse(r[3]));
            }
            body.addView(card("按科目", lines));
        }

        List<String[]> days = db.usageByDay(dayRange);
        if (!days.isEmpty()) {
            String[] lines = new String[days.size()];
            for (int i = 0; i < days.size(); i++) {
                String[] r = days.get(i);
                lines[i] = r[0] + "　" + r[1] + " 次　输入 " + fmt(parse(r[2])) + "　输出 " + fmt(parse(r[3]));
            }
            body.addView(card(dayRange >= 3650 ? "全部时间" : "最近 " + dayRange + " 天", lines));
        } else {
            TextView empty = Ui.text(this, "还没有调用记录。导入课件或做 AI 整理后会记在这里。", 13, Ui.INK2);
            empty.setPadding(0, Ui.dp(this, 20), 0, 0);
            body.addView(empty);
        }

        TextView clear = Ui.text(this, "清空用量记录", 13, 0xFFC0392B);
        clear.setPadding(0, Ui.dp(this, 8), 0, 0);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new android.app.AlertDialog.Builder(UsageActivity.this)
                        .setTitle("清空用量")
                        .setMessage("会删除本机所有调用记录，不影响笔记。")
                        .setPositiveButton("清空", new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialog, int which) {
                                db.clearUsage();
                                finish();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
        body.addView(clear);
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
