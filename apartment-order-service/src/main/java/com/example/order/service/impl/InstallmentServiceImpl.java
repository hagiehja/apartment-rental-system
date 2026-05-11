package com.example.order.service.impl;

import com.example.order.dto.InstallmentDTO;
import com.example.order.entity.InstallmentPlan;
import com.example.order.mapper.InstallmentPlanMapper;
import com.example.order.service.InstallmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分期服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstallmentServiceImpl implements InstallmentService {

    private final InstallmentPlanMapper installmentPlanMapper;

    @Override
    @Transactional
    public List<InstallmentPlan> generateInstallmentPlan(
            String orderNo,
            BigDecimal monthlyRent,
            BigDecimal deposit,
            Integer rentMonths,
            LocalDate rentStartDate) {

        List<InstallmentPlan> planList = new ArrayList<>();

        // 第1期：首付 = 押金 + 首月租金
        InstallmentPlan firstPeriod = new InstallmentPlan();
        firstPeriod.setOrderNo(orderNo);
        firstPeriod.setPeriodNo(1);
        firstPeriod.setAmount(deposit.add(monthlyRent));
        firstPeriod.setDueDate(rentStartDate);
        firstPeriod.setPaymentStatus("UNPAID");
        installmentPlanMapper.insert(firstPeriod);
        planList.add(firstPeriod);

        // 第2期开始：每月租金
        for (int i = 2; i <= rentMonths; i++) {
            InstallmentPlan period = new InstallmentPlan();
            period.setOrderNo(orderNo);
            period.setPeriodNo(i);
            period.setAmount(monthlyRent);
            // 应付日期 = 租期开始日期 + (期数-1)个月
            period.setDueDate(rentStartDate.plusMonths(i - 1));
            period.setPaymentStatus("UNPAID");
            installmentPlanMapper.insert(period);
            planList.add(period);
        }

        log.info("生成分期付款计划成功，orderNo={}, 总期数={}", orderNo, planList.size());
        return planList;
    }

    @Override
    public InstallmentPlan getNextUnpaidInstallment(String orderNo) {
        return installmentPlanMapper.selectOneUnpaidByOrderNo(orderNo);
    }

    @Override
    @Transactional
    public void updateInstallmentPaid(Long installmentId, String paymentNo) {
        InstallmentPlan plan = installmentPlanMapper.selectById(installmentId);
        if (plan != null) {
            plan.setPaymentStatus("PAID");
            plan.setPaymentNo(paymentNo);
            plan.setPaymentTime(LocalDateTime.now());
            installmentPlanMapper.updateById(plan);
            log.info("分期付款已支付，installmentId={}, paymentNo={}", installmentId, paymentNo);
        }
    }

    @Override
    public List<InstallmentDTO> getInstallmentList(String orderNo) {
        List<InstallmentPlan> planList = installmentPlanMapper.selectListByOrderNo(orderNo);
        return planList.stream().map(plan -> {
            InstallmentDTO dto = new InstallmentDTO();
            BeanUtils.copyProperties(plan, dto);
            return dto;
        }).collect(Collectors.toList());
    }
}
