package com.somagochi.pochakfarm.battle.application;

import static com.somagochi.pochakfarm.battle.application.BattleFixture.NPC_GAMBLE_SKILL;
import static com.somagochi.pochakfarm.battle.application.BattleFixture.NPC_STABLE_SKILL;
import static com.somagochi.pochakfarm.battle.application.BattleFixture.USER_GAMBLE_SKILL;
import static com.somagochi.pochakfarm.battle.application.BattleFixture.USER_STABLE_SKILL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.battle.domain.BattleActionPolicy;
import com.somagochi.pochakfarm.battle.domain.BattleEventCode;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.BattleResult;
import com.somagochi.pochakfarm.battle.domain.BattleSide;
import com.somagochi.pochakfarm.battle.domain.BattleStatus;
import com.somagochi.pochakfarm.battle.domain.SkillActivationStatus;
import com.somagochi.pochakfarm.battle.dto.BattleActionRequest;
import com.somagochi.pochakfarm.battle.dto.BattleActionResponse;
import com.somagochi.pochakfarm.battle.dto.BattleBroadcastEventResponse;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleActionRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleBroadcastEventRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleEntryRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderAnimalRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderRepository;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.random.RandomProvider;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BattleActionServiceTest {

  private static final Long USER_ID = 212L;
  private static final Long OTHER_USER_ID = 213L;
  private static final Instant STARTED_AT = Instant.parse("2026-08-25T00:00:00Z");
  private static final int PERCENTAGE_BOUND = 100;

  @Autowired private BattleActionService battleActionService;
  @Autowired private BattleRepository battleRepository;
  @Autowired private BattleEntryRepository battleEntryRepository;
  @Autowired private BattleActionRepository battleActionRepository;
  @Autowired private BattleBroadcastEventRepository battleBroadcastEventRepository;
  @Autowired private GymLeaderRepository gymLeaderRepository;
  @Autowired private GymLeaderAnimalRepository gymLeaderAnimalRepository;

  @MockitoBean private RandomProvider randomProvider;
  @MockitoBean private Clock clock;
  @MockitoBean private BattleRewardService battleRewardService;

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
  }

  @Test
  void processesNineActionsInOrderAndKeepsBarPositionAcrossEntrySwaps() {
    Long battleId = startBattle();
    activateUserOnly();

    List<BattleActionResponse> responses = new ArrayList<>();
    for (int actionSeq = 1; actionSeq <= BattlePolicy.TOTAL_ACTION_COUNT; actionSeq++) {
      responses.add(select(battleId, actionSeq, USER_STABLE_SKILL));
    }

    BattleActionResponse lastActionOfFirstEntry = responses.get(2);
    BattleActionResponse firstActionOfSecondEntry = responses.get(3);
    BattleActionResponse lastAction = responses.get(8);

    assertEquals(1, lastActionOfFirstEntry.entryOrder());
    assertEquals(3, lastActionOfFirstEntry.actionNoInEntry());
    assertEquals(3, lastActionOfFirstEntry.barPosition());
    assertEquals(2, firstActionOfSecondEntry.entryOrder());
    assertEquals(1, firstActionOfSecondEntry.actionNoInEntry());
    assertEquals(4, firstActionOfSecondEntry.barPosition());
    assertEquals(3, lastAction.entryOrder());
    assertEquals(9, lastAction.barPosition());
    assertEquals(BattleStatus.FINISHED, lastAction.battleStatus());
    assertEquals(BattleResult.WIN, lastAction.battleResult());
    assertNull(lastAction.nextActionSeq());
    assertEquals(BattlePolicy.TOTAL_ACTION_COUNT, battleActionRepository.countByBattleId(battleId));
  }

  @Test
  void preparesFinalRoundOnlyForTieOrUpToTwoPointDeficit() {
    for (int barPosition = 0; barPosition >= -2; barPosition--) {
      Long battleId = fixture().barPosition(barPosition).start(USER_ID, STARTED_AT).getId();
      failBothSides();

      BattleActionResponse response = playAllActions(battleId);
      var battle = battleRepository.findById(battleId).orElseThrow();

      assertEquals(BattleStatus.IN_PROGRESS, response.battleStatus());
      assertNull(response.battleResult());
      assertEquals(STARTED_AT, battle.getFinalReadyAt());
    }
  }

  @Test
  void finishesImmediatelyAfterNinthActionWhenUserLeadsOrTrailsByThree() {
    Long winningBattleId = fixture().barPosition(1).start(USER_ID, STARTED_AT).getId();
    failBothSides();
    BattleActionResponse winning = playAllActions(winningBattleId);

    Long losingBattleId = fixture().barPosition(-3).start(USER_ID, STARTED_AT).getId();
    failBothSides();
    BattleActionResponse losing = playAllActions(losingBattleId);

    assertEquals(BattleResult.WIN, winning.battleResult());
    assertEquals(BattleResult.LOSE, losing.battleResult());
    assertEquals(BattleStatus.FINISHED, winning.battleStatus());
    assertEquals(BattleStatus.FINISHED, losing.battleStatus());
  }

  @Test
  void rejectsActionSeqOutOfOrder() {
    Long battleId = startBattle();
    activateUserOnly();

    assertBusinessException(
        ErrorCode.BATTLE_ACTION_SEQUENCE_MISMATCH, () -> select(battleId, 2, USER_STABLE_SKILL));
    assertBusinessException(
        ErrorCode.BATTLE_ACTION_SEQUENCE_MISMATCH, () -> select(battleId, 0, USER_STABLE_SKILL));
    assertBusinessException(
        ErrorCode.BATTLE_ACTION_SEQUENCE_MISMATCH,
        () -> select(battleId, BattlePolicy.TOTAL_ACTION_COUNT + 1, USER_STABLE_SKILL));
  }

  @Test
  void returnsFirstResolutionWhenSameActionSeqIsRequestedAgain() {
    Long battleId = startBattle();
    randomValues(0, PERCENTAGE_BOUND - 1);

    BattleActionResponse first = select(battleId, 1, USER_GAMBLE_SKILL);
    BattleActionResponse retried = select(battleId, 1, USER_STABLE_SKILL);

    assertEquals(USER_GAMBLE_SKILL, first.user().skill());
    assertEquals(first.user().skill(), retried.user().skill());
    assertEquals(first.user().status(), retried.user().status());
    assertEquals(first.npc().skill(), retried.npc().skill());
    assertEquals(first.npc().status(), retried.npc().status());
    assertEquals(first.netPoint(), retried.netPoint());
    assertEquals(first.barPosition(), retried.barPosition());
    assertEquals(first.broadcastEvents(), retried.broadcastEvents());
    assertEquals(1, battleActionRepository.countByBattleId(battleId));
    verify(randomProvider, times(2)).nextInt(PERCENTAGE_BOUND);
  }

  @Test
  void replaysNinthActionPositionBeforeFinalRoundResult() {
    Long battleId = fixture().barPosition(-2).start(USER_ID, STARTED_AT).getId();
    failBothSides();
    BattleActionResponse ninthAction = playAllActions(battleId);
    var battle = battleRepository.findById(battleId).orElseThrow();
    battle.startFinalRound(STARTED_AT.plusSeconds(3));
    battle.applyFinalRound(20, 3, 1);
    battle.finish(BattleResult.WIN, STARTED_AT.plusSeconds(3));
    battleRepository.save(battle);

    BattleActionResponse replayed =
        select(battleId, BattlePolicy.TOTAL_ACTION_COUNT, USER_GAMBLE_SKILL);

    assertEquals(-2, ninthAction.barPosition());
    assertEquals(ninthAction.barPosition(), replayed.barPosition());
  }

  @Test
  void separatesNotSelectedFromFailedActivation() {
    Long battleId = startBattle();
    randomValues(0, PERCENTAGE_BOUND - 1, PERCENTAGE_BOUND - 1);

    BattleActionResponse notSelected = select(battleId, 1, null);

    assertEquals(SkillActivationStatus.NOT_SELECTED, notSelected.user().status());
    assertNull(notSelected.user().skill());
    assertEquals(0, notSelected.user().point());
    assertEquals(NPC_STABLE_SKILL, notSelected.npc().skill());
    assertEquals(SkillActivationStatus.ACTIVATED, notSelected.npc().status());
    assertEquals(-1, notSelected.netPoint());
    assertEquals(-1, notSelected.barPosition());
    assertTrue(
        notSelected.broadcastEvents().stream()
            .anyMatch(
                event ->
                    event.animalSide() == BattleSide.USER
                        && event.eventCode() == BattleEventCode.SKILL_NOT_SELECTED));

    BattleActionResponse failed = select(battleId, 2, USER_STABLE_SKILL);

    assertEquals(SkillActivationStatus.FAILED, failed.user().status());
    assertEquals(USER_STABLE_SKILL, failed.user().skill());
    assertEquals(0, failed.user().point());
    assertEquals(-1, failed.barPosition());
    assertTrue(
        failed.broadcastEvents().stream()
            .anyMatch(
                event ->
                    event.animalSide() == BattleSide.USER
                        && event.eventCode() == BattleEventCode.SKILL_FAILED));
  }

  @Test
  void rejectsSkillSelectionArrivedAfterServerDeadlineButAcceptsNoSelection() {
    Long battleId = startBattle();
    randomValues(PERCENTAGE_BOUND - 1);
    now = STARTED_AT.plusSeconds(4);

    assertBusinessException(
        ErrorCode.BATTLE_ACTION_SELECTION_CLOSED, () -> select(battleId, 1, USER_STABLE_SKILL));

    BattleActionResponse response = select(battleId, 1, null);

    assertEquals(SkillActivationStatus.NOT_SELECTED, response.user().status());
    assertEquals(
        now.plus(BattleActionPolicy.SKILL_SELECTION_TIME_LIMIT), response.nextSelectionExpiresAt());
  }

  @Test
  void finishesBattleImmediatelyWhenBarPositionReachesMaximum() {
    Long battleId = startBattle();
    activateUserOnly();

    BattleActionResponse response = null;
    for (int actionSeq = 1; actionSeq <= 5; actionSeq++) {
      response = select(battleId, actionSeq, USER_GAMBLE_SKILL);
    }

    assertEquals(BattlePolicy.MAX_BAR_POSITION, response.barPosition());
    assertEquals(BattleStatus.FINISHED, response.battleStatus());
    assertEquals(BattleResult.WIN, response.battleResult());
    assertNull(response.nextActionSeq());
    assertNull(response.nextSelectionExpiresAt());
    assertBusinessException(
        ErrorCode.BATTLE_NOT_IN_PROGRESS, () -> select(battleId, 6, USER_GAMBLE_SKILL));
    assertEquals(5, battleActionRepository.countByBattleId(battleId));
  }

  @Test
  void appliesTierAndTypeAdvantageWhenEntryStarts() {
    Long battleId =
        fixture()
            .tiers(Tier.SSS, Tier.C)
            .cardTypes(CardType.SEA, CardType.SPACE)
            .start(USER_ID, STARTED_AT)
            .getId();
    randomValues(PERCENTAGE_BOUND - 1, PERCENTAGE_BOUND - 1);

    BattleActionResponse response = select(battleId, 1, USER_STABLE_SKILL);

    assertEquals(0, response.netPoint());
    assertEquals(
        BattlePolicy.MAX_TIER_MOVE_DISTANCE + BattlePolicy.TYPE_ADVANTAGE_MOVE_DISTANCE,
        response.barPosition());
    List<BattleBroadcastEventResponse> events = response.broadcastEvents();
    assertEquals(BattleEventCode.TIER_ADVANTAGE, events.get(0).eventCode());
    assertEquals(BattleEventCode.BATTLE_POINT_APPLIED, events.get(1).eventCode());
    assertEquals(BattlePolicy.MAX_TIER_MOVE_DISTANCE, events.get(1).point());
    assertEquals(BattleEventCode.TYPE_ADVANTAGE, events.get(2).eventCode());
    assertEquals(BattleEventCode.BATTLE_POINT_APPLIED, events.get(3).eventCode());
    assertEquals(BattlePolicy.TYPE_ADVANTAGE_MOVE_DISTANCE, events.get(3).point());
    assertEquals(BattleEventCode.SKILL_FAILED, events.get(4).eventCode());
    assertEquals(BattleSide.USER, events.get(4).animalSide());
    assertEquals(BattleEventCode.SKILL_FAILED, events.get(5).eventCode());
    assertEquals(BattleSide.NPC, events.get(5).animalSide());
  }

  @Test
  void rejectsSkillThatCurrentEntryDoesNotHave() {
    Long battleId = startBattle();

    assertBusinessException(
        ErrorCode.INVALID_BATTLE_ENTRY, () -> select(battleId, 1, CardSkill.SKY_WIND_DASH));
  }

  @Test
  void rejectsUnknownBattleAndOtherUsersBattle() {
    Long battleId = startBattle();

    assertBusinessException(
        ErrorCode.BATTLE_NOT_FOUND,
        () ->
            battleActionService.selectSkill(
                USER_ID, battleId + 1_000L, new BattleActionRequest(1, USER_STABLE_SKILL)));
    assertBusinessException(
        ErrorCode.FORBIDDEN_BATTLE_ACCESS,
        () ->
            battleActionService.selectSkill(
                OTHER_USER_ID, battleId, new BattleActionRequest(1, USER_STABLE_SKILL)));
  }

  private BattleActionResponse select(Long battleId, int actionSeq, CardSkill skill) {
    return battleActionService.selectSkill(
        USER_ID, battleId, new BattleActionRequest(actionSeq, skill));
  }

  private void activateUserOnly() {
    AtomicInteger callCount = new AtomicInteger();
    given(randomProvider.nextInt(PERCENTAGE_BOUND))
        .willAnswer(invocation -> callCount.getAndIncrement() % 2 == 0 ? 0 : PERCENTAGE_BOUND - 1);
  }

  private void failBothSides() {
    given(randomProvider.nextInt(PERCENTAGE_BOUND)).willReturn(PERCENTAGE_BOUND - 1);
  }

  private BattleActionResponse playAllActions(Long battleId) {
    BattleActionResponse response = null;
    for (int actionSeq = 1; actionSeq <= BattlePolicy.TOTAL_ACTION_COUNT; actionSeq++) {
      response = select(battleId, actionSeq, USER_STABLE_SKILL);
    }
    return response;
  }

  private void randomValues(int... values) {
    Queue<Integer> queue = new ArrayDeque<>();
    for (int value : values) {
      queue.add(value);
    }
    given(randomProvider.nextInt(PERCENTAGE_BOUND)).willAnswer(invocation -> queue.poll());
  }

  private Long startBattle() {
    return fixture().start(USER_ID, STARTED_AT).getId();
  }

  private BattleFixture fixture() {
    return new BattleFixture(
            battleRepository, battleEntryRepository, gymLeaderRepository, gymLeaderAnimalRepository)
        .userSkills(USER_STABLE_SKILL, USER_GAMBLE_SKILL)
        .npcSkills(NPC_STABLE_SKILL, NPC_GAMBLE_SKILL);
  }

  private void assertBusinessException(ErrorCode errorCode, Executable executable) {
    BusinessException exception = assertThrows(BusinessException.class, executable);
    assertEquals(errorCode.getCode(), exception.getCode());
  }
}
