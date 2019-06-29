package com.google.work.calendar.widget.service;


import com.google.work.calendar.widget.dto.WorkingDay;

import java.time.LocalDate;

public interface WorkingDayGenerator {

    WorkingDay generate(LocalDate date);

    static WorkingDayGenerator nightShift() {
        return WorkingDay::night;
    }

    static WorkingDayGenerator dayShift() {
        return WorkingDay::day;
    }

    static WorkingDayGenerator eveningShift() {
        return WorkingDay::evening;
    }

    static WorkingDayGenerator dayOff() {
        return WorkingDay::dayOff;
    }
}
