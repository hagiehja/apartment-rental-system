package com.example.order;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * 简化版订单超时检查
 */
@Slf4j
@SpringBootTest
public class SimpleOrderTimeoutCheck {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void checkOrderTimeout() {
        System.out.println("\n========== 订单超时检查 ==========\n");

        // 1. 数据库当前时间
        String dbTime = jdbcTemplate.queryForObject("SELECT NOW()", String.class);
        System.out.println("数据库当前时间: " + dbTime);

        // 2. 待支付订单总数
        Integer pendingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rental_order WHERE order_status = 'PENDING_PAYMENT'",
                Integer.class);
        System.out.println("待支付订单总数: " + pendingCount);

        // 3. 超时订单数量
        Integer expiredCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rental_order WHERE order_status = 'PENDING_PAYMENT' AND expire_time < NOW()",
                Integer.class);
        System.out.println("超时订单数量: " + expiredCount);

        // 4. 显示所有待支付订单详情
        if (pendingCount > 0) {
            System.out.println("\n--- 待支付订单详情 ---");
            String sql = "SELECT order_no, expire_time, " +
                    "TIMESTAMPDIFF(MINUTE, NOW(), expire_time) as minutes_left, " +
                    "CASE WHEN expire_time < NOW() THEN '已超时' ELSE '未超时' END as timeout_status " +
                    "FROM rental_order " +
                    "WHERE order_status = 'PENDING_PAYMENT' " +
                    "ORDER BY expire_time";

            List<Map<String, Object>> orders = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> order : orders) {
                System.out.println(String.format("订单号: %s, 过期时间: %s, 剩余分钟: %s, 状态: %s",
                        order.get("order_no"),
                        order.get("expire_time"),
                        order.get("minutes_left"),
                        order.get("timeout_status")));
            }
        }

        // 5. 检查最近取消的订单 (验证自动取消是否生效)
        System.out.println("\n--- 最近取消的订单 (验证定时任务) ---");
        String cancelledSql = "SELECT order_no, order_status, update_time, remark " +
                "FROM rental_order " +
                "WHERE order_status = 'CANCELLED' " +
                "ORDER BY update_time DESC LIMIT 5";
        List<Map<String, Object>> cancelledOrders = jdbcTemplate.queryForList(cancelledSql);
        if (cancelledOrders.isEmpty()) {
            System.out.println("没有找到最近取消的订单");
        } else {
            for (Map<String, Object> order : cancelledOrders) {
                System.out.println(String.format("订单号: %s, 状态: %s, 更新时间: %s, 备注: %s",
                        order.get("order_no"),
                        order.get("order_status"),
                        order.get("update_time"),
                        order.get("remark")));
            }
        }

        System.out.println("\n========== 检查完成 ==========\n");
    }
}
