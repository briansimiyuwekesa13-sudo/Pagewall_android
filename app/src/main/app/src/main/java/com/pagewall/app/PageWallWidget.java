package com.pagewall.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class PageWallWidget extends AppWidgetProvider {

@Override
public void onUpdate(Context context,
                     AppWidgetManager appWidgetManager,
                     int[] appWidgetIds) {

    for (int appWidgetId : appWidgetIds) {

        RemoteViews views = new RemoteViews(
            context.getPackageName(),
            R.layout.pagewall_widget
        );

        Intent intent = new Intent(context, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT |
            PendingIntent.FLAG_IMMUTABLE
        );

        views.setOnClickPendingIntent(
            R.id.widget_button,
            pendingIntent
        );

        appWidgetManager.updateAppWidget(
            appWidgetId,
            views
        );
    }
}

}
