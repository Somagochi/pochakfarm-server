package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.properties.BattleProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class GymLeaderTypeTest {

  private final BattlePolicy battlePolicy =
      new BattlePolicy(
          new BattleProperties(
              Duration.ofMinutes(30),
              Duration.ofMinutes(45),
              Duration.ofSeconds(30),
              Duration.ofSeconds(3),
              Duration.ofSeconds(1)));

  @Test
  void coversEveryCardTypeWithSameLabelAndCounter() {
    for (CardType cardType : CardType.values()) {
      GymLeaderType leaderType = GymLeaderType.valueOf(cardType.name());

      assertEquals(cardType.label(), leaderType.label());
      assertTrue(
          battlePolicy.hasTypeAdvantage(CardType.valueOf(leaderType.counter().name()), cardType));
    }
    assertEquals(CardType.values().length + 1, GymLeaderType.values().length);
  }

  @Test
  void mixedCountersItself() {
    assertEquals("복합", GymLeaderType.MIXED.label());
    assertEquals(GymLeaderType.MIXED, GymLeaderType.MIXED.counter());
  }
}
