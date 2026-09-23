package com.example.house.recommendation;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class UserPreferenceSnapshot {
    String city;
    String district;
    BigDecimal minPrice;
    BigDecimal maxPrice;
    Integer roomCount;
    BigDecimal minArea;
    BigDecimal maxArea;
    String rentType;
}
