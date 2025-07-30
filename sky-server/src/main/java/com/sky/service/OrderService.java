package com.sky.service;

import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {

    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    void cancel(OrdersCancelDTO ordersCancelDTO);

    PageResult<Orders> page(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO statistics();

    void complete(Long id);

    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    void confirm(OrdersConfirmDTO id);

    OrderVO details(Integer id);

    void delivery(Integer id);

    PageResult historyOrders(int page , int pageSize, Integer status);

    void repetition(Long id);

    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

}
