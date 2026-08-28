package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import com.somagochi.pochakfarm.achievement.domain.AchievementMetricValues;
import java.util.Collection;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AchievementStatsLoader {

  private final AchievementMetricCalculatorRegistry calculatorRegistry;

  public AchievementMetricValues load(Long userId) {
    return load(userId, EnumSet.allOf(AchievementMetric.class));
  }

  public AchievementMetricValues load(Long userId, Collection<AchievementMetric> metrics) {
    return calculatorRegistry.calculate(userId, metrics);
  }
}
