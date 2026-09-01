package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.properties.BattleProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class BattleAdvantageResolverTest {

  private final BattlePolicy battlePolicy =
      new BattlePolicy(
          new BattleProperties(
              Duration.ofMinutes(30),
              Duration.ofMinutes(30),
              Duration.ofSeconds(30),
              Duration.ofSeconds(3),
              Duration.ofSeconds(1)));
  private final BattleAdvantageResolver resolver = new BattleAdvantageResolver(battlePolicy);

  @Test
  void resolvesTierPointsAndReturnsTerminalSignal() {
    BattlePositionChange change = resolver.resolveTier(BattlePosition.of(14), Tier.SSS, Tier.C);

    assertEquals(2, change.calculatedPoints());
    assertEquals(1, change.appliedPoints());
    assertEquals(15, change.after().value());
    assertTrue(change.terminal());
    assertEquals(BattleSide.USER, change.winner());
  }

  @Test
  void resolvesNpcTypeAdvantageWithNegativePoints() {
    BattlePositionChange change =
        resolver.resolveType(BattlePosition.of(-14), CardType.GROUND, CardType.SKY);

    assertEquals(-1, change.calculatedPoints());
    assertEquals(-1, change.appliedPoints());
    assertEquals(-15, change.after().value());
    assertTrue(change.terminal());
    assertEquals(BattleSide.NPC, change.winner());
  }

  @Test
  void neutralTypeCombinationDoesNotMovePosition() {
    BattlePositionChange change =
        resolver.resolveType(BattlePosition.of(4), CardType.SKY, CardType.SEA);

    assertEquals(0, change.calculatedPoints());
    assertEquals(4, change.after().value());
    assertFalse(change.terminal());
  }
}
