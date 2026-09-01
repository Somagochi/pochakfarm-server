package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
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
                    SELECT status, result, started_at, ended_at, bar_position,
                           last_action_at, final_ready_at, final_expires_at, final_tap_count, final_points
                    FROM battles
                    WHERE id = :id
                    """)
                .setParameter("id", battle.getId())
                .getSingleResult();

    assertEquals(BattleStatus.IN_PROGRESS.name(), row[0].toString());
    assertNull(row[1]);
    assertNull(row[3]);
    assertEquals(BattlePolicy.INITIAL_BAR_POSITION, ((Number) row[4]).intValue());
    assertNull(row[5]);
    assertNull(row[6]);
    assertNull(row[7]);
    assertNull(row[8]);
    assertNull(row[9]);
  }

  @Test
  void rejectsDuplicateClientRequestIdForSameUser() {
    String clientRequestId = UUID.randomUUID().toString();
    entityManager.persist(Battle.start(1L, 1L, clientRequestId, STARTED_AT));
    entityManager.flush();

    assertThrows(
        RuntimeException.class,
        () -> {
          entityManager.persist(Battle.start(1L, 2L, clientRequestId, STARTED_AT));
          entityManager.flush();
        });
  }

  @Test
  void persistsUserAndNpcEntriesInOneTable() {
    Battle battle = persistBattle(1L, 1L);
    GymLeader leader = persistGymLeader(1);
    for (int orderNo = 1; orderNo <= BattleEntry.ENTRY_COUNT; orderNo++) {
      entityManager.persist(userEntry(battle.getId(), orderNo, (long) orderNo));
      entityManager.persist(
          BattleEntry.ofNpc(battle.getId(), persistGymLeaderAnimal(leader, orderNo)));
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
    GymLeader leader = persistGymLeader(1);

    for (int orderNo = 1; orderNo <= BattleEntry.ENTRY_COUNT; orderNo++) {
      entityManager.persist(
          BattleEntry.ofNpc(battle.getId(), persistGymLeaderAnimal(leader, orderNo)));
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
  void persistsEntrySnapshotColumns() {
    Battle battle = persistBattle(1L, 1L);
    entityManager.persist(userEntry(battle.getId(), 1, 100L));
    entityManager.flush();

    Object[] row =
        (Object[])
            entityManager
                .createNativeQuery(
                    """
                    SELECT animal_name, card_type, tier, skill_1, skill_2
                    FROM battle_entries
                    WHERE battle_id = :id AND side = 'USER' AND order_no = 1
                    """)
                .setParameter("id", battle.getId())
                .getSingleResult();

    assertEquals("두부", row[0].toString());
    assertEquals(CardType.GROUND.name(), row[1].toString());
    assertEquals(Tier.C.name(), row[2].toString());
    assertEquals(CardSkill.GROUND_MOSS_CUSHION.name(), row[3].toString());
    assertEquals(CardSkill.GROUND_STONE_TAP.name(), row[4].toString());
  }

  @Test
  void copiesGymLeaderAnimalSnapshotIntoNpcEntry() {
    Battle battle = persistBattle(1L, 1L);
    GymLeaderAnimal animal = persistGymLeaderAnimal(persistGymLeader(1), 2);

    BattleEntry entry = BattleEntry.ofNpc(battle.getId(), animal);
    entityManager.persist(entry);
    entityManager.flush();

    assertEquals(BattleSide.NPC, entry.getSide());
    assertEquals(animal.getOrderNo(), entry.getOrderNo());
    assertEquals(animal.getId(), entry.getGymLeaderAnimalId());
    assertEquals(animal.getAnimalName(), entry.getAnimalName());
    assertEquals(animal.getCardType(), entry.getCardType());
    assertEquals(animal.getTier(), entry.getTier());
    assertEquals(animal.getSkill1(), entry.getSkill1());
    assertEquals(animal.getSkill2(), entry.getSkill2());
    assertNull(entry.getCaptureId());
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
    persistGymLeader(1);

    assertThrows(
        RuntimeException.class,
        () -> {
          entityManager.persist(gymLeader(1));
          entityManager.flush();
        });
  }

  @Test
  void rejectsDuplicateGymLeaderAnimalOrderNo() {
    GymLeader leader = persistGymLeader(1);
    persistGymLeaderAnimal(leader, 1);

    assertThrows(
        RuntimeException.class,
        () -> {
          entityManager.persist(gymLeaderAnimal(leader.getId(), 1));
          entityManager.flush();
        });
  }

  @Test
  void rejectsDuplicateActionSeqInSameBattle() {
    Battle battle = persistBattle(1L, 1L);
    entityManager.persist(action(battle.getId(), 1));
    entityManager.flush();

    assertThrows(
        RuntimeException.class,
        () -> {
          entityManager.persist(action(battle.getId(), 1));
          entityManager.flush();
        });
  }

  @Test
  void persistsActionDerivedEntryColumns() {
    Battle battle = persistBattle(1L, 1L);
    BattleAction saved = action(battle.getId(), 5);
    entityManager.persist(saved);
    entityManager.flush();
    entityManager.clear();

    BattleAction found = entityManager.find(BattleAction.class, saved.getId());

    assertEquals(2, found.getEntryOrder());
    assertEquals(2, found.getActionNoInEntry());
    assertEquals(CardSkill.GROUND_MOSS_CUSHION, found.getUserSkill());
    assertEquals(CardSkill.SEA_WAVE_DASH, found.getNpcSkill());
    assertEquals(-1, found.getNetMoveDistance());
  }

  @Test
  void rejectsDuplicateEventSeqInSameBattle() {
    Battle battle = persistBattle(1L, 1L);
    entityManager.persist(broadcastEvent(battle.getId(), 1));
    entityManager.flush();

    assertThrows(
        RuntimeException.class,
        () -> {
          entityManager.persist(broadcastEvent(battle.getId(), 1));
          entityManager.flush();
        });
  }

  @Test
  void allowsBroadcastEventWithoutActionSeq() {
    Battle battle = persistBattle(1L, 1L);
    entityManager.persist(
        BattleBroadcastEvent.record(
            battle.getId(),
            1,
            null,
            1,
            BattleEventCode.BATTLE_POINT_APPLIED,
            null,
            null,
            BattleSide.NPC,
            1));
    entityManager.flush();

    Object[] row =
        (Object[])
            entityManager
                .createNativeQuery(
                    """
                    SELECT action_seq, event_code, param_animal_side, param_skill,
                           param_winner_side, param_points
                    FROM battle_broadcast_events
                    WHERE battle_id = :id AND event_seq = 1
                    """)
                .setParameter("id", battle.getId())
                .getSingleResult();

    assertNull(row[0]);
    assertEquals(BattleEventCode.BATTLE_POINT_APPLIED.name(), row[1].toString());
    assertNull(row[2]);
    assertNull(row[3]);
    assertEquals(BattleSide.NPC.name(), row[4].toString());
    assertEquals(1, ((Number) row[5]).intValue());
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
    Battle battle = Battle.start(userId, gymLeaderId, UUID.randomUUID().toString(), STARTED_AT);
    entityManager.persist(battle);
    entityManager.flush();
    return battle;
  }

  private GymLeader persistGymLeader(int challengeOrder) {
    GymLeader leader = gymLeader(challengeOrder);
    entityManager.persist(leader);
    entityManager.flush();
    return leader;
  }

  private GymLeaderAnimal persistGymLeaderAnimal(GymLeader leader, int orderNo) {
    GymLeaderAnimal animal = gymLeaderAnimal(leader.getId(), orderNo);
    entityManager.persist(animal);
    entityManager.flush();
    return animal;
  }

  private GymLeader gymLeader(int challengeOrder) {
    String suffix = UUID.randomUUID().toString();
    return GymLeader.create("GYM-" + suffix, "관장", challengeOrder, "BDG-" + suffix, null);
  }

  private GymLeaderAnimal gymLeaderAnimal(Long gymLeaderId, int orderNo) {
    return GymLeaderAnimal.create(
        gymLeaderId,
        orderNo,
        AnimalName.from("흙방울"),
        CardType.GROUND,
        Tier.C,
        CardSkill.GROUND_MOSS_CUSHION,
        CardSkill.GROUND_STONE_TAP,
        null);
  }

  private BattleEntry userEntry(Long battleId, Integer orderNo, Long captureId) {
    return BattleEntry.ofUser(
        battleId,
        orderNo,
        captureId,
        AnimalName.from("두부"),
        CardType.GROUND,
        Tier.C,
        CardSkill.GROUND_MOSS_CUSHION,
        CardSkill.GROUND_STONE_TAP);
  }

  private BattleAction action(Long battleId, int actionSeq) {
    return BattleAction.record(
        battleId,
        actionSeq,
        CardSkill.GROUND_MOSS_CUSHION,
        true,
        1,
        CardSkill.SEA_WAVE_DASH,
        true,
        2,
        -1,
        0,
        -1);
  }

  private BattleBroadcastEvent broadcastEvent(Long battleId, int eventSeq) {
    return BattleBroadcastEvent.record(
        battleId,
        eventSeq,
        1,
        1,
        BattleEventCode.SKILL_TRIGGERED,
        BattleSide.USER,
        CardSkill.GROUND_MOSS_CUSHION,
        null,
        null);
  }
}
