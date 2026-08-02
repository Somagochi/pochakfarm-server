package com.somagochi.pochakfarm.achievement.application.reward;

import com.somagochi.pochakfarm.achievement.domain.AchievementReward;
import com.somagochi.pochakfarm.achievement.domain.RewardType;
import com.somagochi.pochakfarm.user.domain.User;

public interface RewardGranter {

  boolean supports(RewardType rewardType);

  void grant(User user, AchievementReward reward);
}
