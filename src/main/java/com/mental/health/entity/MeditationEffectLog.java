package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("meditation_effect_log")
public class MeditationEffectLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long meditationSessionId;
    private Long meditationPrescriptionId;
    private Long patientId;
    private String effectStatus; // RECORDED,REVIEWED
    private Integer preMoodScore;
    private Integer postMoodScore;
    private Integer preStressScore;
    private Integer postStressScore;
    private Integer moodChange;
    private Integer stressChange;
    private Integer perceivedBenefitScore;
    private String note;
    private LocalDateTime recordedAt;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
