package com.mental.health.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ScheduleOverrideRequest {
    private LocalDate overrideDate;
    private String overrideType; // AVAILABLE or UNAVAILABLE
    private String startTime;    // HH:mm
    private String endTime;      // HH:mm
    private Integer slotDuration;
    private String reason;
}
