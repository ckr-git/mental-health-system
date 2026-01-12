package com.mental.health.service;

import com.alibaba.fastjson2.JSON;
import com.mental.health.entity.MoodDiary;
import com.mental.health.entity.TimeCapsule;
import com.mental.health.mapper.MoodDiaryMapper;
import com.mental.health.mapper.TimeCapsuleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 时光信箱服务
 */
@Service
public class TimeCapsuleService {

    @Autowired
    private TimeCapsuleMapper capsuleMapper;

    @Autowired
    private MoodDiaryMapper diaryMapper;

    /**
     * 写信
     */
    @Transactional
    public TimeCapsule writeLetter(TimeCapsule capsule, Long userId) {
        capsule.setUserId(userId);
        capsule.setStatus("sealed");
        
        // 计算解锁天数
        if (capsule.getUnlockDate() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), capsule.getUnlockDate());
            capsule.setUnlockDays((int) days);
        }

        // 自动填充触发数据
        fillTriggerData(capsule, userId);

        capsuleMapper.insert(capsule);
        return capsule;
    }

    /**
     * 填充触发数据（根据近期心情）
     */
    private void fillTriggerData(TimeCapsule capsule, Long userId) {
        // 获取近7天的日记
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Double avgMood = diaryMapper.getAvgMoodScore(userId, sevenDaysAgo);
        
        if (avgMood != null) {
            capsule.setTriggerMoodAvg(BigDecimal.valueOf(avgMood));
            
            // 判断趋势
            List<MoodDiary> recentDiaries = getRecentDiariesForTrend(userId);
            if (recentDiaries.size() >= 3) {
                String trend = calculateMoodTrend(recentDiaries);
                capsule.setTriggerMoodTrend(trend);
            }
            
            // 关联日记ID
            List<Long> diaryIds = recentDiaries.stream()
                    .map(MoodDiary::getId)
                    .limit(5)
                    .collect(Collectors.toList());
            capsule.setRelatedDiaryIds(JSON.toJSONString(diaryIds));
        }
    }

    /**
     * 获取近期日记用于趋势分析
     */
    private List<MoodDiary> getRecentDiariesForTrend(Long userId) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<MoodDiary> page = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MoodDiary> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MoodDiary>()
                .eq(MoodDiary::getUserId, userId)
                .ge(MoodDiary::getCreateTime, sevenDaysAgo)
                .orderByDesc(MoodDiary::getCreateTime);
        return diaryMapper.selectPage(page, wrapper).getRecords();
    }

    /**
     * 计算心情趋势
     */
    private String calculateMoodTrend(List<MoodDiary> diaries) {
        if (diaries.size() < 2) return "stable";
        
        int recentAvg = diaries.subList(0, Math.min(3, diaries.size())).stream()
                .mapToInt(MoodDiary::getMoodScore)
                .sum() / Math.min(3, diaries.size());
        
        int olderAvg = diaries.subList(Math.min(3, diaries.size()), diaries.size()).stream()
                .mapToInt(MoodDiary::getMoodScore)
                .sum() / (diaries.size() - Math.min(3, diaries.size()));
        
        if (recentAvg > olderAvg + 1) return "up";
        if (recentAvg < olderAvg - 1) return "down";
        return "stable";
    }

    /**
     * 获取用户的所有信件
     */
    public List<TimeCapsule> getUserLetters(Long userId) {
        return capsuleMapper.getByUserId(userId);
    }

    /**
     * 检查并解锁到期的信件
     */
    @Transactional
    public List<TimeCapsule> checkAndUnlockLetters(Long userId) {
        LocalDate today = LocalDate.now();
        List<TimeCapsule> unlockable = capsuleMapper.getUnlockable(userId, today);

        // 筛选出真正满足条件的信件
        List<TimeCapsule> reallyUnlockable = new ArrayList<>();

        for (TimeCapsule letter : unlockable) {
            boolean canUnlock = true;

            // 如果是条件触发类型，需要检查特殊条件
            if ("condition".equals(letter.getUnlockType()) && letter.getUnlockConditions() != null) {
                try {
                    List<String> conditions = JSON.parseArray(letter.getUnlockConditions(), String.class);

                    // 检查心情条件
                    if (conditions.contains("mood_low") || conditions.contains("mood_high")) {
                        // 获取最新的日记
                        MoodDiary latestDiary = diaryMapper.getLatestDiary(userId);
                        if (latestDiary != null) {
                            int moodScore = latestDiary.getMoodScore();

                            // 检查是否满足心情条件
                            boolean moodConditionMet = false;
                            if (conditions.contains("mood_low") && moodScore <= 3) {
                                moodConditionMet = true;
                            }
                            if (conditions.contains("mood_high") && moodScore >= 8) {
                                moodConditionMet = true;
                            }

                            // 如果设置了心情条件但不满足，则不能解锁（除非有30天兜底）
                            if (!moodConditionMet && !conditions.contains("days_30")) {
                                canUnlock = false;
                            }
                        } else {
                            // 没有日记记录，无法判断心情条件
                            if (!conditions.contains("days_30")) {
                                canUnlock = false;
                            }
                        }
                    }
                } catch (Exception e) {
                    // 解析条件失败，默认可以解锁（日期已到）
                }
            }

            if (canUnlock) {
                capsuleMapper.unlockLetter(letter.getId());
                reallyUnlockable.add(letter);
            }
        }

        return reallyUnlockable;
    }

    /**
     * 解锁信件（手动）
     */
    @Transactional
    public TimeCapsule unlockLetter(Long letterId, Long userId) {
        TimeCapsule letter = capsuleMapper.selectById(letterId);
        if (letter != null && letter.getUserId().equals(userId)) {
            if ("sealed".equals(letter.getStatus())) {
                capsuleMapper.unlockLetter(letterId);
                letter.setStatus("unlocked");
                letter.setUnlockTime(LocalDateTime.now());
            }
            return capsuleMapper.selectById(letterId);
        }
        return null;
    }

    /**
     * 阅读信件
     */
    @Transactional
    public TimeCapsule readLetter(Long letterId, Long userId) {
        TimeCapsule letter = capsuleMapper.selectById(letterId);
        if (letter != null && letter.getUserId().equals(userId)) {
            if ("unlocked".equals(letter.getStatus())) {
                capsuleMapper.markAsRead(letterId);
            }
            return capsuleMapper.selectById(letterId);
        }
        return null;
    }

    /**
     * 回复信件
     */
    @Transactional
    public boolean replyLetter(Long letterId, Long userId, String replyContent) {
        TimeCapsule letter = capsuleMapper.selectById(letterId);
        if (letter != null && letter.getUserId().equals(userId)) {
            letter.setReplyContent(replyContent);
            letter.setReplyTime(LocalDateTime.now());
            letter.setStatus("replied");
            return capsuleMapper.updateById(letter) > 0;
        }
        return false;
    }

    /**
     * 获取信件详情
     */
    public TimeCapsule getLetterDetail(Long letterId, Long userId) {
        TimeCapsule letter = capsuleMapper.selectById(letterId);
        if (letter != null && letter.getUserId().equals(userId)) {
            return letter;
        }
        return null;
    }

    /**
     * 智能推荐信件类型
     */
    public com.mental.health.entity.LetterRecommendation analyzeAndRecommend(Long userId) {
        // 获取近7天心情数据
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Double avgMood = diaryMapper.getAvgMoodScore(userId, sevenDaysAgo);

        // 获取最近日记用于趋势分析
        List<com.mental.health.entity.MoodDiary> recentDiaries = diaryMapper
                .selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.mental.health.entity.MoodDiary>()
                        .eq("user_id", userId)
                        .ge("create_time", sevenDaysAgo)
                        .orderByDesc("create_time")
                        .last("LIMIT 7"));

        com.mental.health.entity.LetterRecommendation recommendation = new com.mental.health.entity.LetterRecommendation();
        recommendation.setAvgMood(avgMood != null ? avgMood : 6.0);

        // 分析趋势
        String trend = analyzeMoodTrend(recentDiaries);
        recommendation.setMoodTrend(trend);

        // 根据趋势和平均分推荐信件类型
        if (avgMood == null || avgMood < 4.0) {
            // 心情低落 - 推荐希望信
            recommendation.setRecommendType("hope");
            recommendation.setTypeTitle("给低谷中的自己一封希望信");
            recommendation.setTypeDescription("写下对未来的期待,相信一切都会好起来");
            recommendation.setReason("你最近可能有些低落,给未来的自己写封充满希望的信吧");
            recommendation.setTemplate("亲爱的未来的我:\n\n现在的日子虽然有些艰难,但我相信你一定能度过这段时光。记住,黑夜过后总会迎来黎明。\n\n我希望当你读到这封信时,已经...\n\n加油！过去的我");
        } else if (avgMood >= 8.0) {
            // 心情很好 - 推荐表扬信
            recommendation.setRecommendType("praise");
            recommendation.setTypeTitle("给状态不错的自己一封表扬信");
            recommendation.setTypeDescription("记录当下的美好,肯定自己的进步");
            recommendation.setReason("你最近状态不错,给自己写封表扬信,记录这份美好吧!");
            recommendation.setTemplate("Hi,未来的我:\n\n最近的你真的很棒!你...(写下值得表扬的事)\n\n希望未来的你也能保持这份美好。\n\n点赞！现在的我");
        } else if ("up".equals(trend)) {
            // 趋势向上 - 推荐目标信
            recommendation.setRecommendType("goal");
            recommendation.setTypeTitle("给正在进步的自己设定目标");
            recommendation.setTypeDescription("趁着好状态,给自己定个小目标");
            recommendation.setReason("你的心情正在好转,趁现在给未来的自己定个目标吧!");
            recommendation.setTemplate("未来的我:\n\n我希望当你打开这封信时,已经实现了这些目标:\n\n1. ...\n2. ...\n3. ...\n\n现在就开始行动吧!\n\n期待你的好消息!");
        } else {
            // 其他情况 - 推荐感恩信
            recommendation.setRecommendType("thanks");
            recommendation.setTypeTitle("给自己写一封感恩信");
            recommendation.setTypeDescription("感谢那些支持你的人和事");
            recommendation.setReason("写封感恩信,回顾那些温暖的时刻");
            recommendation.setTemplate("亲爱的未来的我:\n\n今天想感谢...\n\n这段时间我特别感激...\n\n希望你也记得这些美好。\n\n感恩的心,现在的我");
        }

        return recommendation;
    }

    /**
     * 分析心情趋势
     */
    private String analyzeMoodTrend(List<com.mental.health.entity.MoodDiary> diaries) {
        if (diaries == null || diaries.size() < 2) {
            return "stable";
        }

        // 计算前后半段的平均分
        int mid = diaries.size() / 2;
        double recentAvg = diaries.subList(0, mid).stream()
                .mapToInt(com.mental.health.entity.MoodDiary::getMoodScore)
                .average()
                .orElse(6.0);

        double olderAvg = diaries.subList(mid, diaries.size()).stream()
                .mapToInt(com.mental.health.entity.MoodDiary::getMoodScore)
                .average()
                .orElse(6.0);

        // 判断趋势
        if (recentAvg - olderAvg > 1.0) {
            return "up";
        } else if (olderAvg - recentAvg > 1.0) {
            return "down";
        } else {
            return "stable";
        }
    }
}
