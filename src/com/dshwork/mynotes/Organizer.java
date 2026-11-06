package com.dshwork.mynotes;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/** AI 自动整理：给每条知识点重新分配分类路径。 */
public class Organizer {

    public interface Progress {
        void onStep(String message);
    }

    public static class Result {
        public int updated;
        public String error;
    }

    public static Result run(Db db, Prefs prefs, String subjectId, Progress cb) {
        Result r = new Result();
        if (!prefs.ready()) {
            r.error = "还没有填 API Key";
            return r;
        }
        List<Db.Note> notes = db.notes(subjectId);
        if (notes.isEmpty()) {
            r.error = "这个科目还没有知识点";
            return r;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < notes.size(); i++) {
            Db.Note n = notes.get(i);
            String cur = n.categoryId == null ? "" : db.categoryPath(n.categoryId);
            sb.append(i).append(". ").append(n.name);
            if (!cur.isEmpty()) sb.append(" | 原分类: ").append(cur);
            sb.append('\n');
        }

        if (cb != null) cb.onStep("正在请求整理建议…");
        try {
            AiClient.Reply reply = AiClient.chat(
                    Prefs.baseUrl(prefs.provider()),
                    prefs.apiKey(),
                    prefs.textModel(),
                    Prompts.ORGANIZE_SYSTEM_V2,
                    sb.toString(),
                    null,
                    prefs.reasoning());
            db.logUsage(subjectId, "organize", reply.inTok, reply.outTok);

            JSONObject obj = new JSONObject(AiClient.pickJson(reply.content));
            JSONArray items = obj.optJSONArray("items");
            if (items == null) {
                r.error = "模型返回的 JSON 里没有 items";
                return r;
            }
            if (cb != null) cb.onStep("正在写回分类…");
            for (int k = 0; k < items.length(); k++) {
                JSONObject it = items.optJSONObject(k);
                if (it == null) continue;
                int idx = it.optInt("i", -1);
                if (idx < 0 || idx >= notes.size()) continue;
                String cat = it.optString("category", "").trim();
                if (cat.isEmpty()) continue;
                Db.Note n = notes.get(idx);
                n.categoryId = db.ensureCategoryPath(subjectId, cat);
                db.saveNote(n);
                r.updated++;
            }
        } catch (Exception e) {
            r.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
        return r;
    }
}
