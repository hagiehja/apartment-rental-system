package com.example.house.recommendation.fm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FmModelLoaderTest {

    @Test
    void loadsDefaultModelFromClasspath() {
        FmModel model = new FmModelLoader().loadDefaultModel();

        assertTrue(model.getLinearWeights().containsKey("match.district"));
        assertTrue(model.getFactors().containsKey("price.inRange"));
    }

    @Test
    void builtInFallbackModelCoversSecondaryFeatures() {
        FmModel model = FmModel.defaultModel();

        assertTrue(model.getLinearWeights().containsKey("price.bucket.mid"));
        assertTrue(model.getLinearWeights().containsKey("status.rented"));
        assertTrue(model.getLinearWeights().containsKey("popularity.medium"));
        assertTrue(model.getLinearWeights().containsKey("freshness.month"));
        assertTrue(model.getFactors().containsKey("price.bucket.mid"));
        assertTrue(model.getFactors().containsKey("status.rented"));
        assertTrue(model.getFactors().containsKey("popularity.medium"));
        assertTrue(model.getFactors().containsKey("freshness.month"));
    }
}
