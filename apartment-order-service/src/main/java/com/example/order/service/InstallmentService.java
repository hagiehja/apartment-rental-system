package com.example.order.service;

import com.example.order.dto.InstallmentDTO;
import com.example.order.entity.InstallmentPlan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 分期服务接口
 */
public interface InstallmentService {

    /**
     * 生成分期付款计划
     */
    List<InstallmentPlan> generateInstallmentPlan(
            String orderNo,
            BigDecimal monthlyRent,
            BigDecimal deposit,
            Integer rentMonths,
            LocalDate rentStartDate);

    /**
     * 获取订单的下一期待支付账单
     */
    InstallmentPlan getNextUnpaidInstallment(String orderNo);

    /**
     * 更新分期为已支付
     */
    void updateInstallmentPaid(Long installmentId, String paymentNo);

    /**
     * 查询订单的所有分期计划
     */
    List<InstallmentDTO> getInstallmentList(String orderNo);
}
