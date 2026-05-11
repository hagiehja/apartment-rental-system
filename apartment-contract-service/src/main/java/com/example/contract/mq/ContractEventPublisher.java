package com.example.contract.mq;

import com.example.contract.dto.ContractRefundEvent;
import com.example.contract.dto.ContractSignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractEventPublisher {

    private static final String TOPIC_CONTRACT_SIGNED = "TOPIC_CONTRACT_SIGNED";
    private static final String TOPIC_CONTRACT_REFUND = "TOPIC_CONTRACT_REFUND";

    private final RocketMQTemplate rocketMQTemplate;

    public void publishSigned(Long contractId, Long userId, String userType) {
        rocketMQTemplate.convertAndSend(
                TOPIC_CONTRACT_SIGNED,
                new ContractSignedEvent(contractId, userId, userType));
        log.info("合同签署消息已发送到 RocketMQ: contractId={}, userId={}", contractId, userId);
    }

    public void publishRefund(Long contractId, Long userId, BigDecimal refundAmount, String reason) {
        rocketMQTemplate.convertAndSend(
                TOPIC_CONTRACT_REFUND,
                new ContractRefundEvent(contractId, userId, refundAmount, reason));
        log.info("合同退租消息已发送到 RocketMQ: contractId={}, userId={}", contractId, userId);
    }
}
