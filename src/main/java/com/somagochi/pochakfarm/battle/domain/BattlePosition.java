package com.somagochi.pochakfarm.battle.domain;

public record BattlePosition(int value) {

  public BattlePosition {
    if (value < BattlePolicy.MIN_BAR_POSITION || value > BattlePolicy.MAX_BAR_POSITION) {
      throw new IllegalArgumentException("Battle position is out of range: " + value);
    }
  }

  public static BattlePosition initial() {
    return new BattlePosition(BattlePolicy.INITIAL_BAR_POSITION);
  }

  public static BattlePosition of(int value) {
    return new BattlePosition(value);
  }

  public BattlePositionChange move(int calculatedPoints) {
    if (isTerminal()) {
      throw new IllegalStateException("Terminal battle position cannot move");
    }

    long calculatedPosition = (long) value + calculatedPoints;
    int nextValue =
        (int)
            Math.max(
                BattlePolicy.MIN_BAR_POSITION,
                Math.min(BattlePolicy.MAX_BAR_POSITION, calculatedPosition));
    BattlePosition after = BattlePosition.of(nextValue);
    return new BattlePositionChange(
        this, calculatedPoints, nextValue - value, after, after.winner());
  }

  public boolean isTerminal() {
    return value == BattlePolicy.MIN_BAR_POSITION || value == BattlePolicy.MAX_BAR_POSITION;
  }

  BattleSide winner() {
    if (value == BattlePolicy.MAX_BAR_POSITION) {
      return BattleSide.USER;
    }
    if (value == BattlePolicy.MIN_BAR_POSITION) {
      return BattleSide.NPC;
    }
    return null;
  }
}
