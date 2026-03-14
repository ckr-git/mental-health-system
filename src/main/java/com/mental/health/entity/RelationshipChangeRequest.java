package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 患者分配变更请求实体
 */
@Data
@TableName("relationship_change_request")
public class RelationshipChangeRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientId;

    private Long doctorId;

    /**
     * 操作类型: claim-认领, release-释放
     */
    private String operationType;

    /**
     * 请求状态: pending-待审核, approved-通过, rejected-拒绝
     */
    private String requestStatus;

    private String requestReason;

    private Long adminId;

    private String adminNote;

    private LocalDateTime approvalTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
