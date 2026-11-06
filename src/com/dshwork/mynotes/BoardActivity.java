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
public class BoardActivity extends Activity {

    private BoardView board;
    private LinearLayout shapeRow;
    private LinearLayout symbolRow;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 6));

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

        TextView undo = Ui.text(this, "撤销", 14, Ui.ACCENT);
        undo.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!board.undo()) Toast.makeText(BoardActivity.this, "没有可撤销的", Toast.LENGTH_SHORT).show();
            }
        });
        bar.addView(undo);

        TextView redo = Ui.text(this, "重做", 14, Ui.ACCENT);
        redo.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        redo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!board.redoLast()) Toast.makeText(BoardActivity.this, "没有可重做的", Toast.LENGTH_SHORT).show();
            }
        });
        bar.addView(redo);

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

        tools.addView(toolButton("选择", BoardView.T_SELECT, tools));
        tools.addView(toolButton("画笔", BoardView.T_PEN, tools));
        tools.addView(toolButton("文字", BoardView.T_TEXT, tools));
        tools.addView(toolButton("图形", BoardView.T_SHAPE, tools));
        tools.addView(toolButton("元件", BoardView.T_SYMBOL, tools));
        tools.addView(toolButton("连线", BoardView.T_WIRE, tools));

        TextView chipTool = toolButton("芯片", BoardView.T_CHIP, tools);
        chipTool.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askChipSpec();
            }
        });
        tools.addView(chipTool);
        toolsScroll.addView(tools);
        root.addView(toolsScroll);

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
        for (int i = 0; i < Symbols.NAMES.length; i++) {
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
        root.addView(symbolRow);

        board = new BoardView(this);
        board.setTextAsker(new BoardView.OnNeedsText() {
            @Override
            public void ask(android.graphics.PointF at, final BoardView.TextSink sink) {
                askTextDialog(sink);
            }
        });
        root.addView(board, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        // 底部提示
        TextView hint = Ui.text(this, "画笔自由画；连线点起点再点终点自动走直角；双指缩放平移", 11.5f, Ui.INK2);
        hint.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 8));
        root.addView(hint);

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
                symbolRow.setVisibility(tool == BoardView.T_SYMBOL ? View.VISIBLE : View.GONE);
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
