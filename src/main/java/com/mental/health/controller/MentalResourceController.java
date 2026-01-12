package com.mental.health.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.common.Result;
import com.mental.health.entity.MentalResource;
import com.mental.health.mapper.MentalResourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 心理资源控制器
 */
@RestController
@RequestMapping("/api")
public class MentalResourceController {

    @Autowired
    private MentalResourceMapper resourceMapper;

    // ==================== 公开接口 ====================

    @GetMapping("/public/resources")
    public Result<IPage<MentalResource>> getResourceList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {

        Page<MentalResource> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MentalResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MentalResource::getStatus, 1);

        if (StringUtils.hasText(type)) {
            wrapper.eq(MentalResource::getType, type);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(MentalResource::getCategory, category);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MentalResource::getTitle, keyword)
                    .or().like(MentalResource::getAuthor, keyword));
        }
        wrapper.orderByDesc(MentalResource::getCreateTime);

        return Result.success(resourceMapper.selectPage(page, wrapper));
    }

    @GetMapping("/public/resources/{id}")
    public Result<MentalResource> getResourceDetail(@PathVariable Long id) {
        MentalResource resource = resourceMapper.selectById(id);
        if (resource != null) {
            resource.setViewCount(resource.getViewCount() + 1);
            resourceMapper.updateById(resource);
            return Result.success(resource);
        }
        return Result.error("资源不存在");
    }

    @GetMapping("/public/resources/hot")
    public Result<List<MentalResource>> getHotResources(@RequestParam(defaultValue = "10") int limit) {
        LambdaQueryWrapper<MentalResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MentalResource::getStatus, 1)
                .orderByDesc(MentalResource::getViewCount)
                .last("LIMIT " + limit);
        return Result.success(resourceMapper.selectList(wrapper));
    }

    // ==================== 管理员接口 ====================

    @GetMapping("/admin/resources/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        LambdaQueryWrapper<MentalResource> wrapper = new LambdaQueryWrapper<>();
        stats.put("totalResources", resourceMapper.selectCount(wrapper));

        wrapper.clear();
        wrapper.select(MentalResource::getViewCount);
        List<MentalResource> all = resourceMapper.selectList(null);

        long totalViews = all.stream().mapToLong(r -> r.getViewCount() != null ? r.getViewCount() : 0).sum();
        long totalLikes = all.stream().mapToLong(r -> r.getLikeCount() != null ? r.getLikeCount() : 0).sum();
        long totalDownloads = all.stream().mapToLong(r -> r.getDownloadCount() != null ? r.getDownloadCount() : 0).sum();

        stats.put("totalViews", totalViews);
        stats.put("totalLikes", totalLikes);
        stats.put("totalDownloads", totalDownloads);

        return Result.success(stats);
    }

    @GetMapping("/admin/resources")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<MentalResource>> getAdminResourceList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

        Page<MentalResource> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MentalResource> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(type)) {
            wrapper.eq(MentalResource::getType, type);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(MentalResource::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(MentalResource::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MentalResource::getTitle, keyword)
                    .or().like(MentalResource::getAuthor, keyword));
        }
        wrapper.orderByDesc(MentalResource::getCreateTime);

        return Result.success(resourceMapper.selectPage(page, wrapper));
    }

    @PostMapping("/admin/resources")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> createResource(@RequestBody MentalResource resource) {
        resource.setViewCount(0);
        resource.setLikeCount(0);
        resource.setDownloadCount(0);
        resource.setStatus(0);
        int rows = resourceMapper.insert(resource);
        return rows > 0 ? Result.success("创建成功") : Result.error("创建失败");
    }

    @PutMapping("/admin/resources/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateResource(@PathVariable Long id, @RequestBody MentalResource resource) {
        resource.setId(id);
        int rows = resourceMapper.updateById(resource);
        return rows > 0 ? Result.success("更新成功") : Result.error("更新失败");
    }

    @PutMapping("/admin/resources/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        MentalResource resource = resourceMapper.selectById(id);
        if (resource == null) {
            return Result.error("资源不存在");
        }
        resource.setStatus(request.get("status"));
        int rows = resourceMapper.updateById(resource);
        return rows > 0 ? Result.success("操作成功") : Result.error("操作失败");
    }

    @DeleteMapping("/admin/resources/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteResource(@PathVariable Long id) {
        int rows = resourceMapper.deleteById(id);
        return rows > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    // ==================== 患者接口 ====================

    @PostMapping("/patient/resources/{id}/like")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<String> likeResource(@PathVariable Long id) {
        MentalResource resource = resourceMapper.selectById(id);
        if (resource == null) {
            return Result.error("资源不存在");
        }
        resource.setLikeCount(resource.getLikeCount() + 1);
        resourceMapper.updateById(resource);
        return Result.success("点赞成功");
    }

    @PostMapping("/patient/resources/{id}/download")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public Result<String> downloadResource(@PathVariable Long id) {
        MentalResource resource = resourceMapper.selectById(id);
        if (resource == null) {
            return Result.error("资源不存在");
        }
        resource.setDownloadCount(resource.getDownloadCount() + 1);
        resourceMapper.updateById(resource);
        return Result.success("下载成功");
    }
}
