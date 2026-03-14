package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.AssessmentSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AssessmentSessionMapper extends BaseMapper<AssessmentSession> {

    @Select("SELECT * FROM assessment_session WHERE id = #{sessionId} AND user_id = #{userId} AND session_status = 'IN_PROGRESS' AND deleted = 0 FOR UPDATE")
    AssessmentSession lockOwnedInProgressSession(@Param("userId") Long userId,
                                                 @Param("sessionId") Long sessionId);
}
