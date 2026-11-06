package com.dshwork.mynotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

/** 某个科目下的知识点列表。 */
public class SubjectActivity extends Activity {

    private Db db;
    private String subjectId;
    private List<Db.Note> data;
    private BaseAdapter adapter;
    private TextView empty;
    private ListView list;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        db = new Db(this);
        subjectId = getIntent().getStringExtra("subjectId");
        String subjectName = getIntent().getStringExtra("subjectName");

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

        list = new ListView(this);
        list.setDivider(null);
        list.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 12));
        list.setClipToPadding(false);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        empty = Ui.text(this, "还没有知识点。", 14, Ui.INK2);
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
                Db.Note n = data.get(position);
                LinearLayout card = new LinearLayout(SubjectActivity.this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackground(Ui.card(SubjectActivity.this));
                card.setPadding(Ui.dp(SubjectActivity.this, 16), Ui.dp(SubjectActivity.this, 12),
                        Ui.dp(SubjectActivity.this, 16), Ui.dp(SubjectActivity.this, 12));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = Ui.dp(SubjectActivity.this, 10);
                card.setLayoutParams(lp);

                String cat = n.category == null ? "" : n.category.trim();
                if (!cat.isEmpty()) {
                    TextView tag = Ui.text(SubjectActivity.this, cat, 11, Ui.ACCENT);
                    card.addView(tag);
                }

                TextView name = Ui.text(SubjectActivity.this, n.name, 16, Ui.INK);
                name.setTypeface(Typeface.DEFAULT_BOLD);
                card.addView(name);

                String preview = n.content == null ? "" : n.content.replace("\n", " ").trim();
                if (preview.length() > 40) preview = preview.substring(0, 40) + "…";
                if (!preview.isEmpty()) {
                    TextView p = Ui.text(SubjectActivity.this, preview, 12, Ui.INK2);
                    p.setPadding(0, Ui.dp(SubjectActivity.this, 4), 0, 0);
                    card.addView(p);
                }
                return card;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openNote(data.get(position).id);
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
        data = db.notes(subjectId);
        adapter.notifyDataSetChanged();
        boolean isEmpty = data.isEmpty();
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void openNote(String noteId) {
        Intent i = new Intent(this, NoteActivity.class);
        i.putExtra("noteId", noteId);
        startActivity(i);
    }
}
