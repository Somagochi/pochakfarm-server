package com.somagochi.pochakfarm.achievement.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AchievementTest {

  private final Achievement oneTypeFocus =
      Achievement.create(
          "ONE_TYPE_FOCUS",
          "한 우물 포착",
          null,
          AchievementCategory.COLLECTION,
          AchievementMetric.MAX_OWNED_COUNT_PER_TYPE,
          null,
          10);

  @Test
  void reportsRawProgressEvenAboveTarget() {
    assertEquals(3L, oneTypeFocus.progressOf(stats(Map.of(CardType.GROUND, 3L))));
    assertEquals(42L, oneTypeFocus.progressOf(stats(Map.of(CardType.GROUND, 42L))));
  }

  @Test
  void isSatisfiedWhenProgressReachesTarget() {
    assertFalse(oneTypeFocus.isSatisfiedBy(stats(Map.of(CardType.GROUND, 9L))));
    assertTrue(oneTypeFocus.isSatisfiedBy(stats(Map.of(CardType.GROUND, 10L))));
    assertTrue(oneTypeFocus.isSatisfiedBy(stats(Map.of(CardType.GROUND, 11L))));
  }

  @Test
  void extractsEachMetricFromStats() {
    AchievementStats stats =
        new AchievementStats(true, 3, Map.of(CardType.GROUND, 2L, CardType.SKY, 1L), true);

    assertEquals(1L, AchievementMetric.PRE_REGISTRATION_CONVERTED.extract(stats, null));
    assertEquals(3L, AchievementMetric.PLACED_ANIMAL_COUNT.extract(stats, null));
    assertEquals(2L, AchievementMetric.MAX_OWNED_COUNT_PER_TYPE.extract(stats, null));
    assertEquals(2L, AchievementMetric.OWNED_TYPE_COUNT.extract(stats, null));
    assertEquals(1L, AchievementMetric.ONLY_START_END_PLACED.extract(stats, null));
  }

  @Test
  void enabledAchievementIsAlwaysListed() {
    assertTrue(oneTypeFocus.isListedWhen(false));
    assertTrue(oneTypeFocus.isListedWhen(true));
  }

  @Test
  void createHiddenMarksAchievementAsHidden() {
    Achievement hidden =
        Achievement.createHidden(
            "START_AND_END",
            "시작과 끝",
            null,
            AchievementCategory.FARM,
            AchievementMetric.ONLY_START_END_PLACED,
            null,
            1);

    assertTrue(hidden.isHidden());
    assertFalse(oneTypeFocus.isHidden());
  }

  @Test
  void rejectsNonPositiveTarget() {
    Achievement zeroTarget =
        Achievement.create(
            "ZERO_TARGET",
            "잘못된 목표",
            null,
            AchievementCategory.COLLECTION,
            AchievementMetric.OWNED_TYPE_COUNT,
            null,
            0);

    assertFalse(zeroTarget.isDefinitionValid());
  }

  @Test
  void rejectsMetricParamBecauseNoMetricUsesIt() {
    Achievement withParam =
        Achievement.create(
            "WITH_PARAM",
            "잘못된 파라미터",
            null,
            AchievementCategory.COLLECTION,
            AchievementMetric.MAX_OWNED_COUNT_PER_TYPE,
            "GROUND",
            10);

    assertFalse(withParam.isDefinitionValid());
  }

  private AchievementStats stats(Map<CardType, Long> ownedCountByType) {
    return new AchievementStats(false, 0, ownedCountByType, false);
  }
}
