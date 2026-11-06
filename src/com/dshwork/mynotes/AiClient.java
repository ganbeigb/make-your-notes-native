package com.dshwork.mynotes;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** 直接调用 OpenAI 兼容接口（DeepSeek / 智谱），不经过任何本地服务。 */
public class AiClient {

    public static class Reply {
        public String content = "";
        public int inTok;
        public int outTok;
    }

    public static Reply chat(String baseUrl, String key, String model, String system, String user,
                             String imageB64, String reasoning) throws IOException {
        boolean withReasoning = reasoning != null && !"off".equals(reasoning);
        try {
            return call(baseUrl, key, model, system, user, imageB64, withReasoning ? reasoning : null);
        } catch (HttpException e) {
            if (withReasoning) {
                // 该模型不支持思考参数时退回普通调用
                return call(baseUrl, key, model, system, user, imageB64, null);
            }
            throw e;
        }
    }

    static class HttpException extends IOException {
        HttpException(String m) {
            super(m);
        }
    }

    private static Reply call(String baseUrl, String key, String model, String system, String user,
                              String imageB64, String reasoning) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("model", model);
            JSONArray msgs = new JSONArray();
            if (system != null && !system.isEmpty()) {
                msgs.put(new JSONObject().put("role", "system").put("content", system));
            }
            JSONObject u = new JSONObject().put("role", "user");
            if (imageB64 != null && !imageB64.isEmpty()) {
                JSONArray parts = new JSONArray();
                parts.put(new JSONObject().put("type", "text").put("text", user));
                parts.put(new JSONObject().put("type", "image_url")
                        .put("image_url", new JSONObject().put("url", "data:image/jpeg;base64," + imageB64)));
                u.put("content", parts);
            } else {
                u.put("content", user);
            }
            msgs.put(u);
            body.put("messages", msgs);
            body.put("temperature", 0.2);
            if (reasoning != null) {
                body.put("thinking", new JSONObject().put("type", "enabled"));
                body.put("reasoning_effort", reasoning);
            }
        } catch (Exception e) {
            throw new IOException("构造请求失败: " + e.getMessage());
        }

        String url = baseUrl.replaceAll("/+$", "") + "/chat/completions";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(180000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Authorization", "Bearer " + key);
        conn.setDoOutput(true);

        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        OutputStream os = conn.getOutputStream();
        os.write(payload);
        os.flush();
        os.close();

        int code = conn.getResponseCode();
        InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        String text = readAll(is);
        conn.disconnect();

        if (code < 200 || code >= 300) {
            String msg = text;
            try {
                JSONObject err = new JSONObject(text);
                if (err.has("error")) msg = err.getJSONObject("error").optString("message", text);
            } catch (Exception ignored) {
            }
            if (msg.length() > 300) msg = msg.substring(0, 300);
            throw new HttpException("HTTP " + code + ": " + msg);
        }

        Reply r = new Reply();
        try {
            JSONObject json = new JSONObject(text);
            JSONObject choice = json.getJSONArray("choices").getJSONObject(0);
            Object content = choice.getJSONObject("message").opt("content");
            if (content instanceof String) {
                r.content = (String) content;
            } else if (content instanceof JSONArray) {
                JSONArray arr = (JSONArray) content;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject part = arr.optJSONObject(i);
                    if (part != null && "text".equals(part.optString("type"))) sb.append(part.optString("text"));
                }
                r.content = sb.toString();
            }
            JSONObject usage = json.optJSONObject("usage");
            if (usage != null) {
                r.inTok = usage.optInt("prompt_tokens", 0);
                r.outTok = usage.optInt("completion_tokens", 0);
            }
        } catch (Exception e) {
            throw new IOException("解析响应失败: " + e.getMessage());
        }
        return r;
    }

    private static String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append('\n');
        br.close();
        return sb.toString();
    }

    /** 把网络/接口异常翻译成用户能看懂的话 */
    public static String friendly(Throwable e) {
        if (e == null) return "未知错误";
        String raw = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        String msg = raw.toLowerCase();
        if (raw.contains("Unable to resolve host") || msg.contains("unknownhost")) {
            return "网络不通：解析不到服务器地址（校园网/代理可能限制了这个域名）";
        }
        if (msg.contains("timeout") || raw.contains("timed out")) {
            return "请求超时：图片多的话可以分几批导入";
        }
        if (msg.contains("401") || msg.contains("unauthorized") || raw.contains("invalid api key")) {
            return "API Key 不对或已失效，去设置里检查一下";
        }
        if (msg.contains("402") || raw.contains("Insufficient Balance") || raw.contains("余额")) {
            return "账户余额不足";
        }
        if (msg.contains("429") || raw.contains("rate limit")) {
            return "请求太频繁，等几秒再试";
        }
        if (raw.contains("Failed to connect") || raw.contains("Connection refused")) {
            return "连不上服务器：检查网络后重试";
        }
        return raw.length() > 160 ? raw.substring(0, 160) : raw;
    }

    /** 去掉模型可能加上的代码围栏，截取最外层 JSON。 */
    public static String pickJson(String s) {
        if (s == null) return "";
        String t = s.trim();
        String fence = String.valueOf((char) 96) + String.valueOf((char) 96) + String.valueOf((char) 96);
        int a1 = t.indexOf(fence);
        if (a1 >= 0) {
            int lineEnd = t.indexOf(10, a1);
            int a2 = t.indexOf(fence, lineEnd + 1);
            if (lineEnd > 0 && a2 > lineEnd) t = t.substring(lineEnd + 1, a2);
        }
        int a = t.indexOf('{');
        int b = t.lastIndexOf('}');
        if (a >= 0 && b > a) return t.substring(a, b + 1);
        int ca = t.indexOf('[');
        int cb = t.lastIndexOf(']');
        if (ca >= 0 && cb > ca) return t.substring(ca, cb + 1);
        return t;
    }
}
