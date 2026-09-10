package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.SkillBattleType;
import com.somagochi.pochakfarm.common.properties.BattleProperties;
import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class BattlePolicyTest {

  private static final Duration REST_DURATION = Duration.ofMinutes(30);
  private static final Duration ABANDON_THRESHOLD = Duration.ofMinutes(45);
  private static final Duration FINAL_ROUND_START_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration FINAL_ROUND_DURATION = Duration.ofSeconds(3);
  private static final Duration FINAL_ROUND_SUBMISSION_GRACE = Duration.ofSeconds(1);

  private final BattlePolicy battlePolicy =
      new BattlePolicy(
          new BattleProperties(
              REST_DURATION,
              ABANDON_THRESHOLD,
              FINAL_ROUND_START_TIMEOUT,
              FINAL_ROUND_DURATION,
              FINAL_ROUND_SUBMISSION_GRACE));

  @Test
  void tierMoveDistanceFollowsStepDifference() {
    assertEquals(0, battlePolicy.tierMoveDistance(Tier.C, Tier.C));
    assertEquals(0, battlePolicy.tierMoveDistance(Tier.C, Tier.SSS));
    assertEquals(1, battlePolicy.tierMoveDistance(Tier.B, Tier.C));
    assertEquals(1, battlePolicy.tierMoveDistance(Tier.A, Tier.C));
    assertEquals(2, battlePolicy.tierMoveDistance(Tier.S, Tier.C));
    assertEquals(2, battlePolicy.tierMoveDistance(Tier.SSS, Tier.C));
  }

  @Test
  void tierMoveDistanceNeverExceedsTwo() {
    for (Tier tier : Tier.values()) {
      for (Tier opponentTier : Tier.values()) {
        int distance = battlePolicy.tierMoveDistance(tier, opponentTier);
        assertTrue(
            distance >= 0 && distance <= BattlePolicy.MAX_TIER_MOVE_DISTANCE,
            "%s vs %s".formatted(tier, opponentTier));
      }
    }
  }

  @Test
  void tierPointDifferenceUsesUserPositiveAndNpcNegative() {
    assertEquals(0, battlePolicy.tierPointDifference(Tier.C, Tier.C));
    assertEquals(1, battlePolicy.tierPointDifference(Tier.B, Tier.C));
    assertEquals(1, battlePolicy.tierPointDifference(Tier.A, Tier.C));
    assertEquals(2, battlePolicy.tierPointDifference(Tier.SSS, Tier.C));
    assertEquals(-1, battlePolicy.tierPointDifference(Tier.C, Tier.B));
    assertEquals(-2, battlePolicy.tierPointDifference(Tier.C, Tier.SSS));
  }

  @Test
  void typeAdvantageFollowsConfirmedFourTypeCycle() {
    assertTrue(battlePolicy.hasTypeAdvantage(CardType.SKY, CardType.GROUND));
    assertFalse(battlePolicy.hasTypeAdvantage(CardType.SKY, CardType.SPACE));
    assertTrue(battlePolicy.hasTypeAdvantage(CardType.SPACE, CardType.SKY));
    assertFalse(battlePolicy.hasTypeAdvantage(CardType.SPACE, CardType.SEA));
    assertTrue(battlePolicy.hasTypeAdvantage(CardType.GROUND, CardType.SEA));
    assertFalse(battlePolicy.hasTypeAdvantage(CardType.GROUND, CardType.SKY));
    assertTrue(battlePolicy.hasTypeAdvantage(CardType.SEA, CardType.SPACE));
    assertFalse(battlePolicy.hasTypeAdvantage(CardType.SEA, CardType.GROUND));
  }

  @Test
  void everyTypeBeatsExactlyOneTypeAndLosesToExactlyOne() {
    for (CardType cardType : CardType.values()) {
      long beaten =
          Arrays.stream(CardType.values())
              .filter(opponent -> battlePolicy.hasTypeAdvantage(cardType, opponent))
              .count();
      long beatenBy =
          Arrays.stream(CardType.values())
              .filter(opponent -> battlePolicy.hasTypeAdvantage(opponent, cardType))
              .count();

      assertEquals(1, beaten, cardType.name());
      assertEquals(1, beatenBy, cardType.name());
    }
  }

  @Test
  void noTypeHasAdvantageOverItself() {
    for (CardType cardType : CardType.values()) {
      assertFalse(battlePolicy.hasTypeAdvantage(cardType, cardType), cardType.name());
    }
  }

  @Test
  void typeAdvantageIsNeverMutual() {
    for (CardType cardType : CardType.values()) {
      for (CardType opponent : CardType.values()) {
        assertFalse(
            battlePolicy.hasTypeAdvantage(cardType, opponent)
                && battlePolicy.hasTypeAdvantage(opponent, cardType),
            "%s vs %s".formatted(cardType, opponent));
      }
    }
  }

  @Test
  void typeAdvantageAwardsOnePointOnlyToTheAdvantagedType() {
    assertEquals(1, battlePolicy.typeAdvantageMoveDistance(CardType.SKY, CardType.GROUND));
    assertEquals(0, battlePolicy.typeAdvantageMoveDistance(CardType.GROUND, CardType.SKY));
    assertEquals(0, battlePolicy.typeAdvantageMoveDistance(CardType.SKY, CardType.SKY));
    assertEquals(0, battlePolicy.typeAdvantageMoveDistance(CardType.SKY, CardType.SEA));
    assertEquals(
        BattlePolicy.TYPE_ADVANTAGE_MOVE_DISTANCE,
        battlePolicy.typeAdvantageMoveDistance(CardType.SEA, CardType.SPACE));
  }

  @Test
  void typePointDifferenceUsesUserPositiveAndNpcNegative() {
    assertEquals(1, battlePolicy.typePointDifference(CardType.SKY, CardType.GROUND));
    assertEquals(-1, battlePolicy.typePointDifference(CardType.GROUND, CardType.SKY));
    assertEquals(0, battlePolicy.typePointDifference(CardType.SKY, CardType.SKY));
    assertEquals(0, battlePolicy.typePointDifference(CardType.SKY, CardType.SEA));
  }

  @Test
  void skillBattleTypeCarriesTriggerChanceAndMoveDistance() {
    assertEquals(80, battlePolicy.skillTriggerPercentage(SkillBattleType.STABLE));
    assertEquals(45, battlePolicy.skillTriggerPercentage(SkillBattleType.BALANCED));
    assertEquals(30, battlePolicy.skillTriggerPercentage(SkillBattleType.GAMBLE));
    assertEquals(1, battlePolicy.skillMoveDistance(SkillBattleType.STABLE));
    assertEquals(2, battlePolicy.skillMoveDistance(SkillBattleType.BALANCED));
    assertEquals(3, battlePolicy.skillMoveDistance(SkillBattleType.GAMBLE));
  }

  @Test
  void finalRoundPointsFollowTapCountBoundariesAndAreClampedAtThree() {
    assertEquals(0, battlePolicy.finalRoundPoints(0));
    assertEquals(0, battlePolicy.finalRoundPoints(4));
    assertEquals(1, battlePolicy.finalRoundPoints(5));
    assertEquals(1, battlePolicy.finalRoundPoints(11));
    assertEquals(2, battlePolicy.finalRoundPoints(12));
    assertEquals(2, battlePolicy.finalRoundPoints(19));
    assertEquals(3, battlePolicy.finalRoundPoints(20));
    assertEquals(3, battlePolicy.finalRoundPoints(100));
  }

  @Test
  void finalRoundIsRequiredOnlyForTieOrUpToTwoPointDeficit() {
    assertFalse(battlePolicy.requiresFinalRound(1));
    assertTrue(battlePolicy.requiresFinalRound(0));
    assertTrue(battlePolicy.requiresFinalRound(-1));
    assertTrue(battlePolicy.requiresFinalRound(-2));
    assertFalse(battlePolicy.requiresFinalRound(-3));
  }

  @Test
  void gymLeaderRewardsFollowChallengeOrder() {
    long[] expectedCoins = {300, 500, 700, 1_000, 1_500, 2_000, 2_500, 3_000};
    long[] expectedExperience = {20, 30, 50, 75, 100, 140, 180, 220};

    for (int challengeOrder = 1; challengeOrder <= 8; challengeOrder++) {
      assertEquals(
          expectedCoins[challengeOrder - 1], battlePolicy.gymLeaderCoinReward(challengeOrder));
      assertEquals(
          expectedExperience[challengeOrder - 1],
          battlePolicy.gymLeaderExperienceReward(challengeOrder));
    }
  }

  @Test
  void exposesDurationsFromConfiguration() {
    assertEquals(REST_DURATION, battlePolicy.restDuration());
    assertEquals(ABANDON_THRESHOLD, battlePolicy.abandonThreshold());
    assertEquals(FINAL_ROUND_START_TIMEOUT, battlePolicy.finalRoundStartTimeout());
    assertEquals(FINAL_ROUND_DURATION, battlePolicy.finalRoundDuration());
    assertEquals(FINAL_ROUND_SUBMISSION_GRACE, battlePolicy.finalRoundSubmissionGrace());
  }

  @Test
  void actionCountsMatchThreeEntriesTimesThreeActions() {
    assertEquals(3, BattlePolicy.ENTRY_COUNT);
    assertEquals(3, BattlePolicy.ACTIONS_PER_ENTRY);
    assertEquals(9, BattlePolicy.TOTAL_ACTION_COUNT);
    assertEquals(0, BattlePolicy.INITIAL_BAR_POSITION);
  }
}
