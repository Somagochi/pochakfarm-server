package com.somagochi.pochakfarm.battle.domain;

import java.util.Objects;

public record SkillBattleResolution(
    SkillActivationResult user,
    SkillActivationResult npc,
    int netPoints,
    BattlePositionChange positionChange) {

  public SkillBattleResolution {
    Objects.requireNonNull(user);
    Objects.requireNonNull(npc);
    Objects.requireNonNull(positionChange);
    if (netPoints != user.points() - npc.points()) {
      throw new IllegalArgumentException("Net points do not match skill outcomes");
    }
    if (netPoints != positionChange.calculatedPoints()) {
      throw new IllegalArgumentException("Net points do not match position change");
    }
  }
}
