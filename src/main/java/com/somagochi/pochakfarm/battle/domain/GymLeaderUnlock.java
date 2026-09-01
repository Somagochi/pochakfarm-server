package com.somagochi.pochakfarm.battle.domain;

public record GymLeaderUnlock(
    int requiredLevel,
    boolean levelSatisfied,
    String previousBadgeCode,
    boolean previousBadgeSatisfied) {

  public boolean isUnlocked() {
    return levelSatisfied && previousBadgeSatisfied;
  }
}
