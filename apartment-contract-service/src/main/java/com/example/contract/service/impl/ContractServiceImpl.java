package com.example.contract.service.impl;

import com.example.common.exception.BusinessException;
import com.example.common.enums.UserRole;
import com.example.common.enums.ContractStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.contract.dto.SignContractDTO;
import com.example.contract.entity.Contract;
import com.example.contract.entity.ContractSignature;
import com.example.contract.mapper.ContractMapper;
import com.example.contract.mapper.ContractSignatureMapper;
import com.example.contract.service.ContractService;
import com.example.contract.service.PdfGeneratorService;
import com.example.contract.feign.OrderFeignClient;
import com.example.contract.feign.PaymentFeignClient;
import com.example.common.api.Result;
import com.example.contract.mq.ContractEventPublisher;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 合同服务业务逻辑实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractMapper contractMapper;
    private final ContractSignatureMapper signatureMapper;
    private final PdfGeneratorService pdfGeneratorService;
    private final OrderFeignClient orderFeignClient;
    private final PaymentFeignClient paymentFeignClient;
    private final ContractEventPublisher contractEventPublisher;
    private final com.example.contract.mapper.LandlordIncomeMapper landlordIncomeMapper;

    // 简单的对象锁，用于替代分布式锁
    private final Object contractGenerateLock = new Object();
    private final Object contractSignLock = new Object();
    private final Object contractTerminateLock = new Object();

    @Value("${contract.file.base-path}")
    private String fileBasePath;

    /**
     * 根据订单生成合同
     * 
     * @param contract 合同信息(从订单服务获取)
     * @return 合同ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateContract(Contract contract) {
        log.info("开始生成合同: orderId={}", contract.getOrderId());

        // 使用简单的同步锁防止同一订单重复生成合同
        synchronized (contractGenerateLock) {
            // 1. 检查订单是否已生成合同
            LambdaQueryWrapper<Contract> query = new LambdaQueryWrapper<>();
            query.eq(Contract::getOrderId, contract.getOrderId());
            Long count = contractMapper.selectCount(query);

            if (count > 0) {
                throw new BusinessException("该订单已生成合同");
            }

            // 2. 生成合同编号
            String contractNo = generateContractNo();
            contract.setContractNo(contractNo);
            contract.setStatus("PENDING_SIGN");
            contract.setCreatedAt(LocalDateTime.now());
            contract.setUpdatedAt(LocalDateTime.now());

            // 3. 保存合同记录
            contractMapper.insert(contract);

            // 4. 生成PDF文件
            try {
                String pdfPath = pdfGeneratorService.generateContractPdf(contract);

                // 更新PDF路径
                LambdaUpdateWrapper<Contract> update = new LambdaUpdateWrapper<>();
                update.eq(Contract::getId, contract.getId())
                        .set(Contract::getFilePath, pdfPath);
                contractMapper.update(null, update);

                log.info("合同生成成功: contractId={}, contractNo={}",
                        contract.getId(), contractNo);
            } catch (Exception e) {
                log.error("生成合同PDF失败", e);
                throw new BusinessException("生成合同PDF失败: " + e.getMessage());
            }

            return contract.getId();
        }
    }

    /**
     * 签署合同
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signContract(Long contractId, SignContractDTO dto) {
        log.info("开始签署合同: contractId={}, userType={}", contractId, dto.getUserType());

        // 使用简单的同步锁防止合同被并发签署
        synchronized (contractSignLock) {
            // 1. 查询合同
            Contract contract = contractMapper.selectById(contractId);
            if (contract == null) {
                throw new BusinessException("合同不存在");
            }

            // 2. 验证用户权限
            if (UserRole.LANDLORD.name().equals(dto.getUserType())) {
                if (!contract.getLandlordId().equals(dto.getUserId())) {
                    throw new BusinessException("您不是该合同的房东");
                }
                if (contract.getLandlordSignedAt() != null) {
                    throw new BusinessException("您已签署该合同");
                }
            } else if (UserRole.TENANT.name().equals(dto.getUserType())) {
                if (!contract.getTenantId().equals(dto.getUserId())) {
                    throw new BusinessException("您不是该合同的租客");
                }
                if (contract.getTenantSignedAt() != null) {
                    throw new BusinessException("您已签署该合同");
                }
            } else {
                throw new BusinessException("无效的用户类型");
            }

            // 3. 记录签署信息
            ContractSignature signature = new ContractSignature();
            signature.setContractId(contractId);
            signature.setUserId(dto.getUserId());
            signature.setUserType(dto.getUserType());
            signature.setSignatureData(dto.getSignatureData());
            signature.setIpAddress(dto.getIpAddress());
            signature.setSignedAt(LocalDateTime.now());
            signatureMapper.insert(signature);

            // 4. 更新合同状态
            LambdaUpdateWrapper<Contract> update = new LambdaUpdateWrapper<>();
            update.eq(Contract::getId, contractId);

            if (UserRole.LANDLORD.name().equals(dto.getUserType())) {
                update.set(Contract::getLandlordSignedAt, LocalDateTime.now());

                // 如果租客已签署,则合同完成
                if (contract.getTenantSignedAt() != null) {
                    update.set(Contract::getStatus, "COMPLETED");
                } else {
                    update.set(Contract::getStatus, ContractStatus.LANDLORD_SIGNED.name());
                }
            } else {
                update.set(Contract::getTenantSignedAt, LocalDateTime.now());

                // 如果房东已签署,则合同完成
                if (contract.getLandlordSignedAt() != null) {
                    update.set(Contract::getStatus, "COMPLETED");
                } else {
                    update.set(Contract::getStatus, ContractStatus.TENANT_SIGNED.name());
                }
            }

            contractMapper.update(null, update);

            log.info("合同签署成功: contractId={}, userType={}", contractId, dto.getUserType());

            // 发送合同签署消息
            try {
                contractEventPublisher.publishSigned(contractId, dto.getUserId(), dto.getUserType());
            } catch (Exception e) {
                log.error("发送合同签署消息失败", e);
            }
        }
    }

    /**
     * 查询合同详情
     */
    @Override
    public Contract getContractById(Long contractId) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }
        return contract;
    }

    /**
     * 根据订单ID查询合同
     */
    @Override
    public Contract getContractByOrderId(Long orderId) {
        LambdaQueryWrapper<Contract> query = new LambdaQueryWrapper<>();
        query.eq(Contract::getOrderId, orderId);
        return contractMapper.selectOne(query);
    }

    /**
     * 查询用户的合同列表
     */
    @Override
    public IPage<Contract> getContractList(
            Long userId,
            String userType,
            String status,
            Integer page,
            Integer size) {

        Page<Contract> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<Contract> query = new LambdaQueryWrapper<>();

        // 根据用户类型过滤
        if (UserRole.LANDLORD.name().equals(userType)) {
            query.eq(Contract::getLandlordId, userId);
        } else if (UserRole.TENANT.name().equals(userType)) {
            query.eq(Contract::getTenantId, userId);
        }

        // 根据状态过滤
        if (status != null && !status.isEmpty()) {
            query.eq(Contract::getStatus, status);
        }

        query.orderByDesc(Contract::getCreatedAt);

        return contractMapper.selectPage(pageParam, query);
    }

    /**
     * 取消合同(订单取消时调用)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelContract(Long orderId) {
        LambdaQueryWrapper<Contract> query = new LambdaQueryWrapper<>();
        query.eq(Contract::getOrderId, orderId)
                .in(Contract::getStatus, ContractStatus.PENDING.name(), ContractStatus.LANDLORD_SIGNED.name(), ContractStatus.TENANT_SIGNED.name());

        LambdaUpdateWrapper<Contract> update = new LambdaUpdateWrapper<>();
        update.eq(Contract::getOrderId, orderId)
                .in(Contract::getStatus, ContractStatus.PENDING.name(), ContractStatus.LANDLORD_SIGNED.name(), ContractStatus.TENANT_SIGNED.name())
                .set(Contract::getStatus, ContractStatus.CANCELLED.name());

        contractMapper.update(null, update);

        log.info("合同已取消: orderId={}", orderId);
    }

    /**
     * 生成合同编号
     * 格式: CON + 年月日 + 时分秒 + 3位随机数
     */
    private String generateContractNo() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 900) + 100;
        return "CON" + timestamp + random;
    }

    /**
     * 申请退租
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public java.math.BigDecimal terminateContract(Long contractId, Long userId, String reason) {
        log.info("开始处理退租申请: contractId={}, userId={}", contractId, userId);

        // 使用简单的同步锁防止并发退租操作
        synchronized (contractTerminateLock) {
            // 1. 查询合同
            Contract contract = contractMapper.selectById(contractId);
            if (contract == null) {
                throw new BusinessException("合同不存在");
            }

            // 2. 验证权限（只有租客可以申请退租）
            if (!contract.getTenantId().equals(userId)) {
                throw new BusinessException("无权操作该合同");
            }

            // 3. 验证合同状态（只有已完成的合同可以退租）
            if (!"COMPLETED".equals(contract.getStatus())) {
                throw new BusinessException("只有已完成的合同才能申请退租");
            }

            // 4. 计算退款金额
            java.math.BigDecimal refundAmount = calculateRefundInternal(contract);

            // 5. 更新合同状态
            contract.setStatus("TERMINATED");
            contract.setActualCheckOutDate(java.time.LocalDate.now());
            contract.setTerminationReason(reason);
            contract.setRefundAmount(refundAmount);
            contract.setUpdatedAt(LocalDateTime.now());

            contractMapper.updateById(contract);

            // 6. 通知订单服务统一处理退款+更新状态+恢复房源状态
            // 注意：由订单服务(updateOrderRefundStatus)统一执行退款，避免重复退款
            try {
                if (contract.getOrderId() != null) {
                    Result<Map<String, Object>> orderRes = orderFeignClient.getOrderDetailById(contract.getOrderId());
                    if (orderRes.getCode() == 200 && orderRes.getData() != null) {
                        String orderNo = (String) orderRes.getData().get("orderNo");
                        if (orderNo != null) {
                            // 由订单服务统一处理：退款（房东扣款→租客退款） + 订单状态更新 + 房源恢复为可租赁
                            orderFeignClient.refundSuccess(orderNo);
                            log.info("退租已通知订单服务处理: orderNo={}", orderNo);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("退租相关订单处理失败: contractId={}", contractId, e);
            }

            log.info("退租申请处理成功: contractId={}, refundAmount={}", contractId, refundAmount);

            // 发送退租消息
            try {
                contractEventPublisher.publishRefund(contractId, userId, refundAmount, reason);
            } catch (Exception e) {
                log.error("发送退租消息失败", e);
            }

            return refundAmount;
        }
    }

    /**
     * 计算退款金额（公开方法）
     */
    @Override
    public java.math.BigDecimal calculateRefund(Long contractId) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }
        return calculateRefundInternal(contract);
    }

    /**
     * 计算退款金额（内部方法）
     */
    private java.math.BigDecimal calculateRefundInternal(Contract contract) {
        java.time.LocalDate startDate = contract.getStartDate();
        java.time.LocalDate endDate = contract.getEndDate();
        java.time.LocalDate actualCheckIn = contract.getActualCheckInDate();
        java.time.LocalDate checkOutDate = java.time.LocalDate.now();

        // 情况1：未入住（实际入住日期为空或在退租日期之后）
        if (actualCheckIn == null || actualCheckIn.isAfter(checkOutDate)) {
            // 全额退款 = 租金 + 押金
            return contract.getRentalAmount().add(contract.getDepositAmount());
        }

        // 情况2：已入住，按天计算
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        long usedDays = java.time.temporal.ChronoUnit.DAYS.between(actualCheckIn, checkOutDate);
        long remainingDays = totalDays - usedDays;

        // 如果已经超过租期，不退款
        if (remainingDays <= 0) {
            // 只退押金
            return contract.getDepositAmount();
        }

        // 每日租金 = 总租金 / 总天数
        java.math.BigDecimal dailyRent = contract.getRentalAmount().divide(
                java.math.BigDecimal.valueOf(totalDays), 2, java.math.RoundingMode.HALF_UP);

        // 退款金额 = 剩余天数 × 每日租金 + 押金
        java.math.BigDecimal refundRent = dailyRent.multiply(java.math.BigDecimal.valueOf(remainingDays));
        return refundRent.add(contract.getDepositAmount());
    }

    /**
     * 记录房东收入
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordLandlordIncome(String orderNo, java.math.BigDecimal totalPayment) {
        log.info("开始记录房东收入: orderNo={}, totalPayment={}", orderNo, totalPayment);

        // 1. 根据订单号查询合同
        LambdaQueryWrapper<Contract> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Contract::getOrderId, orderNo);
        Contract contract = contractMapper.selectOne(queryWrapper);

        if (contract == null) {
            log.warn("未找到对应的合同: orderNo={}", orderNo);
            return;
        }

        // 2. 计算房东收入 = (租金总额 - 押金) × 80%
        java.math.BigDecimal deposit = contract.getDepositAmount();
        java.math.BigDecimal landlordIncome = totalPayment.subtract(deposit)
                .multiply(new java.math.BigDecimal("0.80"));

        // 3. 创建房东收入记录
        com.example.contract.entity.LandlordIncome income = new com.example.contract.entity.LandlordIncome();
        income.setLandlordId(contract.getLandlordId());
        income.setContractId(contract.getId());
        income.setOrderId(contract.getOrderId());
        income.setIncomeType("RENTAL");
        income.setAmount(landlordIncome);
        income.setTotalPayment(totalPayment);
        income.setDepositAmount(deposit);
        income.setCommissionRate(new java.math.BigDecimal("0.80"));
        income.setRemark("租金收入");
        income.setCreatedAt(LocalDateTime.now());

        landlordIncomeMapper.insert(income);

        // 4. 更新合同的房东收入字段
        contract.setLandlordIncome(landlordIncome);
        contract.setUpdatedAt(LocalDateTime.now());
        contractMapper.updateById(contract);

        log.info("房东收入记录成功: landlordId={}, amount={}", contract.getLandlordId(), landlordIncome);
    }
}
