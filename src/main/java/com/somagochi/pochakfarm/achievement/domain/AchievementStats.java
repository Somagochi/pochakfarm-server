package com.somagochi.pochakfarm.achievement.domain;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import java.util.Map;

public record AchievementStats(
    boolean preRegistrationConverted,
    long placedAnimalCount,
    Map<CardType, Long> ownedCountByType,
    boolean onlyStartEndPlaced) {

  public AchievementStats {
    ownedCountByType = Map.copyOf(ownedCountByType);
  }

  public long maxOwnedCountPerType() {
    return ownedCountByType.values().stream().mapToLong(Long::longValue).max().orElse(0L);
  }

  public long ownedTypeCount() {
    return ownedCountByType.values().stream().filter(count -> count > 0).count();
  }
}
