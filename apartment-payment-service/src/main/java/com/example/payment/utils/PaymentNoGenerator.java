package com.example.payment.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 支付单号生成器
 */
public class PaymentNoGenerator {

    /**
     * 生成支付单号：PAY + 时间戳(14位) + 随机数(6位)
     * 示例：PAY20260125235000123456
     */
    public static String generatePaymentNo() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d",
                ThreadLocalRandom.current().nextInt(1000000));
        return "PAY" + timestamp + random;
    }

    /**
     * 生成流水号：TXN + 时间戳(14位) + 随机数(6位)
     * 示例：TXN20260125235000123456
     */
    public static String generateTransactionNo() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d",
                ThreadLocalRandom.current().nextInt(1000000));
        return "TXN" + timestamp + random;
    }
}
