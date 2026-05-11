package com.example.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class CheckRecentOrdersTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void checkRecentOrders() {
        System.out.println("========== 查询所有订单 ==========");

        // 获取当前时间
        Map<String, Object> now = jdbcTemplate.queryForMap("SELECT NOW() as db_now");
        System.out.println("MySQL当前时间: " + now.get("db_now"));
        System.out.println("Java当前时间: " + java.time.LocalDateTime.now());

        // 查询所有订单（按创建时间倒序）
        List<Map<String, Object>> orders = jdbcTemplate.queryForList(
                "SELECT order_no, order_status, payment_status, expire_time, create_time, cancel_reason " +
                        "FROM rental_order ORDER BY create_time DESC");

        System.out.println("\n总订单数: " + orders.size());
        System.out.println("--------------------");

        for (Map<String, Object> order : orders) {
            System.out.println("订单号: " + order.get("order_no"));
            System.out.println("  订单状态: " + order.get("order_status"));
            System.out.println("  支付状态: " + order.get("payment_status"));
            System.out.println("  创建时间: " + order.get("create_time"));
            System.out.println("  过期时间: " + order.get("expire_time"));
            System.out.println("  取消原因: " + order.get("cancel_reason"));
            System.out.println("--------------------");
        }

        // 特别检查今天创建的订单
        List<Map<String, Object>> todayOrders = jdbcTemplate.queryForList(
                "SELECT order_no, order_status, create_time FROM rental_order " +
                        "WHERE DATE(create_time) = CURDATE()");

        System.out.println("\n========== 今天创建的订单 ==========");
        System.out.println("数量: " + todayOrders.size());
        for (Map<String, Object> order : todayOrders) {
            System.out.println("  " + order.get("order_no") + " | " + order.get("order_status") + " | "
                    + order.get("create_time"));
        }
    }
}
