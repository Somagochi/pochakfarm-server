package com.somagochi.pochakfarm.achievement.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.common.entity.EntityChangeType;
import com.somagochi.pochakfarm.common.entity.EntityChangedEvent;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class AchievementHandlerTest {

  private final AchievementSourceResolverRegistry resolverRegistry =
      mock(AchievementSourceResolverRegistry.class);
  private final AchievementEvaluator achievementEvaluator = mock(AchievementEvaluator.class);
  private final AchievementHandler achievementHandler =
      new AchievementHandler(resolverRegistry, achievementEvaluator);

  @Test
  void evaluatesEveryMetricResolvedForTheSameUser() {
    EntityChangedEvent event = new EntityChangedEvent(mock(Animal.class), EntityChangeType.CREATED);
    given(resolverRegistry.resolve(event))
        .willReturn(
            List.of(
                new AchievementEvaluationRequest(1L, Set.of(AchievementMetric.PLACED_ANIMAL_COUNT)),
                new AchievementEvaluationRequest(
                    1L,
                    Set.of(
                        AchievementMetric.MAX_OWNED_COUNT_PER_TYPE,
                        AchievementMetric.OWNED_TYPE_COUNT))));

    achievementHandler.handle(event);

    verify(achievementEvaluator)
        .evaluate(
            ArgumentMatchers.eq(1L),
            ArgumentMatchers.argThat(
                metrics ->
                    Set.copyOf(metrics)
                        .equals(
                            Set.of(
                                AchievementMetric.PLACED_ANIMAL_COUNT,
                                AchievementMetric.MAX_OWNED_COUNT_PER_TYPE,
                                AchievementMetric.OWNED_TYPE_COUNT))));
  }
}
