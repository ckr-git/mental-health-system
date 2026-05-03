package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.PatientDailyMetric;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PatientDailyMetricMapper extends BaseMapper<PatientDailyMetric> {
}
