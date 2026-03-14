package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.entity.MeditationExercise;
import com.mental.health.entity.MeditationSession;
import com.mental.health.mapper.MeditationExerciseMapper;
import com.mental.health.mapper.MeditationSessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class MeditationService {

    @Autowired
    private MeditationExerciseMapper exerciseMapper;

    @Autowired
    private MeditationSessionMapper sessionMapper;

    public List<MeditationExercise> getExercises() {
        return exerciseMapper.findAllActive();
    }

    @Transactional
    public Long startSession(Long userId, Long exerciseId) {
        MeditationExercise exercise = exerciseMapper.selectById(exerciseId);
        if (exercise == null) {
            throw new RuntimeException("练习不存在");
        }

        MeditationSession session = new MeditationSession();
        session.setUserId(userId);
        session.setExerciseId(exerciseId);
        session.setSessionStatus("STARTED");
        session.setStartedAt(LocalDateTime.now());
        session.setPlannedSeconds(exercise.getDurationSeconds());
        session.setActualSeconds(0);
        sessionMapper.insert(session);
        return session.getId();
    }

    @Transactional
    public void completeSession(Long userId, Long sessionId, Integer actualSeconds) {
        MeditationSession session = sessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new RuntimeException("会话不存在");
        }
        if (!"STARTED".equals(session.getSessionStatus())) {
            throw new RuntimeException("会话已结束");
        }
        session.setSessionStatus("COMPLETED");
        session.setCompletedAt(LocalDateTime.now());
        session.setActualSeconds(actualSeconds != null ? actualSeconds : 0);
        sessionMapper.updateById(session);
    }

    public IPage<MeditationSession> getHistory(Long userId, int pageNum, int pageSize) {
        Page<MeditationSession> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MeditationSession> w = new LambdaQueryWrapper<>();
        w.eq(MeditationSession::getUserId, userId)
                .eq(MeditationSession::getSessionStatus, "COMPLETED")
                .orderByDesc(MeditationSession::getStartedAt);
        return sessionMapper.selectPage(page, w);
    }

    public Map<String, Object> getStats(Long userId) {
        Long totalSeconds = sessionMapper.totalPracticeSeconds(userId);
        Long completedCount = sessionMapper.completedCount(userId);
        return Map.of(
                "totalMinutes", totalSeconds / 60,
                "completedSessions", completedCount
        );
    }
}
