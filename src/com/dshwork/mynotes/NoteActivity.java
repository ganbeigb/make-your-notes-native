package com.dshwork.mynotes;

import android.app.Activity;
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
public class NoteActivity extends Activity {

    private Db db;
    private Db.Note note;
    private EditText nameInput;
    private EditText categoryInput;
    private EditText formulaInput;
    private EditText contentInput;
    private FormulaView preview;
    private ImageView figureView;
    private TextView figureBtn;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        db = new Db(this);
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
        bar.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 10));
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

        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 24));

        nameInput = field(form, "标题", note.name, InputType.TYPE_CLASS_TEXT, 1);
        categoryInput = field(form, "分类（用 / 分层，例如 力学/运动学）",
                note.categoryPath == null ? "" : note.categoryPath, InputType.TYPE_CLASS_TEXT, 1);

        formulaInput = field(form, "公式（LaTeX）", note.formula == null ? "" : note.formula, InputType.TYPE_CLASS_TEXT, 1);
        preview = new FormulaView(this);
        preview.setBackground(Ui.card(this));
        preview.setTex(note.formula == null ? "" : note.formula);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.topMargin = Ui.dp(this, 6);
        form.addView(preview, plp);

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

        contentInput = field(form, "正文", note.content == null ? "" : note.content,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 8);

        figureBtn = Ui.text(this, note.figure == null ? "＋ 画配图" : "重新画配图", 13, Ui.ACCENT);
        figureBtn.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 8));
        figureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(NoteActivity.this, BoardActivity.class), 2001);
            }
        });
        form.addView(figureBtn);

        figureView = new ImageView(this);
        figureView.setAdjustViewBounds(true);
        figureView.setBackground(Ui.card(this));
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.topMargin = Ui.dp(this, 6);
        form.addView(figureView, flp);
        showFigure();

        if (note.source != null && !note.source.isEmpty()) {
            TextView src = Ui.text(this, "来源：" + note.source, 12, Ui.INK2);
            src.setPadding(0, Ui.dp(this, 10), 0, 0);
            form.addView(src);
        }

        scroll.addView(form);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    private EditText field(LinearLayout parent, String label, String value, int inputType, int lines) {
        TextView l = Ui.text(this, label, 12, Ui.INK2);
        l.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 6));
        parent.addView(l);

        EditText e = new EditText(this);
        e.setText(value);
        e.setInputType(inputType);
        e.setMinLines(lines);
        e.setTextSize(15);
        e.setTextColor(Ui.INK);
        e.setBackground(Ui.card(this));
        e.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        parent.addView(e, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
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
