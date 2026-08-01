package com.somagochi.pochakfarm.achievement.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AchievementTest {

  private final Achievement levelTen =
      Achievement.create(
          "LEVEL_10",
          "레벨 10 달성",
          null,
          AchievementCategory.LEVEL,
          AchievementMetric.USER_LEVEL,
          null,
          10);

  @Test
  void reportsRawProgressEvenAboveTarget() {
    assertEquals(3L, levelTen.progressOf(stats(3)));
    assertEquals(42L, levelTen.progressOf(stats(42)));
  }

  @Test
  void isSatisfiedWhenProgressReachesTarget() {
    assertFalse(levelTen.isSatisfiedBy(stats(9)));
    assertTrue(levelTen.isSatisfiedBy(stats(10)));
    assertTrue(levelTen.isSatisfiedBy(stats(11)));
  }

  @Test
  void enabledAchievementIsAlwaysListed() {
    assertTrue(levelTen.isListedWhen(false));
    assertTrue(levelTen.isListedWhen(true));
  }

  @Test
  void rejectsNonPositiveTarget() {
    Achievement zeroTarget =
        Achievement.create(
            "LEVEL_ZERO",
            "잘못된 목표",
            null,
            AchievementCategory.LEVEL,
            AchievementMetric.USER_LEVEL,
            null,
            0);

    assertFalse(zeroTarget.isDefinitionValid());
  }

  private AchievementStats stats(int level) {
    return new AchievementStats(level, Map.<CardType, Long>of(), Map.<Tier, Long>of());
  }
}
