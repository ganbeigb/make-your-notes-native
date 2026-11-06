package com.dshwork.mynotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
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

import java.util.List;

/** 科目列表：原生 View，没有 WebView，也没有本地服务器。 */
public class MainActivity extends BaseActivity {

    private Db db;
    private List<Db.Subject> data;
    private BaseAdapter adapter;
    private TextView empty;
    private static final int REQ_RESTORE = 3001;
    private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());
    private ListView list;

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
        bar.setPadding(Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 10));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView title = Ui.text(this, "Make Your Notes", 18, Ui.INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        brand.addView(title);
        TextView brandSub = Ui.text(this, "Local · DeepSeek", 11.5f, Ui.MUTED);
        brandSub.setPadding(0, Ui.dp(this, 1), 0, 0);
        brand.addView(brandSub);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView add = Ui.iconButton(this, "＋");
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askNewSubject();
            }
        });
        TextView usage = Ui.text(this, "用量", 13, Ui.INK2);
        usage.setPadding(Ui.dp(this, 0), 0, Ui.dp(this, 14), 0);
        usage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, UsageActivity.class));
            }
        });
        TextView restore = Ui.text(this, "恢复", 13, Ui.INK2);
        restore.setPadding(0, 0, Ui.dp(this, 14), 0);
        restore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickBackup();
            }
        });
        TextView more = Ui.iconButton(this, "⋯");

        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMenu();
            }
        });
        bar.addView(more);

        bar.addView(add);
        root.addView(bar);


        list = new ListView(this);
        list.setDivider(null);
        list.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 12));
        list.setClipToPadding(false);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        empty = Ui.text(this, "还没有科目，点右上角「＋ 新建」开始。", 14, Ui.INK2);
        empty.setGravity(Gravity.CENTER);
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
                Row row;
                if (convertView == null) {
                    row = new Row();
                    row.card = new LinearLayout(MainActivity.this);
                    row.card.setOrientation(LinearLayout.VERTICAL);
                    row.card.setPadding(Ui.dp(MainActivity.this, 16), Ui.dp(MainActivity.this, 14),
                            Ui.dp(MainActivity.this, 16), Ui.dp(MainActivity.this, 14));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.bottomMargin = Ui.dp(MainActivity.this, 10);
                    row.card.setLayoutParams(lp);

                    LinearLayout head = new LinearLayout(MainActivity.this);
                    head.setOrientation(LinearLayout.HORIZONTAL);
                    head.setGravity(Gravity.CENTER_VERTICAL);
                    row.dot = new android.view.View(MainActivity.this);
                    LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                            Ui.dp(MainActivity.this, 9), Ui.dp(MainActivity.this, 9));
                    dlp.rightMargin = Ui.dp(MainActivity.this, 8);
                    head.addView(row.dot, dlp);
                    row.name = Ui.text(MainActivity.this, "", 16, Ui.INK);
                    row.name.setTypeface(Typeface.DEFAULT_BOLD);
                    head.addView(row.name);
                    row.card.addView(head);

                    row.meta = Ui.text(MainActivity.this, "", 12, Ui.INK2);
                    row.meta.setPadding(0, Ui.dp(MainActivity.this, 4), 0, 0);
                    row.card.addView(row.meta);

                    row.card.setTag(row);
                } else {
                    row = (Row) convertView.getTag();
                }
                Db.Subject s = data.get(position);
                row.card.setBackground(Ui.card(MainActivity.this));
                row.name.setText(s.name);
                int[] st = db.subjectStats(s.id);
                row.meta.setText(st[0] + " 个课件 · " + st[1] + " 页 · " + st[2] + " 个知识点");
                if (row.dot != null) {
                    android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
                    g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                    try {
                        g.setColor(android.graphics.Color.parseColor(s.color == null ? "#2C5D4A" : s.color));
                    } catch (Exception e) {
                        g.setColor(Ui.ACCENT);
                    }
                    row.dot.setBackground(g);
                }
                return row.card;
            }

            class Row {
                LinearLayout card;
                TextView name;
                TextView meta;
                View dot;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(MainActivity.this, SubjectActivity.class);
                i.putExtra("subjectId", data.get(position).id);
                i.putExtra("subjectName", data.get(position).name);
                startActivity(i);
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                subjectMenu(data.get(position));
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

    /** 长按科目：重命名 / 删除（网页 rename-subject、delete-subject） */
    private void subjectMenu(final Db.Subject s) {
        final String[] ops = {"重命名科目", "删除科目"};
        new AlertDialog.Builder(this)
                .setItems(ops, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            final EditText input = new EditText(MainActivity.this);
                            input.setText(s.name);
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("重命名科目")
                                    .setView(input)
                                    .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dlg, int w) {
                                            String name = input.getText().toString().trim();
                                            if (!name.isEmpty()) {
                                                db.renameSubject(s.id, name);
                                                reload();
                                            }
                                        }
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        } else {
                            confirmDelete(s);
                        }
                    }
                })
                .show();
    }

    private void reload() {
        data = db.subjects();
        adapter.notifyDataSetChanged();
        empty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
        list.setVisibility(data.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void openMenu() {
        final String[] titles = {"搜索", "用量", "设置", "恢复备份", "导出全部备份", "自检", "关于"};
        final String[] hints = {
                "跨科目按标题、正文、公式查",
                "调用次数、token 与花费",
                "服务商、模型、主题、字号",
                "从备份目录挑一个 zip 恢复",
                "把全部科目打包成一个 zip",
                "跑 7 项运行期检查",
                "版本、数据说明、功能清单"
        };
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 8));
        for (int i = 0; i < titles.length; i++) {
            final int which = i;
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
                    menuAction(which);
                }
            });
            box.addView(row);
        }
        android.widget.ScrollView sc = new android.widget.ScrollView(this);
        sc.addView(box);
        new AlertDialog.Builder(this)
                .setView(sc)
                .setNegativeButton("关闭", null)
                .show();
    }

    private void menuAction(int which) {
        if (which == 0) startActivity(new Intent(MainActivity.this, SearchActivity.class));
        else if (which == 1) startActivity(new Intent(MainActivity.this, UsageActivity.class));
        else if (which == 2) startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        else if (which == 3) pickBackup();
        else if (which == 4) exportAllBackup();
        else if (which == 5) startActivity(new Intent(MainActivity.this, SelfCheckActivity.class));
        else showAbout();
    }

    private void exportAllBackup() {
        try {
            byte[] data = Exporter.backupAll(db, this);
            java.io.File saved = Backups.save(this, "全部科目-备份-" + Backups.stamp() + ".zip", data);
            new AlertDialog.Builder(this)
                    .setTitle("全部科目已备份")
                    .setMessage("文件：\n" + saved.getName() + "\n\n位置：\n" + Backups.dirHint(this))
                    .setPositiveButton("好", null)
                    .show();
        } catch (Exception e) {
            Toast.makeText(this, "备份失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showAbout() {
        String version = "0.12.0";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
        }
        int[] stat = new int[]{0, 0};
        try {
            for (Db.Subject s : db.subjects()) {
                stat[0]++;
                stat[1] += db.notes(s.id).size();
            }
        } catch (Exception ignored) {
        }
        String msg = "Make Your Notes · 原生版 " + version + "\n\n"
                + "本机数据：" + stat[0] + " 个科目 · " + stat[1] + " 条知识点\n"
                + "备份目录：" + Backups.dirHint(this) + "\n\n"
                + "全部数据保存在本机 SQLite，不上传服务器；只有导入课件和 AI 整理时会把图片/文字发给模型。\n\n"
                + "功能：课件截图提取、自动分类、公式渲染、画板配图、思维导图、导出与备份、用量统计、全局搜索。";
        new AlertDialog.Builder(this)
                .setTitle("关于")
                .setMessage(msg)
                .setPositiveButton("好", null)
                .show();
    }

    private void pickBackup() {
        showRestorePicker();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_RESTORE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try {
            java.io.InputStream in = getContentResolver().openInputStream(data.getData());
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            startRestore(bos.toByteArray());
        } catch (Exception e) {
            Toast.makeText(this, "读取备份失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** 备份目录里能识别的 zip */
    private void showRestorePicker() {
        final java.util.List<java.io.File> files = Backups.list(this);
        final java.util.List<java.io.File> valid = new java.util.ArrayList<>();
        final java.util.List<String> labels = new java.util.ArrayList<>();
        for (java.io.File f : files) {
            if (Backups.kindOf(f).isEmpty()) continue;
            valid.add(f);
            labels.add(f.getName() + "\n" + Backups.describe(f));
        }
        if (valid.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("没有找到备份")
                    .setMessage("备份目录里没有可直接识别的文件：\n" + Backups.dirHint(this)
                            + "\n\n你也可以从其他位置选一个 zip。")
                    .setPositiveButton("从其他位置选择", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            openSystemPicker();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return;
        }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 8));

        for (final java.io.File f : valid) {
            String kind = Backups.kindOf(f);
            boolean web = "web".equals(kind);

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

            TextView name = Ui.text(this, f.getName(), 13.5f, Ui.INK);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            row.addView(name);

            TextView meta = Ui.text(this, (web ? "网页版备份 · " : "本机备份 · ") + Backups.describe(f).replace("网页版备份 · ", "").replace("本机备份 · ", ""),
                    11.5f, Ui.MUTED);
            meta.setPadding(0, Ui.dp(this, 2), 0, 0);
            row.addView(meta);

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmRestore(f);
                }
            });
            box.addView(row);
        }

        LinearLayout other = new LinearLayout(this);
        other.setPadding(Ui.dp(this, 14), Ui.dp(this, 11), Ui.dp(this, 14), Ui.dp(this, 11));
        other.setBackground(Ui.card(this));
        TextView otherText = Ui.text(this, "从其他位置选择…", 13, Ui.ACCENT);
        other.addView(otherText);
        other.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSystemPicker();
            }
        });
        box.addView(other);

        android.widget.ScrollView sc = new android.widget.ScrollView(this);
        sc.addView(box);
        new AlertDialog.Builder(this)
                .setTitle("选择要恢复的备份")
                .setView(sc)
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmRestore(final java.io.File f) {
        String kind = Backups.kindOf(f);
        String detail = f.getName() + "\n" + Backups.describe(f)
                + "\n\n" + ("web".equals(kind)
                ? "会把网页版备份里的科目、课件、页面、知识点、原图导入。"
                : "同名科目按名称合并，重复知识点跳过。");
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("确认恢复")
                .setMessage(detail)
                .setPositiveButton("恢复", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d2, int w2) {
                        try {
                            byte[] data = readAll(f);
                            startRestore(data);
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "读取失败：" + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private byte[] readAll(java.io.File f) throws Exception {
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        in.close();
        return bos.toByteArray();
    }

    private void openSystemPicker() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, REQ_RESTORE);
    }

    /** 解析 zip 并合并进库（原生 / 网页版两种格式自动识别） */
    private void startRestore(final byte[] data) {
        final String[] holder = new String[]{null};
        final boolean[] done = new boolean[]{false};
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    boolean isWeb = false;
                    java.util.zip.ZipInputStream probe = new java.util.zip.ZipInputStream(
                            new java.io.ByteArrayInputStream(data));
                    java.util.zip.ZipEntry pe;
                    while ((pe = probe.getNextEntry()) != null) {
                        if ("manifest.json".equals(pe.getName())) {
                            isWeb = true;
                            break;
                        }
                        if (pe.getName().endsWith("mynotes-backup.json")) break;
                    }
                    probe.close();

                    if (isWeb) {
                        WebBackup.Result wr = WebBackup.importFromZip(MainActivity.this, db,
                                new java.io.ByteArrayInputStream(data), new Prefs(MainActivity.this));
                        msg = wr.error != null ? ("网页版备份导入失败：" + wr.error)
                                : ("已从网页版备份导入：" + wr.subjects + " 科目、" + wr.decks + " 课件、" + wr.categories + " 分类、"
                                   + wr.notes + " 知识点、" + wr.images + " 张原图");
                    } else {
                        String json = null;
                        java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(
                                new java.io.ByteArrayInputStream(data));
                        java.util.zip.ZipEntry e;
                        byte[] buf = new byte[8192];
                        while ((e = zip.getNextEntry()) != null) {
                            if (e.getName().endsWith("mynotes-backup.json")) {
                                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                                int n;
                                while ((n = zip.read(buf)) > 0) bos.write(buf, 0, n);
                                json = new String(bos.toByteArray(), "UTF-8");
                                break;
                            }
                        }
                        zip.close();
                        if (json == null) {
                            msg = "这个 zip 既不是原生版备份，也不是网页版备份";
                        } else {
                            org.json.JSONObject obj = new org.json.JSONObject(json);
                            org.json.JSONObject settings = obj.optJSONObject("settings");
                            if (settings != null) new Prefs(MainActivity.this).restore(settings);
                            int usageAdded = db.importUsage(obj.optJSONArray("usage"));
                            int[] r = db.importBackup(obj);
                            msg = "恢复完成：新增 " + r[0] + " 科目、" + r[1] + " 分类、" + r[2] + " 知识点、"
                                    + usageAdded + " 条用量";
                        }
                    }
                } catch (final Exception ex) {
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "恢复失败：" + ex.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                final String finalMsg = msg;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        reload();
                        Toast.makeText(MainActivity.this, finalMsg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void askNewSubject() {
        final EditText input = new EditText(this);
        input.setHint("例如：电路原理");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
                .setTitle("新建科目")
                .setView(input)
                .setPositiveButton("创建", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) {
                            Toast.makeText(MainActivity.this, "名字不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        db.addSubject(name);
                        reload();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmDelete(final Db.Subject s) {
        new AlertDialog.Builder(this)
                .setTitle("删除科目")
                .setMessage("「" + s.name + "」和它下面的 " + s.noteCount + " 个知识点都会被删除。")
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.deleteSubject(s.id);
                        reload();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
