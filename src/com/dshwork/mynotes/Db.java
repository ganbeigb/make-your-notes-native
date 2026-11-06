package com.dshwork.mynotes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 本地存储：科目 + 知识点，纯 SQLite，没有服务端。 */
public class Db extends SQLiteOpenHelper {

    public static class Subject {
        public String id;
        public String name;
        public long createdAt;
        public int noteCount;
    }

    public static class Note {
        public String id;
        public String subjectId;
        public String name;
        public String category;
        public String formula;
        public String content;
        public long createdAt;
    }

    private static final String FILE = "mynotes.db";
    private static final int VERSION = 1;

    public Db(Context ctx) {
        super(ctx, FILE, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE subjects (" +
                "id TEXT PRIMARY KEY, name TEXT NOT NULL, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE notes (" +
                "id TEXT PRIMARY KEY, subject_id TEXT NOT NULL, name TEXT NOT NULL, " +
                "category TEXT, formula TEXT, content TEXT, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_notes_subject ON notes(subject_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 目前只有 v1，后续迁移写在这里
    }

    public List<Subject> subjects() {
        List<Subject> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT s.id, s.name, s.created_at, " +
                        "(SELECT COUNT(*) FROM notes n WHERE n.subject_id = s.id) " +
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
        getWritableDatabase().delete("subjects", "id = ?", new String[]{id});
    }

    public List<Note> notes(String subjectId) {
        List<Note> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, subject_id, name, category, formula, content, created_at " +
                        "FROM notes WHERE subject_id = ? ORDER BY created_at DESC",
                new String[]{subjectId});
        try {
            while (c.moveToNext()) out.add(readNote(c));
        } finally {
            c.close();
        }
        return out;
    }

    public Note note(String id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, subject_id, name, category, formula, content, created_at FROM notes WHERE id = ?",
                new String[]{id});
        try {
            return c.moveToFirst() ? readNote(c) : null;
        } finally {
            c.close();
        }
    }

    private Note readNote(Cursor c) {
        Note n = new Note();
        n.id = c.getString(0);
        n.subjectId = c.getString(1);
        n.name = c.getString(2);
        n.category = c.getString(3);
        n.formula = c.getString(4);
        n.content = c.getString(5);
        n.createdAt = c.getLong(6);
        return n;
    }

    public Note createNote(String subjectId) {
        Note n = new Note();
        n.id = UUID.randomUUID().toString();
        n.subjectId = subjectId;
        n.name = "新知识点";
        n.category = "";
        n.formula = "";
        n.content = "";
        n.createdAt = System.currentTimeMillis();
        ContentValues v = new ContentValues();
        v.put("id", n.id);
        v.put("subject_id", n.subjectId);
        v.put("name", n.name);
        v.put("category", n.category);
        v.put("formula", n.formula);
        v.put("content", n.content);
        v.put("created_at", n.createdAt);
        getWritableDatabase().insert("notes", null, v);
        return n;
    }

    public void saveNote(Note n) {
        ContentValues v = new ContentValues();
        v.put("name", n.name);
        v.put("category", n.category);
        v.put("formula", n.formula);
        v.put("content", n.content);
        getWritableDatabase().update("notes", v, "id = ?", new String[]{n.id});
    }

    public void deleteNote(String id) {
        getWritableDatabase().delete("notes", "id = ?", new String[]{id});
    }
}
