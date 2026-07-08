package com.example.house.recommendation.fm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FmScorerTest {

    @Test
    void interactionFeatureRaisesProbability() {
        FmModel model = new FmModel(
                -1.0,
                Map.of("match.district", 1.2, "price.bucket.mid", 0.4),
                Map.of(
                        "match.district", List.of(0.9, 0.1),
                        "price.bucket.mid", List.of(0.8, 0.2)
                )
        );
        FmScorer scorer = new FmScorer(model);

        double strong = scorer.scoreProbability(FmFeatureVector.of(Map.of(
                "match.district", 1.0,
                "price.bucket.mid", 1.0
        )));
        double weak = scorer.scoreProbability(FmFeatureVector.of(Map.of(
                "price.bucket.mid", 1.0
        )));

        assertTrue(strong > weak);
        assertTrue(strong >= 0.0 && strong <= 1.0);
    }

    @Test
    void convertsProbabilityToBoundedHundredPointScore() {
        FmModel model = new FmModel(
                0.5,
                Map.of("status.available", 0.8),
                Map.of("status.available", List.of(0.2, 0.1))
        );
        FmScorer scorer = new FmScorer(model);

        double score = scorer.score100(FmFeatureVector.of(Map.of("status.available", 1.0)));

        assertTrue(score >= 0.0);
        assertTrue(score <= 100.0);
    }
}
