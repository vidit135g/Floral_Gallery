package com.absolute.floral.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.places.PlacesIndex;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** "Your map" — every geotagged place plotted on an offline photo-map (no network, no tiles). */
public class PhotoMapActivity extends AppCompatActivity {

    private MapPlot plot;
    private TextView sub;

    @Override protected void onCreate(@Nullable Bundle st) {
        super.onCreate(st);
        final Soma s = SomaSkin.read(this);

        FrameLayout root = new FrameLayout(this);
        root.setId(com.absolute.floral.R.id.root_view);
        root.setBackgroundColor(s.ground[0]);
        setContentView(root);
        SomaSkin.statusBarIcons(this, s);

        plot = new MapPlot(this, s);
        root.addView(plot, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // floating header
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(d(20), d(30), d(20), d(14));
        android.graphics.drawable.GradientDrawable hb = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ s.ground[0], s.ground[0] & 0x00FFFFFF });
        head.setBackground(hb);
        TextView title = new TextView(this);
        title.setText("Your map");
        title.setTypeface(Soma.display(this), Typeface.BOLD);
        title.setTextSize(28);
        title.setTextColor(s.ink);
        head.addView(title);
        sub = new TextView(this);
        sub.setText("Reading location from your photos…");
        sub.setTextColor(s.inkMute);
        sub.setTextSize(13);
        head.addView(sub);
        root.addView(head, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextColor(s.ink);
        back.setTextSize(30);
        back.setPadding(d(18), d(24), d(18), d(10));
        back.setOnClickListener(v -> finish());
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        root.addView(back, blp);

        PlacesIndex.get().ensure(this, places -> {
            if (places == null || places.isEmpty()) {
                sub.setText("No photos with location yet");
                return;
            }
            sub.setText(places.size() + (places.size() == 1 ? " place" : " places"));
            plot.setPlaces(places);
        });
    }

    private int d(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    /* ------------------------------------------------------------------ the plot */

    class MapPlot extends View {

        private final Soma s;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pin = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path teardrop = new Path();

        private final List<PlacesIndex.Place> places = new ArrayList<>();
        // camera in world coords [0,1]; scale = px per full-world-width
        private double camX = 0.5, camY = 0.5;
        private double scale = 800;
        private boolean fitted = false;

        private final ScaleGestureDetector scaleD;
        private final GestureDetector gestureD;

        MapPlot(Context c, Soma s) {
            super(c);
            this.s = s;
            text.setTypeface(Soma.body(c));
            text.setTextSize(sp(11));
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeWidth(dp(1));

            scaleD = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector d) {
                    double f = d.getScaleFactor();
                    double fx = d.getFocusX(), fy = d.getFocusY();
                    double wx = camX + (fx - getWidth() / 2.0) / scale;
                    double wy = camY + (fy - getHeight() / 2.0) / scale;
                    scale = Math.max(200, Math.min(120000, scale * f));
                    camX = wx - (fx - getWidth() / 2.0) / scale;
                    camY = wy - (fy - getHeight() / 2.0) / scale;
                    invalidate();
                    return true;
                }
            });
            gestureD = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
                @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                    camX += dx / scale;
                    camY += dy / scale;
                    clampCam();
                    invalidate();
                    return true;
                }
                @Override public boolean onSingleTapUp(MotionEvent e) {
                    hit(e.getX(), e.getY());
                    return true;
                }
                @Override public boolean onDoubleTap(MotionEvent e) {
                    double wx = camX + (e.getX() - getWidth() / 2.0) / scale;
                    double wy = camY + (e.getY() - getHeight() / 2.0) / scale;
                    scale = Math.min(120000, scale * 2);
                    camX = wx - (e.getX() - getWidth() / 2.0) / scale;
                    camY = wy - (e.getY() - getHeight() / 2.0) / scale;
                    invalidate();
                    return true;
                }
            });
        }

        void setPlaces(List<PlacesIndex.Place> p) {
            places.clear();
            places.addAll(p);
            fitted = false;
            invalidate();
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            scaleD.onTouchEvent(e);
            gestureD.onTouchEvent(e);
            return true;
        }

        private void clampCam() {
            camY = Math.max(0.02, Math.min(0.98, camY));
            camX = Math.max(-0.5, Math.min(1.5, camX));
        }

        private void fit() {
            if (places.isEmpty() || getWidth() == 0) return;
            double minX = 1, minY = 1, maxX = 0, maxY = 0;
            for (PlacesIndex.Place p : places) {
                double wx = lon2x(p.lon), wy = lat2y(p.lat);
                minX = Math.min(minX, wx); maxX = Math.max(maxX, wx);
                minY = Math.min(minY, wy); maxY = Math.max(maxY, wy);
            }
            camX = (minX + maxX) / 2;
            camY = (minY + maxY) / 2;
            double spanX = Math.max(1e-4, maxX - minX);
            double spanY = Math.max(1e-4, maxY - minY);
            double sx = getWidth() * 0.72 / spanX;
            double sy = getHeight() * 0.6 / spanY;
            scale = Math.max(200, Math.min(60000, Math.min(sx, sy)));
            fitted = true;
        }

        @Override protected void onDraw(Canvas cv) {
            if (!fitted) fit();
            int w = getWidth(), h = getHeight();

            // sea / sky ground
            fill.setShader(new LinearGradient(0, 0, 0, h,
                    0xFFE9F1FA, 0xFFDCE8F3, Shader.TileMode.CLAMP));
            cv.drawRect(0, 0, w, h, fill);
            fill.setShader(null);

            drawGraticule(cv, w, h);
            drawPins(cv, w, h);
        }

        private void drawGraticule(Canvas cv, int w, int h) {
            // choose a degree step that lands ~110-190px apart
            double[] steps = { 60, 30, 15, 10, 5, 2, 1, 0.5, 0.2, 0.1, 0.05 };
            double pxPerDegLon = scale / 360.0;
            double step = 60;
            for (double cand : steps) { if (cand * pxPerDegLon <= 190) { step = cand; break; } }

            line.setColor(0x33244A6B);
            text.setColor(0x66244A6B);
            double leftLon = x2lon(camX - (w / 2.0) / scale);
            double rightLon = x2lon(camX + (w / 2.0) / scale);
            for (double lon = Math.ceil(leftLon / step) * step; lon <= rightLon; lon += step) {
                if (lon < -180 || lon > 180) continue;
                float sx = worldToScreenX(lon2x(lon), w);
                cv.drawLine(sx, 0, sx, h, line);
                cv.drawText(fmt(lon) + "°", sx + dp(3), h - dp(8), text);
            }
            double topLat = Math.min(85, y2lat(camY - (h / 2.0) / scale));
            double botLat = Math.max(-85, y2lat(camY + (h / 2.0) / scale));
            for (double lat = Math.ceil(botLat / step) * step; lat <= topLat; lat += step) {
                if (lat < -85 || lat > 85) continue;
                float sy = worldToScreenY(lat2y(lat), h);
                cv.drawLine(0, sy, w, sy, line);
                cv.drawText(fmt(lat) + "°", dp(6), sy - dp(4), text);
            }
        }

        private final List<float[]> hitBoxes = new ArrayList<>(); // x,y,r,index

        private void drawPins(Canvas cv, int w, int h) {
            hitBoxes.clear();
            text.setColor(Color.WHITE);
            text.setTextAlign(Paint.Align.CENTER);
            Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
            label.setTypeface(Typeface.create(Soma.body(getContext()), Typeface.BOLD));
            label.setTextSize(sp(12));

            for (int i = 0; i < places.size(); i++) {
                PlacesIndex.Place p = places.get(i);
                float x = worldToScreenX(lon2x(p.lon), w);
                float y = worldToScreenY(lat2y(p.lat), h);
                if (x < -80 || x > w + 80 || y < -80 || y > h + 120) continue;

                int n = p.items.size();
                float r = dp(13) + Math.min(dp(10), (float) Math.log10(Math.max(1, n)) * dp(7));

                // shadow blob
                pin.setColor(0x33000000);
                cv.drawOval(new RectF(x - r, y - r + dp(3), x + r, y + r + dp(3)), pin);
                // teardrop
                pin.setColor(0xFF0B6BD6);
                drawTeardrop(cv, x, y, r);
                pin.setColor(0xFFFFFFFF);
                cv.drawCircle(x, y - r * 0.15f, r * 0.42f, pin);
                pin.setColor(0xFF0B6BD6);
                text.setColor(0xFF0B6BD6);
                cv.drawText(String.valueOf(n), x, y - r * 0.15f + sp(4), text);

                // label pill
                String name = p.label == null ? "" : p.label;
                float tw = label.measureText(name);
                float ly = y + dp(10);
                RectF pill = new RectF(x - tw / 2 - dp(9), ly, x + tw / 2 + dp(9), ly + dp(24));
                pin.setColor(0xF2FFFFFF);
                cv.drawRoundRect(pill, dp(12), dp(12), pin);
                label.setColor(0xFF1F2933);
                label.setTextAlign(Paint.Align.CENTER);
                cv.drawText(name, x, ly + dp(16.5f), label);

                hitBoxes.add(new float[]{ x, y, Math.max(r, tw / 2 + dp(9)), i });
            }
            text.setTextAlign(Paint.Align.LEFT);
        }

        private void drawTeardrop(Canvas cv, float x, float y, float r) {
            teardrop.reset();
            teardrop.addCircle(x, y - r, r, Path.Direction.CW);
            teardrop.moveTo(x - r * 0.72f, y - r * 0.55f);
            teardrop.lineTo(x, y + r * 0.5f);
            teardrop.lineTo(x + r * 0.72f, y - r * 0.55f);
            teardrop.close();
            cv.drawPath(teardrop, pin);
        }

        private void hit(float px, float py) {
            for (int k = hitBoxes.size() - 1; k >= 0; k--) {
                float[] b = hitBoxes.get(k);
                if (Math.hypot(px - b[0], py - b[1]) <= b[2] + dp(8)) {
                    PlacesIndex.Place p = places.get((int) b[3]);
                    BucketActivity.TITLE = p.label;
                    BucketActivity.KICKER = "PLACE";
                    BucketActivity.ITEMS = new ArrayList<>(p.items);
                    getContext().startActivity(new Intent(getContext(), BucketActivity.class));
                    return;
                }
            }
        }

        /* projections */
        private double lon2x(double lon) { return (lon + 180) / 360.0; }
        private double x2lon(double x) { return x * 360.0 - 180; }
        private double lat2y(double lat) {
            double r = Math.toRadians(Math.max(-85.05, Math.min(85.05, lat)));
            return (1 - Math.log(Math.tan(r) + 1 / Math.cos(r)) / Math.PI) / 2;
        }
        private double y2lat(double y) {
            double n = Math.PI * (1 - 2 * y);
            return Math.toDegrees(Math.atan(Math.sinh(n)));
        }
        private float worldToScreenX(double wx, int w) { return (float) ((wx - camX) * scale + w / 2.0); }
        private float worldToScreenY(double wy, int h) { return (float) ((wy - camY) * scale + h / 2.0); }

        private String fmt(double v) {
            if (v == Math.rint(v)) return String.valueOf((int) Math.rint(v));
            return String.format(Locale.US, "%.1f", v);
        }
        private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
        private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }
    }
}
