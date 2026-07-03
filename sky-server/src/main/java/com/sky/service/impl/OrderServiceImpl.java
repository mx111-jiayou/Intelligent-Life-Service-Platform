package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.service.OrderService;
import com.sky.utils.RedisIdWorker;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired

    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private RedisIdWorker redisIdWorker;

    /**
     * 用户下单
     *
     * @param orderSubmitDTO 订单提交DTO
     * @return 订单提交VO
     */
    @Transactional

    public OrderSubmitVO submitOrder(OrdersSubmitDTO orderSubmitDTO) {
        //处理各种业务异常(地址簿 购物车数据为空 等)
        AddressBook addressBook = addressBookMapper.getById(orderSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //查询当前用户的购物车数据
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        shoppingCartMapper.list(shoppingCart);
        List<ShoppingCart> shoppingCartlist = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartlist == null || shoppingCartlist.size() == 0) {
            //抛出业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //查询当前用户的购物车数据

        //向订单表插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(orderSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setNumber(String.valueOf(redisIdWorker.nextOrderId()));
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setUserId(userId);
        orderMapper.insert(orders);
        List<OrderDetail> orderDetailList = new ArrayList<>();
        //向订单明细表插入n条数据
        for (ShoppingCart cart : shoppingCartlist) {
            OrderDetail orderDetail = new OrderDetail();//订单明细
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());//设置订单id
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
        //清空当前用户的购物车数据
        shoppingCartMapper.deleteByUserId(userId);
        //封装vo返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);
        Orders orders = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                orders.getAmount(), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
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
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
        //向客户端浏览器推送消息 type orderId context
        Map map = new HashMap<>();
        map.put("type", 1);//1 表示来单提醒 2 表示客户催单
        map.put("orderId", ordersDB.getId());//订单id
        map.put("content", "订单号：" + outTradeNo);//上下文
        String json = JSONObject.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 客户催单
     *
     * @param id 订单id
     */
    public void reminder(Long id) {
        // 根据订单id查询订单
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            //抛出业务异常
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Map map = new HashMap<>();
        map.put("type", 2);//2 表示客户催单
        map.put("orderId", id);//订单id
        map.put("content", "订单号：" + ordersDB.getNumber());//上下文

        webSocketServer.sendToAllClient(JSON.toJSONString(map));

    }

    @Override
    public String queryLatestOrderSummary() {
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderMapper.getLatestByUserId(userId);
        if (orders == null) {
            return "暂未查询到你的订单记录。";
        }
        return "你最近一笔订单号为" + orders.getNumber()
                + "，订单状态：" + formatOrderStatus(orders.getStatus())
                + "，实付金额：" + orders.getAmount()
                + "，下单时间：" + orders.getOrderTime() + "。";
    }

    @Override
    public String cancelOrderForAi(Long id, String reason) {
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderMapper.getByIdAndUserId(id, userId);
        if (orders == null) {
            return "未查询到该订单，无法取消。";
        }
        if (Orders.COMPLETED.equals(orders.getStatus()) || Orders.CANCELLED.equals(orders.getStatus())) {
            return "该订单当前状态为" + formatOrderStatus(orders.getStatus()) + "，无法取消。";
        }
        Orders updateOrder = Orders.builder()
                .id(id)
                .status(Orders.CANCELLED)
                .cancelReason(reason)
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(updateOrder);
        return "订单" + orders.getNumber() + "已为你提交取消处理。";
    }

    private String formatOrderStatus(Integer status) {
        if (Orders.PENDING_PAYMENT.equals(status)) {
            return "待付款";
        }
        if (Orders.TO_BE_CONFIRMED.equals(status)) {
            return "待接单";
        }
        if (Orders.CONFIRMED.equals(status)) {
            return "已接单";
        }
        if (Orders.DELIVERY_IN_PROGRESS.equals(status)) {
            return "派送中";
        }
        if (Orders.COMPLETED.equals(status)) {
            return "已完成";
        }
        if (Orders.CANCELLED.equals(status)) {
            return "已取消";
        }
        return "未知状态";
    }
}
