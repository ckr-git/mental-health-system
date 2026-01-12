package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mental.health.entity.PatientDoctorRelationship;
import com.mental.health.entity.User;
import com.mental.health.mapper.PatientDoctorRelationshipMapper;
import com.mental.health.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 医患关系服务类
 */
@Service
public class PatientDoctorRelationshipService {

    @Autowired
    private PatientDoctorRelationshipMapper relationshipMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 检查医生患者数量是否超过10个
     *
     * @param doctorId 医生ID
     * @return true-超过限制, false-未超过限制
     */
    public boolean checkDoctorPatientLimit(Long doctorId) {
        LambdaQueryWrapper<PatientDoctorRelationship> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PatientDoctorRelationship::getDoctorId, doctorId)
                .eq(PatientDoctorRelationship::getRelationshipStatus, "active");

        Long count = relationshipMapper.selectCount(wrapper);
        return count >= 10;
    }

    /**
     * 获取患者的医生
     *
     * @param patientId 患者ID
     * @return 医生信息（包含用户信息），如果不存在返回null
     */
    public Map<String, Object> getPatientDoctor(Long patientId) {
        LambdaQueryWrapper<PatientDoctorRelationship> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PatientDoctorRelationship::getPatientId, patientId)
                .eq(PatientDoctorRelationship::getRelationshipStatus, "active");

        PatientDoctorRelationship relationship = relationshipMapper.selectOne(wrapper);

        if (relationship == null) {
            return null;
        }

        // 获取医生信息
        User doctor = userMapper.selectById(relationship.getDoctorId());

        Map<String, Object> result = new HashMap<>();
        result.put("id", relationship.getId());
        result.put("patientId", relationship.getPatientId());
        result.put("doctorId", relationship.getDoctorId());
        result.put("relationshipStatus", relationship.getRelationshipStatus());
        result.put("createTime", relationship.getCreateTime());
        result.put("updateTime", relationship.getUpdateTime());

        // 添加医生用户信息
        if (doctor != null) {
            result.put("doctorName", doctor.getNickname());
            result.put("doctorUsername", doctor.getUsername());
            result.put("doctorAvatar", doctor.getAvatar());
            result.put("doctorSpecialization", doctor.getSpecialization());
            result.put("doctorPhone", doctor.getPhone());
            result.put("doctorEmail", doctor.getEmail());
        }

        return result;
    }

    /**
     * 获取医生的所有患者列表（包含用户信息）
     *
     * @param doctorId 医生ID
     * @return 患者列表
     */
    public List<Map<String, Object>> getDoctorPatients(Long doctorId) {
        LambdaQueryWrapper<PatientDoctorRelationship> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PatientDoctorRelationship::getDoctorId, doctorId)
                .eq(PatientDoctorRelationship::getRelationshipStatus, "active")
                .orderByDesc(PatientDoctorRelationship::getCreateTime);

        List<PatientDoctorRelationship> relationships = relationshipMapper.selectList(wrapper);
        List<Map<String, Object>> results = new ArrayList<>();

        for (PatientDoctorRelationship relationship : relationships) {
            // 获取患者信息
            User patient = userMapper.selectById(relationship.getPatientId());

            Map<String, Object> result = new HashMap<>();
            result.put("id", relationship.getId());
            result.put("patientId", relationship.getPatientId());
            result.put("doctorId", relationship.getDoctorId());
            result.put("relationshipStatus", relationship.getRelationshipStatus());
            result.put("createTime", relationship.getCreateTime());
            result.put("updateTime", relationship.getUpdateTime());

            // 添加患者用户信息
            if (patient != null) {
                result.put("patientName", patient.getNickname());
                result.put("patientUsername", patient.getUsername());
                result.put("patientAvatar", patient.getAvatar());
                result.put("patientPhone", patient.getPhone());
                result.put("patientEmail", patient.getEmail());
                result.put("patientGender", patient.getGender());
                result.put("patientAge", patient.getAge());
            }

            results.add(result);
        }

        return results;
    }

    /**
     * 检查医生是否有权限访问该患者
     *
     * @param doctorId 医生ID
     * @param patientId 患者ID
     * @return true-有权限, false-无权限
     */
    public boolean hasRelationship(Long doctorId, Long patientId) {
        LambdaQueryWrapper<PatientDoctorRelationship> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PatientDoctorRelationship::getDoctorId, doctorId)
                .eq(PatientDoctorRelationship::getPatientId, patientId)
                .eq(PatientDoctorRelationship::getRelationshipStatus, "active");

        return relationshipMapper.selectCount(wrapper) > 0;
    }

    /**
     * 获取医生当前患者数量
     *
     * @param doctorId 医生ID
     * @return 患者数量
     */
    public Long getDoctorPatientCount(Long doctorId) {
        LambdaQueryWrapper<PatientDoctorRelationship> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PatientDoctorRelationship::getDoctorId, doctorId)
                .eq(PatientDoctorRelationship::getRelationshipStatus, "active");

        return relationshipMapper.selectCount(wrapper);
    }

    /**
     * 创建医患关系（系统使用，不经过审批）
     *
     * @param patientId 患者ID
     * @param doctorId 医生ID
     * @return 创建的关系记录
     */
    @Transactional
    public PatientDoctorRelationship createRelationship(Long patientId, Long doctorId) {
        // 检查是否已存在激活的关系
        LambdaQueryWrapper<PatientDoctorRelationship> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(PatientDoctorRelationship::getPatientId, patientId)
                .eq(PatientDoctorRelationship::getRelationshipStatus, "active");

        PatientDoctorRelationship existingRelation = relationshipMapper.selectOne(checkWrapper);
        if (existingRelation != null) {
            throw new RuntimeException("该患者已有绑定的医生");
        }

        // 检查医生患者数量限制
        if (checkDoctorPatientLimit(doctorId)) {
            throw new RuntimeException("医生患者数量已达上限（10个）");
        }

        // 创建新关系
        PatientDoctorRelationship relationship = new PatientDoctorRelationship();
        relationship.setPatientId(patientId);
        relationship.setDoctorId(doctorId);
        relationship.setRelationshipStatus("active");
        relationship.setCreateTime(LocalDateTime.now());
        relationship.setUpdateTime(LocalDateTime.now());

        relationshipMapper.insert(relationship);

        return relationship;
    }

    /**
     * 解除医患关系（系统使用）
     *
     * @param patientId 患者ID
     * @param doctorId 医生ID
     */
    @Transactional
    public void removeRelationship(Long patientId, Long doctorId) {
        LambdaQueryWrapper<PatientDoctorRelationship> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PatientDoctorRelationship::getPatientId, patientId)
                .eq(PatientDoctorRelationship::getDoctorId, doctorId)
                .eq(PatientDoctorRelationship::getRelationshipStatus, "active");

        PatientDoctorRelationship relationship = relationshipMapper.selectOne(wrapper);

        if (relationship == null) {
            throw new RuntimeException("医患关系不存在");
        }

        // 将关系状态改为inactive
        relationship.setRelationshipStatus("inactive");
        relationship.setUpdateTime(LocalDateTime.now());

        relationshipMapper.updateById(relationship);
    }

    /**
     * 获取患者公海列表（未分配医生的患者）
     *
     * @return 未分配患者列表
     */
    public List<Map<String, Object>> getUnassignedPatients() {
        // 获取所有患者角色的用户
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getRole, "PATIENT")
                .eq(User::getStatus, 1)  // 只获取已激活的患者
                .orderByDesc(User::getCreateTime);

        List<User> allPatients = userMapper.selectList(userWrapper);
        List<Map<String, Object>> unassignedPatients = new ArrayList<>();

        for (User patient : allPatients) {
            // 检查该患者是否已有活跃的医患关系
            LambdaQueryWrapper<PatientDoctorRelationship> relationWrapper = new LambdaQueryWrapper<>();
            relationWrapper.eq(PatientDoctorRelationship::getPatientId, patient.getId())
                    .eq(PatientDoctorRelationship::getRelationshipStatus, "active");

            Long count = relationshipMapper.selectCount(relationWrapper);

            // 如果没有活跃关系，则加入公海
            if (count == 0) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", patient.getId());
                result.put("username", patient.getUsername());
                result.put("nickname", patient.getNickname());
                result.put("avatar", patient.getAvatar());
                result.put("gender", patient.getGender());
                result.put("age", patient.getAge());
                result.put("phone", patient.getPhone());
                result.put("email", patient.getEmail());
                result.put("createTime", patient.getCreateTime());

                unassignedPatients.add(result);
            }
        }

        return unassignedPatients;
    }
}
