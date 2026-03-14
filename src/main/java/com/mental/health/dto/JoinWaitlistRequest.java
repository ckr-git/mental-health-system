package com.mental.health.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class JoinWaitlistRequest {
    private Long doctorId;
    private LocalDate preferredDate;
    private LocalTime preferredTimeStart;
    private LocalTime preferredTimeEnd;
    private String appointmentType;
    private String symptoms;
}
