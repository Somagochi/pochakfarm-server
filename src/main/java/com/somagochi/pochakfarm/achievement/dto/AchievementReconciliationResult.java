package com.somagochi.pochakfarm.achievement.dto;

import java.util.List;

public record AchievementReconciliationResult(
    int requestedCount, int distinctCount, int succeededCount, List<Long> failedUserIds) {

  public AchievementReconciliationResult {
    failedUserIds = List.copyOf(failedUserIds);
  }
}
