# House FM Recommendation V2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a lightweight Factorization Machine recommendation layer for house ranking, then combine it with the existing rule-based V1 score.

**Architecture:** Keep the existing V1 rule scorer as the stable fallback. Add a pure Java FM inference layer under `com.example.house.recommendation.fm`, build sparse user-house features, load a small default model from resources, and blend `70%` rule score with `30%` FM score in `RecommendationServiceImpl`.

**Tech Stack:** Spring Boot 3, Java 17-compatible code, JUnit 5, Lombok, Jackson from Spring Boot starters.

---

### Task 1: FM Core Scorer

**Files:**
- Create: `apartment-house-service/src/test/java/com/example/house/recommendation/fm/FmScorerTest.java`
- Create: `apartment-house-service/src/main/java/com/example/house/recommendation/fm/FmFeatureVector.java`
- Create: `apartment-house-service/src/main/java/com/example/house/recommendation/fm/FmModel.java`
- Create: `apartment-house-service/src/main/java/com/example/house/recommendation/fm/FmScorer.java`

- [ ] **Step 1: Write failing FM scorer tests**

```java
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
```

- [ ] **Step 2: Run RED**

Run: `mvn -q -pl apartment-house-service -Dtest=FmScorerTest test`

Expected: fail because `FmModel`, `FmScorer`, and `FmFeatureVector` do not exist.

- [ ] **Step 3: Implement FM model, vector, and scorer**

Implement the standard second-order FM formula:

```text
raw = bias + sum(w_i * x_i) + 0.5 * sum_f((sum_i(v_if * x_i))^2 - sum_i(v_if^2 * x_i^2))
probability = sigmoid(raw)
score100 = probability * 100
```

- [ ] **Step 4: Run GREEN**

Run: `mvn -q -pl apartment-house-service -Dtest=FmScorerTest test`

Expected: pass.

### Task 2: User-House Feature Builder

**Files:**
- Create: `apartment-house-service/src/test/java/com/example/house/recommendation/fm/FmFeatureBuilderTest.java`
- Create: `apartment-house-service/src/main/java/com/example/house/recommendation/fm/FmFeatureBuilder.java`

- [ ] **Step 1: Write failing feature builder tests**

```java
@Test
void buildsMatchAndBucketFeaturesFromPreferenceAndHouse() {
    FmFeatureBuilder builder = new FmFeatureBuilder();
    UserPreferenceSnapshot preference = UserPreferenceSnapshot.builder()
            .city("Shanghai")
            .district("Pudong")
            .minPrice(new BigDecimal("3000"))
            .maxPrice(new BigDecimal("5000"))
            .roomCount(2)
            .rentType("WHOLE")
            .build();
    RecommendationCandidate house = RecommendationCandidate.builder()
            .city("Shanghai")
            .district("Pudong")
            .price(new BigDecimal("4200"))
            .roomCount(2)
            .rentType("WHOLE")
            .viewCount(320)
            .status("AVAILABLE")
            .createTime(LocalDateTime.now().minusDays(3))
            .build();

    FmFeatureVector vector = builder.build(preference, house);

    assertEquals(1.0, vector.value("match.city"));
    assertEquals(1.0, vector.value("match.district"));
    assertEquals(1.0, vector.value("match.rentType"));
    assertEquals(1.0, vector.value("price.inRange"));
    assertEquals(1.0, vector.value("room.exact"));
    assertEquals(1.0, vector.value("status.available"));
    assertEquals(1.0, vector.value("popularity.high"));
    assertEquals(1.0, vector.value("freshness.week"));
}
```

- [ ] **Step 2: Run RED**

Run: `mvn -q -pl apartment-house-service -Dtest=FmFeatureBuilderTest test`

Expected: fail because `FmFeatureBuilder` does not exist.

- [ ] **Step 3: Implement the feature builder**

Build sparse binary features for city, district, rent type, price range, room match, status, popularity, and freshness.

- [ ] **Step 4: Run GREEN**

Run: `mvn -q -pl apartment-house-service -Dtest=FmFeatureBuilderTest test`

Expected: pass.

### Task 3: Default Model Loader

**Files:**
- Create: `apartment-house-service/src/main/resources/recommendation/fm-model-v1.json`
- Create: `apartment-house-service/src/main/java/com/example/house/recommendation/fm/FmModelLoader.java`
- Create: `apartment-house-service/src/test/java/com/example/house/recommendation/fm/FmModelLoaderTest.java`

- [ ] **Step 1: Write failing model loader test**

```java
@Test
void loadsDefaultModelFromClasspath() {
    FmModel model = new FmModelLoader().loadDefaultModel();

    assertTrue(model.getLinearWeights().containsKey("match.district"));
    assertTrue(model.getFactors().containsKey("price.inRange"));
}
```

- [ ] **Step 2: Run RED**

Run: `mvn -q -pl apartment-house-service -Dtest=FmModelLoaderTest test`

Expected: fail because loader and model resource do not exist.

- [ ] **Step 3: Add JSON model and loader**

The JSON resource contains `bias`, `linearWeights`, and `factors`. The loader reads it with Jackson and falls back to `FmModel.defaultModel()` if the classpath resource is unavailable.

- [ ] **Step 4: Run GREEN**

Run: `mvn -q -pl apartment-house-service -Dtest=FmModelLoaderTest test`

Expected: pass.

### Task 4: Hybrid Ranking Integration

**Files:**
- Modify: `apartment-house-service/src/main/java/com/example/house/service/impl/RecommendationServiceImpl.java`
- Create: `apartment-house-service/src/main/java/com/example/house/recommendation/fm/HybridRecommendationScorer.java`
- Create: `apartment-house-service/src/test/java/com/example/house/recommendation/fm/HybridRecommendationScorerTest.java`

- [ ] **Step 1: Write failing hybrid scorer test**

```java
@Test
void blendsRuleAndFmScoresAndMarksReason() {
    HybridRecommendationScorer scorer = new HybridRecommendationScorer(
            new RecommendationScorer(),
            new FmFeatureBuilder(),
            new FmScorer(FmModel.defaultModel())
    );

    RecommendationScore score = scorer.score(UserPreferenceSnapshot.builder().build(),
            RecommendationCandidate.builder().status("AVAILABLE").viewCount(500).build());

    assertTrue(score.getScore() >= 0.0 && score.getScore() <= 100.0);
    assertTrue(score.getReason().contains("fm_v2"));
}
```

- [ ] **Step 2: Run RED**

Run: `mvn -q -pl apartment-house-service -Dtest=HybridRecommendationScorerTest test`

Expected: fail because `HybridRecommendationScorer` does not exist.

- [ ] **Step 3: Implement hybrid scorer and wire it into service**

Use `0.70 * ruleScore + 0.30 * fmScore100`, round to two decimals, and append `fm_v2` to the recommendation reason.

- [ ] **Step 4: Run GREEN**

Run: `mvn -q -pl apartment-house-service -Dtest=HybridRecommendationScorerTest test`

Expected: pass.

### Task 5: Verification

**Files:**
- No new source files.

- [ ] **Step 1: Run focused FM tests**

Run: `mvn -q -pl apartment-house-service -Dtest=FmScorerTest,FmFeatureBuilderTest,FmModelLoaderTest,HybridRecommendationScorerTest test`

Expected: pass.

- [ ] **Step 2: Run house-service package**

Run: `mvn -q -pl apartment-house-service -DskipTests package`

Expected: pass.

- [ ] **Step 3: Run full project package**

Run: `mvn -q -DskipTests package`

Expected: pass.

### Self-Review

- Spec coverage: FM scorer, feature builder, model loader, hybrid integration, and verification are covered.
- Placeholder scan: no `TBD` or open-ended implementation placeholders remain.
- Type consistency: all Java classes use `RecommendationCandidate`, `UserPreferenceSnapshot`, and `RecommendationScore` already created by V1.
