package com.dshwork.mynotes;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/** 模型与 Key 设置。 */
public class SettingsActivity extends BaseActivity {

    private Prefs prefs;
    private EditText keyInput;
    private EditText zhipuKeyInput;
    private EditText visionInput;
    private EditText textInput;
    private Spinner providerSpinner;
    private Spinner reasoningSpinner;
    private Spinner scaleSpinner;
    private Spinner linesSpinner;
    private Spinner sortSpinner;
    private Spinner themeSpinner;
    private EditText priceInInput;
    private EditText priceOutInput;
    private android.widget.CheckBox quoteBox;
    private android.widget.CheckBox strictBox;

    private final String[] providers = {"deepseek", "zhipu"};
    private final String[] scales = {"小", "标准", "大", "特大"};
    private final float[] scaleValues = {0.9f, 1f, 1.15f, 1.3f};
    private final String[] lineOptions = {"不显示正文", "1 行", "2 行", "3 行", "4 行", "6 行"};
    private final int[] lineValues = {0, 1, 2, 3, 4, 6};
    private final String[] sorts = {"最近创建", "标题", "重要度"};
    private final String[] themes = {"跟随系统", "浅色", "深色"};
    private final String[] themeKeys = {"system", "light", "dark"};
    private final String[] sortKeys = {"recent", "title", "importance"};
    private final String[] reasonings = {"off", "low", "high", "max"};

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Ui.apply(this);
        prefs = new Prefs(this);

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
        TextView title = Ui.text(this, "设置", 19, Ui.INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView save = Ui.button(this, "保存");
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
        bar.addView(save);
        root.addView(bar);

        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 30));

        providerSpinner = spinner(form, "服务商", providers, indexOf(providers, prefs.provider()));
        keyInput = edit(form, "DeepSeek API Key", prefs.apiKey("deepseek"), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        zhipuKeyInput = edit(form, "智谱 API Key（识图用，免费）", prefs.apiKey("zhipu"), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        visionInput = edit(form, "识图模型", prefs.visionModel(), InputType.TYPE_CLASS_TEXT);
        textInput = edit(form, "文本模型", prefs.textModel(), InputType.TYPE_CLASS_TEXT);
        reasoningSpinner = spinner(form, "推理档位", reasonings, indexOf(reasonings, prefs.reasoning()));

        int si = 1;
        for (int i = 0; i < scaleValues.length; i++) if (Math.abs(scaleValues[i] - prefs.uiScale()) < 0.01f) si = i;
        scaleSpinner = spinner(form, "界面字号", scales, si);

        int li = 3;
        for (int i = 0; i < lineValues.length; i++) if (lineValues[i] == prefs.cardLines()) li = i;
        linesSpinner = spinner(form, "卡片正文行数", lineOptions, li);

        sortSpinner = spinner(form, "知识点排序", sorts, Math.max(0, indexOf(sortKeys, prefs.sort())));

        themeSpinner = spinner(form, "主题", themes, Math.max(0, indexOf(themeKeys, prefs.theme())));

        priceInInput = edit(form, "输入单价（元 / 百万 token，0 = 不估算）",
                prefs.priceIn() > 0 ? String.valueOf(prefs.priceIn()) : "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        priceOutInput = edit(form, "输出单价（元 / 百万 token）",
                prefs.priceOut() > 0 ? String.valueOf(prefs.priceOut()) : "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        strictBox = new android.widget.CheckBox(this);
        strictBox.setText("严格校对：每页识别完再用原图核对一次（会多花一次调用）");
        strictBox.setTextSize(13);
        strictBox.setTextColor(Ui.INK);
        quoteBox = new android.widget.CheckBox(this);
        quoteBox.setText("在知识点详情显示「原文摘录」");
        quoteBox.setTextSize(13);
        quoteBox.setTextColor(Ui.INK);
        quoteBox.setChecked(prefs.showQuote());
        quoteBox.setPadding(0, Ui.dp(this, 10), 0, 0);
        form.addView(quoteBox);

        strictBox.setChecked(prefs.strict());
        strictBox.setPadding(0, Ui.dp(this, 16), 0, 0);
        form.addView(strictBox);

        TextView hint = Ui.text(this,
                "识图会自动用智谱（glm-4.6v-flash 免费），所以哪怕主服务商选 DeepSeek，也建议把智谱 Key 填上。" +
                        "两个 Key 分开保存，互不覆盖。", 12, Ui.INK2);
        hint.setPadding(0, Ui.dp(this, 14), 0, 0);
        form.addView(hint);

        scroll.addView(form);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    private float parseFloat(String s) {
        try {
            return Float.parseFloat(s.trim());
        } catch (Exception e) {
            return 0f;
        }
    }

    private int indexOf(String[] arr, String v) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(v)) return i;
        return 0;
    }

    private EditText edit(LinearLayout parent, String label, String value, int type) {
        TextView l = Ui.text(this, label, 12, Ui.INK2);
        l.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 6));
        parent.addView(l);
        EditText e = new EditText(this);
        e.setText(value == null ? "" : value);
        e.setInputType(type);
        e.setTextSize(15);
        e.setTextColor(Ui.INK);
        e.setBackground(Ui.card(this));
        e.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        parent.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
    }

    private Spinner spinner(LinearLayout parent, String label, String[] values, int selected) {
        TextView l = Ui.text(this, label, 12, Ui.INK2);
        l.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 6));
        parent.addView(l);
        Spinner s = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values);
        s.setAdapter(ad);
        s.setSelection(selected);
        parent.addView(s, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return s;
    }

    private void saveSettings() {
        String provider = providerSpinner.getSelectedItem().toString();
        prefs.setProvider(provider);
        prefs.setApiKey("deepseek", keyInput.getText().toString());
        prefs.setApiKey("zhipu", zhipuKeyInput.getText().toString());
        prefs.setVisionModel(visionInput.getText().toString());
        prefs.setTextModel(textInput.getText().toString());
        prefs.setReasoning(reasoningSpinner.getSelectedItem().toString());
        prefs.setUiScale(scaleValues[scaleSpinner.getSelectedItemPosition()]);
        prefs.setCardLines(lineValues[linesSpinner.getSelectedItemPosition()]);
        prefs.setSort(sortKeys[sortSpinner.getSelectedItemPosition()]);
        prefs.setTheme(themeKeys[themeSpinner.getSelectedItemPosition()]);
        float pi = parseFloat(priceInInput.getText().toString());
        float po = parseFloat(priceOutInput.getText().toString());
        prefs.setPrice(pi, po);
        prefs.setStrict(strictBox.isChecked());
        prefs.setShowQuote(quoteBox.isChecked());
        Ui.apply(this);
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}
