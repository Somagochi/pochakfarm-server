package com.somagochi.pochakfarm.user.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LevelRewardPolicyTest {

  private final LevelRewardPolicy policy = new LevelRewardPolicy();

  @Test
  void carriesExperienceAndRewardsNormalLevelUp() {
    LevelReward reward = policy.calculate(1, 39, 10);

    assertEquals(10, reward.experienceReward());
    assertEquals(1, reward.levelBefore());
    assertEquals(2, reward.levelAfter());
    assertEquals(9, reward.experienceAfter());
    assertEquals(50, reward.requiredExperienceForNextLevel());
    assertEquals(500, reward.coinReward());
  }

  @Test
  void supportsMultipleLevelUps() {
    LevelReward reward = policy.calculate(1, 39, 100);

    assertEquals(3, reward.levelAfter());
    assertEquals(49, reward.experienceAfter());
    assertEquals(60, reward.requiredExperienceForNextLevel());
    assertEquals(1_000, reward.coinReward());
  }

  @Test
  void replacesNormalCoinRewardAtSpecialLevel() {
    LevelReward reward = policy.calculate(4, 69, 1);

    assertEquals(5, reward.levelAfter());
    assertEquals(1_000, reward.coinReward());
  }

  @ParameterizedTest
  @MethodSource("specialLevelCoinCases")
  void rewardsEachSpecialLevel(int reachedLevel, long expectedCoins) {
    int currentLevel = reachedLevel - 1;
    long requiredExperience = 40L + 10L * (currentLevel - 1L);

    LevelReward reward = policy.calculate(currentLevel, requiredExperience - 1, 1);

    assertEquals(reachedLevel, reward.levelAfter());
    assertEquals(expectedCoins, reward.coinReward());
  }

  @Test
  void discardsExperienceAfterReachingLevelFifty() {
    LevelReward reward = policy.calculate(49, 519, 10);

    assertEquals(1, reward.experienceReward());
    assertEquals(50, reward.levelAfter());
    assertEquals(0, reward.experienceAfter());
    assertEquals(0, reward.requiredExperienceForNextLevel());
    assertEquals(5_000, reward.coinReward());
  }

  @Test
  void discardsAllExperienceAtLevelFifty() {
    LevelReward reward = policy.calculate(50, 0, 150);

    assertEquals(0, reward.experienceReward());
    assertEquals(50, reward.levelAfter());
    assertEquals(0, reward.experienceAfter());
    assertEquals(0, reward.requiredExperienceForNextLevel());
    assertEquals(0, reward.coinReward());
  }

  @Test
  void returnsRequiredExperienceForCurrentLevel() {
    assertEquals(40, policy.requiredExperienceForNextLevel(1));
    assertEquals(520, policy.requiredExperienceForNextLevel(49));
    assertEquals(0, policy.requiredExperienceForNextLevel(50));
  }

  private static Stream<Arguments> specialLevelCoinCases() {
    return Stream.of(
        Arguments.of(5, 1_000L),
        Arguments.of(10, 2_000L),
        Arguments.of(20, 2_000L),
        Arguments.of(30, 2_500L),
        Arguments.of(40, 3_000L),
        Arguments.of(50, 5_000L));
  }
}
