package com.sky.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.sky.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.data.repository.query.Param;

import com.github.pagehelper.Page;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;

@Mapper
public interface OrderMapper {

    void insert(Orders orders);

    Page<Orders> page(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO getStatistics();
    @Update("update orders set status = 6, cancel_reason = #{cancelReason}, cancel_time = now() where id = #{id}")
    void cancel(OrdersCancelDTO ordersCancelDTO);

    @Update("update orders set status = #{status} where id = #{id}")
    void complete(Orders orders);
    @Update("update orders set rejection_reason = #{rejectionReason}, status = 6 where id = #{id}")
    void rejection(OrdersRejectionDTO ordersRejectionDTO);
    @Update("update orders set status = #{status} where id = #{id}")
    void confirm(Orders orders);
    @Select("select * from orders where id = #{id}")
    OrderVO selete(Integer id);
    @Update("update orders set status = 4, delivery_time = #{estimatedDeliveryTime} where id = #{id}")
    void delivery(Integer id, LocalDateTime estimatedDeliveryTime);

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

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    Double sumBymap(Map map);

    List<DailyTurnoverDTO> conutgetTime(LocalDateTime beginTime, LocalDateTime endTime);

    List<OrderNumbersDTO> getOrderStatistcs(LocalDateTime beginTime, LocalDateTime endTime);
}
