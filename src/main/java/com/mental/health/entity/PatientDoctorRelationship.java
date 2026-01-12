package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 医患关系实体
 */
@Data
@TableName("patient_doctor_relationship")
public class PatientDoctorRelationship {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientId;

    private Long doctorId;

    /**
     * 关系状态: active-激活, inactive-停用
     */
    private String relationshipStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
