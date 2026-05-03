package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.AssessmentBaseline;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AssessmentBaselineMapper extends BaseMapper<AssessmentBaseline> {

    @Select("SELECT * FROM assessment_baseline WHERE patient_id = #{patientId} " +
            "AND scale_code = #{scaleCode} AND superseded_at IS NULL AND deleted = 0 LIMIT 1")
    AssessmentBaseline findActiveBaseline(@Param("patientId") Long patientId,
                                          @Param("scaleCode") String scaleCode);
}
