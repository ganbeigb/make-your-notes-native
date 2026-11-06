package com.dshwork.mynotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
    private ListView list;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
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
        bar.addView(usage);

        TextView settings = Ui.text(this, "设置", 13, Ui.INK2);
        settings.setPadding(0, 0, Ui.dp(this, 12), 0);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        bar.addView(settings);

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
