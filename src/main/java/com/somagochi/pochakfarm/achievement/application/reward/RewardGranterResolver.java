package com.somagochi.pochakfarm.achievement.application.reward;

import com.somagochi.pochakfarm.achievement.domain.AchievementReward;
import com.somagochi.pochakfarm.achievement.domain.RewardType;
import com.somagochi.pochakfarm.user.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RewardGranterResolver {

  private final List<RewardGranter> rewardGranters;

  public void grant(User user, AchievementReward reward) {
    resolveGranter(reward.getRewardType()).grant(user, reward);
  }

  private RewardGranter resolveGranter(RewardType rewardType) {
    return rewardGranters.stream()
        .filter(granter -> granter.supports(rewardType))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("RewardGranter not found for " + rewardType));
  }
}
