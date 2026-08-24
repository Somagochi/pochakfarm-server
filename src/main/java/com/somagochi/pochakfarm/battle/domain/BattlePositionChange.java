package com.somagochi.pochakfarm.battle.domain;

import java.util.Objects;

public record BattlePositionChange(
    BattlePosition before,
    int calculatedPoints,
    int appliedPoints,
    BattlePosition after,
    BattleSide winner) {

  public BattlePositionChange {
    Objects.requireNonNull(before);
    Objects.requireNonNull(after);
    if (after.value() - before.value() != appliedPoints) {
      throw new IllegalArgumentException("Applied points do not match position change");
    }
    if (after.isTerminal() != (winner != null)) {
      throw new IllegalArgumentException("Winner must match terminal position");
    }
  }

  public boolean terminal() {
    return winner != null;
  }
}
