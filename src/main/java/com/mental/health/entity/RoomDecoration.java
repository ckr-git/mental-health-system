package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房间装饰物实体
 */
@Data
@TableName("room_decoration")
public class RoomDecoration {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    // 装饰物信息
    private String decorationType;      // 装饰物类型
    private String decorationName;       // 装饰物名称
    private String decorationIcon;       // 装饰物图标

    // 位置信息
    private Integer positionX;           // X坐标（百分比）
    private Integer positionY;           // Y坐标（百分比）
    private Integer positionZ;           // Z-index层级
    private BigDecimal scale;            // 缩放比例
    private Integer rotation;            // 旋转角度

    // 状态信息
    private Integer isUnlocked;          // 是否已解锁
    private String unlockCondition;      // 解锁条件
    private Integer isActive;            // 是否激活显示

    // 互动数据
    private Integer interactionCount;    // 互动次数
    private LocalDateTime lastInteractionTime;  // 最后互动时间
    private String customData;           // 自定义数据（JSON）

    // 时间戳
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
