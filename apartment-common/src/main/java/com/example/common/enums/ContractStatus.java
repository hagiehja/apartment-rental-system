package com.example.common.enums;

/**
 * 合同状态枚举
 * <p>
 * 枚举名与数据库 {@code contract.status} 列存储值完全一致。
 */
public enum ContractStatus {
    /** 待签约 */
    PENDING,
    /** 房东已签 */
    LANDLORD_SIGNED,
    /** 租客已签 */
    TENANT_SIGNED,
    /** 双方已签(生效中) */
    SIGNED,
    /** 已取消 */
    CANCELLED,
    /** 已到期 */
    EXPIRED;

    public static ContractStatus of(String value) {
        if (value == null) return null;
        try {
            return ContractStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
