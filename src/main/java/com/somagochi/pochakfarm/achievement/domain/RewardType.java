package com.somagochi.pochakfarm.achievement.domain;

public enum RewardType {
  COIN,
  EXPERIENCE,
  BADGE;

  public boolean supports(String referenceCode, Long amount) {
    return switch (this) {
      case COIN, EXPERIENCE -> referenceCode == null && amount != null && amount > 0;
      case BADGE -> referenceCode != null && !referenceCode.isBlank() && amount == null;
    };
  }
}
