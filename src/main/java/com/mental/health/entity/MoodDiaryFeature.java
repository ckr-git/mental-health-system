package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mood_diary_feature")
public class MoodDiaryFeature {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long moodDiaryId;
    private Long userId;
    private String extractionStatus; // PENDING,SUCCESS,FAILED,REVIEWED
    private BigDecimal sentimentScore;
    private String primaryEmotionCode;
    private String emotionTagsJson;
    private String triggerTagsJson;
    private String copingTagsJson;
    private String riskSignalJson;
    private String aiSummary;
    private LocalDateTime extractedAt;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
