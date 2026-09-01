package com.somagochi.pochakfarm.battle.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.GymLeader;
import com.somagochi.pochakfarm.battle.dto.BattleEntryRequest;
import com.somagochi.pochakfarm.battle.dto.BattleStartRequest;
import com.somagochi.pochakfarm.battle.dto.BattleStartResponse;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BattleStartServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-25T05:00:00Z");

  @Autowired private BattleStartService battleStartService;
  @Autowired private AnimalRepository animalRepository;
  @Autowired private BattleFixtures fixtures;

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
    assertEquals(BattlePolicy.INITIAL_BAR_POSITION, response.barPosition());
    assertEquals(BattlePolicy.MIN_BAR_POSITION, response.minBarPosition());
    assertEquals(BattlePolicy.MAX_BAR_POSITION, response.maxBarPosition());
    assertEquals(1, response.userEntry().orderNo());
    assertEquals(1, response.npcEntry().orderNo());
    assertEquals(BattlePolicy.ENTRY_COUNT, response.rests().size());

    Instant expectedRestEndsAt = NOW.plus(Duration.ofMinutes(30));
    for (Animal animal : myAnimals) {
      assertEquals(
          expectedRestEndsAt,
          animalRepository.findById(animal.getId()).orElseThrow().getRestEndsAt());
    }
  }

  @Test
  void exposesUserSkillPointsButNotNpcSkills() {
    BattleStartResponse response =
        battleStartService.start(userId, request(firstGymLeader.getId(), myAnimals), NOW);

    assertNotNull(response.userEntry().skill1());
    assertNotNull(response.userEntry().skill1().battleType());
    assertEquals(80, response.userEntry().skill1().triggerPercentage());
    assertEquals(1, response.userEntry().skill1().point());
  }

  @Test
  void returnsFirstResultForRepeatedClientRequestId() {
    BattleStartRequest request = request(firstGymLeader.getId(), myAnimals);

    BattleStartResponse first = battleStartService.start(userId, request, NOW);
    BattleStartResponse second = battleStartService.start(userId, request, NOW);

    assertEquals(first.battleId(), second.battleId());
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
}
