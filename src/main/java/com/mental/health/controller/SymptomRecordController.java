package com.mental.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.SymptomRecord;
import com.mental.health.service.SymptomRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 症状记录控制器
 */
@RestController
@RequestMapping("/api/patient/symptom")
public class SymptomRecordController {

    @Autowired
    private SymptomRecordService symptomRecordService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 添加症状记录
     */
    @PostMapping("/add")
    public Result<String> addRecord(@RequestBody SymptomRecord record, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        record.setUserId(userId);
        
        boolean success = symptomRecordService.addRecord(record);
        if (success) {
            return Result.success("记录添加成功");
        } else {
            return Result.error("记录添加失败");
        }
    }

    /**
     * 获取我的症状记录列表
     */
    @GetMapping("/list")
    public Result<IPage<SymptomRecord>> getMyRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        IPage<SymptomRecord> records = symptomRecordService.getUserRecords(userId, pageNum, pageSize);
        return Result.success(records);
    }

    /**
     * 获取症状记录详情
     */
    @GetMapping("/{id}")
    public Result<SymptomRecord> getRecordDetail(@PathVariable Long id) {
        SymptomRecord record = symptomRecordService.getRecordById(id);
        if (record != null) {
            return Result.success(record);
        } else {
            return Result.error("记录不存在");
        }
    }

    /**
     * 更新症状记录
     */
    @PutMapping("/update")
    public Result<String> updateRecord(@RequestBody SymptomRecord record) {
        boolean success = symptomRecordService.updateRecord(record);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * 删除症状记录
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteRecord(@PathVariable Long id) {
        boolean success = symptomRecordService.deleteRecord(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }

    /**
     * 获取最近的记录
     */
    @GetMapping("/recent")
    public Result<List<SymptomRecord>> getRecentRecords(
            @RequestParam(defaultValue = "7") int limit,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        List<SymptomRecord> records = symptomRecordService.getRecentRecords(userId, limit);
        return Result.success(records);
    }
}
