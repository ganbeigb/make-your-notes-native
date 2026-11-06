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

    /** off | low | high | max */
    public String reasoning() {
        return sp.getString("reasoning", "off");
    }

    public void setReasoning(String v) {
        sp.edit().putString("reasoning", v).apply();
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
