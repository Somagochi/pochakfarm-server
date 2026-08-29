package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import java.util.List;
import org.junit.jupiter.api.Test;

class BattleBroadcastEventGeneratorTest {

  private static final CardSkill USER_SKILL = CardSkill.SEA_SEASHELL_SHIELD;
  private static final CardSkill NPC_SKILL = CardSkill.SEA_SPLASH_PAW;

  private final BattleBroadcastEventGenerator generator = new BattleBroadcastEventGenerator();

  @Test
  void createsAdvantageThenAppliedPointUsingClampedValue() {
    BattlePositionChange change = BattlePosition.of(14).move(3);

    List<BattleBroadcastEventSpec> events =
        generator.advantage(BattleEventCode.TIER_ADVANTAGE, change);

    assertEquals(2, events.size());
    assertEvent(events.get(0), BattleEventCode.TIER_ADVANTAGE, BattleSide.USER, null, null, null);
    assertEvent(
        events.get(1), BattleEventCode.BATTLE_POINT_APPLIED, null, null, BattleSide.USER, 1);
  }

  @Test
  void createsNoAdvantageEventsWhenNeitherSideHasAdvantage() {
    assertEquals(
        List.of(),
        generator.advantage(BattleEventCode.TYPE_ADVANTAGE, BattlePosition.initial().move(0)));
  }

  @Test
  void rejectsNonAdvantageEventCode() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            generator.advantage(BattleEventCode.SKILL_TRIGGERED, BattlePosition.initial().move(1)));
  }

  @Test
  void createsUserThenNpcSkillEventsAndAppliedPoint() {
    SkillBattleResolution resolution =
        resolution(
            SkillActivationResult.selected(USER_SKILL, true, 1),
            SkillActivationResult.selected(NPC_SKILL, false, 0),
            BattlePosition.of(14));

    List<BattleBroadcastEventSpec> events = generator.skill(resolution);

    assertEquals(3, events.size());
    assertEvent(
        events.get(0), BattleEventCode.SKILL_TRIGGERED, BattleSide.USER, USER_SKILL, null, null);
    assertEvent(events.get(1), BattleEventCode.SKILL_FAILED, BattleSide.NPC, NPC_SKILL, null, null);
    assertEvent(
        events.get(2), BattleEventCode.BATTLE_POINT_APPLIED, null, null, BattleSide.USER, 1);
  }

  @Test
  void createsNotSelectedEventBeforeNpcResult() {
    SkillBattleResolution resolution =
        resolution(
            SkillActivationResult.notSelected(),
            SkillActivationResult.selected(NPC_SKILL, true, 3),
            BattlePosition.initial());

    List<BattleBroadcastEventSpec> events = generator.skill(resolution);

    assertEquals(3, events.size());
    assertEvent(
        events.get(0), BattleEventCode.SKILL_NOT_SELECTED, BattleSide.USER, null, null, null);
    assertEvent(
        events.get(1), BattleEventCode.SKILL_TRIGGERED, BattleSide.NPC, NPC_SKILL, null, null);
    assertEvent(events.get(2), BattleEventCode.BATTLE_POINT_APPLIED, null, null, BattleSide.NPC, 3);
  }

  @Test
  void createsOffsetOnlyWhenBothActivatedWithSamePoints() {
    SkillActivationResult user = SkillActivationResult.selected(USER_SKILL, true, 1);
    SkillActivationResult npc = SkillActivationResult.selected(USER_SKILL, true, 1);

    List<BattleBroadcastEventSpec> events =
        generator.skill(resolution(user, npc, BattlePosition.initial()));

    assertEquals(3, events.size());
    assertEquals(BattleEventCode.SKILL_OFFSET, events.get(2).eventCode());
  }

  @Test
  void doesNotCreateOffsetOrPointForBothFailed() {
    SkillActivationResult user = SkillActivationResult.selected(USER_SKILL, false, 0);
    SkillActivationResult npc = SkillActivationResult.selected(NPC_SKILL, false, 0);

    List<BattleBroadcastEventSpec> events =
        generator.skill(resolution(user, npc, BattlePosition.initial()));

    assertEquals(2, events.size());
    assertEquals(BattleEventCode.SKILL_FAILED, events.get(0).eventCode());
    assertEquals(BattleEventCode.SKILL_FAILED, events.get(1).eventCode());
  }

  @Test
  void doesNotCreateOffsetForNotSelectedAndFailed() {
    SkillActivationResult user = SkillActivationResult.notSelected();
    SkillActivationResult npc = SkillActivationResult.selected(NPC_SKILL, false, 0);

    List<BattleBroadcastEventSpec> events =
        generator.skill(resolution(user, npc, BattlePosition.initial()));

    assertEquals(2, events.size());
    assertEquals(BattleEventCode.SKILL_NOT_SELECTED, events.get(0).eventCode());
    assertEquals(BattleEventCode.SKILL_FAILED, events.get(1).eventCode());
  }

  @Test
  void rejectsPayloadThatDoesNotMatchEventCode() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new BattleBroadcastEventSpec(
                BattleEventCode.BATTLE_POINT_APPLIED, null, null, BattleSide.USER, 0));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new BattleBroadcastEventSpec(
                BattleEventCode.SKILL_NOT_SELECTED, BattleSide.NPC, null, null, null));
  }

  private SkillBattleResolution resolution(
      SkillActivationResult user, SkillActivationResult npc, BattlePosition before) {
    int netPoints = user.points() - npc.points();
    return new SkillBattleResolution(user, npc, netPoints, before.move(netPoints));
  }

  private void assertEvent(
      BattleBroadcastEventSpec event,
      BattleEventCode code,
      BattleSide animalSide,
      CardSkill skill,
      BattleSide winnerSide,
      Integer points) {
    assertEquals(code, event.eventCode());
    assertEquals(animalSide, event.animalSide());
    assertEquals(skill, event.skill());
    assertEquals(winnerSide, event.winnerSide());
    assertEquals(points, event.points());
  }
}
