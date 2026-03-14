package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.entity.PatientDoctorRelationship;
import com.mental.health.entity.RelationshipChangeRequest;
import com.mental.health.entity.User;
import com.mental.health.mapper.PatientDoctorRelationshipMapper;
import com.mental.health.mapper.RelationshipChangeRequestMapper;
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
 * 患者分配变更请求服务类
 */
@Service
public class RelationshipChangeRequestService {

    @Autowired
    private RelationshipChangeRequestMapper requestMapper;

    @Autowired
    private PatientDoctorRelationshipService relationshipService;

    @Autowired
    private PatientDoctorRelationshipMapper relationshipMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 提交认领请求
     *
     * @param doctorId 医生ID
     * @param patientId 患者ID
     * @param reason 申请原因
     * @return 创建的请求记录
     */
    @Transactional
    public RelationshipChangeRequest submitClaimRequest(Long doctorId, Long patientId, String reason) {
        // 检查医生患者数量是否超过限制
        if (relationshipService.checkDoctorPatientLimit(doctorId)) {
            throw new RuntimeException("您的患者数量已达上限（10个），无法提交认领申请");
        }

        // 检查患者是否已有医生
        LambdaQueryWrapper<PatientDoctorRelationship> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(PatientDoctorRelationship::getPatientId, patientId)
                .eq(PatientDoctorRelationship::getRelationshipStatus, "active");

        PatientDoctorRelationship existingRelation = relationshipMapper.selectOne(relationWrapper);
        if (existingRelation != null) {
            throw new RuntimeException("该患者已有绑定的医生，无法提交认领申请");
        }

        // 检查是否已有待审核的认领请求
        LambdaQueryWrapper<RelationshipChangeRequest> requestWrapper = new LambdaQueryWrapper<>();
        requestWrapper.eq(RelationshipChangeRequest::getDoctorId, doctorId)
                .eq(RelationshipChangeRequest::getPatientId, patientId)
                .eq(RelationshipChangeRequest::getOperationType, "claim")
                .eq(RelationshipChangeRequest::getRequestStatus, "pending");

        RelationshipChangeRequest existingRequest = requestMapper.selectOne(requestWrapper);
        if (existingRequest != null) {
            throw new RuntimeException("已存在待审核的认领申请，请勿重复提交");
        }

        // 创建认领请求
        RelationshipChangeRequest request = new RelationshipChangeRequest();
        request.setDoctorId(doctorId);
        request.setPatientId(patientId);
        request.setOperationType("claim");
        request.setRequestStatus("pending");
        request.setRequestReason(reason);
        request.setCreateTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());

        requestMapper.insert(request);

        return request;
    }

    /**
     * 提交释放请求
     *
     * @param doctorId 医生ID
     * @param patientId 患者ID
     * @param reason 申请原因
     * @return 创建的请求记录
     */
    @Transactional
    public RelationshipChangeRequest submitReleaseRequest(Long doctorId, Long patientId, String reason) {
        // 检查医患关系是否存在
        LambdaQueryWrapper<PatientDoctorRelationship> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(PatientDoctorRelationship::getPatientId, patientId)
                .eq(PatientDoctorRelationship::getDoctorId, doctorId)
                .eq(PatientDoctorRelationship::getRelationshipStatus, "active");

        PatientDoctorRelationship relationship = relationshipMapper.selectOne(relationWrapper);
        if (relationship == null) {
            throw new RuntimeException("医患关系不存在，无法提交释放申请");
        }

        // 检查是否已有待审核的释放请求
        LambdaQueryWrapper<RelationshipChangeRequest> requestWrapper = new LambdaQueryWrapper<>();
        requestWrapper.eq(RelationshipChangeRequest::getDoctorId, doctorId)
                .eq(RelationshipChangeRequest::getPatientId, patientId)
                .eq(RelationshipChangeRequest::getOperationType, "release")
                .eq(RelationshipChangeRequest::getRequestStatus, "pending");

        RelationshipChangeRequest existingRequest = requestMapper.selectOne(requestWrapper);
        if (existingRequest != null) {
            throw new RuntimeException("已存在待审核的释放申请，请勿重复提交");
        }

        // 创建释放请求
        RelationshipChangeRequest request = new RelationshipChangeRequest();
        request.setDoctorId(doctorId);
        request.setPatientId(patientId);
        request.setOperationType("release");
        request.setRequestStatus("pending");
        request.setRequestReason(reason);
        request.setCreateTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());

        requestMapper.insert(request);

        return request;
    }

    /**
     * 获取待审核请求列表（分页，包含医生和患者信息）
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public Map<String, Object> getPendingRequests(int pageNum, int pageSize) {
        Page<RelationshipChangeRequest> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<RelationshipChangeRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RelationshipChangeRequest::getRequestStatus, "pending")
                .orderByDesc(RelationshipChangeRequest::getCreateTime);

        Page<RelationshipChangeRequest> resultPage = requestMapper.selectPage(page, wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (RelationshipChangeRequest request : resultPage.getRecords()) {
            Map<String, Object> record = convertToMap(request);
            records.add(record);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", resultPage.getTotal());
        result.put("pageNum", resultPage.getCurrent());
        result.put("pageSize", resultPage.getSize());
        result.put("pages", resultPage.getPages());
        result.put("records", records);

        return result;
    }

    /**
     * 获取医生的申请记录
     *
     * @param doctorId 医生ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public Map<String, Object> getDoctorRequests(Long doctorId, int pageNum, int pageSize) {
        Page<RelationshipChangeRequest> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<RelationshipChangeRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RelationshipChangeRequest::getDoctorId, doctorId)
                .orderByDesc(RelationshipChangeRequest::getCreateTime);

        Page<RelationshipChangeRequest> resultPage = requestMapper.selectPage(page, wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (RelationshipChangeRequest request : resultPage.getRecords()) {
            Map<String, Object> record = convertToMap(request);
            records.add(record);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", resultPage.getTotal());
        result.put("pageNum", resultPage.getCurrent());
        result.put("pageSize", resultPage.getSize());
        result.put("pages", resultPage.getPages());
        result.put("records", records);

        return result;
    }

    /**
     * 审批通过
     *
     * @param requestId 请求ID
     * @param adminId 管理员ID
     * @param adminNote 审批备注
     */
    @Transactional
    public void approveRequest(Long requestId, Long adminId, String adminNote) {
        RelationshipChangeRequest request = requestMapper.selectById(requestId);

        if (request == null) {
            throw new RuntimeException("请求不存在");
        }

        if (!"pending".equals(request.getRequestStatus())) {
            throw new RuntimeException("该请求已被处理");
        }

        // 根据操作类型执行相应操作
        if ("claim".equals(request.getOperationType())) {
            // 认领操作：创建医患关系
            relationshipService.createRelationship(request.getPatientId(), request.getDoctorId());
        } else if ("release".equals(request.getOperationType())) {
            // 释放操作：删除医患关系
            relationshipService.removeRelationship(request.getPatientId(), request.getDoctorId());
        }

        // 更新请求状态
        request.setRequestStatus("approved");
        request.setAdminId(adminId);
        request.setAdminNote(adminNote);
        request.setApprovalTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());

        requestMapper.updateById(request);
    }

    /**
     * 审批拒绝
     *
     * @param requestId 请求ID
     * @param adminId 管理员ID
     * @param adminNote 审批备注
     */
    @Transactional
    public void rejectRequest(Long requestId, Long adminId, String adminNote) {
        RelationshipChangeRequest request = requestMapper.selectById(requestId);

        if (request == null) {
            throw new RuntimeException("请求不存在");
        }

        if (!"pending".equals(request.getRequestStatus())) {
            throw new RuntimeException("该请求已被处理");
        }

        // 更新请求状态
        request.setRequestStatus("rejected");
        request.setAdminId(adminId);
        request.setAdminNote(adminNote);
        request.setApprovalTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());

        requestMapper.updateById(request);
    }

    /**
     * 将请求实体转换为包含用户信息的Map
     *
     * @param request 请求实体
     * @return Map
     */
    private Map<String, Object> convertToMap(RelationshipChangeRequest request) {
        Map<String, Object> result = new HashMap<>();

        result.put("id", request.getId());
        result.put("patientId", request.getPatientId());
        result.put("doctorId", request.getDoctorId());
        result.put("operationType", request.getOperationType());
        result.put("requestStatus", request.getRequestStatus());
        result.put("requestReason", request.getRequestReason());
        result.put("adminId", request.getAdminId());
        result.put("adminNote", request.getAdminNote());
        result.put("approvalTime", request.getApprovalTime());
        result.put("createTime", request.getCreateTime());
        result.put("updateTime", request.getUpdateTime());

        // 获取医生信息
        User doctor = userMapper.selectById(request.getDoctorId());
        if (doctor != null) {
            result.put("doctorName", doctor.getNickname());
            result.put("doctorUsername", doctor.getUsername());
            result.put("doctorAvatar", doctor.getAvatar());
            result.put("doctorSpecialization", doctor.getSpecialization());
        }

        // 获取患者信息
        User patient = userMapper.selectById(request.getPatientId());
        if (patient != null) {
            result.put("patientName", patient.getNickname());
            result.put("patientUsername", patient.getUsername());
            result.put("patientAvatar", patient.getAvatar());
            result.put("patientGender", patient.getGender());
            result.put("patientAge", patient.getAge());
        }

        // 获取管理员信息（如果已审批）
        if (request.getAdminId() != null) {
            User admin = userMapper.selectById(request.getAdminId());
            if (admin != null) {
                result.put("adminName", admin.getNickname());
                result.put("adminUsername", admin.getUsername());
            }
        }

        return result;
    }
}
