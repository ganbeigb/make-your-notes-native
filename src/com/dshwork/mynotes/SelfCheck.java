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

        // 6. 备份
        try {
            byte[] zip = Exporter.backup(db, ctx);
            out.add(new Item("备份 zip", zip != null && zip.length > 100, "大小 " + (zip == null ? 0 : zip.length) + " 字节"));
        } catch (Exception e) {
            out.add(new Item("备份 zip", false, String.valueOf(e.getMessage())));
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
