package com.mental.health.service;

import com.mental.health.algorithm.CollaborativeFiltering;
import com.mental.health.entity.MentalResource;
import com.mental.health.mapper.MentalResourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务 - 基于协同过滤算法
 */
@Service
public class RecommendationService {

    @Autowired
    private CollaborativeFiltering collaborativeFiltering;

    @Autowired
    private MentalResourceMapper mentalResourceMapper;

    /**
     * 为用户推荐资源
     * 
     * @param userId 用户ID
     * @param topN 推荐数量
     * @return 推荐的资源列表
     */
    public List<MentalResource> recommendResources(Long userId, int topN) {
        // 模拟用户评分数据（实际应从数据库获取）
        Map<Long, Map<Long, Double>> userRatings = buildUserRatingsMatrix();

        // 计算用户相似度
        Map<Long, Map<Long, Double>> similarities = collaborativeFiltering.calculateUserSimilarities(userRatings);

        // 获取推荐资源ID列表（K=10个最相似用户）
        List<Long> recommendedIds = collaborativeFiltering.recommendTopN(userId, userRatings, similarities, 10, topN);

        // 根据ID获取资源详情
        if (recommendedIds.isEmpty()) {
            // 如果没有推荐结果，返回热门资源
            return getHotResources(topN);
        }

        return recommendedIds.stream()
                .map(id -> mentalResourceMapper.selectById(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 构建用户-资源评分矩阵
     * 实际应用中应从数据库的用户行为数据构建
     */
    private Map<Long, Map<Long, Double>> buildUserRatingsMatrix() {
        // TODO: 从数据库获取真实的用户行为数据
        // 这里返回模拟数据作为示例
        Map<Long, Map<Long, Double>> userRatings = new HashMap<>();
        
        // 示例：用户1的评分
        Map<Long, Double> user1Ratings = new HashMap<>();
        user1Ratings.put(1L, 5.0);
        user1Ratings.put(2L, 4.0);
        user1Ratings.put(3L, 3.0);
        userRatings.put(1L, user1Ratings);
        
        // 示例：用户2的评分
        Map<Long, Double> user2Ratings = new HashMap<>();
        user2Ratings.put(1L, 4.5);
        user2Ratings.put(2L, 4.5);
        user2Ratings.put(4L, 5.0);
        userRatings.put(2L, user2Ratings);
        
        return userRatings;
    }

    /**
     * 获取热门资源（备选方案）
     */
    private List<MentalResource> getHotResources(int limit) {
        // 实现热门资源查询逻辑
        return new ArrayList<>();
    }

    /**
     * 基于内容的推荐（根据用户症状）
     */
    public List<MentalResource> recommendBySymptoms(String symptomType, String tags, int limit) {
        // TODO: 实现基于症状的内容推荐
        return new ArrayList<>();
    }

    /**
     * 记录用户行为（用于更新评分矩阵）
     */
    public void recordUserBehavior(Long userId, Long resourceId, String action, Double rating) {
        // TODO: 记录用户浏览、下载、点赞等行为，用于协同过滤
        // 这些行为数据将用于构建评分矩阵
    }
}
