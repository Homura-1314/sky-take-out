package com.sky.controller.admin;

import com.sky.dto.OrdersDTOS;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.vo.OrderStatisticsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/admin/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PutMapping("/cancel")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO) {
        log.info("取消订单为：{}", ordersCancelDTO);
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }

    @GetMapping("/conditionSearch")
    public Result<PageResult> page(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("分页查询：{}", ordersPageQueryDTO);
        PageResult<Orders> pageResults = orderService.page(ordersPageQueryDTO);
        return Result.success(pageResults);
    }

    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> statistics() {
        log.info("订单数量统计");
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    @PutMapping("/complete/{id]")
    public Result complete(@PathVariable Integer id) {
        log.info("完成订单id：{}", id);
        orderService.complete(id);
        return Result.success();
    }

    @PutMapping("/rejection")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO) {
        log.info("拒单原因：{}}", ordersRejectionDTO);
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    @PutMapping("/confirm")
    public Result confirm(@RequestParam Integer id) {
        log.info("接单id：{}", id);
        orderService.confirm(id);
        return Result.success();
    }

    @GetMapping("/details/{id}")
    public Result<OrdersDTOS> details(@PathVariable Integer id){
        log.info("根据id查询订单信息：{}", id);
        OrdersDTOS ordersDTOS = orderService.details(id);
        return Result.success(ordersDTOS);
    }

    @PutMapping("/delivery/{id}")
    public Result delivery(@PathVariable Integer id) {
        log.info("派送订单:{}", id);
        orderService.delivery(id);
        return Result.success();
    }

}
