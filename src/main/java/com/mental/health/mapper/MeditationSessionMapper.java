package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.MeditationSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MeditationSessionMapper extends BaseMapper<MeditationSession> {

    @Select("SELECT COALESCE(SUM(actual_seconds), 0) FROM meditation_session WHERE user_id = #{userId} AND session_status = 'COMPLETED' AND deleted = 0")
    Long totalPracticeSeconds(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM meditation_session WHERE user_id = #{userId} AND session_status = 'COMPLETED' AND deleted = 0")
    Long completedCount(@Param("userId") Long userId);
}
