package com.dshwork.mynotes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** 课件截图 → AI → 知识点入库。 */
public class Importer {

    public interface Progress {
        void onStep(int done, int total, String message);
    }

    public static class Outcome {
        public int pages;
        public int points;
        public final List<String> errors = new ArrayList<>();
        public final List<Uri> failed = new ArrayList<>();
    }

    public static Outcome run(Context ctx, Db db, Prefs prefs, String subjectId,
                              List<Uri> uris, Progress cb) {
        Outcome out = new Outcome();
        out.pages = uris.size();
        if (!prefs.ready()) {
            out.errors.add("还没有填 API Key，去设置里填一下");
            return out;
        }

        // 识图必须用支持图片的模型：优先智谱（免费），没有智谱 Key 才用当前服务商
        boolean hasZhipuKey = prefs.apiKey("zhipu") != null && !prefs.apiKey("zhipu").isEmpty();
        String vProvider = hasZhipuKey ? "zhipu" : prefs.provider();
        String base = Prefs.baseUrl(vProvider);
        String key = prefs.apiKey(vProvider);
        String model = prefs.visionModel();
        String reasoning = prefs.reasoning();

        if (!hasZhipuKey && !"zhipu".equals(prefs.provider())) {
            out.errors.add("识图需要视觉模型：请到设置里填「智谱 Key」（免费），或把服务商切成 zhipu");
            return out;
        }

        for (int i = 0; i < uris.size(); i++) {
            Uri uri = uris.get(i);
            if (cb != null) cb.onStep(i, uris.size(), "正在识别第 " + (i + 1) + " / " + uris.size() + " 页");
            try {
                String b64 = toBase64(ctx, uri);
                if (b64 == null) {
                    out.errors.add("第 " + (i + 1) + " 页读取失败");
                    continue;
                }
                AiClient.Reply reply = AiClient.chat(base, key, model,
                        Prompts.EXTRACT_SYSTEM, Prompts.EXTRACT_USER, b64, reasoning);
                db.logUsage(subjectId, "extract", reply.inTok, reply.outTok);
                String json = reply.content;

                if (prefs.strict()) {
                    if (cb != null) cb.onStep(i, uris.size(), "正在核对第 " + (i + 1) + " / " + uris.size() + " 页");
                    try {
                        AiClient.Reply check = AiClient.chat(base, key, model,
                                Prompts.VERIFY_SYSTEM,
                                AiClient.pickJson(json),
                                b64, "off");
                        db.logUsage(subjectId, "verify", check.inTok, check.outTok);
                        String fixed = AiClient.pickJson(check.content);
                        if (fixed.startsWith("{")) json = fixed;
                    } catch (Exception ignored) {
                        // 校对失败就保留原始结果
                    }
                }

                String saved = saveSlide(ctx, uri, i + 1);
                int added = insertPoints(db, subjectId, json, "第 " + (i + 1) + " 页", saved, i + 1);
                out.points += added;

                if (added >= 2) {
                    try {
                        String digest = summarize(db, prefs, subjectId, json);
                        if (digest != null && !digest.trim().isEmpty()) {
                            db.setPageDigest(subjectId, i + 1, digest.trim());
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                out.errors.add("第 " + (i + 1) + " 页失败：" + AiClient.friendly(e));
                out.failed.add(uri);
            }
        }
        if (cb != null) cb.onStep(uris.size(), uris.size(), "完成");
        return out;
    }

    /** 把这一页压缩后存到私有目录，供「查看原图」用 */
    private static String saveSlide(Context ctx, Uri uri, int page) {
        try {
            InputStream is = ctx.getContentResolver().openInputStream(uri);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, bounds);
            if (is != null) is.close();

            int sample = 1;
            int longer = Math.max(bounds.outWidth, bounds.outHeight);
            while (longer / sample > 1200) sample *= 2;

            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = sample;
            InputStream is2 = ctx.getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(is2, null, opt);
            if (is2 != null) is2.close();
            if (bmp == null) return null;

            java.io.File dir = new java.io.File(ctx.getFilesDir(), "slides");
            if (!dir.exists()) dir.mkdirs();
            java.io.File out = new java.io.File(dir, "p" + page + "_" + System.currentTimeMillis() + ".jpg");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
            bmp.compress(Bitmap.CompressFormat.JPEG, 78, fos);
            fos.close();
            bmp.recycle();
            return out.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    /** 让文本模型给这一页写一句汇总 */
    private static String summarize(Db db, Prefs prefs, String subjectId, String json) throws Exception {
        org.json.JSONObject obj = new org.json.JSONObject(AiClient.pickJson(json));
        org.json.JSONArray points = obj.optJSONArray("points");
        if (points == null || points.length() < 2) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < points.length(); i++) {
            org.json.JSONObject p = points.optJSONObject(i);
            if (p == null) continue;
            sb.append("- ").append(p.optString("name", ""));
            String f = p.optString("formula", "");
            if (!f.trim().isEmpty()) sb.append("（公式：").append(f).append("）");
            sb.append('\n');
        }
        AiClient.Reply reply = AiClient.chat(
                Prefs.baseUrl(prefs.provider()),
                prefs.apiKey(),
                prefs.textModel(),
                Prompts.DIGEST_SYSTEM,
                sb.toString(),
                null,
                "off");
        db.logUsage(subjectId, "digest", reply.inTok, reply.outTok);
        return reply.content;
    }

    private static int insertPoints(Db db, String subjectId, String content, String source,
                                    String slidePath, int page) throws Exception {
        String json = AiClient.pickJson(content);
        JSONObject obj = new JSONObject(json);
        JSONArray points = obj.optJSONArray("points");
        if (points == null) return 0;
        int added = 0;
        for (int i = 0; i < points.length(); i++) {
            JSONObject p = points.optJSONObject(i);
            if (p == null) continue;
            String name = p.optString("name", "").trim();
            if (name.isEmpty()) continue;
            Db.Note n = db.createNote(subjectId);
            n.name = name;
            n.content = p.optString("content", "");
            n.formula = p.optString("formula", "");
            n.importance = Math.max(1, Math.min(3, p.optInt("importance", 2)));
            n.quote = p.optString("quote", "");
            n.source = source;
            n.slidePath = slidePath;
            n.slidePage = page;
            String cat = p.optString("category", "");
            if (!cat.trim().isEmpty()) n.categoryId = db.ensureCategoryPath(subjectId, cat);
            JSONArray tags = p.optJSONArray("tags");
            if (tags != null) {
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < tags.length(); k++) {
                    if (k > 0) sb.append(',');
                    sb.append(tags.optString(k));
                }
                n.tags = sb.toString();
            }
            db.saveNote(n);
            added++;
        }
        return added;
    }

    private static String toBase64(Context ctx, Uri uri) {
        try {
            InputStream is = ctx.getContentResolver().openInputStream(uri);
            if (is == null) return null;
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, bounds);
            is.close();

            int sample = 1;
            int max = 1400;
            int longer = Math.max(bounds.outWidth, bounds.outHeight);
            while (longer / sample > max) sample *= 2;

            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = sample;
            InputStream is2 = ctx.getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(is2, null, opt);
            if (is2 != null) is2.close();
            if (bmp == null) return null;

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 82, bos);
            bmp.recycle();
            return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }
}
