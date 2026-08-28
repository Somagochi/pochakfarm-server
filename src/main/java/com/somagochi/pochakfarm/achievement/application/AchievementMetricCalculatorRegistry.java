package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import com.somagochi.pochakfarm.achievement.domain.AchievementMetricValues;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

@Component
public class AchievementMetricCalculatorRegistry implements SmartInitializingSingleton {

  // TODO: Redis 도입 시 cache-aside로 조회하고, source commit 뒤 평가 전에 invalidate한다.
  // Redis miss/장애 시에는 현재 DB 계산기로 fallback한다.
  private final List<AchievementMetricCalculator> calculators;

  public AchievementMetricCalculatorRegistry(List<AchievementMetricCalculator> calculators) {
    this.calculators = List.copyOf(calculators);
  }

  public AchievementMetricValues calculate(
      Long userId, Collection<AchievementMetric> requestedMetrics) {
    if (requestedMetrics.isEmpty()) {
      return new AchievementMetricValues(Map.of());
    }
    Set<AchievementMetric> requested = EnumSet.copyOf(requestedMetrics);
    Map<AchievementMetric, Long> values = new EnumMap<>(AchievementMetric.class);
    for (AchievementMetricCalculator calculator : calculators) {
      Set<AchievementMetric> matching = EnumSet.copyOf(calculator.supportedMetrics());
      matching.retainAll(requested);
      if (!matching.isEmpty()) {
        values.putAll(calculator.calculate(userId, matching));
      }
    }
    if (!values.keySet().containsAll(requested)) {
      Set<AchievementMetric> missing = EnumSet.copyOf(requested);
      missing.removeAll(values.keySet());
      throw new IllegalStateException("Achievement metric value is missing: " + missing);
    }
    return new AchievementMetricValues(values);
  }

  @Override
  public void afterSingletonsInstantiated() {
    Map<AchievementMetric, Integer> supportCounts = new EnumMap<>(AchievementMetric.class);
    calculators.forEach(
        calculator ->
            calculator
                .supportedMetrics()
                .forEach(metric -> supportCounts.merge(metric, 1, Integer::sum)));
    Set<AchievementMetric> invalid = EnumSet.noneOf(AchievementMetric.class);
    for (AchievementMetric metric : AchievementMetric.values()) {
      if (supportCounts.getOrDefault(metric, 0) != 1) {
        invalid.add(metric);
      }
    }
    if (!invalid.isEmpty()) {
      throw new IllegalStateException(
          "Each achievement metric must have exactly one calculator: " + invalid);
    }
  }
}
