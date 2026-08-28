package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.AchievementSource;
import com.somagochi.pochakfarm.common.entity.EntityChangedEvent;
import java.util.Optional;
import java.util.Set;

public interface AchievementSourceResolver {

  Set<Class<? extends AchievementSource>> sourceTypes();

  Optional<AchievementEvaluationRequest> resolve(EntityChangedEvent event);
}
