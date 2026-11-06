package com.dshwork.mynotes;

import android.content.Context;
import android.content.SharedPreferences;

/** 设置项：模型、Key、推理档位。 */
public class Prefs {
    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getSharedPreferences("mynotes", Context.MODE_PRIVATE);
    }

    public String provider() {
        return sp.getString("provider", "deepseek");
    }

    public void setProvider(String v) {
        sp.edit().putString("provider", v).apply();
    }

    public String apiKey() {
        return sp.getString("apiKey_" + provider(), "");
    }

    public String apiKey(String provider) {
        return sp.getString("apiKey_" + provider, "");
    }

    public void setApiKey(String provider, String v) {
        sp.edit().putString("apiKey_" + provider, v.trim()).apply();
    }

    public String visionModel() {
        return sp.getString("visionModel", defaultVision(provider()));
    }

    public void setVisionModel(String v) {
        sp.edit().putString("visionModel", v.trim()).apply();
    }

    public String textModel() {
        return sp.getString("textModel", defaultText(provider()));
    }

    public void setTextModel(String v) {
        sp.edit().putString("textModel", v.trim()).apply();
    }

    public boolean keepFormula() {
        return sp.getBoolean("keepFormula", false);
    }

    public void setKeepFormula(boolean v) {
        sp.edit().putBoolean("keepFormula", v).apply();
    }

    public boolean showQuote() {
        return sp.getBoolean("showQuote", true);
    }

    public void setShowQuote(boolean v) {
        sp.edit().putBoolean("showQuote", v).apply();
    }

    /** light | dark | system */
    public String theme() {
        return sp.getString("theme", "system");
    }

    public void setTheme(String v) {
        sp.edit().putString("theme", v).apply();
    }

    /** 每百万 token 的价格（元），0 表示不估算 */
    public float priceIn() {
        return sp.getFloat("priceIn", 0f);
    }

    public float priceOut() {
        return sp.getFloat("priceOut", 0f);
    }

    public void setPrice(float in, float out) {
        sp.edit().putFloat("priceIn", in).putFloat("priceOut", out).apply();
    }

    /** 导出设置快照，用于备份 */
    public org.json.JSONObject snapshot() {
        org.json.JSONObject o = new org.json.JSONObject();
        try {
            o.put("provider", provider());
            o.put("visionModel", visionModel());
            o.put("textModel", textModel());
            o.put("reasoning", reasoning());
            o.put("strict", strict());
            o.put("uiScale", (double) uiScale());
            o.put("cardLines", cardLines());
            o.put("sort", sort());
            o.put("theme", theme());
            o.put("priceIn", (double) priceIn());
            o.put("priceOut", (double) priceOut());
        } catch (Exception ignored) {
        }
        return o;
    }

    /** 从备份还原设置（不动 API Key） */
    public void restore(org.json.JSONObject o) {
        if (o == null) return;
        if (o.has("provider")) setProvider(o.optString("provider", provider()));
        if (o.has("visionModel")) setVisionModel(o.optString("visionModel", visionModel()));
        if (o.has("textModel")) setTextModel(o.optString("textModel", textModel()));
        if (o.has("reasoning")) setReasoning(o.optString("reasoning", reasoning()));
        if (o.has("strict")) setStrict(o.optBoolean("strict", strict()));
        if (o.has("uiScale")) setUiScale((float) o.optDouble("uiScale", uiScale()));
        if (o.has("cardLines")) setCardLines(o.optInt("cardLines", cardLines()));
        if (o.has("sort")) setSort(o.optString("sort", sort()));
        if (o.has("theme")) setTheme(o.optString("theme", theme()));
        if (o.has("priceIn") || o.has("priceOut")) {
            setPrice((float) o.optDouble("priceIn", priceIn()), (float) o.optDouble("priceOut", priceOut()));
        }
    }

    /** off | low | high | max */
    public String reasoning() {
        return sp.getString("reasoning", "off");
    }

    public void setReasoning(String v) {
        sp.edit().putString("reasoning", v).apply();
    }

    /** 界面字号：0.9 小 / 1 标准 / 1.15 大 / 1.3 特大 */
    public float uiScale() {
        return sp.getFloat("uiScale", 1f);
    }

    public void setUiScale(float v) {
        sp.edit().putFloat("uiScale", v).apply();
    }

    /** 卡片正文显示行数，0 = 不显示 */
    public int cardLines() {
        return sp.getInt("cardLines", 3);
    }

    public void setCardLines(int v) {
        sp.edit().putInt("cardLines", v).apply();
    }

    /** recent | title | importance */
    public String sort() {
        return sp.getString("sort", "recent");
    }

    public void setSort(String v) {
        sp.edit().putString("sort", v).apply();
    }

    public boolean strict() {
        return sp.getBoolean("strict", false);
    }

    public void setStrict(boolean v) {
        sp.edit().putBoolean("strict", v).apply();
    }

    public static String defaultVision(String provider) {
        return "zhipu".equals(provider) ? "glm-4.6v-flash" : "glm-4.6v-flash";
    }

    public static String defaultText(String provider) {
        return "zhipu".equals(provider) ? "glm-4.7-flash" : "deepseek-chat";
    }

    public static String baseUrl(String provider) {
        return "zhipu".equals(provider)
                ? "https://open.bigmodel.cn/api/paas/v4"
                : "https://api.deepseek.com";
    }

    public boolean ready() {
        return !apiKey().isEmpty();
    }
}
