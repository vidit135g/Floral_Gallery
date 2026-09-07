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

    /** Slide an AmbientView ground behind the content of a root ViewGroup. */
    public static AmbientView ground(Activity a, ViewGroup root, Soma s) {
        AmbientView existing = root.findViewById(R.id.soma_ground);
        if (existing != null) { existing.setSoma(s); return existing; }
        AmbientView av = new AmbientView(a);
        av.setId(R.id.soma_ground);
        av.setSoma(s);
        root.addView(av, 0, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        a.getWindow().setBackgroundDrawable(new ColorDrawable(s.ground[1]));
        return av;
    }

    /** Style the two-part toolbar wordmark ("Floral" + "gallery") in Fraunces. */
    public static void wordmark(Toolbar toolbar, Soma s, String big, String small) {
        toolbar.setBackgroundColor(Color.TRANSPARENT);
        TextView t1 = toolbar.findViewById(R.id.toolbar_title);
        TextView t2 = toolbar.findViewById(R.id.toolbar_gallery);
        if (t1 != null) {
            t1.setTypeface(Soma.display(toolbar.getContext()));
            t1.setText(big);
            t1.setTextColor(s.ink);
            t1.setLetterSpacing(0.01f);
        }
        if (t2 != null) {
            t2.setTypeface(Soma.serif(toolbar.getContext()));
            t2.setText(small);
            t2.setTextColor(s.inkMute);
        }
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
    }
}
