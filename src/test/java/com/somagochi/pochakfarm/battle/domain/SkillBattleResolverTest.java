package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.common.properties.BattleProperties;
import com.somagochi.pochakfarm.common.random.RandomSource;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Optional;
import java.util.Queue;
import org.junit.jupiter.api.Test;

class SkillBattleResolverTest {

  private static final CardSkill STABLE = CardSkill.GROUND_LEAF_GUARD;
  private static final CardSkill BALANCED = CardSkill.GROUND_ROOT_BIND;
  private static final CardSkill GAMBLE = CardSkill.GROUND_PAW_STRIKE;

  private final BattlePolicy battlePolicy =
      new BattlePolicy(new BattleProperties(Duration.ofMinutes(30), Duration.ofMinutes(30)));

  @Test
  void judgesUserAndNpcWithIndependentRandomValues() {
    SequenceRandomProvider randomProvider = new SequenceRandomProvider(79, 80);
    SkillBattleResolver resolver = new SkillBattleResolver(battlePolicy, randomProvider);

    SkillBattleResolution result =
        resolver.resolve(BattlePosition.initial(), Optional.of(STABLE), STABLE);

    assertEquals(SkillActivationStatus.ACTIVATED, result.user().status());
    assertEquals(1, result.user().points());
    assertEquals(SkillActivationStatus.FAILED, result.npc().status());
    assertEquals(0, result.npc().points());
    assertEquals(1, result.netPoints());
    assertEquals(1, result.positionChange().after().value());
    assertEquals(2, randomProvider.callCount());
  }

  @Test
  void offsetsBothEarnedPointsBeforeMovingPositionOnce() {
    SkillBattleResolver resolver =
        new SkillBattleResolver(battlePolicy, new SequenceRandomProvider(0, 0));

    SkillBattleResolution result =
        resolver.resolve(BattlePosition.of(5), Optional.of(GAMBLE), BALANCED);

    assertEquals(3, result.user().points());
    assertEquals(2, result.npc().points());
    assertEquals(1, result.netPoints());
    assertEquals(5, result.positionChange().before().value());
    assertEquals(6, result.positionChange().after().value());
  }

  @Test
  void equalPointsCancelEachOther() {
    SkillBattleResolver resolver =
        new SkillBattleResolver(battlePolicy, new SequenceRandomProvider(0, 0));

    SkillBattleResolution result =
        resolver.resolve(BattlePosition.of(-4), Optional.of(STABLE), STABLE);

    assertEquals(0, result.netPoints());
    assertEquals(0, result.positionChange().appliedPoints());
    assertEquals(-4, result.positionChange().after().value());
  }

  @Test
  void doesNotRollForMissingUserSelectionAndStillJudgesNpc() {
    SequenceRandomProvider randomProvider = new SequenceRandomProvider(0);
    SkillBattleResolver resolver = new SkillBattleResolver(battlePolicy, randomProvider);

    SkillBattleResolution result =
        resolver.resolve(BattlePosition.initial(), Optional.empty(), BALANCED);

    assertEquals(SkillActivationStatus.NOT_SELECTED, result.user().status());
    assertTrue(result.user().skill().isEmpty());
    assertEquals(0, result.user().points());
    assertEquals(SkillActivationStatus.ACTIVATED, result.npc().status());
    assertEquals(-2, result.netPoints());
    assertEquals(-2, result.positionChange().after().value());
    assertEquals(1, randomProvider.callCount());
  }

  @Test
  void distinguishesFailedSelectionFromMissingSelection() {
    SkillBattleResolver resolver =
        new SkillBattleResolver(battlePolicy, new SequenceRandomProvider(80, 80));

    SkillBattleResolution result =
        resolver.resolve(BattlePosition.initial(), Optional.of(STABLE), STABLE);

    assertEquals(SkillActivationStatus.FAILED, result.user().status());
    assertTrue(result.user().skill().isPresent());
    assertEquals(SkillActivationStatus.FAILED, result.npc().status());
    assertFalse(result.positionChange().terminal());
  }

  @Test
  void keepsNetPointsWhenPositionIsClampedAtTerminal() {
    SkillBattleResolver resolver =
        new SkillBattleResolver(battlePolicy, new SequenceRandomProvider(0, 99));

    SkillBattleResolution result =
        resolver.resolve(BattlePosition.of(14), Optional.of(GAMBLE), STABLE);

    assertEquals(3, result.netPoints());
    assertEquals(1, result.positionChange().appliedPoints());
    assertEquals(15, result.positionChange().after().value());
    assertTrue(result.positionChange().terminal());
    assertEquals(BattleSide.USER, result.positionChange().winner());
  }

  @Test
  void rejectsTerminalPositionBeforeConsumingRandomValues() {
    SequenceRandomProvider randomProvider = new SequenceRandomProvider(0, 0);
    SkillBattleResolver resolver = new SkillBattleResolver(battlePolicy, randomProvider);

    assertThrows(
        IllegalStateException.class,
        () -> resolver.resolve(BattlePosition.of(15), Optional.of(STABLE), STABLE));

    assertEquals(0, randomProvider.callCount());
  }

  private static final class SequenceRandomProvider implements RandomSource {

    private final Queue<Integer> values;
    private int callCount;

    private SequenceRandomProvider(Integer... values) {
      this.values = new ArrayDeque<>(Arrays.asList(values));
    }

    @Override
    public int nextInt(int bound) {
      assertEquals(100, bound);
      callCount++;
      return values.remove();
    }

    private int callCount() {
      return callCount;
    }
  }
}
