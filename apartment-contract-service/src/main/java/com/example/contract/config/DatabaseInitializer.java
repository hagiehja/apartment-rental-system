package com.example.contract.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化器
 * 用于自动修复缺失的数据库表和字段
 */
@Slf4j
@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("正在检查数据库Schema...");

        // 1. 创建房东收入表
        createLandlordIncomeTable();

        // 2. 添加 contract 表缺失字段
        addMissingContractColumns();
    }

    private void createLandlordIncomeTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS landlord_income (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    landlord_id BIGINT NOT NULL COMMENT '房东ID',
                    contract_id BIGINT COMMENT '合同ID',
                    order_id BIGINT COMMENT '订单ID',
                    income_type VARCHAR(20) NOT NULL COMMENT '收入类型：RENTAL-租金收入/REFUND-退款扣减',
                    amount DECIMAL(10,2) NOT NULL COMMENT '金额',
                    total_payment DECIMAL(10,2) COMMENT '租客支付总额',
                    deposit_amount DECIMAL(10,2) COMMENT '押金金额',
                    commission_rate DECIMAL(5,2) DEFAULT 0.80 COMMENT '分成比例',
                    remark VARCHAR(500) COMMENT '备注',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_landlord (landlord_id),
                    INDEX idx_contract (contract_id)
                ) COMMENT '房东收入记录表';
                """;
        try {
            jdbcTemplate.execute(sql);
            log.info("检查/创建 landlord_income 表完成");
        } catch (Exception e) {
            log.error("创建 landlord_income 表失败", e);
        }
    }

    private void addMissingContractColumns() {
        if (!isColumnExists("contract", "actual_check_in_date")) {
            log.info("检测到 contract 表缺失退租相关字段，正在添加...");
            try {
                // MySQL 8.0+ 支持 ADD COLUMN ...
                // 注意：如果 end_date 不存在会报错，但它是核心字段应该存在
                jdbcTemplate.execute(
                        "ALTER TABLE contract ADD COLUMN actual_check_in_date DATE COMMENT '实际入住日期' AFTER end_date");
                jdbcTemplate.execute(
                        "ALTER TABLE contract ADD COLUMN actual_check_out_date DATE COMMENT '实际退租日期' AFTER actual_check_in_date");
                jdbcTemplate.execute(
                        "ALTER TABLE contract ADD COLUMN termination_reason VARCHAR(500) COMMENT '退租原因' AFTER actual_check_out_date");
                jdbcTemplate.execute(
                        "ALTER TABLE contract ADD COLUMN refund_amount DECIMAL(10,2) COMMENT '退款金额' AFTER termination_reason");
                jdbcTemplate.execute(
                        "ALTER TABLE contract ADD COLUMN landlord_income DECIMAL(10,2) COMMENT '房东收入金额' AFTER refund_amount");
                log.info("成功添加 contract 表缺失字段");
            } catch (Exception e) {
                log.error("添加 contract 表字段失败", e);
            }
        } else {
            log.info("contract 表字段检查通过");
        }
    }

    private boolean isColumnExists(String tableName, String columnName) {
        try {
            String sql = "SELECT count(*) FROM information_schema.columns WHERE table_name = ? AND column_name = ? AND table_schema = DATABASE()";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("检查字段是否存在失败: {}", e.getMessage());
            return false;
        }
    }
}
