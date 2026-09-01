package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BattleTest {

  private static final Instant STARTED_AT = Instant.parse("2026-08-19T01:00:00Z");
  private static final Instant ENDED_AT = Instant.parse("2026-08-19T01:10:00Z");
  private static final String CLIENT_REQUEST_ID = UUID.randomUUID().toString();

  @Test
  void startsInProgressWithoutResultAtCenterBarPosition() {
    Battle battle = battle();

    assertTrue(battle.isInProgress());
    assertEquals(BattleStatus.IN_PROGRESS, battle.getStatus());
    assertEquals(BattlePolicy.INITIAL_BAR_POSITION, battle.getBarPosition());
    assertEquals(CLIENT_REQUEST_ID, battle.getClientRequestId());
    assertNull(battle.getResult());
    assertNull(battle.getEndedAt());
    assertNull(battle.getLastActionAt());
    assertNull(battle.getFinalExpiresAt());
  }

  @Test
  void finishRecordsResultAndEndedAt() {
    Battle battle = battle();

    battle.finish(BattleResult.WIN, ENDED_AT);

    assertEquals(BattleStatus.FINISHED, battle.getStatus());
    assertEquals(BattleResult.WIN, battle.getResult());
    assertEquals(ENDED_AT, battle.getEndedAt());
  }

  @Test
  void abandonLeavesResultNull() {
    Battle battle = battle();

    battle.abandon(ENDED_AT);

    assertEquals(BattleStatus.ABANDONED, battle.getStatus());
    assertNull(battle.getResult());
    assertEquals(ENDED_AT, battle.getEndedAt());
  }

  @Test
  void finishTwiceIsRejected() {
    Battle battle = battle();
    battle.finish(BattleResult.WIN, ENDED_AT);

    assertThrows(IllegalStateException.class, () -> battle.finish(BattleResult.LOSE, ENDED_AT));
  }

  @Test
  void abandonAfterFinishIsRejected() {
    Battle battle = battle();
    battle.finish(BattleResult.LOSE, ENDED_AT);

    assertThrows(IllegalStateException.class, () -> battle.abandon(ENDED_AT));
  }

  @Test
  void isOwnedByComparesUserId() {
    Battle battle = battle();

    assertTrue(battle.isOwnedBy(1L));
    assertFalse(battle.isOwnedBy(2L));
  }

  @Test
  void applyActionMovesBarPositionAndRecordsLastActionAt() {
    Battle battle = battle();

    battle.applyAction(-2, ENDED_AT);

    assertEquals(-2, battle.getBarPosition());
    assertEquals(ENDED_AT, battle.getLastActionAt());
    assertEquals(ENDED_AT, battle.lastProgressAt());
  }

  @Test
  void lastProgressAtFallsBackToStartedAt() {
    assertEquals(STARTED_AT, battle().lastProgressAt());
  }

  @Test
  void isExpiredAtComparesLastProgressWithThreshold() {
    Battle battle = battle();
    Duration threshold = Duration.ofMinutes(30);

    assertFalse(battle.isExpiredAt(STARTED_AT.plus(Duration.ofMinutes(29)), threshold));
    assertTrue(battle.isExpiredAt(STARTED_AT.plus(threshold), threshold));

    battle.applyAction(1, STARTED_AT.plus(Duration.ofMinutes(20)));

    assertFalse(battle.isExpiredAt(STARTED_AT.plus(threshold), threshold));
  }

  @Test
  void finishedBattleIsNeverExpired() {
    Battle battle = battle();
    battle.finish(BattleResult.WIN, ENDED_AT);

    assertFalse(battle.isExpiredAt(ENDED_AT.plus(Duration.ofDays(1)), Duration.ofMinutes(30)));
  }

  @Test
  void finalRoundRecordsExpiryAndResult() {
    Battle battle = battle();
    Instant finalReadyAt = STARTED_AT.plusSeconds(5);
    Instant finalExpiresAt = STARTED_AT.plusSeconds(10);

    battle.prepareFinalRound(finalReadyAt);
    battle.startFinalRound(finalExpiresAt);

    assertEquals(finalReadyAt, battle.getFinalReadyAt());
    assertEquals(finalExpiresAt, battle.getFinalExpiresAt());
    assertFalse(
        battle.isFinalRoundSubmissionExpired(
            finalExpiresAt.plusMillis(999), Duration.ofSeconds(1)));
    assertFalse(
        battle.isFinalRoundSubmissionExpired(finalExpiresAt.plusSeconds(1), Duration.ofSeconds(1)));
    assertTrue(
        battle.isFinalRoundSubmissionExpired(
            finalExpiresAt.plusSeconds(1).plusNanos(1), Duration.ofSeconds(1)));

    battle.applyFinalRound(14, 1, 3);

    assertEquals(14, battle.getFinalTapCount());
    assertEquals(1, battle.getFinalPoints());
    assertEquals(3, battle.getBarPosition());
  }

  @Test
  void finalRoundStartIsIdempotentAndReadyStateExpiresAtThirtySeconds() {
    Battle battle = battle();
    Instant readyAt = STARTED_AT.plusSeconds(1);
    Instant expiresAt = readyAt.plusSeconds(10);
    battle.prepareFinalRound(readyAt);

    assertFalse(battle.isFinalRoundStartExpired(readyAt.plusSeconds(29), Duration.ofSeconds(30)));
    assertTrue(battle.isFinalRoundStartExpired(readyAt.plusSeconds(30), Duration.ofSeconds(30)));

    battle.startFinalRound(expiresAt);
    battle.startFinalRound(expiresAt.plusSeconds(10));

    assertEquals(expiresAt, battle.getFinalExpiresAt());
  }

  @Test
  void requiresUserAndGymLeaderAndClientRequestIdAndStartedAt() {
    assertThrows(
        NullPointerException.class, () -> Battle.start(null, 2L, CLIENT_REQUEST_ID, STARTED_AT));
    assertThrows(
        NullPointerException.class, () -> Battle.start(1L, null, CLIENT_REQUEST_ID, STARTED_AT));
    assertThrows(NullPointerException.class, () -> Battle.start(1L, 2L, null, STARTED_AT));
    assertThrows(NullPointerException.class, () -> Battle.start(1L, 2L, CLIENT_REQUEST_ID, null));
  }

  private Battle battle() {
    return Battle.start(1L, 2L, CLIENT_REQUEST_ID, STARTED_AT);
  }
}
