package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.entity.ConsultationSession;
import com.mental.health.entity.User;
import com.mental.health.mapper.ConsultationSessionMapper;
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
 * 在线咨询会话服务类
 */
@Service
public class ConsultationService {

    @Autowired
    private ConsultationSessionMapper sessionMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取或创建会话
     * 如果存在进行中的会话则返回，否则创建新会话
     *
     * @param patientId 患者ID
     * @param doctorId 医生ID
     * @return 会话信息
     */
    @Transactional
    public Map<String, Object> getOrCreateSession(Long patientId, Long doctorId) {
        // 查找是否存在进行中的会话
        LambdaQueryWrapper<ConsultationSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsultationSession::getPatientId, patientId)
                .eq(ConsultationSession::getDoctorId, doctorId)
                .eq(ConsultationSession::getSessionStatus, "ongoing")
                .orderByDesc(ConsultationSession::getCreateTime)
                .last("LIMIT 1");

        ConsultationSession session = sessionMapper.selectOne(wrapper);

        // 如果不存在进行中的会话，创建新会话
        if (session == null) {
            session = new ConsultationSession();
            session.setPatientId(patientId);
            session.setDoctorId(doctorId);
            session.setSessionStatus("ongoing");
            session.setStartTime(LocalDateTime.now());
            session.setCreateTime(LocalDateTime.now());
            session.setUpdateTime(LocalDateTime.now());

            sessionMapper.insert(session);
        }

        return convertToMap(session);
    }

    /**
     * 获取医生的会话列表（包含患者信息）
     *
     * @param doctorId 医生ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public Map<String, Object> getDoctorSessions(Long doctorId, int pageNum, int pageSize) {
        Page<ConsultationSession> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<ConsultationSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsultationSession::getDoctorId, doctorId)
                .orderByDesc(ConsultationSession::getCreateTime);

        Page<ConsultationSession> resultPage = sessionMapper.selectPage(page, wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (ConsultationSession session : resultPage.getRecords()) {
            Map<String, Object> record = convertToMap(session);
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
     * 获取患者当前会话
     *
     * @param patientId 患者ID
     * @return 会话信息，如果不存在返回null
     */
    public Map<String, Object> getPatientSession(Long patientId) {
        LambdaQueryWrapper<ConsultationSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsultationSession::getPatientId, patientId)
                .eq(ConsultationSession::getSessionStatus, "ongoing")
                .orderByDesc(ConsultationSession::getCreateTime)
                .last("LIMIT 1");

        ConsultationSession session = sessionMapper.selectOne(wrapper);

        if (session == null) {
            return null;
        }

        return convertToMap(session);
    }

    /**
     * 关闭会话
     *
     * @param sessionId 会话ID
     */
    @Transactional
    public void closeSession(Long sessionId) {
        ConsultationSession session = sessionMapper.selectById(sessionId);

        if (session == null) {
            throw new RuntimeException("会话不存在");
        }

        if ("closed".equals(session.getSessionStatus())) {
            throw new RuntimeException("会话已关闭");
        }

        session.setSessionStatus("closed");
        session.setEndTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());

        sessionMapper.updateById(session);
    }

    /**
     * 将会话实体转换为包含用户信息的Map
     *
     * @param session 会话实体
     * @return Map
     */
    private Map<String, Object> convertToMap(ConsultationSession session) {
        Map<String, Object> result = new HashMap<>();

        result.put("id", session.getId());
        result.put("patientId", session.getPatientId());
        result.put("doctorId", session.getDoctorId());
        result.put("sessionStatus", session.getSessionStatus());
        result.put("startTime", session.getStartTime());
        result.put("endTime", session.getEndTime());
        result.put("createTime", session.getCreateTime());
        result.put("updateTime", session.getUpdateTime());

        // 获取患者信息
        User patient = userMapper.selectById(session.getPatientId());
        if (patient != null) {
            result.put("patientName", patient.getNickname());
            result.put("patientUsername", patient.getUsername());
            result.put("patientAvatar", patient.getAvatar());
            result.put("patientGender", patient.getGender());
            result.put("patientAge", patient.getAge());
        }

        // 获取医生信息
        User doctor = userMapper.selectById(session.getDoctorId());
        if (doctor != null) {
            result.put("doctorName", doctor.getNickname());
            result.put("doctorUsername", doctor.getUsername());
            result.put("doctorAvatar", doctor.getAvatar());
            result.put("doctorSpecialization", doctor.getSpecialization());
        }

        return result;
    }

    /**
     * 获取患者咨询历史记录（分页）
     *
     * @param patientId 患者ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public Map<String, Object> getPatientConsultationHistory(Long patientId, int pageNum, int pageSize) {
        Page<ConsultationSession> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<ConsultationSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsultationSession::getPatientId, patientId)
                .orderByDesc(ConsultationSession::getCreateTime);

        Page<ConsultationSession> resultPage = sessionMapper.selectPage(page, wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (ConsultationSession session : resultPage.getRecords()) {
            Map<String, Object> record = convertToMap(session);
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
     * 患者发起咨询会话
     *
     * @param patientId 患者ID
     * @param doctorId 医生ID
     * @param initialMessage 初始消息
     * @return 会话信息
     */
    @Transactional
    public Map<String, Object> startConsultation(Long patientId, Long doctorId, String initialMessage) {
        // 检查是否已有进行中的会话
        LambdaQueryWrapper<ConsultationSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsultationSession::getPatientId, patientId)
                .eq(ConsultationSession::getSessionStatus, "ongoing");

        ConsultationSession existingSession = sessionMapper.selectOne(wrapper);
        if (existingSession != null) {
            return convertToMap(existingSession);
        }

        // 创建新会话
        ConsultationSession session = new ConsultationSession();
        session.setPatientId(patientId);
        session.setDoctorId(doctorId);
        session.setSessionStatus("ongoing");
        session.setStartTime(LocalDateTime.now());
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());

        sessionMapper.insert(session);

        return convertToMap(session);
    }
}
