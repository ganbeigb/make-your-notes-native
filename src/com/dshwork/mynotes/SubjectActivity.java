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
    private List<Db.Note> data;
    private BaseAdapter adapter;
    private TextView empty;
    private ListView list;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final List<Uri> failedUris = new ArrayList<>();
    private TextView retryBtn;
    private boolean selecting = false;
    private final java.util.Set<String> picked = new java.util.HashSet<>();
    private LinearLayout batchRow;
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
        bar.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 8));

        TextView back = Ui.text(this, "‹", 26, Ui.ACCENT);
        back.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 10), 0);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bar.addView(back);

        TextView title = Ui.text(this, subjectName == null ? "科目" : subjectName, 19, Ui.INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

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
                failedUris.clear();
                retryBtn.setVisibility(View.GONE);
                runImport(again);
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

        TextView bulk = Ui.chip(this, "批量", false);
        bulk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selecting) exitSelect();
                else enterSelect();
            }
        });
        tools.addView(bulk);

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
                                reload();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
        batchRow.addView(delPicked);

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
        list.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 12));
        list.setClipToPadding(false);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        empty = Ui.text(this, "还没有知识点。可以先「导入课件截图」，或手动新建。", 14, Ui.INK2);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(Ui.dp(this, 24), 0, Ui.dp(this, 24), 0);
        root.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return data == null ? 0 : data.size();
            }

            @Override
            public Object getItem(int position) {
                return data.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ListRows.NoteRow row = ListRows.obtain(SubjectActivity.this, convertView);
                Db.Note n = data.get(position);
                String path = n.categoryId == null ? "" : db.categoryPath(n.categoryId);
                ListRows.bind(SubjectActivity.this, row, path, n.name, n.content, prefs.cardLines());
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
                Db.Note n = data.get(position);
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
                final Db.Note n = data.get(position);
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
    }

    private void reload() {
        data = db.notesSorted(subjectId, prefs.sort());
        adapter.notifyDataSetChanged();
        boolean isEmpty = data.isEmpty();
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
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
        Intent i = new Intent(this, NoteActivity.class);
        i.putExtra("noteId", noteId);
        startActivity(i);
    }

private void openExportMenu() {
        final String[] items = {"Markdown 笔记", "HTML 笔记（含公式图片）", "公式合集（HTML）", "JSON（仅文字）", "完整备份（zip）"};
        new AlertDialog.Builder(this)
                .setTitle("导出")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startExport(which);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private String safeName() {
        String base = subjectName == null ? "notes" : subjectName.replaceAll("[\\\\/:*?\"<>|]", "_");
        return base.isEmpty() ? "notes" : base;
    }

    private void startExport(int kind) {
        try {
            if (kind == 0) {
                pendingBytes = Exporter.markdown(db, subjectId).getBytes("UTF-8");
                pendingName = safeName() + "-笔记.md";
                pendingMime = "text/markdown";
            } else if (kind == 1) {
                pendingBytes = Exporter.html(this, db, subjectId).getBytes("UTF-8");
                pendingName = safeName() + "-笔记.html";
                pendingMime = "text/html";
            } else if (kind == 2) {
                pendingBytes = Exporter.formulas(this, db, subjectId).getBytes("UTF-8");
                pendingName = safeName() + "-公式合集.html";
                pendingMime = "text/html";
            } else if (kind == 4) {
                pendingBytes = Exporter.json(db, subjectId).getBytes("UTF-8");
                pendingName = safeName() + "-笔记.json";
                pendingMime = "application/json";
            } else {
                byte[] data = Exporter.backupSubject(db, this, subjectId);
                java.io.File saved = Backups.save(this, safeName() + "-备份-" + Backups.stamp() + ".zip", data);
                new AlertDialog.Builder(this)
                        .setTitle("备份已保存")
                        .setMessage("文件：\n" + saved.getName() + "\n\n位置：\n" + Backups.dirHint(this)
                                + "\n\n恢复时在这里直接选，不用去翻文件管理器。")
                        .setPositiveButton("好", null)
                        .show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "生成失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(pendingMime);
        i.putExtra(Intent.EXTRA_TITLE, pendingName);
        startActivityForResult(i, REQ_SAVE);
    }

    private void editCriteria() {
        final EditText input = new EditText(this);
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

    private void runTitles() {
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
                });
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
        runImport(uris);
    }

    private void runImport(final List<Uri> uris) {
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
                Importer.Outcome out = Importer.run(SubjectActivity.this, db, prefs, subjectId, uris,
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
                        reload();
                        StringBuilder sb = new StringBuilder();
                        sb.append("识别 ").append(out.pages).append(" 页，新增 ").append(out.points).append(" 个知识点");
                        if (!out.errors.isEmpty()) {
                            sb.append("\n").append(out.errors.get(0));
                            if (out.errors.size() > 1) sb.append("（共 ").append(out.errors.size()).append(" 个错误）");
                        }
                        failedUris.clear();
                        failedUris.addAll(out.failed);
                        retryBtn.setVisibility(failedUris.isEmpty() ? View.GONE : View.VISIBLE);
                        if (!failedUris.isEmpty()) {
                            retryBtn.setText("  重试失败（" + failedUris.size() + "）  ");
                        }
                        Toast.makeText(SubjectActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }
}
