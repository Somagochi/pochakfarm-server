package com.somagochi.pochakfarm.achievement.application.reward;

import com.somagochi.pochakfarm.achievement.domain.AchievementReward;
import com.somagochi.pochakfarm.achievement.domain.RewardType;
import com.somagochi.pochakfarm.badge.application.BadgeGrantService;
import com.somagochi.pochakfarm.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BadgeRewardGranter implements RewardGranter {

  private final BadgeGrantService badgeGrantService;

  @Override
  public boolean supports(RewardType rewardType) {
    return rewardType == RewardType.BADGE;
  }

  @Override
  public void grant(User user, AchievementReward reward) {
    badgeGrantService.grant(user.getId(), reward.getReferenceCode());
  }
}
