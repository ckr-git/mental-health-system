package com.mental.health.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.CrisisAlert;
import com.mental.health.mapper.CrisisAlertMapper;
import com.mental.health.service.RiskAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/doctor/crisis-alerts")
@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
public class CrisisAlertController {

    @Autowired
    private CrisisAlertMapper crisisAlertMapper;

    @Autowired
    private RiskAlertService riskAlertService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public Result<IPage<CrisisAlert>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestHeader("Authorization") String token) {
        Page<CrisisAlert> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CrisisAlert> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            w.eq(CrisisAlert::getAlertStatus, status);
        }
        if (StringUtils.hasText(level)) {
            w.eq(CrisisAlert::getAlertLevel, level);
        }
        w.orderByDesc(CrisisAlert::getCreateTime);
        return Result.success(crisisAlertMapper.selectPage(page, w));
    }

    @GetMapping("/{id}")
    public Result<CrisisAlert> detail(@PathVariable Long id) {
        CrisisAlert alert = crisisAlertMapper.selectById(id);
        if (alert == null) return Result.error("告警不存在");
        return Result.success(alert);
    }

    @PostMapping("/{id}/acknowledge")
    public Result<Void> acknowledge(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> body,
                                     @RequestHeader("Authorization") String token) {
        Long doctorId = jwtUtil.getUserIdFromToken(token.substring(7));
        String note = body != null ? body.get("note") : null;
        riskAlertService.acknowledge(id, doctorId, note);
        return Result.success(null);
    }

    @PostMapping("/{id}/resolve")
    public Result<Void> resolve(@PathVariable Long id,
                                 @RequestBody(required = false) Map<String, String> body,
                                 @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        String note = body != null ? body.get("note") : null;
        riskAlertService.resolve(id, userId, "DOCTOR", note);
        return Result.success(null);
    }
}
