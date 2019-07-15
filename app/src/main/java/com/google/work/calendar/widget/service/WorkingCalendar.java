package com.google.work.calendar.widget.service;

import com.google.work.calendar.widget.dto.WorkingDay;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class WorkingCalendar {

    private static final WorkingDayGenerator[] WORKING_DAY_GENERATORS = new WorkingDayGenerator[]{
            WorkingDayGenerator.dayShift(),
            WorkingDayGenerator.dayShift(),
            WorkingDayGenerator.dayShift(),
            WorkingDayGenerator.dayShift(),

            WorkingDayGenerator.dayOff(),

            WorkingDayGenerator.eveningShift(),
            WorkingDayGenerator.eveningShift(),
            WorkingDayGenerator.eveningShift(),
            WorkingDayGenerator.eveningShift(),

            WorkingDayGenerator.dayOff(),

            WorkingDayGenerator.nightShift(),
            WorkingDayGenerator.nightShift(),
            WorkingDayGenerator.nightShift(),
            WorkingDayGenerator.nightShift(),

            WorkingDayGenerator.dayOff(),
            WorkingDayGenerator.dayOff(),
    };

    private LocalDate startPoint; // this is the date of the first known day shift

    public WorkingCalendar(LocalDate startPoint) {
        this.startPoint = startPoint;
    }

    public Map<LocalDate, WorkingDay> buildScheduleFor(final Pair<LocalDate, LocalDate> period) {
        Validate.notNull(period, "working period can not be null");
        Validate.notNull(startPoint, "startPoint can not be null");

        final LocalDate from = period.getLeft();
        final LocalDate to = period.getRight();
        Validate.isTrue(from.isEqual(to) || to.isAfter(from), "invalid period is specified " + period + ", from should be >= to");

        final int totalDays = (int) ChronoUnit.DAYS.between(from, to) + 1;
        final Map<LocalDate, WorkingDay> schedule = new HashMap<>(totalDays);

        int daysCounter = 0;
        LocalDate date = from.minusDays(1);

        int i = findFirstGeneratorFor(from);
        while (daysCounter < totalDays) {
            for (; i < WORKING_DAY_GENERATORS.length && daysCounter < totalDays; i++) {
                date = date.plusDays(1);
                WorkingDayGenerator workingDayGenerator = WORKING_DAY_GENERATORS[i];
                schedule.put(date, workingDayGenerator.generate(date));
                daysCounter++;
            }
            i = 0;
        }

        return schedule;
    }

    private int findFirstGeneratorFor(final LocalDate date) {
        long daysBetween = Math.abs(ChronoUnit.DAYS.between(date, startPoint));

        if (startPoint.equals(date)) {
            return 0;
        }
        if (startPoint.isAfter(date)) {
            int shift = (int) (daysBetween % (WORKING_DAY_GENERATORS.length));
            return WORKING_DAY_GENERATORS.length - shift;
        } else {
            return (int) (daysBetween % (WORKING_DAY_GENERATORS.length));
        }
    }
}
