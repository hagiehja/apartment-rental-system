package com.example.house.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HouseRecommendDTO implements Serializable {

    private Long houseId;

    private String title;

    private String city;

    private String district;

    private BigDecimal area;

    private Integer roomCount;

    private Integer hallCount;

    private String rentType;

    private BigDecimal price;

    private String coverImage;

    private Integer viewCount;

    private String status;

    private LocalDateTime createTime;

    private Double recommendScore;

    private String recommendReason;
}
