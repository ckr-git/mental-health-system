package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mental.health.dto.ProfileAggregate;
import com.mental.health.dto.UpdateProfileCommand;
import com.mental.health.entity.PatientProfile;
import com.mental.health.entity.User;
import com.mental.health.mapper.PatientProfileMapper;
import com.mental.health.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PatientProfileService {

    private static final Logger log = LoggerFactory.getLogger(PatientProfileService.class);
    private static final String CACHE_KEY_PREFIX = "profile:aggregate:";
    private static final long CACHE_TTL_MINUTES = 30;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PatientProfileMapper patientProfileMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private OutboxService outboxService;

    public ProfileAggregate getAggregateProfile(Long userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;

        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached instanceof ProfileAggregate) {
                    return (ProfileAggregate) cached;
                }
            } catch (Exception e) {
                log.warn("Redis cache read failed for key {}: {}", cacheKey, e.getMessage());
            }
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        PatientProfile profile = patientProfileMapper.selectOne(
                new LambdaQueryWrapper<PatientProfile>()
                        .eq(PatientProfile::getUserId, userId)
                        .eq(PatientProfile::getDeleted, 0));

        ProfileAggregate aggregate = buildAggregate(user, profile);

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, aggregate, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("Redis cache write failed for key {}: {}", cacheKey, e.getMessage());
            }
        }

        return aggregate;
    }

    @Transactional
    public ProfileAggregate updateAggregateProfile(Long userId, UpdateProfileCommand cmd) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 更新User基础信息
        if (cmd.getNickname() != null) user.setNickname(cmd.getNickname());
        if (cmd.getEmail() != null) user.setEmail(cmd.getEmail());
        if (cmd.getPhone() != null) user.setPhone(cmd.getPhone());
        if (cmd.getGender() != null) user.setGender(cmd.getGender());
        if (cmd.getAge() != null) user.setAge(cmd.getAge());
        user.setPassword(null);
        userMapper.updateById(user);

        // Upsert PatientProfile
        PatientProfile profile = patientProfileMapper.selectOne(
                new LambdaQueryWrapper<PatientProfile>()
                        .eq(PatientProfile::getUserId, userId)
                        .eq(PatientProfile::getDeleted, 0));

        boolean isNew = (profile == null);
        if (isNew) {
            profile = new PatientProfile();
            profile.setUserId(userId);
            profile.setProfileVersion(1);
        }

        if (cmd.getRealName() != null) profile.setRealName(cmd.getRealName());
        if (cmd.getBirthDate() != null) profile.setBirthDate(cmd.getBirthDate());
        if (cmd.getMaritalStatus() != null) profile.setMaritalStatus(cmd.getMaritalStatus());
        if (cmd.getOccupation() != null) profile.setOccupation(cmd.getOccupation());
        if (cmd.getEmergencyContactName() != null) profile.setEmergencyContactName(cmd.getEmergencyContactName());
        if (cmd.getEmergencyContactPhone() != null) profile.setEmergencyContactPhone(cmd.getEmergencyContactPhone());
        if (cmd.getEmergencyContactRelation() != null) profile.setEmergencyContactRelation(cmd.getEmergencyContactRelation());
        if (cmd.getIntroduction() != null) profile.setIntroduction(cmd.getIntroduction());
        if (cmd.getMedicalHistory() != null) profile.setMedicalHistory(cmd.getMedicalHistory());
        if (cmd.getAllergyHistory() != null) profile.setAllergyHistory(cmd.getAllergyHistory());
        if (cmd.getFamilyHistory() != null) profile.setFamilyHistory(cmd.getFamilyHistory());
        if (cmd.getConsentFlags() != null) profile.setConsentFlags(cmd.getConsentFlags());
        if (cmd.getIntakeTags() != null) profile.setIntakeTags(cmd.getIntakeTags());

        if (isNew) {
            patientProfileMapper.insert(profile);
        } else {
            profile.setProfileVersion(profile.getProfileVersion() + 1);
            patientProfileMapper.updateById(profile);
        }

        // 发布事件
        String eventKey = "PROFILE_UPDATED:" + userId + ":" + UUID.randomUUID();
        outboxService.append("PATIENT_PROFILE", userId, "PATIENT_PROFILE_UPDATED",
                eventKey, "{\"userId\":" + userId + ",\"version\":" + profile.getProfileVersion() + "}");

        // 清除缓存
        clearCache(userId);

        return getAggregateProfile(userId);
    }

    public void clearCache(Long userId) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(CACHE_KEY_PREFIX + userId);
            } catch (Exception e) {
                log.warn("Redis cache delete failed for userId {}: {}", userId, e.getMessage());
            }
        }
    }

    private ProfileAggregate buildAggregate(User user, PatientProfile profile) {
        ProfileAggregate aggregate = new ProfileAggregate();
        aggregate.setUserId(user.getId());
        aggregate.setUsername(user.getUsername());
        aggregate.setNickname(user.getNickname());
        aggregate.setAvatar(user.getAvatar());
        aggregate.setPhone(user.getPhone());
        aggregate.setEmail(user.getEmail());
        aggregate.setGender(user.getGender());
        aggregate.setAge(user.getAge());
        aggregate.setRole(user.getRole());
        aggregate.setSpecialization(user.getSpecialization());
        aggregate.setCreateTime(user.getCreateTime());
        aggregate.setUpdateTime(user.getUpdateTime());

        if (profile != null) {
            aggregate.setProfileId(profile.getId());
            aggregate.setRealName(profile.getRealName());
            aggregate.setBirthDate(profile.getBirthDate());
            aggregate.setMaritalStatus(profile.getMaritalStatus());
            aggregate.setOccupation(profile.getOccupation());
            aggregate.setEmergencyContactName(profile.getEmergencyContactName());
            aggregate.setEmergencyContactPhone(profile.getEmergencyContactPhone());
            aggregate.setEmergencyContactRelation(profile.getEmergencyContactRelation());
            aggregate.setIntroduction(profile.getIntroduction());
            aggregate.setMedicalHistory(profile.getMedicalHistory());
            aggregate.setAllergyHistory(profile.getAllergyHistory());
            aggregate.setFamilyHistory(profile.getFamilyHistory());
            aggregate.setConsentFlags(profile.getConsentFlags());
            aggregate.setIntakeTags(profile.getIntakeTags());
            aggregate.setProfileVersion(profile.getProfileVersion());
        }

        return aggregate;
    }
}
