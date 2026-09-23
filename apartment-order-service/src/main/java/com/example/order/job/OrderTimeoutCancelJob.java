package com.example.order.job;

import com.example.order.entity.RentalOrder;
import com.example.order.mapper.OrderMapper;
import com.example.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单超时自动取消定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutCancelJob {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    /**
     * 每分钟执行一次，扫描超时未支付订单
     * 临时禁用：正在调试时区问题
     */
    // @Scheduled(cron = "0 * * * * ?")
    public void cancelExpiredOrders() {
        log.info("开始执行订单超时取消任务");

        try {
            // 查询所有超时未支付订单（使用数据库时间确保时区一致）
            List<RentalOrder> expiredOrders = orderMapper.selectExpiredPendingOrders();

            for (RentalOrder order : expiredOrders) {
                try {
                    // 取消订单
                    orderService.cancelOrder(
                            order.getOrderNo(),
                            "订单超时自动取消（30分钟未支付）",
                            order.getTenantId());

                    log.info("订单超时自动取消成功，orderNo={}", order.getOrderNo());
                } catch (Exception e) {
                    log.error("订单超时取消失败，orderNo={}", order.getOrderNo(), e);
                }
            }

            log.info("订单超时取消任务完成，处理订单数={}", expiredOrders.size());
        } catch (Exception e) {
            log.error("订单超时取消任务执行异常", e);
        }
    }
}
