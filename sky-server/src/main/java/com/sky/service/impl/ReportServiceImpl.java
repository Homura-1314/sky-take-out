package com.sky.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sky.dto.DailyTurnoverDTO;
import com.sky.dto.OrderNumbersDTO;
import com.sky.dto.UserStatisticsDTO;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

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
        List<LocalDate> dateList = dateInterval(begin, end);
        String s = StringUtils.join(dateList, ",");
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        // 一次性查询每天的营业额
        List<DailyTurnoverDTO> dailyTurnover = orderMapper.conutgetTime(beginTime, endTime);
        /**
         *
         * Map[日期，营业额]
         * 
         * */
        Map<LocalDate, Double> teDayTurnover = dailyTurnover.stream().
                collect(Collectors.toMap(DailyTurnoverDTO::getToday, DailyTurnoverDTO::getTurnover));

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate data : dateList) {
            turnoverList.add(teDayTurnover.getOrDefault(data, 0.0));
        }
        String TurnoverDate = StringUtils.join(turnoverList, ",");
        return TurnoverReportVO.builder().dateList(s).
                        turnoverList(TurnoverDate)
                        .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 用于存放begin到end之内的日期
        List<LocalDate> dateList = dateInterval(begin, end);
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

    @Override
    public OrderReportVO getOrderStatistcs(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = dateInterval(begin, end);
        String dateListStr = StringUtils.join(dateList, ",");
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<OrderNumbersDTO> numbersDTOS = orderMapper.getOrderStatistcs(beginTime, endTime);
        Map<LocalDate, OrderNumbersDTO> numbersDTOMap = numbersDTOS
                .stream()
                .collect(Collectors.toMap(OrderNumbersDTO::getDate, Function.identity()));
        List<Integer> totalOrderList = new ArrayList<>();
        List<Integer> validOrderList = new ArrayList<>();
        Integer totalOrderCount = 0;
        Integer validOrderCount = 0;
        Double orderCompletionRate;
        for (LocalDate date : dateList) {
            OrderNumbersDTO dto = numbersDTOMap.get(date);
            if (dto != null) {
                Integer totalOrdersNumber = dto.getOrderNumber();
                Integer validOrderNumber = dto.getValidOrderNumber();
                totalOrderCount += totalOrdersNumber;
                validOrderCount += validOrderNumber;
                totalOrderList.add(totalOrdersNumber);
                validOrderList.add(validOrderNumber);
            }else {
                totalOrderList.add(0);
                validOrderList.add(0);
            }
        }
        String totalOrderListStr = StringUtils.join(totalOrderList, ",");
        String validOrderListStr = StringUtils.join(validOrderList, ",");
        orderCompletionRate = totalOrderCount == 0 ? 0.0 : validOrderCount * 1.0 / totalOrderCount;
        return OrderReportVO.builder()
                .dateList(dateListStr)
                .orderCountList(totalOrderListStr)
                .validOrderCountList(validOrderListStr)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate).build();
    }

    public List<LocalDate> dateInterval(LocalDate begin, LocalDate end){
        // 用于存放begin到end之内的日期
        List<LocalDate> dateList = new ArrayList<>();
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            dateList.add(date);
        }
        return dateList;
    }

}
