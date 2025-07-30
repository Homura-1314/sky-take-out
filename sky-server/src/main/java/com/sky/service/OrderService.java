package com.sky.service;

import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

import java.time.LocalDateTime;

public interface OrderService {

    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    void cancel(OrdersCancelDTO ordersCancelDTO);

    PageResult<Orders> page(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO statistics();

    void complete(Integer id);

    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    void confirm(Integer id);

    OrderVO details(Integer id);

    void delivery(Integer id);

    PageResult historyOrders(int page , int pageSize, Integer status);

    void repetition(Long id);

    LocalDateTime payment(OrdersPaymentDTO ordersPaymentDTO);
}
