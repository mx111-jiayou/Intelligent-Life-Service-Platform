package com.sky.service;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;

public interface OrderService {
    /**
     * 用户下单
     * @param orderSubmitDTO 订单提交DTO
     * @return 订单提交VO
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO orderSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 客户催单
     * @param id 订单id
     */
    void reminder(Long id);

    String queryLatestOrderSummary();

    String queryOrderSummary(Long id);

    String remindOrderForAi(Long id);

    String cancelOrderForAi(Long id, String reason);
}
