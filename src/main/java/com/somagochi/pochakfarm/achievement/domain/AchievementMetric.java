package com.somagochi.pochakfarm.achievement.domain;

public enum AchievementMetric {
  PRE_REGISTRATION_CONVERTED,
  PLACED_ANIMAL_COUNT,
  MAX_OWNED_COUNT_PER_TYPE,
  OWNED_TYPE_COUNT,
  ONLY_START_END_PLACED;

  public boolean supports(String metricParam) {
    return metricParam == null;
  }

  public long extract(AchievementStats stats, String metricParam) {
    return switch (this) {
      case PRE_REGISTRATION_CONVERTED -> stats.preRegistrationConverted() ? 1 : 0;
      case PLACED_ANIMAL_COUNT -> stats.placedAnimalCount();
      case MAX_OWNED_COUNT_PER_TYPE -> stats.maxOwnedCountPerType();
      case OWNED_TYPE_COUNT -> stats.ownedTypeCount();
      case ONLY_START_END_PLACED -> stats.onlyStartEndPlaced() ? 1 : 0;
    };
  }
}
