package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.vo.TurnoverReportVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/report")
public class ReportController {

    public Result<TurnoverReportVO> turnoverReport(
            @DateTimeFormat(pattern = "yyyy")
            LocalDate begin,
            LocalDate end) {

    }

}
