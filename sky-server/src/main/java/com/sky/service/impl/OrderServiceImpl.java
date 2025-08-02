package com.sky.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersDTOS;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.entity.User;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.BaiduSDK;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;

import lombok.extern.slf4j.Slf4j;

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
    @Value("${sky.baidu.max-delivery}")
    private long Max_delivery;
    @Value("${sky.baidu.shop-coordinates}")
    private String shopCoordinates;
    @Value("${sky.payment.mock-enabled}")
    private boolean mockEnabled;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private UserMapper userMapper;
    private RestTemplate restTemplate = new RestTemplate();

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
                .userId(id)
                .build();
        List<ShoppingCart> carts = shoppingMapper.list(cart);
        if (carts == null || carts.isEmpty()) throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(ordersSubmitDTO.getDeliveryStatus());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(id);
        orders.setAddress(userAddress);
        orders.setTablewareStatus(Orders.PENDING_PAYMENT);
        orderMapper.insert(orders);
        // 向订单插入n条数据
        List<OrderDetail> orderDetailList = carts.stream().map(cart_ -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart_, orderDetail, "id", "userId", "createTime"); // 忽略不需要的属性
            orderDetail.setOrderId(orders.getId());
            return orderDetail;
        }).toList();
        orderDetailMapper.insertBatch(orderDetailList);
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
    public PageResult<OrderVO> page(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.page(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList<>();
        // 3. 判断是否有订单数据
        if (page != null && !page.isEmpty()) {
            // 4. 遍历分页查询出的每一个订单
            for (Orders orders : page) {
                // a. 创建一个新的 OrderVO 对象，用于组装最终结果
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);

                // c. 根据当前订单的 ID，查询其对应的订单详情列表
                Long orderId = orders.getId();
                List<OrderDetail> orderDetails = orderDetailMapper.selete(orderId);

                // d. 将查询到的订单详情列表设置到 OrderVO 中
                orderVO.setOrderDetailList(orderDetails);
                // 使用 Stream API 一行代码完成拼接
                String orderDishes = orderDetails.stream()
                        .map(OrderDetail::getName)
                        .collect(Collectors.joining(","));
                // e. 将组装好的 OrderVO 添加到最终的结果列表中
                orderVO.setOrderDishes(orderDishes);
                list.add(orderVO);
            }
        }
        return new PageResult<>(page != null ? page.getTotal() : 0, list);
    }

    @Override
    public OrderStatisticsVO statistics() {
        return orderMapper.getStatistics();
    }

    @Override
    public void complete(Long id) {
        Orders build = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .build();
        orderMapper.complete(build);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        orderMapper.rejection(ordersRejectionDTO);
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders build = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.confirm(build);
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
        Orders orders = orderMapper.getById(Long.valueOf(id));
        orderMapper.delivery(id, orders.getEstimatedDeliveryTime());
    }

    @SuppressWarnings("null")
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

    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        if (mockEnabled) {
            log.warn("当前为模拟支付流程，跳过真实微信支付...");
            String orderNumber = ordersPaymentDTO.getOrderNumber();
            // 构造模拟回调的 URL
            String notifyUrl = "http://localhost:8080/notify/paySuccess/mock?orderNumber=" + orderNumber;
            try {
                restTemplate.getForObject(notifyUrl, String.class);
            } catch (RestClientException e) {
                log.error("自动模拟回调失败，请检查/notify/paySuccess/mock接口是否正常", e);
            }
            // 立即将订单状态改为“已支付”，以便后续流程能够进行
            paySuccess(ordersPaymentDTO.getOrderNumber());
            OrderPaymentVO vo = OrderPaymentVO.builder()
                    .nonceStr("mockNonceStr" + System.currentTimeMillis()) // 模拟一个随机字符串
                    .paySign("mockPaySign" + System.currentTimeMillis())   // 模拟一个签名
                    .timeStamp(String.valueOf(System.currentTimeMillis() / 1000)) // 模拟一个时间戳 (秒)
                    .signType("RSA") // 模拟签名算法
                    .packageStr("prepay_id=mockPrepayId" + System.currentTimeMillis()) // 模拟 package 字符串
                    .build();
            return vo;
        }
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getByOpenid(String.valueOf(userId));

        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(),
                new BigDecimal("0.01"),
                "苍穹外卖订单",
                user.getOpenid()
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    @Override
    public void paySuccess(String outTradeNo) {
        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        if (ordersDB == null || ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 根据用户id查询当前的地址
        Long userid = BaseContext.getCurrentId();
        AddressBook addressBook = addressBookMapper.getById(userid);
        // 获得用户的地址
        String userAddress  = addressBook.getProvinceName() + addressBook.getCityName() +
                addressBook.getDistrictName() + addressBook.getDetail();
        // 将地址转化为坐标
        String coordinates = baiduSDK.getCoordinates(userAddress);
        if (coordinates == null || coordinates.isEmpty()) throw new OrderBusinessException("收货地址解析失败，无法计算配送距离");
        // 计算预计到达的时间
        long timeConsuming = baiduSDK.getTimeConsuming(coordinates, shopCoordinates);
        LocalDateTime dateTime = LocalDateTime.now().plusMinutes(timeConsuming);
        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .estimatedDeliveryTime(dateTime)
                .build();
        orderMapper.update(orders);
        Map map = new HashMap<>();
        map.put("type", 1); // 1表示来单提醒 2表示客户催单
        map.put("orderId", userid);
        map.put("content", "订单号" + outTradeNo);
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    @Override
    public void reminder(Long id) {
        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null || ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("content", ordersDB.getNumber());
        String jsonString = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
    }
}
