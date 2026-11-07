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

        boolean cancelled();
    }

    public static class Outcome {
        public int pages;
        public int points;
        public int skipped;
        public final List<String> errors = new ArrayList<>();
        public final List<Uri> failed = new ArrayList<>();
        public final List<Integer> failedPages = new ArrayList<>();
        public String deckId;
    }

    private static String mockJson(int page) {
        return "{\"points\":[{\"name\":\"演示知识点 A（第 " + page + " 页）\","
                + "\"content\":\"这是演示模式下生成的示例内容，不会调用任何接口。\","
                + "\"formula\":\"E=mc^2\",\"quote\":\"演示原文\","
                + "\"category\":\"演示/示例\",\"importance\":2}]}";
    }

    public static Outcome run(Context ctx, Db db, Prefs prefs, String subjectId,
                              List<Uri> uris, Progress cb) {
        return run(ctx, db, prefs, subjectId, uris, null, null, cb);
    }

    /** 重试用：复用已有课件，并指定每张图原来的页码 */
    public static Outcome run(Context ctx, Db db, Prefs prefs, String subjectId,
                              List<Uri> uris, List<Integer> pageNos, String existingDeckId,
                              Progress cb) {
        Outcome out = new Outcome();
        out.pages = uris.size();
        if (!prefs.ready()) {
            out.errors.add("还没有填 API Key，去设置里填一下");
            return out;
        }

        // 只用 DeepSeek 一家：识图模型需要用户自己在设置里指定
        String vProvider = prefs.provider();
        String base = Prefs.baseUrl(vProvider);
        String key = prefs.apiKey(vProvider);
        String model = prefs.visionModel();
        String reasoning = prefs.reasoning();

        if (prefs.visionModel() == null || prefs.visionModel().trim().isEmpty()) {
            out.errors.add("还没有填「识图模型」：导入课件要读图片，请到设置里填一个你的 Key 能调用的图片模型名" +
                    "（只做文字整理时不需要导入）。");
            return out;
        }

        Db.Deck deck;
        if (existingDeckId != null && !existingDeckId.isEmpty()) {
            java.util.List<Db.Deck> decks = db.decks(subjectId);
            Db.Deck found = null;
            for (Db.Deck d : decks) {
                if (existingDeckId.equals(d.id)) {
                    found = d;
                    break;
                }
            }
            deck = found != null ? found
                    : db.addDeck(subjectId, "课件 " + new java.text.SimpleDateFormat("MM-dd HH:mm",
                    java.util.Locale.CHINA).format(new java.util.Date()));
        } else {
            deck = db.addDeck(subjectId, "课件 " + new java.text.SimpleDateFormat("MM-dd HH:mm",
                    java.util.Locale.CHINA).format(new java.util.Date()));
        }
        out.deckId = deck.id;

        for (int i = 0; i < uris.size(); i++) {
            if (cb != null && cb.cancelled()) break;
            Uri uri = uris.get(i);
            final int pageNo = (pageNos != null && i < pageNos.size() && pageNos.get(i) != null)
                    ? pageNos.get(i) : (i + 1);
            if (cb != null) cb.onStep(i, uris.size(), "正在识别第 " + (i + 1) + " / " + uris.size() + " 页");
            try {
                if (prefs.mock()) {
                    String json = mockJson(pageNo);
                    String savedMock = saveSlide(ctx, uri, pageNo);
                    Db.Slide mockSlide = db.addSlide(subjectId, deck.id, pageNo, savedMock);
                    out.points += insertPoints(db, subjectId, json, "第 " + pageNo + " 页",
                            savedMock, pageNo, mockSlide.id);
                    if (cb != null) cb.onStep(i + 1, uris.size(), "演示模式：第 " + pageNo + " 页");
                    continue;
                }
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

                String saved = saveSlide(ctx, uri, pageNo);
                String hash = sha1OfFile(saved);
                if (db.slideHashExists(subjectId, hash)) {
                    // 同一个科目里已经有这一页（图片完全相同），跳过不重复建
                    java.io.File dupFile = new java.io.File(saved);
                    if (dupFile.exists()) dupFile.delete();
                    out.skipped++;
                    if (cb != null) cb.onStep(i + 1, uris.size(), "第 " + pageNo + " 页已存在，跳过");
                    continue;
                }
                if (db.slideIndexExists(deck.id, pageNo)) {
                    java.io.File dup2 = new java.io.File(saved);
                    if (dup2.exists()) dup2.delete();
                    out.skipped++;
                    if (cb != null) cb.onStep(i + 1, uris.size(), "第 " + pageNo + " 页已存在，跳过");
                    continue;
                }
                Db.Slide slide = db.addSlide(subjectId, deck.id, pageNo, saved, hash);
                int added = insertPoints(db, subjectId, json, "第 " + pageNo + " 页", saved, pageNo, slide.id);
                out.points += added;

                if (added >= 2) {
                    try {
                        String digest = summarize(db, prefs, subjectId, json);
                        if (digest != null && !digest.trim().isEmpty()) {
                            db.setPageDigest(subjectId, pageNo, digest.trim());
                            db.setSlideDigest(slide.id, digest.trim());
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                out.errors.add("第 " + pageNo + " 页失败：" + AiClient.friendly(e));
                out.failed.add(uri);
                    out.failedPages.add(pageNo);
            }
        }
        if (cb != null) cb.onStep(uris.size(), uris.size(), "完成");
        // 取消 / 全部失败 / 全部重复时，不留下空课件
        db.deleteDeckIfEmpty(deck.id);
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
            java.io.File out = new java.io.File(dir,
                    "p" + page + "_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000) + ".jpg");
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
                                    String slidePath, int page, String slideId) throws Exception {
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
            n.slideId = slideId;
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

    /** 整份课件的要点总结 */
    public static String summarizeDeck(Db db, Prefs prefs, String subjectId, Db.Deck deck) throws Exception {
        java.util.List<Db.Note> list = db.notes(subjectId);
        StringBuilder sb = new StringBuilder();
        int count = 0;
        java.util.Set<String> slideIds = new java.util.HashSet<>();
        for (Db.Slide s : db.slides(deck.id)) slideIds.add(s.id);
        for (Db.Note n : list) {
            if (n.slideId == null || !slideIds.contains(n.slideId)) continue;
            sb.append("- ").append(n.name);
            if (n.formula != null && !n.formula.trim().isEmpty()) sb.append("（").append(n.formula).append("）");
            sb.append('\n');
            if (++count >= 400) break;
        }
        if (count == 0) throw new Exception("这份课件还没有知识点");
        AiClient.Reply reply = AiClient.chat(Prefs.baseUrl(prefs.provider()), prefs.apiKey(),
                prefs.textModel(), Prompts.DECK_SYSTEM, sb.toString(), null, "off");
        db.logUsage(subjectId, "digest", reply.inTok, reply.outTok);
        String summary = reply.content == null ? "" : reply.content.trim();
        db.setDeckSummary(deck.id, summary);
        return summary;
    }

    /** 从已有的课件原图重跑一页，返回识别出的 JSON 文本 */
    public static String extractFile(Prefs prefs, String imagePath) throws Exception {
        java.io.File f = new java.io.File(imagePath);
        if (!f.exists()) throw new Exception("这一页的原图不在了");
        String provider = prefs.provider();
        String b64 = toBase64File(imagePath);
        AiClient.Reply reply = AiClient.chat(Prefs.baseUrl(provider), prefs.apiKey(provider),
                prefs.visionModel(), Prompts.EXTRACT_SYSTEM,
                "请识别这一页课件，输出 JSON。", b64, prefs.reasoning());
        return reply.content;
    }

    /** 用新识别结果替换某一页的知识点，返回新增条数 */
    public static int replaceSlide(Context ctx, Db db, Prefs prefs, String subjectId, Db.Slide slide,
                                   String json) throws Exception {
        for (Db.Note old : db.notesBySlide(slide.id)) db.deleteNote(old.id);
        String saved = slide.imagePath;
        return insertPoints(db, subjectId, json, "第 " + slide.idx + " 页", saved, slide.idx, slide.id);
    }

    /** 整册重做：逐页重新识别并替换该页知识点（对应网页 extract-deck） */
    public static int[] redoDeck(Context ctx, Db db, Prefs prefs, String subjectId, Db.Deck deck,
                                 Progress cb) throws Exception {
        java.util.List<Db.Slide> slides = db.slides(deck.id);
        int done = 0, failed = 0;
        for (int idx = 0; idx < slides.size(); idx++) {
            if (cb != null && cb.cancelled()) break;
            Db.Slide s = slides.get(idx);
            if (s.imagePath == null || !new java.io.File(s.imagePath).exists()) {
                failed++;
                continue;
            }
            if (cb != null) cb.onStep(idx + 1, slides.size(), "正在重做第 " + s.idx + " 页…");
            try {
                String json = extractFile(prefs, s.imagePath);
                replaceSlide(ctx, db, prefs, subjectId, s, json);
                done++;
            } catch (Exception e) {
                failed++;
            }
        }
        return new int[]{done, failed};
    }

    /** 给某一页生成汇总 */
    public static String digestSlide(Db db, Prefs prefs, String subjectId, Db.Slide slide) throws Exception {
        java.util.List<Db.Note> list = db.notesBySlide(slide.id);
        if (list.isEmpty()) throw new Exception("这一页还没有知识点");
        StringBuilder sb = new StringBuilder();
        for (Db.Note n : list) {
            sb.append("- ").append(n.name);
            if (n.formula != null && !n.formula.trim().isEmpty()) sb.append("（公式：").append(n.formula).append("）");
            sb.append('\n');
        }
        AiClient.Reply reply = AiClient.chat(Prefs.baseUrl(prefs.provider()), prefs.apiKey(),
                prefs.textModel(), Prompts.DIGEST_SYSTEM, sb.toString(), null, "off");
        db.logUsage(subjectId, "digest", reply.inTok, reply.outTok);
        String digest = reply.content == null ? "" : reply.content.trim();
        db.setSlideDigest(slide.id, digest);
        return digest;
    }

    private static String sha1OfFile(String path) {
        try {
            java.io.File f = new java.io.File(path);
            if (!f.exists()) return "";
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            in.close();
            StringBuilder sb = new StringBuilder();
            for (byte bt : md.digest()) sb.append(String.format("%02x", bt));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String toBase64File(String path) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        int max = 1400;
        int scale = 1;
        while (bounds.outWidth / scale > max || bounds.outHeight / scale > max) scale *= 2;
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = Math.max(1, scale);
        Bitmap bmp = BitmapFactory.decodeFile(path, opt);
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 82, bos);
        bmp.recycle();
        return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
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
