package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.common.properties.BattleProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class NpcSkillSelectorTest {

  private static final CardSkill STABLE = CardSkill.SEA_SEASHELL_SHIELD;
  private static final CardSkill BALANCED = CardSkill.SEA_WATER_TAIL;
  private static final CardSkill GAMBLE = CardSkill.SEA_SPLASH_PAW;

  private final NpcSkillSelector npcSkillSelector =
      new NpcSkillSelector(
          new BattlePolicy(
              new BattleProperties(
                  Duration.ofMinutes(30),
                  Duration.ofMinutes(30),
                  Duration.ofSeconds(30),
                  Duration.ofSeconds(3),
                  Duration.ofSeconds(1))));

  @Test
  void picksHigherTriggerPercentageSkillWhenNpcIsAhead() {
    assertEquals(STABLE, npcSkillSelector.select(BattlePosition.of(-1), GAMBLE, STABLE));
    assertEquals(STABLE, npcSkillSelector.select(BattlePosition.of(-7), STABLE, BALANCED));
  }

  @Test
  void picksFirstSkillWhenBothSidesAreEven() {
    assertEquals(GAMBLE, npcSkillSelector.select(BattlePosition.initial(), GAMBLE, STABLE));
    assertEquals(STABLE, npcSkillSelector.select(BattlePosition.initial(), STABLE, GAMBLE));
  }

  @Test
  void picksHigherPointSkillWhenUserIsAhead() {
    assertEquals(GAMBLE, npcSkillSelector.select(BattlePosition.of(1), STABLE, GAMBLE));
    assertEquals(BALANCED, npcSkillSelector.select(BattlePosition.of(9), STABLE, BALANCED));
  }

  @Test
  void picksFirstSkillWhenComparedValuesAreEqual() {
    assertEquals(
        CardSkill.SEA_SEASHELL_SHIELD,
        npcSkillSelector.select(
            BattlePosition.of(5), CardSkill.SEA_SEASHELL_SHIELD, CardSkill.SEA_BUBBLE_GUARD));
    assertEquals(
        CardSkill.SEA_SEASHELL_SHIELD,
        npcSkillSelector.select(
            BattlePosition.of(-5), CardSkill.SEA_SEASHELL_SHIELD, CardSkill.SEA_BUBBLE_GUARD));
  }

  @Test
  void usesOnlyBarPositionAndOwnSkillsSoResultIsDeterministic() {
    CardSkill firstCall = npcSkillSelector.select(BattlePosition.of(4), STABLE, GAMBLE);
    CardSkill secondCall = npcSkillSelector.select(BattlePosition.of(4), STABLE, GAMBLE);

    assertEquals(firstCall, secondCall);
    assertEquals(GAMBLE, firstCall);
  }

  @Test
  void rejectsSelectionAtTerminalPosition() {
    assertThrows(
        IllegalStateException.class,
        () ->
            npcSkillSelector.select(
                BattlePosition.of(BattlePolicy.MAX_BAR_POSITION), STABLE, GAMBLE));
    assertThrows(
        IllegalStateException.class,
        () ->
            npcSkillSelector.select(
                BattlePosition.of(BattlePolicy.MIN_BAR_POSITION), STABLE, GAMBLE));
  }
}
