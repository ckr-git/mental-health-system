package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.MoodComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 心情留言 Mapper
 */
@Mapper
public interface MoodCommentMapper extends BaseMapper<MoodComment> {

    /**
     * 获取日记的所有留言（按时间倒序）
     */
    @Select("SELECT * FROM mood_comment WHERE diary_id = #{diaryId} ORDER BY create_time DESC")
    List<MoodComment> getByDiaryId(@Param("diaryId") Long diaryId);

    /**
     * 统计日记的留言数
     */
    @Select("SELECT COUNT(*) FROM mood_comment WHERE diary_id = #{diaryId}")
    Integer countByDiaryId(@Param("diaryId") Long diaryId);
}
