package com.somagochi.pochakfarm.battle.application;

import com.somagochi.pochakfarm.animal.application.AnimalQueryService;
import com.somagochi.pochakfarm.animal.application.AnimalRestService;
import com.somagochi.pochakfarm.animal.dto.AnimalBattleProfile;
import com.somagochi.pochakfarm.battle.domain.Battle;
import com.somagochi.pochakfarm.battle.domain.BattleEntry;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.BattleSide;
import com.somagochi.pochakfarm.battle.domain.BattleStatus;
import com.somagochi.pochakfarm.battle.domain.GymLeader;
import com.somagochi.pochakfarm.battle.domain.GymLeaderAnimal;
import com.somagochi.pochakfarm.battle.domain.GymLeaderUnlock;
import com.somagochi.pochakfarm.battle.dto.BattleEntryRequest;
import com.somagochi.pochakfarm.battle.dto.BattleNpcEntryResponse;
import com.somagochi.pochakfarm.battle.dto.BattleRestResponse;
import com.somagochi.pochakfarm.battle.dto.BattleSkillResponse;
import com.somagochi.pochakfarm.battle.dto.BattleStartRequest;
import com.somagochi.pochakfarm.battle.dto.BattleStartResponse;
import com.somagochi.pochakfarm.battle.dto.BattleUserEntryResponse;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleEntryRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderAnimalRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BattleStartService {

  private final BattleRepository battleRepository;
  private final BattleEntryRepository battleEntryRepository;
  private final GymLeaderAnimalRepository gymLeaderAnimalRepository;
  private final GymLeaderQueryService gymLeaderQueryService;
  private final AnimalQueryService animalQueryService;
  private final AnimalRestService animalRestService;
  private final BattlePolicy battlePolicy;
  private final BattleFinalRoundService battleFinalRoundService;
  private final FileStorage fileStorage;

  @Transactional
  public BattleStartResponse start(Long userId, BattleStartRequest request, Instant now) {
    validateRequest(request);

    Optional<Battle> existing =
        battleRepository.findByUserIdAndClientRequestId(userId, request.clientRequestId());
    if (existing.isPresent()) {
      return toResponse(existing.get());
    }

    GymLeader gymLeader = gymLeaderQueryService.getGymLeaderOrThrow(request.gymLeaderId());
    GymLeaderUnlock unlock = gymLeaderQueryService.resolveUnlock(userId, gymLeader);
    if (!unlock.isUnlocked()) {
      throw new BusinessException(ErrorCode.GYM_LEADER_LOCKED);
    }

    rejectWhenBattleInProgress(userId, now);

    List<BattleEntryRequest> entries = sortedEntries(request.entries());
    Map<Long, AnimalBattleProfile> profiles =
        animalQueryService.getOwnedBattleProfiles(userId, animalIdsOf(entries));
    if (profiles.size() != BattlePolicy.ENTRY_COUNT) {
      throw new BusinessException(ErrorCode.ANIMAL_NOT_FOUND);
    }

    List<GymLeaderAnimal> gymLeaderAnimals =
        gymLeaderAnimalRepository.findByGymLeaderIdOrderByOrderNoAsc(gymLeader.getId());
    if (gymLeaderAnimals.size() != BattlePolicy.ENTRY_COUNT) {
      throw new BusinessException(ErrorCode.GYM_LEADER_NOT_FOUND);
    }

    List<Long> captureIds =
        entries.stream().map(entry -> profiles.get(entry.animalId()).captureId()).toList();
    Instant restEndsAt = now.plus(battlePolicy.restDuration());
    animalRestService.reserveRestByCaptureIds(captureIds, restEndsAt, now);

    Battle battle =
        battleRepository.save(
            Battle.start(userId, gymLeader.getId(), request.clientRequestId(), now));

    List<BattleEntry> battleEntries = new ArrayList<>();
    for (BattleEntryRequest entry : entries) {
      AnimalBattleProfile profile = profiles.get(entry.animalId());
      battleEntries.add(
          BattleEntry.ofUser(
              battle.getId(),
              entry.orderNo(),
              profile.captureId(),
              AnimalName.from(profile.animalName()),
              profile.cardType(),
              profile.tier(),
              profile.skill1(),
              profile.skill2()));
    }
    for (GymLeaderAnimal gymLeaderAnimal : gymLeaderAnimals) {
      battleEntries.add(BattleEntry.ofNpc(battle.getId(), gymLeaderAnimal));
    }
    battleEntryRepository.saveAll(battleEntries);

    return toResponse(battle);
  }

  private void validateRequest(BattleStartRequest request) {
    if (request == null
        || request.gymLeaderId() == null
        || request.clientRequestId() == null
        || request.clientRequestId().isBlank()
        || request.entries() == null
        || request.entries().size() != BattlePolicy.ENTRY_COUNT) {
      throw new BusinessException(ErrorCode.INVALID_BATTLE_ENTRY);
    }
    Set<Long> animalIds = new HashSet<>();
    Set<Integer> orderNos = new HashSet<>();
    for (BattleEntryRequest entry : request.entries()) {
      if (entry == null || entry.animalId() == null || entry.orderNo() == null) {
        throw new BusinessException(ErrorCode.INVALID_BATTLE_ENTRY);
      }
      if (entry.orderNo() < 1 || entry.orderNo() > BattlePolicy.ENTRY_COUNT) {
        throw new BusinessException(ErrorCode.INVALID_BATTLE_ENTRY);
      }
      if (!animalIds.add(entry.animalId()) || !orderNos.add(entry.orderNo())) {
        throw new BusinessException(ErrorCode.INVALID_BATTLE_ENTRY);
      }
    }
  }

  private void rejectWhenBattleInProgress(Long userId, Instant now) {
    battleRepository
        .findFirstByUserIdAndStatusOrderByStartedAtDesc(userId, BattleStatus.IN_PROGRESS)
        .ifPresent(
            battle -> {
              battleFinalRoundService.finishWhenTimedOut(battle, now);
              if (!battle.isInProgress()) {
                battleRepository.saveAndFlush(battle);
                return;
              }
              if (battle.isExpiredAt(now, battlePolicy.abandonThreshold())) {
                battle.abandon(now);
                battleRepository.saveAndFlush(battle);
                return;
              }
              throw new BusinessException(ErrorCode.BATTLE_ALREADY_IN_PROGRESS);
            });
  }

  private List<BattleEntryRequest> sortedEntries(List<BattleEntryRequest> entries) {
    return entries.stream().sorted(Comparator.comparing(BattleEntryRequest::orderNo)).toList();
  }

  private List<Long> animalIdsOf(List<BattleEntryRequest> entries) {
    return entries.stream().map(BattleEntryRequest::animalId).toList();
  }

  private BattleStartResponse toResponse(Battle battle) {
    List<BattleEntry> entries =
        battleEntryRepository.findByBattleIdOrderBySideAscOrderNoAsc(battle.getId());
    BattleEntry userEntry = firstEntry(entries, BattleSide.USER);
    BattleEntry npcEntry = firstEntry(entries, BattleSide.NPC);

    List<Long> captureIds = userCaptureIds(entries);
    Map<Long, Long> animalIdByCaptureId = animalQueryService.getAnimalIdsByCaptureIds(captureIds);
    Map<Long, Instant> restEndsAtByCaptureId =
        animalQueryService.getRestEndsAtByCaptureIds(captureIds);

    List<BattleRestResponse> rests =
        captureIds.stream()
            .map(
                captureId ->
                    new BattleRestResponse(
                        animalIdByCaptureId.get(captureId), restEndsAtByCaptureId.get(captureId)))
            .toList();

    return new BattleStartResponse(
        battle.getId(),
        battle.getGymLeaderId(),
        battle.getBarPosition(),
        BattlePolicy.MIN_BAR_POSITION,
        BattlePolicy.MAX_BAR_POSITION,
        toUserEntryResponse(userEntry, animalIdByCaptureId.get(userEntry.getCaptureId())),
        toNpcEntryResponse(npcEntry),
        rests);
  }

  private List<Long> userCaptureIds(List<BattleEntry> entries) {
    return entries.stream()
        .filter(BattleEntry::isUserSide)
        .sorted(Comparator.comparing(BattleEntry::getOrderNo))
        .map(BattleEntry::getCaptureId)
        .toList();
  }

  private BattleEntry firstEntry(List<BattleEntry> entries, BattleSide side) {
    return entries.stream()
        .filter(entry -> entry.getSide() == side)
        .min(Comparator.comparing(BattleEntry::getOrderNo))
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_BATTLE_ENTRY));
  }

  private BattleUserEntryResponse toUserEntryResponse(BattleEntry entry, Long animalId) {
    return new BattleUserEntryResponse(
        animalId,
        entry.getOrderNo(),
        entry.getAnimalName(),
        entry.getCardType(),
        entry.getTier(),
        toSkillResponse(entry.getSkill1()),
        toSkillResponse(entry.getSkill2()));
  }

  private BattleNpcEntryResponse toNpcEntryResponse(BattleEntry entry) {
    GymLeaderAnimal gymLeaderAnimal =
        gymLeaderAnimalRepository.findById(entry.getGymLeaderAnimalId()).orElse(null);
    return new BattleNpcEntryResponse(
        entry.getOrderNo(),
        entry.getAnimalName(),
        entry.getCardType(),
        entry.getTier(),
        gymLeaderAnimal == null || gymLeaderAnimal.getImageKey() == null
            ? null
            : fileStorage.buildUrl(gymLeaderAnimal.getImageKey()));
  }

  private BattleSkillResponse toSkillResponse(CardSkill skill) {
    return new BattleSkillResponse(
        skill.displayName(),
        skill.battleType(),
        battlePolicy.skillTriggerPercentage(skill.battleType()),
        battlePolicy.skillMoveDistance(skill.battleType()));
  }
}
