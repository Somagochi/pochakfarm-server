package com.somagochi.pochakfarm.battle.application;

import com.somagochi.pochakfarm.battle.domain.Battle;
import com.somagochi.pochakfarm.battle.domain.BattleAction;
import com.somagochi.pochakfarm.battle.domain.BattleAdvantageResolver;
import com.somagochi.pochakfarm.battle.domain.BattleBroadcastEvent;
import com.somagochi.pochakfarm.battle.domain.BattleBroadcastEventGenerator;
import com.somagochi.pochakfarm.battle.domain.BattleBroadcastEventSpec;
import com.somagochi.pochakfarm.battle.domain.BattleEntry;
import com.somagochi.pochakfarm.battle.domain.BattleEventCode;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.BattlePosition;
import com.somagochi.pochakfarm.battle.domain.BattlePositionChange;
import com.somagochi.pochakfarm.battle.domain.BattleResult;
import com.somagochi.pochakfarm.battle.domain.BattleSide;
import com.somagochi.pochakfarm.battle.domain.NpcSkillSelector;
import com.somagochi.pochakfarm.battle.domain.SkillActivationResult;
import com.somagochi.pochakfarm.battle.domain.SkillBattleResolution;
import com.somagochi.pochakfarm.battle.domain.SkillBattleResolver;
import com.somagochi.pochakfarm.battle.dto.BattleActionRequest;
import com.somagochi.pochakfarm.battle.dto.BattleActionResponse;
import com.somagochi.pochakfarm.battle.dto.BattleBroadcastEventResponse;
import com.somagochi.pochakfarm.battle.dto.BattleFinalRoundStateResponse;
import com.somagochi.pochakfarm.battle.dto.BattleSkillOutcomeResponse;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleActionRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleBroadcastEventRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleEntryRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleRepository;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BattleActionService {

  private final BattleRepository battleRepository;
  private final BattleEntryRepository battleEntryRepository;
  private final BattleActionRepository battleActionRepository;
  private final BattleBroadcastEventRepository battleBroadcastEventRepository;
  private final BattleAdvantageResolver battleAdvantageResolver;
  private final BattleBroadcastEventGenerator battleBroadcastEventGenerator;
  private final SkillBattleResolver skillBattleResolver;
  private final NpcSkillSelector npcSkillSelector;
  private final BattlePolicy battlePolicy;
  private final BattleRewardService battleRewardService;
  private final Clock clock;

  public BattleActionService(
      BattleRepository battleRepository,
      BattleEntryRepository battleEntryRepository,
      BattleActionRepository battleActionRepository,
      BattleBroadcastEventRepository battleBroadcastEventRepository,
      BattleAdvantageResolver battleAdvantageResolver,
      BattleBroadcastEventGenerator battleBroadcastEventGenerator,
      SkillBattleResolver skillBattleResolver,
      NpcSkillSelector npcSkillSelector,
      BattlePolicy battlePolicy,
      BattleRewardService battleRewardService,
      Clock clock) {
    this.battleRepository = battleRepository;
    this.battleEntryRepository = battleEntryRepository;
    this.battleActionRepository = battleActionRepository;
    this.battleBroadcastEventRepository = battleBroadcastEventRepository;
    this.battleAdvantageResolver = battleAdvantageResolver;
    this.battleBroadcastEventGenerator = battleBroadcastEventGenerator;
    this.skillBattleResolver = skillBattleResolver;
    this.npcSkillSelector = npcSkillSelector;
    this.battlePolicy = battlePolicy;
    this.battleRewardService = battleRewardService;
    this.clock = clock;
  }

  @Transactional
  public BattleActionResponse selectSkill(Long userId, Long battleId, BattleActionRequest request) {
    Battle battle =
        battleRepository
            .findByIdForUpdate(battleId)
            .orElseThrow(() -> new BusinessException(ErrorCode.BATTLE_NOT_FOUND));
    if (!battle.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN_BATTLE_ACCESS);
    }

    int actionSeq = requireActionSeqInRange(request.actionSeq());
    Optional<BattleAction> alreadyResolved =
        battleActionRepository.findByBattleIdAndActionSeq(battleId, actionSeq);
    if (alreadyResolved.isPresent()) {
      return replay(battle, alreadyResolved.get());
    }
    if (!battle.isInProgress()) {
      throw new BusinessException(ErrorCode.BATTLE_NOT_IN_PROGRESS);
    }
    if (actionSeq != nextActionSeq(battleId)) {
      throw new BusinessException(ErrorCode.BATTLE_ACTION_SEQUENCE_MISMATCH);
    }

    Instant now = clock.instant();
    int entryOrder = BattleAction.entryOrderOf(actionSeq);
    BattleEntry userEntry = entry(battleId, BattleSide.USER, entryOrder);
    BattleEntry npcEntry = entry(battleId, BattleSide.NPC, entryOrder);
    Optional<CardSkill> selectedSkill = validateSelectedSkill(userEntry, request.skill());

    BroadcastEventLog eventLog = new BroadcastEventLog(battleId, actionSeq, nextEventSeq(battleId));
    BattlePosition position =
        applyEntryAdvantages(
            battleId, entryOrder, BattlePosition.of(battle.getBarPosition()), eventLog);
    if (position.isTerminal()) {
      throw new BusinessException(ErrorCode.BATTLE_NOT_IN_PROGRESS);
    }

    CardSkill npcSkill =
        npcSkillSelector.select(position, npcEntry.getSkill1(), npcEntry.getSkill2());
    SkillBattleResolution resolution =
        skillBattleResolver.resolve(position, selectedSkill, npcSkill);
    eventLog.recordAll(entryOrder, battleBroadcastEventGenerator.skill(resolution));

    saveAction(BattleAction.from(battleId, actionSeq, resolution));
    position = resolution.positionChange().after();
    if (!position.isTerminal() && startsNextEntry(actionSeq)) {
      position = applyEntryAdvantages(battleId, entryOrder + 1, position, eventLog);
    }

    battle.applyAction(position.value(), now);
    if (position.isTerminal()) {
      finish(battle, resultOf(position), now);
    } else if (actionSeq == BattlePolicy.TOTAL_ACTION_COUNT) {
      completeActions(battle, position, now);
    }
    List<BattleBroadcastEvent> events =
        battleBroadcastEventRepository.saveAll(eventLog.recordedEvents());

    return toResponse(battle, actionSeq, resolution.user(), resolution.npc(), position, events);
  }

  private BattleActionResponse replay(Battle battle, BattleAction action) {
    return toResponse(
        battle,
        action.getActionSeq(),
        action.userActivation(),
        action.npcActivation(),
        BattlePosition.of(positionAfter(battle, action)),
        battleBroadcastEventRepository.findByBattleIdAndActionSeqOrderByEventSeqAsc(
            action.getBattleId(), action.getActionSeq()));
  }

  private int positionAfter(Battle battle, BattleAction action) {
    return battleActionRepository
        .findByBattleIdAndActionSeq(action.getBattleId(), action.getActionSeq() + 1)
        .map(BattleAction::getBarPositionBefore)
        .orElseGet(action::getBarPositionAfter);
  }

  private BattleActionResponse toResponse(
      Battle battle,
      int actionSeq,
      SkillActivationResult user,
      SkillActivationResult npc,
      BattlePosition position,
      List<BattleBroadcastEvent> events) {
    Integer nextActionSeq = remainingActionSeq(battle);
    return new BattleActionResponse(
        battle.getId(),
        actionSeq,
        BattleAction.entryOrderOf(actionSeq),
        BattleAction.actionNoInEntryOf(actionSeq),
        BattleSkillOutcomeResponse.from(user),
        BattleSkillOutcomeResponse.from(npc),
        user.points() - npc.points(),
        position.value(),
        BattlePolicy.MIN_BAR_POSITION,
        BattlePolicy.MAX_BAR_POSITION,
        battle.getStatus(),
        battle.getResult(),
        nextActionSeq,
        BattleFinalRoundStateResponse.from(battle, battlePolicy),
        battle.isInProgress() ? null : battleRewardService.findResult(battle),
        BattleBroadcastEventResponse.from(events));
  }

  private void saveAction(BattleAction action) {
    try {
      battleActionRepository.saveAndFlush(action);
    } catch (DataIntegrityViolationException e) {
      throw new BusinessException(ErrorCode.BATTLE_ACTION_CONFLICT);
    }
  }

  private BattlePosition applyEntryAdvantages(
      Long battleId, int entryOrder, BattlePosition position, BroadcastEventLog eventLog) {
    if (battleBroadcastEventRepository.existsByBattleIdAndEntryOrder(battleId, entryOrder)) {
      return position;
    }
    BattleEntry userEntry = entry(battleId, BattleSide.USER, entryOrder);
    BattleEntry npcEntry = entry(battleId, BattleSide.NPC, entryOrder);

    BattlePositionChange tierChange =
        battleAdvantageResolver.resolveTier(position, userEntry.getTier(), npcEntry.getTier());
    eventLog.recordAll(
        entryOrder,
        battleBroadcastEventGenerator.advantage(BattleEventCode.TIER_ADVANTAGE, tierChange));
    if (tierChange.after().isTerminal()) {
      return tierChange.after();
    }

    BattlePositionChange typeChange =
        battleAdvantageResolver.resolveType(
            tierChange.after(), userEntry.getCardType(), npcEntry.getCardType());
    eventLog.recordAll(
        entryOrder,
        battleBroadcastEventGenerator.advantage(BattleEventCode.TYPE_ADVANTAGE, typeChange));
    return typeChange.after();
  }

  private Optional<CardSkill> validateSelectedSkill(BattleEntry userEntry, CardSkill skill) {
    if (skill == null) {
      return Optional.empty();
    }
    if (skill != userEntry.getSkill1() && skill != userEntry.getSkill2()) {
      throw new BusinessException(ErrorCode.INVALID_BATTLE_ENTRY);
    }
    return Optional.of(skill);
  }

  private BattleEntry entry(Long battleId, BattleSide side, int orderNo) {
    return battleEntryRepository
        .findByBattleIdAndSideAndOrderNo(battleId, side, orderNo)
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_BATTLE_ENTRY));
  }

  private boolean startsNextEntry(int actionSeq) {
    return BattleAction.actionNoInEntryOf(actionSeq) == BattlePolicy.ACTIONS_PER_ENTRY
        && BattleAction.entryOrderOf(actionSeq) < BattlePolicy.ENTRY_COUNT;
  }

  private BattleResult resultOf(BattlePosition position) {
    return position.value() == BattlePolicy.MAX_BAR_POSITION ? BattleResult.WIN : BattleResult.LOSE;
  }

  private void completeActions(Battle battle, BattlePosition position, Instant now) {
    if (battlePolicy.requiresFinalRound(position.value())) {
      battle.prepareFinalRound(now);
      return;
    }
    finish(
        battle,
        position.value() > BattlePolicy.INITIAL_BAR_POSITION ? BattleResult.WIN : BattleResult.LOSE,
        now);
  }

  private void finish(Battle battle, BattleResult result, Instant now) {
    battle.finish(result, now);
    if (result == BattleResult.WIN) {
      battleRewardService.grantFirstClear(battle);
    }
  }

  private Integer remainingActionSeq(Battle battle) {
    if (!battle.isInProgress()) {
      return null;
    }
    int nextActionSeq = nextActionSeq(battle.getId());
    return nextActionSeq > BattlePolicy.TOTAL_ACTION_COUNT ? null : nextActionSeq;
  }

  private int nextActionSeq(Long battleId) {
    return battleActionRepository
        .findFirstByBattleIdOrderByActionSeqDesc(battleId)
        .map(action -> action.getActionSeq() + 1)
        .orElse(1);
  }

  private int nextEventSeq(Long battleId) {
    return battleBroadcastEventRepository
        .findFirstByBattleIdOrderByEventSeqDesc(battleId)
        .map(event -> event.getEventSeq() + 1)
        .orElse(1);
  }

  private int requireActionSeqInRange(Integer actionSeq) {
    if (actionSeq == null || actionSeq < 1 || actionSeq > BattlePolicy.TOTAL_ACTION_COUNT) {
      throw new BusinessException(ErrorCode.BATTLE_ACTION_SEQUENCE_MISMATCH);
    }
    return actionSeq;
  }

  private static final class BroadcastEventLog {

    private final Long battleId;
    private final Integer actionSeq;
    private final List<BattleBroadcastEvent> events = new ArrayList<>();
    private int eventSeq;

    private BroadcastEventLog(Long battleId, Integer actionSeq, int firstEventSeq) {
      this.battleId = battleId;
      this.actionSeq = actionSeq;
      this.eventSeq = firstEventSeq;
    }

    private void record(
        Integer entryOrder,
        BattleEventCode eventCode,
        BattleSide animalSide,
        CardSkill skill,
        BattleSide winnerSide,
        Integer point) {
      events.add(
          BattleBroadcastEvent.record(
              battleId,
              eventSeq++,
              actionSeq,
              entryOrder,
              eventCode,
              animalSide,
              skill,
              winnerSide,
              point));
    }

    private void recordAll(Integer entryOrder, List<BattleBroadcastEventSpec> specs) {
      specs.forEach(
          spec ->
              record(
                  entryOrder,
                  spec.eventCode(),
                  spec.animalSide(),
                  spec.skill(),
                  spec.winnerSide(),
                  spec.points()));
    }

    private List<BattleBroadcastEvent> recordedEvents() {
      return events;
    }
  }
}
