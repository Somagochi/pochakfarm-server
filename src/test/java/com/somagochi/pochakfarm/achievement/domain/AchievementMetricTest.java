package com.somagochi.pochakfarm.achievement.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AchievementMetricTest {

  private final AchievementStats stats =
      new AchievementStats(
          7, Map.of(CardType.SEA, 3L, CardType.SKY, 11L), Map.of(Tier.S, 5L, Tier.SSS, 1L));

  @Test
  void extractsEachMetricFromSingleSnapshot() {
    assertEquals(7L, AchievementMetric.USER_LEVEL.extract(stats, null));
    assertEquals(11L, AchievementMetric.CAPTURE_COUNT_BY_CARD_TYPE.extract(stats, "SKY"));
    assertEquals(5L, AchievementMetric.CAPTURE_COUNT_BY_TIER.extract(stats, "S"));
  }

  @Test
  void extractsZeroForAbsentKey() {
    assertEquals(0L, AchievementMetric.CAPTURE_COUNT_BY_CARD_TYPE.extract(stats, "SPACE"));
    assertEquals(0L, AchievementMetric.CAPTURE_COUNT_BY_TIER.extract(stats, "SS"));
  }

  @Test
  void supportsOnlyMatchingMetricParam() {
    assertTrue(AchievementMetric.USER_LEVEL.supports(null));
    assertFalse(AchievementMetric.USER_LEVEL.supports("SEA"));

    assertTrue(AchievementMetric.CAPTURE_COUNT_BY_CARD_TYPE.supports("SEA"));
    assertFalse(AchievementMetric.CAPTURE_COUNT_BY_CARD_TYPE.supports("S"));
    assertFalse(AchievementMetric.CAPTURE_COUNT_BY_CARD_TYPE.supports("sea"));
    assertFalse(AchievementMetric.CAPTURE_COUNT_BY_CARD_TYPE.supports(null));

    assertTrue(AchievementMetric.CAPTURE_COUNT_BY_TIER.supports("SSS"));
    assertFalse(AchievementMetric.CAPTURE_COUNT_BY_TIER.supports("SEA"));
    assertFalse(AchievementMetric.CAPTURE_COUNT_BY_TIER.supports(null));
  }

  @Test
  void rejectsInvalidDefinitionBeforeExtraction() {
    Achievement broken =
        Achievement.create(
            "BROKEN",
            "잘못된 정의",
            null,
            AchievementCategory.CARD_TYPE,
            AchievementMetric.CAPTURE_COUNT_BY_CARD_TYPE,
            "OCEAN",
            10);

    assertFalse(broken.isDefinitionValid());
  }
}
