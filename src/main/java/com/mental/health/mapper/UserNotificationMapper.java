package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.UserNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {

    @Select("SELECT COUNT(*) FROM user_notification WHERE user_id = #{userId} AND read_status = 0 AND deleted = 0")
    Long countUnread(@Param("userId") Long userId);

    @Update("UPDATE user_notification SET read_status = 1, read_time = NOW(), update_time = NOW() WHERE user_id = #{userId} AND read_status = 0 AND deleted = 0")
    int markAllRead(@Param("userId") Long userId);
}
