package com.example.contract.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.contract.dto.SignContractDTO;
import com.example.contract.entity.Contract;

import java.math.BigDecimal;

/**
 * 合同服务接口
 */
public interface ContractService {

    /**
     * 根据订单生成合同
     * 
     * @param contract 合同信息(从订单服务获取)
     * @return 合同ID
     */
    Long generateContract(Contract contract);

    /**
     * 签署合同
     */
    void signContract(Long contractId, SignContractDTO dto);

    /**
     * 查询合同详情
     */
    Contract getContractById(Long contractId);

    /**
     * 根据订单ID查询合同
     */
    Contract getContractByOrderId(Long orderId);

    /**
     * 查询用户的合同列表
     */
    IPage<Contract> getContractList(Long userId, String userType, String status, Integer page, Integer size);

    /**
     * 取消合同(订单取消时调用)
     */
    void cancelContract(Long orderId);

    /**
     * 申请退租
     * 
     * @param contractId 合同ID
     * @param userId     用户ID
     * @param reason     退租原因
     * @return 退款金额
     */
    BigDecimal terminateContract(Long contractId, Long userId, String reason);

    /**
     * 计算退款金额
     * 
     * @param contractId 合同ID
     * @return 退款金额
     */
    BigDecimal calculateRefund(Long contractId);

    /**
     * 记录房东收入
     * 
     * @param orderNo      订单号
     * @param totalPayment 租客支付总额
     */
    void recordLandlordIncome(String orderNo, BigDecimal totalPayment);
}
