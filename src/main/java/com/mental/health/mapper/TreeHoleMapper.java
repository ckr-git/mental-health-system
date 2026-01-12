package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.TreeHole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 心情树洞Mapper
 */
@Mapper
public interface TreeHoleMapper extends BaseMapper<TreeHole> {

    /**
     * 获取用户的所有未消失的倾诉记录
     */
    @Select("SELECT * FROM tree_hole WHERE user_id = #{userId} AND is_expired = 0 ORDER BY create_time DESC")
    List<TreeHole> getActiveByUserId(@Param("userId") Long userId);

    /**
     * 获取用户的档案馆记录（所有记录，包括已消失的）
     */
    @Select("SELECT * FROM tree_hole WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<TreeHole> getArchiveByUserId(@Param("userId") Long userId);

    /**
     * 根据倾诉对象类型获取记录
     */
    @Select("SELECT * FROM tree_hole WHERE user_id = #{userId} AND speak_to_type = #{speakToType} ORDER BY create_time DESC")
    List<TreeHole> getByUserIdAndType(@Param("userId") Long userId, @Param("speakToType") String speakToType);

    /**
     * 标记已过期的记录
     */
    @Update("UPDATE tree_hole SET is_expired = 1 WHERE expire_time <= #{now} AND is_expired = 0 AND expire_type != 'forever'")
    int markExpired(@Param("now") LocalDateTime now);

    /**
     * 增加查看次数
     */
    @Update("UPDATE tree_hole SET view_count = view_count + 1, last_view_time = #{viewTime} WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id, @Param("viewTime") LocalDateTime viewTime);
}
