package com.example.house.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class UserPreferenceDTO implements Serializable {

    private Long userId;

    private String city;

    private String district;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Integer roomCount;

    private BigDecimal minArea;

    private BigDecimal maxArea;

    private String rentType;

    private String commuteAddress;
}
