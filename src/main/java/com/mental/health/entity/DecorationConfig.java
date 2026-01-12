package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 装饰物配置实体
 */
@Data
@TableName("decoration_config")
public class DecorationConfig {

    @TableId(type = IdType.AUTO)
    private Integer id;

    // 装饰物基本信息
    private String decorationType;       // 装饰物类型（唯一）
    private String decorationName;       // 装饰物名称
    private String decorationIcon;       // 装饰物图标
    private String category;             // 分类

    // 解锁条件
    private String unlockCondition;      // 解锁条件描述
    private String unlockRequirement;    // 解锁要求（JSON）

    // 互动配置
    private Integer canInteract;         // 是否可互动
    private String interactionType;      // 互动类型
    private String interactionEffect;    // 互动效果描述

    // 视觉配置
    private BigDecimal defaultScale;     // 默认缩放
    private String sizeClass;            // 大小分类
    private String animationConfig;      // 动画配置（JSON）

    // 顺序和推荐
    private Integer displayOrder;        // 显示顺序
    private Integer isRecommended;       // 是否推荐

    private LocalDateTime createTime;
}
