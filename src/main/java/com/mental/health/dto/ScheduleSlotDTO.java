package com.mental.health.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class ScheduleSlotDTO {
    private Long doctorId;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer slotDuration;
    private Integer maxPatients;
    private String location;
    private Integer active;
}
