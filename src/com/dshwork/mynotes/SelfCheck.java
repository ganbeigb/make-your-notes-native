package com.dshwork.mynotes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/** 运行期自检：不依赖设备人工操作，验证各子系统能正常工作。 */
public class SelfCheck {

    public static class Item {
        public String name;
        public boolean ok;
        public String detail;

        Item(String n, boolean o, String d) {
            name = n;
            ok = o;
            detail = d;
        }
    }

    public static List<Item> run(Context ctx) {
        List<Item> out = new ArrayList<>();
        Db db = new Db(ctx);
        String tempSubjectId = null;

        // 1. 数据库读写
        try {
            Db.Subject s = db.addSubject("__selfcheck__");
            tempSubjectId = s.id;
            Db.Subject back = db.subject(s.id);
            out.add(new Item("数据库读写", back != null && "__selfcheck__".equals(back.name),
                    back == null ? "写入后读不到" : "科目 id " + s.id.substring(0, 8)));
        } catch (Exception e) {
            out.add(new Item("数据库读写", false, e.getClass().getSimpleName() + ": " + e.getMessage()));
        }

        // 2. 分类路径
        try {
            String leaf = db.ensureCategoryPath(tempSubjectId, "A/B/C");
            String path = db.categoryPath(leaf);
            out.add(new Item("分类层级", "A / B / C".equals(path), "得到：" + path));
        } catch (Exception e) {
            out.add(new Item("分类层级", false, String.valueOf(e.getMessage())));
        }

        // 3. 知识点读写
        try {
            Db.Note n = db.createNote(tempSubjectId);
            n.name = "自检知识点";
            n.formula = "E=mc^2";
            n.quote = "原文";
            n.importance = 3;
            db.saveNote(n);
            Db.Note back = db.note(n.id);
            boolean ok = back != null && "E=mc^2".equals(back.formula) && back.importance == 3;
            out.add(new Item("知识点读写", ok, ok ? "公式/重要性字段正常" : "字段丢失"));
        } catch (Exception e) {
            out.add(new Item("知识点读写", false, String.valueOf(e.getMessage())));
        }

        // 3.5 课件与页面
        try {
            Db.Deck deck = db.addDeck(tempSubjectId, "自检课件");
            Db.Slide slide = db.addSlide(tempSubjectId, deck.id, 1, null, "selfcheckhash");
            boolean hashOk = db.slideHashExists(tempSubjectId, "selfcheckhash");
            boolean idxOk = db.slideIndexExists(deck.id, 1);
            int slideCount = db.slides(deck.id).size();
            db.deleteDeck(deck.id);
            boolean gone = db.slides(deck.id).isEmpty();
            boolean ok = hashOk && idxOk && slideCount == 1 && gone && slide != null;
            out.add(new Item("课件与页面", ok,
                    "页数 " + slideCount + "，指纹/页码查重 " + (hashOk && idxOk ? "正常" : "异常")
                            + "，删除后清理 " + (gone ? "正常" : "残留")));
        } catch (Exception e) {
            out.add(new Item("课件与页面", false, String.valueOf(e.getMessage())));
        }

        // 4. 公式渲染
        try {
            FormulaView v = new FormulaView(ctx);
            v.setTex("\\frac{1}{2}mv^2 + \\sqrt{x}");
            int spec = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED);
            v.measure(spec, spec);
            int w = Math.max(1, v.getMeasuredWidth());
            int h = Math.max(1, v.getMeasuredHeight());
            v.layout(0, 0, w, h);
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            c.drawColor(0xFFFFFFFF);
            v.draw(c);
            int ink = 0;
            for (int y = 0; y < h; y += 2) {
                for (int x = 0; x < w; x += 2) {
                    if (bmp.getPixel(x, y) != 0xFFFFFFFF) ink++;
                }
            }
            bmp.recycle();
            out.add(new Item("公式渲染", ink > 20, "画布 " + w + "x" + h + "，非空白像素 " + ink));
        } catch (Exception e) {
            out.add(new Item("公式渲染", false, String.valueOf(e.getMessage())));
        }

        // 5. 导出
        try {
            String md = Exporter.markdown(db, tempSubjectId);
            String html = Exporter.html(ctx, db, tempSubjectId);
            String fx = Exporter.formulas(ctx, db, tempSubjectId);
            boolean ok = md.contains("自检知识点") && html.contains("<html") && fx.contains("<html");
            out.add(new Item("导出 Markdown/HTML", ok,
                    "md " + md.length() + " 字符，html " + html.length() + " 字符"));
        } catch (Exception e) {
            out.add(new Item("导出 Markdown/HTML", false, String.valueOf(e.getMessage())));
        }

        // 5.5 导出写入：把 Markdown 真正写到磁盘再读回来
        try {
            String md = Exporter.markdown(db, tempSubjectId);
            java.io.File mdFile = new java.io.File(ctx.getFilesDir(), "selfcheck-export.md");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(mdFile);
            fos.write(md.getBytes("UTF-8"));
            fos.close();
            byte[] back = new byte[(int) mdFile.length()];
            java.io.FileInputStream fis = new java.io.FileInputStream(mdFile);
            int total = 0;
            int got;
            while (total < back.length && (got = fis.read(back, total, back.length - total)) > 0) {
                total += got;
            }
            fis.close();
            boolean ok = total == md.getBytes("UTF-8").length && total > 0;
            mdFile.delete();
            out.add(new Item("导出写入", ok, "写入 " + total + " 字节并成功读回"));
        } catch (Exception e) {
            out.add(new Item("导出写入", false, String.valueOf(e.getMessage())));
        }

        // 6. 备份
        try {
            byte[] zip = Exporter.backup(db, ctx);
            int parsedNotes = -1;
            boolean hasJson = false;
            if (zip != null) {
                java.util.zip.ZipInputStream zs = new java.util.zip.ZipInputStream(
                        new java.io.ByteArrayInputStream(zip));
                java.util.zip.ZipEntry ze;
                byte[] buf2 = new byte[8192];
                while ((ze = zs.getNextEntry()) != null) {
                    if (ze.getName().endsWith("mynotes-backup.json")) {
                        hasJson = true;
                        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                        int n2;
                        while ((n2 = zs.read(buf2)) > 0) bo.write(buf2, 0, n2);
                        org.json.JSONObject parsed = new org.json.JSONObject(new String(bo.toByteArray(), "UTF-8"));
                        org.json.JSONArray ns = parsed.optJSONArray("notes");
                        parsedNotes = ns == null ? 0 : ns.length();
                        break;
                    }
                }
                zs.close();
            }
            int localNotes = db.notes(tempSubjectId).size();
            boolean backupOk = zip != null && zip.length > 100 && hasJson && parsedNotes == localNotes;
            out.add(new Item("备份 zip", backupOk,
                    "大小 " + (zip == null ? 0 : zip.length) + " 字节，清单知识点 " + parsedNotes
                            + " 条，与本机 " + localNotes + " 条" + (backupOk ? " 一致" : " 不一致")));
        } catch (Exception e) {
            out.add(new Item("备份 zip", false, String.valueOf(e.getMessage())));
        }

        // 6.5 恢复幂等：同一份备份再导入一次，不应产生重复知识点
        try {
            byte[] zip2 = Exporter.backup(db, ctx);
            String json2 = null;
            java.util.zip.ZipInputStream zs2 = new java.util.zip.ZipInputStream(
                    new java.io.ByteArrayInputStream(zip2));
            java.util.zip.ZipEntry ze2;
            byte[] buf3 = new byte[8192];
            while ((ze2 = zs2.getNextEntry()) != null) {
                if (ze2.getName().endsWith("mynotes-backup.json")) {
                    java.io.ByteArrayOutputStream bo2 = new java.io.ByteArrayOutputStream();
                    int n3;
                    while ((n3 = zs2.read(buf3)) > 0) bo2.write(buf3, 0, n3);
                    json2 = new String(bo2.toByteArray(), "UTF-8");
                    break;
                }
            }
            zs2.close();
            int before = db.notes(tempSubjectId).size();
            int[] r2 = db.importBackup(new org.json.JSONObject(json2), null);
            int after = db.notes(tempSubjectId).size();
            boolean ok = after == before;
            out.add(new Item("恢复幂等", ok,
                    "再次导入新增 " + r2[2] + " 条，导入前后 " + before + " → " + after
                            + (ok ? " 未重复" : " 出现重复")));
        } catch (Exception e) {
            out.add(new Item("恢复幂等", false, String.valueOf(e.getMessage())));
        }

        // 6.8 配图渲染：画板能否导出成非空 PNG
        try {
            BoardView board = new BoardView(ctx);
            android.graphics.Bitmap bmp = board.renderToBitmap();
            int bytes = 0;
            if (bmp != null) {
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, bos);
                bytes = bos.size();
                bmp.recycle();
            }
            out.add(new Item("配图渲染", bytes > 100,
                    bmp == null ? "画板渲染返回空" : ("PNG " + bytes + " 字节")));
        } catch (Exception e) {
            out.add(new Item("配图渲染", false, String.valueOf(e.getMessage())));
        }

        // 6.9 导图渲染：构造一个小树并导出 PNG
        try {
            MindMapView map = new MindMapView(ctx);
            MindMapView.Node rootNode = new MindMapView.Node();
            rootNode.kind = 0;
            rootNode.label = "自检";
            MindMapView.Node catNode = new MindMapView.Node();
            catNode.kind = 1;
            catNode.label = "分类";
            MindMapView.Node leaf = new MindMapView.Node();
            leaf.kind = 2;
            leaf.id = "x";
            leaf.label = "知识点";
            catNode.children.add(leaf);
            rootNode.children.add(catNode);
            map.setTree(rootNode);
            int spec = android.view.View.MeasureSpec.makeMeasureSpec(600, android.view.View.MeasureSpec.EXACTLY);
            map.measure(spec, android.view.View.MeasureSpec.makeMeasureSpec(400, android.view.View.MeasureSpec.EXACTLY));
            map.layout(0, 0, 600, 400);
            android.graphics.Bitmap bmp = map.renderToBitmap();
            int bytes = 0;
            if (bmp != null) {
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, bos);
                bytes = bos.size();
                bmp.recycle();
            }
            out.add(new Item("导图渲染", bytes > 100, bmp == null ? "导图渲染返回空" : ("PNG " + bytes + " 字节")));
        } catch (Exception e) {
            out.add(new Item("导图渲染", false, String.valueOf(e.getMessage())));
        }

        // 7. 文件目录可写
        try {
            File dir = new File(ctx.getFilesDir(), "selfcheck");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "t.txt");
            FileOutputStream fos = new FileOutputStream(f);
            fos.write("ok".getBytes("UTF-8"));
            fos.close();
            boolean ok = f.exists() && f.length() == 2;
            f.delete();
            out.add(new Item("私有目录可写", ok, dir.getAbsolutePath()));
        } catch (Exception e) {
            out.add(new Item("私有目录可写", false, String.valueOf(e.getMessage())));
        }

        // 清理临时数据
        try {
            if (tempSubjectId != null) db.deleteSubject(tempSubjectId);
        } catch (Exception ignored) {
        }
        return out;
    }
}
