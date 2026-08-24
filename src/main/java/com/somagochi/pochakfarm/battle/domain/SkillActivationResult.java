package com.somagochi.pochakfarm.battle.domain;

import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.SkillBattleType;
import java.util.Objects;
import java.util.Optional;

public record SkillActivationResult(
    Optional<CardSkill> skill,
    SkillBattleType battleType,
    SkillActivationStatus status,
    int points) {

  public SkillActivationResult {
    Objects.requireNonNull(skill);
    Objects.requireNonNull(status);
    if (status == SkillActivationStatus.NOT_SELECTED) {
      if (skill.isPresent() || battleType != null || points != 0) {
        throw new IllegalArgumentException("Not selected result cannot have skill outcome");
      }
    } else {
      CardSkill selectedSkill = skill.orElseThrow();
      if (battleType != selectedSkill.battleType()) {
        throw new IllegalArgumentException("Battle type must match selected skill");
      }
      if (points < 0 || (status == SkillActivationStatus.FAILED && points != 0)) {
        throw new IllegalArgumentException("Invalid skill points: " + points);
      }
    }
  }

  public static SkillActivationResult notSelected() {
    return new SkillActivationResult(Optional.empty(), null, SkillActivationStatus.NOT_SELECTED, 0);
  }

  public static SkillActivationResult selected(CardSkill skill, boolean activated, int points) {
    Objects.requireNonNull(skill);
    return new SkillActivationResult(
        Optional.of(skill),
        skill.battleType(),
        activated ? SkillActivationStatus.ACTIVATED : SkillActivationStatus.FAILED,
        points);
  }
}
