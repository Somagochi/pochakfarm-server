package com.somagochi.pochakfarm.achievement.domain;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import java.util.Map;

public record AchievementStats(
    int userLevel, Map<CardType, Long> captureCountByCardType, Map<Tier, Long> captureCountByTier) {

  public AchievementStats {
    captureCountByCardType = Map.copyOf(captureCountByCardType);
    captureCountByTier = Map.copyOf(captureCountByTier);
  }

  public long captureCountOf(CardType cardType) {
    return captureCountByCardType.getOrDefault(cardType, 0L);
  }

  public long captureCountOf(Tier tier) {
    return captureCountByTier.getOrDefault(tier, 0L);
  }
}
