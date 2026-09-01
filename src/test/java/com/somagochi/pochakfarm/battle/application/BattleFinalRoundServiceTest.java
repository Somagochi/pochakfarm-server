package com.somagochi.pochakfarm.battle.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.somagochi.pochakfarm.battle.domain.Battle;
import com.somagochi.pochakfarm.battle.domain.BattleResult;
import com.somagochi.pochakfarm.battle.domain.BattleStatus;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundResultRequest;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundResultResponse;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundStartResponse;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleActionRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleBroadcastEventRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleEntryRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderAnimalRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BattleFinalRoundServiceTest {

  private static final Long USER_ID = 213L;
  private static final Long OTHER_USER_ID = 214L;
  private static final Instant READY_AT = Instant.parse("2026-09-01T00:00:00Z");

  @Autowired private BattleFinalRoundService battleFinalRoundService;
  @Autowired private BattleRepository battleRepository;
  @Autowired private BattleEntryRepository battleEntryRepository;
  @Autowired private BattleActionRepository battleActionRepository;
  @Autowired private BattleBroadcastEventRepository battleBroadcastEventRepository;
  @Autowired private GymLeaderRepository gymLeaderRepository;
  @Autowired private GymLeaderAnimalRepository gymLeaderAnimalRepository;

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
    now = READY_AT.plusSeconds(1);
    given(clock.instant()).willAnswer(invocation -> now);
  }

  @Test
  void startsThreeSecondFinalRoundAfterClientIsReadyAndRetryDoesNotExtendIt() {
    Battle battle = readyBattle(0);

    BattleFinalRoundStartResponse first = battleFinalRoundService.start(USER_ID, battle.getId());
    now = now.plusSeconds(1);
    BattleFinalRoundStartResponse retried = battleFinalRoundService.start(USER_ID, battle.getId());

    assertEquals(READY_AT.plusSeconds(4), first.finalRound().inputExpiresAt());
    assertEquals(READY_AT.plusSeconds(5), first.finalRound().submissionExpiresAt());
    assertEquals(first.finalRound().inputExpiresAt(), retried.finalRound().inputExpiresAt());
  }

  @Test
  void losesWhenFinalRoundIsNotStartedWithinThirtySeconds() {
    Battle battle = readyBattle(0);
    now = READY_AT.plusSeconds(30);

    BattleFinalRoundStartResponse response = battleFinalRoundService.start(USER_ID, battle.getId());

    assertEquals(BattleStatus.FINISHED, response.battleStatus());
    assertEquals(BattleResult.LOSE, response.battleResult());
  }

  @Test
  void convertsTapCountToPointsAndWinsFromTwoPointDeficitAtTwentyTaps() {
    Battle battle = readyBattle(-2);
    battleFinalRoundService.start(USER_ID, battle.getId());
    now = READY_AT.plusSeconds(4);

    BattleFinalRoundResultResponse response =
        battleFinalRoundService.submit(
            USER_ID, battle.getId(), new BattleFinalRoundResultRequest(20));

    assertEquals(1, response.barPosition());
    assertEquals(3, response.finalRound().point());
    assertEquals(BattleResult.WIN, response.battleResult());
  }

  @Test
  void exactCenterIsLossAndDuplicateResultReturnsFirstResolution() {
    Battle battle = readyBattle(-2);
    battleFinalRoundService.start(USER_ID, battle.getId());

    BattleFinalRoundResultResponse first =
        battleFinalRoundService.submit(
            USER_ID, battle.getId(), new BattleFinalRoundResultRequest(19));
    BattleFinalRoundResultResponse retried =
        battleFinalRoundService.submit(
            USER_ID, battle.getId(), new BattleFinalRoundResultRequest(100));

    assertEquals(0, first.barPosition());
    assertEquals(BattleResult.LOSE, first.battleResult());
    assertEquals(first, retried);
  }

  @Test
  void acceptsSubmissionAtGraceDeadlineAndLosesAfterIt() {
    Battle accepted = readyBattle(0);
    battleFinalRoundService.start(USER_ID, accepted.getId());
    now = READY_AT.plusSeconds(5);

    assertEquals(
        BattleResult.WIN,
        battleFinalRoundService
            .submit(USER_ID, accepted.getId(), new BattleFinalRoundResultRequest(5))
            .battleResult());

    Battle expired = readyBattle(0);
    now = READY_AT.plusSeconds(1);
    battleFinalRoundService.start(USER_ID, expired.getId());
    now = READY_AT.plusSeconds(5).plusNanos(1);

    assertEquals(
        BattleResult.LOSE,
        battleFinalRoundService
            .submit(USER_ID, expired.getId(), new BattleFinalRoundResultRequest(20))
            .battleResult());
  }

  @Test
  void rejectsInvalidStateInvalidTapCountAndOtherUsersBattle() {
    Battle notReady = fixture().start(USER_ID, READY_AT);
    assertBusinessException(
        ErrorCode.BATTLE_FINAL_ROUND_NOT_READY,
        () -> battleFinalRoundService.start(USER_ID, notReady.getId()));

    Battle ready = readyBattle(0);
    assertBusinessException(
        ErrorCode.BATTLE_FINAL_ROUND_NOT_STARTED,
        () ->
            battleFinalRoundService.submit(
                USER_ID, ready.getId(), new BattleFinalRoundResultRequest(5)));
    battleFinalRoundService.start(USER_ID, ready.getId());
    assertBusinessException(
        ErrorCode.INVALID_PARAMETER,
        () ->
            battleFinalRoundService.submit(
                USER_ID, ready.getId(), new BattleFinalRoundResultRequest(-1)));
    assertBusinessException(
        ErrorCode.FORBIDDEN_BATTLE_ACCESS,
        () -> battleFinalRoundService.start(OTHER_USER_ID, ready.getId()));
  }

  private Battle readyBattle(int barPosition) {
    Battle battle = fixture().barPosition(barPosition).start(USER_ID, READY_AT);
    battle.prepareFinalRound(READY_AT);
    return battleRepository.save(battle);
  }

  private BattleFixture fixture() {
    return new BattleFixture(
        battleRepository, battleEntryRepository, gymLeaderRepository, gymLeaderAnimalRepository);
  }

  private void assertBusinessException(ErrorCode errorCode, Runnable executable) {
    BusinessException exception = assertThrows(BusinessException.class, executable::run);
    assertEquals(errorCode.getCode(), exception.getCode());
  }
}
