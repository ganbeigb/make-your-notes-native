package com.dshwork.mynotes;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** 全局搜索：跨科目按标题 / 正文 / 公式查找。 */
public class SearchActivity extends BaseActivity {

    private Db db;
    private List<String[]> hits = new ArrayList<>();
    private BaseAdapter adapter;
    private TextView empty;

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
        bar.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 6));
        TextView back = Ui.text(this, "‹", 26, Ui.ACCENT);
        back.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 10), 0);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bar.addView(back);
        TextView title = Ui.text(this, "搜索", 18, Ui.INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(bar);

        final EditText input = new EditText(this);
        input.setHint("输入关键字，比如 电容、能量、E=mc");
        input.setTextSize(15);
        input.setTextColor(Ui.INK);
        input.setBackground(Ui.card(this));
        input.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ilp.setMargins(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 8));
        root.addView(input, ilp);

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 12));
        list.setClipToPadding(false);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        empty = Ui.text(this, "输入关键字开始搜索", 13, Ui.INK2);
        empty.setGravity(Gravity.CENTER);
        root.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return hits.size();
            }

            @Override
            public Object getItem(int position) {
                return hits.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ListRows.NoteRow row = ListRows.obtain(SearchActivity.this, convertView);
                String[] h = hits.get(position);
                ListRows.bind(SearchActivity.this, row, h[2], h[1], "", 0);
                return row.card;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(SearchActivity.this, NoteActivity.class);
                i.putExtra("noteId", hits.get(position)[0]);
                startActivity(i);
            }
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                doSearch(s.toString());
            }
        });

        setContentView(root);
    }

    private void doSearch(String q) {
        hits.clear();
        if (q != null && q.trim().length() > 0) {
            hits.addAll(db.search(q));
        }
        adapter.notifyDataSetChanged();
        boolean none = hits.isEmpty();
        empty.setVisibility(none ? View.VISIBLE : View.GONE);
        empty.setText(q == null || q.trim().isEmpty() ? "输入关键字开始搜索" : "没有找到匹配的知识点");
    }
}
