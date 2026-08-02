package com.somagochi.pochakfarm.user.domain;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LevelRewardPolicy {

  private static final int MAX_LEVEL = 50;
  private static final long NORMAL_LEVEL_UP_COINS = 500L;
  private static final Map<Integer, Long> SPECIAL_LEVEL_UP_COINS =
      Map.of(5, 1_000L, 10, 2_000L, 20, 2_000L, 30, 2_500L, 40, 3_000L, 50, 5_000L);

  public LevelReward calculate(int currentLevel, long currentExperience, long experienceReward) {
    int level = currentLevel;
    long experience = currentExperience;
    long remaining = experienceReward;
    long appliedExperience = 0;
    long coinReward = 0;

    while (level < MAX_LEVEL && remaining > 0) {
      long required = requiredExperience(level);
      long applied = Math.min(remaining, required - experience);
      experience += applied;
      remaining -= applied;
      appliedExperience += applied;

      if (experience == required) {
        level++;
        experience = 0;
        coinReward += coinsFor(level);
      }
    }

    if (level == MAX_LEVEL) {
      experience = 0;
    }
    long nextRequired = requiredExperienceForNextLevel(level);
    return new LevelReward(
        appliedExperience, currentLevel, level, experience, nextRequired, coinReward);
  }

  public long requiredExperienceForNextLevel(int level) {
    return level >= MAX_LEVEL ? 0 : requiredExperience(level);
  }

  private long requiredExperience(int level) {
    return 40L + 10L * (level - 1L);
  }

  private long coinsFor(int reachedLevel) {
    return SPECIAL_LEVEL_UP_COINS.getOrDefault(reachedLevel, NORMAL_LEVEL_UP_COINS);
  }
}
