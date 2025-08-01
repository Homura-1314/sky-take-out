package com.sky.mapper;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderDetailMapper {

    void insertBatch(List<OrderDetail> orderDetails);
    @Select("select * from order_detail where order_id = #{id}")
    List<OrderDetail> selete(Long id);
    @Select("select * from order_detail")
    List<List<OrderDetail>> listData();

    List<GoodsSalesDTO> getcountData(LocalDateTime beginTime, LocalDateTime endTime);
}
