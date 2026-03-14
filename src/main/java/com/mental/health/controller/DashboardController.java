package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.mapper.MoodDiaryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/patient/analytics")
@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
public class DashboardController {

    @Autowired
    private MoodDiaryMapper moodDiaryMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/mood-heatmap")
    public Result<List<Map<String, Object>>> getMoodHeatmap(
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(defaultValue = "3") int month,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        List<Map<String, Object>> heatmap = new ArrayList<>();
        try {
            List<Map<String, Object>> dataPoints = moodDiaryMapper.getDiaryDataPoints(userId, 90);
            for (Map<String, Object> dp : dataPoints) {
                Map<String, Object> cell = new HashMap<>();
                cell.put("date", dp.get("date") != null ? dp.get("date").toString() : null);
                cell.put("moodScore", dp.get("moodScore"));
                heatmap.add(cell);
            }
        } catch (Exception e) {
            // return empty on error
        }
        return Result.success(heatmap);
    }

    @GetMapping("/correlations")
    public Result<Map<String, Object>> getCorrelations(
            @RequestParam(defaultValue = "30") int range,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        Map<String, Object> result = new HashMap<>();
        result.put("scatterPoints", new ArrayList<>());
        result.put("correlations", Map.of("moodSleep", 0, "moodStress", 0, "moodEnergy", 0));
        try {
            List<Map<String, Object>> dataPoints = moodDiaryMapper.getDiaryDataPoints(userId, range);
            List<Map<String, Object>> scatterPoints = new ArrayList<>();
            for (Map<String, Object> dp : dataPoints) {
                Map<String, Object> point = new HashMap<>();
                point.put("mood", dp.get("moodScore"));
                point.put("sleep", dp.get("sleepQuality"));
                point.put("stress", dp.get("stressLevel"));
                point.put("energy", dp.get("energyLevel"));
                point.put("date", dp.get("date") != null ? dp.get("date").toString() : null);
                scatterPoints.add(point);
            }
            result.put("scatterPoints", scatterPoints);
        } catch (Exception e) {
            // return empty on error
        }
        return Result.success(result);
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> getSummary(
            @RequestParam(defaultValue = "7") int range,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        Map<String, Object> summary = new HashMap<>();
        summary.put("avgMood", 0);
        summary.put("avgSleep", 0);
        summary.put("avgStress", 0);
        summary.put("days", range);
        try {
            Double avgMood = moodDiaryMapper.avgMoodScore(userId, range);
            Double avgSleep = moodDiaryMapper.avgSleepQuality(userId, range);
            Double avgStress = moodDiaryMapper.avgStressLevel(userId, range);
            summary.put("avgMood", avgMood != null ? avgMood : 0);
            summary.put("avgSleep", avgSleep != null ? avgSleep : 0);
            summary.put("avgStress", avgStress != null ? avgStress : 0);
        } catch (Exception e) {
            // return defaults on error
        }
        return Result.success(summary);
    }
}
