package com.example.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class DebugOrderTimeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void debugOrderTime() throws Exception {
        System.out.println("========== 订单时间调试 ==========");

        // 1. 获取MySQL当前时间
        Map<String, Object> dbTime = jdbcTemplate.queryForMap(
                "SELECT NOW() as db_now, CURRENT_TIMESTAMP as db_timestamp");
        System.out.println("MySQL NOW(): " + dbTime.get("db_now"));
        System.out.println("Java LocalDateTime.now(): " + java.time.LocalDateTime.now());

        // 2. 查询最新的订单
        List<Map<String, Object>> orders = jdbcTemplate.queryForList(
                "SELECT order_no, order_status, expire_time, create_time, " +
                        "CASE WHEN expire_time < NOW() THEN 'YES_EXPIRED' ELSE 'NOT_EXPIRED' END as is_expired, " +
                        "TIMESTAMPDIFF(MINUTE, NOW(), expire_time) as minutes_until_expire " +
                        "FROM rental_order ORDER BY create_time DESC LIMIT 5");

        System.out.println("\n========== 最近5个订单 ==========");
        for (Map<String, Object> order : orders) {
            System.out.println("订单号: " + order.get("order_no"));
            System.out.println("  状态: " + order.get("order_status"));
            System.out.println("  创建时间: " + order.get("create_time"));
            System.out.println("  过期时间: " + order.get("expire_time"));
            System.out.println("  是否已过期: " + order.get("is_expired"));
            System.out.println("  距过期分钟: " + order.get("minutes_until_expire"));
            System.out.println();
        }

        // 3. 使用与定时任务相同的SQL查询
        List<Map<String, Object>> expiredOrders = jdbcTemplate.queryForList(
                "SELECT order_no, order_status, expire_time FROM rental_order " +
                        "WHERE order_status = 'PENDING_PAYMENT' AND expire_time < NOW() LIMIT 10");

        System.out.println("========== 符合超时条件的订单 ==========");
        System.out.println("数量: " + expiredOrders.size());
        for (Map<String, Object> order : expiredOrders) {
            System.out.println("  " + order.get("order_no") + " | " + order.get("expire_time"));
        }
    }
}
