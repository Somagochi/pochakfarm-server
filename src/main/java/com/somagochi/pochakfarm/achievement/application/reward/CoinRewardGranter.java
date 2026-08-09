package com.somagochi.pochakfarm.achievement.application.reward;

import com.somagochi.pochakfarm.achievement.domain.AchievementReward;
import com.somagochi.pochakfarm.achievement.domain.RewardType;
import com.somagochi.pochakfarm.user.application.UserCoinService;
import com.somagochi.pochakfarm.user.domain.CoinTransactionReason;
import com.somagochi.pochakfarm.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoinRewardGranter implements RewardGranter {

  private final UserCoinService userCoinService;

  @Override
  public boolean supports(RewardType rewardType) {
    return rewardType == RewardType.COIN;
  }

  @Override
  public void grant(User user, AchievementReward reward) {
    userCoinService.earn(
        user,
        reward.getAmount(),
        CoinTransactionReason.ACHIEVEMENT_REWARD,
        reward.getAchievementId());
  }
}
