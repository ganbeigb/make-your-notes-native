package com.dshwork.mynotes;

import org.json.JSONArray;
import org.json.JSONObject;

/** 「让 AI 改这条笔记」与「AI 起草」。 */
public class Refine {

    public static class Result {
        public String error;
        public String name;
        public String content;
        public String formula;
        public String tags;
    }

    private static Result parse(Prefs prefs, Db db, String subjectId, AiClient.Reply reply, String kind) {
        Result r = new Result();
        db.logUsage(subjectId, kind, reply.inTok, reply.outTok);
        try {
            JSONObject o = new JSONObject(AiClient.pickJson(reply.content));
            r.name = o.optString("name", "");
            r.content = o.optString("content", "");
            r.formula = o.optString("formula", "");
            JSONArray tags = o.optJSONArray("tags");
            if (tags != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < tags.length(); i++) {
                    if (i > 0) sb.append(',');
                    sb.append(tags.optString(i));
                }
                r.tags = sb.toString();
            }
        } catch (Exception e) {
            r.error = "模型返回的不是可解析的 JSON";
        }
        return r;
    }

    /** 按用户要求改写 / 追加 */
    public static Result edit(Prefs prefs, Db db, Db.Note note, String instruction, boolean append) {
        Result r = new Result();
        if (!prefs.ready()) {
            r.error = "还没有填 API Key";
            return r;
        }
        StringBuilder u = new StringBuilder();
        u.append("科目：").append(subjectName(db, note.subjectId)).append('\n');
        u.append("标题：").append(note.name == null ? "" : note.name).append('\n');
        u.append("正文：").append(note.content == null ? "" : note.content).append('\n');
        u.append("公式：").append(note.formula == null ? "" : note.formula).append('\n');
        u.append("要求：").append(instruction).append('\n');
        u.append(append ? "请在原内容基础上补充，不要丢原有信息。" : "请按要求改写，保留事实。");
        if (prefs.keepFormula()) u.append("公式必须原样保留，不要改写。");

        try {
            AiClient.Reply reply = AiClient.chat(
                    Prefs.baseUrl(prefs.provider()), prefs.apiKey(), prefs.textModel(),
                    Prompts.REFINE_SYSTEM, u.toString(), null, prefs.reasoning());
            return parse(prefs, db, note.subjectId, reply, "refine");
        } catch (Exception e) {
            r.error = AiClient.friendly(e);
            return r;
        }
    }

    /** 从一句要求起草一条笔记 */
    public static Result draft(Prefs prefs, Db db, String subjectId, String instruction) {
        Result r = new Result();
        if (!prefs.ready()) {
            r.error = "还没有填 API Key";
            return r;
        }
        String u = "科目：" + subjectName(db, subjectId) + "\n要求：" + instruction;
        if (prefs.keepFormula()) u += "\n公式如涉及请用标准写法，不要自造符号。";
        try {
            AiClient.Reply reply = AiClient.chat(
                    Prefs.baseUrl(prefs.provider()), prefs.apiKey(), prefs.textModel(),
                    Prompts.DRAFT_SYSTEM, u, null, prefs.reasoning());
            return parse(prefs, db, subjectId, reply, "draft");
        } catch (Exception e) {
            r.error = AiClient.friendly(e);
            return r;
        }
    }

    private static String subjectName(Db db, String id) {
        Db.Subject s = db.subject(id);
        return s == null ? "" : s.name;
    }
}
