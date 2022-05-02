package com.google.work.calendar.widget.converter;

import com.google.work.calendar.widget.dto.CalendarEvent;
import com.google.work.calendar.widget.dto.WorkingDay;

public final class EventConverter {

    public CalendarEvent convert(WorkingDay workingDay) {
        return CalendarEvent.builder()
                .name(workingDay.getWorkShift().getShortLabel().toLowerCase())
                .build();
    }
}
