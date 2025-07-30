package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.sky.entity.User;

@Mapper
public interface UserMapper {

    @Select("select * from user where id = #{id}")
    User getByOpenid(String id);


    void insert(User build);

    @Select("select * from user where openid = #{openId}")
    public User getByOpenids(String openId);
}
