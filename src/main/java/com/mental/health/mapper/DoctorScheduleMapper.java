package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.DoctorSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DoctorScheduleMapper extends BaseMapper<DoctorSchedule> {

    @Select("SELECT * FROM doctor_schedule WHERE doctor_id = #{doctorId} AND active = 1 AND deleted = 0 ORDER BY day_of_week, start_time")
    List<DoctorSchedule> findActiveByDoctorId(Long doctorId);
}
