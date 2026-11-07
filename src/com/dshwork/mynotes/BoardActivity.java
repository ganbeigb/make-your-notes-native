package com.dshwork.mynotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

/** 画板页面：工具栏 + 画布，完成后把画布导出成 PNG 交给调用方。 */
public class BoardActivity extends BaseActivity {

    private BoardView board;
    private LinearLayout shapeRow;
    private LinearLayout symbolRow;
    private LinearLayout dirRow;
    private TextView hintView;
    private android.widget.HorizontalScrollView symbolScroll;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Ui.apply(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 10));

        TextView close = Ui.text(this, "✕", 20, Ui.ACCENT);
        close.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 12), 0);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bar.addView(close);

        TextView title = Ui.text(this, "画板", 17, Ui.INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView undo = Ui.iconButton(this, "↶");
        undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!board.undo()) Toast.makeText(BoardActivity.this, "没有可撤销的", Toast.LENGTH_SHORT).show();
            }
        });
        bar.addView(undo);

        TextView redo = Ui.iconButton(this, "↷");
        redo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!board.redoLast()) Toast.makeText(BoardActivity.this, "没有可重做的", Toast.LENGTH_SHORT).show();
            }
        });
        bar.addView(redo);

        TextView del = Ui.iconButton(this, "🗑");
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!board.deleteSelected()) {
                    Toast.makeText(BoardActivity.this, "先用「选择」点中要删的东西", Toast.LENGTH_SHORT).show();
                }
            }
        });
        bar.addView(del);

        TextView zoomOutBtn = Ui.iconButton(this, "−");
        zoomOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                board.zoomBy(0.8f);
            }
        });
        bar.addView(zoomOutBtn);

        TextView zoomInBtn = Ui.iconButton(this, "＋");
        zoomInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                board.zoomBy(1.25f);
            }
        });
        bar.addView(zoomInBtn);

        TextView resetBtn = Ui.iconButton(this, "⤢");
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                board.resetView();
            }
        });
        bar.addView(resetBtn);

        TextView clear = Ui.iconButton(this, "✕");
        clear.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(BoardActivity.this)
                        .setTitle("清空画布")
                        .setMessage("会删掉画布上所有内容（可撤销）。")
                        .setPositiveButton("清空", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                board.clearAll();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
        bar.addView(clear);

        TextView done = Ui.button(this, "完成");
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishWithBitmap();
            }
        });
        bar.addView(done);
        root.addView(bar);

        // 工具行
        HorizontalScrollView toolsScroll = new HorizontalScrollView(this);
        toolsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 6));

        tools.addView(toolButton("↖ 选择", BoardView.T_SELECT, tools));
        tools.addView(toolButton("✎ 画笔", BoardView.T_PEN, tools));
        tools.addView(toolButton("T 文字", BoardView.T_TEXT, tools));
        tools.addView(toolButton("▭ 图形", BoardView.T_SHAPE, tools));
        TextView symTool = toolButton("◫ 元件", BoardView.T_SYMBOL, tools);
        symTool.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSymbolPicker();
            }
        });
        tools.addView(symTool);
        tools.addView(toolButton("⌐ 连线", BoardView.T_WIRE, tools));

        TextView chipTool = toolButton("▣ 芯片", BoardView.T_CHIP, tools);
        chipTool.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askChipSpec();
            }
        });
        tools.addView(chipTool);
        toolsScroll.addView(tools);
        root.addView(toolsScroll);

        // 样式面板：颜色 / 粗细 / 透明度 / 旋转
        LinearLayout styleRow = new LinearLayout(this);
        styleRow.setOrientation(LinearLayout.HORIZONTAL);
        styleRow.setGravity(Gravity.CENTER_VERTICAL);
        styleRow.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 8));

        final int[] colors = {0xFF243029, 0xFF000000, 0xFFC0392B, 0xFF1F6FB2, 0xFFD68910, 0xFF7A7A7A};
        for (final int col : colors) {
            View sw = new View(this);
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setColor(col);
            g.setCornerRadius(Ui.dp(this, 8));
            g.setStroke(Ui.dp(this, 1), 0x33000000);
            sw.setBackground(g);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(Ui.dp(this, 24), Ui.dp(this, 24));
            slp.rightMargin = Ui.dp(this, 8);
            sw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    board.setPenColor(col);
                }
            });
            styleRow.addView(sw, slp);
        }

        final TextView widthLabel = Ui.text(this, "2", 13, Ui.INK);
        widthLabel.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        TextView minus = Ui.text(this, "－", 16, Ui.ACCENT);
        minus.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float w = Math.max(1f, board.penWidth() - 1f);
                board.setPenWidth(w);
                widthLabel.setText(String.valueOf((int) w));
            }
        });
        TextView plus = Ui.text(this, "＋", 16, Ui.ACCENT);
        plus.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float w = Math.min(12f, board.penWidth() + 1f);
                board.setPenWidth(w);
                widthLabel.setText(String.valueOf((int) w));
            }
        });
        styleRow.addView(minus);
        styleRow.addView(widthLabel);
        styleRow.addView(plus);

        final int[] alphas = {255, 180, 110};
        final String[] alphaLabels = {"不透明", "70%", "43%"};
        final int[] alphaIdx = {0};
        final TextView alphaBtn = Ui.chip(this, "不透明", false);
        alphaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alphaIdx[0] = (alphaIdx[0] + 1) % alphas.length;
                board.setPenAlpha(alphas[alphaIdx[0]]);
                alphaBtn.setText(alphaLabels[alphaIdx[0]]);
            }
        });
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.leftMargin = Ui.dp(this, 10);
        styleRow.addView(alphaBtn, alp);

        TextView rot = Ui.chip(this, "旋转 90°", false);
        rot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!board.rotateSelected90()) {
                    Toast.makeText(BoardActivity.this, "先用选择工具点中一个元素", Toast.LENGTH_SHORT).show();
                }
            }
        });
        LinearLayout.LayoutParams rlp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp2.leftMargin = Ui.dp(this, 8);
        styleRow.addView(rot, rlp2);

        TextView scaleUpBtn = Ui.chip(this, "放大", false);
        scaleUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!board.scaleSelected(1.15f)) {
                    Toast.makeText(BoardActivity.this, "先选中要放大的元素", Toast.LENGTH_SHORT).show();
                }
            }
        });
        LinearLayout.LayoutParams suPlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        suPlp.leftMargin = Ui.dp(this, 8);
        styleRow.addView(scaleUpBtn, suPlp);

        TextView scaleDownBtn = Ui.chip(this, "缩小", false);
        scaleDownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!board.scaleSelected(0.87f)) {
                    Toast.makeText(BoardActivity.this, "先选中要缩小的元素", Toast.LENGTH_SHORT).show();
                }
            }
        });
        LinearLayout.LayoutParams sdPlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sdPlp.leftMargin = Ui.dp(this, 8);
        styleRow.addView(scaleDownBtn, sdPlp);

        TextView groupBtn = Ui.chip(this, "组合", false);
        groupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!board.groupSelected()) {
                    Toast.makeText(BoardActivity.this, "先选中两个以上元素（选择工具可框选）", Toast.LENGTH_SHORT).show();
                }
            }
        });
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        glp.leftMargin = Ui.dp(this, 8);
        styleRow.addView(groupBtn, glp);

        TextView ungroupBtn = Ui.chip(this, "拆开", false);
        ungroupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!board.ungroupSelected()) {
                    Toast.makeText(BoardActivity.this, "选中项没有组合", Toast.LENGTH_SHORT).show();
                }
            }
        });
        LinearLayout.LayoutParams uglp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        uglp.leftMargin = Ui.dp(this, 8);
        styleRow.addView(ungroupBtn, uglp);

        TextView boardCfg = Ui.chip(this, "画板设置", false);
        boardCfg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBoardSettings();
            }
        });
        LinearLayout.LayoutParams blp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp2.leftMargin = Ui.dp(this, 8);
        styleRow.addView(boardCfg, blp2);

        android.widget.HorizontalScrollView styleScroll = new android.widget.HorizontalScrollView(this);
        styleScroll.setHorizontalScrollBarEnabled(false);
        styleScroll.addView(styleRow);
        root.addView(styleScroll);

        // 图形 / 元件 选择行
        shapeRow = new LinearLayout(this);
        shapeRow.setOrientation(LinearLayout.HORIZONTAL);
        shapeRow.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 6));
        String[] shapes = {"矩形", "椭圆", "直线"};
        for (int i = 0; i < shapes.length; i++) {
            final int k = i;
            TextView b = chip(shapes[i]);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    board.setShapeKind(k);
                }
            });
            shapeRow.addView(b);
        }
        shapeRow.setVisibility(View.GONE);
        root.addView(shapeRow);

        symbolRow = new LinearLayout(this);
        symbolRow.setOrientation(LinearLayout.HORIZONTAL);
        symbolRow.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 6));
        final java.util.List<SymbolCell> cells = new java.util.ArrayList<>();
        for (int i = 0; i < Symbols.NAMES.length; i++) {
            final String name = Symbols.NAMES[i];
            final SymbolCell cell = new SymbolCell(this, name, Symbols.LABELS[i]);
            cells.add(cell);
            if (i == 0) cell.setSelected(true);
            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (SymbolCell c2 : cells) c2.setSelected(c2 == cell);
                    board.setSymbolName(name);
                }
            });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.rightMargin = Ui.dp(this, 6);
            symbolRow.addView(cell, clp);
        }
        if (false) for (int i = 0; i < Symbols.NAMES.length; i++) {
            final String name = Symbols.NAMES[i];
            TextView b = chip(Symbols.LABELS[i]);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    board.setSymbolName(name);
                }
            });
            symbolRow.addView(b);
        }
        symbolRow.setVisibility(View.GONE);
        android.widget.HorizontalScrollView symScroll = new android.widget.HorizontalScrollView(this);
        symScroll.setHorizontalScrollBarEnabled(false);
        symScroll.addView(symbolRow);
        symScroll.setVisibility(View.GONE);
        root.addView(symScroll);
        symbolScroll = symScroll;

        board = new BoardView(this);
        board.setTextEditor(new BoardView.OnEditText() {
            @Override
            public void edit(String current, final BoardView.TextSink sink) {
                final EditText input = new EditText(BoardActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(current);
                new AlertDialog.Builder(BoardActivity.this)
                        .setTitle("修改文字")
                        .setView(input)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sink.onText(input.getText().toString());
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });

        board.setTextAsker(new BoardView.OnNeedsText() {
            @Override
            public void ask(android.graphics.PointF at, final BoardView.TextSink sink) {
                askTextDialog(sink);
            }
        });
        final LinearLayout dirRow = new LinearLayout(this);
        dirRow.setOrientation(LinearLayout.HORIZONTAL);
        dirRow.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 6));
        dirRow.setVisibility(View.GONE);
        final String[] dirs = {"auto", "up", "right", "down", "left"};
        final String[] dirLabels = {"自动", "↑", "→", "↓", "←"};
        final java.util.List<TextView> dirChips = new java.util.ArrayList<>();
        for (int i = 0; i < dirs.length; i++) {
            final String dir = dirs[i];
            final TextView chipDir = Ui.chip(this, dirLabels[i], i == 0);
            chipDir.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    board.pipeDir = dir;
                    for (int k = 0; k < dirChips.size(); k++) {
                        boolean on = dirs[k].equals(dir);
                        dirChips.get(k).setBackground(Ui.chip(BoardActivity.this, "x", on).getBackground());
                        dirChips.get(k).setTextColor(on ? 0xFFFFFFFF : Ui.INK);
                    }
                }
            });
            dirChips.add(chipDir);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dlp.rightMargin = Ui.dp(this, 6);
            dirRow.addView(chipDir, dlp);
        }
        root.addView(dirRow);
        this.dirRow = dirRow;

        root.addView(board, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        // 底部提示
        final TextView hint = Ui.text(this, "", 11.5f, Ui.MUTED);
        hint.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 8));
        root.addView(hint);
        this.hintView = hint;
        updateHint();

        setContentView(root);
        board.setTool(BoardView.T_PEN);
    }

    private TextView toolButton(String label, final int tool, final LinearLayout row) {
        final TextView b = chip(label);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                board.setTool(tool);
                for (int i = 0; i < row.getChildCount(); i++) {
                    View c = row.getChildAt(i);
                    c.setBackground(i == row.indexOfChild(b) ? Ui.button(BoardActivity.this, "").getBackground() : Ui.card(BoardActivity.this));
                    ((TextView) c).setTextColor(c == b ? 0xFFFFFFFF : Ui.INK);
                }
                shapeRow.setVisibility(tool == BoardView.T_SHAPE ? View.VISIBLE : View.GONE);
                if (symbolScroll != null) symbolScroll.setVisibility(tool == BoardView.T_SYMBOL ? View.VISIBLE : View.GONE);
        if (dirRow != null) dirRow.setVisibility(tool == BoardView.T_WIRE ? View.VISIBLE : View.GONE);
            }
        });
        return b;
    }

    private TextView chip(String label) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(13);
        t.setTextColor(Ui.INK);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(this, 12), Ui.dp(this, 7), Ui.dp(this, 12), Ui.dp(this, 7));
        t.setBackground(Ui.card(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Ui.dp(this, 8);
        t.setLayoutParams(lp);
        return t;
    }

    /** 画板设置：引脚吸附 / 显示网格 / 拖动对齐网格 */
    /** 元件选择：网格面板（一行 4 个） */
    private void showSymbolPicker() {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 8));

        LinearLayout row = null;
        final java.util.List<SymbolCell> cells = new java.util.ArrayList<>();
        for (int i = 0; i < Symbols.NAMES.length; i++) {
            if (i % 4 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row);
            }
            final String id = Symbols.NAMES[i];
            final SymbolCell cell = new SymbolCell(this, id, Symbols.LABELS[i]);
            cells.add(cell);
            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    board.setSymbolName(id);
                    for (SymbolCell c2 : cells) c2.setSelected(c2 == cell);
                    Toast.makeText(BoardActivity.this, "已选：" + Symbols.LABELS[java.util.Arrays.asList(Symbols.NAMES).indexOf(id)],
                            Toast.LENGTH_SHORT).show();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = Ui.dp(this, 4);
            row.addView(cell, lp);
        }

        android.widget.ScrollView sc = new android.widget.ScrollView(this);
        sc.addView(grid);
        new android.app.AlertDialog.Builder(this)
                .setTitle("选择元件（点一下后到画布上放置）")
                .setView(sc)
                .setNegativeButton("关闭", null)
                .show();
    }

    /** 按当前工具更新底部提示（对应网页版 updateHint） */
    private void updateHint() {
        if (hintView == null || board == null) return;
        String s;
        switch (board.getTool()) {
            case BoardView.T_PEN:
                s = "按住拖动自由画；双指缩放平移，粗细与颜色在上方样式行";
                break;
            case BoardView.T_TEXT:
                s = "点画布空白处输入文字；已有的文字用「选择」工具点一下即可修改";
                break;
            case BoardView.T_SHAPE:
                s = "选一种图形后在画布上拖出大小；选中后可旋转、放大缩小";
                break;
            case BoardView.T_SYMBOL:
                s = "点「元件」挑一个符号，再到画布上点放；导线端点会自动吸到引脚";
                break;
            case BoardView.T_WIRE:
                s = "点起点 → 点终点，自动走直角并绕开元件；上方可选出线方向";
                break;
            case BoardView.T_CHIP:
                s = "先设名称与每边引脚数，再点画布放置；引脚可被导线吸附";
                break;
            default:
                s = "点选元素可拖动、旋转、放大；空白处拖动可框选，组合后整组移动";
        }
        hintView.setText(s);
    }

    private void showBoardSettings() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0);

        final android.widget.CheckBox snap = new android.widget.CheckBox(this);
        snap.setText("引脚自动吸附（导线端点对齐引脚）");
        snap.setTextSize(13);
        snap.setChecked(board.snapPins);
        box.addView(snap);

        final android.widget.CheckBox grid = new android.widget.CheckBox(this);
        grid.setText("显示网格（10px）");
        grid.setTextSize(13);
        grid.setChecked(board.showGrid);
        box.addView(grid);

        final android.widget.CheckBox drag = new android.widget.CheckBox(this);
        drag.setText("拖动时对齐网格（按 10px 跳动）");
        drag.setTextSize(13);
        drag.setChecked(board.snapDrag);
        box.addView(drag);

        new android.app.AlertDialog.Builder(this)
                .setTitle("画板设置")
                .setView(box)
                .setPositiveButton("好", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        board.snapPins = snap.isChecked();
                        board.showGrid = grid.isChecked();
                        board.snapDrag = drag.isChecked();
                        board.invalidate();
                    }
                })
                .show();
    }

    private void askChipSpec() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 16);
        box.setPadding(pad, Ui.dp(this, 8), pad, 0);

        final EditText name = new EditText(this);
        name.setHint("芯片名称，例如 NE555");
        box.addView(name);

        final EditText count = new EditText(this);
        count.setHint("每边引脚数（1-6）");
        count.setInputType(InputType.TYPE_CLASS_NUMBER);
        count.setText("3");
        box.addView(count);

        new AlertDialog.Builder(this)
                .setTitle("放置芯片")
                .setView(box)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int n = 3;
                        try {
                            n = Integer.parseInt(count.getText().toString().trim());
                        } catch (Exception ignored) {
                        }
                        board.setChipSpec(name.getText().toString(), n);
                        Toast.makeText(BoardActivity.this, "点画布放置芯片", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void askTextDialog(final BoardView.TextSink sink) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
                .setTitle("添加文字")
                .setView(input)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sink.onText(input.getText().toString());
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void finishWithBitmap() {
        if (board.isEmpty()) {
            Toast.makeText(this, "画布是空的", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Bitmap bmp = board.renderToBitmap();
            File f = new File(getCacheDir(), "board_" + System.currentTimeMillis() + ".png");
            FileOutputStream fos = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            bmp.recycle();
            Intent data = new Intent();
            data.putExtra("path", f.getAbsolutePath());
            setResult(RESULT_OK, data);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
