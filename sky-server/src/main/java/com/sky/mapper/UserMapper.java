package com.sky.mapper;

import com.sky.dto.UserStatisticsDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.sky.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("select * from user where id = #{id}")
    User getByOpenid(String id);


    void insert(User build);

    @Select("select * from user where openid = #{openId}")
    public User getByOpenids(String openId);

    List<UserStatisticsDTO> countNewUsersByDateRange(LocalDateTime beginTime, LocalDateTime endTime);

    Integer countTotalUsersBeforeDate(LocalDateTime beginTime);
}
