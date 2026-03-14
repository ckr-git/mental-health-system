package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.MentalResource;
import com.mental.health.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推荐系统控制器
 */
@RestController
@RequestMapping("/api/patient/recommendations")
@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取个性化推荐资源
     */
    @GetMapping("/resources")
    public Result<List<MentalResource>> getRecommendedResources(
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        List<MentalResource> resources = recommendationService.recommendResources(userId, limit);
        return Result.success(resources);
    }

    /**
     * 记录用户行为
     */
    @PostMapping("/record-behavior")
    public Result<String> recordBehavior(
            @RequestParam Long resourceId,
            @RequestParam String action,
            @RequestParam(required = false) Double rating,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        recommendationService.recordUserBehavior(userId, resourceId, action, rating);
        return Result.success("记录成功");
    }
}
