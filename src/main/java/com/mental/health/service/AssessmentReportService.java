package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.entity.AssessmentReport;
import com.mental.health.mapper.AssessmentReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 评估报告服务
 */
@Service
public class AssessmentReportService {

    @Autowired
    private AssessmentReportMapper assessmentReportMapper;

    /**
     * 创建评估报告
     */
    public boolean createReport(AssessmentReport report) {
        report.setStatus(1);
        return assessmentReportMapper.insert(report) > 0;
    }

    /**
     * 获取用户的评估报告列表
     */
    public IPage<AssessmentReport> getUserReports(Long userId, int pageNum, int pageSize) {
        Page<AssessmentReport> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AssessmentReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AssessmentReport::getUserId, userId)
                .orderByDesc(AssessmentReport::getCreateTime);
        return assessmentReportMapper.selectPage(page, queryWrapper);
    }

    /**
     * 获取医生创建的评估报告列表
     */
    public IPage<AssessmentReport> getDoctorReports(Long doctorId, int pageNum, int pageSize) {
        Page<AssessmentReport> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AssessmentReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AssessmentReport::getDoctorId, doctorId)
                .orderByDesc(AssessmentReport::getCreateTime);
        return assessmentReportMapper.selectPage(page, queryWrapper);
    }

    /**
     * 获取评估报告详情
     */
    public AssessmentReport getReportById(Long id) {
        return assessmentReportMapper.selectById(id);
    }

    /**
     * 更新评估报告
     */
    public boolean updateReport(AssessmentReport report) {
        return assessmentReportMapper.updateById(report) > 0;
    }

    /**
     * 删除评估报告
     */
    public boolean deleteReport(Long id) {
        return assessmentReportMapper.deleteById(id) > 0;
    }

    /**
     * 获取用户最近的报告
     */
    public List<AssessmentReport> getRecentReports(Long userId, int limit) {
        LambdaQueryWrapper<AssessmentReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AssessmentReport::getUserId, userId)
                .orderByDesc(AssessmentReport::getCreateTime)
                .last("LIMIT " + limit);
        return assessmentReportMapper.selectList(queryWrapper);
    }

    /**
     * 根据报告类型获取报告
     */
    public List<AssessmentReport> getReportsByType(Long userId, String reportType) {
        LambdaQueryWrapper<AssessmentReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AssessmentReport::getUserId, userId)
                .eq(AssessmentReport::getReportType, reportType)
                .orderByDesc(AssessmentReport::getCreateTime);
        return assessmentReportMapper.selectList(queryWrapper);
    }

    /**
     * 获取AI生成的报告
     */
    public List<AssessmentReport> getAIReports(Long userId) {
        LambdaQueryWrapper<AssessmentReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AssessmentReport::getUserId, userId)
                .eq(AssessmentReport::getIsAiGenerated, 1)
                .orderByDesc(AssessmentReport::getCreateTime);
        return assessmentReportMapper.selectList(queryWrapper);
    }
}
