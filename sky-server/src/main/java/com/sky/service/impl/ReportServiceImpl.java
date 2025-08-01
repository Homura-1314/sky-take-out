package com.sky.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sky.dto.UserStatisticsDTO;
import com.sky.mapper.UserMapper;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end) {
        // 用于存放begin到end之内的日期
        List<LocalDate> dateList = new ArrayList<>();
        while (begin.isBefore(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumBymap(map);
            turnover = turnover == null ? 0 : turnover;
            turnoverList.add(turnover);
        }
        String s = StringUtils.join(dateList, ",");

        return TurnoverReportVO.builder().dateList(s).
                turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 用于存放begin到end之内的日期
        List<LocalDate> dateList = new ArrayList<>();
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            dateList.add(date);
        }
        String dateListStr = StringUtils.join(dateList, ",");
        // 一次性查询每日新增用户数
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        // 一次数据库查询
        List<UserStatisticsDTO> dailyNewUsers = userMapper.countNewUsersByDateRange(beginTime, endTime);

        // 将列表结果转为 Map<日期, 数量>
        Map<LocalDate, Integer> newUserMap = dailyNewUsers.stream()
                .collect(Collectors.toMap(UserStatisticsDTO::getDate, UserStatisticsDTO::getNewUserCount));

        // 填充每日新增用户列表 (处理没有新增用户的日期)
        List<Integer> newUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            newUserList.add(newUserMap.getOrDefault(date, 0));
        }
        String newUserListStr = StringUtils.join(newUserList, ",");

        // 计算每日总用户数
        List<Integer> totalUserList = new ArrayList<>();
        // 获取起始日期之前的总用户数
        Integer initialTotalUsers = userMapper.countTotalUsersBeforeDate(beginTime);
        if (initialTotalUsers == null) initialTotalUsers = 0;

        Integer runningTotal = initialTotalUsers;
        for (Integer newUserCount : newUserList) {
            runningTotal += newUserCount;
            totalUserList.add(runningTotal);
        }
        String totalUserListStr = StringUtils.join(totalUserList, ",");

        // --- 5. 构建并返回结果 ---
        return UserReportVO.builder()
                .dateList(dateListStr)
                .totalUserList(totalUserListStr)
                .newUserList(newUserListStr)
                .build();
    }
}
