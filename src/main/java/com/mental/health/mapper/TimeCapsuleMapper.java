package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.TimeCapsule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

/**
 * 时光信箱 Mapper
 */
@Mapper
public interface TimeCapsuleMapper extends BaseMapper<TimeCapsule> {

    /**
     * 获取用户的所有信件
     */
    @Select("SELECT * FROM time_capsule WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<TimeCapsule> getByUserId(@Param("userId") Long userId);

    /**
     * 获取用户可解锁的信件
     */
    @Select("SELECT * FROM time_capsule WHERE user_id = #{userId} AND status = 'sealed' AND unlock_date <= #{today}")
    List<TimeCapsule> getUnlockable(@Param("userId") Long userId, @Param("today") LocalDate today);

    /**
     * 解锁信件
     */
    @Update("UPDATE time_capsule SET status = 'unlocked', unlock_time = NOW() WHERE id = #{id} AND status = 'sealed'")
    Integer unlockLetter(@Param("id") Long id);

    /**
     * 标记已读
     */
    @Update("UPDATE time_capsule SET status = 'read', read_time = NOW() WHERE id = #{id} AND status = 'unlocked'")
    Integer markAsRead(@Param("id") Long id);
}
