package com.mental.health.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookAppointmentRequest {
    private Long doctorId;
    private LocalDateTime appointmentTime;
    private String appointmentType;
    private String symptoms;
    private String notes;
}
