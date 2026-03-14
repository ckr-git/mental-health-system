package com.mental.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mental.health.entity.MeditationExercise;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MeditationExerciseMapper extends BaseMapper<MeditationExercise> {

    @Select("SELECT * FROM meditation_exercise WHERE active = 1 AND deleted = 0 ORDER BY category, duration_seconds")
    List<MeditationExercise> findAllActive();
}
