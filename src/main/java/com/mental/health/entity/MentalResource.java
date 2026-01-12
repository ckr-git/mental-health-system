package com.mental.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 心理资源实体类
 */
@Data
@TableName("mental_resource")
public class MentalResource implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String title;
    private String type;
    private String category;
    private String content;
    private String fileUrl;
    private String coverImage;
    private String author;
    private Integer duration;
    private String tags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer downloadCount;
    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
