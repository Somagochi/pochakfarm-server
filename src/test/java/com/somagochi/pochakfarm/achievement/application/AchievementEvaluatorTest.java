package com.somagochi.pochakfarm.achievement.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.somagochi.pochakfarm.achievement.domain.Achievement;
import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import com.somagochi.pochakfarm.achievement.domain.UserAchievement;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.AchievementRepository;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.UserAchievementRepository;
import com.somagochi.pochakfarm.user.application.UserQueryService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AchievementEvaluatorTest {

  private static final Long USER_ID = 1L;
  private static final Long ACHIEVEMENT_ID = 10L;

  private final AchievementRepository achievementRepository = mock(AchievementRepository.class);
  private final UserAchievementRepository userAchievementRepository =
      mock(UserAchievementRepository.class);
  private final AchievementStatsLoader achievementStatsLoader = mock(AchievementStatsLoader.class);
  private final AchievementRecorder achievementRecorder = mock(AchievementRecorder.class);
  private final UserQueryService userQueryService = mock(UserQueryService.class);
  private final AchievementEvaluator achievementEvaluator =
      new AchievementEvaluator(
          achievementRepository,
          userAchievementRepository,
          achievementStatsLoader,
          achievementRecorder,
          userQueryService);

  @Test
  void skipsStatsAggregationWhenAllRelatedAchievementsAreAlreadyAchieved() {
    Achievement achievement = mock(Achievement.class);
    UserAchievement achieved = UserAchievement.achieve(USER_ID, ACHIEVEMENT_ID);
    given(userAchievementRepository.findByUserId(USER_ID)).willReturn(List.of(achieved));
    given(
            achievementRepository.findByEnabledTrueAndMetricIn(
                Set.of(AchievementMetric.ONLY_START_END_PLACED)))
        .willReturn(List.of(achievement));
    given(achievement.isDefinitionValid()).willReturn(true);
    given(achievement.getId()).willReturn(ACHIEVEMENT_ID);

    achievementEvaluator.evaluate(USER_ID, Set.of(AchievementMetric.ONLY_START_END_PLACED));

    verifyNoInteractions(achievementStatsLoader, achievementRecorder);
  }
}
