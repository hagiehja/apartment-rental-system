package com.example.house.recommendation.fm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public class FmScorer {

    private final FmModel model;

    public FmScorer(FmModel model) {
        this.model = model == null ? FmModel.defaultModel() : model;
    }

    public double scoreProbability(FmFeatureVector vector) {
        FmFeatureVector safeVector = vector == null ? FmFeatureVector.empty() : vector;
        double raw = model.getBias() + linearScore(safeVector) + interactionScore(safeVector);
        return sigmoid(raw);
    }

    public double score100(FmFeatureVector vector) {
        return round(scoreProbability(vector) * 100.0);
    }

    private double linearScore(FmFeatureVector vector) {
        double score = 0.0;
        for (Map.Entry<String, Double> entry : vector.values().entrySet()) {
            score += model.getLinearWeights().getOrDefault(entry.getKey(), 0.0) * entry.getValue();
        }
        return score;
    }

    private double interactionScore(FmFeatureVector vector) {
        int factorSize = model.getFactors().values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);
        double score = 0.0;
        for (int factor = 0; factor < factorSize; factor++) {
            double summed = 0.0;
            double squaredSum = 0.0;
            for (Map.Entry<String, Double> entry : vector.values().entrySet()) {
                List<Double> factors = model.getFactors().get(entry.getKey());
                if (factors == null || factor >= factors.size()) {
                    continue;
                }
                double vx = factors.get(factor) * entry.getValue();
                summed += vx;
                squaredSum += vx * vx;
            }
            score += 0.5 * ((summed * summed) - squaredSum);
        }
        return score;
    }

    private double sigmoid(double value) {
        if (value >= 35.0) {
            return 1.0;
        }
        if (value <= -35.0) {
            return 0.0;
        }
        return 1.0 / (1.0 + Math.exp(-value));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
