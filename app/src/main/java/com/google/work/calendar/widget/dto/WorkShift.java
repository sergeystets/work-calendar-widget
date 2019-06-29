package com.google.work.calendar.widget.dto;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkShift {

    NIGHT("Ночная смена", "н", LocalTime.of(0, 0), LocalTime.of(8, 0)),
    DAY("Дневная смена", "д", LocalTime.of(8, 0), LocalTime.of(16, 0)),
    EVENING("Вечерняя смена", "ч", LocalTime.of(16, 0), LocalTime.of(23, 59)),
    DAY_OFF("Выходной", "в", LocalTime.of(0, 0), LocalTime.of(23, 59));

    private String label;
    private String shortLabel;
    private LocalTime start;
    private LocalTime end;

    public boolean isNight() {
        return this == NIGHT;
    }

    public boolean isDay() {
        return this == DAY;
    }

    public boolean isEvening() {
        return this == EVENING;
    }

    public boolean isDayOff() {
        return this == DAY_OFF;
    }
}
