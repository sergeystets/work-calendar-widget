package com.google.work.calendar.widget.dto;

import android.graphics.Color;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CalendarEvent {

    private LocalDateTime from;
    private LocalDateTime to;
    private Color color;
    private String name;
    private String description;
}
