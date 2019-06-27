package com.google.work.calendar.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

import java.util.Calendar;

public class MonthCalendarWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId : appWidgetIds) {
            drawWidget(context, appWidgetId);
        }
    }

    private void drawWidget(Context context, int appWidgetId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.widget);
        Calendar calendar = Calendar.getInstance();

        widget.setTextViewText(R.id.month_label, DateFormat.format("MMMM yyyy", calendar));
        appWidgetManager.updateAppWidget(appWidgetId, widget);
    }
}
