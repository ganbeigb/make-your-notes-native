package com.dshwork.mynotes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** 手绘板：画笔、文字、图形、元件、导线（直角走线 + 简单绕行）、撤销。 */
public class BoardView extends View {

    public static final int T_SELECT = 0;
    public static final int T_PEN = 1;
    public static final int T_TEXT = 2;
    public static final int T_SHAPE = 3;
    public static final int T_SYMBOL = 4;
    public static final int T_WIRE = 5;

    static final int K_STROKE = 0;
    static final int K_SHAPE = 1;
    static final int K_TEXT = 2;
    static final int K_SYMBOL = 3;
    static final int K_WIRE = 4;

    public interface OnNeedsText {
        void ask(PointF at, TextSink sink);
    }

    public interface TextSink {
        void onText(String text);
    }

    static class Item {
        int kind;
        List<PointF> pts = new ArrayList<>();
        String text;
        String sym;
        int shape; // 0 矩形 1 椭圆 2 直线
        int color = 0xFF243029;
        float width = 2.5f;

        Item copy() {
            Item it = new Item();
            it.kind = kind;
            for (PointF p : pts) it.pts.add(new PointF(p.x, p.y));
            it.text = text;
            it.sym = sym;
            it.shape = shape;
            it.color = color;
            it.width = width;
            return it;
        }

        RectF bounds() {
            RectF r = new RectF(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
            for (PointF p : pts) {
                r.left = Math.min(r.left, p.x);
                r.top = Math.min(r.top, p.y);
                r.right = Math.max(r.right, p.x);
                r.bottom = Math.max(r.bottom, p.y);
            }
            if (kind == K_SYMBOL && !pts.isEmpty()) {
                r.left -= 26; r.right += 26; r.top -= 24; r.bottom += 24;
            }
            if (kind == K_TEXT && !pts.isEmpty()) {
                r.right += 120; r.bottom += 22; r.top -= 20;
            }
            r.inset(-6, -6);
            return r;
        }
    }

    private final List<Item> items = new ArrayList<>();
    private final List<List<Item>> history = new ArrayList<>();
    private final List<List<Item>> redo = new ArrayList<>();

    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint symPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int tool = T_SELECT;
    private int shapeKind = 0;
    private String symbolName = "resistor";
    private int color = 0xFF243029;
    private float width = 2.5f;

    private float scale = 1f;
    private float tx = 0, ty = 0;

    private Item draft;
    private PointF wireStart;
    private boolean dragging;
    private float downX, downY, lastX, lastY;
    private float pinchDist, pinchCx, pinchCy;
    private Item selected;
    private OnNeedsText textAsker;

    public BoardView(Context c) {
        super(c);
        setBackgroundColor(0xFFFFFDF8);
        gridPaint.setColor(0xFFEFE8D8);
        gridPaint.setStrokeWidth(1);
        textPaint.setColor(color);
        textPaint.setTextSize(Ui.dp(c, 14));
        symPaint.setStyle(Paint.Style.STROKE);
        symPaint.setStrokeWidth(Ui.dp(c, 1.6f));
        symPaint.setColor(color);
    }

    public void addText(PointF at, String text) {
        if (text == null || text.trim().isEmpty()) return;
        snapshot();
        Item it = new Item();
        it.kind = K_TEXT;
        it.text = text.trim();
        it.color = color;
        it.width = width;
        it.pts.add(new PointF(at.x, at.y));
        items.add(it);
        invalidate();
    }

    public void setTextAsker(OnNeedsText t) {
        textAsker = t;
    }

    public void setTool(int t) {
        tool = t;
        wireStart = null;
        invalidate();
    }

    public void setShapeKind(int k) {
        shapeKind = k;
        tool = T_SHAPE;
    }

    public void setSymbolName(String n) {
        symbolName = n;
        tool = T_SYMBOL;
    }

    public void setColor(int c) {
        color = c;
        textPaint.setColor(c);
        symPaint.setColor(c);
    }

    public void setWidth(float w) {
        width = w;
    }

    public int getTool() {
        return tool;
    }

    /* ---------------- 撤销 ---------------- */

    private void snapshot() {
        List<Item> copy = new ArrayList<>();
        for (Item it : items) copy.add(it.copy());
        history.add(copy);
        if (history.size() > 40) history.remove(0);
        redo.clear();
    }

    public boolean undo() {
        if (history.isEmpty()) return false;
        List<Item> prev = history.remove(history.size() - 1);
        List<Item> cur = new ArrayList<>();
        for (Item it : items) cur.add(it.copy());
        redo.add(cur);
        items.clear();
        items.addAll(prev);
        invalidate();
        return true;
    }

    public boolean redoLast() {
        if (redo.isEmpty()) return false;
        List<Item> next = redo.remove(redo.size() - 1);
        List<Item> cur = new ArrayList<>();
        for (Item it : items) cur.add(it.copy());
        history.add(cur);
        items.clear();
        items.addAll(next);
        invalidate();
        return true;
    }

    public void clearAll() {
        snapshot();
        items.clear();
        invalidate();
    }

    /* ---------------- 绘制 ---------------- */

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        drawGrid(c);
        c.save();
        c.translate(tx, ty);
        c.scale(scale, scale);
        for (Item it : items) drawItem(c, it);
        if (draft != null) drawItem(c, draft);
        if (selected != null) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStyle(Paint.Style.STROKE);
            p.setColor(0xFF3D7A61);
            p.setStrokeWidth(1.5f / scale);
            c.drawRect(selected.bounds(), p);
        }
        c.restore();
    }

    private void drawGrid(Canvas c) {
        float step = 20 * scale;
        if (step < 12) return;
        float ox = tx % step;
        float oy = ty % step;
        for (float x = ox; x < getWidth(); x += step) c.drawLine(x, 0, x, getHeight(), gridPaint);
        for (float y = oy; y < getHeight(); y += step) c.drawLine(0, y, getWidth(), y, gridPaint);
    }

    private void drawItem(Canvas c, Item it) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(it.color);
        p.setStrokeWidth(it.width);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);

        switch (it.kind) {
            case K_STROKE:
            case K_WIRE: {
                if (it.pts.size() < 2) return;
                Path path = new Path();
                path.moveTo(it.pts.get(0).x, it.pts.get(0).y);
                for (int i = 1; i < it.pts.size(); i++) path.lineTo(it.pts.get(i).x, it.pts.get(i).y);
                c.drawPath(path, p);
                break;
            }
            case K_SHAPE: {
                if (it.pts.size() < 2) return;
                PointF a = it.pts.get(0), b = it.pts.get(1);
                RectF r = new RectF(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.max(a.x, b.x), Math.max(a.y, b.y));
                if (it.shape == 0) c.drawRect(r, p);
                else if (it.shape == 1) c.drawOval(r, p);
                else c.drawLine(a.x, a.y, b.x, b.y, p);
                break;
            }
            case K_TEXT: {
                if (it.pts.isEmpty() || it.text == null) return;
                Paint t = new Paint(textPaint);
                t.setColor(it.color);
                t.setTextSize(it.width * 5 + 10);
                c.drawText(it.text, it.pts.get(0).x, it.pts.get(0).y, t);
                break;
            }
            case K_SYMBOL: {
                if (it.pts.isEmpty()) return;
                Paint s = new Paint(symPaint);
                s.setColor(it.color);
                s.setStrokeWidth(1.8f);
                Symbols.draw(c, it.sym, it.pts.get(0).x, it.pts.get(0).y, s);
                break;
            }
        }
    }

    /* ---------------- 触摸 ---------------- */

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                downX = lastX = e.getX();
                downY = lastY = e.getY();
                dragging = false;
                PointF w0 = toWorld(e.getX(), e.getY());
                if (tool == T_PEN) {
                    snapshot();
                    draft = new Item();
                    draft.kind = K_STROKE;
                    draft.color = color;
                    draft.width = width;
                    draft.pts.add(w0);
                } else if (tool == T_TEXT) {
                    if (textAsker != null) {
                        final PointF at = w0;
                        textAsker.ask(at, new TextSink() {
                            @Override
                            public void onText(String text) {
                                addText(at, text);
                            }
                        });
                    }
                } else if (tool == T_SYMBOL) {
                    snapshot();
                    Item it = new Item();
                    it.kind = K_SYMBOL;
                    it.sym = symbolName;
                    it.color = color;
                    it.pts.add(w0);
                    items.add(it);
                    invalidate();
                } else if (tool == T_SHAPE) {
                    snapshot();
                    draft = new Item();
                    draft.kind = K_SHAPE;
                    draft.shape = shapeKind;
                    draft.color = color;
                    draft.width = width;
                    draft.pts.add(w0);
                    draft.pts.add(new PointF(w0.x, w0.y));
                } else if (tool == T_WIRE) {
                    if (wireStart == null) {
                        wireStart = w0;
                    } else {
                        snapshot();
                        Item it = new Item();
                        it.kind = K_WIRE;
                        it.color = color;
                        it.width = width;
                        it.pts.addAll(routeWire(wireStart, w0));
                        items.add(it);
                        wireStart = null;
                    }
                    invalidate();
                } else {
                    selected = hitTest(w0);
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (e.getPointerCount() >= 2) {
                    pinchDist = dist(e);
                    pinchCx = (e.getX(0) + e.getX(1)) / 2;
                    pinchCy = (e.getY(0) + e.getY(1)) / 2;
                    draft = null;
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (e.getPointerCount() >= 2) {
                    float d = dist(e);
                    if (pinchDist > 1) {
                        float f = d / pinchDist;
                        float cx = (e.getX(0) + e.getX(1)) / 2;
                        float cy = (e.getY(0) + e.getY(1)) / 2;
                        scale = Math.max(0.3f, Math.min(3f, scale * f));
                        tx = cx - (pinchCx - tx) * f;
                        ty = cy - (pinchCy - ty) * f;
                        pinchCx = cx;
                        pinchCy = cy;
                        invalidate();
                    }
                    pinchDist = d;
                    return true;
                }
                PointF w = toWorld(e.getX(), e.getY());
                if (draft != null) {
                    if (draft.kind == K_STROKE) draft.pts.add(w);
                    else if (draft.pts.size() >= 2) draft.pts.set(1, w);
                    dragging = true;
                    invalidate();
                    return true;
                }
                if (tool == T_SELECT && selected != null) {
                    float dx = (e.getX() - lastX) / scale;
                    float dy = (e.getY() - lastY) / scale;
                    for (PointF p : selected.pts) {
                        p.x += dx;
                        p.y += dy;
                    }
                    dragging = true;
                    invalidate();
                }
                lastX = e.getX();
                lastY = e.getY();
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (draft != null) {
                    if (draft.kind == K_STROKE && draft.pts.size() < 2) {
                        // 点一下就是一个点，不保留
                    } else if (draft.kind == K_SHAPE && draft.pts.size() >= 2) {
                        PointF a = draft.pts.get(0), b = draft.pts.get(1);
                        if (Math.abs(a.x - b.x) < 3 && Math.abs(a.y - b.y) < 3) {
                            draft = null;
                            invalidate();
                            return true;
                        }
                    }
                    if (draft != null) items.add(draft);
                    draft = null;
                    invalidate();
                }
                pinchDist = 0;
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP:
                pinchDist = 0;
                return true;
        }
        return super.onTouchEvent(e);
    }

    private float dist(MotionEvent e) {
        if (e.getPointerCount() < 2) return 0;
        return (float) Math.hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1));
    }

    public PointF toWorld(float sx, float sy) {
        return new PointF((sx - tx) / scale, (sy - ty) / scale);
    }

    private Item hitTest(PointF p) {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).bounds().contains(p.x, p.y)) return items.get(i);
        }
        return null;
    }

    /* ---------------- 直角走线 ---------------- */

    private List<PointF> routeWire(PointF a, PointF b) {
        List<PointF> out = new ArrayList<>();
        out.add(new PointF(a.x, a.y));
        boolean hFirst = Math.abs(b.x - a.x) >= Math.abs(b.y - a.y);
        List<PointF> l1 = new ArrayList<>();
        if (hFirst) {
            l1.add(new PointF(b.x, a.y));
        } else {
            l1.add(new PointF(a.x, b.y));
        }
        l1.add(new PointF(b.x, b.y));

        if (!hitsAnything(a, l1)) {
            out.addAll(l1);
            return out;
        }

        // 换一种折法
        List<PointF> l2 = new ArrayList<>();
        if (hFirst) l2.add(new PointF(a.x, b.y));
        else l2.add(new PointF(b.x, a.y));
        l2.add(new PointF(b.x, b.y));
        if (!hitsAnything(a, l2)) {
            out.addAll(l2);
            return out;
        }

        // 中段绕行
        float mid = hFirst ? (a.x + b.x) / 2 : (a.y + b.y) / 2;
        List<PointF> l3 = new ArrayList<>();
        if (hFirst) {
            l3.add(new PointF(mid, a.y));
            l3.add(new PointF(mid, b.y - 30));
            l3.add(new PointF(b.x, b.y - 30));
        } else {
            l3.add(new PointF(a.x, mid));
            l3.add(new PointF(a.x - 30, mid));
            l3.add(new PointF(a.x - 30, b.y));
        }
        l3.add(new PointF(b.x, b.y));
        out.addAll(l3);
        return out;
    }

    private boolean hitsAnything(PointF a, List<PointF> path) {
        List<Item> solids = new ArrayList<>();
        for (Item it : items) {
            if (it.kind == K_SYMBOL || it.kind == K_SHAPE) solids.add(it);
        }
        if (solids.isEmpty()) return false;
        PointF prev = a;
        for (PointF p : path) {
            for (Item s : solids) {
                RectF r = s.bounds();
                RectF seg = new RectF(Math.min(prev.x, p.x), Math.min(prev.y, p.y),
                        Math.max(prev.x, p.x), Math.max(prev.y, p.y));
                if (RectF.intersects(seg, r)) return true;
            }
            prev = p;
        }
        return false;
    }

    /* ---------------- 导出 PNG ---------------- */

    public Bitmap renderToBitmap() {
        RectF b = null;
        for (Item it : items) {
            RectF r = it.bounds();
            if (b == null) b = new RectF(r);
            else b.union(r);
        }
        if (b == null) b = new RectF(0, 0, 800, 600);
        b.inset(-30, -30);

        int w = (int) Math.min(2400, Math.max(320, b.width()));
        int h = (int) Math.min(2400, Math.max(240, b.height()));
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(0xFFFFFDF8);
        c.translate(-b.left, -b.top);
        for (Item it : items) drawItem(c, it);
        return bmp;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
