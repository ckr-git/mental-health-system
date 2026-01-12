package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.AIConversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI对话记录Mapper接口
 */
@Mapper
public interface AIConversationMapper extends BaseMapper<AIConversation> {
}
