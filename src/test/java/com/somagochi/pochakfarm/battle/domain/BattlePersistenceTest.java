package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BattlePersistenceTest {

  private static final Instant STARTED_AT = Instant.parse("2026-08-19T01:00:00Z");

  @Autowired private EntityManager entityManager;

  @Test
  void persistsBattleWithStringStatusAndNullableResult() {
    Battle battle = persistBattle(1L, 1L);

    Object[] row =
        (Object[])
            entityManager
                .createNativeQuery(
                    """
                    SELECT status, result, started_at, ended_at
                    FROM battles
                    WHERE id = :id
                    """)
                .setParameter("id", battle.getId())
                .getSingleResult();

    assertEquals(BattleStatus.IN_PROGRESS.name(), row[0].toString());
    assertNull(row[1]);
    assertNull(row[3]);
  }

  @Test
  void persistsUserAndNpcEntriesInOneTable() {
    Battle battle = persistBattle(1L, 1L);
    for (int orderNo = 1; orderNo <= BattleEntry.ENTRY_COUNT; orderNo++) {
      entityManager.persist(userEntry(battle.getId(), orderNo, (long) orderNo));
      entityManager.persist(npcEntry(battle.getId(), orderNo, (long) orderNo));
    }
    entityManager.flush();

    Number count =
        (Number)
            entityManager
                .createNativeQuery(
                    "SELECT COUNT(*) FROM battle_entries WHERE battle_id = :id AND side = 'NPC'")
                .setParameter("id", battle.getId())
                .getSingleResult();

    assertEquals(BattleEntry.ENTRY_COUNT, count.intValue());
  }

  @Test
  void allowsThreeNpcEntriesWithNullCaptureId() {
    Battle battle = persistBattle(1L, 1L);

    for (int orderNo = 1; orderNo <= BattleEntry.ENTRY_COUNT; orderNo++) {
      entityManager.persist(npcEntry(battle.getId(), orderNo, (long) orderNo));
    }
    entityManager.flush();

    Number count =
        (Number)
            entityManager
                .createNativeQuery(
                    "SELECT COUNT(*) FROM battle_entries "
                        + "WHERE battle_id = :id AND capture_id IS NULL")
                .setParameter("id", battle.getId())
                .getSingleResult();

    assertEquals(BattleEntry.ENTRY_COUNT, count.intValue());
  }

  @Test
  void rejectsDuplicateCaptureInSameBattle() {
    Battle battle = persistBattle(1L, 1L);
    entityManager.persist(userEntry(battle.getId(), 1, 100L));
    entityManager.flush();

    assertThrows(
        RuntimeException.class,
        () -> {
          entityManager.persist(userEntry(battle.getId(), 2, 100L));
          entityManager.flush();
        });
  }

  @Test
  void rejectsDuplicateOrderNoOnSameSide() {
    Battle battle = persistBattle(1L, 1L);
    entityManager.persist(userEntry(battle.getId(), 1, 100L));
    entityManager.flush();

    assertThrows(
        RuntimeException.class,
        () -> {
          entityManager.persist(userEntry(battle.getId(), 1, 200L));
          entityManager.flush();
        });
  }

  @Test
  void rejectsDuplicateGymLeaderChallengeOrder() {
    entityManager.persist(gymLeader(1));
    entityManager.flush();

    assertThrows(
        RuntimeException.class,
        () -> {
          entityManager.persist(gymLeader(1));
          entityManager.flush();
        });
  }

  @Test
  void persistsFinishedResultAsString() {
    Battle battle = persistBattle(1L, 1L);
    battle.finish(BattleResult.WIN, STARTED_AT.plusSeconds(600));
    entityManager.flush();
    entityManager.clear();

    Battle saved = entityManager.find(Battle.class, battle.getId());

    assertEquals(BattleStatus.FINISHED, saved.getStatus());
    assertEquals(BattleResult.WIN, saved.getResult());
    assertEquals(STARTED_AT.plusSeconds(600), saved.getEndedAt());
  }

  private Battle persistBattle(Long userId, Long gymLeaderId) {
    Battle battle = Battle.start(userId, gymLeaderId, STARTED_AT);
    entityManager.persist(battle);
    entityManager.flush();
    return battle;
  }

  private GymLeader gymLeader(int challengeOrder) {
    String suffix = UUID.randomUUID().toString();
    return GymLeader.create("GYM-" + suffix, "관장", challengeOrder, "BDG-" + suffix, null);
  }

  private BattleEntry userEntry(Long battleId, Integer orderNo, Long captureId) {
    return BattleEntry.ofUser(battleId, orderNo, captureId);
  }

  private BattleEntry npcEntry(Long battleId, Integer orderNo, Long gymLeaderAnimalId) {
    return BattleEntry.ofNpc(battleId, orderNo, gymLeaderAnimalId);
  }
}
