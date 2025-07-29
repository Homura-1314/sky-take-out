package com.sky.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.sky.exception.OrderBusinessException;
import com.sky.utils.BaiduSDK;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersDTOS;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingMapper shoppingMapper;
    @Autowired
    private BaiduSDK baiduSDK;
    @Value("${sky.baidu.shop}")
    private String shop;
    @Value("${sky.baidu.max-delivery}")
    private long Max_delivery;
    @Value("${sky.baidu.shop-coordinates}")
    private String shopCoordinates;

    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        // 获得用户的地址
        String userAddress  = addressBook.getProvinceName() + addressBook.getCityName() +
                addressBook.getDistrictName() + addressBook.getDetail();
        // 将地址转化为坐标
        String coordinates = baiduSDK.getCoordinates(userAddress);
        if (coordinates == null || coordinates.isEmpty()) throw new OrderBusinessException("收货地址解析失败，无法计算配送距离");
        // 计算用户和商家的骑行距离
        long distance = baiduSDK.getRidingDistance(shopCoordinates, coordinates);
        if (distance == -1L) throw new OrderBusinessException("无法规划配送路线，请检查地址是否正确");
        // 判断是否超出配送范围
        if (distance > Max_delivery){
            log.warn("用户地址超出配送范围，距离：{}米", distance);
            throw new OrderBusinessException("您的收货地址超出本店配送范围");
        }
        Long id = BaseContext.getCurrentId();
        ShoppingCart cart = ShoppingCart.builder()
                .id(id)
                .build();
        List<ShoppingCart> carts = shoppingMapper.list(cart);
        if (carts.isEmpty()) throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(id);
        orders.setAddress(userAddress);
        orderMapper.insert(orders);
        // 向订单插入n条数据
        List<OrderDetail> orderDetails = new ArrayList<>();
        carts.forEach(item -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(item, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        });
        orderDetailMapper.insertBatch(orderDetails);
        shoppingMapper.cleanShopping(id);
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        orderMapper.cancel(ordersCancelDTO);
    }

    @Override
    public PageResult<Orders> page(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.page(ordersPageQueryDTO);
        return new PageResult<>(page.getTotal(), page.getResult());
    }

    @Override
    public OrderStatisticsVO statistics() {
        return orderMapper.getStatistics();
    }

    @Override
    public void complete(Integer id) {
        orderMapper.complete(id);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        orderMapper.rejection(ordersRejectionDTO);
    }

    @Override
    public void confirm(Integer id) {
        orderMapper.confirm(id);
    }

    @Override
    public OrderVO details(Integer id) {
        Orders orders = orderMapper.selete(id);
        // 查询该订单对应的菜品/套餐明细
        List<OrderDetail> details = orderDetailMapper.selete(orders.getId());
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(details);
        return orderVO;
    }

    @Override
    public void delivery(Integer id) {
        orderMapper.delivery(id);
    }

    @Override
    public PageResult<OrdersDTOS> historyOrders(int page, int pageSize, Integer status) {
        // 设置分页
        PageHelper.startPage(page, pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 分页条件查询
        Page<Orders> pages = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList();
        // 查询出订单明细，并封装入OrderVO进行响应
        if (pages != null && pages.getTotal() > 0) {
            for (Orders orders : pages) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.selete(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
            return new PageResult(pages.getTotal(), list);
    }

    @Override
    public void repetition(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.selete(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingMapper.insert_for(shoppingCartList);
    }
}
