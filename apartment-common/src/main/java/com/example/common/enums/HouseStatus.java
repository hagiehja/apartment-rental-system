package com.example.common.enums;

/**
 * 房源状态枚举
 * <p>
 * 枚举名与数据库 {@code house.status} 列存储值完全一致。
 */
public enum HouseStatus {
    /** 可租赁 */
    AVAILABLE,
    /** 已租赁 */
    RENTED,
    /** 已下架 */
    OFFLINE;

    public static HouseStatus of(String value) {
        if (value == null) return null;
        try {
            return HouseStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
