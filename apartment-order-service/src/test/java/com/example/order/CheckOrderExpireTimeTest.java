package com.example.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class CheckOrderExpireTimeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void checkOrders() throws Exception {
        System.out.println("=== 检查订单超时时间 ===");

        // 获取MySQL当前时间
        Map<String, Object> timeInfo = jdbcTemplate.queryForMap(
                "SELECT NOW() as db_now, @@global.time_zone as global_tz, @@session.time_zone as session_tz");
        System.out.println("MySQL NOW(): " + timeInfo.get("db_now"));
        System.out.println("MySQL全局时区: " + timeInfo.get("global_tz"));
        System.out.println("MySQL会话时区: " + timeInfo.get("session_tz"));
        System.out.println("Java当前时间: " + java.time.LocalDateTime.now());

        // 获取最近的订单
        List<Map<String, Object>> orders = jdbcTemplate.queryForList(
                "SELECT order_no, order_status, expire_time, create_time FROM rental_order ORDER BY create_time DESC LIMIT 5");

        StringBuilder sb = new StringBuilder();
        sb.append("=== 最近5个订单 ===\n");
        for (Map<String, Object> order : orders) {
            sb.append("订单号: ").append(order.get("order_no"))
                    .append(", 状态: ").append(order.get("order_status"))
                    .append(", 过期时间: ").append(order.get("expire_time"))
                    .append(", 创建时间: ").append(order.get("create_time"))
                    .append("\n");
        }
        System.out.println(sb.toString());

        // 写入文件
        java.nio.file.Files.write(
                java.nio.file.Paths.get("order_check_result.txt"),
                sb.toString().getBytes());
    }
}
