package com.example.order;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 创建测试用超时订单
 */
@Slf4j
@SpringBootTest
public class CreateExpiredOrderTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void createExpiredOrder() {
        System.out.println("\n========== 创建测试用超时订单 ==========\n");

        // 创建一个5分钟前就已经过期的订单
        String sql = "INSERT INTO rental_order " +
                "(order_no, tenant_id, house_id, landlord_id, " +
                "rent_start_date, rent_end_date, rent_months, " +
                "monthly_rent, deposit, total_amount, first_payment_amount, " +
                "installment_enabled, order_status, payment_status, expire_time, create_time) " +
                "VALUES " +
                "(CONCAT('TEST_EXPIRED_', UNIX_TIMESTAMP()), 1, 1, 1, " +
                "CURDATE(), DATE_ADD(CURDATE(), INTERVAL 12 MONTH), 12, " +
                "3000.00, 3000.00, 39000.00, 6000.00, " +
                "0, 'PENDING_PAYMENT', 'UNPAID', DATE_SUB(NOW(), INTERVAL 5 MINUTE), NOW())";

        try {
            jdbcTemplate.execute(sql);
            System.out.println("✅ 成功创建超时测试订单");
            System.out.println("   - 订单状态: PENDING_PAYMENT");
            System.out.println("   - 过期时间: 5分钟前");
            System.out.println("");

            // 查询刚创建的订单
            String query = "SELECT order_no, expire_time, " +
                    "TIMESTAMPDIFF(MINUTE, expire_time, NOW()) as minutes_overdue " +
                    "FROM rental_order " +
                    "WHERE order_no LIKE 'TEST_EXPIRED_%' " +
                    "ORDER BY create_time DESC LIMIT 1";

            jdbcTemplate.queryForMap(query).forEach((k, v) -> {
                System.out.println("   " + k + ": " + v);
            });

            System.out.println("");
            System.out.println("📋 下一步验证：");
            System.out.println("   1. 等待1分钟，让定时任务执行");
            System.out.println("   2. 查看 Order Service 控制台是否有日志：");
            System.out.println("      - '开始执行订单超时取消任务'");
            System.out.println("      - '订单超时自动取消成功'");
            System.out.println("   3. 运行 mvn test -Dtest=SimpleOrderTimeoutCheck 查看订单是否被取消");

        } catch (Exception e) {
            System.err.println("❌ 创建测试订单失败: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n========== 创建完成 ==========\n");
    }
}
