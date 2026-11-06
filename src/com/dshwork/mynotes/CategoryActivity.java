package com.dshwork.mynotes;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 分类浏览：上方分类胶囊筛选，下方知识点列表。 */
public class CategoryActivity extends BaseActivity {

    private Db db;
    private Prefs prefs;
    private String subjectId;
    private String subjectName;
    private List<Db.Note> all = new ArrayList<>();
    private List<Db.Note> shown = new ArrayList<>();
    private String filterId = null; // null = 全部
    private BaseAdapter adapter;
    private LinearLayout chipRow;
    private TextView empty;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Ui.apply(this);
        db = new Db(this);
        prefs = new Prefs(this);
        subjectId = getIntent().getStringExtra("subjectId");
        subjectName = getIntent().getStringExtra("subjectName");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 8));
        TextView back = Ui.text(this, "‹", 26, Ui.ACCENT);
        back.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 10), 0);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bar.addView(back);
        TextView title = Ui.text(this, (subjectName == null ? "科目" : subjectName) + " · 分类", 17, Ui.INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(bar);

        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        modes.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 6));
        TextView listMode = Ui.chip(this, "列表", true);
        modes.addView(listMode);
        TextView mapMode = Ui.chip(this, "导图", false);
        mapMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mi = new Intent(CategoryActivity.this, MindMapActivity.class);
                mi.putExtra("subjectId", subjectId);
                mi.putExtra("subjectName", subjectName);
                startActivity(mi);
            }
        });
        modes.addView(mapMode);
        root.addView(modes);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 8));
        scroll.addView(chipRow);
        root.addView(scroll);

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 12));
        list.setClipToPadding(false);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        empty = Ui.text(this, "这个分类下还没有知识点。", 14, Ui.INK2);
        empty.setGravity(Gravity.CENTER);
        root.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return shown.size();
            }

            @Override
            public Object getItem(int position) {
                return shown.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ListRows.NoteRow row = ListRows.obtain(CategoryActivity.this, convertView);
                Db.Note n = shown.get(position);
                String path = n.categoryId == null ? "未分类" : db.categoryPath(n.categoryId);
                ListRows.bind(CategoryActivity.this, row, path, n.name, n.content, 2);
                return row.card;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(CategoryActivity.this, NoteActivity.class);
                i.putExtra("noteId", shown.get(position).id);
                startActivity(i);
            }
        });

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        all = db.notesSorted(subjectId, prefs.sort());
        buildChips();
        applyFilter();
    }

    private void buildChips() {
        chipRow.removeAllViews();
        chipRow.addView(chip("全部 (" + all.size() + ")", null));
        List<Db.Category> cats = db.categories(subjectId);
        Map<String, Integer> counts = new HashMap<>();
        Map<String, List<String>> children = new HashMap<>();
        for (Db.Category c : cats) {
            children.put(c.id, new ArrayList<String>());
        }
        for (Db.Note n : all) {
            String id = n.categoryId;
            while (id != null) {
                Integer c0 = counts.get(id);
                counts.put(id, c0 == null ? 1 : c0 + 1);
                Db.Category c = findCat(cats, id);
                id = c == null ? null : c.parentId;
            }
        }
        for (Db.Category c : cats) {
            String path = db.categoryPath(c.id);
            int cnt = counts.get(c.id) == null ? 0 : counts.get(c.id);
            if (cnt == 0) continue;
            chipRow.addView(chip(path + " (" + cnt + ")", c.id));
        }
    }

    private Db.Category findCat(List<Db.Category> cats, String id) {
        for (Db.Category c : cats) if (c.id.equals(id)) return c;
        return null;
    }

    private View chip(String label, final String id) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(13);
        boolean active = (id == null && filterId == null) || (id != null && id.equals(filterId));
        t.setTextColor(active ? 0xFFFFFFFF : Ui.INK);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(this, 12), Ui.dp(this, 7), Ui.dp(this, 12), Ui.dp(this, 7));
        t.setBackground(active ? Ui.button(this, "").getBackground() : Ui.card(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Ui.dp(this, 8);
        t.setLayoutParams(lp);
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterId = id;
                buildChips();
                applyFilter();
            }
        });
        return t;
    }

    private void applyFilter() {
        shown.clear();
        for (Db.Note n : all) {
            if (filterId == null) {
                shown.add(n);
            } else if (n.categoryId != null && (n.categoryId.equals(filterId) || db.categoryPath(n.categoryId).startsWith(db.categoryPath(filterId)))) {
                shown.add(n);
            }
        }
        adapter.notifyDataSetChanged();
        empty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
