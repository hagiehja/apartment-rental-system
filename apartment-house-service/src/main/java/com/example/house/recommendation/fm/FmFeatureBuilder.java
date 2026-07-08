package com.example.house.recommendation.fm;

import com.example.house.recommendation.RecommendationCandidate;
import com.example.house.recommendation.UserPreferenceSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class FmFeatureBuilder {

    public FmFeatureVector build(UserPreferenceSnapshot preference, RecommendationCandidate candidate) {
        if (candidate == null) {
            return FmFeatureVector.empty();
        }
        UserPreferenceSnapshot safePreference = preference == null
                ? UserPreferenceSnapshot.builder().build()
                : preference;
        Map<String, Double> features = new LinkedHashMap<>();
        addLocationFeatures(features, safePreference, candidate);
        addPriceFeatures(features, safePreference, candidate);
        addRoomFeatures(features, safePreference, candidate);
        addRentTypeFeature(features, safePreference, candidate);
        addStatusFeature(features, candidate);
        addPopularityFeature(features, candidate);
        addFreshnessFeature(features, candidate);
        return FmFeatureVector.of(features);
    }

    private void addLocationFeatures(Map<String, Double> features,
                                     UserPreferenceSnapshot preference,
                                     RecommendationCandidate candidate) {
        if (sameText(preference.getCity(), candidate.getCity())) {
            features.put("match.city", 1.0);
        }
        if (sameText(preference.getDistrict(), candidate.getDistrict())) {
            features.put("match.district", 1.0);
        }
    }

    private void addPriceFeatures(Map<String, Double> features,
                                  UserPreferenceSnapshot preference,
                                  RecommendationCandidate candidate) {
        BigDecimal price = candidate.getPrice();
        if (price == null) {
            return;
        }
        if (price.compareTo(new BigDecimal("3000")) < 0) {
            features.put("price.bucket.low", 1.0);
        } else if (price.compareTo(new BigDecimal("7000")) <= 0) {
            features.put("price.bucket.mid", 1.0);
        } else {
            features.put("price.bucket.high", 1.0);
        }

        BigDecimal min = preference.getMinPrice();
        BigDecimal max = preference.getMaxPrice();
        if (min != null && max != null && price.compareTo(min) >= 0 && price.compareTo(max) <= 0) {
            features.put("price.inRange", 1.0);
            return;
        }
        if (max != null && price.compareTo(max.multiply(new BigDecimal("1.15"))) <= 0) {
            features.put("price.nearRange", 1.0);
        }
    }

    private void addRoomFeatures(Map<String, Double> features,
                                 UserPreferenceSnapshot preference,
                                 RecommendationCandidate candidate) {
        Integer preferred = preference.getRoomCount();
        Integer actual = candidate.getRoomCount();
        if (preferred == null || actual == null) {
            return;
        }
        int distance = Math.abs(preferred - actual);
        if (distance == 0) {
            features.put("room.exact", 1.0);
        } else if (distance == 1) {
            features.put("room.near", 1.0);
        }
    }

    private void addRentTypeFeature(Map<String, Double> features,
                                    UserPreferenceSnapshot preference,
                                    RecommendationCandidate candidate) {
        if (sameText(preference.getRentType(), candidate.getRentType())) {
            features.put("match.rentType", 1.0);
        }
    }

    private void addStatusFeature(Map<String, Double> features, RecommendationCandidate candidate) {
        if (Objects.equals("AVAILABLE", candidate.getStatus())) {
            features.put("status.available", 1.0);
        } else if (Objects.equals("RENTED", candidate.getStatus())) {
            features.put("status.rented", 1.0);
        } else if (StringUtils.hasText(candidate.getStatus())) {
            features.put("status.other", 1.0);
        }
    }

    private void addPopularityFeature(Map<String, Double> features, RecommendationCandidate candidate) {
        int views = candidate.getViewCount() == null ? 0 : Math.max(0, candidate.getViewCount());
        if (views >= 300) {
            features.put("popularity.high", 1.0);
        } else if (views >= 50) {
            features.put("popularity.medium", 1.0);
        } else if (views > 0) {
            features.put("popularity.low", 1.0);
        }
    }

    private void addFreshnessFeature(Map<String, Double> features, RecommendationCandidate candidate) {
        LocalDateTime createTime = candidate.getCreateTime();
        if (createTime == null) {
            return;
        }
        long days = Math.max(0, Duration.between(createTime, LocalDateTime.now()).toDays());
        if (days <= 7) {
            features.put("freshness.week", 1.0);
        } else if (days <= 30) {
            features.put("freshness.month", 1.0);
        } else if (days <= 90) {
            features.put("freshness.quarter", 1.0);
        }
    }

    private boolean sameText(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right) && left.equalsIgnoreCase(right);
    }
}
