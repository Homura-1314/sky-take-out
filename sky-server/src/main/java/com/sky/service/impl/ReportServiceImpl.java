package com.sky.service.impl;

import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ReportServiceImpl implements ReportService {

    @Override
    public TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end) {
        return null;
    }
}
