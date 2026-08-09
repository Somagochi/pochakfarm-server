package com.somagochi.pochakfarm.achievement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.somagochi.pochakfarm.achievement.domain.AchievementReward;
import com.somagochi.pochakfarm.achievement.domain.RewardType;
import com.somagochi.pochakfarm.badge.dto.BadgeResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AchievementRewardResponse(
    RewardType type, Long amount, String badgeName, String badgeImageUrl) {

  public static AchievementRewardResponse of(AchievementReward reward, BadgeResponse badge) {
    if (badge == null) {
      return new AchievementRewardResponse(reward.getRewardType(), reward.getAmount(), null, null);
    }
    return new AchievementRewardResponse(
        reward.getRewardType(), reward.getAmount(), badge.name(), badge.imageUrl());
  }
}
