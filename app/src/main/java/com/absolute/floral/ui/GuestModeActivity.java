package com.absolute.floral.ui;

import android.animation.ValueAnimator;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.absolute.floral.R;
import com.absolute.floral.adapter.photos.PhotoGridAdapter;
import com.absolute.floral.data.FlagStore;
import com.absolute.floral.data.GuestMode;
import com.absolute.floral.data.PhotoTimeline;
import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.data.provider.MediaProvider;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Set up and start a Guest Mode session — pick exactly the photos a guest may
 * see, set a PIN, then hand over the phone. Bento styling, a lock-closing
 * animation on start.
 */
public class GuestModeActivity extends AppCompatActivity {

    /** Optional: paths to pre-add to the allow-list (from a Select ▸ Guest action). */
    public static final String EXTRA_ADD_PATHS = "add_paths";

    private Soma soma;
    private final Set<String> chosen = new HashSet<>();
    private TextView chosenLabel, startBtn;
    private ImageView lockGlyph;
    private EditText pin1;
    private PhotoGridAdapter grid;

    @Override protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        soma = SomaSkin.read(this);
        chosen.addAll(GuestMode.allowed(this));
        ArrayList<String> add = getIntent().getStringArrayListExtra(EXTRA_ADD_PATHS);
        if (add != null) chosen.addAll(add);

        int p = dp(20);

        // root: fixed header + hero + chips, a scrolling grid, a fixed footer
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackgroundColor(soma.ground[0]);
        setContentView(col);
        SomaSkin.statusBarIcons(this, soma);

        // header
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(p, dp(40), p, dp(4));
        TextView title = new TextView(this);
        title.setText("Guest Mode");
        title.setTextColor(soma.ink);
        title.setTextSize(28);
        title.setTypeface(Soma.display(this), Typeface.BOLD);
        title.setLetterSpacing(-0.02f);
        head.addView(title);
        col.addView(head);

        // hero bento card (compact)
        FrameLayout hero = new FrameLayout(this);
        GradientDrawable hg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{ 0xFF9AA6DF, 0xFFB9A6D8, 0xFFDBB2CE });
        hg.setCornerRadius(dp(22));
        hero.setBackground(hg);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(96));
        hlp.setMargins(p, dp(8), p, dp(6));
        col.addView(hero, hlp);
        lockGlyph = new ImageView(this);
        lockGlyph.setImageDrawable(tintedLock(R.drawable.ic_lock_open_glyph, 0xFF3C3651));
        FrameLayout.LayoutParams lg = new FrameLayout.LayoutParams(dp(32), dp(32));
        lg.gravity = Gravity.CENTER_VERTICAL; lg.leftMargin = dp(20);
        hero.addView(lockGlyph, lg);
        TextView heroText = new TextView(this);
        heroText.setText("Only the photos you choose stay visible.\nEverything else is hidden until you unlock.");
        heroText.setTextColor(0xFF3C3651);
        heroText.setTextSize(13);
        heroText.setLineSpacing(dp(3), 1f);
        heroText.setTypeface(Soma.body(this), Typeface.BOLD);
        FrameLayout.LayoutParams ht = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ht.gravity = Gravity.CENTER_VERTICAL; ht.leftMargin = dp(72); ht.rightMargin = dp(16);
        hero.addView(heroText, ht);

        // quick actions
        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        quick.setPadding(p, dp(4), p, dp(2));
        quick.addView(chip("Add all Favourites", () -> {
            chosen.addAll(FlagStore.favorites(this).all());
            syncGrid(); updateChosen();
        }), lpWeight());
        View gap = new View(this); quick.addView(gap, new LinearLayout.LayoutParams(dp(10), 1));
        quick.addView(chip("Clear", () -> { chosen.clear(); syncGrid(); updateChosen(); }), lpWeight());
        col.addView(quick);

        chosenLabel = new TextView(this);
        chosenLabel.setTextColor(soma.inkMute);
        chosenLabel.setTextSize(12);
        chosenLabel.setTypeface(Soma.body(this));
        chosenLabel.setPadding(p, dp(6), p, dp(6));
        col.addView(chosenLabel);

        // the picker grid — takes the remaining height and scrolls
        RecyclerView rv = new RecyclerView(this);
        rv.setPadding(dp(6), 0, dp(6), dp(6));
        rv.setClipToPadding(false);
        GridLayoutManager glm = new GridLayoutManager(this, 4);
        rv.setLayoutManager(glm);
        grid = new PhotoGridAdapter(this, timelineOfAll());
        grid.setSpanCount(4);
        grid.setContinuous(true);
        grid.setIgnoreGuestFilter(true);
        grid.enterSelection();
        grid.setSelectionListener(count -> {});
        grid.setSelectionOverride(chosen, this::updateChosen);
        glm.setSpanSizeLookup(grid.spanSizeLookup());
        rv.setAdapter(grid);
        rv.setItemAnimator(null);
        rv.setHasFixedSize(true);
        col.addView(rv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // footer: PIN + Start
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(p, dp(8), p, dp(14));
        GradientDrawable fbg = new GradientDrawable();
        fbg.setColor(soma.surface);
        fbg.setCornerRadii(new float[]{ dp(22), dp(22), dp(22), dp(22), 0, 0, 0, 0 });
        footer.setBackground(fbg);
        footer.setElevation(dp(12));

        pin1 = pinField(GuestMode.hasPin(this) ? "Enter your Guest PIN" : "Choose a PIN (4–6 digits)");
        footer.addView(pin1);
        if (GuestMode.hasPin(this)) {
            TextView forgot = new TextView(this);
            forgot.setText("Forgot PIN? Reset it");
            forgot.setTextColor(soma.accent);
            forgot.setTextSize(12f);
            forgot.setPadding(0, dp(6), 0, 0);
            forgot.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Reset Guest PIN?")
                    .setMessage("This clears the Guest PIN. Set a new one below.")
                    .setPositiveButton("Reset", (d, w) -> { GuestMode.clearPin(this); recreate(); })
                    .setNegativeButton("Cancel", null)
                    .show());
            footer.addView(forgot);
        }

        startBtn = new TextView(this);
        startBtn.setText("Start Guest Mode");
        startBtn.setTextColor(0xFF322C46);
        startBtn.setTextSize(16);
        startBtn.setTypeface(Soma.display(this), Typeface.BOLD);
        startBtn.setGravity(Gravity.CENTER);
        startBtn.setPadding(0, dp(15), 0, dp(15));
        GradientDrawable sb = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{ 0xFF9AA6DF, 0xFFB9A6D8, 0xFFDBB2CE });
        sb.setCornerRadius(dp(16));
        startBtn.setBackground(sb);
        startBtn.setOnClickListener(v -> start());
        LinearLayout.LayoutParams sblp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sblp.topMargin = dp(10);
        footer.addView(startBtn, sblp);
        col.addView(footer);

        updateChosen();
    }

    /* ---------- data ---------- */

    private List<AlbumItem> allItems() {
        List<AlbumItem> all = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        ArrayList<Album> albums = MediaProvider.getAlbumsWithVirtualDirectories(this);
        if (albums != null) for (Album a : albums)
            if (a.getAlbumItems() != null) for (AlbumItem it : a.getAlbumItems())
                if (it != null && it.getPath() != null && seen.add(it.getPath())) all.add(it);
        java.util.Collections.sort(all, (x, y) -> Long.compare(y.getDate(), x.getDate()));
        return all;
    }

    private PhotoTimeline timelineOfAll() {
        Album a = new Album();
        a.setPath("guest-pick");
        a.getAlbumItems().addAll(allItems());
        ArrayList<Album> l = new ArrayList<>();
        l.add(a);
        return PhotoTimeline.from(l);
    }

    private void syncGrid() {
        if (grid != null) grid.setSelectionOverride(chosen, this::updateChosen);
    }

    private void updateChosen() {
        int n = chosen.size();
        chosenLabel.setText(n == 0
                ? "No photos chosen yet — guests will see an empty gallery."
                : n + (n == 1 ? " photo" : " photos") + " will be visible to guests.");
    }

    /* ---------- start ---------- */

    private void start() {
        String a = pin1.getText().toString().trim();
        if (GuestMode.hasPin(this)) {
            if (!GuestMode.checkPin(this, a)) { Toast.makeText(this, "Wrong PIN", Toast.LENGTH_SHORT).show(); return; }
        } else {
            if (a.length() < 4 || a.length() > 6) { Toast.makeText(this, "PIN must be 4–6 digits", Toast.LENGTH_SHORT).show(); return; }
            GuestMode.setPin(this, a);
        }
        if (chosen.isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Start with no photos?")
                    .setMessage("Guests will see an empty gallery. You can still start.")
                    .setPositiveButton("Start", (d, w) -> commit())
                    .setNegativeButton("Pick photos", null)
                    .show();
            return;
        }
        commit();
    }

    private void commit() {
        GuestMode.setAllowed(this, chosen);
        GuestMode.enter(this);
        // lock-closing flourish, then drop back to the (now filtered) gallery
        lockGlyph.setImageDrawable(tintedLock(R.drawable.ic_lock_glyph, 0xFF3C3651));
        startBtn.setEnabled(false);
        startBtn.setText("Locking…");
        ValueAnimator va = ValueAnimator.ofFloat(1f, 0.7f, 1.15f, 1f);
        va.setDuration(520);
        va.addUpdateListener(x -> {
            float f = (float) x.getAnimatedValue();
            lockGlyph.setScaleX(f); lockGlyph.setScaleY(f);
        });
        va.start();
        lockGlyph.postDelayed(() -> {
            android.content.Intent i = new android.content.Intent(this, MainActivity.class);
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        }, 560);
    }

    /* ---------- widgets ---------- */

    private android.graphics.drawable.Drawable tintedLock(int res, int color) {
        android.graphics.drawable.Drawable d = ContextCompat.getDrawable(this, res);
        if (d == null) return null;
        d = DrawableCompat.wrap(d).mutate();
        DrawableCompat.setTint(d, color);
        return d;
    }

    private EditText pinField(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        e.setTextColor(soma.ink);
        e.setHintTextColor(soma.inkMute);
        return e;
    }

    private TextView chip(String label, Runnable r) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(soma.ink);
        t.setTextSize(13);
        t.setTypeface(Soma.body(this), Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(14), dp(11), dp(14), dp(11));
        GradientDrawable g = new GradientDrawable();
        g.setColor(soma.surfaceStrong);
        g.setCornerRadius(dp(14));
        t.setBackground(g);
        t.setOnClickListener(v -> r.run());
        return t;
    }

    private LinearLayout.LayoutParams lpWeight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
