package com.example.payment;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@SpringBootTest
public class PaymentDiagnosticTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void checkTables() {
        log.info("Checking tables...");
        try {
            Integer paymentCount = jdbcTemplate.queryForObject("SELECT count(*) FROM payment", Integer.class);
            log.info("✅ Table 'payment' exists. Count: {}", paymentCount);
        } catch (Exception e) {
            log.error("❌ Table 'payment' MISSING or Error: {}", e.getMessage());
        }

        try {
            Integer accountCount = jdbcTemplate.queryForObject("SELECT count(*) FROM user_account", Integer.class);
            log.info("✅ Table 'user_account' exists. Count: {}", accountCount);
        } catch (Exception e) {
            log.error("❌ Table 'user_account' MISSING or Error: {}", e.getMessage());
        }

        try {
            Integer txCount = jdbcTemplate.queryForObject("SELECT count(*) FROM account_transaction", Integer.class);
            log.info("✅ Table 'account_transaction' exists. Count: {}", txCount);
        } catch (Exception e) {
            log.error("❌ Table 'account_transaction' MISSING or Error: {}", e.getMessage());
        }

        try {
            Integer orderCount = jdbcTemplate.queryForObject("SELECT count(*) FROM rental_order", Integer.class);
            log.info("✅ Table 'rental_order' exists. Count: {}", orderCount);
        } catch (Exception e) {
            log.error("❌ Table 'rental_order' MISSING or Error: {}", e.getMessage());
        }
    }
}
