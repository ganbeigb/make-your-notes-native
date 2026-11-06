package com.dshwork.mynotes;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** 知识点详情 / 编辑。 */
public class NoteActivity extends Activity {

    private Db db;
    private Db.Note note;
    private EditText nameInput;
    private EditText categoryInput;
    private EditText formulaInput;
    private EditText contentInput;

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
        categoryInput = field(form, "分类", note.category == null ? "" : note.category, InputType.TYPE_CLASS_TEXT, 1);
        formulaInput = field(form, "公式（LaTeX，例如 E=mc^2）", note.formula == null ? "" : note.formula, InputType.TYPE_CLASS_TEXT, 1);
        contentInput = field(form, "正文", note.content == null ? "" : note.content,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 8);

        TextView hint = Ui.text(this, "当前版本只编辑纯文本；公式渲染、AI 提取与画板在后续里程碑接入。", 12, Ui.INK2);
        hint.setPadding(0, Ui.dp(this, 12), 0, 0);
        form.addView(hint);

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

    private void save(boolean toast) {
        note.name = nameInput.getText().toString().trim();
        if (note.name.isEmpty()) note.name = "未命名";
        note.category = categoryInput.getText().toString().trim();
        note.formula = formulaInput.getText().toString().trim();
        note.content = contentInput.getText().toString();
        db.saveNote(note);
        if (toast) Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        save(false);
    }
}
