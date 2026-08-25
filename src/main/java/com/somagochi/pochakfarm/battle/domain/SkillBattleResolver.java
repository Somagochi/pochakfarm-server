package com.somagochi.pochakfarm.battle.domain;

import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.common.random.RandomSource;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SkillBattleResolver {

  private static final int PERCENTAGE_BOUND = 100;

  private final BattlePolicy battlePolicy;
  private final RandomSource randomSource;

  public SkillBattleResolver(BattlePolicy battlePolicy, RandomSource randomSource) {
    this.battlePolicy = battlePolicy;
    this.randomSource = randomSource;
  }

  public SkillBattleResolution resolve(
      BattlePosition position, Optional<CardSkill> userSkill, CardSkill npcSkill) {
    Objects.requireNonNull(position);
    Objects.requireNonNull(userSkill);
    Objects.requireNonNull(npcSkill);
    if (position.isTerminal()) {
      throw new IllegalStateException("Terminal battle position cannot resolve a skill battle");
    }

    SkillActivationResult user =
        userSkill.map(this::resolveSelected).orElseGet(SkillActivationResult::notSelected);
    SkillActivationResult npc = resolveSelected(npcSkill);
    int netPoints = user.points() - npc.points();
    BattlePositionChange positionChange = position.move(netPoints);
    return new SkillBattleResolution(user, npc, netPoints, positionChange);
  }

  private SkillActivationResult resolveSelected(CardSkill skill) {
    boolean activated =
        randomSource.nextInt(PERCENTAGE_BOUND)
            < battlePolicy.skillTriggerPercentage(skill.battleType());
    int points = activated ? battlePolicy.skillMoveDistance(skill.battleType()) : 0;
    return SkillActivationResult.selected(skill, activated, points);
  }
}
