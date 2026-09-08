package com.absolute.floral.soma;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.absolute.floral.R;
import com.absolute.floral.data.Settings;
import com.absolute.floral.themes.BlackTheme;
import com.absolute.floral.themes.Theme;

/** Glue that drapes the Soma language over the app's existing themed activities. */
public final class SomaSkin {

    private SomaSkin() {}

    /** Map the user's chosen Theme onto a Soma palette for right now. */
    public static Soma read(Context a) {
        Theme t = Settings.getInstance(a).getThemeInstance(a);
        int base;
        if (t instanceof BlackTheme) base = Soma.BASE_OLED;
        else if (t.isBaseLight())    base = Soma.BASE_LIGHT;
        else                          base = Soma.BASE_DARK;
        return Soma.forNow(a, base);
    }

    /** Paint the window + root a flat Material surface colour. */
    public static Object ground(Activity a, ViewGroup root, Soma s) {
        root.setBackgroundColor(s.ground[0]);
        a.getWindow().setBackgroundDrawable(new ColorDrawable(s.ground[0]));
        a.getWindow().setStatusBarColor(s.ground[0]);
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            try { a.getWindow().setNavigationBarColor(s.ground[0]); } catch (Exception ignored) {}
        }
        return null;
    }

    /** Left-aligned Google-Photos-style screen title. */
    public static void wordmark(Toolbar toolbar, Soma s, String big, String small) {
        toolbar.setBackgroundColor(s.ground[0]);
        TextView t1 = toolbar.findViewById(R.id.toolbar_title);
        TextView t2 = toolbar.findViewById(R.id.toolbar_gallery);
        if (t1 != null) {
            t1.setTypeface(Soma.display(toolbar.getContext()), android.graphics.Typeface.BOLD);
            t1.setText(big);
            t1.setTextColor(s.ink);
            t1.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 26f);
            t1.setLetterSpacing(-0.02f);
            // only when the title is a direct Toolbar child (older layouts) —
            // the two-line layouts wrap it in a LinearLayout
            if (t1.getParent() == toolbar) {
                Toolbar.LayoutParams lp = new Toolbar.LayoutParams(
                        Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
                lp.gravity = android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL;
                t1.setLayoutParams(lp);
            }
        }
        // no navigation icon on the tab screens — GP keeps the title flush-left
        toolbar.setNavigationIcon(null);
        toolbar.setContentInsetStartWithNavigation(0);
        toolbar.setContentInsetsRelative((int) Soma.dp(toolbar.getContext(), 16), 0);
        if (t2 != null) {
            t2.setText("");
            t2.setVisibility(View.GONE);
        }
        subtitle(toolbar, s, small);
    }

    /** The small date-range line under a wordmark title (Apple-Photos Library). */
    public static void subtitle(Toolbar toolbar, Soma s, String text) {
        TextView sub = toolbar.findViewById(R.id.toolbar_subtitle);
        if (sub == null) return;
        if (text == null || text.isEmpty()) { sub.setVisibility(View.GONE); return; }
        sub.setTypeface(Soma.body(toolbar.getContext()));
        sub.setText(text);
        sub.setTextColor(s.inkMute);
        sub.setVisibility(View.VISIBLE);
    }

    /** Recolour a Toolbar's icons/overflow to the ink colour. */
    public static void toolbarIcons(Toolbar toolbar, Soma s) {
        if (toolbar.getNavigationIcon() != null)
            toolbar.getNavigationIcon().setTint(s.ink);
        if (toolbar.getOverflowIcon() != null)
            toolbar.getOverflowIcon().setTint(s.ink);
        for (int i = 0; i < toolbar.getMenu().size(); i++) {
            android.graphics.drawable.Drawable d = toolbar.getMenu().getItem(i).getIcon();
            if (d != null) d.setTint(s.ink);
        }
    }

    public static void statusBarIcons(Activity a, Soma s) {
        View decor = a.getWindow().getDecorView();
        int f = decor.getSystemUiVisibility();
        if (s.lightBase) f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        else f &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        decor.setSystemUiVisibility(f);
        try {
            a.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            a.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            a.getWindow().setStatusBarColor(s.ground[0]);
            if (android.os.Build.VERSION.SDK_INT >= 27) a.getWindow().setNavigationBarColor(s.ground[0]);
        } catch (Exception ignored) {}
    }
}
