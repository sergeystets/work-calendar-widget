package com.google.work.calendar.widget.converter;

import android.graphics.Color;

import com.google.common.collect.ImmutableMap;
import com.google.work.calendar.widget.dto.CalendarEvent;
import com.google.work.calendar.widget.dto.WorkShift;
import com.google.work.calendar.widget.dto.WorkingDay;
import com.google.work.calendar.widget.utils.DateUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

public final class EventConverter {

    private final static Map<WorkShift, Color> COLORS = ImmutableMap.of(
            WorkShift.NIGHT, Color.valueOf(102, 178, 255),      // blue
            WorkShift.DAY, Color.valueOf(255, 178, 102),        // yellow
            WorkShift.EVENING, Color.valueOf(0, 204, 102),      // green
            WorkShift.DAY_OFF, Color.valueOf(255, 51, 51));     // red

    public CalendarEvent convert(Locale locale, WorkingDay workingDay) {
        return CalendarEvent.builder()
                .color(COLORS.get(workingDay.getWorkShift()))
                .from(LocalDateTime.of(workingDay.getDate(), workingDay.getWorkShift().getStart()))
                .to(LocalDateTime.of(workingDay.getDate(), workingDay.getWorkShift().getEnd()))
                .name(workingDay.getWorkShift().getShortLabel().toLowerCase())
                .description(workingDay.getDate().format(DateUtils.DATE_TIME_FORMATTER.withLocale(locale)) + " - " +
                        workingDay
                                .getWorkShift()
                                .getShortLabel()
                                .toLowerCase())
                .build();
    }
}
