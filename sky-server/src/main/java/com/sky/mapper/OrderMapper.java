package com.sky.mapper;

import com.sky.dto.OrdersDTOS;
import com.sky.dto.OrdersRejectionDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;

@Mapper
public interface OrderMapper {

    void insert(Orders orders);

    Page<Orders> page(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO getStatistics();
    @Update("update orders set status = 6, cancel_reason = #{cancelReason} where id = #{id}")
    void cancel(OrdersCancelDTO ordersCancelDTO);

    @Update("update orders set status = 5 where id = #{id}")
    void complete(Integer id);
    @Update("update orders set rejection_reason = #{rejectionReason} where id = #{id}")
    void rejection(OrdersRejectionDTO ordersRejectionDTO);
    @Update("update orders set status = 3 where id = #{id}")
    void confirm(Integer id);
    @Select("select * from orders where id = #{id}")
    OrdersDTOS selete(Integer id);
    @Update("update orders set status = 4 where id = #{id}")
    void delivery(Integer id);

    Page<OrdersDTOS> pageUser(int page, int pageSize, Integer status);

    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);
}
