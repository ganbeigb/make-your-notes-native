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
public class MainActivity extends Activity {

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
        Ui.setScale(new Prefs(this).uiScale());
        db = new Db(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 10));

        TextView title = Ui.text(this, "我的科目", 22, Ui.INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView add = Ui.button(this, "＋ 新建");
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
        TextView more = Ui.text(this, "⋯", 20, Ui.ACCENT);
        more.setPadding(0, 0, Ui.dp(this, 12), 0);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMenu();
            }
        });
        bar.addView(more);

        bar.addView(add);
        root.addView(bar);

        TextView sub = Ui.text(this, "原生架构 · SQLite 本地存储 · 不加载网页", 12, Ui.INK2);
        sub.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 10));
        root.addView(sub);

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
                Db.Subject s = data.get(position);
                LinearLayout card = new LinearLayout(MainActivity.this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackground(Ui.card(MainActivity.this));
                card.setPadding(Ui.dp(MainActivity.this, 16), Ui.dp(MainActivity.this, 14),
                        Ui.dp(MainActivity.this, 16), Ui.dp(MainActivity.this, 14));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = Ui.dp(MainActivity.this, 10);
                card.setLayoutParams(lp);

                TextView name = Ui.text(MainActivity.this, s.name, 17, Ui.INK);
                name.setTypeface(Typeface.DEFAULT_BOLD);
                card.addView(name);

                TextView meta = Ui.text(MainActivity.this, s.noteCount + " 个知识点", 12, Ui.INK2);
                meta.setPadding(0, Ui.dp(MainActivity.this, 4), 0, 0);
                card.addView(meta);
                return card;
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
                confirmDelete(data.get(position));
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
        data = db.subjects();
        adapter.notifyDataSetChanged();
        empty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
        list.setVisibility(data.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void openMenu() {
        final String[] items = {"搜索", "用量", "设置", "恢复备份", "关于"};
        new AlertDialog.Builder(this)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) startActivity(new Intent(MainActivity.this, SearchActivity.class));
                        else if (which == 1) startActivity(new Intent(MainActivity.this, UsageActivity.class));
                        else if (which == 2) startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                        else if (which == 3) pickBackup();
                        else showAbout();
                    }
                })
                .show();
    }

    private void showAbout() {
        String version = "0.12.0";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
        }
        String msg = "Make Your Notes · 原生版 " + version + "\n\n"
                + "全部数据保存在本机 SQLite，不上传服务器；只有导入课件和 AI 整理时会把图片/文字发给模型。\n\n"
                + "功能：课件截图提取、自动分类、公式渲染、画板配图、思维导图、导出与备份、用量统计、全局搜索。";
        new AlertDialog.Builder(this)
                .setTitle("关于")
                .setMessage(msg)
                .setPositiveButton("好", null)
                .show();
    }

    private void pickBackup() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, REQ_RESTORE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_RESTORE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String msg;
                try {
                    java.io.InputStream is = getContentResolver().openInputStream(uri);
                    java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(is);
                    String json = null;
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
                        msg = "这个 zip 里没有找到 mynotes-backup.json";
                    } else {
                        int[] r = db.importBackup(new org.json.JSONObject(json));
                        msg = "恢复完成：新增 " + r[0] + " 个科目、" + r[1] + " 个分类、" + r[2] + " 条知识点";
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
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        reload();
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
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
