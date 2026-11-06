package com.dshwork.mynotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
    private android.widget.CheckBox keepFormulaBox;
    private android.widget.CheckBox mockBox;
    private android.widget.CheckBox autoOrgBox;
    private android.widget.CheckBox strictBox;

    private final String[] providers = {"deepseek", "zhipu"};
    private final String[] scales = {"小", "标准", "大", "特大"};
    private final float[] scaleValues = {0.9f, 1f, 1.15f, 1.3f};
    private final String[] lineOptions = {"不显示正文", "1 行", "2 行", "3 行", "4 行", "6 行", "不截断（显示全文）"};
    private final int[] lineValues = {0, 1, 2, 3, 4, 6, -1};
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
        strictBox.setText("严格校对");
        strictBox.setTextSize(13);
        strictBox.setTextColor(Ui.INK);
        quoteBox = new android.widget.CheckBox(this);
        quoteBox.setText("显示原文摘录");
        quoteBox.setTextSize(13);
        quoteBox.setTextColor(Ui.INK);
        quoteBox.setChecked(prefs.showQuote());

        final android.widget.CheckBox keepFormulaBox = new android.widget.CheckBox(this);
        keepFormulaBox.setText("保留原公式");
        keepFormulaBox.setTextSize(13);
        keepFormulaBox.setTextColor(Ui.INK);
        keepFormulaBox.setChecked(prefs.keepFormula());
        keepFormulaBox.setPadding(0, Ui.dp(this, 8), 0, 0);
        form.addView(Ui.settingRow(this, keepFormulaBox.getText().toString(), null, keepFormulaBox));
        this.keepFormulaBox = keepFormulaBox;

        mockBox = new android.widget.CheckBox(this);
        mockBox.setText("演示模式");
        mockBox.setTextSize(13);
        mockBox.setTextColor(Ui.INK);
        mockBox.setChecked(prefs.mock());
        mockBox.setPadding(0, Ui.dp(this, 10), 0, 0);
        form.addView(Ui.settingRow(this, mockBox.getText().toString(), null, mockBox));

        autoOrgBox = new android.widget.CheckBox(this);
        autoOrgBox.setText("导入后自动整理");
        autoOrgBox.setTextSize(13);
        autoOrgBox.setTextColor(Ui.INK);
        autoOrgBox.setChecked(prefs.autoOrganize());
        autoOrgBox.setPadding(0, Ui.dp(this, 8), 0, 0);
        form.addView(Ui.settingRow(this, autoOrgBox.getText().toString(), null, autoOrgBox));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, Ui.dp(this, 14), 0, 0);

        TextView testBtn = Ui.chip(this, "测试 Key", false);
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testKey();
            }
        });
        actions.addView(testBtn);

        TextView wipeBtn = Ui.chip(this, "清空全部数据", false);
        wipeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("清空全部数据")
                        .setMessage("会删除所有科目、知识点、分类与用量记录，不可恢复。\n建议先导出备份。")
                        .setPositiveButton("清空", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new Db(SettingsActivity.this).clearAll();
                                Toast.makeText(SettingsActivity.this, "已清空", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
        actions.addView(wipeBtn);

        TextView repair = Ui.chip(this, "修复配图", false);
        repair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int fixed = new Db(SettingsActivity.this).repairImages();
                Toast.makeText(SettingsActivity.this, "已清理 " + fixed + " 条失效引用", Toast.LENGTH_SHORT).show();
            }
        });
        actions.addView(repair);
        form.addView(actions);
        this.keepFormulaBox = keepFormulaBox;
        quoteBox.setPadding(0, Ui.dp(this, 10), 0, 0);
        form.addView(Ui.settingRow(this, quoteBox.getText().toString(), null, quoteBox));

        strictBox.setChecked(prefs.strict());
        strictBox.setPadding(0, Ui.dp(this, 16), 0, 0);
        form.addView(Ui.settingRow(this, strictBox.getText().toString(), null, strictBox));

        TextView hint = Ui.text(this,
                "识图会自动用智谱（glm-4.6v-flash 免费），所以哪怕主服务商选 DeepSeek，也建议把智谱 Key 填上。" +
                        "两个 Key 分开保存，互不覆盖。", 12, Ui.INK2);
        hint.setPadding(0, Ui.dp(this, 14), 0, 0);
        form.addView(hint);

        scroll.addView(form);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    private void testKey() {
        final Prefs p = new Prefs(this);
        if (!p.ready()) {
            Toast.makeText(this, "先填 API Key", Toast.LENGTH_SHORT).show();
            return;
        }
        final android.app.ProgressDialog dlg = new android.app.ProgressDialog(this);
        dlg.setMessage("正在连接 " + p.provider() + " …");
        dlg.setCancelable(false);
        dlg.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                long t0 = System.currentTimeMillis();
                String msg;
                try {
                    AiClient.Reply r = AiClient.chat(Prefs.baseUrl(p.provider()), p.apiKey(), p.textModel(),
                            "只回复 OK", "ping", null, "off");
                    long ms = System.currentTimeMillis() - t0;
                    msg = "连接正常（" + ms + " ms）\n模型：" + p.textModel() + "\n回复：" + r.content.trim();
                } catch (Exception e) {
                    msg = "失败：" + AiClient.friendly(e);
                }
                final String finalMsg = msg;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dlg.dismiss();
                        new AlertDialog.Builder(SettingsActivity.this)
                                .setTitle("测试结果")
                                .setMessage(finalMsg)
                                .setPositiveButton("好", null)
                                .show();
                    }
                });
            }
        }).start();
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
        EditText e = new EditText(this);
        e.setText(value == null ? "" : value);
        e.setInputType(type);
        e.setTextSize(13.5f);
        e.setTextColor(Ui.INK);
        e.setBackground(Ui.card(this));
        e.setPadding(Ui.dp(this, 11), Ui.dp(this, 9), Ui.dp(this, 11), Ui.dp(this, 9));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 170), ViewGroup.LayoutParams.WRAP_CONTENT);
        e.setLayoutParams(lp);
        parent.addView(Ui.settingRow(this, label, null, e));
        return e;
    }

    private Spinner spinner(LinearLayout parent, String label, String[] values, int selected) {
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values);
        sp.setAdapter(ad);
        sp.setSelection(selected);
        sp.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this, 170), ViewGroup.LayoutParams.WRAP_CONTENT));
        parent.addView(Ui.settingRow(this, label, null, sp));
        return sp;
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
        if (keepFormulaBox != null) prefs.setKeepFormula(keepFormulaBox.isChecked());
        if (mockBox != null) prefs.setMock(mockBox.isChecked());
        if (autoOrgBox != null) prefs.setAutoOrganize(autoOrgBox.isChecked());
        Ui.apply(this);
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}
