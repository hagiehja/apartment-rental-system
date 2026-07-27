package com.example.house.recommendation.fm;

import com.example.house.recommendation.RecommendationCandidate;
import com.example.house.recommendation.RecommendationScore;
import com.example.house.recommendation.RecommendationScorer;
import com.example.house.recommendation.UserPreferenceSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HybridRecommendationScorerTest {

    @Test
    void blendsRuleAndFmScoresAndMarksReason() {
        HybridRecommendationScorer scorer = new HybridRecommendationScorer(
                new RecommendationScorer(),
                new FmFeatureBuilder(),
                new DeepFmScorer(wrap(FmModel.defaultModel()))
        );

        RecommendationScore score = scorer.score(
                UserPreferenceSnapshot.builder()
                        .city("Shanghai")
                        .district("Pudong")
                        .minPrice(new BigDecimal("3000"))
                        .maxPrice(new BigDecimal("5000"))
                        .roomCount(2)
                        .rentType("WHOLE")
                        .build(),
                RecommendationCandidate.builder()
                        .city("Shanghai")
                        .district("Pudong")
                        .price(new BigDecimal("4200"))
                        .roomCount(2)
                        .rentType("WHOLE")
                        .status("AVAILABLE")
                        .viewCount(500)
                        .createTime(LocalDateTime.now().minusDays(2))
                        .build()
        );

        assertTrue(score.getScore() >= 0.0 && score.getScore() <= 100.0);
        assertTrue(score.getReason().contains("fm_v2") || score.getReason().contains("deepfm"));
    }

    @Test
    void keepsRuleFallbackWhenPreferenceIsEmpty() {
        HybridRecommendationScorer scorer = new HybridRecommendationScorer(
                new RecommendationScorer(),
                new FmFeatureBuilder(),
                new DeepFmScorer(wrap(FmModel.defaultModel()))
        );

        RecommendationScore score = scorer.score(
                UserPreferenceSnapshot.builder().build(),
                RecommendationCandidate.builder()
                        .status("AVAILABLE")
                        .viewCount(500)
                        .createTime(LocalDateTime.now().minusDays(2))
                        .build()
        );

        assertTrue(score.getScore() > 0.0);
        assertTrue(score.getReason().contains("fm_v2") || score.getReason().contains("deepfm"));
    }

    /** Wrap an FmModel as a no-DNN DeepFmModel (backward compatible path). */
    private static DeepFmModel wrap(FmModel fm) {
        return new DeepFmModel(fm.getBias(), fm.getLinearWeights(), fm.getFactors(),
                null, new double[0], 0.0);
    }
}
