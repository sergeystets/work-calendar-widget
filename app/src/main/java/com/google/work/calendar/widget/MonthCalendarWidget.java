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
import android.widget.RemoteViews;

import com.google.common.collect.ImmutableMap;
import com.google.work.calendar.widget.converter.EventConverter;
import com.google.work.calendar.widget.dto.WorkShift;
import com.google.work.calendar.widget.dto.WorkingDay;
import com.google.work.calendar.widget.service.WorkingCalendar;
import com.google.work.calendar.widget.utils.LocaleUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class MonthCalendarWidget extends AppWidgetProvider {

    private static final String SETTINGS_CURRENT_MONTH = "com.google.work.calendar.widget.settings.SELECTED_MONTH";
    private static final String SETTINGS_WEEK_STARTS_FROM_MONDAY = "com.google.work.calendar.widget.settings.WEEK_START_FROM_MONDAY";
    private static final String SETTINGS_CURRENT_YEAR = "com.google.work.calendar.widget.settings.SELECTED_YEAR";
    private static final String ACTION_PREVIOUS_MONTH = "com.google.work.calendar.widget.action.PREVIOUS_MONTH";
    private static final String ACTION_NEXT_MONTH = "com.google.work.calendar.widget.action.NEXT_MONTH";
    private static final String ACTION_SELECT_BRIGADE = "com.google.work.calendar.widget.action.ACTION_SELECT_BRIGADE";
    private static final String ACTION_SELECT_HOURS_PER_DAY = "com.google.work.calendar.widget.action.ACTION_SELECT_HOURS_PER_DAY";
    private static final String SETTINGS_BRIGADE_SELECTED = "com.google.work.calendar.widget.settings.SETTINGS_BRIGADE_SELECTED";
    private static final String SETTINGS_HOURS_PER_DAY_SELECTED = "com.google.work.calendar.widget.settings.SETTINGS_HOURS_PER_DAY_SELECTED";

    private static int[] weekdaysStartingFromSunday = new int[]{
            Calendar.SUNDAY,
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY};

    private static int[] weekdaysStartingFromMonday = new int[]{
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY,
    };

    private static final Map<Integer, Map<WorkShift.HoursPerDay, LocalDate>> startPoints = ImmutableMap.of(
            1,
            ImmutableMap.of(
                    WorkShift.HoursPerDay.EIGHT_HOURS, LocalDate.of(2019, Month.JULY, 3),
                    WorkShift.HoursPerDay.TWELVE_HOURS, LocalDate.of(2022, Month.APRIL, 25)
            ),
            2,
            ImmutableMap.of(
                    WorkShift.HoursPerDay.EIGHT_HOURS, LocalDate.of(2019, Month.JULY, 11),
                    WorkShift.HoursPerDay.TWELVE_HOURS, LocalDate.of(2022, Month.APRIL, 27)
            ),
            3,
            ImmutableMap.of(
                    WorkShift.HoursPerDay.EIGHT_HOURS, LocalDate.of(2019, Month.JULY, 7),
                    WorkShift.HoursPerDay.TWELVE_HOURS, LocalDate.of(2022, Month.APRIL, 26)
            ),
            4,
            ImmutableMap.of(
                    WorkShift.HoursPerDay.EIGHT_HOURS, LocalDate.of(2019, Month.JULY, 15),
                    WorkShift.HoursPerDay.TWELVE_HOURS, LocalDate.of(2022, Month.APRIL, 28))
    );

    private static final EventConverter converter = new EventConverter();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        if (LocaleUtils.isRussianLocale(context)) {
            settings.edit().putBoolean(SETTINGS_WEEK_STARTS_FROM_MONDAY, true).apply();
        }

        for (int appWidgetId : appWidgetIds) {
            drawWidget(context, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        if (ACTION_PREVIOUS_MONTH.equals(action)) {
            Calendar month = getDisplayedMonth(settings);
            month.add(Calendar.MONTH, -1);
            settings.edit()
                    .putInt(SETTINGS_CURRENT_MONTH, month.get(Calendar.MONTH))
                    .putInt(SETTINGS_CURRENT_YEAR, month.get(Calendar.YEAR))
                    .apply();
            redrawWidgets(context);

        } else if (ACTION_NEXT_MONTH.equals(action)) {
            Calendar calendar = getDisplayedMonth(settings);
            calendar.add(Calendar.MONTH, 1);
            settings.edit()
                    .putInt(SETTINGS_CURRENT_MONTH, calendar.get(Calendar.MONTH))
                    .putInt(SETTINGS_CURRENT_YEAR, calendar.get(Calendar.YEAR))
                    .apply();
            redrawWidgets(context);
        } else if (ACTION_SELECT_BRIGADE.equals(action)) {
            Uri uri = intent.getData();
            int brigade = uri == null ?
                    1 :
                    Integer.valueOf(StringUtils.defaultString(uri.getQueryParameter("brigade"), "1"));
            settings.edit().putInt(SETTINGS_BRIGADE_SELECTED, brigade).apply();
            redrawWidgets(context);
        } else if (ACTION_SELECT_HOURS_PER_DAY.equals(action)) {
            Uri uri = intent.getData();
            int hoursPerDay = uri == null ?
                    8 :
                    Integer.valueOf(StringUtils.defaultString(uri.getQueryParameter("hoursPerDay"), "8"));
            settings.edit().putInt(SETTINGS_HOURS_PER_DAY_SELECTED, hoursPerDay).apply();
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

        Calendar selectedCalendar = getDisplayedMonth(settings);
        Calendar todayCalendar = Calendar.getInstance();

        int selectedMonth = selectedCalendar.get(Calendar.MONTH);
        int selectedYear = selectedCalendar.get(Calendar.YEAR);

        int today = todayCalendar.get(Calendar.DAY_OF_MONTH);
        int todayMonth = todayCalendar.get(Calendar.MONTH);
        int todayYear = todayCalendar.get(Calendar.YEAR);

        LocalDate selectedDate = toLocalDate(selectedCalendar);
        Locale locale = LocaleUtils.getLocaleFor(context);
        DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy", locale);
        widget.setTextViewText(R.id.month_label,
                selectedDate.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, locale) +
                        " " +
                        selectedDate.format(yearFormatter));

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
        String[] weekdayNames = DateFormatSymbols.getInstance().getShortWeekdays();
        boolean weekStartsFromMonday = settings.getBoolean(SETTINGS_WEEK_STARTS_FROM_MONDAY, false);
        int[] weekdays = weekStartsFromMonday ? weekdaysStartingFromMonday : weekdaysStartingFromSunday;

        WorkShift.HoursPerDay hoursPerDay = WorkShift.HoursPerDay.fromHours(
                settings.getInt(SETTINGS_HOURS_PER_DAY_SELECTED,
                WorkShift.HoursPerDay.TWELVE_HOURS.getHours()));
        widget.removeAllViews(R.id.hours_per_day);

        for (int day : weekdays) {
            RemoteViews dayView = new RemoteViews(context.getPackageName(), R.layout.cell_header);
            dayView.setTextViewText(android.R.id.text1, weekdayNames[day]);
            rowHeader.addView(R.id.row_container, dayView);
        }
        widget.addView(R.id.calendar, rowHeader);

        // reset the calendar to the first day of the visible area on the screen
        int monthStartDayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK);
        selectedCalendar.add(Calendar.DAY_OF_MONTH, weekStartsFromMonday ?
                2 - monthStartDayOfWeek :
                1 - monthStartDayOfWeek);

        Map<LocalDate, WorkingDay> schedule = getSchedule(
                selectedMonth + 1,
                selectedYear,
                settings.getInt(SETTINGS_BRIGADE_SELECTED, 1),
                hoursPerDay);

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

                WorkingDay workingDay = schedule.get(toLocalDate(selectedCalendar));

                String eventName = "";
                if (workingDay != null) {
                    eventName = converter.convert(workingDay).getName();
                }

                dayCell.setTextViewText(android.R.id.text1,
                        selectedCalendar.get(Calendar.DAY_OF_MONTH) + " " + eventName);
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

        // add brigade selector
        widget.removeAllViews(R.id.brigade);
        for (int i = 1; i < 5; i++) {
            int cellLayout = i == settings.getInt(SETTINGS_BRIGADE_SELECTED, 1) ?
                    R.layout.cell_selected_brigade :
                    R.layout.cell_brigade;
            RemoteViews brigadeCell = new RemoteViews(context.getPackageName(), cellLayout);
            brigadeCell.setTextViewText(android.R.id.text1, String.valueOf(i));
            Intent intent = new Intent(context, MonthCalendarWidget.class);
            intent.setData(Uri.parse("/?brigade=" + i));
            intent.setAction(ACTION_SELECT_BRIGADE);
            brigadeCell.setOnClickPendingIntent(android.R.id.text1,
                    PendingIntent.getBroadcast(context, 0,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT));

            widget.addView(R.id.brigade, brigadeCell);
        }

        // hours per day
        boolean is8HoursSelected = hoursPerDay.equals(WorkShift.HoursPerDay.EIGHT_HOURS);
        widget.removeAllViews(R.id.hours_per_day);
        RemoteViews hoursPerDayView_8 = new RemoteViews(context.getPackageName(),
                is8HoursSelected ? R.layout.cell_selected_brigade : R.layout.cell_brigade);
        hoursPerDayView_8.setTextViewText(android.R.id.text1, String.valueOf(WorkShift.HoursPerDay.EIGHT_HOURS.getHours()));
        RemoteViews hoursPerDayView_12 = new RemoteViews(context.getPackageName(),
                !is8HoursSelected ? R.layout.cell_selected_brigade : R.layout.cell_brigade);
        hoursPerDayView_12.setTextViewText(android.R.id.text1, String.valueOf(WorkShift.HoursPerDay.TWELVE_HOURS.getHours()));
        widget.addView(R.id.hours_per_day, hoursPerDayView_8);
        widget.addView(R.id.hours_per_day, hoursPerDayView_12);


        Intent intent_8 = new Intent(context, MonthCalendarWidget.class);
        intent_8.setData(Uri.parse("/?hoursPerDay=" + WorkShift.HoursPerDay.EIGHT_HOURS.getHours()));
        intent_8.setAction(ACTION_SELECT_HOURS_PER_DAY);
        hoursPerDayView_8.setOnClickPendingIntent(android.R.id.text1,
                PendingIntent.getBroadcast(context, 0,
                        intent_8,
                        PendingIntent.FLAG_UPDATE_CURRENT));

        Intent intent_12 = new Intent(context, MonthCalendarWidget.class);
        intent_12.setData(Uri.parse("/?hoursPerDay=" + WorkShift.HoursPerDay.TWELVE_HOURS.getHours()));
        intent_12.setAction(ACTION_SELECT_HOURS_PER_DAY);
        hoursPerDayView_12.setOnClickPendingIntent(android.R.id.text1,
                PendingIntent.getBroadcast(context, 0,
                        intent_12,
                        PendingIntent.FLAG_UPDATE_CURRENT));


        appWidgetManager.updateAppWidget(appWidgetId, widget);
    }

    private Map<LocalDate, WorkingDay> getSchedule(int month, int year, int brigade, WorkShift.HoursPerDay hoursPerDay) {
        LocalDate selected = LocalDate.of(year, month, 1);
        LocalDate from = selected.minusMonths(1);
        LocalDate to = selected.plusMonths(2);
        Pair<LocalDate, LocalDate> period = Pair.of(from, to);

        LocalDate startPoint = requireNonNull(startPoints.get(brigade)).get(hoursPerDay);
        return new WorkingCalendar(startPoint).buildScheduleFor(period, hoursPerDay);
    }

    private static Calendar getDisplayedMonth(SharedPreferences settings) {
        Calendar calendar = Calendar.getInstance();

        int month = settings.getInt(SETTINGS_CURRENT_MONTH, calendar.get(Calendar.MONTH));
        int year = settings.getInt(SETTINGS_CURRENT_YEAR, calendar.get(Calendar.YEAR));

        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        return calendar;
    }

    private static LocalDate toLocalDate(Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);

        return LocalDate.of(year, month, day);
    }
}
