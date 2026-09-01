package com.somagochi.pochakfarm.battle.application;

import com.somagochi.pochakfarm.battle.domain.Battle;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.BattlePosition;
import com.somagochi.pochakfarm.battle.domain.BattleResult;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundResultRequest;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundResultResponse;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundStartResponse;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BattleFinalRoundService {

  private final BattleRepository battleRepository;
  private final BattlePolicy battlePolicy;
  private final BattleRewardService battleRewardService;
  private final Clock clock;

  @Transactional
  public BattleFinalRoundStartResponse start(Long userId, Long battleId) {
    Battle battle = battleForUpdate(userId, battleId);
    Instant now = clock.instant();
    finishWhenTimedOut(battle, now);
    if (!battle.isInProgress()) {
      return startResponse(battle);
    }
    if (!battle.isFinalRoundReady() && !battle.isFinalRoundStarted()) {
      throw new BusinessException(ErrorCode.BATTLE_FINAL_ROUND_NOT_READY);
    }

    battle.startFinalRound(now.plus(battlePolicy.finalRoundDuration()));
    return startResponse(battle);
  }

  @Transactional
  public BattleFinalRoundResultResponse submit(
      Long userId, Long battleId, BattleFinalRoundResultRequest request) {
    Battle battle = battleForUpdate(userId, battleId);
    Instant now = clock.instant();
    finishWhenTimedOut(battle, now);
    if (!battle.isInProgress()) {
      return resultResponse(battle);
    }
    if (!battle.isFinalRoundStarted()) {
      throw new BusinessException(ErrorCode.BATTLE_FINAL_ROUND_NOT_STARTED);
    }

    int tapCount = requireTapCount(request);
    int point = battlePolicy.finalRoundPoints(tapCount);
    int position = BattlePosition.of(battle.getBarPosition()).move(point).after().value();
    battle.applyFinalRound(tapCount, point, position);
    BattleResult result =
        position > BattlePolicy.INITIAL_BAR_POSITION ? BattleResult.WIN : BattleResult.LOSE;
    battle.finish(result, now);
    if (result == BattleResult.WIN) {
      battleRewardService.grantFirstClear(battle);
    }
    return resultResponse(battle);
  }

  private Battle battleForUpdate(Long userId, Long battleId) {
    Battle battle =
        battleRepository
            .findByIdForUpdate(battleId)
            .orElseThrow(() -> new BusinessException(ErrorCode.BATTLE_NOT_FOUND));
    if (!battle.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN_BATTLE_ACCESS);
    }
    return battle;
  }

  void finishWhenTimedOut(Battle battle, Instant now) {
    if (!battle.isInProgress()) {
      return;
    }
    boolean startExpired =
        battle.isFinalRoundStartExpired(now, battlePolicy.finalRoundStartTimeout());
    boolean submissionExpired =
        battle.isFinalRoundSubmissionExpired(now, battlePolicy.finalRoundSubmissionGrace());
    if (startExpired || submissionExpired) {
      battle.finish(BattleResult.LOSE, now);
    }
  }

  private BattleFinalRoundStartResponse startResponse(Battle battle) {
    return BattleFinalRoundStartResponse.from(
        battle,
        battlePolicy,
        battle.isInProgress() ? null : battleRewardService.findResult(battle));
  }

  private BattleFinalRoundResultResponse resultResponse(Battle battle) {
    return BattleFinalRoundResultResponse.from(
        battle,
        battlePolicy,
        battle.isInProgress() ? null : battleRewardService.findResult(battle));
  }

  private int requireTapCount(BattleFinalRoundResultRequest request) {
    if (request == null || request.tapCount() == null || request.tapCount() < 0) {
      throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
    return request.tapCount();
  }
}
