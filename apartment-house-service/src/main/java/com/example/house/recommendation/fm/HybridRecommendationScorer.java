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

/**
 * 混合评分器 (规则 70% + FM/DeepFM 30%)
 * <p>
 * 自动检测模型类型:
 *   - 若 fm-model-v1.json 含 dnnWeights -> 使用 DeepFmScorer
 *   - 否则 -> 使用 FmScorer
 * reason 字段会标注 "fm_v2:" 或 "deepfm:" 以便前端/日志区分。
 */
@Component
public class HybridRecommendationScorer {

    private static final double RULE_WEIGHT = 0.70;
    private static final double FM_WEIGHT = 0.30;

    private final RecommendationScorer ruleScorer;
    private final FmFeatureBuilder featureBuilder;
    private final DeepFmScorer fmScorer;
    private final boolean usingDeepFm;

    @Autowired
    public HybridRecommendationScorer(RecommendationScorer ruleScorer,
                                      FmFeatureBuilder featureBuilder,
                                      FmModelLoader modelLoader) {
        this(ruleScorer, featureBuilder, new DeepFmScorer(modelLoader.loadDefaultDeepFmModel()));
    }

    public HybridRecommendationScorer(RecommendationScorer ruleScorer,
                                      FmFeatureBuilder featureBuilder,
                                      DeepFmScorer fmScorer) {
        this.ruleScorer = ruleScorer;
        this.featureBuilder = featureBuilder;
        this.fmScorer = fmScorer;
        this.usingDeepFm = fmScorer.hasDnn();
    }

    public RecommendationScore score(UserPreferenceSnapshot preference, RecommendationCandidate candidate) {
        RecommendationScore ruleScore = ruleScorer.score(preference, candidate);
        double fmScore = fmScorer.score100(featureBuilder.build(preference, candidate));
        double blended = round((ruleScore.getScore() * RULE_WEIGHT) + (fmScore * FM_WEIGHT));
        String tag = usingDeepFm ? "deepfm:" : "fm_v2:";
        String reason = StringUtils.hasText(ruleScore.getReason())
                ? ruleScore.getReason() + "," + tag + round(fmScore)
                : tag + round(fmScore);
        return new RecommendationScore(Math.max(0.0, Math.min(100.0, blended)), reason);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}