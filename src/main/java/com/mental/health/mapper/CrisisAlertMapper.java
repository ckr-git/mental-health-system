package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.CrisisAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CrisisAlertMapper extends BaseMapper<CrisisAlert> {

    @Select("SELECT * FROM crisis_alert WHERE user_id = #{userId} AND alert_level = #{level} " +
            "AND alert_status IN ('OPEN','ACKNOWLEDGED') AND deleted = 0 " +
            "AND create_time >= #{since} LIMIT 1")
    CrisisAlert findRecentActiveByUserAndLevel(@Param("userId") Long userId,
                                                @Param("level") String level,
                                                @Param("since") LocalDateTime since);

    @Select("SELECT * FROM crisis_alert WHERE alert_status IN ('OPEN','ACKNOWLEDGED') " +
            "AND sla_deadline <= #{now} AND deleted = 0")
    List<CrisisAlert> findOverdueOpenAlerts(@Param("now") LocalDateTime now);
}
