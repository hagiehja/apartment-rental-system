package com.example.house.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房源列表项DTO
 */
@Data
public class HouseListDTO implements Serializable {

    private Long houseId;

    private Long landlordId; // 房东ID

    private String landlordName; // 房东用户名

    private String title;

    private String city;

    private String district;

    private BigDecimal area;

    private Integer roomCount;

    private Integer hallCount;

    private String rentType;

    private BigDecimal price;

    private String coverImage; // 封面图

    private Integer viewCount;

    private LocalDateTime createTime;

    /** 房源状态：AVAILABLE-可租赁，RENTED-已租赁 */
    private String status;
}
