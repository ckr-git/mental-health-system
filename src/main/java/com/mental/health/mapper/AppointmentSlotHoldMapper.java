package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.AppointmentSlotHold;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Mapper
public interface AppointmentSlotHoldMapper extends BaseMapper<AppointmentSlotHold> {

    @Select("SELECT * FROM appointment_slot_hold WHERE doctor_id = #{doctorId} " +
            "AND slot_date = #{slotDate} AND slot_start = #{slotStart} " +
            "AND hold_status = 'HELD' AND deleted = 0")
    AppointmentSlotHold findActiveHold(@Param("doctorId") Long doctorId,
                                       @Param("slotDate") LocalDate slotDate,
                                       @Param("slotStart") LocalTime slotStart);

    @Select("SELECT * FROM appointment_slot_hold WHERE hold_status = 'HELD' " +
            "AND expire_at <= NOW() AND deleted = 0")
    List<AppointmentSlotHold> findExpiredHolds();
}
