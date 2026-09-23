package com.example.house.recommendation;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class RecommendationCandidate {
    Long houseId;
    String city;
    String district;
    BigDecimal price;
    Integer roomCount;
    BigDecimal area;
    String rentType;
    Integer viewCount;
    String status;
    LocalDateTime createTime;
}
