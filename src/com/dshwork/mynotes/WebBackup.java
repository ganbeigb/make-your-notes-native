package com.dshwork.mynotes;

import android.content.Context;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** 导入网页版（Make Your Notes Web）导出的备份 zip：manifest.json + images/。 */
public class WebBackup {

    public static class Result {
        public int subjects;
        public int categories;
        public int notes;
        public int images;
        public String error;
    }

    public static Result importFromZip(Context ctx, Db db, InputStream zipStream, Prefs prefs) {
        Result r = new Result();
        try {
            java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(zipStream);
            Map<String, byte[]> files = new HashMap<>();
            java.util.zip.ZipEntry e;
            byte[] buf = new byte[16384];
            while ((e = zip.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                String name = e.getName();
                if (!"manifest.json".equals(name) && !name.startsWith("images/")) continue;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int n;
                while ((n = zip.read(buf)) > 0) bos.write(buf, 0, n);
                files.put(name, bos.toByteArray());
            }
            zip.close();

            byte[] man = files.get("manifest.json");
            if (man == null) {
                r.error = "zip 里没有 manifest.json，不像是网页版备份";
                return r;
            }
            JSONObject m = new JSONObject(new String(man, "UTF-8"));

            // 1) 科目
            Map<String, String> subjMap = new HashMap<>();
            JSONArray subjects = m.optJSONArray("subjects");
            if (subjects != null) {
                for (int i = 0; i < subjects.length(); i++) {
                    JSONObject s = subjects.optJSONObject(i);
                    if (s == null) continue;
                    String name = s.optString("name", "").trim();
                    if (name.isEmpty()) continue;
                    String id = db.findSubjectIdByName(name);
                    if (id == null) {
                        id = db.addSubject(name).id;
                        r.subjects++;
                    }
                    subjMap.put(s.optString("id"), id);
                }
            }

            // 2) 分类（两遍，先建后回填）
            Map<String, String> catMap = new HashMap<>();
            JSONArray cats = m.optJSONArray("categories");
            java.util.List<JSONObject> pending = new java.util.ArrayList<>();
            if (cats != null) {
                for (int i = 0; i < cats.length(); i++) {
                    JSONObject c = cats.optJSONObject(i);
                    if (c == null) continue;
                    String newSubj = subjMap.get(c.optString("subjectId"));
                    if (newSubj == null) continue;
                    String name = c.optString("name", "").trim();
                    if (name.isEmpty()) continue;
                    String existing = db.findCategoryId(newSubj, null, name);
                    if (existing == null) {
                        existing = db.ensureCategoryPath(newSubj, name);
                        r.categories++;
                    }
                    catMap.put(c.optString("id"), existing);
                    pending.add(c);
                }
                for (JSONObject c : pending) {
                    String oldParent = c.optString("parentId", null);
                    if (oldParent == null || oldParent.isEmpty()) continue;
                    String newParent = catMap.get(oldParent);
                    String self = catMap.get(c.optString("id"));
                    if (newParent == null || self == null) continue;
                    db.reparentCategory(self, newParent);
                }
            }

            // 3) 页面：slideId → 页码 + 图片
            Map<String, Integer> slidePage = new HashMap<>();
            Map<String, String> slideImage = new HashMap<>();
            JSONArray slides = m.optJSONArray("slides");
            if (slides != null) {
                for (int i = 0; i < slides.length(); i++) {
                    JSONObject s = slides.optJSONObject(i);
                    if (s == null) continue;
                    slidePage.put(s.optString("id"), s.optInt("index", i + 1));
                    String img = s.optString("image", "");
                    if (!img.isEmpty()) slideImage.put(s.optString("id"), trimBasename(img));
                }
            }

            // 4) 知识点（items）
            Map<String, String> pageImagePath = new HashMap<>();
            JSONArray items = m.optJSONArray("items");
            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject it = items.optJSONObject(i);
                    if (it == null) continue;
                    String newSubj = subjMap.get(it.optString("subjectId"));
                    if (newSubj == null) continue;
                    String name = it.optString("name", "").trim();
                    if (name.isEmpty()) continue;
                    if (db.noteExistsByTitle(newSubj, name)) continue;

                    String catId = catMap.get(it.optString("categoryId", ""));
                    Db.Note n = db.createNote(newSubj);
                    n.name = name;
                    n.content = it.optString("content", "");
                    n.formula = it.optString("formula", "");
                    n.quote = it.optString("quote", "");
                    n.importance = Math.max(1, Math.min(3, it.optInt("importance", 2)));
                    n.categoryId = catId;
                    JSONArray tags = it.optJSONArray("tags");
                    if (tags != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int k = 0; k < tags.length(); k++) {
                            if (k > 0) sb.append(',');
                            sb.append(tags.optString(k));
                        }
                        n.tags = sb.toString();
                    }
                    String slideId = it.optString("slideId", null);
                    if (slideId != null && slidePage.containsKey(slideId)) {
                        int page = slidePage.get(slideId);
                        n.slidePage = page;
                        n.source = "第 " + page + " 页";
                        String base = slideImage.get(slideId);
                        if (base != null) {
                            String path = pageImagePath.get(base);
                            if (path == null) {
                                byte[] img = files.get("images/" + base);
                                if (img != null) {
                                    path = writeSlide(ctx, base, img);
                                    if (path != null) r.images++;
                                }
                                pageImagePath.put(base, path);
                            }
                            n.slidePath = path;
                        }
                    }
                    db.saveNote(n);
                    r.notes++;
                }
            }

            // 5) 设置（能被识别的项照搬）
            if (prefs != null) prefs.restore(m.optJSONObject("settings"));
        } catch (Exception e) {
            r.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
        return r;
    }

    private static String trimBasename(String p) {
        int i = p.lastIndexOf('/');
        return i >= 0 ? p.substring(i + 1) : p;
    }

    private static String writeSlide(Context ctx, String base, byte[] data) {
        try {
            File dir = new File(ctx.getFilesDir(), "slides");
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, "web_" + System.currentTimeMillis() + "_" + base);
            FileOutputStream fos = new FileOutputStream(out);
            fos.write(data);
            fos.close();
            // 能解码就保留，不能解码就当没图
            if (BitmapFactory.decodeFile(out.getAbsolutePath()) == null) {
                out.delete();
                return null;
            }
            return out.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
}
