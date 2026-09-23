package com.example.notification.mq;

import com.example.common.exception.BusinessException;
import com.example.notification.dto.SendNotificationDTO;
import com.example.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 通知消息监听器
 * 监听来自其他服务(订单、支付、合同)的通知请求
 */
@Component
@Slf4j
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "NOTIFICATION_TOPIC", // 主题
        consumerGroup = "notification-consumer-group" // 消费者组
)
public class NotificationMessageListener implements RocketMQListener<SendNotificationDTO> {

    private final NotificationService notificationService;

    @Override
    public void onMessage(SendNotificationDTO dto) {
        try {
            log.info("收到通知消息: userId={}, templateCode={}",
                    dto.getUserId(), dto.getTemplateCode());

            // 发送通知
            Long messageId = notificationService.sendNotification(dto);

            log.info("通知消息处理成功: messageId={}", messageId);
        } catch (Exception e) {
            log.error("处理通知消息失败", e);
            // 这里可以添加重试逻辑或发送到死信队列
            throw new BusinessException("处理通知消息失败: " + e.getMessage());
        }
    }
}
