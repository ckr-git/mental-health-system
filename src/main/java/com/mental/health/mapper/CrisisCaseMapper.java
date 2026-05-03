package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.CrisisCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrisisCaseMapper extends BaseMapper<CrisisCase> {

    @Select("SELECT * FROM crisis_case WHERE patient_id = #{patientId} " +
            "AND case_status NOT IN ('RESOLVED','POST_REVIEW') AND deleted = 0 " +
            "ORDER BY create_time DESC LIMIT 1")
    CrisisCase findOpenCaseByPatient(@Param("patientId") Long patientId);

    @Select("SELECT * FROM crisis_case WHERE case_status IN ('NEW','TRIAGED','ACKED','CONTACTING','INTERVENING','ESCALATED') " +
            "AND sla_deadline <= NOW() AND deleted = 0")
    List<CrisisCase> findOverdueCases();

    @Select("SELECT * FROM crisis_case WHERE case_status = 'RESOLVED' " +
            "AND post_review_due_at IS NOT NULL AND post_review_completed_at IS NULL " +
            "AND post_review_due_at <= NOW() AND deleted = 0")
    List<CrisisCase> findPendingPostReviews();
}
