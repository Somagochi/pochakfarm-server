package com.somagochi.pochakfarm.battle.domain;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.SkillBattleType;
import com.somagochi.pochakfarm.common.properties.BattleProperties;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BattlePolicy {

  public static final int ENTRY_COUNT = BattleEntry.ENTRY_COUNT;
  public static final int ACTIONS_PER_ENTRY = 3;
  public static final int TOTAL_ACTION_COUNT = ENTRY_COUNT * ACTIONS_PER_ENTRY;
  public static final int INITIAL_BAR_POSITION = 0;

  public static final int MAX_TIER_MOVE_DISTANCE = 2;
  public static final int TIER_STEP_FOR_MAX_MOVE_DISTANCE = 3;
  public static final int TYPE_ADVANTAGE_MOVE_DISTANCE = 1;

  public static final int FINAL_ROUND_ONE_MOVE_TAP_COUNT = 8;
  public static final int FINAL_ROUND_TWO_MOVE_TAP_COUNT = 16;
  public static final int MAX_FINAL_ROUND_MOVE_DISTANCE = 2;

  private static final Map<CardType, CardType> TYPE_ADVANTAGES =
      Map.of(
          CardType.SPACE, CardType.SKY,
          CardType.SKY, CardType.GROUND,
          CardType.GROUND, CardType.SEA,
          CardType.SEA, CardType.SPACE);
  private static final Map<SkillBattleType, Integer> SKILL_MOVE_DISTANCES =
      Map.of(
          SkillBattleType.STABLE, 1,
          SkillBattleType.BALANCED, 2,
          SkillBattleType.GAMBLE, 3);
  private static final Map<SkillBattleType, Integer> SKILL_TRIGGER_PERCENTAGES =
      Map.of(
          SkillBattleType.STABLE, 80,
          SkillBattleType.BALANCED, 45,
          SkillBattleType.GAMBLE, 30);

  private final BattleProperties battleProperties;

  public BattlePolicy(BattleProperties battleProperties) {
    this.battleProperties = battleProperties;
  }

  public int tierMoveDistance(Tier tier, Tier opponentTier) {
    int step = tier.ordinal() - opponentTier.ordinal();
    if (step <= 0) {
      return 0;
    }
    return step >= TIER_STEP_FOR_MAX_MOVE_DISTANCE ? MAX_TIER_MOVE_DISTANCE : 1;
  }

  public boolean hasTypeAdvantage(CardType cardType, CardType opponentCardType) {
    return TYPE_ADVANTAGES.get(cardType) == opponentCardType;
  }

  public int typeAdvantageMoveDistance(CardType cardType, CardType opponentCardType) {
    return hasTypeAdvantage(cardType, opponentCardType) ? TYPE_ADVANTAGE_MOVE_DISTANCE : 0;
  }

  public int skillMoveDistance(SkillBattleType battleType) {
    return SKILL_MOVE_DISTANCES.get(battleType);
  }

  public int skillTriggerPercentage(SkillBattleType battleType) {
    return SKILL_TRIGGER_PERCENTAGES.get(battleType);
  }

  public int finalRoundMoveDistance(int tapCount) {
    if (tapCount >= FINAL_ROUND_TWO_MOVE_TAP_COUNT) {
      return MAX_FINAL_ROUND_MOVE_DISTANCE;
    }
    return tapCount >= FINAL_ROUND_ONE_MOVE_TAP_COUNT ? 1 : 0;
  }

  public Duration restDuration() {
    return battleProperties.restDuration();
  }

  public Duration abandonThreshold() {
    return battleProperties.abandonThreshold();
  }
}
