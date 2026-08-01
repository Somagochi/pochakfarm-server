package com.somagochi.pochakfarm.achievement.dto;

import com.somagochi.pochakfarm.achievement.domain.Achievement;
import com.somagochi.pochakfarm.achievement.domain.AchievementCategory;
import com.somagochi.pochakfarm.achievement.domain.UserAchievement;
import java.time.Instant;
import java.util.List;

public record AchievementResponse(
    String code,
    String title,
    String description,
    AchievementCategory category,
    long current,
    long target,
    boolean achieved,
    Instant achievedAt,
    boolean rewardClaimed,
    List<AchievementRewardResponse> rewards) {

  public static AchievementResponse of(
      Achievement achievement,
      long current,
      UserAchievement record,
      List<AchievementRewardResponse> rewards) {
    return new AchievementResponse(
        achievement.getCode(),
        achievement.getTitle(),
        achievement.getDescription(),
        achievement.getCategory(),
        current,
        achievement.getTargetValue(),
        record != null,
        record == null ? null : record.getAchievedAt(),
        record != null && record.isRewardClaimed(),
        rewards);
  }
}
