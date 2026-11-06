package com.dshwork.mynotes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** 思维导图：Canvas 自绘树形图，支持双指缩放、单指拖动、点节点跳转。 */
public class MindMapView extends View {

    public interface OnNodeClick {
        void onClick(String noteId);
    }

    static class Node {
        String id;
        String label;
        int kind; // 0 科目，1 分类，2 知识点
        List<Node> children = new ArrayList<>();
        float x, y, w, h;
        boolean collapsed;
    }

    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillRoot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillCat = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillNote = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Node rootNode;
    private float scale = 1f;
    private float tx = 0f;
    private float ty = 0f;
    private boolean fitted = false;

    private float downX, downY, lastX, lastY;
    private float pinchDist;
    private float pinchCx, pinchCy;
    private boolean moved;
    private OnNodeClick listener;

    public MindMapView(Context c) {
        super(c);
        textPaint.setTextSize(Ui.dp(c, 13));
        textPaint.setColor(Ui.INK);
        linePaint.setColor(Ui.LINE);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(Ui.dp(c, 1.4f));
        nodePaint.setStyle(Paint.Style.STROKE);
        nodePaint.setStrokeWidth(Ui.dp(c, 1.2f));
        nodePaint.setColor(Ui.LINE);

        fillRoot.setColor(Ui.ACCENT);
        fillCat.setColor(Ui.ACCENT_SOFT);
        fillNote.setColor(Ui.CARD);
        setBackgroundColor(Ui.BG);
    }

    public void setOnNodeClick(OnNodeClick l) {
        listener = l;
    }

    public void setTree(Node root) {
        rootNode = root;
        fitted = false;
        layoutTree(root, 0, new int[]{0});
        requestLayout();
        invalidate();
    }

    /* ---------------- 布局 ---------------- */

    private float measureWidth(Node n) {
        String s = n.label == null ? "" : n.label;
        float tw = textPaint.measureText(s);
        float max = Ui.dp(getContext(), 190);
        float min = Ui.dp(getContext(), 76);
        n.w = Math.min(max, Math.max(min, tw + Ui.dp(getContext(), 26)));
        n.h = n.kind == 0 ? Ui.dp(getContext(), 46) : Ui.dp(getContext(), 36);
        return n.w;
    }

    private void layoutTree(Node n, int depth, int[] cursor) {
        measureWidth(n);
        n.x = depth * Ui.dp(getContext(), 210);
        if (n.collapsed || n.children.isEmpty()) {
            n.y = cursor[0] * Ui.dp(getContext(), 58);
            cursor[0]++;
            return;
        }
        int before = cursor[0];
        for (Node c : n.children) layoutTree(c, depth + 1, cursor);
        float sum = 0;
        for (Node c : n.children) sum += c.y;
        n.y = sum / n.children.size();
        if (cursor[0] == before) cursor[0]++;
    }

    /* ---------------- 绘制 ---------------- */

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (rootNode == null) return;
        if (!fitted) {
            fit();
            fitted = true;
        }
        canvas.save();
        canvas.translate(tx, ty);
        canvas.scale(scale, scale);

        drawEdges(canvas, rootNode);
        drawNodes(canvas, rootNode);

        canvas.restore();
    }

    private void drawEdges(Canvas c, Node n) {
        for (Node ch : n.children) {
            float x1 = n.x + n.w;
            float y1 = n.y + n.h / 2;
            float x2 = ch.x;
            float y2 = ch.y + ch.h / 2;
            float mid = (x1 + x2) / 2;
            Path p = new Path();
            p.moveTo(x1, y1);
            p.cubicTo(mid, y1, mid, y2, x2, y2);
            c.drawPath(p, linePaint);
            drawEdges(c, ch);
        }
    }

    private void drawNodes(Canvas c, Node n) {
        RectF r = new RectF(n.x, n.y, n.x + n.w, n.y + n.h);
        Paint fill = n.kind == 0 ? fillRoot : n.kind == 1 ? fillCat : fillNote;
        float rad = (n.kind == 0 ? Ui.dp(getContext(), 23) : Ui.dp(getContext(), 18));
        c.drawRoundRect(r, rad, rad, fill);
        if (n.kind != 0) c.drawRoundRect(r, rad, rad, nodePaint);

        if (flashNode == n) {
            Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
            glow.setStyle(Paint.Style.STROKE);
            glow.setStrokeWidth(Ui.dp(getContext(), 2));
            glow.setColor(Ui.ACCENT);
            c.drawRoundRect(new RectF(r.left - 3, r.top - 3, r.right + 3, r.bottom + 3),
                    rad + 3, rad + 3, glow);
        }

        TextPaint tp = new TextPaint(textPaint);
        tp.setColor(n.kind == 0 ? Color.WHITE : Ui.INK);
        tp.setTextSize(n.kind == 0 ? Ui.dp(getContext(), 14) : Ui.dp(getContext(), 12.5f));
        String label = TextUtils.ellipsize(n.label == null ? "" : n.label, tp,
                n.w - Ui.dp(getContext(), 18), TextUtils.TruncateAt.END).toString();
        float ty2 = n.y + n.h / 2 - (tp.descent() + tp.ascent()) / 2;
        c.drawText(label, n.x + Ui.dp(getContext(), 9), ty2, tp);

        if (n.collapsed && !n.children.isEmpty()) {
            int count = countDescendants(n);
            Paint badge = new Paint(Paint.ANTI_ALIAS_FLAG);
            badge.setColor(Ui.ACCENT);
            float brad = Ui.dp(getContext(), 11);
            float bx = n.x + n.w + brad + 4;
            float by = n.y + n.h / 2;
            c.drawCircle(bx, by, brad, badge);
            Paint num = new Paint(textPaint);
            num.setColor(0xFFFFFFFF);
            num.setTextSize(Ui.dp(getContext(), 11));
            num.setTextAlign(Paint.Align.CENTER);
            c.drawText(String.valueOf(count), bx, by + Ui.dp(getContext(), 4), num);
        }

        for (Node ch : n.children) drawNodes(c, ch);
    }

    /* ---------------- 缩放 / 平移 ---------------- */

    private void fit() {
        RectF b = bounds(rootNode, new RectF(Float.MAX_VALUE, Float.MAX_VALUE, -1, -1));
        if (b.width() <= 0 || b.height() <= 0) return;
        float vw = getWidth() - Ui.dp(getContext(), 24);
        float vh = getHeight() - Ui.dp(getContext(), 24);
        float s = Math.min(vw / b.width(), vh / b.height());
        scale = Math.max(0.25f, Math.min(1.6f, s));
        tx = Ui.dp(getContext(), 12) - b.left * scale;
        ty = Ui.dp(getContext(), 12) - b.top * scale;
    }

    private int countDescendants(Node n) {
        int total = 0;
        for (Node c : n.children) total += 1 + countDescendants(c);
        return total;
    }

    private RectF bounds(Node n, RectF acc) {
        acc.left = Math.min(acc.left, n.x);
        acc.top = Math.min(acc.top, n.y);
        acc.right = Math.max(acc.right, n.x + n.w);
        acc.bottom = Math.max(acc.bottom, n.y + n.h);
        for (Node c : n.children) bounds(c, acc);
        return acc;
    }

    public android.graphics.Bitmap renderToBitmap() {
        if (rootNode == null) return null;
        RectF b = bounds(rootNode, new RectF(Float.MAX_VALUE, Float.MAX_VALUE, -1, -1));
        b.inset(-40, -40);
        int w = (int) Math.min(2400, Math.max(320, b.width()));
        int h = (int) Math.min(2400, Math.max(240, b.height()));
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(Ui.BG);
        c.translate(-b.left, -b.top);
        drawEdges(c, rootNode);
        drawNodes(c, rootNode);
        return bmp;
    }

    public void zoomBy(float factor) {
        scale = Math.max(0.25f, Math.min(3f, scale * factor));
        invalidate();
    }

    public void resetView() {
        fitted = false;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = e.getX();
                downY = lastY = e.getY();
                moved = false;
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (e.getPointerCount() >= 2) {
                    pinchDist = dist(e);
                    pinchCx = (e.getX(0) + e.getX(1)) / 2;
                    pinchCy = (e.getY(0) + e.getY(1)) / 2;
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (e.getPointerCount() >= 2) {
                    float d = dist(e);
                    if (pinchDist > 1) {
                        float f = d / pinchDist;
                        scale = Math.max(0.25f, Math.min(3f, scale * f));
                        float cx = (e.getX(0) + e.getX(1)) / 2;
                        float cy = (e.getY(0) + e.getY(1)) / 2;
                        tx = cx - (pinchCx - tx) * f;
                        ty = cy - (pinchCy - ty) * f;
                        pinchCx = cx;
                        pinchCy = cy;
                        moved = true;
                        invalidate();
                    }
                    pinchDist = d;
                    return true;
                }
                tx += e.getX() - lastX;
                ty += e.getY() - lastY;
                lastX = e.getX();
                lastY = e.getY();
                if (Math.hypot(e.getX() - downX, e.getY() - downY) > Ui.dp(getContext(), 8)) moved = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (!moved) tap(e.getX(), e.getY());
                return true;
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

    private void tap(float sx, float sy) {
        if (rootNode == null) return;
        float wx = (sx - tx) / scale;
        float wy = (sy - ty) / scale;
        Node hit = hit(rootNode, wx, wy);
        if (hit == null) return;
        flash(hit);
        if (hit.kind == 2) {
            if (listener != null) listener.onClick(hit.id);
            return;
        }
        if (!hit.children.isEmpty()) {
            hit.collapsed = !hit.collapsed;
            layoutTree(rootNode, 0, new int[]{0});
            requestLayout();
            invalidate();
        }
    }

    private Node flashNode;

    private void flash(Node n) {
        flashNode = n;
        invalidate();
        postInvalidateDelayed(180);
    }

    private Node hit(Node n, float x, float y) {
        if (x >= n.x && x <= n.x + n.w && y >= n.y && y <= n.y + n.h) return n;
        for (Node c : n.children) {
            Node r = hit(c, x, y);
            if (r != null) return r;
        }
        return null;
    }
}
