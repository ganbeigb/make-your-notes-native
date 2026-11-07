package com.dshwork.mynotes;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

/** 所有页面的基类：统一处理系统栏留白与主题切换后的重建。 */
public class BaseActivity extends Activity {

    private boolean builtDark;
    private float builtScale;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        CrashLog.install(this);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        Ui.padForInsets(view);
        builtDark = Ui.dark;
        builtScale = Ui.scale;
        Ui.tintSystemBars(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Ui.resolvedDark(this) != builtDark) {
            recreate();
            return;
        }
        // 界面字号改了也要重建，否则复用的行还留着旧字号
        float prefScale = new Prefs(this).uiScale();
        if (Math.abs(prefScale - builtScale) > 0.001f) {
            Ui.setScale(prefScale);
            recreate();
            return;
        }
        Ui.tintSystemBars(this);
    }
}
