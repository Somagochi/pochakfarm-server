package com.somagochi.pochakfarm.battle.application;

import static com.somagochi.pochakfarm.battle.application.BattleFixture.NPC_GAMBLE_SKILL;
import static com.somagochi.pochakfarm.battle.application.BattleFixture.NPC_STABLE_SKILL;
import static com.somagochi.pochakfarm.battle.application.BattleFixture.USER_GAMBLE_SKILL;
import static com.somagochi.pochakfarm.battle.application.BattleFixture.USER_STABLE_SKILL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import com.somagochi.pochakfarm.battle.domain.BattleActionPolicy;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.BattleSide;
import com.somagochi.pochakfarm.battle.domain.BattleStatus;
import com.somagochi.pochakfarm.battle.dto.BattleActionRequest;
import com.somagochi.pochakfarm.battle.dto.BattleBroadcastEventResponse;
import com.somagochi.pochakfarm.battle.dto.BattleStateResponse;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleActionRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleBroadcastEventRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleEntryRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderAnimalRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.random.RandomProvider;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BattleStateQueryServiceTest {

  private static final Long USER_ID = 2120L;
  private static final Long OTHER_USER_ID = 2121L;
  private static final Instant STARTED_AT = Instant.parse("2026-08-25T00:00:00Z");
  private static final int PERCENTAGE_BOUND = 100;

  @Autowired private BattleActionService battleActionService;
  @Autowired private BattleStateQueryService battleStateQueryService;
  @Autowired private BattleRepository battleRepository;
  @Autowired private BattleEntryRepository battleEntryRepository;
  @Autowired private BattleActionRepository battleActionRepository;
  @Autowired private BattleBroadcastEventRepository battleBroadcastEventRepository;
  @Autowired private GymLeaderRepository gymLeaderRepository;
  @Autowired private GymLeaderAnimalRepository gymLeaderAnimalRepository;

  @MockitoBean private RandomProvider randomProvider;
  @MockitoBean private Clock clock;

  private Instant now;

  @BeforeEach
  void setUp() {
    battleActionRepository.deleteAll();
    battleBroadcastEventRepository.deleteAll();
    battleEntryRepository.deleteAll();
    battleRepository.deleteAll();
    gymLeaderAnimalRepository.deleteAll();
    gymLeaderRepository.deleteAll();
    now = STARTED_AT;
    given(clock.instant()).willAnswer(invocation -> now);
    AtomicInteger callCount = new AtomicInteger();
    given(randomProvider.nextInt(PERCENTAGE_BOUND))
        .willAnswer(invocation -> callCount.getAndIncrement() % 2 == 0 ? 0 : PERCENTAGE_BOUND - 1);
  }

  @Test
  void restoresProgressOfBattleInTheMiddleOfSecondEntry() {
    Long battleId = startBattle();
    for (int actionSeq = 1; actionSeq <= 4; actionSeq++) {
      battleActionService.selectSkill(
          USER_ID, battleId, new BattleActionRequest(actionSeq, USER_STABLE_SKILL));
    }

    BattleStateResponse state = battleStateQueryService.getBattle(USER_ID, battleId);

    assertEquals(BattleStatus.IN_PROGRESS, state.status());
    assertNull(state.result());
    assertEquals(4, state.barPosition());
    assertEquals(BattlePolicy.MIN_BAR_POSITION, state.minBarPosition());
    assertEquals(BattlePolicy.MAX_BAR_POSITION, state.maxBarPosition());
    assertEquals(4, state.completedActionCount());
    assertEquals(BattlePolicy.TOTAL_ACTION_COUNT, state.totalActionCount());
    assertEquals(2, state.currentEntryOrder());
    assertEquals(5, state.nextActionSeq());
    assertEquals(
        now.plus(BattleActionPolicy.SKILL_SELECTION_TIME_LIMIT), state.nextSelectionExpiresAt());
    assertEquals(BattleSide.USER, state.userEntry().side());
    assertEquals(2, state.userEntry().orderNo());
    assertEquals("유저2", state.userEntry().animalName());
    assertEquals(2, state.userEntry().skills().size());
    assertEquals(BattleSide.NPC, state.npcEntry().side());
    assertEquals("관장2", state.npcEntry().animalName());
    assertNull(state.npcEntry().skills());

    List<BattleBroadcastEventResponse> events = state.broadcastEvents();
    assertEquals(
        events.stream()
            .sorted(Comparator.comparingInt(BattleBroadcastEventResponse::eventSeq))
            .toList(),
        events);
    assertTrue(events.stream().anyMatch(event -> event.animalSide() == BattleSide.USER));
    assertTrue(
        events.stream().anyMatch(event -> event.actionSeq() != null && event.actionSeq() == 4));
  }

  @Test
  void restoresBattleThatHasNotStartedAnyAction() {
    Long battleId = startBattle();

    BattleStateResponse state = battleStateQueryService.getBattle(USER_ID, battleId);

    assertEquals(0, state.completedActionCount());
    assertEquals(1, state.currentEntryOrder());
    assertEquals(1, state.nextActionSeq());
    assertNotNull(state.nextSelectionExpiresAt());
    assertEquals(BattlePolicy.INITIAL_BAR_POSITION, state.barPosition());
    assertTrue(state.broadcastEvents().isEmpty());
  }

  @Test
  void rejectsOtherUsersBattle() {
    Long battleId = startBattle();

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> battleStateQueryService.getBattle(OTHER_USER_ID, battleId));

    assertEquals(ErrorCode.FORBIDDEN_BATTLE_ACCESS.getCode(), exception.getCode());
  }

  private Long startBattle() {
    return new BattleFixture(
            battleRepository, battleEntryRepository, gymLeaderRepository, gymLeaderAnimalRepository)
        .userSkills(USER_STABLE_SKILL, USER_GAMBLE_SKILL)
        .npcSkills(NPC_STABLE_SKILL, NPC_GAMBLE_SKILL)
        .start(USER_ID, STARTED_AT)
        .getId();
  }
}
