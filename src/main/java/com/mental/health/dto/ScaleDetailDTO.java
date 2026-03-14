package com.mental.health.dto;

import com.mental.health.entity.AssessmentScaleItem;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class ScaleDetailDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String scaleCode;
    private String scaleName;
    private String introText;
    private List<AssessmentScaleItem> items;
}
