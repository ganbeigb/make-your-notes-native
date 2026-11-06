package com.dshwork.mynotes;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 拓展标题：把「拓展 / 考点 / 易错点」这类空泛标题改写成概括性标题。 */
public class Titles {

    public interface Progress {
        void onStep(String message);
    }

    public static class Result {
        public int updated;
        public String error;
    }

    private static boolean needsRename(String name) {
        if (name == null) return true;
        String n = name.trim();
        if (n.isEmpty()) return true;
        return n.contains("拓展") || n.contains("考点") || n.contains("易错") || n.contains("补充")
                || n.contains("知识点") || n.equals("新知识点");
    }

    public static Result run(Db db, Prefs prefs, String subjectId, Progress cb) {
        return run(db, prefs, subjectId, cb, false);
    }

    /** onlyExtension=true 时只改「拓展」类型条目（对应网页 fix-ext-names） */
    public static Result run(Db db, Prefs prefs, String subjectId, Progress cb, boolean onlyExtension) {
        Result r = new Result();
        if (!prefs.ready()) {
            r.error = "还没有填 API Key";
            return r;
        }
        List<Db.Note> all = db.notes(subjectId);
        List<Db.Note> targets = new ArrayList<>();
        for (Db.Note n : all) {
            if (onlyExtension && !"extension".equals(n.kind)) continue;
            if (needsRename(n.name)) targets.add(n);
        }
        if (targets.isEmpty()) {
            r.error = "没有需要改写的标题";
            return r;
        }

        int batch = Math.min(15, targets.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < batch; i++) {
            Db.Note n = targets.get(i);
            String content = n.content == null ? "" : n.content.replace("\n", " ").trim();
            if (content.length() > 90) content = content.substring(0, 90);
            sb.append(i).append(". ").append(n.name).append("：").append(content).append('\n');
        }

        if (cb != null) cb.onStep("正在生成标题…");
        try {
            AiClient.Reply reply = AiClient.chat(
                    Prefs.baseUrl(prefs.provider()),
                    prefs.apiKey(),
                    prefs.textModel(),
                    Prompts.TITLE_SYSTEM,
                    sb.toString(),
                    null,
                    prefs.reasoning());
            db.logUsage(subjectId, "title", reply.inTok, reply.outTok);

            JSONObject obj = new JSONObject(AiClient.pickJson(reply.content));
            JSONArray items = obj.optJSONArray("items");
            if (items == null) {
                r.error = "模型返回的 JSON 里没有 items";
                return r;
            }
            for (int k = 0; k < items.length(); k++) {
                JSONObject it = items.optJSONObject(k);
                if (it == null) continue;
                int idx = it.optInt("i", -1);
                if (idx < 0 || idx >= batch) continue;
                String title = it.optString("title", "").trim();
                if (title.isEmpty() || title.length() > 24) continue;
                Db.Note n = targets.get(idx);
                n.name = title;
                db.saveNote(n);
                r.updated++;
            }
        } catch (Exception e) {
            r.error = AiClient.friendly(e);
        }
        return r;
    }
}
