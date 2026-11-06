package com.dshwork.mynotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** 知识点详情：编辑 + 公式实时预览。 */
public class NoteActivity extends BaseActivity {

    private Db db;
    private Prefs prefs;
    private String snapName, snapContent, snapFormula;
    private boolean hasSnap;
    private TextView undoAiChip;
    private Db.Note note;
    private EditText nameInput;
    private EditText categoryInput;
    private EditText tagsInput;
    private EditText formulaInput;
    private EditText contentInput;
    private FormulaView preview;
    private ImageView figureView;
    private TextView figureBtn;
    private EditText focused;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Ui.apply(this);
        db = new Db(this);
        prefs = new Prefs(this);
        note = db.note(getIntent().getStringExtra("noteId"));
        if (note == null) {
            finish();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 10));
        TextView back = Ui.text(this, "‹", 26, Ui.ACCENT);
        back.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 12), 0);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save(false);
                finish();
            }
        });
        bar.addView(back);
        TextView title = Ui.text(this, "知识点", 19, Ui.INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView saveBtn = Ui.button(this, "保存");
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save(true);
            }
        });
        bar.addView(saveBtn);
        root.addView(bar);

        LinearLayout aiRow = new LinearLayout(this);
        aiRow.setOrientation(LinearLayout.HORIZONTAL);
        aiRow.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 4));

        final TextView kindPoint = Ui.chip(this, "知识点", note.kind == null || "point".equals(note.kind));
        final TextView kindExt = Ui.chip(this, "拓展", "extension".equals(note.kind));
        kindPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                note.kind = "point";
                kindPoint.setBackground(Ui.chip(NoteActivity.this, "x", true).getBackground());
                kindPoint.setTextColor(0xFFFFFFFF);
                kindExt.setBackground(Ui.chip(NoteActivity.this, "x", false).getBackground());
                kindExt.setTextColor(Ui.INK);
                save(false);
            }
        });
        kindExt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                note.kind = "extension";
                kindExt.setBackground(Ui.chip(NoteActivity.this, "x", true).getBackground());
                kindExt.setTextColor(0xFFFFFFFF);
                kindPoint.setBackground(Ui.chip(NoteActivity.this, "x", false).getBackground());
                kindPoint.setTextColor(Ui.INK);
                save(false);
            }
        });
        aiRow.addView(kindPoint);
        aiRow.addView(kindExt);

        final java.util.List<TextView> impChips = new java.util.ArrayList<>();
        for (int k = 1; k <= 5; k++) {
            final int level = k;
            final TextView chipImp = Ui.chip(this, k == 5 ? "★★★" : (k == 4 ? "★★" : (k >= 2 ? "★" : "·")), note.importance == k);
            chipImp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    note.importance = level;
                    for (int i = 0; i < impChips.size(); i++) {
                        boolean on = i + 1 == level;
                        impChips.get(i).setBackground(Ui.chip(NoteActivity.this, "x", on).getBackground());
                        impChips.get(i).setTextColor(on ? 0xFFFFFFFF : Ui.INK);
                    }
                    save(false);
                }
            });
            impChips.add(chipImp);
            aiRow.addView(chipImp);
        }

        TextView pickCat = Ui.chip(this, "选分类", false);
        pickCat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickCategory();
            }
        });
        aiRow.addView(pickCat);

        TextView copy = Ui.chip(this, "复制", false);
        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                duplicate();
            }
        });
        aiRow.addView(copy);

        undoAiChip = Ui.chip(this, "撤销 AI 修改", false);
        undoAiChip.setVisibility(View.GONE);
        undoAiChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasSnap) return;
                nameInput.setText(snapName);
                contentInput.setText(snapContent);
                formulaInput.setText(snapFormula);
                hasSnap = false;
                undoAiChip.setVisibility(View.GONE);
                save(false);
                Toast.makeText(NoteActivity.this, "已还原", Toast.LENGTH_SHORT).show();
            }
        });
        aiRow.addView(undoAiChip);
        root.addView(aiRow);

        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 24));

        LinearLayout draftBlock = new LinearLayout(this);
        draftBlock.setOrientation(LinearLayout.VERTICAL);
        draftBlock.setBackground(Ui.card(this));
        draftBlock.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        LinearLayout.LayoutParams dblp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dblp.bottomMargin = Ui.dp(this, 12);
        draftBlock.setLayoutParams(dblp);

        TextView draftTitle = Ui.text(this, "让 AI 起草", 14, Ui.INK);
        draftTitle.setTypeface(Typeface.DEFAULT_BOLD);
        draftBlock.addView(draftTitle);
        TextView draftHint = Ui.text(this, "写一句要求，生成后还可以改", 11.5f, Ui.MUTED);
        draftHint.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 8));
        draftBlock.addView(draftHint);

        LinearLayout draftRow = new LinearLayout(this);
        draftRow.setOrientation(LinearLayout.HORIZONTAL);
        draftRow.setGravity(Gravity.CENTER_VERTICAL);
        final EditText draftInput = new EditText(this);
        draftInput.setHint("例如：用两句话解释洛必达法则的适用条件");
        draftInput.setTextSize(13);
        draftInput.setBackground(Ui.card(this));
        draftInput.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 6));
        draftRow.addView(draftInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView draftGo = Ui.button(this, "生成");
        LinearLayout.LayoutParams dglp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dglp.leftMargin = Ui.dp(this, 8);
        draftGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String req = draftInput.getText().toString().trim();
                if (req.isEmpty()) {
                    Toast.makeText(NoteActivity.this, "先写一句要求", Toast.LENGTH_SHORT).show();
                    return;
                }
                runAiDraft(req);
            }
        });
        draftRow.addView(draftGo, dglp);
        draftBlock.addView(draftRow);
        form.addView(draftBlock);

        LinearLayout editBlock = new LinearLayout(this);
        editBlock.setOrientation(LinearLayout.VERTICAL);
        editBlock.setBackground(Ui.card(this));
        editBlock.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        LinearLayout.LayoutParams eblp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        eblp.bottomMargin = Ui.dp(this, 12);
        editBlock.setLayoutParams(eblp);

        TextView editTitle = Ui.text(this, "让 AI 改这条", 14, Ui.INK);
        editTitle.setTypeface(Typeface.DEFAULT_BOLD);
        editBlock.addView(editTitle);
        TextView editHint = Ui.text(this, "写一句要求，或点下面的快捷建议", 11.5f, Ui.MUTED);
        editHint.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 8));
        editBlock.addView(editHint);

        final EditText editInput = new EditText(this);
        editInput.setHint("例如：改写成适合背诵的短句");
        editInput.setTextSize(13);
        editInput.setBackground(Ui.card(this));
        editInput.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 6));
        editBlock.addView(editInput);

        LinearLayout sugRow = new LinearLayout(this);
        sugRow.setOrientation(LinearLayout.HORIZONTAL);
        sugRow.setPadding(0, Ui.dp(this, 8), 0, 0);
        String[] suggests = {"更易背诵", "补充适用条件", "精简到两句", "加上易错点", "换成公式推导"};
        for (final String sug : suggests) {
            TextView chipSug = Ui.chip(this, sug, false);
            chipSug.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String old = editInput.getText().toString().trim();
                    editInput.setText(old.isEmpty() ? sug : old + "；" + sug);
                    editInput.setSelection(editInput.getText().length());
                }
            });
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.rightMargin = Ui.dp(this, 6);
            sugRow.addView(chipSug, slp);
        }
        android.widget.HorizontalScrollView sugScroll = new android.widget.HorizontalScrollView(this);
        sugScroll.setHorizontalScrollBarEnabled(false);
        sugScroll.addView(sugRow);
        editBlock.addView(sugScroll);

        LinearLayout editFoot = new LinearLayout(this);
        editFoot.setOrientation(LinearLayout.HORIZONTAL);
        editFoot.setGravity(Gravity.CENTER_VERTICAL);
        editFoot.setPadding(0, Ui.dp(this, 8), 0, 0);
        final android.widget.CheckBox appendBox = new android.widget.CheckBox(this);
        appendBox.setText("在原内容后追加");
        appendBox.setTextSize(12);
        editFoot.addView(appendBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView editGo = Ui.button(this, "开始改写");
        editGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String req = editInput.getText().toString().trim();
                if (req.isEmpty()) {
                    Toast.makeText(NoteActivity.this, "先写一句要求或点个快捷建议", Toast.LENGTH_SHORT).show();
                    return;
                }
                runAiEdit(req, appendBox.isChecked());
            }
        });
        editFoot.addView(editGo);
        editBlock.addView(editFoot);

        form.addView(editBlock);

        nameInput = field(form, "标题", note.name, InputType.TYPE_CLASS_TEXT, 1);

        contentInput = field(form, "正文", note.content == null ? "" : note.content,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 8);

        formulaInput = field(form, "公式（LaTeX）", note.formula == null ? "" : note.formula, InputType.TYPE_CLASS_TEXT, 1);
        preview = new FormulaView(this);
        preview.setBackground(Ui.card(this));
        preview.setTex(note.formula == null ? "" : note.formula);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.topMargin = Ui.dp(this, 6);
        form.addView(preview, plp);

        formulaInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) focused = formulaInput;
            }
        });

        formulaInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                preview.setTex(s.toString());
            }
        });

        categoryInput = field(form, "分类（用 / 分层，例如 力学/运动学）",
                note.categoryPath == null ? "" : note.categoryPath, InputType.TYPE_CLASS_TEXT, 1);

        tagsInput = field(form, "标签（逗号分隔）", note.tags == null ? "" : note.tags,
                InputType.TYPE_CLASS_TEXT, 1);

        String digest = note.slidePage > 0 ? db.pageDigest(note.subjectId, note.slidePage) : null;
        if (digest != null && !digest.trim().isEmpty()) {
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setBackground(Ui.card(this));
            box.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
            LinearLayout.LayoutParams lp0 = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp0.topMargin = Ui.dp(this, 14);
            TextView label = Ui.text(this, "本页汇总（第 " + note.slidePage + " 页）", 11, Ui.INK2);
            box.addView(label);
            TextView text = Ui.text(this, digest.trim(), 13, Ui.INK);
            text.setPadding(0, Ui.dp(this, 4), 0, 0);
            box.addView(text);
            form.addView(box, lp0);
        }

        if (prefs.showQuote() && note.quote != null && !note.quote.trim().isEmpty()) {
            LinearLayout quoteBox = new LinearLayout(this);
            quoteBox.setOrientation(LinearLayout.VERTICAL);
            quoteBox.setBackground(Ui.card(this));
            quoteBox.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
            LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            qlp.topMargin = Ui.dp(this, 14);
            TextView qLabel = Ui.text(this, "原文摘录", 11, Ui.INK2);
            quoteBox.addView(qLabel);
            TextView qText = Ui.text(this, "「" + note.quote.trim() + "」", 13, Ui.INK);
            qText.setPadding(0, Ui.dp(this, 4), 0, 0);
            quoteBox.addView(qText);
            form.addView(quoteBox, qlp);
        }

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, Ui.dp(this, 14), 0, 0);
        if (note.slidePath != null && !note.slidePath.isEmpty()) {
            TextView slideBtn = Ui.chip(this, "查看课件原图 · 第 " + note.slidePage + " 页", false);
            slideBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSlide();
                }
            });
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.rightMargin = Ui.dp(this, 8);
            actionRow.addView(slideBtn, slp);
        }

        figureBtn = Ui.chip(this, note.figure == null ? "画配图" : "重画配图", false);
        figureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(NoteActivity.this, BoardActivity.class), 2001);
            }
        });
        actionRow.addView(figureBtn);
        form.addView(actionRow);

        figureView = new ImageView(this);
        figureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                figureMenu();
            }
        });
        figureView.setAdjustViewBounds(true);
        figureView.setBackground(Ui.card(this));
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.topMargin = Ui.dp(this, 6);
        form.addView(figureView, flp);
        showFigure();

        contentInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) focused = contentInput;
            }
        });

        TextView symLabel = Ui.text(this, "符号键盘", 12, Ui.INK2);
        symLabel.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 6));
        form.addView(symLabel);

        // 分组与字符与网页版 SYMBOLS 一致
        final String[][] groups = {
                {"常用", "± × ÷ ≠ ≈ ≤ ≥ ∞ ∝ ∴ ∵ ° ′ ″ → ⇒"},
                {"希腊", "α β γ δ ε ζ η θ κ λ μ ν ξ π ρ σ τ φ χ ψ ω Δ Σ Π Ω"},
                {"运算", "∑ ∏ ∫ ∬ ∮ ∂ ∇ √ ∠ ⊥ ∥ ⊕ ⊗ · ! ∘"},
                {"集合", "∈ ∉ ⊂ ⊃ ⊆ ⊇ ∪ ∩ ∅ ∀ ∃ ¬ ∧ ∨ ⇔ ↦"}
        };
        final LinearLayout symRow = new LinearLayout(this);
        symRow.setOrientation(LinearLayout.HORIZONTAL);
        android.widget.HorizontalScrollView symScroll = new android.widget.HorizontalScrollView(this);
        symScroll.setHorizontalScrollBarEnabled(false);
        symScroll.addView(symRow);

        final LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        final java.util.List<TextView> tabViews = new java.util.ArrayList<>();
        for (int gi = 0; gi < groups.length; gi++) {
            final int index = gi;
            final TextView tab = Ui.chip(this, groups[gi][0], gi == 0);
            tab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int k = 0; k < tabViews.size(); k++) {
                        boolean on = k == index;
                        tabViews.get(k).setBackground(Ui.chip(NoteActivity.this, "x", on).getBackground());
                        tabViews.get(k).setTextColor(on ? 0xFFFFFFFF : Ui.INK);
                    }
                    fillSymbols(symRow, groups[index][1]);
                }
            });
            tabViews.add(tab);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tlp.rightMargin = Ui.dp(this, 6);
            tabRow.addView(tab, tlp);
        }
        form.addView(tabRow);
        form.addView(symScroll);
        fillSymbols(symRow, groups[0][1]);

        if (note.source != null && !note.source.isEmpty()) {
            LinearLayout srcBox = new LinearLayout(this);
            srcBox.setOrientation(LinearLayout.VERTICAL);
            srcBox.setBackground(Ui.card(this));
            srcBox.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.topMargin = Ui.dp(this, 14);
            srcBox.setLayoutParams(slp);
            srcBox.addView(Ui.text(this, "来源", 11, Ui.MUTED));
            TextView srcValue = Ui.text(this, note.source, 13, Ui.INK);
            srcValue.setPadding(0, Ui.dp(this, 2), 0, 0);
            srcBox.addView(srcValue);
            form.addView(srcBox);
        }

        scroll.addView(form);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        // 底部固定操作条：删除 / 保存并返回
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomBar.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 12));
        bottomBar.setBackground(Ui.card(this));

        TextView del = Ui.text(this, "删除这条", 13, Ui.RED);
        del.setPadding(Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8));
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(NoteActivity.this)
                        .setTitle("删除知识点")
                        .setMessage(note.name == null ? "这条笔记" : note.name)
                        .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                db.deleteNote(note.id);
                                finish();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
        bottomBar.addView(del, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView done = Ui.button(this, "保存并返回");
        done.setPadding(Ui.dp(this, 20), Ui.dp(this, 9), Ui.dp(this, 20), Ui.dp(this, 9));
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save(true);
                finish();
            }
        });
        bottomBar.addView(done);

        root.addView(bottomBar);
        setContentView(root);
    }

    private EditText field(LinearLayout parent, String label, String value, int inputType, int lines) {
        TextView l = Ui.text(this, label, 12, Ui.INK2);
        l.setTypeface(Typeface.DEFAULT_BOLD);
        l.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 6));
        parent.addView(l);

        EditText e = new EditText(this);
        e.setText(value);
        e.setInputType(inputType);
        e.setMinLines(lines);
        e.setTextSize(14);
        e.setTextColor(Ui.INK);
        e.setBackground(Ui.card(this));
        e.setPadding(Ui.dp(this, 11), Ui.dp(this, 9), Ui.dp(this, 11), Ui.dp(this, 9));
        parent.addView(e, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
    }

    /** 配图菜单：查看大图 / 删除 */
    private void figureMenu() {
        if (note.figure == null || note.figure.isEmpty()) {
            Toast.makeText(this, "还没有配图，先点「画配图」", Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] ops = {"查看大图", "删除配图"};
        new AlertDialog.Builder(this)
                .setItems(ops, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            showFigureFull();
                        } else {
                            note.figure = null;
                            db.saveNote(note);
                            showFigure();
                            Toast.makeText(NoteActivity.this, "已删除配图", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    private void showFigureFull() {
        ImageView big = new ImageView(this);
        big.setAdjustViewBounds(true);
        android.graphics.Bitmap bmp = BitmapFactory.decodeFile(note.figure);
        if (bmp == null) {
            Toast.makeText(this, "配图文件找不到了", Toast.LENGTH_SHORT).show();
            return;
        }
        big.setImageBitmap(bmp);
        android.widget.ScrollView sc = new android.widget.ScrollView(this);
        sc.addView(big);
        new AlertDialog.Builder(this).setView(sc).setPositiveButton("关闭", null).show();
    }

    private void showSlide() {
        if (note.slidePath == null) return;
        android.graphics.Bitmap bmp = BitmapFactory.decodeFile(note.slidePath);
        if (bmp == null) {
            Toast.makeText(this, "原图不存在了", Toast.LENGTH_SHORT).show();
            return;
        }
        ImageView view = new ImageView(this);
        view.setImageBitmap(bmp);
        view.setAdjustViewBounds(true);
        view.setBackgroundColor(0xFFF2EEE3);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(view);
        new android.app.AlertDialog.Builder(this)
                .setTitle("第 " + note.slidePage + " 页")
                .setView(scroll)
                .setPositiveButton("关闭", null)
                .show();
    }

    /** 让 AI 按要求改写这条笔记 */
    private void askAiEdit() {
        if (!prefs.ready()) {
            Toast.makeText(this, "先到设置里填 API Key", Toast.LENGTH_SHORT).show();
            return;
        }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), 0);

        final EditText input = new EditText(this);
        input.setHint("例如：改写成适合背诵的短句；补充适用条件；把公式改成标准形式");
        input.setMinLines(2);
        box.addView(input);

        String[] suggests = {"更易背诵", "补充适用条件", "精简到两句", "加上易错点", "换成公式推导"};
        LinearLayout sugRow = new LinearLayout(this);
        sugRow.setOrientation(LinearLayout.HORIZONTAL);
        sugRow.setPadding(0, Ui.dp(this, 6), 0, 0);
        for (final String s : suggests) {
            TextView chip = Ui.chip(this, s, false);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String old = input.getText().toString().trim();
                    input.setText(old.isEmpty() ? s : old + "；" + s);
                    input.setSelection(input.getText().length());
                }
            });
            sugRow.addView(chip);
        }
        android.widget.HorizontalScrollView sugScroll = new android.widget.HorizontalScrollView(this);
        sugScroll.setHorizontalScrollBarEnabled(false);
        sugScroll.addView(sugRow);
        box.addView(sugScroll);

        final android.widget.CheckBox append = new android.widget.CheckBox(this);
        append.setText("在原内容后面追加（不覆盖）");
        append.setTextSize(13);
        box.addView(append);

        new AlertDialog.Builder(this)
                .setTitle("让 AI 修改")
                .setView(box)
                .setPositiveButton("开始", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        final String req = input.getText().toString().trim();
                        if (req.isEmpty()) return;
                        runAiEdit(req, append.isChecked());
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void runAiEdit(final String req, final boolean appendMode) {
        save(false);
        snapName = nameInput.getText().toString();
        snapContent = contentInput.getText().toString();
        snapFormula = formulaInput.getText().toString();
        final android.app.ProgressDialog dlg = new android.app.ProgressDialog(this);
        dlg.setMessage("AI 正在改写…");
        dlg.setCancelable(false);
        dlg.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Refine.Result r = Refine.edit(prefs, db, note, req, appendMode);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dlg.dismiss();
                        if (r.error != null) {
                            Toast.makeText(NoteActivity.this, r.error, Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (r.name != null && !r.name.trim().isEmpty()) nameInput.setText(r.name.trim());
                        if (r.content != null && !r.content.trim().isEmpty()) {
                            if (appendMode) {
                                String old = contentInput.getText().toString().trim();
                                contentInput.setText(old.isEmpty() ? r.content.trim() : old + "\n\n" + r.content.trim());
                            } else {
                                contentInput.setText(r.content.trim());
                            }
                        }
                        if (r.formula != null && !r.formula.trim().isEmpty()) formulaInput.setText(r.formula.trim());
                        save(false);
                        hasSnap = true;
                        if (undoAiChip != null) undoAiChip.setVisibility(View.VISIBLE);
                        Toast.makeText(NoteActivity.this, "AI 已更新，可继续手改", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /** 复制这条知识点 */
    /** 从已有分类里挑一个，省得手打路径 */
    private void pickCategory() {
        final java.util.List<String> paths = db.categoryPaths(note.subjectId);
        if (paths.isEmpty()) {
            Toast.makeText(this, "这个科目还没有分类，直接在输入框里写，例如 力学/运动学", Toast.LENGTH_LONG).show();
            return;
        }
        paths.add(0, "（不分类）");
        new AlertDialog.Builder(this)
                .setTitle("选择分类")
                .setItems(paths.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        categoryInput.setText(which == 0 ? "" : paths.get(which));
                        save(false);
                    }
                })
                .show();
    }

    private void duplicate() {
        save(false);
        Db.Note copy = db.createNote(note.subjectId);
        copy.name = (note.name == null ? "知识点" : note.name) + "（副本）";
        copy.content = note.content;
        copy.formula = note.formula;
        copy.quote = note.quote;
        copy.tags = note.tags;
        copy.categoryId = note.categoryId;
        copy.importance = note.importance;
        copy.source = note.source;
        copy.slideId = note.slideId;
        copy.slidePath = note.slidePath;
        copy.slidePage = note.slidePage;
        db.saveNote(copy);
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void askAiDraft() {
        if (!prefs.ready()) {
            Toast.makeText(this, "先到设置里填 API Key", Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText input = new EditText(this);
        input.setHint("例如：用一句话解释基尔霍夫电流定律，并给公式");
        new AlertDialog.Builder(this)
                .setTitle("AI 起草")
                .setView(input)
                .setPositiveButton("生成", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String req = input.getText().toString().trim();
                        if (!req.isEmpty()) runAiDraft(req);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void runAiDraft(final String req) {
        final android.app.ProgressDialog dlg = new android.app.ProgressDialog(this);
        dlg.setMessage("AI 正在起草…");
        dlg.setCancelable(false);
        dlg.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Refine.Result r = Refine.draft(prefs, db, note.subjectId, req);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dlg.dismiss();
                        if (r.error != null) {
                            Toast.makeText(NoteActivity.this, r.error, Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (r.name != null && !r.name.trim().isEmpty()) nameInput.setText(r.name.trim());
                        if (r.content != null) contentInput.setText(r.content.trim());
                        if (r.formula != null) formulaInput.setText(r.formula.trim());
                        save(false);
                        Toast.makeText(NoteActivity.this, "已生成，可继续改", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /** 按分组把符号填进一行 */
    private void fillSymbols(LinearLayout row, String chars) {
        row.removeAllViews();
        for (String s : chars.split(" ")) {
            if (s.trim().isEmpty()) continue;
            final String sym = s.trim();
            TextView b = new TextView(this);
            b.setText(sym);
            b.setTextSize(15);
            b.setTextColor(Ui.INK);
            b.setGravity(Gravity.CENTER);
            b.setPadding(Ui.dp(this, 11), Ui.dp(this, 7), Ui.dp(this, 11), Ui.dp(this, 7));
            b.setBackground(Ui.card(this));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = Ui.dp(this, 6);
            b.setLayoutParams(lp);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    insertSymbol(sym);
                }
            });
            row.addView(b);
        }
    }

    private void insertSymbol(String s) {
        EditText target = focused;
        if (target == null) target = contentInput;
        int start = Math.max(0, target.getSelectionStart());
        int end = Math.max(start, target.getSelectionEnd());
        target.getText().replace(start, end, s);
    }

    private void showFigure() {
        if (note.figure == null || note.figure.isEmpty()) {
            figureView.setVisibility(View.GONE);
            return;
        }
        android.graphics.Bitmap bmp = BitmapFactory.decodeFile(note.figure);
        if (bmp == null) {
            figureView.setVisibility(View.GONE);
            return;
        }
        figureView.setImageBitmap(bmp);
        figureView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 2001 || resultCode != RESULT_OK || data == null) return;
        String path = data.getStringExtra("path");
        if (path == null) return;
        try {
            java.io.File src = new java.io.File(path);
            java.io.File dir = new java.io.File(getFilesDir(), "figures");
            if (!dir.exists()) dir.mkdirs();
            java.io.File dst = new java.io.File(dir, note.id + ".png");
            java.io.InputStream is = new java.io.FileInputStream(src);
            java.io.OutputStream os = new java.io.FileOutputStream(dst);
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
            is.close();
            os.close();
            src.delete();
            note.figure = dst.getAbsolutePath();
            db.saveNote(note);
            showFigure();
            Toast.makeText(this, "配图已保存", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "保存配图失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void save(boolean toast) {
        note.name = nameInput.getText().toString().trim();
        if (note.name.isEmpty()) note.name = "未命名";
        note.formula = formulaInput.getText().toString().trim();
        note.content = contentInput.getText().toString();
        if (tagsInput != null) note.tags = tagsInput.getText().toString().trim();
        String cat = categoryInput.getText().toString().trim();
        note.categoryId = cat.isEmpty() ? null : db.ensureCategoryPath(note.subjectId, cat);
        db.saveNote(note);
        if (toast) Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        save(false);
    }
}
