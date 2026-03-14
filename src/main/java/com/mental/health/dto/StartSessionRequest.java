package com.mental.health.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class StartSessionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String scaleCode;
}
