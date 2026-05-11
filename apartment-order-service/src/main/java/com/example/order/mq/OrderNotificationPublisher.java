package com.example.order.mq;

import com.example.order.dto.OrderPaySuccessEvent;
import com.example.order.dto.SendNotificationDTO;
import com.example.order.entity.RentalOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationPublisher {

    private static final String TOPIC_ORDER_PAY_SUCCESS = "TOPIC_ORDER_PAY_SUCCESS";
    private static final String NOTIFICATION_TOPIC = "NOTIFICATION_TOPIC";

    private final RocketMQTemplate rocketMQTemplate;

    public void publishPaymentSuccess(RentalOrder order, String houseTitle) {
        OrderPaySuccessEvent event = new OrderPaySuccessEvent(
                order.getOrderNo(),
                order.getTenantId(),
                order.getLandlordId(),
                order.getMonthlyRent(),
                String.valueOf(order.getHouseId()),
                houseTitle == null ? "" : houseTitle);
        rocketMQTemplate.convertAndSend(TOPIC_ORDER_PAY_SUCCESS, event);
        log.info("订单支付成功消息已发送到 RocketMQ: orderNo={}", order.getOrderNo());
    }

    public void publishNotification(Long userId, String templateCode, Long bizId, Map<String, String> params) {
        SendNotificationDTO dto = new SendNotificationDTO(userId, templateCode, params, bizId);
        rocketMQTemplate.convertAndSend(NOTIFICATION_TOPIC, dto);
        log.info("通知消息已发送到 RocketMQ: userId={}, templateCode={}", userId, templateCode);
    }

    public void publishContractCreated(Long tenantId, Long landlordId, String houseTitle, Long contractId) {
        Map<String, String> params = new HashMap<>();
        params.put("houseTitle", houseTitle == null ? "" : houseTitle);
        params.put("contractNo", String.valueOf(contractId));

        publishNotification(tenantId, "CONTRACT_CREATED_NOTICE", contractId, params);
        publishNotification(landlordId, "CONTRACT_CREATED_NOTICE", contractId, params);
    }

    public void publishRefundNotifications(RentalOrder order, String houseTitle) {
        Map<String, String> params = new HashMap<>();
        params.put("orderNo", order.getOrderNo());
        params.put("houseTitle", houseTitle == null ? "" : houseTitle);
        params.put("amount", order.getTotalAmount() == null ? "" : order.getTotalAmount().toPlainString());

        Long bizId = buildOrderBizId(order.getOrderNo());
        publishNotification(order.getLandlordId(), "LANDLORD_REFUND_NOTICE", bizId, params);
        publishNotification(order.getTenantId(), "TENANT_REFUND_SUCCESS", bizId, params);
    }

    private Long buildOrderBizId(String orderNo) {
        return Math.abs((long) orderNo.hashCode());
    }
}
