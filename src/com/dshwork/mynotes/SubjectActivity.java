package com.dshwork.mynotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** 科目下的知识点：列表 + 导入课件。 */
public class SubjectActivity extends BaseActivity {

    private static final int REQ_PICK = 1001;
    private static final int REQ_SAVE = 1002;

    private byte[] pendingBytes;
    private String pendingName = "export";
    private String pendingMime = "text/plain";

    private Db db;
    private Prefs prefs;
    private String subjectId;
    private String subjectName;
    private TextView statsView;
    private List<Db.Note> data;
    private final List<Object> rows = new ArrayList<>();
    private final java.util.Set<String> collapsedDecks = new java.util.HashSet<>();
    private BaseAdapter adapter;
    private TextView empty;
    private ListView list;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final List<Uri> failedUris = new ArrayList<>();
    private final List<Integer> failedPages = new ArrayList<>();
    private String lastDeckId;
    private String lastOpenedNoteId;
    private String pendingAppendDeckId;
    private TextView retryBtn;
    private boolean selecting = false;
    private String viewMode = "decks";
    private String filterKind = "all";
    private final java.util.Set<String> expanded = new java.util.HashSet<>();
    private final java.util.Set<String> picked = new java.util.HashSet<>();
    private LinearLayout batchRow;
    private LinearLayout filterRow;
    private android.widget.TextView[] filterViewsRef;
    private String[] filterLabelsRef;
    private TextView selectCount;
    private volatile boolean importCancelled = false;

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
        LinearLayout titleLine = new LinearLayout(this);
        titleLine.setOrientation(LinearLayout.HORIZONTAL);
        titleLine.setGravity(Gravity.CENTER_VERTICAL);
        View titleDot = new View(this);
        android.graphics.drawable.GradientDrawable dg = new android.graphics.drawable.GradientDrawable();
        dg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        dg.setColor(Ui.ACCENT);
        titleDot.setBackground(dg);
        LinearLayout.LayoutParams tdlp = new LinearLayout.LayoutParams(Ui.dp(this, 9), Ui.dp(this, 9));
        tdlp.rightMargin = Ui.dp(this, 8);
        titleLine.addView(titleDot, tdlp);
        TextView title = Ui.text(this, subjectName == null ? "科目" : subjectName, 18, Ui.INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        titleLine.addView(title);
        titleBox.addView(titleLine);
        statsView = Ui.text(this, "", 11.5f, Ui.MUTED);
        statsView.setPadding(0, Ui.dp(this, 1), 0, 0);
        titleBox.addView(statsView);
        bar.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView add = Ui.button(this, "＋ 知识点");
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Db.Note n = db.createNote(subjectId);
                openNote(n.id);
            }
        });
        bar.addView(add);
        root.addView(bar);

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setPadding(Ui.dp(this, 16), 0, 0, Ui.dp(this, 8));

        TextView imp = Ui.chip(this, "导入课件", true);
        imp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImages();
            }
        });
        tools.addView(imp);

        retryBtn = Ui.chip(this, "重试失败", true);
        retryBtn.setVisibility(View.GONE);
        retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Uri> again = new ArrayList<>(failedUris);
                final List<Integer> againPages = new ArrayList<>(failedPages);
                failedUris.clear();
                failedPages.clear();
                retryBtn.setVisibility(View.GONE);
                runImport(again, againPages, lastDeckId);
            }
        });
        tools.addView(retryBtn);

        TextView exp = Ui.chip(this, "导出", false);
        exp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExportMenu();
            }
        });
        tools.addView(exp);

        TextView map = Ui.chip(this, "导图", false);
        map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SubjectActivity.this, MindMapActivity.class);
                i.putExtra("subjectId", subjectId);
                i.putExtra("subjectName", subjectName);
                startActivity(i);
            }
        });
        tools.addView(map);

        TextView cat = Ui.chip(this, "分类", false);
        cat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SubjectActivity.this, CategoryActivity.class);
                i.putExtra("subjectId", subjectId);
                i.putExtra("subjectName", subjectName);
                startActivity(i);
            }
        });
        tools.addView(cat);

        final LinearLayout tabHolder = new LinearLayout(this);
        tabHolder.setOrientation(LinearLayout.HORIZONTAL);
        tabHolder.setPadding(0, 0, Ui.dp(this, 8), 0);

        final LinearLayout segContainer = new LinearLayout(this);
        segContainer.setOrientation(LinearLayout.HORIZONTAL);

        final TextView[] tabs = new TextView[2];
        tabs[0] = Ui.text(this, "课件", 12.5f, Ui.INK);
        tabs[1] = Ui.text(this, "知识点", 12.5f, Ui.INK2);
        for (int ti = 0; ti < 2; ti++) {
            final int idx = ti;
            final TextView tabView = tabs[ti];
            tabView.setGravity(Gravity.CENTER);
            tabView.setPadding(Ui.dp(this, 14), Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 6));
            tabView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewMode = idx == 0 ? "decks" : "items";
                    for (int k = 0; k < 2; k++) {
                        boolean on = k == idx;
                        tabs[k].setTextColor(on ? Ui.INK : Ui.INK2);
                        if (on) {
                            android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
                            d.setColor(Ui.CARD);
                            d.setCornerRadius(Ui.dp(SubjectActivity.this, 7));
                            d.setStroke(Ui.dp(SubjectActivity.this, 1), Ui.LINE_STRONG);
                            tabs[k].setBackground(d);
                        } else {
                            tabs[k].setBackground(null);
                        }
                    }
                    buildRows();
                    adapter.notifyDataSetChanged();
                }
            });
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tlp.rightMargin = Ui.dp(this, 2);
            segContainer.addView(tabView, tlp);
        }
        android.graphics.drawable.GradientDrawable segBg = new android.graphics.drawable.GradientDrawable();
        segBg.setColor(0xFFEBE5D8);
        segBg.setCornerRadius(Ui.dp(this, 9));
        segContainer.setBackground(segBg);
        segContainer.setPadding(Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3));
        // 初始高亮「课件」
        android.graphics.drawable.GradientDrawable sel0 = new android.graphics.drawable.GradientDrawable();
        sel0.setColor(Ui.CARD);
        sel0.setCornerRadius(Ui.dp(this, 7));
        sel0.setStroke(Ui.dp(this, 1), Ui.LINE_STRONG);
        tabs[0].setBackground(sel0);

        tabHolder.addView(segContainer);
        tools.addView(tabHolder);

        TextView tidy = Ui.chip(this, "整理", false);
        tidy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmOrganize();
            }
        });
        tools.addView(tidy);

        TextView rename = Ui.chip(this, "标题", false);
        rename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmTitles();
            }
        });
        tools.addView(rename);

        TextView foldAll = Ui.chip(this, "折叠", false);
        foldAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (collapsedDecks.isEmpty()) {
                    for (Db.Deck d : db.decks(subjectId)) collapsedDecks.add(d.id);
                } else {
                    collapsedDecks.clear();
                }
                buildRows();
                adapter.notifyDataSetChanged();
            }
        });
        tools.addView(foldAll);

        TextView extTitles = Ui.chip(this, "拓展题名", false);
        extTitles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmExtTitles();
            }
        });
        tools.addView(extTitles);

        TextView rule = Ui.chip(this, "标准", false);
        rule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editCriteria();
            }
        });
        tools.addView(rule);

        android.widget.HorizontalScrollView toolScroll = new android.widget.HorizontalScrollView(this);
        toolScroll.setHorizontalScrollBarEnabled(false);
        toolScroll.addView(tools);
        root.addView(toolScroll);

        filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 8));
        final String[] filterKeys = {"all", "point", "ext"};
        final String[] filterLabels = {"全部", "知识点", "拓展"};
        final TextView[] filterViews = new TextView[filterKeys.length];
        for (int fi = 0; fi < filterKeys.length; fi++) {
            final int idx = fi;
            final TextView fv = Ui.chip(this, filterLabels[fi], fi == 0);
            filterViews[fi] = fv;
            fv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    filterKind = filterKeys[idx];
                    for (int k = 0; k < filterViews.length; k++) {
                        boolean on = k == idx;
                        filterViews[k].setBackground(Ui.chip(SubjectActivity.this, "x", on).getBackground());
                        filterViews[k].setTextColor(on ? 0xFFFFFFFF : Ui.INK);
                    }
                    buildRows();
                    adapter.notifyDataSetChanged();
                    if (list != null) list.setSelection(0);
                    updateFilterLabels(filterViews, filterLabels);
                }
            });
            LinearLayout.LayoutParams flp2 = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            flp2.rightMargin = Ui.dp(this, 6);
            filterRow.addView(fv, flp2);
        }
        final android.widget.Spinner inlineSort = new android.widget.Spinner(this);
        String[] sortLabels = {"最近创建", "按标题", "按重要度"};
        final String[] sortKeys = {"recent", "title", "importance"};
        android.widget.ArrayAdapter<String> sortAdapter = Ui.spinnerAdapter(this, sortLabels);
        inlineSort.setAdapter(sortAdapter);
        int current = 0;
        for (int k = 0; k < sortKeys.length; k++) {
            if (sortKeys[k].equals(prefs.sort())) current = k;
        }
        inlineSort.setSelection(current);
        inlineSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= sortKeys.length) return;
                if (sortKeys[position].equals(prefs.sort())) return;
                prefs.setSort(sortKeys[position]);
                if (adapter != null && list != null) reload();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        LinearLayout.LayoutParams sortLp = new LinearLayout.LayoutParams(
                Ui.dp(this, 120), ViewGroup.LayoutParams.WRAP_CONTENT);
        sortLp.leftMargin = Ui.dp(this, 8);
        filterRow.addView(inlineSort, sortLp);

        final TextView bulkChip = Ui.chip(this, "批量", false);
        bulkChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selecting) exitSelect();
                else enterSelect();
            }
        });
        LinearLayout.LayoutParams blp3 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp3.leftMargin = Ui.dp(this, 8);
        filterRow.addView(bulkChip, blp3);

        root.addView(filterRow);

        batchRow = new LinearLayout(this);
        batchRow.setOrientation(LinearLayout.HORIZONTAL);
        batchRow.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 8));
        batchRow.setVisibility(View.GONE);

        selectCount = Ui.text(this, "已选 0", 13, Ui.INK2);
        selectCount.setPadding(Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
        batchRow.addView(selectCount);

        TextView pickAll = batchChip("全选");
        pickAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                picked.clear();
                for (Db.Note n : data) picked.add(n.id);
                refreshBatch();
            }
        });
        batchRow.addView(pickAll);

        TextView delPicked = batchChip("删除");
        delPicked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (picked.isEmpty()) return;
                new AlertDialog.Builder(SubjectActivity.this)
                        .setTitle("删除选中")
                        .setMessage("将删除 " + picked.size() + " 条知识点，不可恢复。")
                        .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                db.bulkDelete(new ArrayList<>(picked));
                                picked.clear();
                                exitSelect();
                                keepScroll(new Runnable() {
                                    @Override
                                    public void run() {
                                        reload();
                                    }
                                });
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
        batchRow.addView(delPicked);

        TextView mmSelected = batchChip("生成导图");
        mmSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (picked.isEmpty()) {
                    Toast.makeText(SubjectActivity.this, "先选中知识点", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent mi = new Intent(SubjectActivity.this, MindMapActivity.class);
                mi.putExtra("subjectId", subjectId);
                mi.putExtra("subjectName", subjectName);
                mi.putStringArrayListExtra("noteIds", new ArrayList<>(picked));
                exitSelect();
                startActivity(mi);
            }
        });
        batchRow.addView(mmSelected);

        TextView movePicked = batchChip("改分类");
        movePicked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (picked.isEmpty()) return;
                askCategoryForPicked();
            }
        });
        batchRow.addView(movePicked);

        TextView cancel = batchChip("取消");
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitSelect();
            }
        });
        batchRow.addView(cancel);

        root.addView(batchRow);

        list = new ListView(this);
        list.setDivider(null);
        list.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 16));
        list.setClipToPadding(false);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        empty = Ui.text(this, "还没有知识点。可以先「导入课件截图」，或手动新建。", 14, Ui.INK2);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(Ui.dp(this, 24), 0, Ui.dp(this, 24), 0);
        root.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return rows.size();
            }

            @Override
            public Object getItem(int position) {
                return rows.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Object o = rows.get(position);
                if (o instanceof HintRow) {
                    TextView tip = Ui.text(SubjectActivity.this, ((HintRow) o).text, 11.5f, Ui.MUTED);
                    tip.setPadding(Ui.dp(SubjectActivity.this, 4), Ui.dp(SubjectActivity.this, 6),
                            Ui.dp(SubjectActivity.this, 4), Ui.dp(SubjectActivity.this, 10));
                    return tip;
                }
                if (o instanceof Db.Deck) {
                    return deckHeader((Db.Deck) o);
                }
                ListRows.NoteRow row = ListRows.obtain(SubjectActivity.this, convertView);
                Db.Note n = (Db.Note) o;
                String path = n.categoryId == null ? "" : db.categoryPath(n.categoryId);
                if ("extension".equals(n.kind)) path = path.isEmpty() ? "拓展" : path + " · 拓展";
                boolean isExpanded = expanded.contains(n.id);
                boolean isExt = "extension".equals(n.kind);
                String indexLabel = isExt ? "拓展"
                        : (n.slidePage > 0 ? "P" + n.slidePage : (path.isEmpty() ? "" : path));
                String srcLabel;
                if (n.slideId == null || n.slideId.isEmpty()) {
                    srcLabel = "手动笔记";
                } else {
                    String deckTitle = db.deckTitleBySlide(n.slideId);
                    srcLabel = (deckTitle == null || deckTitle.isEmpty() ? "" : deckTitle + " · ")
                            + "第 " + n.slidePage + " 页";
                }
                ListRows.bind(SubjectActivity.this, row, indexLabel, n.name, n.content,
                        isExpanded ? -1 : prefs.cardLines(), n.importance,
                        n.formula != null && !n.formula.trim().isEmpty(),
                        n.figure != null && !n.figure.isEmpty(),
                        prefs.showQuote() ? n.quote : null,
                        n.tags, srcLabel, isExt, selecting, selecting && picked.contains(n.id));
                boolean longContent = prefs.cardLines() > 0 && n.content != null
                        && n.content.length() > prefs.cardLines() * 24;
                if (row.more != null) {
                    row.more.setVisibility(longContent ? View.VISIBLE : View.GONE);
                    if (longContent) {
                        row.more.setText(isExpanded ? "收起" : "展开全文");
                        final String noteId = n.id;
                        row.more.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (expanded.contains(noteId)) expanded.remove(noteId);
                                else expanded.add(noteId);
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
                if (selecting && picked.contains(n.id)) {
                    row.card.setBackground(Ui.cardSelected(SubjectActivity.this));
                }
                return row.card;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object clicked = rows.get(position);
                if (clicked instanceof HintRow) return;
                if (clicked instanceof Db.Deck) {
                    Db.Deck d = (Db.Deck) clicked;
                    if (collapsedDecks.contains(d.id)) collapsedDecks.remove(d.id);
                    else collapsedDecks.add(d.id);
                    buildRows();
                    adapter.notifyDataSetChanged();
                    return;
                }
                Db.Note n = (Db.Note) clicked;
                if (selecting) {
                    if (picked.contains(n.id)) picked.remove(n.id);
                    else picked.add(n.id);
                    refreshBatch();
                    adapter.notifyDataSetChanged();
                    return;
                }
                openNote(n.id);
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Object raw = rows.get(position);
                if (raw instanceof Db.Deck) {
                    deckMenu((Db.Deck) raw);
                    return true;
                }
                final Db.Note n = (Db.Note) raw;
                new AlertDialog.Builder(SubjectActivity.this)
                        .setTitle("删除知识点")
                        .setMessage(n.name)
                        .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                db.deleteNote(n.id);
                                reload();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            }
        });

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
        scrollToLastOpened();
    }

    /** 刷新列表但保持滚动位置 */
    private void keepScroll(Runnable reloadTask) {
        int first = 0;
        int top = 0;
        try {
            first = list.getFirstVisiblePosition();
            View v0 = list.getChildAt(0);
            top = v0 == null ? 0 : v0.getTop();
        } catch (Exception ignored) {
        }
        reloadTask.run();
        final int f = first;
        final int t = top;
        list.post(new Runnable() {
            @Override
            public void run() {
                list.setSelectionFromTop(f, t);
            }
        });
    }

    private void reload() {
        if (db == null || prefs == null || subjectId == null) return;
        try {
            data = db.notesSorted(subjectId, prefs.sort());
        } catch (Exception e) {
            data = new ArrayList<>();
        }
        if (data == null) data = new ArrayList<>();
        if (statsView != null) {
            int[] st = db.subjectStats(subjectId);
            statsView.setText(st[0] + " 个课件 · " + st[1] + " 页 · " + st[2] + " 个知识点");
        }
        buildRows();
        if (adapter != null) adapter.notifyDataSetChanged();
        if (filterViewsRef != null && filterLabelsRef != null) {
            try {
                updateFilterLabels(filterViewsRef, filterLabelsRef);
            } catch (Exception ignored) {
            }
        }
        boolean isEmpty = data.isEmpty();
        if (empty != null) empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (list != null) list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    /** 把「课件 → 其下知识点」摊平成行；没有课件的归到「未分组」 */
    /** 给筛选胶囊带上数量：全部 / 知识点 / 拓展 */
    private void updateFilterLabels(android.widget.TextView[] views, String[] labels) {
        int points = 0, exts = 0;
        for (Db.Note n : data) {
            if ("extension".equals(n.kind)) exts++;
            else points++;
        }
        int[] counts = {points + exts, points, exts};
        for (int k = 0; k < views.length && k < labels.length; k++) {
            views[k].setText(labels[k] + " " + counts[k]);
        }
    }

    private void buildRows() {
        rows.clear();
        java.util.List<Db.Note> visible = new ArrayList<>();
        for (Db.Note n : data) {
            boolean ext = "extension".equals(n.kind);
            if ("point".equals(filterKind) && ext) continue;
            if ("ext".equals(filterKind) && !ext) continue;
            visible.add(n);
        }
        java.util.List<Db.Deck> decks = db.decks(subjectId);
        java.util.Map<String, java.util.List<Db.Note>> byDeck = new java.util.HashMap<>();
        java.util.List<Db.Note> loose = new ArrayList<>();
        java.util.Set<String> slideToDeck = new java.util.HashMap<String, String>().keySet();
        java.util.Map<String, String> slideDeck = new java.util.HashMap<>();
        for (Db.Deck d : decks) {
            for (Db.Slide s : db.slides(d.id)) slideDeck.put(s.id, d.id);
        }
        for (Db.Note n : visible) {
            String deckId = n.slideId == null ? null : slideDeck.get(n.slideId);
            if (deckId == null) {
                loose.add(n);
            } else {
                if (!byDeck.containsKey(deckId)) byDeck.put(deckId, new ArrayList<Db.Note>());
                byDeck.get(deckId).add(n);
            }
        }
        if ("items".equals(viewMode)) {
            for (Db.Note n : visible) rows.add(n);
            return;
        }
        if (!visible.isEmpty() && (decks == null || decks.isEmpty())) {
            // 全是手动笔记：在顶部给一句说明，避免以为分组坏了
            HintRow hint = new HintRow();
            hint.text = "这些是手动新建的笔记。导入课件后，这里会按课件分组显示。";
            rows.add(hint);
        }
        for (Db.Deck d : decks) {
            java.util.List<Db.Note> list = byDeck.get(d.id);
            if (list == null || list.isEmpty()) continue;
            rows.add(d);
            if (!collapsedDecks.contains(d.id)) rows.addAll(list);
        }
        for (Db.Note n : loose) rows.add(n);
    }

    static class HintRow {
        String text;
    }

    private View deckHeader(final Db.Deck d) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 14));
        android.graphics.drawable.GradientDrawable dbg = new android.graphics.drawable.GradientDrawable();
        dbg.setColor(Ui.CARD);
        dbg.setCornerRadius(Ui.dp(this, 16));
        dbg.setStroke(Ui.dp(this, 1), Ui.LINE);
        box.setBackground(dbg);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.bottomMargin = Ui.dp(this, 10);
        box.setLayoutParams(dlp);
        try {
            box.setElevation(Ui.dp(this, 1));
        } catch (Exception ignored) {
        }

        boolean open = !collapsedDecks.contains(d.id);
        TextView arrow = Ui.text(this, open ? "▾" : "▸", 14, Ui.ACCENT);
        arrow.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 6), 0);
        box.addView(arrow);

        TextView title = Ui.text(this, d.title == null ? "课件" : d.title, 13.5f, Ui.INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView meta = Ui.text(this, d.slideCount + " 页 · " + d.noteCount + " 条", 11.5f, Ui.INK2);
        box.addView(meta);
        return box;
    }

    private void deckMenu(final Db.Deck d) {
        final String[] items = {"本节总结（AI）", "重做整册（AI）", "追加图片到这册", "重命名课件", "查看页面", "删除课件"};
        new AlertDialog.Builder(this)
                .setTitle(d.title)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            deckSummary(d);
                        } else if (which == 1) {
                            redoDeckMenu(d);
                        } else if (which == 2) {
                            appendToDeck(d);
                        } else if (which == 3) {
                            final EditText input = new EditText(SubjectActivity.this);
                            Ui.styleInput(input);
                            input.setText(d.title);
                            new AlertDialog.Builder(SubjectActivity.this)
                                    .setTitle("重命名课件")
                                    .setView(input)
                                    .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dlg, int w) {
                                            db.renameDeck(d.id, input.getText().toString().trim());
                                            reload();
                                        }
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        } else if (which == 4) {
                            showSlides(d);
                        } else {
                            new AlertDialog.Builder(SubjectActivity.this)
                                    .setTitle("删除课件")
                                    .setMessage("会删除该课件下的所有页面与知识点，不可恢复。")
                                    .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dlg, int w) {
                                            db.deleteDeck(d.id);
                                            reload();
                                        }
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        }
                    }
                })
                .show();
    }

    private void showSlides(Db.Deck d) {
        java.util.List<Db.Slide> slides = db.slides(d.id);
        String[] labels = new String[slides.size()];
        for (int i = 0; i < slides.size(); i++) {
            Db.Slide s = slides.get(i);
            labels[i] = (s.title == null || s.title.isEmpty() ? "第 " + s.idx + " 页" : s.title) + " · " + s.noteCount + " 条"
                    + (s.digest != null && !s.digest.isEmpty() ? " · 有汇总" : "");
        }
        new AlertDialog.Builder(this)
                .setTitle(d.title + "（" + slides.size() + " 页）")
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        slideMenu(slides.get(which));
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    private void deckSummary(final Db.Deck d) {
        if (!prefs.ready()) {
            Toast.makeText(this, "先到设置里填 API Key", Toast.LENGTH_SHORT).show();
            return;
        }
        final android.app.ProgressDialog dlg = new android.app.ProgressDialog(this);
        dlg.setMessage("正在总结整份课件…");
        dlg.setCancelable(false);
        dlg.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    msg = Importer.summarizeDeck(db, prefs, subjectId, d);
                } catch (final Exception e) {
                    msg = "失败：" + AiClient.friendly(e);
                }
                final String finalMsg = msg;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        dlg.dismiss();
                        new AlertDialog.Builder(SubjectActivity.this)
                                .setTitle(d.title + " · 本节总结")
                                .setMessage(finalMsg)
                                .setPositiveButton("好", null)
                                .show();
                    }
                });
            }
        }).start();
    }

    /** 整册重做：逐页重新识别 */
    /** 把新图片追加到已有课件（网页版「追加图片到这份课件」） */
    private void appendToDeck(Db.Deck d) {
        if (!prefs.ready()) {
            Toast.makeText(this, "先到设置里填 API Key", Toast.LENGTH_SHORT).show();
            return;
        }
        pendingAppendDeckId = d.id;
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, REQ_PICK);
    }

    private void redoDeckMenu(final Db.Deck d) {
        if (!prefs.ready()) {
            Toast.makeText(this, "先到设置里填 API Key", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("重做整册")
                .setMessage("会把《" + d.title + "》里每一页重新识别一遍，覆盖该页现有知识点。\n共 "
                        + d.slideCount + " 页，调用次数与页数相同。")
                .setPositiveButton("开始", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        runRedoDeck(d);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void runRedoDeck(final Db.Deck d) {
        final android.app.ProgressDialog dlg = new android.app.ProgressDialog(this);
        dlg.setTitle("正在重做整册");
        dlg.setMessage("准备中…");
        dlg.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        dlg.setMax(Math.max(1, d.slideCount));
        dlg.setCancelable(false);
        dlg.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    int[] r = Importer.redoDeck(SubjectActivity.this, db, prefs, subjectId, d,
                            new Importer.Progress() {
                                @Override
                                public void onStep(final int done, final int total, final String message) {
                                    ui.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            dlg.setMax(total);
                                            dlg.setProgress(done);
                                            dlg.setMessage(message);
                                        }
                                    });
                                }

                                @Override
                                public boolean cancelled() {
                                    return false;
                                }
                            });
                    msg = "整册重做完成：成功 " + r[0] + " 页" + (r[1] > 0 ? "，失败 " + r[1] + " 页" : "");
                } catch (final Exception e) {
                    msg = "失败：" + AiClient.friendly(e);
                }
                final String finalMsg = msg;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        dlg.dismiss();
                        Toast.makeText(SubjectActivity.this, finalMsg, Toast.LENGTH_LONG).show();
                        reload();
                    }
                });
            }
        }).start();
    }

    private void slideMenu(final Db.Slide s) {
        final String[] ops = {"打开该页知识点", "重命名这一页", "让 AI 重做这一页", "生成该页汇总", "删除这一页"};
        new AlertDialog.Builder(this)
                .setTitle("第 " + s.idx + " 页 · " + s.noteCount + " 条")
                .setItems(ops, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            java.util.List<Db.Note> list = db.notesBySlide(s.id);
                            if (list.isEmpty()) {
                                Toast.makeText(SubjectActivity.this, "这一页没有知识点", Toast.LENGTH_SHORT).show();
                            } else {
                                openNote(list.get(0).id);
                            }
                        } else if (which == 1) {
                            final EditText renameInput = new EditText(SubjectActivity.this);
                            Ui.styleInput(renameInput);
                            renameInput.setText(s.title == null ? "" : s.title);
                            renameInput.setHint("这一页的标题");
                            new AlertDialog.Builder(SubjectActivity.this)
                                    .setTitle("重命名第 " + s.idx + " 页")
                                    .setView(renameInput)
                                    .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dlg, int w) {
                                            db.renameSlide(s.id, renameInput.getText().toString().trim());
                                        }
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        } else if (which == 2) {
                            redoSlide(s);
                        } else if (which == 3) {
                            makeDigest(s);
                        } else {
                            new AlertDialog.Builder(SubjectActivity.this)
                                    .setTitle("删除这一页")
                                    .setMessage("会删除该页的知识点，不可恢复。")
                                    .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dlg, int w2) {
                                            db.deleteSlide(s.id);
                                            reload();
                                        }
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        }
                    }
                })
                .show();
    }

    private void redoSlide(final Db.Slide s) {
        if (!prefs.ready()) {
            Toast.makeText(this, "先到设置里填 API Key", Toast.LENGTH_SHORT).show();
            return;
        }
        final android.app.ProgressDialog dlg = new android.app.ProgressDialog(this);
        dlg.setMessage("AI 正在重做第 " + s.idx + " 页…");
        dlg.setCancelable(false);
        dlg.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    String json = Importer.extractFile(prefs, s.imagePath);
                    int added = Importer.replaceSlide(SubjectActivity.this, db, prefs, subjectId, s, json);
                    msg = "第 " + s.idx + " 页已重做，现 " + added + " 条";
                } catch (final Exception e) {
                    msg = "失败：" + AiClient.friendly(e);
                }
                final String finalMsg = msg;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        dlg.dismiss();
                        Toast.makeText(SubjectActivity.this, finalMsg, Toast.LENGTH_LONG).show();
                        reload();
                    }
                });
            }
        }).start();
    }

    private void makeDigest(final Db.Slide s) {
        if (!prefs.ready()) {
            Toast.makeText(this, "先到设置里填 API Key", Toast.LENGTH_SHORT).show();
            return;
        }
        final android.app.ProgressDialog dlg = new android.app.ProgressDialog(this);
        dlg.setMessage("正在生成第 " + s.idx + " 页汇总…");
        dlg.setCancelable(false);
        dlg.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    msg = Importer.digestSlide(db, prefs, subjectId, s);
                    if (msg.length() > 120) msg = msg.substring(0, 120) + "…";
                } catch (final Exception e) {
                    msg = "失败：" + AiClient.friendly(e);
                }
                final String finalMsg = msg;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        dlg.dismiss();
                        new AlertDialog.Builder(SubjectActivity.this)
                                .setTitle("第 " + s.idx + " 页汇总")
                                .setMessage(finalMsg)
                                .setPositiveButton("好", null)
                                .show();
                    }
                });
            }
        }).start();
    }

    private TextView batchChip(String label) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(13);
        t.setTextColor(Ui.INK);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        t.setBackground(Ui.card(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(this, 8);
        t.setLayoutParams(lp);
        return t;
    }

    private void enterSelect() {
        selecting = true;
        picked.clear();
        batchRow.setVisibility(View.VISIBLE);
        refreshBatch();
        adapter.notifyDataSetChanged();
    }

    private void exitSelect() {
        selecting = false;
        picked.clear();
        batchRow.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void refreshBatch() {
        if (selectCount != null) selectCount.setText("已选 " + picked.size());
    }

    private void askCategoryForPicked() {
        final EditText input = new EditText(this);
        Ui.styleInput(input);
        input.setHint("分类路径，例如 力学/运动学");
        new AlertDialog.Builder(this)
                .setTitle("把 " + picked.size() + " 条移到…")
                .setView(input)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String path = input.getText().toString().trim();
                        String catId = path.isEmpty() ? null : db.ensureCategoryPath(subjectId, path);
                        db.bulkSetCategory(new ArrayList<>(picked), catId);
                        picked.clear();
                        exitSelect();
                        reload();
                        Toast.makeText(SubjectActivity.this, "已更新分类", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void openNote(String noteId) {
        lastOpenedNoteId = noteId;
        Intent i = new Intent(this, NoteActivity.class);
        i.putExtra("noteId", noteId);
        startActivity(i);
    }

    /** 从详情返回后，把列表滚到刚才那条 */
    private void scrollToLastOpened() {
        if (lastOpenedNoteId == null || list == null) return;
        final String target = lastOpenedNoteId;
        lastOpenedNoteId = null;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i) instanceof Db.Note) {
                Db.Note n = (Db.Note) rows.get(i);
                if (target.equals(n.id)) {
                    final int index = i;
                    list.post(new Runnable() {
                        @Override
                        public void run() {
                            list.setSelection(Math.max(0, index - 1));
                        }
                    });
                    return;
                }
            }
        }
    }

    private void openExportMenu() {
        final String[] titles = {"Markdown 笔记", "HTML 笔记", "公式合集", "JSON（仅文字）", "完整备份（zip）"};
        final String[] hints = {
                "纯文本公式写成 $$…$$，适合粘到别的笔记软件",
                "公式与配图内嵌，双击就能在浏览器里看",
                "只挑带公式的条目，A4 打印样式",
                "结构化文本，方便脚本处理",
                "含课件、设置与用量，换机可整包恢复"
        };
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 8));
        for (int i = 0; i < titles.length; i++) {
            final int kind = i;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(Ui.dp(this, 14), Ui.dp(this, 11), Ui.dp(this, 14), Ui.dp(this, 11));
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(Ui.CARD);
            bg.setCornerRadius(Ui.dp(this, 10));
            row.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = Ui.dp(this, 6);
            row.setLayoutParams(lp);

            TextView title = Ui.text(this, titles[i], 14, Ui.INK);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            row.addView(title);
            TextView hint = Ui.text(this, hints[i], 11.5f, Ui.MUTED);
            hint.setPadding(0, Ui.dp(this, 2), 0, 0);
            row.addView(hint);

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startExport(kind);
                }
            });
            box.addView(row);
        }
        android.widget.ScrollView sc = new android.widget.ScrollView(this);
        sc.addView(box);
        new AlertDialog.Builder(this)
                .setTitle("导出")
                .setView(sc)
                .setNegativeButton("取消", null)
                .show();
    }

    private String safeName() {
        String base = subjectName == null ? "notes" : subjectName.replaceAll("[\\\\/:*?\"<>|]", "_");
        return base.isEmpty() ? "notes" : base;
    }

    private void startExport(final int kind) {
        final android.app.ProgressDialog dlg = new android.app.ProgressDialog(this);
        dlg.setMessage("正在生成…");
        dlg.setCancelable(false);
        dlg.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String err = null;
                java.io.File savedZip = null;
                try {
                    if (kind == 0) {
                        pendingBytes = Exporter.markdown(db, subjectId).getBytes("UTF-8");
                        pendingName = safeName() + "-笔记.md";
                        pendingMime = "text/markdown";
                    } else if (kind == 1) {
                        pendingBytes = Exporter.html(SubjectActivity.this, db, subjectId).getBytes("UTF-8");
                        pendingName = safeName() + "-笔记.html";
                        pendingMime = "text/html";
                    } else if (kind == 2) {
                        pendingBytes = Exporter.formulas(SubjectActivity.this, db, subjectId).getBytes("UTF-8");
                        pendingName = safeName() + "-公式合集.html";
                        pendingMime = "text/html";
                    } else if (kind == 4) {
                        pendingBytes = Exporter.json(db, subjectId).getBytes("UTF-8");
                        pendingName = safeName() + "-笔记.json";
                        pendingMime = "application/json";
                    } else {
                        savedZip = Backups.stream(SubjectActivity.this,
                                safeName() + "-备份-" + Backups.stamp() + ".zip",
                                new Backups.Sink() {
                                    @Override
                                    public void write(java.io.OutputStream os) throws Exception {
                                        Exporter.backupTo(os, db, SubjectActivity.this, subjectId);
                                    }
                                });
                    }
                } catch (Exception e) {
                    err = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                }
                final String finalErr = err;
                final java.io.File zip = savedZip;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        dlg.dismiss();
                        if (finalErr != null) {
                            Toast.makeText(SubjectActivity.this, "生成失败：" + finalErr, Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (zip != null) {
                            new AlertDialog.Builder(SubjectActivity.this)
                                    .setTitle("备份已保存")
                                    .setMessage("文件：\n" + zip.getName() + "\n\n位置：\n" + Backups.dirHint(SubjectActivity.this)
                                            + "\n\n恢复时在这里直接选，不用去翻文件管理器。")
                                    .setPositiveButton("好", null)
                                    .show();
                            return;
                        }
                        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType(pendingMime);
                        i.putExtra(Intent.EXTRA_TITLE, pendingName);
                        startActivityForResult(i, REQ_SAVE);
                    }
                });
            }
        }).start();
    }

    private void editCriteria() {
        final EditText input = new EditText(this);
        Ui.styleInput(input);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(4);
        input.setText(db.subjectCriteria(subjectId) == null ? "" : db.subjectCriteria(subjectId));
        input.setHint("例如：按「电路/模电/数电」三大块分；公式题单独归到「计算」；不要出现「其他」");
        new AlertDialog.Builder(this)
                .setTitle("本学科的整理标准")
                .setMessage("会自动拼进「整理」的提示词里，让分类更贴合你的习惯。")
                .setView(input)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.setSubjectCriteria(subjectId, input.getText().toString());
                        Toast.makeText(SubjectActivity.this, "已保存", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmOrganize() {
        if (!prefs.ready()) {
            Toast.makeText(this, "先到设置里填 API Key", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("自动整理")
                .setMessage("让模型重新给所有知识点分类（会调用一次文本模型，按条计费）。")
                .setPositiveButton("开始", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        runOrganize();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmTitles() {
        if (!prefs.ready()) {
            Toast.makeText(this, "先到设置里填 API Key", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("改写标题")
                .setMessage("把「拓展 / 考点 / 易错点」这类空泛标题改写成能看出内容的概括标题（一次最多 15 条）。")
                .setPositiveButton("开始", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        runTitles();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmExtTitles() {
        if (!prefs.ready()) {
            Toast.makeText(this, "先到设置里填 API Key", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("整理拓展标题")
                .setMessage("只把「拓展」类型里空泛的标题改写成能看出内容的样子。")
                .setPositiveButton("开始", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        runTitles(true);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void runTitles() {
        runTitles(false);
    }

    private void runTitles(final boolean onlyExt) {
        final ProgressDialog dlg = new ProgressDialog(this);
        dlg.setTitle("正在生成标题");
        dlg.setMessage("准备中…");
        dlg.setCancelable(false);
        dlg.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Titles.Result r = Titles.run(db, prefs, subjectId, new Titles.Progress() {
                    @Override
                    public void onStep(final String message) {
                        ui.post(new Runnable() {
                            @Override
                            public void run() {
                                dlg.setMessage(message);
                            }
                        });
                    }
                }, onlyExt);
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        dlg.dismiss();
                        reload();
                        if (r.error != null) {
                            Toast.makeText(SubjectActivity.this, r.error, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SubjectActivity.this, "已改写 " + r.updated + " 条标题", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void runOrganize() {
        final ProgressDialog dlg = new ProgressDialog(this);
        dlg.setTitle("正在整理");
        dlg.setMessage("准备中…");
        dlg.setCancelable(false);
        dlg.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Organizer.Result r = Organizer.run(db, prefs, subjectId, new Organizer.Progress() {
                    @Override
                    public void onStep(final String message) {
                        ui.post(new Runnable() {
                            @Override
                            public void run() {
                                dlg.setMessage(message);
                            }
                        });
                    }
                });
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        dlg.dismiss();
                        reload();
                        if (r.error != null) {
                            Toast.makeText(SubjectActivity.this, "整理失败：" + r.error, Toast.LENGTH_LONG).show();
                        } else {
                            String extra = r.merged > 0 ? "，合并 " + r.merged + " 条重复" : "";
                            Toast.makeText(SubjectActivity.this, "已更新 " + r.updated + " 条分类" + extra, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void pickImages() {
        if (!prefs.ready()) {
            new AlertDialog.Builder(this)
                    .setTitle("先填 API Key")
                    .setMessage("导入课件需要调用模型，请先去设置里填 Key。")
                    .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(SubjectActivity.this, SettingsActivity.class));
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, REQ_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_SAVE) {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
            if (pendingBytes == null) {
                Toast.makeText(this, "没有可写出的内容", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                java.io.OutputStream os = getContentResolver().openOutputStream(data.getData());
                os.write(pendingBytes);
                os.flush();
                os.close();
                String name = pendingName == null ? "文件" : pendingName;
                Toast.makeText(this, "已保存：" + name + "（" + pendingBytes.length / 1024 + " KB）",
                        Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "写入失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            } finally {
                pendingBytes = null;
            }
            return;
        }

        if (requestCode != REQ_PICK || resultCode != RESULT_OK || data == null) return;
        final List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                uris.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        if (uris.isEmpty()) return;
        if (pendingAppendDeckId != null) {
            String deckId = pendingAppendDeckId;
            pendingAppendDeckId = null;
            java.util.List<Db.Slide> existing = db.slides(deckId);
            int base = 0;
            for (Db.Slide sl : existing) {
                if (sl.idx > base) base = sl.idx;
            }
            List<Integer> pageNos = new ArrayList<>();
            for (int k = 0; k < uris.size(); k++) pageNos.add(base + k + 1);
            runImport(uris, pageNos, deckId);
            return;
        }
        runImport(uris);
    }

    private void runImport(final List<Uri> uris) {
        runImport(uris, null, null);
    }

    private void runImport(final List<Uri> uris, final List<Integer> pageNos, final String deckId) {
        final ProgressDialog dlg = new ProgressDialog(this);
        dlg.setTitle("正在解析课件");
        dlg.setMessage("准备中…");
        dlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dlg.setMax(uris.size());
        dlg.setProgress(0);
        dlg.setCancelable(true);
        importCancelled = false;
        dlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                importCancelled = true;
            }
        });
        dlg.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Importer.Outcome out = Importer.run(SubjectActivity.this, db, prefs, subjectId, uris, pageNos, deckId,
                        new Importer.Progress() {
                            @Override
                            public void onStep(final int done, final int total, final String message) {
                                ui.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        dlg.setMax(total);
                                        dlg.setProgress(done);
                                        dlg.setMessage(message);
                                    }
                                });
                            }

                            @Override
                            public boolean cancelled() {
                                return importCancelled;
                            }
                        });
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        dlg.dismiss();
                        keepScroll(new Runnable() {
                            @Override
                            public void run() {
                                reload();
                            }
                        });
                        StringBuilder sb = new StringBuilder();
                        sb.append("识别 ").append(out.pages).append(" 页，新增 ").append(out.points).append(" 个知识点");
                        if (out.skipped > 0) {
                            sb.append("，跳过 ").append(out.skipped).append(" 页重复");
                        }
                        if (!out.errors.isEmpty()) {
                            sb.append("\n").append(out.errors.get(0));
                            if (out.errors.size() > 1) sb.append("（共 ").append(out.errors.size()).append(" 个错误）");
                        }
                        failedUris.clear();
                        failedPages.clear();
                        failedUris.addAll(out.failed);
                        failedPages.addAll(out.failedPages);
                        if (out.deckId != null) lastDeckId = out.deckId;
                        retryBtn.setVisibility(failedUris.isEmpty() ? View.GONE : View.VISIBLE);
                        if (!failedUris.isEmpty()) {
                            retryBtn.setText("  重试失败（" + failedUris.size() + "）  ");
                        }
                        Toast.makeText(SubjectActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
                        if (prefs.autoOrganize() && out.points > 0 && prefs.ready()) {
                            runOrganize();
                        }
                    }
                });
            }
        }).start();
    }
}
