package com.sky.Task;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    @Scheduled(cron = "0 * * * * *")
    public void processTimeOrder(){
        log.info("定时处理超时订单：{}", LocalDateTime.now());
        List<Orders> orderTime = orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(-15));
        if (!orderTime.isEmpty()){
            // 2. 从对象列表中提取出 ID 列表
            List<Long> ids = orderTime.stream()
                    .map(Orders::getId)
                    .toList();
            // 3. 执行一次性的批量更新
            log.info("批量取消超时订单，IDs: {}", ids);
            orderMapper.updateStatusByIds(
                    Orders.CANCELLED,
                    "订单超时，自动取消",
                    LocalDateTime.now(),
                    ids
            );
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        log.info("处于派送中的订单：{}", LocalDateTime.now());
        List<Orders> orderTime = orderMapper.getByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().plusMinutes(-60));
        if (!orderTime.isEmpty()){
            // 2. 从对象列表中提取出 ID 列表
            List<Long> ids = orderTime.stream()
                    .map(Orders::getId)
                    .toList();
            // 3. 执行一次性的批量更新
            log.info("批量取消超时订单，IDs: {}", ids);
            orderMapper.updateStatusByIds(
                    Orders.COMPLETED,
                    "订单已完成",
                    LocalDateTime.now(),
                    ids
            );
        }

    }


}
