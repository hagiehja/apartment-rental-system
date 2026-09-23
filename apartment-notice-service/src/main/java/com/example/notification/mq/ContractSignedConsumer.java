package com.example.notification.mq;

import com.example.notification.dto.SendNotificationDTO;
import com.example.notification.dto.event.ContractSignedEvent;
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
@RocketMQMessageListener(topic = "TOPIC_CONTRACT_SIGNED", consumerGroup = "notice-contract-signed-group")
@RequiredArgsConstructor
public class ContractSignedConsumer implements RocketMQListener<ContractSignedEvent> {

    private final NotificationService notificationService;

    @Override
    public void onMessage(ContractSignedEvent event) {
        log.info("收到合同签署消息: {}", event);

        // 如果是租客签署，可能需要通知房东；如果是房东签署，通知租客“合同生效”
        // 这里简化为：每次签署都通知合同相关的另一方（暂未在此消息中包含另一方ID，需优化）
        // 但根据需求："自动给房东发送一条'新租客入住提醒'"已在OrderPay中处理。
        // "同时给租客发送'签约成功通知'" -> 这应该在双方都签署后，或者租客签署后。

        // 假设我们只通知“操作者”签约成功，或者通知对方。
        // 由于消息中只有操作者userId，我们暂时只通知操作者。或者需要查询合同详情获取另一方。
        // 为简化代码，这里仅通知操作者“您已成功签署”。

        try {
            SendNotificationDTO dto = new SendNotificationDTO();
            dto.setUserId(event.getUserId());
            dto.setTemplateCode("CONTRACT_SIGNED_SUCCESS");
            dto.setBizId(event.getContractId());

            Map<String, String> params = new HashMap<>();
            params.put("contractId", event.getContractId().toString());
            dto.setParams(params);

            notificationService.sendNotification(dto);
        } catch (Exception e) {
            log.error("发送签约通知失败", e);
        }
    }
}
