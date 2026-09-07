package com.absolute.floral.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.absolute.floral.R;
import com.absolute.floral.ui.FirstActivity;

/** "Memories" — a single photo from a day in your library, drawn premium. */
public class MemoriesWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.absolute.floral.widget.REFRESH";

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) render(ctx, mgr, id);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager mgr,
                                          int id, Bundle newOptions) {
        render(ctx, mgr, id);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, MemoriesWidgetProvider.class));
            for (int id : ids) render(ctx, mgr, id);
        }
    }

    static void render(Context ctx, AppWidgetManager mgr, int id) {
        Bundle opts = mgr.getAppWidgetOptions(id);
        float d = ctx.getResources().getDisplayMetrics().density;
        int wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
        int hDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160);
        if (hDp <= 0) hDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160);
        int wPx = Math.max(1, Math.round(wDp * d));
        int hPx = Math.max(1, Math.round(hDp * d));
        // cap to keep the bitmap sane
        wPx = Math.min(wPx, Math.round(420 * d));
        hPx = Math.min(hPx, Math.round(320 * d));

        boolean dark = (ctx.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_memories);
        try {
            Bitmap bmp = WidgetRenderer.render(ctx, wPx, hPx, dark);
            rv.setImageViewBitmap(R.id.widget_image, bmp);
        } catch (Throwable t) {
            // leave the placeholder
        }

        Intent open = new Intent(ctx, FirstActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        rv.setOnClickPendingIntent(R.id.widget_image, PendingIntent.getActivity(
                ctx, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        mgr.updateAppWidget(id, rv);
    }
}
