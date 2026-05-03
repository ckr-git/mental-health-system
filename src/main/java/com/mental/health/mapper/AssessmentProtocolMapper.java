package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.AssessmentProtocol;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AssessmentProtocolMapper extends BaseMapper<AssessmentProtocol> {

    @Select("SELECT * FROM assessment_protocol WHERE trigger_condition = #{trigger} AND active = 1 AND deleted = 0")
    List<AssessmentProtocol> findByTrigger(@Param("trigger") String triggerCondition);

    @Select("SELECT * FROM assessment_protocol WHERE protocol_code = #{code} AND deleted = 0 LIMIT 1")
    AssessmentProtocol findByCode(@Param("code") String protocolCode);
}
