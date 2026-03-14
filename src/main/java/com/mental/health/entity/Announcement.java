package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告实体类
 */
@Data
@TableName("announcement")
public class Announcement implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 公告标题
     */
    private String title;

    /**
     * 公告内容
     */
    private String content;

    /**
     * 公告类型: SYSTEM-系统公告, MAINTENANCE-维护公告, FEATURE-新功能, ACTIVITY-活动公告
     */
    private String announcementType;

    /**
     * 目标用户角色（多个用逗号分隔，空表示所有角色）
     */
    private String targetRoles;

    /**
     * 优先级: LOW-低, NORMAL-普通, HIGH-高
     */
    private String priority;

    /**
     * 发布状态: 0-草稿, 1-已发布, 2-已下线
     */
    private Integer status;

    /**
     * 是否置顶: 0-否, 1-是
     */
    private Integer pinned;

    /**
     * 发布人ID
     */
    private Long publisherId;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 生效时间
     */
    private LocalDateTime effectiveTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 阅读次数
     */
    private Integer viewCount;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
