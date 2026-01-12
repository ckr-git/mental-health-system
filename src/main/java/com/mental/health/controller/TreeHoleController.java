package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.TreeHole;
import com.mental.health.service.TreeHoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 心情树洞控制器
 */
@RestController
@RequestMapping("/api/patient/tree-hole")
@PreAuthorize("hasRole('PATIENT')")
public class TreeHoleController {

    @Autowired
    private TreeHoleService treeHoleService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 添加倾诉记录
     */
    @PostMapping("/add")
    public Result<TreeHole> addTreeHole(
            @RequestBody TreeHole treeHole,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));

        // 验证必填字段
        if (treeHole.getSpeakToType() == null || treeHole.getSpeakToName() == null ||
            treeHole.getContent() == null || treeHole.getExpireType() == null) {
            return Result.error("请完善倾诉内容");
        }

        TreeHole result = treeHoleService.addTreeHole(treeHole, userId);
        return Result.success("倾诉成功", result);
    }

    /**
     * 获取活跃的倾诉记录（未过期的）
     */
    @GetMapping("/active")
    public Result<List<TreeHole>> getActiveTreeHoles(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        List<TreeHole> treeHoles = treeHoleService.getActiveTreeHoles(userId);
        return Result.success(treeHoles);
    }

    /**
     * 获取档案馆（所有记录）
     */
    @GetMapping("/archive")
    public Result<Map<String, List<TreeHole>>> getArchive(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));

        // 检查是否有权限查看档案馆
        if (!treeHoleService.canViewArchive(userId)) {
            return Result.error("当前情绪状态不满足查看条件（需要心情<3分或>8分）");
        }

        Map<String, List<TreeHole>> archive = treeHoleService.getArchive(userId);
        return Result.success(archive);
    }

    /**
     * 检查是否可以查看档案馆
     */
    @GetMapping("/can-view-archive")
    public Result<Boolean> canViewArchive(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        boolean canView = treeHoleService.canViewArchive(userId);
        return Result.success(canView);
    }

    /**
     * 查看倾诉详情
     */
    @GetMapping("/view/{id}")
    public Result<TreeHole> viewTreeHole(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));

        // 先检查记录是否存在
        TreeHole treeHole = treeHoleService.getTreeHoleById(id, userId);
        if (treeHole == null) {
            return Result.error("记录不存在或已被删除");
        }

        // 检查是否满足查看条件
        String conditionMessage = treeHoleService.checkViewConditionMessage(treeHole, userId);
        if (conditionMessage != null) {
            return Result.error(conditionMessage);
        }

        // 满足条件,增加查看次数并返回
        treeHole = treeHoleService.viewTreeHole(id, userId);
        return Result.success(treeHole);
    }

    /**
     * 删除倾诉记录
     */
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteTreeHole(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        boolean success = treeHoleService.deleteTreeHole(id, userId);

        if (success) {
            return Result.success("删除成功");
        }

        return Result.error("删除失败");
    }

    /**
     * 获取统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Integer>> getStats(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        Map<String, Integer> stats = treeHoleService.getStatsByType(userId);
        return Result.success(stats);
    }
}
