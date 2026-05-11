package com.example.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同传输对象
 */
@Data
public class ContractDTO {
    private Long id;
    private String contractNo;
    private Long orderId;
    private Long landlordId;
    private Long tenantId;
    private Long houseId;
    private String houseName;
    private String houseAddress;
    private BigDecimal rentalAmount;
    private BigDecimal depositAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}
