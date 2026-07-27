package com.example.house.recommendation.fm;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeepFmScorerTest {

    @Test
    void computesProbabilityForPureFmModel() {
        // Pure FM (no DNN weights)
        FmModel fm = FmModel.defaultModel();
        DeepFmScorer scorer = new DeepFmScorer(wrap(fm));
        FmFeatureVector vec = FmFeatureVector.of(new LinkedHashMap<>(Map.of(
                "match.city", 1.0, "price.inRange", 1.0, "status.available", 1.0)));
        double p = scorer.scoreProbability(vec);
        assertTrue(p >= 0.0 && p <= 1.0, "probability in [0,1]");
        assertFalse(scorer.hasDnn(), "no DNN");
    }

    @Test
    void computesProbabilityForDeepFmModel() {
        // Build a tiny DeepFM with 1 hidden layer (14 -> 4) + output (4 -> 1)
        Map<String, Double> lw = new LinkedHashMap<>();
        Map<String, List<Double>> fac = new LinkedHashMap<>();
        for (String n : new String[]{"match.city", "match.district", "match.rentType",
                "price.inRange", "price.nearRange", "price.bucket.mid",
                "room.exact", "room.near",
                "status.available", "status.rented",
                "popularity.high", "popularity.medium",
                "freshness.week", "freshness.month"}) {
            lw.put(n, 0.1);
            fac.put(n, List.of(0.1, 0.2, 0.3));
        }
        // DNN layer 14 -> 4
        double[][] W1 = new double[14][4];
        for (int i = 0; i < 14; i++) java.util.Arrays.fill(W1[i], 0.05);
        double[] b1 = new double[]{0.1, 0.1, 0.1, 0.1};
        // Output layer 4 -> 1
        double[] outW = new double[]{0.5, 0.5, 0.5, 0.5};
        double outB = 0.0;
        DeepFmModel model = new DeepFmModel(
                0.1, lw, fac,
                java.util.List.of(new DeepFmModel.DnnLayer(W1, b1)),
                outW, outB);
        DeepFmScorer scorer = new DeepFmScorer(model);
        assertTrue(scorer.hasDnn());

        FmFeatureVector vec = FmFeatureVector.of(new LinkedHashMap<>(Map.of(
                "match.city", 1.0, "price.inRange", 1.0)));
        double p = scorer.scoreProbability(vec);
        assertTrue(p >= 0.0 && p <= 1.0, "probability in [0,1]");
        assertTrue(p > 0.5, "all-positive weights should yield > 0.5");
    }

    @Test
    void score100ScalesBy100() {
        DeepFmScorer scorer = new DeepFmScorer(wrap(FmModel.defaultModel()));
        FmFeatureVector vec = FmFeatureVector.of(Map.of("match.city", 1.0));
        double s = scorer.score100(vec);
        assertTrue(s >= 0.0 && s <= 100.0);
    }

    private static DeepFmModel wrap(FmModel fm) {
        return new DeepFmModel(fm.getBias(), fm.getLinearWeights(), fm.getFactors(),
                null, new double[0], 0.0);
    }
}