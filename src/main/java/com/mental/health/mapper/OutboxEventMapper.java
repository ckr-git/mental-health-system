package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEvent> {

    @Select("SELECT COUNT(1) FROM outbox_event WHERE event_key = #{eventKey}")
    boolean existsByEventKey(@Param("eventKey") String eventKey);

    @Select("SELECT * FROM outbox_event " +
            "WHERE status IN ('PENDING', 'FAILED') AND next_retry_time <= #{now} " +
            "ORDER BY next_retry_time ASC LIMIT #{batchSize} FOR UPDATE SKIP LOCKED")
    List<OutboxEvent> lockNextBatch(@Param("batchSize") int batchSize,
                                   @Param("now") LocalDateTime now);

    @Update("UPDATE outbox_event SET status = 'SENT', update_time = NOW() WHERE id = #{id}")
    int markSent(@Param("id") Long id);

    @Update("UPDATE outbox_event SET status = 'FAILED', retry_count = retry_count + 1, " +
            "next_retry_time = #{nextRetryTime}, last_error = #{error}, update_time = NOW() " +
            "WHERE id = #{id}")
    int scheduleRetry(@Param("id") Long id,
                      @Param("nextRetryTime") LocalDateTime nextRetryTime,
                      @Param("error") String error);

    @Update("UPDATE outbox_event SET status = 'DEAD', last_error = #{error}, update_time = NOW() " +
            "WHERE id = #{id}")
    int markDead(@Param("id") Long id, @Param("error") String error);
}
