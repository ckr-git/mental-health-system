package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.RiskRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RiskRuleMapper extends BaseMapper<RiskRule> {

    @Select("SELECT * FROM risk_rule WHERE active = 1 AND deleted = 0 AND source_type = #{sourceType}")
    List<RiskRule> findActiveBySourceType(@Param("sourceType") String sourceType);

    @Select("SELECT * FROM risk_rule WHERE active = 1 AND deleted = 0")
    List<RiskRule> findAllActive();
}
