package com.example.house.recommendation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationScorerTest {

    private final RecommendationScorer scorer = new RecommendationScorer();

    @Test
    void matchingHouseScoresHigherThanMismatchedHouse() {
        UserPreferenceSnapshot preference = UserPreferenceSnapshot.builder()
                .city("Shanghai")
                .district("Pudong")
                .minPrice(new BigDecimal("3000"))
                .maxPrice(new BigDecimal("5000"))
                .roomCount(2)
                .minArea(new BigDecimal("60"))
                .maxArea(new BigDecimal("90"))
                .rentType("WHOLE")
                .build();

        RecommendationCandidate matching = RecommendationCandidate.builder()
                .houseId(1L)
                .city("Shanghai")
                .district("Pudong")
                .price(new BigDecimal("4200"))
                .roomCount(2)
                .area(new BigDecimal("75"))
                .rentType("WHOLE")
                .viewCount(500)
                .status("AVAILABLE")
                .createTime(LocalDateTime.now().minusDays(2))
                .build();

        RecommendationCandidate mismatched = RecommendationCandidate.builder()
                .houseId(2L)
                .city("Shanghai")
                .district("Minhang")
                .price(new BigDecimal("8200"))
                .roomCount(4)
                .area(new BigDecimal("130"))
                .rentType("SHARED")
                .viewCount(20)
                .status("RENTED")
                .createTime(LocalDateTime.now().minusDays(80))
                .build();

        RecommendationScore matchingScore = scorer.score(preference, matching);
        RecommendationScore mismatchedScore = scorer.score(preference, mismatched);

        assertTrue(matchingScore.getScore() > mismatchedScore.getScore());
        assertTrue(matchingScore.getScore() >= 80.0);
        assertTrue(matchingScore.getReason().contains("district"));
        assertTrue(matchingScore.getReason().contains("price"));
        assertTrue(matchingScore.getReason().contains("available"));
    }

    @Test
    void scorerKeepsScoreInZeroToOneHundredRange() {
        UserPreferenceSnapshot preference = UserPreferenceSnapshot.builder()
                .city("Shanghai")
                .district("Pudong")
                .minPrice(new BigDecimal("3000"))
                .maxPrice(new BigDecimal("5000"))
                .roomCount(2)
                .build();

        RecommendationCandidate candidate = RecommendationCandidate.builder()
                .houseId(3L)
                .city("Beijing")
                .district("Haidian")
                .price(new BigDecimal("20000"))
                .roomCount(6)
                .area(new BigDecimal("240"))
                .viewCount(0)
                .status("OFFLINE")
                .createTime(LocalDateTime.now().minusDays(365))
                .build();

        RecommendationScore score = scorer.score(preference, candidate);

        assertTrue(score.getScore() >= 0.0);
        assertTrue(score.getScore() <= 100.0);
    }
}
