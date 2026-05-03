package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.AssessmentAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AssessmentAssignmentMapper extends BaseMapper<AssessmentAssignment> {

    @Select("SELECT * FROM assessment_assignment WHERE assignment_status = 'ASSIGNED' " +
            "AND due_at <= NOW() AND deleted = 0")
    List<AssessmentAssignment> findOverdueAssignments();

    @Select("SELECT * FROM assessment_assignment WHERE patient_id = #{patientId} " +
            "AND assignment_status IN ('ASSIGNED','IN_PROGRESS') AND deleted = 0 " +
            "ORDER BY due_at ASC")
    List<AssessmentAssignment> findPendingByPatient(@Param("patientId") Long patientId);
}
