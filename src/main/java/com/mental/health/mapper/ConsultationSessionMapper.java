package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.ConsultationSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 在线咨询会话Mapper
 */
@Mapper
public interface ConsultationSessionMapper extends BaseMapper<ConsultationSession> {
}
