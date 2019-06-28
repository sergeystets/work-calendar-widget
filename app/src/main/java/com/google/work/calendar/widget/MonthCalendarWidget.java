package com.google.work.calendar.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

import java.text.DateFormatSymbols;
import java.util.Calendar;

public class MonthCalendarWidget extends AppWidgetProvider {

    private static final String SETTINGS_CURRENT_MONTH = "com.google.work.calendar.widget.settings.SELECTED_MONTH";
    private static final String SETTINGS_CURRENT_YEAR = "com.google.work.calendar.widget.settings.SELECTED_YEAR";
    private static final String ACTION_PREVIOUS_MONTH = "com.google.work.calendar.widget.action.PREVIOUS_MONTH";
    private static final String ACTION_NEXT_MONTH = "com.google.work.calendar.widget.action.NEXT_MONTH";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId : appWidgetIds) {
            drawWidget(context, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();

        if (ACTION_PREVIOUS_MONTH.equals(action)) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            Calendar calendar = getCurrentMonth(settings);
            calendar.add(Calendar.MONTH, -1);
            settings.edit()
                    .putInt(SETTINGS_CURRENT_MONTH, calendar.get(Calendar.MONTH))
                    .putInt(SETTINGS_CURRENT_YEAR, calendar.get(Calendar.YEAR))
                    .apply();
            redrawWidgets(context);

        } else if (ACTION_NEXT_MONTH.equals(action)) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            Calendar calendar = getCurrentMonth(settings);
            calendar.add(Calendar.MONTH, 1);
            settings.edit()
                    .putInt(SETTINGS_CURRENT_MONTH, calendar.get(Calendar.MONTH))
                    .putInt(SETTINGS_CURRENT_YEAR, calendar.get(Calendar.YEAR))
                    .apply();
            redrawWidgets(context);
        }
    }

    private void redrawWidgets(Context context) {
        int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
                new ComponentName(context, MonthCalendarWidget.class));
        for (int appWidgetId : appWidgetIds) {
            drawWidget(context, appWidgetId);
        }
    }

    private void drawWidget(Context context, int appWidgetId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.widget);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        Calendar selectedCalendar = getCurrentMonth(settings);
        Calendar todayCalendar = Calendar.getInstance();

        int selectedMonth = selectedCalendar.get(Calendar.MONTH);

        int today = todayCalendar.get(Calendar.DAY_OF_MONTH);
        int todayMonth = todayCalendar.get(Calendar.MONTH);
        int todayYear = todayCalendar.get(Calendar.YEAR);

        widget.setTextViewText(R.id.month_label, DateFormat.format("MMMM yyyy", selectedCalendar));

        // broadcast event when 'previous month' clicked
        widget.setOnClickPendingIntent(R.id.prev_month_button,
                PendingIntent.getBroadcast(context, 0,
                        new Intent(context, MonthCalendarWidget.class)
                                .setAction(ACTION_PREVIOUS_MONTH),
                        PendingIntent.FLAG_UPDATE_CURRENT));

        // broadcast event when 'next month' clicked
        widget.setOnClickPendingIntent(R.id.next_month_button,
                PendingIntent.getBroadcast(context, 0,
                        new Intent(context, MonthCalendarWidget.class)
                                .setAction(ACTION_NEXT_MONTH),
                        PendingIntent.FLAG_UPDATE_CURRENT));

        widget.removeAllViews(R.id.calendar);

        // show week days
        RemoteViews rowHeader = new RemoteViews(context.getPackageName(), R.layout.row_header);
        DateFormatSymbols dfs = DateFormatSymbols.getInstance();
        String[] weekdays = dfs.getShortWeekdays();

        for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
            RemoteViews dayView = new RemoteViews(context.getPackageName(), R.layout.cell_header);
            dayView.setTextViewText(android.R.id.text1, weekdays[day]);
            rowHeader.addView(R.id.row_container, dayView);
        }
        widget.addView(R.id.calendar, rowHeader);

        int monthStartDayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK);
        selectedCalendar.add(Calendar.DAY_OF_MONTH, 1 - monthStartDayOfWeek);

        // show days
        for (int week = 0; week < 6; week++) {
            RemoteViews weekRow = new RemoteViews(context.getPackageName(), R.layout.row_week);

            for (int day = 0; day < 7; day++) {
                boolean inMonth = selectedCalendar.get(Calendar.MONTH) == selectedMonth;
                boolean isToday =
                        selectedCalendar.get(Calendar.DAY_OF_MONTH) == today &&
                                selectedCalendar.get(Calendar.MONTH) == todayMonth &&
                                selectedCalendar.get(Calendar.YEAR) == todayYear;
                int cellLayout;
                if (isToday) {
                    cellLayout = R.layout.cell_today;
                } else if (inMonth) {
                    cellLayout = R.layout.cell_day_this_month;
                } else {
                    cellLayout = R.layout.cell_day;
                }

                RemoteViews dayCell = new RemoteViews(context.getPackageName(), cellLayout);
                dayCell.setTextViewText(android.R.id.text1,
                        Integer.toString(selectedCalendar.get(Calendar.DAY_OF_MONTH)));
                weekRow.addView(R.id.row_container, dayCell);

                if (inMonth) {
                    Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                    builder.appendPath("time");
                    builder.appendPath(Long.toString(selectedCalendar.getTimeInMillis()));
                    Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    dayCell.setOnClickPendingIntent(android.R.id.text1,
                            PendingIntent.getActivity(context, 0, intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT));

                }

                selectedCalendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            widget.addView(R.id.calendar, weekRow);
        }

        appWidgetManager.updateAppWidget(appWidgetId, widget);
    }

    private static Calendar getCurrentMonth(SharedPreferences settings) {
        Calendar calendar = Calendar.getInstance();

        int month = settings.getInt(SETTINGS_CURRENT_MONTH, calendar.get(Calendar.MONTH));
        int year = settings.getInt(SETTINGS_CURRENT_YEAR, calendar.get(Calendar.YEAR));

        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        return calendar;
    }
}
