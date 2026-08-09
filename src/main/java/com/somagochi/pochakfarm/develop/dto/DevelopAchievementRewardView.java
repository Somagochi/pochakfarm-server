package com.somagochi.pochakfarm.develop.dto;

import com.somagochi.pochakfarm.achievement.domain.AchievementReward;
import com.somagochi.pochakfarm.achievement.domain.RewardType;
import com.somagochi.pochakfarm.badge.domain.Badge;

public record DevelopAchievementRewardView(
    Long id,
    RewardType rewardType,
    Long amount,
    String badgeCode,
    String badgeName,
    String badgeImageUrl) {

  public static DevelopAchievementRewardView of(
      AchievementReward reward, Badge badge, String badgeImageUrl) {
    return new DevelopAchievementRewardView(
        reward.getId(),
        reward.getRewardType(),
        reward.getAmount(),
        badge == null ? reward.getReferenceCode() : badge.getCode(),
        badge == null ? null : badge.getName(),
        badgeImageUrl);
  }

  public boolean isBadge() {
    return rewardType == RewardType.BADGE;
  }
}
