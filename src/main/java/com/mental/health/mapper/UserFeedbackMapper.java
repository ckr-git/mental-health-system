package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.UserFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户反馈Mapper接口
 */
@Mapper
public interface UserFeedbackMapper extends BaseMapper<UserFeedback> {
}
