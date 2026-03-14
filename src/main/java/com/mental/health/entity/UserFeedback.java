package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户反馈实体类
 */
@Data
@TableName("user_feedback")
public class UserFeedback implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 反馈用户ID
     */
    private Long userId;

    /**
     * 反馈类型: BUG-bug报告, FEATURE-功能建议, COMPLAINT-投诉, OTHER-其他
     */
    private String feedbackType;

    /**
     * 反馈标题
     */
    private String title;

    /**
     * 反馈内容
     */
    private String content;

    /**
     * 联系方式
     */
    private String contactInfo;

    /**
     * 附件URL（多个用逗号分隔）
     */
    private String attachments;

    /**
     * 处理状态: 0-待处理, 1-处理中, 2-已完成, 3-已关闭
     */
    private Integer status;

    /**
     * 优先级: LOW-低, NORMAL-普通, HIGH-高
     */
    private String priority;

    /**
     * 管理员回复
     */
    private String adminReply;

    /**
     * 回复时间
     */
    private LocalDateTime replyTime;

    /**
     * 处理人ID
     */
    private Long handlerId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
