package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BattlePositionTest {

  @Test
  void startsAtCenter() {
    BattlePosition position = BattlePosition.initial();

    assertEquals(0, position.value());
    assertFalse(position.isTerminal());
  }

  @Test
  void clampsUserPointsAtMaximumAndKeepsCalculatedPoints() {
    BattlePositionChange change = BattlePosition.of(14).move(3);

    assertEquals(14, change.before().value());
    assertEquals(3, change.calculatedPoints());
    assertEquals(1, change.appliedPoints());
    assertEquals(15, change.after().value());
    assertTrue(change.terminal());
    assertEquals(BattleSide.USER, change.winner());
  }

  @Test
  void clampsNpcPointsAtMinimumAndKeepsCalculatedPoints() {
    BattlePositionChange change = BattlePosition.of(-13).move(-3);

    assertEquals(-13, change.before().value());
    assertEquals(-3, change.calculatedPoints());
    assertEquals(-2, change.appliedPoints());
    assertEquals(-15, change.after().value());
    assertTrue(change.terminal());
    assertEquals(BattleSide.NPC, change.winner());
  }

  @Test
  void zeroPointsDoNotMoveOrFinishBattle() {
    BattlePositionChange change = BattlePosition.of(4).move(0);

    assertEquals(0, change.appliedPoints());
    assertEquals(4, change.after().value());
    assertFalse(change.terminal());
    assertEquals(null, change.winner());
  }

  @Test
  void rejectsPositionOutsidePolicyRange() {
    assertThrows(IllegalArgumentException.class, () -> BattlePosition.of(-16));
    assertThrows(IllegalArgumentException.class, () -> BattlePosition.of(16));
  }

  @Test
  void rejectsMovementAfterTerminalPosition() {
    BattlePosition terminal = BattlePosition.of(BattlePolicy.MAX_BAR_POSITION);

    assertThrows(IllegalStateException.class, () -> terminal.move(-1));
  }
}
