package com.sky.service;

import java.time.LocalDate;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import javax.servlet.http.HttpServletResponse;

public interface ReportService {

    TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end);

    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    OrderReportVO getOrderStatistcs(LocalDate begin, LocalDate end);

    SalesTop10ReportVO getrdersSalesTop10(LocalDate begin, LocalDate end);

    void exportBusinessData(HttpServletResponse response);
}
