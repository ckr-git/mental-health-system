package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.PatientDoctorRelationship;
import org.apache.ibatis.annotations.Mapper;

/**
 * 医患关系Mapper
 */
@Mapper
public interface PatientDoctorRelationshipMapper extends BaseMapper<PatientDoctorRelationship> {
}
