package com.dshwork.mynotes;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
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
public class MindMapActivity extends BaseActivity {

    private Db db;
    private java.util.List<String> onlyIds;
    private MindMapView view;
    private static final int REQ_SAVE = 4001;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Ui.apply(this);
        db = new Db(this);
        String subjectId = getIntent().getStringExtra("subjectId");
        String subjectName = getIntent().getStringExtra("subjectName");
        onlyIds = getIntent().getStringArrayListExtra("noteIds");

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
        TextView title = Ui.text(this, (subjectName == null ? "导图" : subjectName) + " · 导图", 17, Ui.INK);
        titleBox.addView(title);
        int catCount = db.categories(subjectId).size();
        int noteCount = db.notes(subjectId).size();
        TextView sub = Ui.text(this, catCount + " 个分类 · " + noteCount + " 个知识点", 11.5f, Ui.MUTED);
        sub.setPadding(0, Ui.dp(this, 1), 0, 0);
        titleBox.addView(sub);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

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

        TextView export = Ui.chip(this, "导出 PNG", false);
        export.setPadding(Ui.dp(this, 6), 0, Ui.dp(this, 10), 0);
        export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/png");
                i.putExtra(Intent.EXTRA_TITLE, "思维导图.png");
                startActivityForResult(i, REQ_SAVE);
            }
        });
        bar.addView(export);

        TextView reset = Ui.chip(this, "适应视图", false);
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
        if (db.notes(subjectId).isEmpty()) {
            LinearLayout empty = Ui.emptyState(this, "还没有知识点",
                    "导入课件并完成识别后，这里会长出知识点导图。", null, null);
            root.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        } else {
            root.addView(view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_SAVE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try {
            Bitmap bmp = view.renderToBitmap();
            if (bmp == null) return;
            java.io.OutputStream os = getContentResolver().openOutputStream(data.getData());
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.close();
            bmp.recycle();
            android.widget.Toast.makeText(this, "已导出 PNG", android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "导出失败：" + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
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
            if (onlyIds != null && !onlyIds.isEmpty() && !onlyIds.contains(note.id)) continue;
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
