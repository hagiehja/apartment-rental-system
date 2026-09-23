package com.example.notification.mq;

import com.example.notification.dto.SendNotificationDTO;
import com.example.notification.dto.event.ContractRefundEvent;
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
@RocketMQMessageListener(topic = "TOPIC_CONTRACT_REFUND", consumerGroup = "notice-contract-refund-group")
@RequiredArgsConstructor
public class ContractRefundConsumer implements RocketMQListener<ContractRefundEvent> {

    private final NotificationService notificationService;

    @Override
    public void onMessage(ContractRefundEvent event) {
        log.info("收到退租退款消息: {}", event);

        // 给租客发送消息
        try {
            SendNotificationDTO dto = new SendNotificationDTO();
            dto.setUserId(event.getUserId());
            dto.setTemplateCode("CONTRACT_REFUND_NOTICE");
            dto.setBizId(event.getContractId());

            Map<String, String> params = new HashMap<>();
            params.put("amount", event.getRefundAmount().toString());
            params.put("reason", event.getReason() != null ? event.getReason() : "无");
            dto.setParams(params);

            notificationService.sendNotification(dto);
        } catch (Exception e) {
            log.error("发送退款通知失败", e);
        }
    }
}
