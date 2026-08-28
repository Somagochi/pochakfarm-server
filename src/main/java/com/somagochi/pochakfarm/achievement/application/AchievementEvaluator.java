package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.Achievement;
import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import com.somagochi.pochakfarm.achievement.domain.AchievementMetricValues;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.AchievementRepository;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.UserAchievementRepository;
import com.somagochi.pochakfarm.user.application.UserQueryService;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AchievementEvaluator {

  private final AchievementRepository achievementRepository;
  private final UserAchievementRepository userAchievementRepository;
  private final AchievementStatsLoader achievementStatsLoader;
  private final AchievementRecorder achievementRecorder;
  private final UserQueryService userQueryService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int evaluate(Long userId, Collection<AchievementMetric> metrics) {
    if (metrics.isEmpty()) {
      return 0;
    }
    userQueryService.getForUpdate(userId);
    return evaluateCandidates(userId, achievementRepository.findByEnabledTrueAndMetricIn(metrics));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int reconcile(Long userId) {
    userQueryService.getForUpdate(userId);
    return evaluateCandidates(userId, achievementRepository.findByEnabledTrue());
  }

  private int evaluateCandidates(Long userId, Collection<Achievement> definitions) {
    Set<Long> achievedIds = new HashSet<>();
    userAchievementRepository
        .findByUserId(userId)
        .forEach(record -> achievedIds.add(record.getAchievementId()));
    List<Achievement> candidates =
        definitions.stream()
            .filter(Achievement::isDefinitionValid)
            .filter(achievement -> !achievedIds.contains(achievement.getId()))
            .toList();
    if (candidates.isEmpty()) {
      return 0;
    }

    Set<AchievementMetric> requiredMetrics = new HashSet<>();
    candidates.forEach(achievement -> requiredMetrics.add(achievement.getMetric()));
    AchievementMetricValues metricValues = achievementStatsLoader.load(userId, requiredMetrics);
    List<Long> achievedAchievementIds =
        candidates.stream()
            .filter(achievement -> achievement.isSatisfiedBy(metricValues))
            .map(Achievement::getId)
            .toList();
    achievementRecorder.record(userId, achievedAchievementIds);
    return achievedAchievementIds.size();
  }
}
