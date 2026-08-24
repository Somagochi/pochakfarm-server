package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class BattleTest {

  private static final Instant STARTED_AT = Instant.parse("2026-08-19T01:00:00Z");
  private static final Instant ENDED_AT = Instant.parse("2026-08-19T01:10:00Z");

  @Test
  void startsInProgressWithoutResult() {
    Battle battle = Battle.start(1L, 2L, STARTED_AT);

    assertTrue(battle.isInProgress());
    assertEquals(BattleStatus.IN_PROGRESS, battle.getStatus());
    assertNull(battle.getResult());
    assertNull(battle.getEndedAt());
  }

  @Test
  void finishRecordsResultAndEndedAt() {
    Battle battle = Battle.start(1L, 2L, STARTED_AT);

    battle.finish(BattleResult.WIN, ENDED_AT);

    assertEquals(BattleStatus.FINISHED, battle.getStatus());
    assertEquals(BattleResult.WIN, battle.getResult());
    assertEquals(ENDED_AT, battle.getEndedAt());
  }

  @Test
  void abandonLeavesResultNull() {
    Battle battle = Battle.start(1L, 2L, STARTED_AT);

    battle.abandon(ENDED_AT);

    assertEquals(BattleStatus.ABANDONED, battle.getStatus());
    assertNull(battle.getResult());
    assertEquals(ENDED_AT, battle.getEndedAt());
  }

  @Test
  void finishTwiceIsRejected() {
    Battle battle = Battle.start(1L, 2L, STARTED_AT);
    battle.finish(BattleResult.WIN, ENDED_AT);

    assertThrows(IllegalStateException.class, () -> battle.finish(BattleResult.LOSE, ENDED_AT));
  }

  @Test
  void abandonAfterFinishIsRejected() {
    Battle battle = Battle.start(1L, 2L, STARTED_AT);
    battle.finish(BattleResult.LOSE, ENDED_AT);

    assertThrows(IllegalStateException.class, () -> battle.abandon(ENDED_AT));
  }

  @Test
  void isOwnedByComparesUserId() {
    Battle battle = Battle.start(1L, 2L, STARTED_AT);

    assertTrue(battle.isOwnedBy(1L));
    assertTrue(!battle.isOwnedBy(2L));
  }

  @Test
  void requiresUserAndGymLeaderAndStartedAt() {
    assertThrows(NullPointerException.class, () -> Battle.start(null, 2L, STARTED_AT));
    assertThrows(NullPointerException.class, () -> Battle.start(1L, null, STARTED_AT));
    assertThrows(NullPointerException.class, () -> Battle.start(1L, 2L, null));
  }
}
