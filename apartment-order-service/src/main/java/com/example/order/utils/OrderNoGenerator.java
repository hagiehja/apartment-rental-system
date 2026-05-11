package com.example.order.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单号生成器
 */
public class OrderNoGenerator {

    /**
     * 生成订单号：ORD + 时间戳(14位) + 随机数(6位)
     * 示例：ORD20260125233000123456
     */
    public static String generateOrderNo() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d",
                ThreadLocalRandom.current().nextInt(1000000));
        return "ORD" + timestamp + random;
    }
}
