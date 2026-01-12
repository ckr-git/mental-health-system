package com.mental.health.algorithm;

import org.apache.commons.math3.linear.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 协同过滤推荐算法
 * 基于用户-资源评分矩阵，计算用户相似度，推荐个性化资源
 */
@Component
public class CollaborativeFiltering {

    /**
     * 计算余弦相似度
     */
    public double cosineSimilarity(Map<Long, Double> user1Ratings, Map<Long, Double> user2Ratings) {
        Set<Long> commonResources = new HashSet<>(user1Ratings.keySet());
        commonResources.retainAll(user2Ratings.keySet());

        if (commonResources.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (Long resourceId : commonResources) {
            double rating1 = user1Ratings.get(resourceId);
            double rating2 = user2Ratings.get(resourceId);

            dotProduct += rating1 * rating2;
            norm1 += rating1 * rating1;
            norm2 += rating2 * rating2;
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 计算所有用户之间的相似度
     */
    public Map<Long, Map<Long, Double>> calculateUserSimilarities(Map<Long, Map<Long, Double>> userRatings) {
        Map<Long, Map<Long, Double>> similarities = new HashMap<>();

        List<Long> userIds = new ArrayList<>(userRatings.keySet());

        for (int i = 0; i < userIds.size(); i++) {
            Long userId1 = userIds.get(i);
            Map<Long, Double> user1Ratings = userRatings.get(userId1);

            similarities.putIfAbsent(userId1, new HashMap<>());

            for (int j = i + 1; j < userIds.size(); j++) {
                Long userId2 = userIds.get(j);
                Map<Long, Double> user2Ratings = userRatings.get(userId2);

                double similarity = cosineSimilarity(user1Ratings, user2Ratings);

                similarities.get(userId1).put(userId2, similarity);
                similarities.putIfAbsent(userId2, new HashMap<>());
                similarities.get(userId2).put(userId1, similarity);
            }
        }

        return similarities;
    }

    /**
     * 获取K个最相似的用户
     */
    public List<Long> getKNearestNeighbors(Long targetUserId, Map<Long, Map<Long, Double>> similarities, int k) {
        if (!similarities.containsKey(targetUserId)) {
            return new ArrayList<>();
        }

        Map<Long, Double> userSimilarities = similarities.get(targetUserId);

        return userSimilarities.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(k)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 预测用户对资源的评分
     */
    public Map<Long, Double> predictRatings(Long targetUserId, Map<Long, Map<Long, Double>> userRatings,
                                             Map<Long, Map<Long, Double>> similarities, int k) {
        Map<Long, Double> predictions = new HashMap<>();

        List<Long> neighbors = getKNearestNeighbors(targetUserId, similarities, k);

        if (neighbors.isEmpty()) {
            return predictions;
        }

        Map<Long, Double> targetUserRatings = userRatings.getOrDefault(targetUserId, new HashMap<>());

        // 收集所有邻居评分过但目标用户未评分的资源
        Set<Long> candidateResources = new HashSet<>();
        for (Long neighborId : neighbors) {
            Map<Long, Double> neighborRatings = userRatings.get(neighborId);
            candidateResources.addAll(neighborRatings.keySet());
        }
        candidateResources.removeAll(targetUserRatings.keySet());

        // 预测评分
        for (Long resourceId : candidateResources) {
            double numerator = 0.0;
            double denominator = 0.0;

            for (Long neighborId : neighbors) {
                Map<Long, Double> neighborRatings = userRatings.get(neighborId);
                if (neighborRatings.containsKey(resourceId)) {
                    double similarity = similarities.get(targetUserId).get(neighborId);
                    double rating = neighborRatings.get(resourceId);

                    numerator += similarity * rating;
                    denominator += Math.abs(similarity);
                }
            }

            if (denominator > 0) {
                double predictedRating = numerator / denominator;
                predictions.put(resourceId, predictedRating);
            }
        }

        return predictions;
    }

    /**
     * 推荐Top-N资源
     */
    public List<Long> recommendTopN(Long targetUserId, Map<Long, Map<Long, Double>> userRatings,
                                     Map<Long, Map<Long, Double>> similarities, int k, int n) {
        Map<Long, Double> predictions = predictRatings(targetUserId, userRatings, similarities, k);

        return predictions.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(n)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
