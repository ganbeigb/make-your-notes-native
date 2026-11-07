package com.dshwork.mynotes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 一个轻量的 LaTeX 子集渲染器：上下标、分式、根号、常用符号与希腊字母。 */
public class FormulaView extends View {

    private static final Map<String, String> SYMBOLS = new HashMap<>();

    static {
        SYMBOLS.put("alpha", "α"); SYMBOLS.put("beta", "β"); SYMBOLS.put("gamma", "γ"); SYMBOLS.put("delta", "δ");
        SYMBOLS.put("epsilon", "ε"); SYMBOLS.put("varepsilon", "ε"); SYMBOLS.put("theta", "θ"); SYMBOLS.put("lambda", "λ");
        SYMBOLS.put("mu", "μ"); SYMBOLS.put("pi", "π"); SYMBOLS.put("rho", "ρ"); SYMBOLS.put("sigma", "σ");
        SYMBOLS.put("tau", "τ"); SYMBOLS.put("phi", "φ"); SYMBOLS.put("omega", "ω"); SYMBOLS.put("Delta", "Δ");
        SYMBOLS.put("Omega", "Ω"); SYMBOLS.put("Sigma", "Σ"); SYMBOLS.put("infty", "∞"); SYMBOLS.put("cdot", "·");
        SYMBOLS.put("times", "×"); SYMBOLS.put("div", "÷"); SYMBOLS.put("pm", "±"); SYMBOLS.put("mp", "∓");
        SYMBOLS.put("le", "≤"); SYMBOLS.put("leq", "≤"); SYMBOLS.put("ge", "≥"); SYMBOLS.put("geq", "≥");
        SYMBOLS.put("ne", "≠"); SYMBOLS.put("neq", "≠"); SYMBOLS.put("approx", "≈"); SYMBOLS.put("to", "→");
        SYMBOLS.put("sum", "∑"); SYMBOLS.put("int", "∫"); SYMBOLS.put("partial", "∂"); SYMBOLS.put("propto", "∝");
        SYMBOLS.put("angle", "∠"); SYMBOLS.put("degree", "°");
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String tex = "";
    private Box root;
    private int color = Ui.INK;
    private float spSize = 17f;
    private float textScale = 1f;

    public FormulaView(Context c) {
        this(c, null);
    }

    public FormulaView(Context c, AttributeSet a) {
        super(c, a);
        paint.setColor(Ui.INK);
        color = Ui.INK;
        paint.setTextSize(spSize * textScale * c.getResources().getDisplayMetrics().scaledDensity);
        paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
    }

    /** 整体放大倍率（详情页弹窗放大用） */
    public void setTextScale(float factor) {
        textScale = factor;
        paint.setTextSize(spSize * textScale * getResources().getDisplayMetrics().scaledDensity);
        rebuild();
    }

    public void setColor(int c) {
        color = c;
        paint.setColor(c);
        rebuild();
    }

    public void setTex(String s) {
        tex = s == null ? "" : s.trim();
        rebuild();
    }

    public String getTex() {
        return tex;
    }

    private void rebuild() {
        String t = tex;
        if (t.startsWith("$$") && t.endsWith("$$") && t.length() > 4) t = t.substring(2, t.length() - 2);
        else if (t.startsWith("$") && t.endsWith("$") && t.length() > 2) t = t.substring(1, t.length() - 1);
        try {
            root = new Parser(t, paint).parse();
        } catch (Exception e) {
            root = new TextBox(t, paint);
        }
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int pad = Ui.dp(getContext(), 8);
        int w = root == null ? pad * 2 : (int) Math.ceil(root.w) + pad * 2;
        int h = root == null ? pad * 2 : (int) Math.ceil(root.h) + pad * 2;
        setMeasuredDimension(resolveSize(w, widthSpec), resolveSize(h, heightSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (root == null) return;
        int pad = Ui.dp(getContext(), 8);
        root.draw(canvas, pad, pad + root.base);
    }

    /* ---------------- 排版盒子 ---------------- */

    abstract static class Box {
        float w, h, base;

        abstract void draw(Canvas c, float x, float y);
    }

    static class TextBox extends Box {
        final String text;
        final Paint p;

        TextBox(String t, Paint paint) {
            text = t;
            p = new Paint(paint);
            p.setTextSize(paint.getTextSize());
            w = p.measureText(t);
            Paint.FontMetrics fm = p.getFontMetrics();
            h = fm.descent - fm.ascent;
            base = -fm.ascent;
        }

        @Override
        void draw(Canvas c, float x, float y) {
            c.drawText(text, x, y, p);
        }
    }

    static class RowBox extends Box {
        final List<Box> items = new ArrayList<>();

        void add(Box b) {
            items.add(b);
            w += b.w;
            h = Math.max(h, b.h);
            base = Math.max(base, b.base);
        }

        @Override
        void draw(Canvas c, float x, float y) {
            float cx = x;
            for (Box b : items) {
                b.draw(c, cx, y);
                cx += b.w;
            }
        }
    }

    static class FracBox extends Box {
        final Box num, den;
        final Paint bar;

        FracBox(Box n, Box d, Paint p) {
            num = n;
            den = d;
            bar = new Paint(p);
            float gap = p.getTextSize() * 0.22f;
            float axis = num.h + gap / 2f;
            w = Math.max(num.w, den.w) + p.getTextSize() * 0.30f;
            h = axis + gap / 2f + den.h;
            base = axis;
        }

        @Override
        void draw(Canvas c, float x, float y) {
            float top = y - base;
            num.draw(c, x + (w - num.w) / 2f, top + num.base);
            c.drawRect(x + w * 0.04f, y - 0.6f, x + w * 0.96f, y + 0.6f, bar);
            den.draw(c, x + (w - den.w) / 2f, top + num.h + (h - num.h - den.h) + den.base);
        }
    }

    static class SqrtBox extends Box {
        final Box child;
        final Paint p;
        final float signW;

        SqrtBox(Box c, Paint paint) {
            child = c;
            p = new Paint(paint);
            signW = p.getTextSize() * 0.62f;
            w = signW + child.w + p.getTextSize() * 0.08f;
            h = child.h + p.getTextSize() * 0.10f;
            base = child.base + p.getTextSize() * 0.04f;
        }

        @Override
        void draw(Canvas c, float x, float y) {
            float top = y - base;
            Paint.FontMetrics fm = p.getFontMetrics();
            c.drawText("√", x, top + (-fm.ascent), p);
            c.drawRect(x + signW * 0.82f, top + 0.8f, x + w, top + 1.8f, p);
            child.draw(c, x + signW, y);
        }
    }

    static class ScriptBox extends Box {
        final Box main, sup, sub;

        ScriptBox(Box m, Box s, Box b) {
            main = m;
            sup = s;
            sub = b;
            float supH = sup == null ? 0 : sup.h;
            float subH = sub == null ? 0 : sub.h;
            w = main.w + Math.max(sup == null ? 0 : sup.w, sub == null ? 0 : sub.w);
            h = Math.max(main.h, main.base + subH) + supH * 0.85f;
            base = main.base + supH * 0.85f;
        }

        @Override
        void draw(Canvas c, float x, float y) {
            main.draw(c, x, y);
            if (sup != null) sup.draw(c, x + main.w, y - main.base * 0.55f - sup.h * 0.15f);
            if (sub != null) sub.draw(c, x + main.w, y + sub.h * 0.55f);
        }
    }

    /** 矩阵 / 方程组：按行与列对齐，可带括号 */
    static class MatrixBox extends Box {
        final java.util.List<java.util.List<Box>> rows;
        final String env;
        final Paint paint;
        final float[] colW;
        final float[] rowH;
        final float[] rowBase;
        final float gap = 6f;
        final float bracketW = 7f;

        MatrixBox(java.util.List<java.util.List<Box>> rows, String env, Paint p) {
            this.rows = rows;
            this.env = env;
            this.paint = new Paint(p);
            int cols = 0;
            for (java.util.List<Box> r : rows) cols = Math.max(cols, r.size());
            colW = new float[Math.max(1, cols)];
            for (java.util.List<Box> r : rows) {
                for (int c = 0; c < r.size(); c++) colW[c] = Math.max(colW[c], r.get(c).w);
            }
            rowH = new float[rows.size()];
            rowBase = new float[rows.size()];
            for (int i = 0; i < rows.size(); i++) {
                float h = 0, b = 0;
                for (Box box : rows.get(i)) {
                    h = Math.max(h, box.h);
                    b = Math.max(b, box.base);
                }
                rowH[i] = h + 8f;
                rowBase[i] = b + 4f;
            }
            float contentW = 0;
            for (float cw : colW) contentW += cw + gap;
            float contentH = 0;
            for (float rh : rowH) contentH += rh;
            boolean bracketed = !"matrix".equals(env);
            w = contentW + (bracketed ? bracketW * 2 + 6 : 0);
            h = contentH;
            base = contentH / 2f;
        }

        @Override
        void draw(Canvas c, float x, float y) {
            float top = y - base;
            boolean bracketed = !"matrix".equals(env);
            float cx = x + (bracketed ? bracketW + 4 : 0);
            Paint bp = new Paint(paint);
            bp.setStyle(Paint.Style.STROKE);
            bp.setStrokeWidth(Math.max(1.2f, paint.getTextSize() * 0.08f));

            if (bracketed) {
                if ("cases".equals(env)) {
                    Path brace = new Path();
                    brace.moveTo(x + bracketW, top);
                    brace.cubicTo(x, top + h * 0.2f, x, top + h * 0.8f, x + bracketW, top + h);
                    c.drawPath(brace, bp);
                } else if ("pmatrix".equals(env)) {
                    c.drawArc(new android.graphics.RectF(x - bracketW, top, x + bracketW, top + h), 90, 180, false, bp);
                    c.drawArc(new android.graphics.RectF(x + w - bracketW * 3, top, x + w - bracketW, top + h), -90, 180, false, bp);
                } else {
                    drawBracket(c, x, top, h, false, bp);
                    drawBracket(c, x + w - bracketW, top, h, true, bp);
                }
            }

            float cy = top;
            for (int r = 0; r < rows.size(); r++) {
                float rx = cx;
                java.util.List<Box> row = rows.get(r);
                for (int col = 0; col < row.size(); col++) {
                    Box box = row.get(col);
                    float cellCx = rx + (colW[col] - box.w) / 2f;
                    float cellBaseline = cy + rowBase[r];
                    box.draw(c, cellCx, cellBaseline);
                    rx += colW[col] + gap;
                }
                cy += rowH[r];
            }
        }

        private void drawBracket(Canvas c, float x, float top, float h, boolean right, Paint bp) {
            Path path = new Path();
            float dir = right ? -1 : 1;
            path.moveTo(x + (right ? 0 : bracketW), top);
            path.lineTo(x + (right ? 0 : 0), top);
            path.lineTo(x + (right ? 0 : 0), top + h);
            path.lineTo(x + (right ? 0 : bracketW), top + h);
            c.drawPath(path, bp);
        }
    }

    /* ---------------- 解析器 ---------------- */

    static class Parser {
        private float scale = 1f;

        private Paint sp() {
            Paint q = new Paint(p);
            q.setTextSize(p.getTextSize() * scale);
            return q;
        }

        private final String s;
        private final Paint p;
        private int i;

        Parser(String s, Paint p) {
            this.s = s;
            this.p = p;
        }

        Box parse() {
            RowBox row = parseRow(false);
            return row.items.size() == 1 ? row.items.get(0) : row;
        }

        private boolean end() {
            return i >= s.length();
        }

        private char peek() {
            return s.charAt(i);
        }

        private RowBox parseRow(boolean insideGroup) {
            RowBox row = new RowBox();
            while (!end()) {
                char c = peek();
                if (c == '}') {
                    if (insideGroup) break;
                    i++;
                    continue;
                }
                if (c == '^' || c == '_') {
                    i++;
                    Box script = parseScriptArg();
                    Box last = row.items.isEmpty() ? new TextBox("", sp()) : row.items.remove(row.items.size() - 1);
                    Box merged = c == '^'
                            ? new ScriptBox(last, script, null)
                            : new ScriptBox(last, null, script);
                    row.add(merged);
                    continue;
                }
                if (c == '\\') {
                    row.add(parseCommand());
                    continue;
                }
                if (c == '{') {
                    i++;
                    RowBox inner = parseRow(true);
                    if (!end() && peek() == '}') i++;
                    row.add(inner.items.size() == 1 ? inner.items.get(0) : inner);
                    continue;
                }
                if (c == ' ') {
                    i++;
                    TextBox sp = new TextBox(" ", sp());
                    row.add(sp);
                    continue;
                }
                // 普通字符：字母用斜体
                StringBuilder sb = new StringBuilder();
                while (!end()) {
                    char ch = peek();
                    if (ch == '\\' || ch == '^' || ch == '_' || ch == '{' || ch == '}' || ch == ' ') break;
                    sb.append(ch);
                    i++;
                }
                if (sb.length() == 0) {
                    i++;
                    continue;
                }
                Paint np = new Paint(p);
                boolean letters = sb.toString().matches("[A-Za-z]+");
                np.setTypeface(Typeface.create(Typeface.SERIF, letters ? Typeface.ITALIC : Typeface.NORMAL));
                row.add(new TextBox(sb.toString(), np));
            }
            return row;
        }

        private Box parseScriptArg() {
            if (end()) return new TextBox("", sp());
            if (peek() == '{') {
                i++;
                scale *= 0.68f;
                RowBox inner = parseRow(true);
                scale /= 0.68f;
                if (!end() && peek() == '}') i++;
                return inner.items.size() == 1 ? inner.items.get(0) : inner;
            }
            String ch = String.valueOf(peek());
            i++;
            Paint np = new Paint(p);
            np.setTextSize(p.getTextSize() * 0.68f);
            return new TextBox(ch, np);
        }

        private Box parseCommand() {
            i++;
            StringBuilder name = new StringBuilder();
            while (!end() && Character.isLetter(peek())) {
                name.append(peek());
                i++;
            }
            String cmd = name.toString();
            if ("frac".equals(cmd) || "dfrac".equals(cmd)) {
                Box a = readGroup();
                Box b = readGroup();
                return new FracBox(a, b, p);
            }
            if ("sqrt".equals(cmd)) {
                Box a = readGroup();
                return new SqrtBox(a, p);
            }
            if ("text".equals(cmd) || "mathrm".equals(cmd)) {
                Box a = readGroup();
                return a;
            }
            if ("begin".equals(cmd)) {
                String env = readEnvName();
                int endIdx = s.indexOf("\\end{" + env + "}", i);
                if (endIdx < 0) {
                    return new TextBox("[matrix]", sp());
                }
                String body = s.substring(i, endIdx);
                i = endIdx + ("\\end{" + env + "}").length();
                return buildMatrix(body, env);
            }
            if ("end".equals(cmd)) {
                readEnvName();
                return new TextBox("", sp());
            }
            if ("left".equals(cmd) || "right".equals(cmd)) {
                return new TextBox("", sp());
            }
            String sym = SYMBOLS.get(cmd);
            if (sym != null) {
                Paint np = new Paint(p);
                np.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
                np.setTextSize(p.getTextSize() * ("sum".equals(cmd) || "int".equals(cmd) ? 1.25f : 1f));
                return new TextBox(sym, np);
            }
            if (cmd.isEmpty()) {
                String ch = end() ? String.valueOf(' ') : String.valueOf(peek());
                if (!end()) i++;
                return new TextBox(ch, sp());
            }
            return new TextBox(cmd, sp());
        }

        private String readEnvName() {
            while (!end() && peek() == ' ') i++;
            if (!end() && peek() == '{') i++;
            StringBuilder sb = new StringBuilder();
            while (!end() && peek() != '}') {
                sb.append(peek());
                i++;
            }
            if (!end() && peek() == '}') i++;
            return sb.toString().trim();
        }

        private Box buildMatrix(String body, String env) {
            java.util.List<java.util.List<Box>> rows = new java.util.ArrayList<>();
            String[] rawRows = body.split("\\\\\\\\");
            for (String rawRow : rawRows) {
                if (rawRow.trim().isEmpty()) continue;
                java.util.List<Box> cells = new java.util.ArrayList<>();
                String[] rawCells = rawRow.split("&");
                for (String cell : rawCells) {
                    cells.add(new Parser(cell.trim(), p).parse());
                }
                rows.add(cells);
            }
            if (rows.isEmpty()) return new TextBox("", sp());
            return new MatrixBox(rows, env, p);
        }

        private Box readGroup() {
            while (!end() && peek() == ' ') i++;
            if (end()) return new TextBox("", sp());
            if (peek() == '{') {
                i++;
                RowBox inner = parseRow(true);
                if (!end() && peek() == '}') i++;
                return inner.items.size() == 1 ? inner.items.get(0) : inner;
            }
            String ch = String.valueOf(peek());
            i++;
            return new TextBox(ch, sp());
        }
    }
}
