package com.dshwork.mynotes;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 思维导图页面：科目 → 分类 → 知识点。 */
public class MindMapActivity extends Activity {

    private Db db;
    private MindMapView view;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Ui.setScale(new Prefs(this).uiScale());
        db = new Db(this);
        String subjectId = getIntent().getStringExtra("subjectId");
        String subjectName = getIntent().getStringExtra("subjectName");

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

        TextView title = Ui.text(this, (subjectName == null ? "导图" : subjectName) + " · 导图", 17, Ui.INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView zoomIn = Ui.text(this, "＋", 18, Ui.ACCENT);
        zoomIn.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        zoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.zoomBy(1.25f);
            }
        });
        bar.addView(zoomIn);

        TextView zoomOut = Ui.text(this, "－", 18, Ui.ACCENT);
        zoomOut.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        zoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.zoomBy(0.8f);
            }
        });
        bar.addView(zoomOut);

        TextView reset = Ui.text(this, "适应", 14, Ui.ACCENT);
        reset.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 6), 0);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.resetView();
            }
        });
        bar.addView(reset);
        root.addView(bar);

        view = new MindMapView(this);
        root.addView(view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);

        view.setOnNodeClick(new MindMapView.OnNodeClick() {
            @Override
            public void onClick(String noteId) {
                Intent i = new Intent(MindMapActivity.this, NoteActivity.class);
                i.putExtra("noteId", noteId);
                startActivity(i);
            }
        });
        view.setTree(build(subjectId, subjectName));
    }

    private MindMapView.Node build(String subjectId, String subjectName) {
        MindMapView.Node root = new MindMapView.Node();
        root.kind = 0;
        root.label = subjectName == null ? "科目" : subjectName;

        List<Db.Category> cats = db.categories(subjectId);
        Map<String, MindMapView.Node> catNodes = new HashMap<>();
        Map<String, Db.Category> byId = new HashMap<>();
        for (Db.Category c : cats) byId.put(c.id, c);

        for (Db.Category c : cats) {
            MindMapView.Node n = new MindMapView.Node();
            n.kind = 1;
            n.label = c.name;
            catNodes.put(c.id, n);
            MindMapView.Node parent = c.parentId == null ? root : catNodes.get(c.parentId);
            if (parent == null) parent = root;
            parent.children.add(n);
        }

        MindMapView.Node misc = null;
        for (Db.Note note : db.notes(subjectId)) {
            MindMapView.Node n = new MindMapView.Node();
            n.kind = 2;
            n.id = note.id;
            n.label = note.name;
            MindMapView.Node parent = note.categoryId == null ? null : catNodes.get(note.categoryId);
            if (parent == null) {
                if (misc == null) {
                    misc = new MindMapView.Node();
                    misc.kind = 1;
                    misc.label = "未分类";
                    root.children.add(misc);
                }
                parent = misc;
            }
            parent.children.add(n);
        }
        return root;
    }
}
