package com.somagochi.pochakfarm.achievement.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import com.somagochi.pochakfarm.achievement.domain.AchievementMetricValues;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AchievementMetricCalculatorRegistryTest {

  @Test
  void calculatesEveryRequestedMetricAcrossCalculators() {
    AchievementMetricCalculator animal = mock(AchievementMetricCalculator.class);
    AchievementMetricCalculator coupon = mock(AchievementMetricCalculator.class);
    given(animal.supportedMetrics())
        .willReturn(
            Set.of(
                AchievementMetric.PLACED_ANIMAL_COUNT, AchievementMetric.MAX_OWNED_COUNT_PER_TYPE));
    given(coupon.supportedMetrics())
        .willReturn(Set.of(AchievementMetric.PRE_REGISTRATION_CONVERTED));
    given(
            animal.calculate(
                1L,
                Set.of(
                    AchievementMetric.PLACED_ANIMAL_COUNT,
                    AchievementMetric.MAX_OWNED_COUNT_PER_TYPE)))
        .willReturn(
            Map.of(
                AchievementMetric.PLACED_ANIMAL_COUNT,
                3L,
                AchievementMetric.MAX_OWNED_COUNT_PER_TYPE,
                2L));
    given(coupon.calculate(1L, Set.of(AchievementMetric.PRE_REGISTRATION_CONVERTED)))
        .willReturn(Map.of(AchievementMetric.PRE_REGISTRATION_CONVERTED, 1L));
    AchievementMetricCalculatorRegistry registry =
        new AchievementMetricCalculatorRegistry(List.of(animal, coupon));

    AchievementMetricValues values =
        registry.calculate(
            1L,
            Set.of(
                AchievementMetric.PRE_REGISTRATION_CONVERTED,
                AchievementMetric.PLACED_ANIMAL_COUNT,
                AchievementMetric.MAX_OWNED_COUNT_PER_TYPE));

    assertEquals(1L, values.get(AchievementMetric.PRE_REGISTRATION_CONVERTED));
    assertEquals(3L, values.get(AchievementMetric.PLACED_ANIMAL_COUNT));
    assertEquals(2L, values.get(AchievementMetric.MAX_OWNED_COUNT_PER_TYPE));
  }

  @Test
  void failsStartupUnlessEveryMetricHasExactlyOneCalculator() {
    AchievementMetricCalculator incomplete = mock(AchievementMetricCalculator.class);
    given(incomplete.supportedMetrics())
        .willReturn(EnumSet.of(AchievementMetric.PRE_REGISTRATION_CONVERTED));
    AchievementMetricCalculatorRegistry registry =
        new AchievementMetricCalculatorRegistry(List.of(incomplete));

    assertThrows(IllegalStateException.class, registry::afterSingletonsInstantiated);
  }
}
