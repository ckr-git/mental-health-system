package com.mental.health.entity;

import lombok.Data;

/**
 * 信件推荐数据
 */
@Data
public class LetterRecommendation {

    /**
     * 推荐的信件类型
     */
    private String recommendType;  // praise, hope, thanks, goal

    /**
     * 推荐理由
     */
    private String reason;

    /**
     * 心情趋势
     */
    private String moodTrend;  // up, stable, down

    /**
     * 平均心情分数
     */
    private Double avgMood;

    /**
     * 推荐的信件模板
     */
    private String template;

    /**
     * 信件类型标题
     */
    private String typeTitle;

    /**
     * 信件类型描述
     */
    private String typeDescription;
}
