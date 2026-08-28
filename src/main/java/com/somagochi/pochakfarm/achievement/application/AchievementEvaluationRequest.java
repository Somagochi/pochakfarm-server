package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import java.util.Objects;
import java.util.Set;

public record AchievementEvaluationRequest(Long userId, Set<AchievementMetric> metrics) {

  public AchievementEvaluationRequest {
    Objects.requireNonNull(userId);
    metrics = Set.copyOf(metrics);
  }
}
