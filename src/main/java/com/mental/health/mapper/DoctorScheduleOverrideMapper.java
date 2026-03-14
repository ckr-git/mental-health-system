package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.DoctorScheduleOverride;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DoctorScheduleOverrideMapper extends BaseMapper<DoctorScheduleOverride> {

    @Select("SELECT * FROM doctor_schedule_override WHERE doctor_id = #{doctorId} AND override_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0")
    List<DoctorScheduleOverride> findByDoctorAndDateRange(Long doctorId, LocalDate startDate, LocalDate endDate);

    @Select("SELECT * FROM doctor_schedule_override WHERE doctor_id = #{doctorId} AND override_date = #{date} AND deleted = 0")
    DoctorScheduleOverride findByDoctorAndDate(Long doctorId, LocalDate date);
}
