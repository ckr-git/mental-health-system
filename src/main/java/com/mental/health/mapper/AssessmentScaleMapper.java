package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.AssessmentScale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AssessmentScaleMapper extends BaseMapper<AssessmentScale> {

    @Select("SELECT * FROM assessment_scale WHERE active = 1 AND deleted = 0 ORDER BY scale_code, version DESC")
    List<AssessmentScale> findActiveScales();

    @Select("SELECT * FROM assessment_scale WHERE scale_code = #{scaleCode} AND active = 1 AND deleted = 0 ORDER BY version DESC LIMIT 1")
    AssessmentScale findLatestByScaleCode(@Param("scaleCode") String scaleCode);
}
