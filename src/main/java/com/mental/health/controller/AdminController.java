package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.service.RelationshipChangeRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员控制器
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RelationshipChangeRequestService relationshipChangeRequestService;

    // ==================== 患者分配审核相关 ====================

    /**
     * 获取待审核的患者分配请求列表
     */
    @GetMapping("/patient-assignments/pending")
    public Result<Map<String, Object>> getPendingAssignments(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String requestType) {
        Long adminId = jwtUtil.getUserIdFromToken(token.substring(7));
        Map<String, Object> result = relationshipChangeRequestService.getPendingRequests(pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 审批通过
     */
    @PostMapping("/patient-assignments/{requestId}/approve")
    public Result<String> approveAssignment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long requestId,
            @RequestBody Map<String, String> request) {
        Long adminId = jwtUtil.getUserIdFromToken(token.substring(7));
        String adminNote = request.get("adminNote");

        try {
            relationshipChangeRequestService.approveRequest(requestId, adminId, adminNote);
            return Result.success("审批通过");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审批拒绝
     */
    @PostMapping("/patient-assignments/{requestId}/reject")
    public Result<String> rejectAssignment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long requestId,
            @RequestBody Map<String, String> request) {
        Long adminId = jwtUtil.getUserIdFromToken(token.substring(7));
        String adminNote = request.get("adminNote");

        if (adminNote == null || adminNote.trim().isEmpty()) {
            return Result.error("请填写拒绝理由");
        }

        try {
            relationshipChangeRequestService.rejectRequest(requestId, adminId, adminNote);
            return Result.success("已拒绝该申请");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
