package com.somagochi.pochakfarm.achievement.application.reward;

import com.somagochi.pochakfarm.achievement.domain.AchievementReward;
import com.somagochi.pochakfarm.achievement.domain.RewardType;
import com.somagochi.pochakfarm.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public class ExperienceRewardGranter implements RewardGranter {

  @Override
  public boolean supports(RewardType rewardType) {
    return rewardType == RewardType.EXPERIENCE;
  }

  @Override
  public void grant(User user, AchievementReward reward) {
    user.addExperience(reward.getAmount());
  }
}
