package com.example.contract.mq;

import com.example.contract.dto.ContractRefundEvent;
import com.example.contract.dto.ContractSignedEvent;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ContractEventPublisherTest {

    @Test
    void publishSignedSendsSignedEventToRocketMq() {
        RocketMQTemplate rocketMQTemplate = mock(RocketMQTemplate.class);
        ContractEventPublisher publisher = new ContractEventPublisher(rocketMQTemplate);

        publisher.publishSigned(7L, 11L, "TENANT");

        ArgumentCaptor<ContractSignedEvent> captor = ArgumentCaptor.forClass(ContractSignedEvent.class);
        verify(rocketMQTemplate).convertAndSend(eq("TOPIC_CONTRACT_SIGNED"), captor.capture());
        assertThat(captor.getValue().getContractId()).isEqualTo(7L);
        assertThat(captor.getValue().getUserId()).isEqualTo(11L);
        assertThat(captor.getValue().getUserType()).isEqualTo("TENANT");
    }

    @Test
    void publishRefundSendsRefundEventToRocketMq() {
        RocketMQTemplate rocketMQTemplate = mock(RocketMQTemplate.class);
        ContractEventPublisher publisher = new ContractEventPublisher(rocketMQTemplate);

        publisher.publishRefund(7L, 11L, new BigDecimal("800.00"), "退租");

        ArgumentCaptor<ContractRefundEvent> captor = ArgumentCaptor.forClass(ContractRefundEvent.class);
        verify(rocketMQTemplate).convertAndSend(eq("TOPIC_CONTRACT_REFUND"), captor.capture());
        assertThat(captor.getValue().getContractId()).isEqualTo(7L);
        assertThat(captor.getValue().getUserId()).isEqualTo(11L);
        assertThat(captor.getValue().getRefundAmount()).isEqualByComparingTo("800.00");
        assertThat(captor.getValue().getReason()).isEqualTo("退租");
    }
}
