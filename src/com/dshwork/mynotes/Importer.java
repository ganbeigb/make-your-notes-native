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
    }

    public static Outcome run(Context ctx, Db db, Prefs prefs, String subjectId,
                              List<Uri> uris, Progress cb) {
        Outcome out = new Outcome();
        out.pages = uris.size();
        if (!prefs.ready()) {
            out.errors.add("还没有填 API Key，去设置里填一下");
            return out;
        }

        String base = Prefs.baseUrl(prefs.provider());
        String key = prefs.apiKey();
        String model = prefs.visionModel();
        String reasoning = prefs.reasoning();

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

                int added = insertPoints(db, subjectId, json, "第 " + (i + 1) + " 页");
                out.points += added;
            } catch (Exception e) {
                String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                out.errors.add("第 " + (i + 1) + " 页失败：" + msg);
            }
        }
        if (cb != null) cb.onStep(uris.size(), uris.size(), "完成");
        return out;
    }

    private static int insertPoints(Db db, String subjectId, String content, String source) throws Exception {
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
            n.source = source;
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
