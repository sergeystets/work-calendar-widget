package com.google.work.calendar.widget.service;

import com.google.common.collect.ImmutableMap;
import com.google.work.calendar.widget.dto.WorkShift;
import com.google.work.calendar.widget.dto.WorkingDay;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class WorkingCalendar {

    private static final WorkingDayGenerator[] WORKING_DAY_GENERATORS_8_HOURS = new WorkingDayGenerator[]{
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

    private static final WorkingDayGenerator[] WORKING_DAY_GENERATORS_12_HOURS = new WorkingDayGenerator[]{
            WorkingDayGenerator.dayShift(),
            WorkingDayGenerator.nightShift(),
            WorkingDayGenerator.sleepShift(),
            WorkingDayGenerator.dayOff(),
    };

    private static final Map<WorkShift.HoursPerDay, WorkingDayGenerator[]> WORKING_DAY_GENERATORS_BY_TYPE = ImmutableMap.of(
            WorkShift.HoursPerDay.EIGHT_HOURS, WORKING_DAY_GENERATORS_8_HOURS,
            WorkShift.HoursPerDay.TWELVE_HOURS, WORKING_DAY_GENERATORS_12_HOURS);

    private LocalDate startPoint; // this is the date of the first known day shift

    public WorkingCalendar(LocalDate startPoint) {
        this.startPoint = startPoint;
    }

    public Map<LocalDate, WorkingDay> buildScheduleFor(final Pair<LocalDate, LocalDate> period, WorkShift.HoursPerDay hoursPerDay) {
        Validate.notNull(period, "working period can not be null");
        Validate.notNull(startPoint, "startPoint can not be null");

        final LocalDate from = period.getLeft();
        final LocalDate to = period.getRight();
        Validate.isTrue(from.isEqual(to) || to.isAfter(from), "invalid period is specified " + period + ", from should be >= to");

        final int totalDays = (int) ChronoUnit.DAYS.between(from, to) + 1;
        final Map<LocalDate, WorkingDay> schedule = new HashMap<>(totalDays);

        int daysCounter = 0;
        LocalDate date = from.minusDays(1);

        int i = findFirstGeneratorFor(hoursPerDay, from);
        while (daysCounter < totalDays) {
            WorkingDayGenerator[] workingDayGenerators = getWorkingDayGenerators(hoursPerDay);
            for (; i < workingDayGenerators.length && daysCounter < totalDays; i++) {
                date = date.plusDays(1);
                WorkingDayGenerator workingDayGenerator = workingDayGenerators[i];
                schedule.put(date, workingDayGenerator.generate(date));
                daysCounter++;
            }
            i = 0;
        }

        return schedule;
    }

    private WorkingDayGenerator[] getWorkingDayGenerators(WorkShift.HoursPerDay hoursPerDay) {
        return WORKING_DAY_GENERATORS_BY_TYPE.get(hoursPerDay);
    }

    private int findFirstGeneratorFor(WorkShift.HoursPerDay hoursPerDay, final LocalDate date) {
        long daysBetween = Math.abs(ChronoUnit.DAYS.between(date, startPoint));

        if (startPoint.equals(date)) {
            return 0;
        }
        if (startPoint.isAfter(date)) {
            int shift = (int) (daysBetween % (getWorkingDayGenerators(hoursPerDay).length));
            return getWorkingDayGenerators(hoursPerDay).length - shift;
        } else {
            return (int) (daysBetween % (getWorkingDayGenerators(hoursPerDay).length));
        }
    }
}
