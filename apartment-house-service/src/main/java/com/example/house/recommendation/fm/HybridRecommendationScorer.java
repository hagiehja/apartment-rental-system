package com.example.house.recommendation.fm;

import com.example.house.recommendation.RecommendationCandidate;
import com.example.house.recommendation.RecommendationScore;
import com.example.house.recommendation.RecommendationScorer;
import com.example.house.recommendation.UserPreferenceSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class HybridRecommendationScorer {

    private static final double RULE_WEIGHT = 0.70;
    private static final double FM_WEIGHT = 0.30;

    private final RecommendationScorer ruleScorer;
    private final FmFeatureBuilder featureBuilder;
    private final FmScorer fmScorer;

    @Autowired
    public HybridRecommendationScorer(RecommendationScorer ruleScorer,
                                      FmFeatureBuilder featureBuilder,
                                      FmModelLoader modelLoader) {
        this(ruleScorer, featureBuilder, new FmScorer(modelLoader.loadDefaultModel()));
    }

    public HybridRecommendationScorer(RecommendationScorer ruleScorer,
                                      FmFeatureBuilder featureBuilder,
                                      FmScorer fmScorer) {
        this.ruleScorer = ruleScorer;
        this.featureBuilder = featureBuilder;
        this.fmScorer = fmScorer;
    }

    public RecommendationScore score(UserPreferenceSnapshot preference, RecommendationCandidate candidate) {
        RecommendationScore ruleScore = ruleScorer.score(preference, candidate);
        double fmScore = fmScorer.score100(featureBuilder.build(preference, candidate));
        double blended = round((ruleScore.getScore() * RULE_WEIGHT) + (fmScore * FM_WEIGHT));
        String reason = StringUtils.hasText(ruleScore.getReason())
                ? ruleScore.getReason() + ",fm_v2:" + round(fmScore)
                : "fm_v2:" + round(fmScore);
        return new RecommendationScore(Math.max(0.0, Math.min(100.0, blended)), reason);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}