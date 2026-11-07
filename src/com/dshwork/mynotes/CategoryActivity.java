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
    private TextView statText;
    private TextView empty;
    private TextView emptyTitle;
    private TextView emptyHint;
    private LinearLayout emptyBoxRef;

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
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        TextView title = Ui.text(this, subjectName == null ? "分类" : subjectName + " · 分类", 17, Ui.INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        titleBox.addView(title);
        TextView sub = Ui.text(this, "", 11.5f, Ui.MUTED);
        sub.setPadding(0, Ui.dp(this, 1), 0, 0);
        titleBox.addView(sub);
        statText = sub;
        bar.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
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

        final android.widget.Spinner sortSpinnerInline = new android.widget.Spinner(this);
        String[] sortLabels = {"最近创建", "按标题", "按重要度"};
        final String[] sortKeys2 = {"recent", "title", "importance"};
        android.widget.ArrayAdapter<String> sa = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, sortLabels);
        sortSpinnerInline.setAdapter(sa);
        int cur = 0;
        for (int k = 0; k < sortKeys2.length; k++) {
            if (sortKeys2[k].equals(prefs.sort())) cur = k;
        }
        sortSpinnerInline.setSelection(cur);
        sortSpinnerInline.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= sortKeys2.length) return;
                if (sortKeys2[position].equals(prefs.sort())) return;
                prefs.setSort(sortKeys2[position]);
                reload();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                Ui.dp(this, 124), ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.leftMargin = Ui.dp(this, 8);
        modes.addView(sortSpinnerInline, slp);

        root.addView(modes);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 8));
        android.widget.HorizontalScrollView chipScroll = new android.widget.HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.addView(chipRow);
        scroll.addView(chipScroll);
        root.addView(scroll);

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 16));
        list.setClipToPadding(false);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        emptyTitle = Ui.text(this, "还没有知识点", 14.5f, Ui.INK);
        emptyTitle.setTypeface(Typeface.DEFAULT_BOLD);
        emptyTitle.setGravity(Gravity.CENTER);
        emptyHint = Ui.text(this, "上传课件并完成识别后，这里会出现结构化的知识点卡片。", 12.5f, Ui.MUTED);
        emptyHint.setGravity(Gravity.CENTER);
        emptyHint.setPadding(0, Ui.dp(this, 6), 0, 0);

        LinearLayout emptyBox = new LinearLayout(this);
        emptyBox.setOrientation(LinearLayout.VERTICAL);
        emptyBox.setGravity(Gravity.CENTER);
        emptyBox.addView(emptyTitle);
        emptyBox.addView(emptyHint);
        emptyBox.setVisibility(View.GONE);
        root.addView(emptyBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        emptyBoxRef = emptyBox;

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
                boolean catExt = "extension".equals(n.kind);
                String catIndex = catExt ? "拓展" : (n.slidePage > 0 ? "P" + n.slidePage : path);
                ListRows.bind(CategoryActivity.this, row, catIndex, n.name, n.content, 2, n.importance,
                        n.formula != null && !n.formula.trim().isEmpty(),
                        n.figure != null && !n.figure.isEmpty(),
                        null, n.tags, null, catExt);
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
        refreshStats();
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

    private void refreshStats() {
        if (statText == null) return;
        int cats = db.categories(subjectId == null ? "" : subjectId).size();
        statText.setText(cats + " 个分类 · " + all.size() + " 条知识点");
    }

    private Db.Category findCat(List<Db.Category> cats, String id) {
        for (Db.Category c : cats) if (c.id.equals(id)) return c;
        return null;
    }

    private View chip(String label, final String id) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(12);
        t.setSingleLine(true);
        boolean active = (id == null && filterId == null) || (id != null && id.equals(filterId));
        t.setTextColor(active ? Ui.ACCENT : Ui.INK2);
        t.setGravity(Gravity.CENTER);
        t.setMinHeight(Ui.dp(this, 28));
        t.setPadding(Ui.dp(this, 11), 0, Ui.dp(this, 11), 0);
        android.graphics.drawable.GradientDrawable cg = new android.graphics.drawable.GradientDrawable();
        cg.setCornerRadius(Ui.dp(this, 999));
        if (active) {
            cg.setColor(Ui.ACCENT_SOFT);
            cg.setStroke(Ui.dp(this, 1), Ui.ACCENT2);
        } else {
            cg.setColor(Ui.CARD);
            cg.setStroke(Ui.dp(this, 1), Ui.LINE);
        }
        t.setBackground(cg);
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
        if (emptyBoxRef != null) emptyBoxRef.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        if (!shown.isEmpty()) return;
        if (emptyTitle != null) emptyTitle.setText("还没有知识点");
        if (emptyHint != null) emptyHint.setText("上传课件并完成识别后，这里会出现结构化的知识点卡片。");
    }
}
