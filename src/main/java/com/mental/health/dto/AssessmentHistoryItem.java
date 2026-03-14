package com.mental.health.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AssessmentHistoryItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long sessionId;
    private String scaleCode;
    private String scaleName;
    private Integer totalScore;
    private String severityLevel;
    private LocalDateTime submittedAt;
}
