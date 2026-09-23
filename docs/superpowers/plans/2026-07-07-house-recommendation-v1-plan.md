# House Recommendation V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a first-version rental-scene recommendation feature to apartment-house-service using behavior collection, user preference profiles, and an explainable rule-based scorer.

**Architecture:** Keep recommendation V1 inside apartment-house-service. Store user behavior and user preferences in MySQL, compute candidate scores in Java, and expose `/house/recommend` plus behavior/preference endpoints. The scorer is pure Java and unit-tested first so future FM/offline ranking can replace it behind the same service boundary.

**Tech Stack:** Spring Boot 3, MyBatis-Plus, MySQL, JUnit 5, Dockerized existing house-service.

---

### Task 1: Pure Recommendation Scorer

**Files:**
- Create: `apartment-house-service/src/test/java/com/example/house/recommendation/RecommendationScorerTest.java`
- Create: `apartment-house-service/src/main/java/com/example/house/recommendation/RecommendationScorer.java`
- Create: `apartment-house-service/src/main/java/com/example/house/recommendation/RecommendationCandidate.java`
- Create: `apartment-house-service/src/main/java/com/example/house/recommendation/UserPreferenceSnapshot.java`
- Create: `apartment-house-service/src/main/java/com/example/house/recommendation/RecommendationScore.java`

- [ ] Write tests proving district, price, room count, area, popularity, freshness, and availability affect the score.
- [ ] Run the test and verify it fails because scorer classes do not exist.
- [ ] Implement minimal scorer and value objects.
- [ ] Run the scorer test and verify it passes.

### Task 2: Data Model and SQL

**Files:**
- Create: `apartment-house-service/src/main/java/com/example/house/entity/UserBehavior.java`
- Create: `apartment-house-service/src/main/java/com/example/house/entity/UserPreference.java`
- Create: `apartment-house-service/src/main/java/com/example/house/mapper/UserBehaviorMapper.java`
- Create: `apartment-house-service/src/main/java/com/example/house/mapper/UserPreferenceMapper.java`
- Create: `sql/recommendation-v1.sql`

- [ ] Add behavior and preference entities.
- [ ] Add mappers.
- [ ] Add SQL migration for `user_behavior` and `user_preference` tables.

### Task 3: Recommendation API

**Files:**
- Create: `apartment-house-service/src/main/java/com/example/house/dto/BehaviorTrackDTO.java`
- Create: `apartment-house-service/src/main/java/com/example/house/dto/UserPreferenceDTO.java`
- Create: `apartment-house-service/src/main/java/com/example/house/dto/HouseRecommendDTO.java`
- Create: `apartment-house-service/src/main/java/com/example/house/service/RecommendationService.java`
- Create: `apartment-house-service/src/main/java/com/example/house/service/impl/RecommendationServiceImpl.java`
- Modify: `apartment-house-service/src/main/java/com/example/house/controller/HouseController.java`

- [ ] Add `/house/behavior` POST for behavior tracking.
- [ ] Add `/house/preference` PUT for user preference upsert.
- [ ] Add `/house/recommend` GET for Top-K recommended houses.
- [ ] Keep the controller response format consistent with existing `Result` and `PageResult`.

### Task 4: Verification

**Files:**
- Existing Maven project files.

- [ ] Run `mvn -q -pl apartment-house-service -DskipTests=false test`.
- [ ] Run `mvn -q -DskipTests package`.
- [ ] Report remaining runtime checks that require MySQL data and Docker.
