package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mental.health.entity.DecorationConfig;
import com.mental.health.entity.MoodDiary;
import com.mental.health.entity.RoomDecoration;
import com.mental.health.entity.TimeCapsule;
import com.mental.health.mapper.DecorationConfigMapper;
import com.mental.health.mapper.MoodDiaryMapper;
import com.mental.health.mapper.RoomDecorationMapper;
import com.mental.health.mapper.TimeCapsuleMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 房间装饰服务
 */
@Slf4j
@Service
public class RoomDecorationService {

    @Autowired
    private RoomDecorationMapper decorationMapper;

    @Autowired
    private DecorationConfigMapper configMapper;

    @Autowired
    private MoodDiaryMapper moodDiaryMapper;

    @Autowired
    private TimeCapsuleMapper capsuleMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取用户房间中当前激活的装饰物，按 Z 轴顺序排序
     */
    public List<RoomDecoration> getUserDecorations(Long userId) {
        QueryWrapper<RoomDecoration> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("is_active", 1)
                .orderByAsc("position_z");
        return decorationMapper.selectList(wrapper);
    }

    /**
     * 获取用户的所有装饰物记录（包含未激活），并按装饰类型去重
     * 去重规则：优先保留最近更新/创建的数据
     */
    public List<RoomDecoration> getAllUserDecorations(Long userId) {
        QueryWrapper<RoomDecoration> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .orderByDesc("update_time")
                .orderByDesc("create_time");
        List<RoomDecoration> decorations = decorationMapper.selectList(wrapper);
        if (decorations.isEmpty()) {
            return decorations;
        }

        Map<String, RoomDecoration> deduplicated = new LinkedHashMap<>();
        for (RoomDecoration decoration : decorations) {
            if (!StringUtils.hasText(decoration.getDecorationType())) {
                continue;
            }
            // 由于按时间倒序查询，这里只保留第一条
            deduplicated.putIfAbsent(decoration.getDecorationType(), decoration);
        }

        if (deduplicated.size() < decorations.size()) {
            log.warn("Detected {} duplicate decoration records for user {}",
                    decorations.size() - deduplicated.size(), userId);
        }

        List<RoomDecoration> result = new ArrayList<>(deduplicated.values());
        result.sort(Comparator.comparing(
                RoomDecoration::getCreateTime,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));
        return result;
    }

    /**
     * 获取全部装饰物配置
     */
    public List<DecorationConfig> getAllConfigs() {
        QueryWrapper<DecorationConfig> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("display_order");
        return configMapper.selectList(wrapper);
    }

    /**
     * 获取推荐的装饰物配置
     */
    public List<DecorationConfig> getRecommendedConfigs() {
        QueryWrapper<DecorationConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("is_recommended", 1)
                .orderByAsc("display_order");
        return configMapper.selectList(wrapper);
    }

    /**
     * 检查并根据用户行为解锁装饰物
     *
     * @return result.newlyUnlocked  本次新解锁的装饰名称列表
     * @return result.totalUnlocked  当前已解锁总数
     * @return result.totalAvailable 配置中可解锁的总数
     */
    @Transactional
    public Map<String, Object> checkAndUnlockDecorations(Long userId) {
        List<DecorationConfig> allConfigs = getAllConfigs();
        // 使用 getAllUserDecorations 获取所有装饰（包括历史未激活记录）
        List<RoomDecoration> userDecorations = getAllUserDecorations(userId);

        // 用户已拥有的装饰类型
        Set<String> ownedTypes = userDecorations.stream()
                .map(RoomDecoration::getDecorationType)
                .collect(Collectors.toSet());

        List<String> newlyUnlocked = new ArrayList<>();
        int totalUnlocked = 0;

        // 判断是否首次解锁（没有任何装饰记录）
        boolean isFirstTime = userDecorations.isEmpty();
        int autoActiveCount = 0;
        final int MAX_AUTO_ACTIVE = 4; // 首次最多自动激活的装饰数量

        for (DecorationConfig config : allConfigs) {
            String type = config.getDecorationType();
            if (!StringUtils.hasText(type)) {
                continue;
            }

            // 已拥有该类型则只统计数量
            if (ownedTypes.contains(type)) {
                totalUnlocked++;
                continue;
            }

            // 检查解锁条件是否满足
            if (!checkUnlockCondition(userId, config)) {
                continue;
            }

            // 创建新的装饰记录
            RoomDecoration decoration = new RoomDecoration();
            decoration.setUserId(userId);
            decoration.setDecorationType(type);
            decoration.setDecorationName(config.getDecorationName());
            decoration.setDecorationIcon(config.getDecorationIcon());

            // 默认位置配置
            decoration.setPositionX(50);
            decoration.setPositionY(50);
            decoration.setPositionZ(totalUnlocked);
            decoration.setScale(config.getDefaultScale());
            decoration.setRotation(0);

            decoration.setIsUnlocked(1);
            decoration.setUnlockCondition(config.getUnlockCondition());

            // 首次解锁时，前 MAX_AUTO_ACTIVE 个自动激活并随机放置在画布中
            if (isFirstTime && autoActiveCount < MAX_AUTO_ACTIVE) {
                decoration.setIsActive(1);
                decoration.setPositionX((int) (30 + (autoActiveCount % 2) * 40 + (Math.random() * 10 - 5)));
                decoration.setPositionY((int) (30 + (autoActiveCount / 2) * 30 + (Math.random() * 10 - 5)));
                autoActiveCount++;
            } else {
                // 默认不激活，需要用户手动放置
                decoration.setIsActive(0);
            }

            decoration.setInteractionCount(0);

            decorationMapper.insert(decoration);
            newlyUnlocked.add(config.getDecorationName());
            totalUnlocked++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("newlyUnlocked", newlyUnlocked);
        result.put("totalUnlocked", totalUnlocked);
        result.put("totalAvailable", allConfigs.size());
        return result;
    }

    /**
     * 检查是否满足某个装饰的解锁条件
     */
    private boolean checkUnlockCondition(Long userId, DecorationConfig config) {
        try {
            String requirementJson = config.getUnlockRequirement();
            if (requirementJson == null
                    || requirementJson.trim().isEmpty()
                    || "{}".equals(requirementJson)) {
                // 没有解锁条件，直接解锁
                return true;
            }

            Map<String, Integer> requirements = objectMapper.readValue(
                    requirementJson,
                    new TypeReference<Map<String, Integer>>() {
                    }
            );

            // 以下所有条件都是“至少达到”的语义，只要有一项不达标就不解锁
            Integer required;
            int actual;

            required = requirements.get("diary_count");
            if (required != null) {
                actual = getUserDiaryCount(userId);
                if (actual < required) {
                    return false;
                }
            }

            required = requirements.get("diary_streak");
            if (required != null) {
                actual = getUserDiaryStreak(userId);
                if (actual < required) {
                    return false;
                }
            }

            required = requirements.get("letter_count");
            if (required != null) {
                actual = getUserLetterCount(userId);
                if (actual < required) {
                    return false;
                }
            }

            required = requirements.get("mood_better_count");
            if (required != null) {
                actual = getUserMoodBetterCount(userId);
                if (actual < required) {
                    return false;
                }
            }

            required = requirements.get("overcome_count");
            if (required != null) {
                actual = getUserOvercomeCount(userId);
                if (actual < required) {
                    return false;
                }
            }

            required = requirements.get("high_mood_streak");
            if (required != null) {
                actual = getUserHighMoodStreak(userId);
                if (actual < required) {
                    return false;
                }
            }

            // 预留：灯光开关次数，目前统计逻辑未实现，保守起见按照“未达成”处理
            required = requirements.get("light_toggle_count");
            if (required != null) {
                // TODO: 实现灯光开关次数统计后，再根据 actual 与 required 比较
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to check unlock condition", e);
            // 出现异常时不要误解锁，直接返回不满足
            return false;
        }
    }

    // 统计辅助方法

    private int getUserDiaryCount(Long userId) {
        QueryWrapper<MoodDiary> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return Math.toIntExact(moodDiaryMapper.selectCount(wrapper));
    }

    private int getUserDiaryStreak(Long userId) {
        QueryWrapper<MoodDiary> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .orderByDesc("create_time");
        List<MoodDiary> records = moodDiaryMapper.selectList(wrapper);

        if (records.isEmpty()) {
            return 0;
        }

        int streak = 1;
        LocalDateTime lastDate = records.get(0).getCreateTime().toLocalDate().atStartOfDay();

        for (int i = 1; i < records.size(); i++) {
            LocalDateTime currentDate = records.get(i).getCreateTime().toLocalDate().atStartOfDay();
            long daysDiff = java.time.Duration.between(currentDate, lastDate).toDays();

            if (daysDiff == 1) {
                streak++;
                lastDate = currentDate;
            } else if (daysDiff > 1) {
                break;
            }
        }

        return streak;
    }

    private int getUserLetterCount(Long userId) {
        QueryWrapper<TimeCapsule> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return Math.toIntExact(capsuleMapper.selectCount(wrapper));
    }

    private int getUserMoodBetterCount(Long userId) {
        QueryWrapper<MoodDiary> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("status", "better");
        return Math.toIntExact(moodDiaryMapper.selectCount(wrapper));
    }

    private int getUserOvercomeCount(Long userId) {
        QueryWrapper<MoodDiary> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .in("status", "overcome", "proud");
        return Math.toIntExact(moodDiaryMapper.selectCount(wrapper));
    }

    private int getUserHighMoodStreak(Long userId) {
        QueryWrapper<MoodDiary> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .orderByDesc("create_time")
                .last("LIMIT 10");
        List<MoodDiary> records = moodDiaryMapper.selectList(wrapper);

        int streak = 0;
        for (MoodDiary record : records) {
            if (record.getMoodScore() != null && record.getMoodScore() > 8) {
                streak++;
            } else {
                break;
            }
        }

        return streak;
    }

    /**
     * 添加装饰物到房间
     * 如果已经存在多条同类型记录，会优先复用未激活的记录，其次复用最近的一条
     */
    @Transactional
    public boolean addDecorationToRoom(Long userId, String decorationType, Integer x, Integer y) {
        QueryWrapper<RoomDecoration> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("decoration_type", decorationType)
                .orderByAsc("is_active")
                .orderByDesc("update_time")
                .orderByDesc("create_time");

        List<RoomDecoration> decorations = decorationMapper.selectList(wrapper);
        RoomDecoration decoration = null;

        if (!decorations.isEmpty()) {
            decoration = decorations.stream()
                    .filter(d -> d.getIsActive() == null || d.getIsActive() == 0)
                    .findFirst()
                    .orElse(decorations.get(0));

            if (decorations.size() > 1) {
                log.warn("Found {} duplicate decorations for user {} and type {}",
                        decorations.size() - 1, userId, decorationType);
            }
        }

        boolean isNewRecord = false;
        if (decoration == null) {
            // 如果历史数据缺失，依据配置补建一条装饰记录
            QueryWrapper<DecorationConfig> configWrapper = new QueryWrapper<>();
            configWrapper.eq("decoration_type", decorationType);
            DecorationConfig config = configMapper.selectOne(configWrapper);
            if (config == null) {
                log.warn("Decoration config not found for type {}", decorationType);
                return false;
            }
            decoration = new RoomDecoration();
            decoration.setUserId(userId);
            decoration.setDecorationType(decorationType);
            decoration.setDecorationName(config.getDecorationName());
            decoration.setDecorationIcon(config.getDecorationIcon());
            decoration.setUnlockCondition(config.getUnlockCondition());
            decoration.setScale(config.getDefaultScale());
            decoration.setRotation(0);
            decoration.setInteractionCount(0);
            decoration.setPositionZ(0);
            isNewRecord = true;
        }

        decoration.setIsActive(1);
        decoration.setIsUnlocked(1);
        decoration.setPositionX(x);
        decoration.setPositionY(y);

        boolean success = isNewRecord
                ? decorationMapper.insert(decoration) > 0
                : decorationMapper.updateById(decoration) > 0;

        if (success) {
            log.info("Decoration {} placed at ({}, {}) for user {}",
                    decorationType, x, y, userId);
        }
        return success;
    }

    /**
     * 更新装饰物的位置和状态
     */
    @Transactional
    public boolean updateDecorationPosition(Long userId,
                                            Long decorationId,
                                            Integer x,
                                            Integer y,
                                            Integer z,
                                            BigDecimal scale,
                                            Integer rotation) {
        QueryWrapper<RoomDecoration> wrapper = new QueryWrapper<>();
        wrapper.eq("id", decorationId)
                .eq("user_id", userId);

        RoomDecoration decoration = decorationMapper.selectOne(wrapper);
        if (decoration == null) {
            return false;
        }

        if (x != null) {
            decoration.setPositionX(x);
        }
        if (y != null) {
            decoration.setPositionY(y);
        }
        if (z != null) {
            decoration.setPositionZ(z);
        }
        if (scale != null) {
            decoration.setScale(scale);
        }
        if (rotation != null) {
            decoration.setRotation(rotation);
        }

        return decorationMapper.updateById(decoration) > 0;
    }

    /**
     * 从房间中移除装饰物（逻辑删除，只是取消激活）
     */
    @Transactional
    public boolean removeDecoration(Long userId, Long decorationId) {
        QueryWrapper<RoomDecoration> wrapper = new QueryWrapper<>();
        wrapper.eq("id", decorationId)
                .eq("user_id", userId);

        RoomDecoration decoration = decorationMapper.selectOne(wrapper);
        if (decoration == null) {
            return false;
        }

        decoration.setIsActive(0);
        return decorationMapper.updateById(decoration) > 0;
    }

    /**
     * 与装饰物交互，返回对应互动效果
     */
    @Transactional
    public Map<String, Object> interactWithDecoration(Long userId, Long decorationId) {
        QueryWrapper<RoomDecoration> wrapper = new QueryWrapper<>();
        wrapper.eq("id", decorationId)
                .eq("user_id", userId)
                .eq("is_active", 1);

        RoomDecoration decoration = decorationMapper.selectOne(wrapper);
        if (decoration == null) {
            return Collections.emptyMap();
        }

        // 更新互动次数和时间
        decoration.setInteractionCount(decoration.getInteractionCount() + 1);
        decoration.setLastInteractionTime(LocalDateTime.now());
        decorationMapper.updateById(decoration);

        // 根据配置返回互动效果
        QueryWrapper<DecorationConfig> configWrapper = new QueryWrapper<>();
        configWrapper.eq("decoration_type", decoration.getDecorationType());
        DecorationConfig config = configMapper.selectOne(configWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("decorationId", decorationId);
        result.put("decorationType", decoration.getDecorationType());
        result.put("interactionType", config != null ? config.getInteractionType() : "");
        result.put("effect", config != null ? config.getInteractionEffect() : "");
        result.put("interactionCount", decoration.getInteractionCount());

        return result;
    }
}

