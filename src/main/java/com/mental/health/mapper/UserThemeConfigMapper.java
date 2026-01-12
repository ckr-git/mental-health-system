package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.UserThemeConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户主题配置 Mapper
 */
@Mapper
public interface UserThemeConfigMapper extends BaseMapper<UserThemeConfig> {

    /**
     * 根据用户ID获取配置
     */
    @Select("SELECT * FROM user_theme_config WHERE user_id = #{userId}")
    UserThemeConfig getByUserId(@Param("userId") Long userId);
}
