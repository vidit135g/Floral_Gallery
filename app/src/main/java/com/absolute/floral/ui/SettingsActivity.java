package com.absolute.floral.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.widget.NestedScrollView;

import com.absolute.floral.R;
import com.absolute.floral.bento.LibrarySnapshot;
import com.absolute.floral.data.Settings;
import com.absolute.floral.people.PeopleIndex;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.absolute.floral.themes.Theme;

/**
 * Apple-Photos-style Settings — a hand-built grouped table: profile header,
 * rounded section cards, green switches, blue Reset actions. Only rows that do
 * something real (no inert cloud / HDR / shared-library rows).
 */
public class SettingsActivity extends ThemeableActivity {

    private interface BoolCb { void accept(boolean v); }

    private static final int GREEN = 0xFF34C759;
    /** survives the recreate() a theme change triggers */
    public static boolean sChanged = false;
    private LinearLayout col;
    private Soma s;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        s = SomaSkin.read(this);

        NestedScrollView scroll = new NestedScrollView(this);
        scroll.setId(R.id.root_view);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(s.ground[0]);

        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(10), dp(16), dp(40));
        scroll.addView(col, new NestedScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            col.setPadding(dp(16), dp(10) + insets.getSystemWindowInsetTop(),
                    dp(16), dp(40) + insets.getSystemWindowInsetBottom());
            return insets;
        });

        buildHeader();
        buildDisplay();
        buildViewOptions();
        buildLibraryReading();
        buildReset();
        setSystemUiFlags();
    }

    /* ------------------------------------------------------------ sections */

    private void buildHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(6), dp(4), dp(18));

        TextView title = new TextView(this);
        title.setText("Settings");
        title.setTypeface(Soma.display(this), Typeface.BOLD);
        title.setTextSize(30);
        title.setTextColor(s.ink);
        row.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView close = new TextView(this);
        close.setText("✕");
        close.setTextSize(17);
        close.setGravity(Gravity.CENTER);
        close.setTextColor(s.ink);
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(s.surfaceStrong);
        close.setBackground(disc);
        int d = dp(34);
        close.setOnClickListener(v -> finish());
        row.addView(close, new LinearLayout.LayoutParams(d, d));
        col.addView(row);

        // profile card
        LinearLayout card = groupCard();
        LinearLayout p = new LinearLayout(this);
        p.setGravity(Gravity.CENTER_VERTICAL);
        p.setPadding(dp(14), dp(14), dp(14), dp(14));

        TextView avatar = new TextView(this);
        avatar.setText(initials());
        avatar.setGravity(Gravity.CENTER);
        avatar.setTextColor(0xFFFFFFFF);
        avatar.setTextSize(18);
        avatar.setTypeface(Soma.body(this), Typeface.BOLD);
        GradientDrawable ad = new GradientDrawable();
        ad.setShape(GradientDrawable.OVAL);
        ad.setColor(getResources().getColor(R.color.ios_blue));
        avatar.setBackground(ad);
        int as = dp(48);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(as, as);
        alp.rightMargin = dp(14);
        p.addView(avatar, alp);

        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(this);
        name.setText("This device");
        name.setTypeface(Soma.body(this), Typeface.BOLD);
        name.setTextSize(16);
        name.setTextColor(s.ink);
        txt.addView(name);
        final TextView sub = new TextView(this);
        sub.setText("Your library");
        sub.setTextSize(13);
        sub.setTextColor(s.inkMute);
        txt.addView(sub);
        p.addView(txt);
        card.addView(p);
        col.addView(card);

        LibrarySnapshot.get(this, snap -> {
            if (snap != null) sub.setText(snap.photos + " Photos, " + snap.videos + " Videos");
        });
    }

    private void buildDisplay() {
        sectionHeader("DISPLAY");
        LinearLayout card = groupCard();
        Settings st = Settings.getInstance(this);
        final String[] vals = { "LIGHT", "DARK", "BLACK" };
        final String[] names = { "Light", "Dark", "Black (OLED)" };
        int cur = 0;
        for (int i = 0; i < vals.length; i++) if (vals[i].equals(st.getTheme())) cur = i;
        final int shown = cur;
        addNav(card, "Appearance", names[shown], true, () -> {
            int nx = (shown + 1) % vals.length;
            st.setTheme(this, vals[nx]);
            sChanged = true; setResult(RESULT_OK); recreate();
        });
        col.addView(card);
    }

    private void buildViewOptions() {
        sectionHeader("VIEW OPTIONS");
        LinearLayout card = groupCard();
        Settings st = Settings.getInstance(this);
        addSwitch(card, "Auto-Play Motion", st.autoPlayMotion(),
                v -> { st.setAutoPlayMotion(this, v); mark(); }, true);
        addSwitch(card, "Loop Videos", st.loopVideos(),
                v -> { st.setLoopVideos(this, v); mark(); }, true);
        addSwitch(card, "Show Videos", st.showVideos(),
                v -> { st.showVideos(this, v); mark(); }, false);
        col.addView(card);
    }

    private void buildLibraryReading() {
        sectionHeader("READING YOUR LIBRARY");
        LinearLayout card = groupCard();
        Settings st = Settings.getInstance(this);
        addSwitch(card, "MediaStore Retriever", st.useStorageRetriever(),
                v -> { st.useStorageRetriever(this, v); mark(); }, true);
        addSwitch(card, "8-bit Colour", st.use8BitColor(),
                v -> { st.use8BitColor(this, v); mark(); }, true);
        addSwitch(card, "Camera Shortcut", st.getCameraShortcut(),
                v -> { st.setCameraShortcut(this, v); mark(); }, true);
        addSwitch(card, "Max Brightness in Viewer", st.isMaxBrightness(),
                v -> { st.setMaxBrightness(this, v); mark(); }, true);
        addNav(card, "Excluded Paths", "", true,
                () -> startActivity(new Intent(this, ExcludePathsActivity.class)));
        addNav(card, "Virtual Albums", "", false,
                () -> startActivity(new Intent(this, VirtualAlbumsActivity.class)));
        col.addView(card);
    }

    private void buildReset() {
        sectionHeader("RESET");
        LinearLayout card = groupCard();
        addAction(card, "Reset People & Pets Suggestions", () -> {
            PeopleIndex.get().clear();
            android.widget.Toast.makeText(this, "People & Pets will rescan", android.widget.Toast.LENGTH_SHORT).show();
        }, false);
        col.addView(card);
    }

    /* ------------------------------------------------------------ helpers */

    private void mark() { sChanged = true; setResult(RESULT_OK); }

    private void sectionHeader(String t) {
        TextView h = new TextView(this);
        h.setText(t);
        h.setTextSize(12);
        h.setLetterSpacing(0.06f);
        h.setTypeface(Soma.body(this), Typeface.BOLD);
        h.setTextColor(s.inkMute);
        h.setPadding(dp(10), dp(22), dp(10), dp(8));
        col.addView(h);
    }

    private LinearLayout groupCard() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(s.surface);
        g.setCornerRadius(dp(16));
        g.setStroke(Math.round(Soma.dp(this, 1)), s.hairline);
        c.setBackground(g);
        c.setClipToOutline(true);
        return c;
    }

    private void divider(LinearLayout card) {
        View d = new View(this);
        d.setBackgroundColor(s.hairline);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.leftMargin = dp(14);
        card.addView(d, lp);
    }

    private LinearLayout rowBase(LinearLayout card, String label, boolean dividerAfterNeeded) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(13), dp(14), dp(13));
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(15);
        t.setTextColor(s.ink);
        t.setTypeface(Soma.body(this));
        row.addView(t, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row);
        if (dividerAfterNeeded) divider(card);
        return row;
    }

    private void addSwitch(LinearLayout card, String label, boolean on,
                           final BoolCb cb, boolean div) {
        LinearLayout row = rowBase(card, label, div);
        SwitchCompat sw = new SwitchCompat(this);
        sw.setChecked(on);
        int[][] st = { {android.R.attr.state_checked}, {} };
        sw.setThumbTintList(new android.content.res.ColorStateList(st, new int[]{ 0xFFFFFFFF, 0xFFFFFFFF }));
        sw.setTrackTintList(new android.content.res.ColorStateList(st, new int[]{ GREEN, s.hairline }));
        sw.setOnCheckedChangeListener((b, v) -> cb.accept(v));
        row.addView(sw);
    }

    private void addNav(LinearLayout card, String label, String value, boolean div, final Runnable r) {
        LinearLayout row = rowBase(card, label, div);
        if (value != null && !value.isEmpty()) {
            TextView vt = new TextView(this);
            vt.setText(value);
            vt.setTextSize(14);
            vt.setTextColor(s.inkMute);
            row.addView(vt);
        }
        TextView chev = new TextView(this);
        chev.setText(" ›");
        chev.setTextSize(18);
        chev.setTextColor(s.inkMute);
        row.addView(chev);
        row.setOnClickListener(v -> r.run());
    }

    private void addAction(LinearLayout card, String label, final Runnable r, boolean div) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(14), dp(13), dp(14), dp(13));
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(15);
        t.setTextColor(getResources().getColor(R.color.ios_blue));
        t.setTypeface(Soma.body(this));
        row.addView(t);
        row.setOnClickListener(v -> r.run());
        card.addView(row);
        if (div) divider(card);
    }

    private String initials() {
        return "F";
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override public void finish() {
        setResult(sChanged ? RESULT_OK : RESULT_CANCELED);
        super.finish();
    }

    @Override public int getDarkThemeRes() { return R.style.CameraRoll_Theme_Settings; }
    @Override public int getLightThemeRes() { return R.style.CameraRoll_Theme_Light_Settings; }

    @Override public void onThemeApplied(Theme theme) {
        Soma soma = SomaSkin.read(this);
        View root = findViewById(R.id.root_view);
        if (root != null) root.setBackgroundColor(soma.ground[0]);
        SomaSkin.statusBarIcons(this, soma);
    }
}
