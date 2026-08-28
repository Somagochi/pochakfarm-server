package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import java.util.Map;
import java.util.Set;

public interface AchievementMetricCalculator {

  Set<AchievementMetric> supportedMetrics();

  Map<AchievementMetric, Long> calculate(Long userId, Set<AchievementMetric> requestedMetrics);
}
