package com.example.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合同签署记录实体类
 */
@Data
@TableName("contract_signature")
public class ContractSignature {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 签署用户ID
     */
    private Long userId;

    /**
     * 用户类型(LANDLORD-房东/TENANT-租客)
     */
    private String userType;

    /**
     * 签名数据(Base64编码,可选)
     */
    private String signatureData;

    /**
     * 签署IP地址
     */
    private String ipAddress;

    /**
     * 签署时间
     */
    private LocalDateTime signedAt;
}
