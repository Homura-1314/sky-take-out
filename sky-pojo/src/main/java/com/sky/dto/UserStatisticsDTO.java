package com.sky.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserStatisticsDTO {
    // 日期
    private LocalDate date;

    // 当日新增用户数
    private Integer newUserCount;
}
