package com.example.order.mq;

import com.example.order.dto.OrderPaySuccessEvent;
import com.example.order.dto.SendNotificationDTO;
import com.example.order.entity.RentalOrder;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderNotificationPublisherTest {

    @Test
    void publishPaymentSuccessSendsOrderEventToRocketMq() {
        RocketMQTemplate rocketMQTemplate = mock(RocketMQTemplate.class);
        OrderNotificationPublisher publisher = new OrderNotificationPublisher(rocketMQTemplate);
        RentalOrder order = new RentalOrder();
        order.setOrderNo("ORD1001");
        order.setTenantId(11L);
        order.setLandlordId(22L);
        order.setHouseId(33L);
        order.setMonthlyRent(new BigDecimal("1800.00"));

        publisher.publishPaymentSuccess(order, "阳光公寓");

        ArgumentCaptor<OrderPaySuccessEvent> captor = ArgumentCaptor.forClass(OrderPaySuccessEvent.class);
        verify(rocketMQTemplate).convertAndSend(eq("TOPIC_ORDER_PAY_SUCCESS"), captor.capture());
        assertThat(captor.getValue().getOrderNo()).isEqualTo("ORD1001");
        assertThat(captor.getValue().getTenantId()).isEqualTo(11L);
        assertThat(captor.getValue().getLandlordId()).isEqualTo(22L);
        assertThat(captor.getValue().getHouseId()).isEqualTo("33");
        assertThat(captor.getValue().getHouseTitle()).isEqualTo("阳光公寓");
    }

    @Test
    void publishNotificationSendsCommonNotificationToRocketMq() {
        RocketMQTemplate rocketMQTemplate = mock(RocketMQTemplate.class);
        OrderNotificationPublisher publisher = new OrderNotificationPublisher(rocketMQTemplate);

        publisher.publishNotification(11L, "CONTRACT_CREATED_NOTICE", 99L, Map.of("contractNo", "99"));

        ArgumentCaptor<SendNotificationDTO> captor = ArgumentCaptor.forClass(SendNotificationDTO.class);
        verify(rocketMQTemplate).convertAndSend(eq("NOTIFICATION_TOPIC"), captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(11L);
        assertThat(captor.getValue().getTemplateCode()).isEqualTo("CONTRACT_CREATED_NOTICE");
        assertThat(captor.getValue().getBizId()).isEqualTo(99L);
        assertThat(captor.getValue().getParams()).containsEntry("contractNo", "99");
    }
}
