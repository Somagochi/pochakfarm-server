package com.somagochi.pochakfarm.achievement.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AchievementMetricTest {

  private final AchievementStats stats =
      new AchievementStats(
          true, 3, Map.of(CardType.GROUND, 7L, CardType.SKY, 2L, CardType.SEA, 0L), false);

  @Test
  void supportsOnlyNullMetricParam() {
    for (AchievementMetric metric : AchievementMetric.values()) {
      assertTrue(metric.supports(null));
      assertFalse(metric.supports("GROUND"));
    }
  }

  @Test
  void extractsPreRegistrationConversionAsBinaryProgress() {
    assertEquals(1L, AchievementMetric.PRE_REGISTRATION_CONVERTED.extract(stats, null));
    assertEquals(
        0L,
        AchievementMetric.PRE_REGISTRATION_CONVERTED.extract(
            new AchievementStats(false, 0, Map.of(), false), null));
  }

  @Test
  void extractsPlacedAnimalCount() {
    assertEquals(3L, AchievementMetric.PLACED_ANIMAL_COUNT.extract(stats, null));
  }

  @Test
  void extractsMaxOwnedCountAmongTypes() {
    assertEquals(7L, AchievementMetric.MAX_OWNED_COUNT_PER_TYPE.extract(stats, null));
    assertEquals(
        0L,
        AchievementMetric.MAX_OWNED_COUNT_PER_TYPE.extract(
            new AchievementStats(false, 0, Map.of(), false), null));
  }

  @Test
  void countsOnlyTypesWithAtLeastOneAnimal() {
    assertEquals(2L, AchievementMetric.OWNED_TYPE_COUNT.extract(stats, null));
  }

  @Test
  void extractsStartEndPlacementAsBinaryProgress() {
    assertEquals(0L, AchievementMetric.ONLY_START_END_PLACED.extract(stats, null));
    assertEquals(
        1L,
        AchievementMetric.ONLY_START_END_PLACED.extract(
            new AchievementStats(false, 2, Map.of(), true), null));
  }
}
