package com.example.contract.dto;

import lombok.Data;

/**
 * 签署合同DTO
 */
@Data
public class SignContractDTO {

    /**
     * 用户ID
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
     * IP地址
     */
    private String ipAddress;
}
