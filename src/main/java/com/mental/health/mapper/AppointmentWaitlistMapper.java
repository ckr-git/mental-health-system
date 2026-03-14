package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.AppointmentWaitlist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AppointmentWaitlistMapper extends BaseMapper<AppointmentWaitlist> {

    @Select("SELECT * FROM appointment_waitlist WHERE doctor_id = #{doctorId} AND preferred_date = #{date} AND status = 'WAITING' AND deleted = 0 ORDER BY create_time ASC")
    List<AppointmentWaitlist> findWaitingByDoctorAndDate(Long doctorId, LocalDate date);

    @Select("SELECT * FROM appointment_waitlist WHERE patient_id = #{patientId} AND status IN ('WAITING', 'NOTIFIED') AND deleted = 0 ORDER BY preferred_date ASC")
    List<AppointmentWaitlist> findActiveByPatient(Long patientId);
}
