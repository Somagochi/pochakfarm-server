package com.somagochi.pochakfarm.battle.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.battle.domain.BattleEventCode;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.BattleResult;
import com.somagochi.pochakfarm.battle.domain.BattleSide;
import com.somagochi.pochakfarm.battle.domain.BattleStatus;
import com.somagochi.pochakfarm.battle.domain.GymLeader;
import com.somagochi.pochakfarm.battle.dto.BattleActionRequest;
import com.somagochi.pochakfarm.battle.dto.BattleActionResponse;
import com.somagochi.pochakfarm.battle.dto.BattleBroadcastEventResponse;
import com.somagochi.pochakfarm.battle.dto.BattleEntryRequest;
import com.somagochi.pochakfarm.battle.dto.BattleStartRequest;
import com.somagochi.pochakfarm.battle.dto.BattleStartResponse;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.random.RandomProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BattleStartServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-25T05:00:00Z");

  @Autowired private BattleStartService battleStartService;
  @Autowired private BattleActionService battleActionService;
  @Autowired private BattleStateQueryService battleStateQueryService;
  @Autowired private AnimalRepository animalRepository;
  @Autowired private BattleFixtures fixtures;
  @MockitoBean private RandomProvider randomProvider;

  private Long userId;
  private GymLeader firstGymLeader;
  private List<Animal> myAnimals;

  @BeforeEach
  void setUp() {
    fixtures.cleanUp();
    userId = fixtures.createUser();
    firstGymLeader = fixtures.createGymLeader(1, BattlePolicy.ENTRY_COUNT);
    myAnimals =
        List.of(
            fixtures.createAnimal(userId, CardType.SKY, Tier.A, CardSkill.SKY_FEATHER_GUARD),
            fixtures.createAnimal(userId, CardType.SKY, Tier.B, CardSkill.SKY_TAILWIND),
            fixtures.createAnimal(userId, CardType.SKY, Tier.C, CardSkill.SKY_CLOUD_CUSHION));
  }

  @AfterEach
  void tearDown() {
    fixtures.cleanUp();
  }

  @Test
  void startsBattleAndReservesThirtyMinuteRestFromServerTime() {
    BattleStartResponse response =
        battleStartService.start(userId, request(firstGymLeader.getId(), myAnimals), NOW);

    assertNotNull(response.battleId());
    assertEquals(3, response.barPosition());
    assertEquals(BattlePolicy.MIN_BAR_POSITION, response.minBarPosition());
    assertEquals(BattlePolicy.MAX_BAR_POSITION, response.maxBarPosition());
    assertEquals(1, response.userEntry().orderNo());
    assertEquals(1, response.npcEntry().orderNo());
    assertEquals(BattlePolicy.ENTRY_COUNT, response.rests().size());

    var state = battleStateQueryService.getBattle(userId, response.battleId());
    assertEquals(3, state.barPosition());
    assertEquals(4, state.broadcastEvents().size());
    assertEquals(BattleEventCode.TIER_ADVANTAGE, state.broadcastEvents().get(0).eventCode());
    assertEquals(BattleEventCode.BATTLE_POINT_APPLIED, state.broadcastEvents().get(1).eventCode());
    assertEquals(BattleSide.USER, state.broadcastEvents().get(1).winnerSide());
    assertEquals(BattleEventCode.TYPE_ADVANTAGE, state.broadcastEvents().get(2).eventCode());
    assertEquals(BattleEventCode.BATTLE_POINT_APPLIED, state.broadcastEvents().get(3).eventCode());
    state
        .broadcastEvents()
        .forEach(
            event -> {
              assertNull(event.actionSeq());
              assertEquals(1, event.entryOrder());
            });

    var action =
        battleActionService.selectSkill(
            userId, response.battleId(), new BattleActionRequest(1, CardSkill.SKY_FEATHER_GUARD));
    assertFalse(
        action.broadcastEvents().stream()
            .anyMatch(
                event ->
                    event.eventCode() == BattleEventCode.TIER_ADVANTAGE
                        || event.eventCode() == BattleEventCode.TYPE_ADVANTAGE));

    Instant expectedRestEndsAt = NOW.plus(Duration.ofMinutes(30));
    for (Animal animal : myAnimals) {
      assertEquals(
          expectedRestEndsAt,
          animalRepository.findById(animal.getId()).orElseThrow().getRestEndsAt());
    }
  }

  @Test
  void exposesUserSkillDetailsAndNpcSkillPreviews() {
    BattleStartResponse response =
        battleStartService.start(userId, request(firstGymLeader.getId(), myAnimals), NOW);

    assertNotNull(response.userEntry().skill1());
    assertNotNull(response.userEntry().skill1().battleType());
    assertEquals(80, response.userEntry().skill1().triggerPercentage());
    assertEquals(1, response.userEntry().skill1().point());
    assertEquals(2, response.npcEntry().skills().size());
    assertEquals("나뭇잎 방어", response.npcEntry().skills().getFirst().name());
    assertEquals(
        CardSkill.GROUND_LEAF_GUARD.battleType(),
        response.npcEntry().skills().getFirst().battleType());
  }

  @Test
  void simulatesBattleFromInitialStateThroughAllThreeEntries() {
    given(randomProvider.nextInt(100)).willReturn(99);
    BattleStartResponse started =
        battleStartService.start(userId, request(firstGymLeader.getId(), myAnimals), NOW);
    List<CardSkill> skills =
        List.of(CardSkill.SKY_FEATHER_GUARD, CardSkill.SKY_TAILWIND, CardSkill.SKY_CLOUD_CUSHION);

    var initialState = battleStateQueryService.getBattle(userId, started.battleId());
    assertEquals(
        List.of(
            BattleEventCode.TIER_ADVANTAGE,
            BattleEventCode.BATTLE_POINT_APPLIED,
            BattleEventCode.TYPE_ADVANTAGE,
            BattleEventCode.BATTLE_POINT_APPLIED),
        eventCodes(initialState.broadcastEvents()));

    BattleActionResponse response = null;
    for (int actionSeq = 1; actionSeq <= BattlePolicy.TOTAL_ACTION_COUNT; actionSeq++) {
      CardSkill skill = skills.get((actionSeq - 1) / BattlePolicy.ACTIONS_PER_ENTRY);
      response =
          battleActionService.selectSkill(
              userId, started.battleId(), new BattleActionRequest(actionSeq, skill));

      if (actionSeq == 1) {
        assertEquals(
            List.of(BattleEventCode.SKILL_FAILED, BattleEventCode.SKILL_FAILED),
            eventCodes(response.broadcastEvents()));
      }
      if (actionSeq == 3) {
        assertEquals(
            List.of(
                BattleEventCode.SKILL_FAILED,
                BattleEventCode.SKILL_FAILED,
                BattleEventCode.TIER_ADVANTAGE,
                BattleEventCode.BATTLE_POINT_APPLIED,
                BattleEventCode.TYPE_ADVANTAGE,
                BattleEventCode.BATTLE_POINT_APPLIED),
            eventCodes(response.broadcastEvents()));
      }
      if (actionSeq == 6) {
        assertEquals(
            List.of(
                BattleEventCode.SKILL_FAILED,
                BattleEventCode.SKILL_FAILED,
                BattleEventCode.TYPE_ADVANTAGE,
                BattleEventCode.BATTLE_POINT_APPLIED),
            eventCodes(response.broadcastEvents()));
      }
    }

    assertNotNull(response);
    assertEquals(6, response.barPosition());
    assertEquals(BattleStatus.FINISHED, response.battleStatus());
    assertEquals(BattleResult.WIN, response.battleResult());

    var finalState = battleStateQueryService.getBattle(userId, started.battleId());
    assertEquals(BattlePolicy.TOTAL_ACTION_COUNT, finalState.completedActionCount());
    assertEquals(28, finalState.broadcastEvents().size());
  }

  @Test
  void returnsFirstResultForRepeatedClientRequestId() {
    BattleStartRequest request = request(firstGymLeader.getId(), myAnimals);

    BattleStartResponse first = battleStartService.start(userId, request, NOW);
    BattleStartResponse second = battleStartService.start(userId, request, NOW);

    assertEquals(first, second);
  }

  @Test
  void rejectsLockedGymLeaderWhenPreviousBadgeIsMissing() {
    GymLeader second = fixtures.createGymLeader(2, BattlePolicy.ENTRY_COUNT);
    fixtures.changeLevel(userId, 40);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> battleStartService.start(userId, request(second.getId(), myAnimals), NOW));

    assertEquals(ErrorCode.GYM_LEADER_LOCKED.getCode(), exception.getCode());
  }

  @Test
  void rejectsLockedGymLeaderWhenLevelIsNotEnough() {
    GymLeader second = fixtures.createGymLeader(2, BattlePolicy.ENTRY_COUNT);
    fixtures.grantBadge(userId, firstGymLeader.getBadgeCode());
    fixtures.changeLevel(userId, 1);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> battleStartService.start(userId, request(second.getId(), myAnimals), NOW));

    assertEquals(ErrorCode.GYM_LEADER_LOCKED.getCode(), exception.getCode());
  }

  @Test
  void startsUnlockedGymLeaderWhenBothConditionsAreSatisfied() {
    GymLeader second = fixtures.createGymLeader(2, BattlePolicy.ENTRY_COUNT);
    fixtures.grantBadge(userId, firstGymLeader.getBadgeCode());
    fixtures.changeLevel(userId, 3);

    assertNotNull(battleStartService.start(userId, request(second.getId(), myAnimals), NOW));
  }

  @Test
  void rejectsAnimalOwnedByAnotherUser() {
    Long otherUserId = fixtures.createUser();
    Animal foreign =
        fixtures.createAnimal(otherUserId, CardType.SEA, Tier.A, CardSkill.SEA_BUBBLE_GUARD);
    List<Animal> entries = List.of(myAnimals.get(0), myAnimals.get(1), foreign);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> battleStartService.start(userId, request(firstGymLeader.getId(), entries), NOW));

    assertEquals(ErrorCode.ANIMAL_NOT_FOUND.getCode(), exception.getCode());
  }

  @Test
  void rejectsDuplicatedAnimal() {
    List<Animal> entries = List.of(myAnimals.get(0), myAnimals.get(0), myAnimals.get(1));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> battleStartService.start(userId, request(firstGymLeader.getId(), entries), NOW));

    assertEquals(ErrorCode.INVALID_BATTLE_ENTRY.getCode(), exception.getCode());
  }

  @Test
  void rejectsEntryCountOtherThanThree() {
    List<Animal> entries = List.of(myAnimals.get(0), myAnimals.get(1));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> battleStartService.start(userId, request(firstGymLeader.getId(), entries), NOW));

    assertEquals(ErrorCode.INVALID_BATTLE_ENTRY.getCode(), exception.getCode());
  }

  @Test
  void rejectsRestingAnimalAndKeepsOtherAnimalsFreeOfRest() {
    Animal resting = myAnimals.get(2);
    fixtures.markResting(resting.getId(), NOW.plus(Duration.ofMinutes(10)));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                battleStartService.start(userId, request(firstGymLeader.getId(), myAnimals), NOW));

    assertEquals(ErrorCode.BATTLE_ANIMAL_RESTING.getCode(), exception.getCode());
    assertNull(animalRepository.findById(myAnimals.get(0).getId()).orElseThrow().getRestEndsAt());
    assertNull(animalRepository.findById(myAnimals.get(1).getId()).orElseThrow().getRestEndsAt());
  }

  @Test
  void rejectsSecondBattleWhileAnotherIsInProgress() {
    battleStartService.start(userId, request(firstGymLeader.getId(), myAnimals), NOW);
    List<Animal> others =
        List.of(
            fixtures.createAnimal(userId, CardType.SEA, Tier.A, CardSkill.SEA_BUBBLE_GUARD),
            fixtures.createAnimal(userId, CardType.SEA, Tier.B, CardSkill.SEA_FOAM_ROLL),
            fixtures.createAnimal(userId, CardType.SEA, Tier.C, CardSkill.SEA_CORAL_HIDE));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> battleStartService.start(userId, request(firstGymLeader.getId(), others), NOW));

    assertEquals(ErrorCode.BATTLE_ALREADY_IN_PROGRESS.getCode(), exception.getCode());
  }

  private BattleStartRequest request(Long gymLeaderId, List<Animal> animals) {
    List<BattleEntryRequest> entries =
        java.util.stream.IntStream.range(0, animals.size())
            .mapToObj(index -> new BattleEntryRequest(animals.get(index).getId(), index + 1))
            .toList();
    return new BattleStartRequest(gymLeaderId, UUID.randomUUID().toString(), entries);
  }

  private List<BattleEventCode> eventCodes(List<BattleBroadcastEventResponse> events) {
    return events.stream().map(event -> event.eventCode()).toList();
  }
}
