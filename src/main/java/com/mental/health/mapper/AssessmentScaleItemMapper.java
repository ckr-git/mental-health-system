package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.AssessmentScaleItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AssessmentScaleItemMapper extends BaseMapper<AssessmentScaleItem> {

    @Select("SELECT * FROM assessment_scale_item WHERE scale_id = #{scaleId} AND deleted = 0 ORDER BY item_no")
    List<AssessmentScaleItem> findByScaleId(@Param("scaleId") Long scaleId);
}
