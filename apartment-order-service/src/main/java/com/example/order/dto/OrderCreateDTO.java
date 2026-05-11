package com.example.order.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建订单DTO
 */
@Data
public class OrderCreateDTO {

    @NotNull(message = "房源ID不能为空")
    private Long houseId; // 房源ID

    @NotNull(message = "租期开始日期不能为空")
    @Future(message = "租期开始日期必须是未来日期")
    private LocalDate rentStartDate; // 租期开始日期

    @NotNull(message = "租赁月数不能为空")
    @Min(value = 1, message = "租赁月数至少为1个月")
    @Max(value = 24, message = "租赁月数最多为24个月")
    private Integer rentMonths; // 租赁月数

    private Boolean installmentEnabled = false; // 是否分期付款

    private String remark; // 备注
}
