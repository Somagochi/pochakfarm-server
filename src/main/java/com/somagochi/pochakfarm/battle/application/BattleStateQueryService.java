package com.somagochi.pochakfarm.battle.application;

import com.somagochi.pochakfarm.battle.domain.Battle;
import com.somagochi.pochakfarm.battle.domain.BattleAction;
import com.somagochi.pochakfarm.battle.domain.BattleActionPolicy;
import com.somagochi.pochakfarm.battle.domain.BattleEntry;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.BattleSide;
import com.somagochi.pochakfarm.battle.dto.BattleBroadcastEventResponse;
import com.somagochi.pochakfarm.battle.dto.BattleEntryResponse;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundStateResponse;
import com.somagochi.pochakfarm.battle.dto.BattleStateResponse;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleActionRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleBroadcastEventRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleEntryRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BattleStateQueryService {

  private final BattleRepository battleRepository;
  private final BattleEntryRepository battleEntryRepository;
  private final BattleActionRepository battleActionRepository;
  private final BattleBroadcastEventRepository battleBroadcastEventRepository;
  private final BattlePolicy battlePolicy;
  private final BattleActionPolicy battleActionPolicy;
  private final BattleFinalRoundService battleFinalRoundService;
  private final BattleRewardService battleRewardService;
  private final Clock clock;

  public BattleStateQueryService(
      BattleRepository battleRepository,
      BattleEntryRepository battleEntryRepository,
      BattleActionRepository battleActionRepository,
      BattleBroadcastEventRepository battleBroadcastEventRepository,
      BattlePolicy battlePolicy,
      BattleActionPolicy battleActionPolicy,
      BattleFinalRoundService battleFinalRoundService,
      BattleRewardService battleRewardService,
      Clock clock) {
    this.battleRepository = battleRepository;
    this.battleEntryRepository = battleEntryRepository;
    this.battleActionRepository = battleActionRepository;
    this.battleBroadcastEventRepository = battleBroadcastEventRepository;
    this.battlePolicy = battlePolicy;
    this.battleActionPolicy = battleActionPolicy;
    this.battleFinalRoundService = battleFinalRoundService;
    this.battleRewardService = battleRewardService;
    this.clock = clock;
  }

  @Transactional
  public BattleStateResponse getBattle(Long userId, Long battleId) {
    Battle battle =
        battleRepository
            .findByIdForUpdate(battleId)
            .orElseThrow(() -> new BusinessException(ErrorCode.BATTLE_NOT_FOUND));
    if (!battle.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN_BATTLE_ACCESS);
    }
    Instant now = clock.instant();
    battleFinalRoundService.finishWhenTimedOut(battle, now);

    int completedActionCount = battleActionRepository.countByBattleId(battleId);
    Integer nextActionSeq = nextActionSeq(battle, completedActionCount);
    int currentEntryOrder =
        BattleAction.entryOrderOf(
            nextActionSeq == null ? BattlePolicy.TOTAL_ACTION_COUNT : nextActionSeq);

    return new BattleStateResponse(
        battle.getId(),
        battle.getGymLeaderId(),
        battle.getStatus(),
        battle.getResult(),
        battle.getBarPosition(),
        BattlePolicy.MIN_BAR_POSITION,
        BattlePolicy.MAX_BAR_POSITION,
        completedActionCount,
        BattlePolicy.TOTAL_ACTION_COUNT,
        currentEntryOrder,
        nextActionSeq,
        nextActionSeq == null
            ? null
            : battleActionPolicy.selectionExpiresAt(battle.lastProgressAt()),
        BattleEntryResponse.from(entry(battleId, BattleSide.USER, currentEntryOrder), battlePolicy),
        BattleEntryResponse.from(entry(battleId, BattleSide.NPC, currentEntryOrder), battlePolicy),
        BattleFinalRoundStateResponse.from(battle, battlePolicy),
        battle.isInProgress() ? null : battleRewardService.findResult(battle),
        BattleBroadcastEventResponse.from(
            battleBroadcastEventRepository.findByBattleIdOrderByEventSeqAsc(battleId)));
  }

  private Integer nextActionSeq(Battle battle, int completedActionCount) {
    if (!battle.isInProgress() || completedActionCount >= BattlePolicy.TOTAL_ACTION_COUNT) {
      return null;
    }
    return completedActionCount + 1;
  }

  private BattleEntry entry(Long battleId, BattleSide side, int orderNo) {
    return battleEntryRepository
        .findByBattleIdAndSideAndOrderNo(battleId, side, orderNo)
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_BATTLE_ENTRY));
  }
}
