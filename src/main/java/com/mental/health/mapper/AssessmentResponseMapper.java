package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.AssessmentResponse;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AssessmentResponseMapper extends BaseMapper<AssessmentResponse> {

    @Select("SELECT * FROM assessment_response WHERE session_id = #{sessionId} AND deleted = 0 ORDER BY item_id")
    List<AssessmentResponse> findBySessionId(@Param("sessionId") Long sessionId);

    @Insert("INSERT INTO assessment_response (session_id, item_id, answer_value, answer_label, deleted, create_time, update_time) " +
            "VALUES (#{sessionId}, #{itemId}, #{answerValue}, #{answerLabel}, 0, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE answer_value = VALUES(answer_value), answer_label = VALUES(answer_label), deleted = 0, update_time = NOW()")
    int upsertAnswer(@Param("sessionId") Long sessionId, @Param("itemId") Long itemId,
                     @Param("answerValue") Integer answerValue, @Param("answerLabel") String answerLabel);
}
