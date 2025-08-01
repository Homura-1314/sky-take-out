package com.sky.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OrderNumbersDTO {
    private LocalDate date;
    private Integer orderNumber;
    private Integer validOrderNumber;
}
