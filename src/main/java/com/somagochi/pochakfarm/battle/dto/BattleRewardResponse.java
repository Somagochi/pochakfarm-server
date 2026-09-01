package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.battle.domain.GymLeaderClear;
import com.somagochi.pochakfarm.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관장전 결과 보상")
public record BattleRewardResponse(
    boolean firstClear,
    boolean rewardGranted,
    long gymLeaderCoins,
    long experience,
    String badgeCode,
    boolean levelUp,
    int levelBefore,
    int levelAfter,
    long experienceAfter,
    long requiredExperienceForNextLevel,
    long levelUpCoins,
    long coinsAfter) {

  public static BattleRewardResponse granted(GymLeaderClear clear) {
    return new BattleRewardResponse(
        true,
        true,
        clear.getGymLeaderCoinReward(),
        clear.getExperienceReward(),
        clear.getBadgeCode(),
        clear.getLevelAfter() > clear.getLevelBefore(),
        clear.getLevelBefore(),
        clear.getLevelAfter(),
        clear.getExperienceAfter(),
        clear.getRequiredExperienceForNextLevel(),
        clear.getLevelUpCoinReward(),
        clear.getCoinsAfter());
  }

  public static BattleRewardResponse notGranted(User user, long requiredExperienceForNextLevel) {
    return new BattleRewardResponse(
        false,
        false,
        0,
        0,
        null,
        false,
        user.getLevel(),
        user.getLevel(),
        user.getExperience(),
        requiredExperienceForNextLevel,
        0,
        user.getCoins());
  }
}
