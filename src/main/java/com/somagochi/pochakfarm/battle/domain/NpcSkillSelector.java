package com.somagochi.pochakfarm.battle.domain;

import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.SkillBattleType;
import java.util.Objects;
import java.util.function.ToIntFunction;
import org.springframework.stereotype.Component;

@Component
public class NpcSkillSelector {

  private final BattlePolicy battlePolicy;

  public NpcSkillSelector(BattlePolicy battlePolicy) {
    this.battlePolicy = battlePolicy;
  }

  public CardSkill select(BattlePosition position, CardSkill skill1, CardSkill skill2) {
    Objects.requireNonNull(position);
    Objects.requireNonNull(skill1);
    Objects.requireNonNull(skill2);

    if (position.value() < BattlePolicy.INITIAL_BAR_POSITION) {
      return higherOrFirst(skill1, skill2, battlePolicy::skillTriggerPercentage);
    }
    if (position.value() > BattlePolicy.INITIAL_BAR_POSITION) {
      return higherOrFirst(skill1, skill2, battlePolicy::skillMoveDistance);
    }
    return skill1;
  }

  private CardSkill higherOrFirst(
      CardSkill skill1, CardSkill skill2, ToIntFunction<SkillBattleType> measure) {
    int first = measure.applyAsInt(skill1.battleType());
    int second = measure.applyAsInt(skill2.battleType());
    return second > first ? skill2 : skill1;
  }
}
