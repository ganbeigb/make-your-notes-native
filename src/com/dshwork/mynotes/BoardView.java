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
    public static final int T_CHIP = 6;

    static final int K_STROKE = 0;
    static final int K_SHAPE = 1;
    static final int K_TEXT = 2;
    static final int K_SYMBOL = 3;
    static final int K_WIRE = 4;
    static final int K_CHIP = 5;

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
        float w, h;
        String chipName;
        List<float[]> pins = new ArrayList<>(); // {side 0L 1R 2T 3B, t 0..1}

        Item copy() {
            Item it = new Item();
            it.kind = kind;
            for (PointF p : pts) it.pts.add(new PointF(p.x, p.y));
            it.text = text;
            it.sym = sym;
            it.shape = shape;
            it.color = color;
            it.width = width;
            it.w = w;
            it.h = h;
            it.chipName = chipName;
            for (float[] pin : pins) it.pins.add(new float[]{pin[0], pin[1]});
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
            if (kind == K_CHIP && !pts.isEmpty()) {
                r.left = Math.min(r.left, pts.get(0).x - 26);
                r.right = Math.max(r.right, pts.get(0).x + w + 26);
                r.bottom = Math.max(r.bottom, pts.get(0).y + h);
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
    private int selSeg = -1;
    private int dragMode = 0; // 0 无 1 整体 2 段 3 顶点
    private int dragIndex = -1;
    private OnNeedsText textAsker;
    private String chipName = "IC";
    private int chipPerSide = 3;

    public BoardView(Context c) {
        super(c);
        setBackgroundColor(Ui.dark ? 0xFF141A16 : 0xFFFFFDF8);
        gridPaint.setColor(Ui.dark ? 0xFF24302A : 0xFFEFE8D8);
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

    public void setChipSpec(String name, int perSide) {
        chipName = (name == null || name.trim().isEmpty()) ? "IC" : name.trim();
        chipPerSide = Math.max(1, Math.min(6, perSide));
        tool = T_CHIP;
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

    /** 删除：选中段只删这一段，剩下的拆成两条导线 */
    public boolean deleteSelected() {
        if (selected == null) return false;
        snapshot();
        if (selected.kind == K_WIRE && selSeg >= 0 && selected.pts.size() > 2) {
            List<PointF> left = new ArrayList<>(selected.pts.subList(0, selSeg + 1));
            List<PointF> right = new ArrayList<>(selected.pts.subList(selSeg + 1, selected.pts.size()));
            int idx = items.indexOf(selected);
            if (idx >= 0) items.remove(idx);
            if (left.size() >= 2) items.add(makeWire(left));
            if (right.size() >= 2) items.add(makeWire(right));
        } else {
            items.remove(selected);
        }
        selected = null;
        selSeg = -1;
        dragMode = 0;
        invalidate();
        return true;
    }

    private Item makeWire(List<PointF> pts) {
        Item w = new Item();
        w.kind = K_WIRE;
        w.color = color;
        w.width = width;
        for (PointF p : pts) w.pts.add(new PointF(p.x, p.y));
        return w;
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
            if (selSeg >= 0 && selSeg + 1 < selected.pts.size()) {
                PointF a = selected.pts.get(selSeg), b = selected.pts.get(selSeg + 1);
                p.setStrokeWidth((selected.width + 5) / Math.max(0.4f, scale * 0.9f));
                p.setAlpha(90);
                c.drawLine(a.x, a.y, b.x, b.y, p);
                p.setAlpha(255);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(a.x, a.y, 4 / scale, p);
                c.drawCircle(b.x, b.y, 4 / scale, p);
            } else {
                p.setStrokeWidth(1.5f / scale);
                c.drawRect(selected.bounds(), p);
            }
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
            case K_CHIP: {
                if (it.pts.isEmpty()) return;
                PointF o = it.pts.get(0);
                RectF r = new RectF(o.x, o.y, o.x + it.w, o.y + it.h);
                c.drawRoundRect(r, 6, 6, p);
                Paint name = new Paint(textPaint);
                name.setColor(it.color);
                name.setTextSize(13);
                name.setTextAlign(Paint.Align.CENTER);
                c.drawText(it.chipName == null ? "IC" : it.chipName, o.x + it.w / 2, o.y + it.h / 2 + 5, name);
                name.setTextAlign(Paint.Align.LEFT);
                int num = 0;
                for (float[] pin : it.pins) {
                    num++;
                    float px = pinX(it, (int) pin[0], pin[1]);
                    float py = pinY(it, (int) pin[0], pin[1]);
                    float ax = pin[0] == 0 ? o.x : o.x + it.w;
                    float ay = pin[1] * it.h + o.y;
                    if (pin[0] >= 2) {
                        ax = pin[1] * it.w + o.x;
                        ay = pin[0] == 2 ? o.y : o.y + it.h;
                    }
                    c.drawLine(ax, ay, px, py, p);
                    c.drawCircle(px, py, 2.2f, p);
                    Paint small = new Paint(textPaint);
                    small.setTextSize(8);
                    small.setColor(0xFF6B7A72);
                    float txPos = pin[0] == 0 ? px - 12 : pin[0] == 1 ? px + 3 : px - 3;
                    float tyPos = pin[0] == 0 || pin[0] == 1 ? py + 3 : pin[0] == 2 ? py - 3 : py + 8;
                    c.drawText(String.valueOf(num), txPos, tyPos, small);
                }
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
                } else if (tool == T_CHIP) {
                    snapshot();
                    Item it = new Item();
                    it.kind = K_CHIP;
                    it.chipName = chipName;
                    it.color = color;
                    it.w = 160;
                    it.h = Math.max(70, 30 + chipPerSide * 26);
                    it.pts.add(new PointF(w0.x, w0.y));
                    for (int i = 0; i < chipPerSide; i++) {
                        float tt = (i + 1f) / (chipPerSide + 1);
                        it.pins.add(new float[]{0, tt});
                        it.pins.add(new float[]{1, tt});
                    }
                    items.add(it);
                    invalidate();
                } else if (tool == T_WIRE) {
                    PointF snapped = snapToPin(w0);
                    if (wireStart == null) {
                        wireStart = snapped;
                    } else {
                        snapshot();
                        Item it = new Item();
                        it.kind = K_WIRE;
                        it.color = color;
                        it.width = width;
                        it.pts.addAll(routeWire(wireStart, snapped));
                        items.add(it);
                        wireStart = null;
                    }
                    invalidate();
                } else {
                    selected = hitTest(w0);
                    selSeg = -1;
                    dragMode = 0;
                    dragIndex = -1;
                    if (selected != null && selected.kind == K_WIRE) {
                        float tol = Math.max(8, selected.width + 5) / 1f;
                        int seg = hitSegment(selected, w0, tol);
                        int vtx = hitVertex(selected, w0, 10);
                        if (seg >= 0) {
                            selSeg = seg;
                            dragMode = vtx >= 0 ? 3 : 2;
                            dragIndex = vtx >= 0 ? vtx : seg;
                        }
                    } else if (selected != null) {
                        dragMode = 1;
                    }
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
                    if (dragMode == 2 && dragIndex >= 0 && dragIndex + 1 < selected.pts.size()) {
                        PointF a = selected.pts.get(dragIndex), b = selected.pts.get(dragIndex + 1);
                        boolean horizontal = Math.abs(a.y - b.y) < 0.01;
                        if (horizontal) {
                            a.y += dy;
                            b.y += dy;
                        } else {
                            a.x += dx;
                            b.x += dx;
                        }
                    } else if (dragMode == 3 && dragIndex >= 0 && dragIndex < selected.pts.size()) {
                        PointF v = selected.pts.get(dragIndex);
                        PointF prev = dragIndex > 0 ? selected.pts.get(dragIndex - 1) : null;
                        PointF next = dragIndex + 1 < selected.pts.size() ? selected.pts.get(dragIndex + 1) : null;
                        v.x += dx;
                        v.y += dy;
                        // 相邻段保持直角：把原来同坐标的那一维跟着改
                        if (prev != null) {
                            boolean wasH = Math.abs(prev.y - v.y + dy) < 0.01;
                            boolean wasV = Math.abs(prev.x - v.x + dx) < 0.01;
                            if (wasH && !wasV) prev.y = v.y;
                            else if (wasV && !wasH) prev.x = v.x;
                        }
                        if (next != null) {
                            boolean wasH = Math.abs(next.y - v.y + dy) < 0.01;
                            boolean wasV = Math.abs(next.x - v.x + dx) < 0.01;
                            if (wasH && !wasV) next.y = v.y;
                            else if (wasV && !wasH) next.x = v.x;
                        }
                    } else {
                        for (PointF p : selected.pts) {
                            p.x += dx;
                            p.y += dy;
                        }
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

    static float pinX(Item chip, int side, float t01) {
        PointF o = chip.pts.get(0);
        if (side == 0) return o.x - 22;
        if (side == 1) return o.x + chip.w + 22;
        return o.x + t01 * chip.w;
    }

    static float pinY(Item chip, int side, float t01) {
        PointF o = chip.pts.get(0);
        if (side == 2) return o.y - 22;
        if (side == 3) return o.y + chip.h + 22;
        return o.y + t01 * chip.h;
    }

    /** 把所有芯片引脚摊平，供导线吸附 */
    private List<PointF> allPins() {
        List<PointF> out = new ArrayList<>();
        for (Item it : items) {
            if (it.kind != K_CHIP) continue;
            for (float[] pin : it.pins) {
                out.add(new PointF(pinX(it, (int) pin[0], pin[1]), pinY(it, (int) pin[0], pin[1])));
            }
        }
        return out;
    }

    private PointF snapToPin(PointF p) {
        PointF best = null;
        float bestD = 18;
        for (PointF q : allPins()) {
            float d = (float) Math.hypot(q.x - p.x, q.y - p.y);
            if (d < bestD) {
                bestD = d;
                best = q;
            }
        }
        return best == null ? p : best;
    }

    public PointF toWorld(float sx, float sy) {
        return new PointF((sx - tx) / scale, (sy - ty) / scale);
    }

    /** 点到折线的最短距离与所在段 */
    private int hitSegment(Item it, PointF p, float tol) {
        int best = -1;
        float bestD = tol;
        for (int i = 0; i + 1 < it.pts.size(); i++) {
            PointF a = it.pts.get(i), b = it.pts.get(i + 1);
            float dx = b.x - a.x, dy = b.y - a.y;
            float len2 = dx * dx + dy * dy;
            float t01 = len2 == 0 ? 0 : ((p.x - a.x) * dx + (p.y - a.y) * dy) / len2;
            t01 = Math.max(0, Math.min(1, t01));
            float px = a.x + dx * t01, py = a.y + dy * t01;
            float d = (float) Math.hypot(p.x - px, p.y - py);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    private int hitVertex(Item it, PointF p, float tol) {
        int best = -1;
        float bestD = tol;
        for (int i = 0; i < it.pts.size(); i++) {
            PointF q = it.pts.get(i);
            float d = (float) Math.hypot(q.x - p.x, q.y - p.y);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
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

        List<PointF> astar = astarPath(a, b);
        if (astar != null) return astar;

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

    private static final int STEP = 10;

    private List<RectF> obstacles() {
        List<RectF> out = new ArrayList<>();
        for (Item it : items) {
            if (it.kind == K_SYMBOL || it.kind == K_CHIP || it.kind == K_SHAPE) {
                RectF r = it.bounds();
                r.inset(-4, -4);
                out.add(r);
            }
        }
        return out;
    }

    /** 网格 A*，返回除起点外的折点；无解返回 null */
    private List<PointF> astarPath(PointF a, PointF b) {
        List<RectF> obs = obstacles();
        if (obs.isEmpty()) return null;

        float pad = 90;
        int minX = (int) Math.floor((Math.min(a.x, b.x) - pad) / STEP) * STEP;
        int minY = (int) Math.floor((Math.min(a.y, b.y) - pad) / STEP) * STEP;
        int maxX = (int) Math.ceil((Math.max(a.x, b.x) + pad) / STEP) * STEP;
        int maxY = (int) Math.ceil((Math.max(a.y, b.y) + pad) / STEP) * STEP;

        int cols = (maxX - minX) / STEP + 1;
        int rows = (maxY - minY) / STEP + 1;
        if (cols < 3 || rows < 3 || cols * rows > 40000) return null;

        boolean[] blocked = new boolean[cols * rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float x = minX + c * STEP;
                float y = minY + r * STEP;
                for (RectF o : obs) {
                    if (x > o.left && x < o.right && y > o.top && y < o.bottom) {
                        blocked[r * cols + c] = true;
                        break;
                    }
                }
            }
        }

        int sc = Math.round((a.x - minX) / STEP), sr = Math.round((a.y - minY) / STEP);
        int gc = Math.round((b.x - minX) / STEP), gr = Math.round((b.y - minY) / STEP);
        sc = Math.max(0, Math.min(cols - 1, sc));
        sr = Math.max(0, Math.min(rows - 1, sr));
        gc = Math.max(0, Math.min(cols - 1, gc));
        gr = Math.max(0, Math.min(rows - 1, gr));
        // 起终点周围放开
        for (int dr = -2; dr <= 2; dr++) {
            for (int dc = -2; dc <= 2; dc++) {
                int c1 = sc + dc, r1 = sr + dr, c2 = gc + dc, r2 = gr + dr;
                if (c1 >= 0 && r1 >= 0 && c1 < cols && r1 < rows) blocked[r1 * cols + c1] = false;
                if (c2 >= 0 && r2 >= 0 && c2 < cols && r2 < rows) blocked[r2 * cols + c2] = false;
            }
        }

        int total = cols * rows;
        float[] g = new float[total];
        int[] came = new int[total];
        boolean[] closed = new boolean[total];
        java.util.Arrays.fill(g, Float.MAX_VALUE);
        java.util.Arrays.fill(came, -1);
        int start = sr * cols + sc, goal = gr * cols + gc;
        g[start] = 0;

        java.util.PriorityQueue<int[]> open = new java.util.PriorityQueue<>(64, new java.util.Comparator<int[]>() {
            @Override
            public int compare(int[] x, int[] y) {
                return Integer.compare(x[1], y[1]);
            }
        });
        open.add(new int[]{start, Math.abs(sc - gc) + Math.abs(sr - gr)});

        int[] dcx = {1, -1, 0, 0};
        int[] dcy = {0, 0, 1, -1};
        boolean found = false;

        while (!open.isEmpty()) {
            int[] cur = open.poll();
            int k = cur[0];
            if (closed[k]) continue;
            if (k == goal) {
                found = true;
                break;
            }
            closed[k] = true;
            int c = k % cols, r = k / cols;
            for (int i = 0; i < 4; i++) {
                int nc = c + dcx[i], nr = r + dcy[i];
                if (nc < 0 || nr < 0 || nc >= cols || nr >= rows) continue;
                int nk = nr * cols + nc;
                if (blocked[nk] || closed[nk]) continue;
                float ng = g[k] + 1;
                if (ng < g[nk]) {
                    g[nk] = ng;
                    came[nk] = k;
                    int h = Math.abs(nc - gc) + Math.abs(nr - gr);
                    open.add(new int[]{nk, (int) (ng + h)});
                }
            }
        }
        if (!found) return null;

        List<int[]> cells = new ArrayList<>();
        int cur = goal;
        while (cur != -1) {
            cells.add(new int[]{cur % cols, cur / cols});
            cur = came[cur];
        }
        java.util.Collections.reverse(cells);

        List<PointF> pts = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            int c = cells.get(i)[0], r = cells.get(i)[1];
            pts.add(new PointF(minX + c * STEP, minY + r * STEP));
        }
        pts.set(0, new PointF(a.x, a.y));
        pts.set(pts.size() - 1, new PointF(b.x, b.y));

        // 去掉共线点
        List<PointF> out = new ArrayList<>();
        out.add(pts.get(0));
        for (int i = 1; i < pts.size() - 1; i++) {
            PointF p0 = out.get(out.size() - 1), p1 = pts.get(i), p2 = pts.get(i + 1);
            boolean sameX = Math.abs(p0.x - p1.x) < 0.5 && Math.abs(p1.x - p2.x) < 0.5;
            boolean sameY = Math.abs(p0.y - p1.y) < 0.5 && Math.abs(p1.y - p2.y) < 0.5;
            if (!sameX && !sameY) out.add(p1);
        }
        out.add(pts.get(pts.size() - 1));
        out.remove(0);
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
        c.drawColor(Ui.dark ? 0xFF141A16 : 0xFFFFFDF8);
        c.translate(-b.left, -b.top);
        for (Item it : items) drawItem(c, it);
        return bmp;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
