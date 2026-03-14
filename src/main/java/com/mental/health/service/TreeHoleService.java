package com.mental.health.service;

import com.mental.health.entity.MoodDiary;
import com.mental.health.entity.TreeHole;
import com.mental.health.mapper.MoodDiaryMapper;
import com.mental.health.mapper.TreeHoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 心情树洞服务
 */
@Service
public class TreeHoleService {

    @Autowired
    private TreeHoleMapper treeHoleMapper;

    @Autowired
    private MoodDiaryMapper moodDiaryMapper;

    /**
     * 添加倾诉记录
     */
    @Transactional
    public TreeHole addTreeHole(TreeHole treeHole, Long userId) {
        treeHole.setUserId(userId);
        treeHole.setIsExpired(0);
        treeHole.setCanView(0);
        treeHole.setViewCount(0);

        // 计算过期时间
        LocalDateTime expireTime = calculateExpireTime(treeHole.getExpireType());
        treeHole.setExpireTime(expireTime);

        // 如果是conditional类型，设置为可查看
        if ("conditional".equals(treeHole.getExpireType())) {
            treeHole.setCanView(1);
        }

        treeHoleMapper.insert(treeHole);
        return treeHole;
    }

    /**
     * 计算过期时间
     */
    private LocalDateTime calculateExpireTime(String expireType) {
        LocalDateTime now = LocalDateTime.now();

        switch (expireType) {
            case "5sec":
                return now.plusSeconds(5);
            case "1hour":
                return now.plusHours(1);
            case "tonight":
                // 今晚12点
                return LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
            case "tomorrow":
                // 明天凌晨
                return LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIN);
            case "forever":
                // 永久保存，设置为null或很远的未来
                return null;
            case "conditional":
                // 条件触发，设置为30天后
                return now.plusDays(30);
            default:
                return now.plusHours(1);
        }
    }

    /**
     * 获取用户的活跃倾诉记录（未过期的）
     */
    public List<TreeHole> getActiveTreeHoles(Long userId) {
        return treeHoleMapper.getActiveByUserId(userId);
    }

    /**
     * 获取用户的档案馆（所有记录）
     */
    public Map<String, List<TreeHole>> getArchive(Long userId) {
        // 检查用户是否有权限查看档案馆
        if (!canViewArchive(userId)) {
            return new HashMap<>();
        }

        List<TreeHole> allRecords = treeHoleMapper.getArchiveByUserId(userId);

        // 按倾诉对象分类
        Map<String, List<TreeHole>> archive = allRecords.stream()
                .collect(Collectors.groupingBy(
                        th -> th.getSpeakToType() + ":" + th.getSpeakToName(),
                        Collectors.toList()
                ));

        return archive;
    }

    /**
     * 检查用户是否可以查看档案馆
     * 条件：最近3天平均心情 <3分 或 >8分
     */
    public boolean canViewArchive(Long userId) {
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        // 获取最近3天的日记
        Double avgMood = moodDiaryMapper.getAvgMoodScore(userId, threeDaysAgo);

        if (avgMood == null) {
            return false;  // 没有足够的数据
        }

        return avgMood < 3.0 || avgMood > 8.0;
    }

    /**
     * 查看倾诉详情（增加查看次数）
     */
    @Transactional
    public TreeHole viewTreeHole(Long id, Long userId) {
        TreeHole treeHole = treeHoleMapper.selectById(id);

        if (treeHole != null && treeHole.getUserId().equals(userId)) {
            // 检查是否可以查看
            if ("conditional".equals(treeHole.getExpireType())) {
                // 条件类型需要满足查看条件
                if (!checkViewCondition(treeHole, userId)) {
                    return null;  // 不满足查看条件
                }
            }

            // 增加查看次数
            treeHoleMapper.incrementViewCount(id, LocalDateTime.now());

            return treeHoleMapper.selectById(id);
        }

        return null;
    }

    /**
     * 获取指定ID的树洞记录(不增加查看次数)
     */
    public TreeHole getTreeHoleById(Long id, Long userId) {
        TreeHole treeHole = treeHoleMapper.selectById(id);
        if (treeHole != null && treeHole.getUserId().equals(userId)) {
            return treeHole;
        }
        return null;
    }

    /**
     * 检查查看条件并返回友好的错误信息
     * @return null表示满足条件,否则返回错误信息
     */
    public String checkViewConditionMessage(TreeHole treeHole, Long userId) {
        if (!"conditional".equals(treeHole.getExpireType())) {
            return null;  // 非条件类型,直接允许查看
        }

        String viewCondition = treeHole.getViewCondition();
        if (viewCondition == null || viewCondition.isEmpty()) {
            return null;
        }

        // 解析查看条件
        if (viewCondition.contains("mood_low")) {
            // 检查用户最新心情是否低落 (<=3分)
            MoodDiary latestDiary = moodDiaryMapper.getLatestDiary(userId);
            if (latestDiary == null) {
                return "需要先写一篇心情日记才能查看此内容";
            }
            if (latestDiary.getMoodScore() > 3) {
                return "当前心情指数为" + latestDiary.getMoodScore() + "分,需要心情低落时(≤3分)才能查看";
            }
            return null;  // 满足条件
        } else if (viewCondition.contains("mood_high")) {
            // 检查用户最新心情是否高涨 (>=8分)
            MoodDiary latestDiary = moodDiaryMapper.getLatestDiary(userId);
            if (latestDiary == null) {
                return "需要先写一篇心情日记才能查看此内容";
            }
            if (latestDiary.getMoodScore() < 8) {
                return "当前心情指数为" + latestDiary.getMoodScore() + "分,需要心情高涨时(≥8分)才能查看";
            }
            return null;  // 满足条件
        } else if (viewCondition.contains("after_30days")) {
            // 检查是否已经过了30天
            LocalDateTime unlockTime = treeHole.getCreateTime().plusDays(30);
            if (LocalDateTime.now().isBefore(unlockTime)) {
                long daysLeft = java.time.Duration.between(LocalDateTime.now(), unlockTime).toDays();
                return "此内容将在" + daysLeft + "天后解锁(30天后可查看)";
            }
            return null;  // 满足条件
        }

        return null;
    }

    /**
     * 检查是否满足查看条件
     */
    private boolean checkViewCondition(TreeHole treeHole, Long userId) {
        String viewCondition = treeHole.getViewCondition();

        if (viewCondition == null || viewCondition.isEmpty()) {
            return true;
        }

        // 解析查看条件
        if (viewCondition.contains("mood_low")) {
            // 检查用户最新心情是否低落 (<=3分)
            MoodDiary latestDiary = moodDiaryMapper.getLatestDiary(userId);
            return latestDiary != null && latestDiary.getMoodScore() <= 3;
        } else if (viewCondition.contains("mood_high")) {
            // 检查用户最新心情是否高涨 (>=8分)
            MoodDiary latestDiary = moodDiaryMapper.getLatestDiary(userId);
            return latestDiary != null && latestDiary.getMoodScore() >= 8;
        } else if (viewCondition.contains("after_30days")) {
            // 检查是否已经过了30天
            return LocalDateTime.now().isAfter(treeHole.getCreateTime().plusDays(30));
        }

        return true;
    }

    /**
     * 清理过期内容（定时任务调用）
     */
    @Transactional
    public int cleanExpiredContent() {
        return treeHoleMapper.markExpired(LocalDateTime.now());
    }

    /**
     * 删除倾诉记录
     */
    @Transactional
    public boolean deleteTreeHole(Long id, Long userId) {
        TreeHole treeHole = treeHoleMapper.selectById(id);

        if (treeHole != null && treeHole.getUserId().equals(userId)) {
            return treeHoleMapper.deleteById(id) > 0;
        }

        return false;
    }

    /**
     * 按倾诉对象类型获取统计
     */
    public Map<String, Integer> getStatsByType(Long userId) {
        List<TreeHole> allRecords = treeHoleMapper.getArchiveByUserId(userId);

        Map<String, Integer> stats = new HashMap<>();
        for (TreeHole record : allRecords) {
            String key = record.getSpeakToType() + ":" + record.getSpeakToName();
            stats.put(key, stats.getOrDefault(key, 0) + 1);
        }

        return stats;
    }
}
