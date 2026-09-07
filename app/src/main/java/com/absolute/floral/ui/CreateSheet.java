package com.absolute.floral.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.google.android.material.bottomsheet.BottomSheetDialog;

/** The Google-Photos "Create" menu — collage, animation, highlight. */
public final class CreateSheet {

    private CreateSheet() {}

    public static void show(final Activity a) {
        Soma s = SomaSkin.read(a);
        BottomSheetDialog dlg = new BottomSheetDialog(a);

        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = d(a, 20);
        col.setPadding(pad, d(a, 14), pad, d(a, 26));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(s.lightBase ? 0xFFFDFBF7 : 0xFF17140F);
        float r = d(a, 26);
        bg.setCornerRadii(new float[]{ r, r, r, r, 0, 0, 0, 0 });
        col.setBackground(bg);

        TextView h = new TextView(a);
        h.setText("Create");
        h.setTypeface(Soma.display(a));
        h.setTextSize(22);
        h.setTextColor(s.ink);
        h.setPadding(d(a, 6), d(a, 6), 0, d(a, 12));
        col.addView(h);

        col.addView(row(a, s, "Collage", "Combine 2–9 photos into one", () -> {
            dlg.dismiss();
            Intent i = new Intent(a, CollageActivity.class);
            a.startActivity(i);
        }));
        col.addView(row(a, s, "Animation", "Turn a burst into a moving picture — soon", () -> {
            dlg.dismiss();
            android.widget.Toast.makeText(a, "Animations are coming soon", android.widget.Toast.LENGTH_SHORT).show();
        }));
        col.addView(row(a, s, "Highlight film", "Auto-picked moments set to motion — soon", () -> {
            dlg.dismiss();
            android.widget.Toast.makeText(a, "Highlight films are coming soon", android.widget.Toast.LENGTH_SHORT).show();
        }));

        dlg.setContentView(col);
        if (dlg.getWindow() != null)
            dlg.getWindow().findViewById(com.google.android.material.R.id.design_bottom_sheet)
                    .setBackgroundColor(Color.TRANSPARENT);
        dlg.show();
    }

    private static View row(Activity a, Soma s, String title, String sub, Runnable onTap) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(d(a, 6), d(a, 14), d(a, 6), d(a, 14));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClickable(true);
        row.setOnClickListener(v -> onTap.run());

        TextView t = new TextView(a);
        t.setText(title);
        t.setTypeface(Soma.body(a));
        t.setTextSize(16);
        t.setTextColor(s.ink);
        row.addView(t);

        TextView st = new TextView(a);
        st.setText(sub);
        st.setTypeface(Soma.bodyRegular(a));
        st.setTextSize(12);
        st.setTextColor(s.inkMute);
        row.addView(st);
        return row;
    }

    private static int d(Activity a, float v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }
}
