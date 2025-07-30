package com.sky.mapper;

import com.sky.dto.*;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.github.pagehelper.Page;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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
    OrderVO selete(Integer id);
    @Update("update orders set status = 4 where id = #{id}")
    void delivery(Integer id);

    Page<OrdersDTOS> pageUser(int page, int pageSize, Integer status);

    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTime(Integer status, LocalDateTime orderTime);

    /**
     * 批量更新订单状态
     * @param status       目标状态
     * @param cancelReason 取消原因
     * @param cancelTime   取消时间
     * @param ids          需要更新的订单ID列表
     */
    void updateStatusByIds(@Param("status") Integer status,
                           @Param("cancelReason") String cancelReason,
                           @Param("cancelTime") LocalDateTime cancelTime,
                           @Param("ids") List<Long> ids);
    @Update("update orders set pay_method = #{ordersPaymentDTO.payMethod}, " +
            "estimated_delivery_time = #{dateTime}," +
            "number = #{ordersPaymentDTO.orderNumber}, pay_status = 1, checkout_time = now()," +
            "status = 2 where id = #{userid}")
    void updateTiemOut(OrdersPaymentDTO ordersPaymentDTO, Long userid, LocalDateTime dateTime);
}
