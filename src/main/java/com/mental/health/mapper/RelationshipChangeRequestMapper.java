package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.RelationshipChangeRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 患者分配变更请求Mapper
 */
@Mapper
public interface RelationshipChangeRequestMapper extends BaseMapper<RelationshipChangeRequest> {
}
