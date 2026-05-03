package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_memory_summary")
public class AiMemorySummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long patientId;
    private String summaryType;     // ROLLING,SESSION_END,CROSS_SESSION
    private String summaryContent;
    private String keyTopicsJson;
    private String identifiedEmotionsJson;
    private String identifiedTriggersJson;
    private String suggestedCopingJson;
    private String riskIndicatorsJson;
    private Long coversMessageFrom;
    private Long coversMessageTo;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
