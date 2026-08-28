package com.somagochi.pochakfarm.achievement.domain;

import java.util.Map;

public record AchievementMetricValues(Map<AchievementMetric, Long> values) {

  public AchievementMetricValues {
    values = Map.copyOf(values);
  }

  public long get(AchievementMetric metric) {
    return values.getOrDefault(metric, 0L);
  }
}
