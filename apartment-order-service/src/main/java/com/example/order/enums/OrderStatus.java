package com.example.order.enums;

/**
 * 订单状态枚举
 */
public enum OrderStatus {
    PENDING_PAYMENT("待支付"),
    PAID("已支付"),
    RENTING("租赁中"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    REFUNDED("已退款");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 验证状态流转是否合法
     */
    public boolean canTransitionTo(OrderStatus target) {
        switch (this) {
            case PENDING_PAYMENT:
                return target == PAID || target == CANCELLED;
            case PAID:
                return target == RENTING || target == REFUNDED;
            case RENTING:
                return target == COMPLETED;
            default:
                return false;
        }
    }
}
