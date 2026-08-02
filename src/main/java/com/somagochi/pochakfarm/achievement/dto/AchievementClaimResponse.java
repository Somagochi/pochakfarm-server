package com.somagochi.pochakfarm.achievement.dto;

import com.somagochi.pochakfarm.user.domain.User;
import java.util.List;

public record AchievementClaimResponse(
    String code, List<AchievementRewardResponse> rewards, long coins, long experience) {

  public static AchievementClaimResponse of(
      String code, List<AchievementRewardResponse> rewards, User user) {
    return new AchievementClaimResponse(code, rewards, user.getCoins(), user.getExperience());
  }
}
