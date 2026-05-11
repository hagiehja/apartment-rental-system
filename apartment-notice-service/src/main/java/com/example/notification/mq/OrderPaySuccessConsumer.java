package com.example.notification.mq;

import com.example.notification.dto.SendNotificationDTO;
import com.example.notification.dto.event.OrderPaySuccessEvent;
import com.example.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RocketMQMessageListener(topic = "TOPIC_ORDER_PAY_SUCCESS", consumerGroup = "notice-order-pay-success-group")
@RequiredArgsConstructor
public class OrderPaySuccessConsumer implements RocketMQListener<OrderPaySuccessEvent> {

    private final NotificationService notificationService;

    @Override
    public void onMessage(OrderPaySuccessEvent event) {
        log.info("收到订单支付成功消息: orderNo={}, tenantId={}, landlordId={}",
                event.getOrderNo(), event.getTenantId(), event.getLandlordId());

        // 1. 通知租客：支付成功及租赁确认
        sendToTenant(event);

        // 2. 通知房东：房源已被成功租赁且租金已到账
        sendToLandlord(event);
    }

    /**
     * 向租客发送"支付成功，租赁确认"通知
     */
    private void sendToTenant(OrderPaySuccessEvent event) {
        try {
            SendNotificationDTO dto = new SendNotificationDTO();
            dto.setUserId(event.getTenantId());
            // 使用数据库中已存在的模板
            dto.setTemplateCode("TENANT_PAY_SUCCESS");
            dto.setBizId(Long.valueOf(Math.abs(event.getOrderNo().hashCode())));

            Map<String, String> params = new HashMap<>();
            params.put("orderNo", event.getOrderNo());
            // houseTitle 由 order-service 在事件中传入，展示具体房源名称
            params.put("houseTitle", event.getHouseTitle() != null ? event.getHouseTitle() : "");
            params.put("amount", event.getAmount() != null ? event.getAmount().toPlainString() : "");
            dto.setParams(params);

            notificationService.sendNotification(dto);
            log.info("租客支付成功通知已发送: tenantId={}, orderNo={}",
                    event.getTenantId(), event.getOrderNo());
        } catch (Exception e) {
            log.error("发送租客支付通知失败: orderNo={}", event.getOrderNo(), e);
        }
    }

    /**
     * 向房东发送"租金已到账"通知
     */
    private void sendToLandlord(OrderPaySuccessEvent event) {
        try {
            SendNotificationDTO dto = new SendNotificationDTO();
            dto.setUserId(event.getLandlordId());
            // 使用数据库中已存在的模板
            dto.setTemplateCode("LANDLORD_RENT_RECEIVED");
            dto.setBizId(Long.valueOf(Math.abs(event.getOrderNo().hashCode())));

            Map<String, String> params = new HashMap<>();
            params.put("orderNo", event.getOrderNo());
            params.put("houseTitle", event.getHouseTitle() != null ? event.getHouseTitle() : "");
            params.put("amount", event.getAmount() != null ? event.getAmount().toPlainString() : "");
            dto.setParams(params);

            notificationService.sendNotification(dto);
            log.info("房东租金到账通知已发送: landlordId={}, orderNo={}",
                    event.getLandlordId(), event.getOrderNo());
        } catch (Exception e) {
            log.error("发送房东到账通知失败: orderNo={}", event.getOrderNo(), e);
        }
    }
}
