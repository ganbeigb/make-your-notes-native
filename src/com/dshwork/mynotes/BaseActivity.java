package com.dshwork.mynotes;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

/** 所有页面的基类：统一处理系统栏留白与主题切换后的重建。 */
public class BaseActivity extends Activity {

    private boolean builtDark;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        Ui.padForInsets(view);
        builtDark = Ui.dark;
        Ui.tintSystemBars(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Ui.resolvedDark(this) != builtDark) {
            recreate();
            return;
        }
        Ui.tintSystemBars(this);
    }
}
