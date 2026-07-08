package com.example.house.recommendation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class RecommendationScorer {

    public RecommendationScore score(UserPreferenceSnapshot preference, RecommendationCandidate candidate) {
        if (candidate == null) {
            return new RecommendationScore(0.0, "empty candidate");
        }
        UserPreferenceSnapshot safePreference = preference == null
                ? UserPreferenceSnapshot.builder().build()
                : preference;

        List<String> reasons = new ArrayList<>();
        double score = 0.0;
        score += locationScore(safePreference, candidate, reasons);
        score += priceScore(safePreference, candidate, reasons);
        score += roomScore(safePreference, candidate, reasons);
        score += areaScore(safePreference, candidate, reasons);
        score += rentTypeScore(safePreference, candidate, reasons);
        score += popularityScore(candidate, reasons);
        score += freshnessScore(candidate, reasons);
        score += statusScore(candidate, reasons);

        double bounded = Math.max(0.0, Math.min(100.0, round(score)));
        if (reasons.isEmpty()) {
            reasons.add("fallback ranking");
        }
        return new RecommendationScore(bounded, String.join(",", reasons));
    }

    private double locationScore(UserPreferenceSnapshot preference, RecommendationCandidate candidate, List<String> reasons) {
        double score = 0.0;
        if (sameText(preference.getCity(), candidate.getCity())) {
            score += 8.0;
            reasons.add("city");
        }
        if (sameText(preference.getDistrict(), candidate.getDistrict())) {
            score += 17.0;
            reasons.add("district");
        }
        return score;
    }

    private double priceScore(UserPreferenceSnapshot preference, RecommendationCandidate candidate, List<String> reasons) {
        BigDecimal price = candidate.getPrice();
        if (price == null) {
            return 0.0;
        }
        BigDecimal min = preference.getMinPrice();
        BigDecimal max = preference.getMaxPrice();
        if (min != null && max != null && price.compareTo(min) >= 0 && price.compareTo(max) <= 0) {
            reasons.add("price");
            return 20.0;
        }
        if (min != null && price.compareTo(min) < 0) {
            reasons.add("low price");
            return 12.0;
        }
        if (max != null && price.compareTo(max.multiply(new BigDecimal("1.15"))) <= 0) {
            reasons.add("near price");
            return 8.0;
        }
        return 0.0;
    }

    private double roomScore(UserPreferenceSnapshot preference, RecommendationCandidate candidate, List<String> reasons) {
        Integer preferred = preference.getRoomCount();
        Integer actual = candidate.getRoomCount();
        if (preferred == null || actual == null) {
            return 0.0;
        }
        int distance = Math.abs(preferred - actual);
        if (distance == 0) {
            reasons.add("room");
            return 15.0;
        }
        if (distance == 1) {
            reasons.add("near room");
            return 8.0;
        }
        return 0.0;
    }

    private double areaScore(UserPreferenceSnapshot preference, RecommendationCandidate candidate, List<String> reasons) {
        BigDecimal area = candidate.getArea();
        if (area == null) {
            return 0.0;
        }
        BigDecimal min = preference.getMinArea();
        BigDecimal max = preference.getMaxArea();
        if (min != null && max != null && area.compareTo(min) >= 0 && area.compareTo(max) <= 0) {
            reasons.add("area");
            return 10.0;
        }
        return 0.0;
    }

    private double rentTypeScore(UserPreferenceSnapshot preference, RecommendationCandidate candidate, List<String> reasons) {
        if (sameText(preference.getRentType(), candidate.getRentType())) {
            reasons.add("rent type");
            return 8.0;
        }
        return 0.0;
    }

    private double popularityScore(RecommendationCandidate candidate, List<String> reasons) {
        int views = candidate.getViewCount() == null ? 0 : Math.max(0, candidate.getViewCount());
        if (views >= 300) {
            reasons.add("popular");
            return 8.0;
        }
        if (views >= 100) {
            return 5.0;
        }
        if (views >= 20) {
            return 2.0;
        }
        return 0.0;
    }

    private double freshnessScore(RecommendationCandidate candidate, List<String> reasons) {
        LocalDateTime createTime = candidate.getCreateTime();
        if (createTime == null) {
            return 0.0;
        }
        long days = Math.max(0, Duration.between(createTime, LocalDateTime.now()).toDays());
        if (days <= 7) {
            reasons.add("fresh");
            return 7.0;
        }
        if (days <= 30) {
            return 4.0;
        }
        if (days <= 90) {
            return 1.0;
        }
        return 0.0;
    }

    private double statusScore(RecommendationCandidate candidate, List<String> reasons) {
        if (Objects.equals("AVAILABLE", candidate.getStatus())) {
            reasons.add("available");
            return 12.0;
        }
        if (Objects.equals("RENTED", candidate.getStatus())) {
            return 2.0;
        }
        return -10.0;
    }

    private boolean sameText(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right) && left.equalsIgnoreCase(right);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
