package com.example.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 租赁合同实体类
 */
@Data
@TableName("contract")
public class Contract {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 合同编号(唯一)
     */
    private String contractNo;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 房东ID
     */
    private Long landlordId;

    /**
     * 租客ID
     */
    private Long tenantId;

    /**
     * 房源ID
     */
    private Long houseId;

    /**
     * 房源名称
     */
    private String houseName;

    /**
     * 房源地址
     */
    private String houseAddress;

    /**
     * 租金金额(元/月)
     */
    private BigDecimal rentalAmount;

    /**
     * 押金金额
     */
    private BigDecimal depositAmount;

    /**
     * 租赁开始日期
     */
    private LocalDate startDate;

    /**
     * 租赁结束日期
     */
    private LocalDate endDate;

    /**
     * 实际入住日期
     */
    private LocalDate actualCheckInDate;

    /**
     * 实际退租日期
     */
    private LocalDate actualCheckOutDate;

    /**
     * 退租原因
     */
    private String terminationReason;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 房东收入金额
     */
    private BigDecimal landlordIncome;

    /**
     * 状态(PENDING-待签署/LANDLORD_SIGNED-房东已签署/COMPLETED-已完成/ARCHIVED-已归档/CANCELLED-已作废/TERMINATED-已退租)
     */
    private String status;

    /**
     * 合同PDF文件路径
     */
    private String filePath;

    /**
     * 房东签署时间
     */
    private LocalDateTime landlordSignedAt;

    /**
     * 租客签署时间
     */
    private LocalDateTime tenantSignedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
