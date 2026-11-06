package com.dshwork.mynotes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 本地存储：科目 / 分类 / 知识点 / 用量。 */
public class Db extends SQLiteOpenHelper {

    public static class Subject {
        public String id;
        public String name;
        public long createdAt;
        public int noteCount;
    }

    public static class Category {
        public String id;
        public String subjectId;
        public String parentId;
        public String name;
        public int orderNo;
        public int depth;
        public String path;
    }

    public static class Note {
        public String id;
        public String subjectId;
        public String categoryId;
        public String categoryPath;
        public String name;
        public String formula;
        public String content;
        public String tags;
        public String source;
        public int importance;
        public String figure;
        public String quote;
        public String slidePath;
        public int slidePage;
        public long createdAt;
    }

    private static final String FILE = "mynotes.db";
    private static final int VERSION = 7;

    public Db(Context ctx) {
        super(ctx, FILE, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE subjects (id TEXT PRIMARY KEY, name TEXT NOT NULL, created_at INTEGER NOT NULL, criteria TEXT)");
        db.execSQL("CREATE TABLE categories (id TEXT PRIMARY KEY, subject_id TEXT NOT NULL, parent_id TEXT, " +
                "name TEXT NOT NULL, order_no INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE notes (id TEXT PRIMARY KEY, subject_id TEXT NOT NULL, category_id TEXT, " +
                "name TEXT NOT NULL, content TEXT, formula TEXT, tags TEXT, source TEXT, figure TEXT, " +
                "quote TEXT, slide_path TEXT, slide_page INTEGER NOT NULL DEFAULT 0, " +
                "importance INTEGER NOT NULL DEFAULT 2, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE usage_log (id TEXT PRIMARY KEY, ts INTEGER NOT NULL, kind TEXT, " +
                "subject_id TEXT, in_tok INTEGER NOT NULL DEFAULT 0, out_tok INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE page_digest (subject_id TEXT NOT NULL, page INTEGER NOT NULL, " +
                "text TEXT, updated_at INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(subject_id, page))");
        db.execSQL("CREATE INDEX idx_notes_subject ON notes(subject_id)");
        db.execSQL("CREATE INDEX idx_cat_subject ON categories(subject_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 7) {
            db.execSQL("CREATE TABLE IF NOT EXISTS page_digest (subject_id TEXT NOT NULL, page INTEGER NOT NULL, " +
                    "text TEXT, updated_at INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(subject_id, page))");
        }
        if (oldVersion < 6) {
            tryAdd(db, "notes", "quote", "TEXT");
        }
        if (oldVersion < 5) {
            tryAdd(db, "subjects", "criteria", "TEXT");
        }
        if (oldVersion < 4) {
            tryAdd(db, "notes", "slide_path", "TEXT");
            tryAdd(db, "notes", "slide_page", "INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 3) {
            tryAdd(db, "notes", "figure", "TEXT");
        }
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS categories (id TEXT PRIMARY KEY, subject_id TEXT NOT NULL, " +
                    "parent_id TEXT, name TEXT NOT NULL, order_no INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS usage_log (id TEXT PRIMARY KEY, ts INTEGER NOT NULL, kind TEXT, " +
                    "subject_id TEXT, in_tok INTEGER NOT NULL DEFAULT 0, out_tok INTEGER NOT NULL DEFAULT 0)");
            tryAdd(db, "notes", "category_id", "TEXT");
            tryAdd(db, "notes", "tags", "TEXT");
            tryAdd(db, "notes", "source", "TEXT");
            tryAdd(db, "notes", "importance", "INTEGER NOT NULL DEFAULT 2");
        }
    }

    private void tryAdd(SQLiteDatabase db, String table, String col, String type) {
        try {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + col + " " + type);
        } catch (Exception ignored) {
        }
    }

    /* ---------- subjects ---------- */

    public List<Subject> subjects() {
        List<Subject> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT s.id, s.name, s.created_at, (SELECT COUNT(*) FROM notes n WHERE n.subject_id = s.id) " +
                        "FROM subjects s ORDER BY s.created_at ASC", null);
        try {
            while (c.moveToNext()) {
                Subject s = new Subject();
                s.id = c.getString(0);
                s.name = c.getString(1);
                s.createdAt = c.getLong(2);
                s.noteCount = c.getInt(3);
                out.add(s);
            }
        } finally {
            c.close();
        }
        return out;
    }

    public Subject addSubject(String name) {
        Subject s = new Subject();
        s.id = UUID.randomUUID().toString();
        s.name = name.trim();
        s.createdAt = System.currentTimeMillis();
        ContentValues v = new ContentValues();
        v.put("id", s.id);
        v.put("name", s.name);
        v.put("created_at", s.createdAt);
        getWritableDatabase().insert("subjects", null, v);
        return s;
    }

    public void deleteSubject(String id) {
        getWritableDatabase().delete("notes", "subject_id = ?", new String[]{id});
        getWritableDatabase().delete("categories", "subject_id = ?", new String[]{id});
        getWritableDatabase().delete("subjects", "id = ?", new String[]{id});
    }

    public String subjectCriteria(String subjectId) {
        Cursor c = getReadableDatabase().rawQuery("SELECT criteria FROM subjects WHERE id = ?", new String[]{subjectId});
        try {
            return c.moveToFirst() ? c.getString(0) : null;
        } finally {
            c.close();
        }
    }

    public void setPageDigest(String subjectId, int page, String text) {
        ContentValues v = new ContentValues();
        v.put("subject_id", subjectId);
        v.put("page", page);
        v.put("text", text);
        v.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("page_digest", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String pageDigest(String subjectId, int page) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT text FROM page_digest WHERE subject_id = ? AND page = ?",
                new String[]{subjectId, String.valueOf(page)});
        try {
            return c.moveToFirst() ? c.getString(0) : null;
        } finally {
            c.close();
        }
    }

    public void setSubjectCriteria(String subjectId, String text) {
        ContentValues v = new ContentValues();
        v.put("criteria", text == null ? "" : text.trim());
        getWritableDatabase().update("subjects", v, "id = ?", new String[]{subjectId});
    }

    public Subject subject(String id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT id, name, created_at FROM subjects WHERE id = ?", new String[]{id});
        try {
            if (!c.moveToFirst()) return null;
            Subject s = new Subject();
            s.id = c.getString(0);
            s.name = c.getString(1);
            s.createdAt = c.getLong(2);
            return s;
        } finally {
            c.close();
        }
    }

    /* ---------- categories ---------- */

    public List<Category> categories(String subjectId) {
        List<Category> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, subject_id, parent_id, name, order_no FROM categories WHERE subject_id = ? " +
                        "ORDER BY order_no ASC, created_at ASC", new String[]{subjectId});
        try {
            while (c.moveToNext()) out.add(readCategory(c));
        } finally {
            c.close();
        }
        return out;
    }

    private Category readCategory(Cursor c) {
        Category k = new Category();
        k.id = c.getString(0);
        k.subjectId = c.getString(1);
        k.parentId = c.getString(2);
        k.name = c.getString(3);
        k.orderNo = c.getInt(4);
        return k;
    }

    /** 按 "一级/二级" 找到或创建分类，返回叶子 id；空路径返回 null。 */
    public String ensureCategoryPath(String subjectId, String path) {
        if (path == null) return null;
        String[] parts = path.split("/");
        String parent = null;
        String leafId = null;
        for (String raw : parts) {
            String name = raw.trim();
            if (name.isEmpty()) continue;
            String found = findCategory(subjectId, parent, name);
            if (found == null) {
                Category k = new Category();
                k.id = UUID.randomUUID().toString();
                k.subjectId = subjectId;
                k.parentId = parent;
                k.name = name;
                k.orderNo = nextOrder(subjectId, parent);
                ContentValues v = new ContentValues();
                v.put("id", k.id);
                v.put("subject_id", subjectId);
                v.put("parent_id", parent);
                v.put("name", name);
                v.put("order_no", k.orderNo);
                v.put("created_at", System.currentTimeMillis());
                getWritableDatabase().insert("categories", null, v);
                found = k.id;
            }
            leafId = found;
            parent = found;
        }
        return leafId;
    }

    private int nextOrder(String subjectId, String parent) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM categories WHERE subject_id = ? AND " +
                        (parent == null ? "parent_id IS NULL" : "parent_id = ?"),
                parent == null ? new String[]{subjectId} : new String[]{subjectId, parent});
        try {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            c.close();
        }
    }

    private String findCategory(String subjectId, String parent, String name) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id FROM categories WHERE subject_id = ? AND name = ? AND " +
                        (parent == null ? "parent_id IS NULL" : "parent_id = ?"),
                parent == null ? new String[]{subjectId, name} : new String[]{subjectId, name, parent});
        try {
            return c.moveToFirst() ? c.getString(0) : null;
        } finally {
            c.close();
        }
    }

    public String categoryPath(String categoryId) {
        if (categoryId == null) return "";
        String path = "";
        String cur = categoryId;
        int guard = 0;
        while (cur != null && guard++ < 12) {
            Cursor c = getReadableDatabase().rawQuery("SELECT name, parent_id FROM categories WHERE id = ?", new String[]{cur});
            try {
                if (!c.moveToFirst()) break;
                path = path.isEmpty() ? c.getString(0) : c.getString(0) + " / " + path;
                cur = c.getString(1);
            } finally {
                c.close();
            }
        }
        return path;
    }

    /* ---------- notes ---------- */

    public List<Note> notes(String subjectId) {
        List<Note> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, subject_id, category_id, name, content, formula, tags, source, importance, created_at, figure, quote, slide_path, slide_page " +
                        "FROM notes WHERE subject_id = ? ORDER BY created_at DESC", new String[]{subjectId});
        try {
            while (c.moveToNext()) out.add(readNote(c));
        } finally {
            c.close();
        }
        return out;
    }

    /** 按标题或重要度排序 */
    public List<Note> notesSorted(String subjectId, String order) {
        List<Note> out = new ArrayList<>();
        String sql = "SELECT id, subject_id, category_id, name, content, formula, tags, source, importance, created_at, figure, quote, slide_path, slide_page " +
                "FROM notes WHERE subject_id = ? ";
        if ("title".equals(order)) sql += "ORDER BY name COLLATE NOCASE ASC";
        else if ("importance".equals(order)) sql += "ORDER BY importance DESC, created_at DESC";
        else sql += "ORDER BY created_at DESC";
        Cursor c = getReadableDatabase().rawQuery(sql, new String[]{subjectId});
        try {
            while (c.moveToNext()) out.add(readNote(c));
        } finally {
            c.close();
        }
        return out;
    }

    private Note readNote(Cursor c) {
        Note n = new Note();
        n.id = c.getString(0);
        n.subjectId = c.getString(1);
        n.categoryId = c.getString(2);
        n.name = c.getString(3);
        n.content = c.getString(4);
        n.formula = c.getString(5);
        n.tags = c.getString(6);
        n.source = c.getString(7);
        n.importance = c.getInt(8);
        n.createdAt = c.getLong(9);
        n.figure = c.getString(10);
        n.quote = c.getString(11);
        n.slidePath = c.getString(12);
        n.slidePage = c.getInt(13);
        return n;
    }

    public Note note(String id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, subject_id, category_id, name, content, formula, tags, source, importance, created_at, figure, quote, slide_path, slide_page " +
                        "FROM notes WHERE id = ?", new String[]{id});
        try {
            if (!c.moveToFirst()) return null;
            Note n = readNote(c);
            n.categoryPath = categoryPath(n.categoryId);
            return n;
        } finally {
            c.close();
        }
    }

    public Note createNote(String subjectId) {
        Note n = new Note();
        n.id = UUID.randomUUID().toString();
        n.subjectId = subjectId;
        n.name = "新知识点";
        n.content = "";
        n.formula = "";
        n.tags = "";
        n.source = "";
        n.importance = 2;
        n.createdAt = System.currentTimeMillis();
        ContentValues v = new ContentValues();
        v.put("id", n.id);
        v.put("subject_id", subjectId);
        v.put("category_id", (String) null);
        v.put("name", n.name);
        v.put("content", "");
        v.put("formula", "");
        v.put("tags", "");
        v.put("source", "");
        v.put("figure", (String) null);
        v.put("importance", 2);
        v.put("created_at", n.createdAt);
        getWritableDatabase().insert("notes", null, v);
        return n;
    }

    public void saveNote(Note n) {
        ContentValues v = new ContentValues();
        v.put("name", n.name);
        v.put("content", n.content);
        v.put("formula", n.formula);
        v.put("tags", n.tags);
        v.put("importance", n.importance);
        v.put("category_id", n.categoryId);
        v.put("figure", n.figure);
        v.put("quote", n.quote);
        v.put("slide_path", n.slidePath);
        v.put("slide_page", n.slidePage);
        getWritableDatabase().update("notes", v, "id = ?", new String[]{n.id});
    }

    public void deleteNote(String id) {
        getWritableDatabase().delete("notes", "id = ?", new String[]{id});
    }

    /* ---------- usage ---------- */

    public void logUsage(String subjectId, String kind, int inTok, int outTok) {
        ContentValues v = new ContentValues();
        v.put("id", UUID.randomUUID().toString());
        v.put("ts", System.currentTimeMillis());
        v.put("kind", kind);
        v.put("subject_id", subjectId);
        v.put("in_tok", inTok);
        v.put("out_tok", outTok);
        getWritableDatabase().insert("usage_log", null, v);
    }

    /** 按用途汇总：kind, 次数, 输入, 输出 */
    public List<String[]> usageByKind() {
        List<String[]> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(kind,'other'), COUNT(*), COALESCE(SUM(in_tok),0), COALESCE(SUM(out_tok),0) " +
                        "FROM usage_log GROUP BY kind ORDER BY COUNT(*) DESC", null);
        try {
            while (c.moveToNext()) {
                out.add(new String[]{c.getString(0), String.valueOf(c.getInt(1)),
                        String.valueOf(c.getLong(2)), String.valueOf(c.getLong(3))});
            }
        } finally {
            c.close();
        }
        return out;
    }

    /** 按天汇总：yyyy-MM-dd, 次数, 输入, 输出 */
    public List<String[]> usageByDay(int days) {
        List<String[]> out = new ArrayList<>();
        long since = System.currentTimeMillis() - days * 24L * 3600 * 1000;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT date(ts/1000,'unixepoch','localtime') AS d, COUNT(*), " +
                        "COALESCE(SUM(in_tok),0), COALESCE(SUM(out_tok),0) " +
                        "FROM usage_log WHERE ts >= ? GROUP BY d ORDER BY d DESC", new String[]{String.valueOf(since)});
        try {
            while (c.moveToNext()) {
                out.add(new String[]{c.getString(0), String.valueOf(c.getInt(1)),
                        String.valueOf(c.getLong(2)), String.valueOf(c.getLong(3))});
            }
        } finally {
            c.close();
        }
        return out;
    }

    /** 导入备份（按名称合并科目，重复跳过） */
    public int[] importBackup(org.json.JSONObject root) {
        int subjCount = 0, catCount = 0, noteCount = 0;
        try {
            org.json.JSONArray subjects = root.optJSONArray("subjects");
            org.json.JSONArray cats = root.optJSONArray("categories");
            org.json.JSONArray notes = root.optJSONArray("notes");

            java.util.Map<String, String> subjMap = new java.util.HashMap<>();
            if (subjects != null) {
                for (int i = 0; i < subjects.length(); i++) {
                    org.json.JSONObject s = subjects.optJSONObject(i);
                    if (s == null) continue;
                    String name = s.optString("name", "").trim();
                    if (name.isEmpty()) continue;
                    String id = findSubjectByName(name);
                    if (id == null) {
                        Subject created = addSubject(name);
                        id = created.id;
                        subjCount++;
                    }
                    subjMap.put(s.optString("id"), id);
                }
            }

            java.util.Map<String, String> catMap = new java.util.HashMap<>();
            if (cats != null) {
                for (int i = 0; i < cats.length(); i++) {
                    org.json.JSONObject c = cats.optJSONObject(i);
                    if (c == null) continue;
                    String newSubj = subjMap.get(c.optString("subjectId"));
                    if (newSubj == null) continue;
                    String oldParent = c.optString("parentId", null);
                    String newParent = oldParent == null || oldParent.isEmpty() ? null : catMap.get(oldParent);
                    String name = c.optString("name", "").trim();
                    if (name.isEmpty()) continue;
                    String existing = findCategory(newSubj, newParent, name);
                    if (existing == null) {
                        Category k = new Category();
                        k.id = UUID.randomUUID().toString();
                        k.subjectId = newSubj;
                        k.parentId = newParent;
                        k.name = name;
                        k.orderNo = c.optInt("orderNo", 0);
                        ContentValues v = new ContentValues();
                        v.put("id", k.id);
                        v.put("subject_id", newSubj);
                        v.put("parent_id", newParent);
                        v.put("name", name);
                        v.put("order_no", k.orderNo);
                        v.put("created_at", System.currentTimeMillis());
                        getWritableDatabase().insert("categories", null, v);
                        existing = k.id;
                        catCount++;
                    }
                    catMap.put(c.optString("id"), existing);
                }
            }

            if (notes != null) {
                for (int i = 0; i < notes.length(); i++) {
                    org.json.JSONObject n = notes.optJSONObject(i);
                    if (n == null) continue;
                    String newSubj = subjMap.get(n.optString("subjectId"));
                    if (newSubj == null) continue;
                    String name = n.optString("name", "").trim();
                    if (name.isEmpty()) continue;
                    if (noteExists(newSubj, name)) continue;

                    String catId = n.optString("categoryId", null);
                    String newCat = catId == null || catId.isEmpty() ? null : catMap.get(catId);

                    ContentValues v = new ContentValues();
                    v.put("id", UUID.randomUUID().toString());
                    v.put("subject_id", newSubj);
                    v.put("category_id", newCat);
                    v.put("name", name);
                    v.put("content", n.optString("content", ""));
                    v.put("formula", n.optString("formula", ""));
                    v.put("tags", n.optString("tags", ""));
                    v.put("source", n.optString("source", ""));
                    v.put("importance", n.optInt("importance", 2));
                    v.put("created_at", n.optLong("createdAt", System.currentTimeMillis()));
                    getWritableDatabase().insert("notes", null, v);
                    noteCount++;
                }
            }
        } catch (Exception ignored) {
        }
        return new int[]{subjCount, catCount, noteCount};
    }

    private String findSubjectByName(String name) {
        Cursor c = getReadableDatabase().rawQuery("SELECT id FROM subjects WHERE name = ?", new String[]{name});
        try {
            return c.moveToFirst() ? c.getString(0) : null;
        } finally {
            c.close();
        }
    }

    private boolean noteExists(String subjectId, String name) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM notes WHERE subject_id = ? AND name = ? LIMIT 1", new String[]{subjectId, name});
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    /** 全局搜索：返回 {noteId, 标题, 科目名} */
    public List<String[]> search(String query) {
        List<String[]> out = new ArrayList<>();
        String q = "%" + (query == null ? "" : query.trim()) + "%";
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT n.id, n.name, s.name FROM notes n JOIN subjects s ON s.id = n.subject_id " +
                        "WHERE n.name LIKE ? OR n.content LIKE ? OR n.formula LIKE ? " +
                        "ORDER BY n.created_at DESC LIMIT 80",
                new String[]{q, q, q});
        try {
            while (c.moveToNext()) out.add(new String[]{c.getString(0), c.getString(1), c.getString(2)});
        } finally {
            c.close();
        }
        return out;
    }

    /** 导出全部用量记录 */
    public org.json.JSONArray exportUsage() {
        org.json.JSONArray arr = new org.json.JSONArray();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT ts, kind, subject_id, in_tok, out_tok FROM usage_log ORDER BY ts ASC", null);
        try {
            while (c.moveToNext()) {
                org.json.JSONObject o = new org.json.JSONObject();
                try {
                    o.put("ts", c.getLong(0));
                    o.put("kind", c.getString(1));
                    o.put("subjectId", c.getString(2));
                    o.put("in", c.getLong(3));
                    o.put("out", c.getLong(4));
                } catch (Exception ignored) {
                }
                arr.put(o);
            }
        } finally {
            c.close();
        }
        return arr;
    }

    /** 导入用量：同时间戳+用途视为重复，跳过 */
    public int importUsage(org.json.JSONArray arr) {
        int added = 0;
        if (arr == null) return 0;
        for (int i = 0; i < arr.length(); i++) {
            org.json.JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            long ts = o.optLong("ts", 0);
            String kind = o.optString("kind", "");
            if (ts <= 0) continue;
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT 1 FROM usage_log WHERE ts = ? AND COALESCE(kind,'') = ? LIMIT 1",
                    new String[]{String.valueOf(ts), kind == null ? "" : kind});
            boolean exists;
            try {
                exists = c.moveToFirst();
            } finally {
                c.close();
            }
            if (exists) continue;
            ContentValues v = new ContentValues();
            v.put("id", UUID.randomUUID().toString());
            v.put("ts", ts);
            v.put("kind", kind);
            v.put("subject_id", o.optString("subjectId", ""));
            v.put("in_tok", o.optLong("in", 0));
            v.put("out_tok", o.optLong("out", 0));
            getWritableDatabase().insert("usage_log", null, v);
            added++;
        }
        return added;
    }

    public void clearUsage() {
        getWritableDatabase().delete("usage_log", null, null);
    }

    public long[] usageTotals() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(in_tok),0), COALESCE(SUM(out_tok),0), COUNT(*) FROM usage_log", null);
        try {
            return c.moveToFirst() ? new long[]{c.getLong(0), c.getLong(1), c.getLong(2)} : new long[]{0, 0, 0};
        } finally {
            c.close();
        }
    }
}
