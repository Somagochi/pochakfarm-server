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

  private final BattlePolicy battlePolicy =
      new BattlePolicy(new BattleProperties(REST_DURATION, ABANDON_THRESHOLD));

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
  void typeAdvantageMovesOneCellOnlyForTheAdvantagedType() {
    assertEquals(1, battlePolicy.typeAdvantageMoveDistance(CardType.SKY, CardType.GROUND));
    assertEquals(0, battlePolicy.typeAdvantageMoveDistance(CardType.GROUND, CardType.SKY));
    assertEquals(0, battlePolicy.typeAdvantageMoveDistance(CardType.SKY, CardType.SKY));
    assertEquals(0, battlePolicy.typeAdvantageMoveDistance(CardType.SKY, CardType.SEA));
    assertEquals(
        BattlePolicy.TYPE_ADVANTAGE_MOVE_DISTANCE,
        battlePolicy.typeAdvantageMoveDistance(CardType.SEA, CardType.SPACE));
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
  void finalRoundMoveDistanceIsClampedAtTwo() {
    assertEquals(0, battlePolicy.finalRoundMoveDistance(0));
    assertEquals(0, battlePolicy.finalRoundMoveDistance(7));
    assertEquals(1, battlePolicy.finalRoundMoveDistance(8));
    assertEquals(1, battlePolicy.finalRoundMoveDistance(15));
    assertEquals(2, battlePolicy.finalRoundMoveDistance(16));
    assertEquals(2, battlePolicy.finalRoundMoveDistance(100));
  }

  @Test
  void exposesDurationsFromConfiguration() {
    assertEquals(REST_DURATION, battlePolicy.restDuration());
    assertEquals(ABANDON_THRESHOLD, battlePolicy.abandonThreshold());
  }

  @Test
  void actionCountsMatchThreeEntriesTimesThreeActions() {
    assertEquals(3, BattlePolicy.ENTRY_COUNT);
    assertEquals(3, BattlePolicy.ACTIONS_PER_ENTRY);
    assertEquals(9, BattlePolicy.TOTAL_ACTION_COUNT);
    assertEquals(0, BattlePolicy.INITIAL_BAR_POSITION);
  }
}
