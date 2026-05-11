package com.example.order;

import com.example.order.entity.RentalOrder;
import com.example.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单超时识别诊断测试
 * 用于排查订单超时自动取消功能是否正常工作
 */
@Slf4j
@SpringBootTest
public class OrderTimeoutDiagnosticTest {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testDatabaseTime() {
        log.info("========== 测试1: 数据库时间检查 ==========");

        // 获取数据库当前时间
        String dbNow = jdbcTemplate.queryForObject("SELECT NOW()", String.class);
        log.info("数据库当前时间 (MySQL NOW()): {}", dbNow);

        // 获取Java当前时间
        LocalDateTime javaNow = LocalDateTime.now();
        log.info("Java当前时间 (LocalDateTime.now()): {}", javaNow);

        // 获取数据库+30分钟时间
        LocalDateTime expireTime = orderMapper.getExpireTime();
        log.info("订单过期时间 (DB NOW() + 30分钟): {}", expireTime);

        log.info("提示: 如果两个时间相差超过1分钟，说明存在时区配置问题");
    }

    @Test
    public void testExpiredOrdersQuery() {
        log.info("========== 测试2: 查询超时订单SQL ==========");

        // 查询所有待支付订单
        String countPendingSql = "SELECT COUNT(*) FROM rental_order WHERE order_status = 'PENDING_PAYMENT'";
        Integer pendingCount = jdbcTemplate.queryForObject(countPendingSql, Integer.class);
        log.info("待支付订单总数: {}", pendingCount);

        // 查询超时订单
        List<RentalOrder> expiredOrders = orderMapper.selectExpiredPendingOrders();
        log.info("超时未支付订单数量: {}", expiredOrders.size());

        if (!expiredOrders.isEmpty()) {
            log.info("--- 超时订单详情 ---");
            for (RentalOrder order : expiredOrders) {
                log.info("订单号: {}, 过期时间: {}, 状态: {}",
                        order.getOrderNo(),
                        order.getExpireTime(),
                        order.getOrderStatus());
            }
        }

        // 显示所有待支付订单的过期时间对比
        String sql = "SELECT order_no, expire_time, " +
                "TIMESTAMPDIFF(MINUTE, expire_time, NOW()) as minutes_overdue, " +
                "NOW() as current_db_time " +
                "FROM rental_order " +
                "WHERE order_status = 'PENDING_PAYMENT' " +
                "ORDER BY expire_time DESC";

        List<Map<String, Object>> orders = jdbcTemplate.queryForList(sql);
        log.info("--- 所有待支付订单时间分析 ---");
        for (Map<String, Object> row : orders) {
            log.info("订单号: {}, 过期时间: {}, 超时分钟数: {}, 数据库当前时间: {}",
                    row.get("order_no"),
                    row.get("expire_time"),
                    row.get("minutes_overdue"),
                    row.get("current_db_time"));
        }
    }

    @Test
    public void testScheduledTaskStatus() {
        log.info("========== 测试3: 定时任务状态检查 ==========");
        log.info("定时任务配置: @Scheduled(cron = \"0 * * * * ?\") - 每分钟执行一次");
        log.info("请检查控制台日志中是否有以下内容:");
        log.info("  - '开始执行订单超时取消任务'");
        log.info("  - '订单超时取消任务完成，处理订单数=X'");
        log.info("");
        log.info("如果没有看到定时任务日志，请检查:");
        log.info("  1. 主类是否添加了 @EnableScheduling 注解");
        log.info("  2. OrderTimeoutCancelJob 是否被Spring扫描到");
        log.info("  3. 是否有异常阻止了定时任务执行");
    }

    @Test
    public void testCreateTestExpiredOrder() {
        log.info("========== 测试4: 创建测试用过期订单 ==========");

        // 创建一个已过期的测试订单
        String sql = "INSERT INTO rental_order " +
                "(order_no, tenant_id, house_id, landlord_id, " +
                "rent_start_date, rent_end_date, rent_months, " +
                "monthly_rent, deposit, total_amount, first_payment_amount, " +
                "order_status, payment_status, expire_time) " +
                "VALUES " +
                "('TEST_EXPIRED_' || UNIX_TIMESTAMP(), 1, 1, 1, " +
                "CURDATE(), DATE_ADD(CURDATE(), INTERVAL 12 MONTH), 12, " +
                "3000.00, 3000.00, 39000.00, 6000.00, " +
                "'PENDING_PAYMENT', 'UNPAID', DATE_SUB(NOW(), INTERVAL 5 MINUTE))";

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 成功创建过期测试订单（过期时间为5分钟前）");
            log.info("请等待1分钟后检查该订单是否被自动取消");
            log.info("或者手动运行定时任务来验证");
        } catch (Exception e) {
            log.error("❌ 创建测试订单失败: {}", e.getMessage());
        }
    }

    @Test
    public void testFullDiagnostic() {
        log.info("\n");
        log.info("=============================================");
        log.info("         订单超时识别完整诊断报告");
        log.info("=============================================");

        testDatabaseTime();
        log.info("");
        testExpiredOrdersQuery();
        log.info("");
        testScheduledTaskStatus();

        log.info("\n=============================================");
        log.info("诊断完成，请查看上方日志分析问题原因");
        log.info("=============================================");
    }
}
