package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.NotificationTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NotificationTemplateMapper extends BaseMapper<NotificationTemplate> {

    @Select("SELECT * FROM notification_template WHERE template_code = #{code} AND active = 1 AND deleted = 0 LIMIT 1")
    NotificationTemplate findByCode(@Param("code") String templateCode);
}
