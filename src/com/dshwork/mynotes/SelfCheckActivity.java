package com.dshwork.mynotes;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** 自检页：点「开始自检」跑一遍各子系统的运行期断言。 */
public class SelfCheckActivity extends BaseActivity {

    private LinearLayout list;
    private TextView summary;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Ui.apply(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 10));
        TextView back = Ui.text(this, "‹", 26, Ui.ACCENT);
        back.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 10), 0);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bar.addView(back);
        TextView title = Ui.text(this, "自检", 19, Ui.INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView run = Ui.button(this, "开始");
        run.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });
        bar.addView(run);
        root.addView(bar);

        summary = Ui.text(this, "点右上角「开始」运行自检（不会改动你的数据）", 13, Ui.INK2);
        summary.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 8));
        root.addView(summary);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 24));
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);
    }

    private void start() {
        list.removeAllViews();
        summary.setText("运行中…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<SelfCheck.Item> items = SelfCheck.run(SelfCheckActivity.this);
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        int pass = 0;
                        for (SelfCheck.Item it : items) {
                            if (it.ok) pass++;
                            addRow(it);
                        }
                        summary.setText("完成：" + pass + " / " + items.size() + " 项通过");
                    }
                });
            }
        }).start();
    }

    private void addRow(SelfCheck.Item it) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Ui.card(this));
        box.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(this, 8);
        box.setLayoutParams(lp);

        TextView head = Ui.text(this, (it.ok ? "✓ " : "✗ ") + it.name, 15, it.ok ? Ui.ACCENT : 0xFFC0392B);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(head);

        TextView detail = Ui.text(this, it.detail == null ? "" : it.detail, 12, Ui.INK2);
        detail.setPadding(0, Ui.dp(this, 4), 0, 0);
        box.addView(detail);

        list.addView(box);
    }
}
