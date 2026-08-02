package com.somagochi.pochakfarm.achievement.domain;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;

public enum AchievementMetric {
  USER_LEVEL,
  CAPTURE_COUNT_BY_CARD_TYPE,
  CAPTURE_COUNT_BY_TIER;

  public boolean supports(String metricParam) {
    return switch (this) {
      case USER_LEVEL -> metricParam == null;
      case CAPTURE_COUNT_BY_CARD_TYPE -> isConstantOf(CardType.class, metricParam);
      case CAPTURE_COUNT_BY_TIER -> isConstantOf(Tier.class, metricParam);
    };
  }

  public long extract(AchievementStats stats, String metricParam) {
    return switch (this) {
      case USER_LEVEL -> stats.userLevel();
      case CAPTURE_COUNT_BY_CARD_TYPE -> stats.captureCountOf(CardType.valueOf(metricParam));
      case CAPTURE_COUNT_BY_TIER -> stats.captureCountOf(Tier.valueOf(metricParam));
    };
  }

  private static <E extends Enum<E>> boolean isConstantOf(Class<E> type, String name) {
    if (name == null) {
      return false;
    }
    for (E constant : type.getEnumConstants()) {
      if (constant.name().equals(name)) {
        return true;
      }
    }
    return false;
  }
}
