package com.sky.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SetneakDishMapper {

    
    List<Long> getSetmealIds(List<Long> ids);
    
}