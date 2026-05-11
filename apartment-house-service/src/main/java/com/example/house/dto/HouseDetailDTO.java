package com.example.house.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 房源详情DTO
 */
@Data
public class HouseDetailDTO implements Serializable {

    private Long houseId;

    private Long landlordId;

    private String title;

    private String description;

    private String province;

    private String city;

    private String district;

    private String address;

    private BigDecimal area;

    private Integer roomCount;

    private Integer hallCount;

    private Integer bathroomCount;

    private Integer floor;

    private Integer totalFloor;

    private String orientation;

    private String decoration;

    private String rentType;

    private BigDecimal price;

    private String paymentMethod;

    private List<String> facilities; // 配套设施

    private String status;

    private Integer viewCount;

    private List<ImageDTO> images; // 图片列表

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
