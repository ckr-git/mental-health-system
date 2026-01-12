package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预约Mapper接口
 */
@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {
}
