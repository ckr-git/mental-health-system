package com.mental.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.MeditationExercise;
import com.mental.health.entity.MeditationSession;
import com.mental.health.service.MeditationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patient/meditation")
@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
public class MeditationController {

    @Autowired
    private MeditationService meditationService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/exercises")
    public Result<List<MeditationExercise>> getExercises() {
        return Result.success(meditationService.getExercises());
    }

    @PostMapping("/sessions/start")
    public Result<Long> startSession(@RequestBody Map<String, Long> body,
                                      @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        Long exerciseId = body.get("exerciseId");
        return Result.success(meditationService.startSession(userId, exerciseId));
    }

    @PostMapping("/sessions/{id}/complete")
    public Result<Void> completeSession(@PathVariable Long id,
                                         @RequestBody Map<String, Integer> body,
                                         @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        Integer actualSeconds = body.get("actualSeconds");
        meditationService.completeSession(userId, id, actualSeconds);
        return Result.success(null);
    }

    @GetMapping("/history")
    public Result<IPage<MeditationSession>> getHistory(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return Result.success(meditationService.getHistory(userId, pageNum, pageSize));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return Result.success(meditationService.getStats(userId));
    }
}
