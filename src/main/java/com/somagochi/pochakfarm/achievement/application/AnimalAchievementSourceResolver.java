package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.AchievementMetric;
import com.somagochi.pochakfarm.achievement.domain.AchievementSource;
import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.common.entity.EntityChangeType;
import com.somagochi.pochakfarm.common.entity.EntityChangedEvent;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnimalAchievementSourceResolver implements AchievementSourceResolver {

  private static final Set<Class<? extends AchievementSource>> SOURCE_TYPES = Set.of(Animal.class);
  private static final Set<AchievementMetric> CREATED_METRICS =
      Set.of(
          AchievementMetric.PLACED_ANIMAL_COUNT,
          AchievementMetric.MAX_OWNED_COUNT_PER_TYPE,
          AchievementMetric.OWNED_TYPE_COUNT,
          AchievementMetric.ONLY_START_END_PLACED);
  private static final Set<AchievementMetric> POSITION_METRICS =
      Set.of(AchievementMetric.ONLY_START_END_PLACED);

  private final FarmSpaceRepository farmSpaceRepository;

  @Override
  public Set<Class<? extends AchievementSource>> sourceTypes() {
    return SOURCE_TYPES;
  }

  @Override
  public Optional<AchievementEvaluationRequest> resolve(EntityChangedEvent event) {
    if (!(event.entity() instanceof Animal animal) || animal.getSpaceId() == null) {
      return Optional.empty();
    }
    Set<AchievementMetric> metrics = metricsOf(event.changeType());
    return farmSpaceRepository
        .findById(animal.getSpaceId())
        .map(space -> new AchievementEvaluationRequest(space.getUserId(), metrics));
  }

  private Set<AchievementMetric> metricsOf(EntityChangeType changeType) {
    return changeType == EntityChangeType.CREATED ? CREATED_METRICS : POSITION_METRICS;
  }
}
