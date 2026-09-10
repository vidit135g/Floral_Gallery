package com.absolute.floral.soma;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

/**
 * Bento-styled AlertDialogs — big rounded corners, the Soma surface colour,
 * Poppins, accent-tinted buttons. Drop-in for {@code new AlertDialog.Builder}.
 * Deliberately built on plain AppCompat (not Material) so it works with the
 * app's Theme.AppCompat base.
 */
public final class Dialogs {

    private Dialogs() {}

    public static Builder alert(Context c) {
        return new Builder(c);
    }

    public static class Builder extends AlertDialog.Builder {

        private final Soma soma;
        private final float d;

        Builder(Context c) {
            super(c, com.absolute.floral.R.style.Soma_Dialog);
            this.soma = SomaSkin.read(c);
            this.d = c.getResources().getDisplayMetrics().density;
        }

        private int dp(float v) { return Math.round(v * d); }

        @Override public AlertDialog create() {
            final AlertDialog dialog = super.create();
            dialog.setOnShowListener(x -> style(dialog));
            return dialog;
        }

        @Override public AlertDialog show() {
            AlertDialog dialog = create();
            dialog.show();
            return dialog;
        }

        private void style(AlertDialog dialog) {
            Window w = dialog.getWindow();
            if (w != null) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(soma.surface);
                bg.setCornerRadius(dp(26));
                int inset = dp(20);
                w.setBackgroundDrawable(new InsetDrawable(bg, inset, 0, inset, 0));
                WindowManager.LayoutParams lp = w.getAttributes();
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                w.setAttributes(lp);
            }

            Context c = dialog.getContext();
            Typeface body = Soma.body(c);
            Typeface display = Soma.display(c);

            TextView title = find(dialog,
                    androidx.appcompat.R.id.alertTitle, android.R.id.title);
            if (title != null) {
                title.setTextColor(soma.ink);
                title.setTypeface(display, Typeface.BOLD);
                title.setTextSize(19f);
                title.setLetterSpacing(-0.01f);
            }
            TextView msg = find(dialog, android.R.id.message);
            if (msg != null) {
                msg.setTextColor(soma.inkSoft);
                msg.setTypeface(body);
                msg.setLineSpacing(dp(3), 1f);
            }
            // any other stray label (e.g. list items with a dark default colour)
            View root = dialog.findViewById(androidx.appcompat.R.id.parentPanel);
            if (root instanceof android.view.ViewGroup) recolorText((android.view.ViewGroup) root, title, msg);
            for (int which : new int[]{ AlertDialog.BUTTON_POSITIVE,
                    AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL }) {
                Button b = dialog.getButton(which);
                if (b == null) continue;
                boolean primary = which == AlertDialog.BUTTON_POSITIVE;
                b.setTextColor(primary ? soma.accent : soma.inkMute);
                b.setTypeface(body, primary ? Typeface.BOLD : Typeface.NORMAL);
                b.setAllCaps(false);
                b.setTextSize(14.5f);
                b.setLetterSpacing(0f);
            }
        }

        /** Recolour any TextView the theme left dark (list rows, custom labels) that isn't a button. */
        private void recolorText(android.view.ViewGroup g, View... skip) {
            for (int i = 0; i < g.getChildCount(); i++) {
                View v = g.getChildAt(i);
                boolean skipIt = false;
                for (View s : skip) if (s == v) skipIt = true;
                if (v instanceof android.view.ViewGroup) {
                    recolorText((android.view.ViewGroup) v, skip);
                } else if (!skipIt && v instanceof TextView && !(v instanceof Button)) {
                    TextView t = (TextView) v;
                    int col = t.getCurrentTextColor();
                    // only touch near-opaque dark text
                    if (android.graphics.Color.alpha(col) > 200
                            && (col & 0xFFFFFF) < 0x707070) t.setTextColor(soma.inkSoft);
                }
            }
        }

        private static TextView find(AlertDialog dialog, int... ids) {
            for (int id : ids) {
                if (id == 0) continue;
                View v = dialog.findViewById(id);
                if (v instanceof TextView) return (TextView) v;
            }
            return null;
        }
    }
}
