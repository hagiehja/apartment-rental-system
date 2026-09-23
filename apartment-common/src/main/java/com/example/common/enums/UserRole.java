package com.example.common.enums;

/**
 * 用户角色枚举
 * <p>
 * 枚举名 {@code name()} 与数据库 {@code user.role} 列存储值完全一致,
 * 保证兼容历史数据(列值即为枚举名)。
 */
public enum UserRole {
    /** 租客 */
    TENANT,
    /** 房东 */
    LANDLORD,
    /** 管理员 */
    ADMIN;

    public static UserRole of(String value) {
        if (value == null) return null;
        try {
            return UserRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
