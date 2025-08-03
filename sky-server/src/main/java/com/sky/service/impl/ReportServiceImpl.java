package com.sky.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sky.dto.GoodsSalesDTO;
import com.sky.mapper.OrderDetailMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sky.dto.DailyTurnoverDTO;
import com.sky.dto.OrderNumbersDTO;
import com.sky.dto.UserStatisticsDTO;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WorkspaceService workspaceService;

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

    @Override
    public SalesTop10ReportVO getrdersSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> reportDTOList = orderDetailMapper.getcountData(beginTime, endTime);
        String nameList = reportDTOList.stream().map(GoodsSalesDTO::getName).
                collect(Collectors.joining(","));
        String numbersList = StringUtils.join(reportDTOList.
                stream().map(GoodsSalesDTO::getNumber)
                .toList(), ",");
        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numbersList)
                .build();
    }

    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //1. 查询数据库，获取营业数据---查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        //2. 通过POI将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            //获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            //3. 通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
