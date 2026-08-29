package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BattlePositionChangeTest {

  @Test
  void acceptsWinnerThatMatchesTerminalPosition() {
    assertDoesNotThrow(
        () ->
            new BattlePositionChange(
                BattlePosition.of(14),
                3,
                1,
                BattlePosition.of(BattlePolicy.MAX_BAR_POSITION),
                BattleSide.USER));
    assertDoesNotThrow(
        () ->
            new BattlePositionChange(
                BattlePosition.of(-14),
                -3,
                -1,
                BattlePosition.of(BattlePolicy.MIN_BAR_POSITION),
                BattleSide.NPC));
  }

  @Test
  void rejectsWinnerThatDoesNotMatchTerminalPosition() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new BattlePositionChange(
                BattlePosition.of(14),
                1,
                1,
                BattlePosition.of(BattlePolicy.MAX_BAR_POSITION),
                BattleSide.NPC));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new BattlePositionChange(
                BattlePosition.of(-14),
                -1,
                -1,
                BattlePosition.of(BattlePolicy.MIN_BAR_POSITION),
                BattleSide.USER));
  }

  @Test
  void rejectsWinnerBeforeTerminalPosition() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new BattlePositionChange(
                BattlePosition.initial(), 1, 1, BattlePosition.of(1), BattleSide.USER));
  }
}
