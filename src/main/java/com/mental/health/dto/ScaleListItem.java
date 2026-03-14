package com.mental.health.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ScaleListItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String scaleCode;
    private String scaleName;
    private String scaleType;
    private String introText;
    private Integer estimatedMinutes;
    private Long itemCount;
}
