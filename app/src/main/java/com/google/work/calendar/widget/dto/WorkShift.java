package com.google.work.calendar.widget.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkShift {

    NIGHT("Ночная смена", "н"),
    DAY("Дневная смена", "д"),
    EVENING("Вечерняя смена", "ч"),
    DAY_OFF("Выходной", "в"),
    SLEEP("Отсыпной", "o");

    private String label;
    private String shortLabel;

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


    @Getter
    public enum HoursPerDay {
        EIGHT_HOURS(8),
        TWELVE_HOURS(12);

        private int hours;

        HoursPerDay(int hours) {
            this.hours = hours;
        }

        public static HoursPerDay fromHours(int hours) {
            if (hours == 8) {
                return EIGHT_HOURS;
            } else {
                return TWELVE_HOURS;
            }
        }

    }
}
