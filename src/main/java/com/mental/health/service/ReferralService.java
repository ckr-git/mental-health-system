package com.mental.health.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mental.health.dto.CreateNotificationCommand;
import com.mental.health.entity.*;
import com.mental.health.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 转诊与护理团队服务
 *
 * 转诊状态机: INITIATED → ACCEPTED → HANDOFF_READY → HANDOFF_DONE → CLOSED
 *            INITIATED/ACCEPTED → REJECTED
 *            任意非终态 → CANCELLED
 */
@Service
public class ReferralService {

    private static final Logger log = LoggerFactory.getLogger(ReferralService.class);

    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.ofEntries(
            Map.entry("INITIATED", Set.of("ACCEPTED", "REJECTED", "CANCELLED")),
            Map.entry("ACCEPTED", Set.of("HANDOFF_READY", "REJECTED", "CANCELLED")),
            Map.entry("HANDOFF_READY", Set.of("HANDOFF_DONE", "CANCELLED")),
            Map.entry("HANDOFF_DONE", Set.of("CLOSED"))
    );

    @Autowired private ReferralCaseMapper referralCaseMapper;
    @Autowired private ReferralHandoffMapper handoffMapper;
    @Autowired private CareTeamMemberMapper careTeamMapper;
    @Autowired private UserNotificationService notificationService;
    @Autowired private OutboxService outboxService;

    // ===== 转诊 =====

    @Transactional
    public Long initiateReferral(Long patientId, Long fromDoctorId, Long toDoctorId,
                                  String referralType, String urgencyLevel, String reason,
                                  String clinicalSummary, Long treatmentPlanId) {
        ReferralCase rc = new ReferralCase();
        rc.setPatientId(patientId);
        rc.setFromDoctorId(fromDoctorId);
        rc.setToDoctorId(toDoctorId);
        rc.setReferralStatus("INITIATED");
        rc.setReferralType(referralType);
        rc.setUrgencyLevel(urgencyLevel != null ? urgencyLevel : "NORMAL");
        rc.setReason(reason);
        rc.setClinicalSummary(clinicalSummary);
        rc.setTreatmentPlanId(treatmentPlanId);
        rc.setInitiatedAt(LocalDateTime.now());
        rc.setDeadlineAt(LocalDateTime.now().plusDays("EMERGENCY".equals(urgencyLevel) ? 1 : 7));
        referralCaseMapper.insert(rc);

        // 通知目标医生
        if (toDoctorId != null) {
            CreateNotificationCommand cmd = new CreateNotificationCommand();
            cmd.setUserId(toDoctorId);
            cmd.setCategory("TREATMENT");
            cmd.setPriority("EMERGENCY".equals(urgencyLevel) ? "URGENT" : "HIGH");
            cmd.setTitle("新转诊请求：患者#" + patientId);
            cmd.setContent("医生#" + fromDoctorId + "发起" + referralType + "转诊，原因: " + reason);
            cmd.setSourceType("REFERRAL_CASE");
            cmd.setSourceId(rc.getId());
            try { notificationService.createNotification(cmd); } catch (Exception e) { log.error("通知失败: {}", e.getMessage()); }
        }

        log.info("转诊#{} 已发起: 患者#{}, {} → {}", rc.getId(), patientId, fromDoctorId, toDoctorId);
        return rc.getId();
    }

    @Transactional
    public void acceptReferral(Long referralId, Long doctorId) {
        ReferralCase rc = referralCaseMapper.selectById(referralId);
        validateTransition(rc, "ACCEPTED");
        rc.setReferralStatus("ACCEPTED");
        rc.setToDoctorId(doctorId);
        rc.setAcceptedAt(LocalDateTime.now());
        referralCaseMapper.updateById(rc);
        log.info("转诊#{} 已接受", referralId);
    }

    @Transactional
    public void rejectReferral(Long referralId, Long doctorId, String reason) {
        ReferralCase rc = referralCaseMapper.selectById(referralId);
        validateTransition(rc, "REJECTED");
        rc.setReferralStatus("REJECTED");
        rc.setRejectedAt(LocalDateTime.now());
        rc.setRejectReason(reason);
        referralCaseMapper.updateById(rc);
    }

    @Transactional
    public Long createHandoff(Long referralId, Long createdBy, ReferralHandoff handoff) {
        ReferralCase rc = referralCaseMapper.selectById(referralId);
        validateTransition(rc, "HANDOFF_READY");

        handoff.setReferralCaseId(referralId);
        handoff.setCreatedBy(createdBy);
        handoff.setHandoffStatus("READY");
        handoffMapper.insert(handoff);

        rc.setReferralStatus("HANDOFF_READY");
        rc.setHandoffReadyAt(LocalDateTime.now());
        referralCaseMapper.updateById(rc);

        // 通知接收方
        if (rc.getToDoctorId() != null) {
            CreateNotificationCommand cmd = new CreateNotificationCommand();
            cmd.setUserId(rc.getToDoctorId());
            cmd.setCategory("TREATMENT");
            cmd.setPriority("HIGH");
            cmd.setTitle("交接包已就绪：患者#" + rc.getPatientId());
            cmd.setContent("转诊#" + referralId + "的交接包已生成，请查看并确认接收。");
            cmd.setSourceType("REFERRAL_HANDOFF");
            cmd.setSourceId(handoff.getId());
            try { notificationService.createNotification(cmd); } catch (Exception e) { log.error("通知失败: {}", e.getMessage()); }
        }

        return handoff.getId();
    }

    @Transactional
    public void acknowledgeHandoff(Long handoffId, Long doctorId) {
        ReferralHandoff handoff = handoffMapper.selectById(handoffId);
        if (handoff == null) throw new RuntimeException("交接包不存在");

        handoff.setHandoffStatus("ACKNOWLEDGED");
        handoff.setAcknowledgedBy(doctorId);
        handoff.setAcknowledgedAt(LocalDateTime.now());
        handoffMapper.updateById(handoff);

        ReferralCase rc = referralCaseMapper.selectById(handoff.getReferralCaseId());
        if (rc != null) {
            rc.setReferralStatus("HANDOFF_DONE");
            rc.setHandoffDoneAt(LocalDateTime.now());
            referralCaseMapper.updateById(rc);
        }
    }

    @Transactional
    public void completeReferral(Long referralId, Long operatorId) {
        ReferralCase rc = referralCaseMapper.selectById(referralId);
        validateTransition(rc, "CLOSED");
        rc.setReferralStatus("CLOSED");
        rc.setClosedAt(LocalDateTime.now());
        rc.setClosedBy(operatorId);
        referralCaseMapper.updateById(rc);

        // 如果是TRANSFER类型，更新护理团队
        if ("TRANSFER".equals(rc.getReferralType())) {
            addCareTeamMember(rc.getPatientId(), rc.getToDoctorId(), "PRIMARY", operatorId, null);
        }
    }

    // ===== 护理团队 =====

    @Transactional
    public Long addCareTeamMember(Long patientId, Long doctorId, String roleCode,
                                   Long addedBy, String accessScopeJson) {
        // 检查是否已存在
        LambdaQueryWrapper<CareTeamMember> w = new LambdaQueryWrapper<>();
        w.eq(CareTeamMember::getPatientId, patientId)
                .eq(CareTeamMember::getDoctorId, doctorId)
                .eq(CareTeamMember::getMemberRoleCode, roleCode)
                .eq(CareTeamMember::getMemberStatus, "ACTIVE");
        if (careTeamMapper.selectCount(w) > 0) return null;

        CareTeamMember member = new CareTeamMember();
        member.setPatientId(patientId);
        member.setDoctorId(doctorId);
        member.setMemberRoleCode(roleCode);
        member.setMemberStatus("ACTIVE");
        member.setAccessScopeJson(accessScopeJson != null ? accessScopeJson :
                JSON.toJSONString(Map.of("crisis", true, "plan", true, "notes", "PRIMARY".equals(roleCode), "assessment", true)));
        member.setAddedBy(addedBy);
        member.setAddedAt(LocalDateTime.now());
        careTeamMapper.insert(member);
        return member.getId();
    }

    public List<CareTeamMember> getActiveTeamMembers(Long patientId) {
        LambdaQueryWrapper<CareTeamMember> w = new LambdaQueryWrapper<>();
        w.eq(CareTeamMember::getPatientId, patientId)
                .eq(CareTeamMember::getMemberStatus, "ACTIVE")
                .orderByAsc(CareTeamMember::getMemberRoleCode);
        return careTeamMapper.selectList(w);
    }

    @Transactional
    public void removeCareTeamMember(Long memberId, String reason) {
        CareTeamMember member = careTeamMapper.selectById(memberId);
        if (member == null) return;
        member.setMemberStatus("REMOVED");
        member.setRemovedAt(LocalDateTime.now());
        member.setRemovedReason(reason);
        careTeamMapper.updateById(member);
    }

    // ===== 查询 =====

    public List<ReferralCase> getDoctorReferrals(Long doctorId, String role, String status) {
        LambdaQueryWrapper<ReferralCase> w = new LambdaQueryWrapper<>();
        if ("from".equals(role)) w.eq(ReferralCase::getFromDoctorId, doctorId);
        else w.eq(ReferralCase::getToDoctorId, doctorId);
        if (status != null) w.eq(ReferralCase::getReferralStatus, status);
        w.orderByDesc(ReferralCase::getCreateTime);
        return referralCaseMapper.selectList(w);
    }

    public ReferralHandoff getHandoff(Long referralId) {
        LambdaQueryWrapper<ReferralHandoff> w = new LambdaQueryWrapper<>();
        w.eq(ReferralHandoff::getReferralCaseId, referralId)
                .orderByDesc(ReferralHandoff::getCreateTime)
                .last("LIMIT 1");
        return handoffMapper.selectOne(w);
    }

    private void validateTransition(ReferralCase rc, String target) {
        if (rc == null) throw new RuntimeException("转诊不存在");
        Set<String> valid = VALID_TRANSITIONS.getOrDefault(rc.getReferralStatus(), Set.of());
        if (!valid.contains(target)) {
            throw new RuntimeException("无效转换: " + rc.getReferralStatus() + " → " + target);
        }
    }
}
