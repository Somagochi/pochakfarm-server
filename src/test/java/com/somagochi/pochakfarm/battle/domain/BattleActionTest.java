package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import org.junit.jupiter.api.Test;

class BattleActionTest {

  @Test
  void derivesEntryOrderAndActionNoFromActionSeq() {
    assertEquals(1, BattleAction.entryOrderOf(1));
    assertEquals(1, BattleAction.entryOrderOf(3));
    assertEquals(2, BattleAction.entryOrderOf(4));
    assertEquals(3, BattleAction.entryOrderOf(9));
    assertEquals(1, BattleAction.actionNoInEntryOf(1));
    assertEquals(3, BattleAction.actionNoInEntryOf(3));
    assertEquals(1, BattleAction.actionNoInEntryOf(4));
    assertEquals(3, BattleAction.actionNoInEntryOf(9));
  }

  @Test
  void recordsDerivedColumnsOnCreation() {
    BattleAction action = action(7);

    assertEquals(3, action.getEntryOrder());
    assertEquals(1, action.getActionNoInEntry());
    assertEquals(7, action.getActionSeq());
  }

  @Test
  void rejectsActionSeqOutOfRange() {
    assertThrows(IllegalArgumentException.class, () -> action(0));
    assertThrows(IllegalArgumentException.class, () -> action(BattlePolicy.TOTAL_ACTION_COUNT + 1));
  }

  @Test
  void rejectsEventSeqBelowOne() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            BattleBroadcastEvent.record(
                1L, 0, null, null, BattleEventCode.SKILL_OFFSET, null, null, null, 1));
  }

  private BattleAction action(int actionSeq) {
    return BattleAction.record(
        1L,
        actionSeq,
        CardSkill.GROUND_MOSS_CUSHION,
        true,
        1,
        CardSkill.SEA_WAVE_DASH,
        false,
        0,
        1,
        0,
        1);
  }
}
