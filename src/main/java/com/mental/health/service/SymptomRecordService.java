package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.entity.SymptomRecord;
import com.mental.health.mapper.SymptomRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 症状记录服务
 */
@Service
public class SymptomRecordService {

    @Autowired
    private SymptomRecordMapper symptomRecordMapper;

    /**
     * 添加症状记录
     */
    public boolean addRecord(SymptomRecord record) {
        return symptomRecordMapper.insert(record) > 0;
    }

    /**
     * 获取用户症状记录列表（分页）
     */
    public IPage<SymptomRecord> getUserRecords(Long userId, int pageNum, int pageSize) {
        Page<SymptomRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SymptomRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SymptomRecord::getUserId, userId)
                .orderByDesc(SymptomRecord::getCreateTime);
        return symptomRecordMapper.selectPage(page, queryWrapper);
    }

    /**
     * 获取症状记录详情
     */
    public SymptomRecord getRecordById(Long id) {
        return symptomRecordMapper.selectById(id);
    }

    /**
     * 更新症状记录
     */
    public boolean updateRecord(SymptomRecord record) {
        return symptomRecordMapper.updateById(record) > 0;
    }

    /**
     * 删除症状记录
     */
    public boolean deleteRecord(Long id) {
        return symptomRecordMapper.deleteById(id) > 0;
    }

    /**
     * 获取用户最近N条记录
     */
    public List<SymptomRecord> getRecentRecords(Long userId, int limit) {
        LambdaQueryWrapper<SymptomRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SymptomRecord::getUserId, userId)
                .orderByDesc(SymptomRecord::getCreateTime)
                .last("LIMIT " + limit);
        return symptomRecordMapper.selectList(queryWrapper);
    }

    /**
     * 根据症状类型查询记录
     */
    public List<SymptomRecord> getRecordsByType(Long userId, String symptomType) {
        LambdaQueryWrapper<SymptomRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SymptomRecord::getUserId, userId)
                .eq(SymptomRecord::getSymptomType, symptomType)
                .orderByDesc(SymptomRecord::getCreateTime);
        return symptomRecordMapper.selectList(queryWrapper);
    }
}
