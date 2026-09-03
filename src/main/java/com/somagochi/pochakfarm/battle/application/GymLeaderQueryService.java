package com.somagochi.pochakfarm.battle.application;

import com.somagochi.pochakfarm.badge.application.BadgeQueryService;
import com.somagochi.pochakfarm.battle.domain.GymLeader;
import com.somagochi.pochakfarm.battle.domain.GymLeaderAnimal;
import com.somagochi.pochakfarm.battle.domain.GymLeaderUnlock;
import com.somagochi.pochakfarm.battle.domain.GymLeaderUnlockResolver;
import com.somagochi.pochakfarm.battle.dto.GymLeaderAnimalResponse;
import com.somagochi.pochakfarm.battle.dto.GymLeaderDetailResponse;
import com.somagochi.pochakfarm.battle.dto.GymLeaderProfileResponse;
import com.somagochi.pochakfarm.battle.dto.GymLeaderResponse;
import com.somagochi.pochakfarm.battle.dto.GymLeaderUnlockResponse;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderAnimalRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import com.somagochi.pochakfarm.user.application.UserQueryService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GymLeaderQueryService {

  private final GymLeaderRepository gymLeaderRepository;
  private final GymLeaderAnimalRepository gymLeaderAnimalRepository;
  private final GymLeaderUnlockResolver gymLeaderUnlockResolver;
  private final BadgeQueryService badgeQueryService;
  private final UserQueryService userQueryService;
  private final FileStorage fileStorage;

  @Transactional(readOnly = true)
  public List<GymLeaderResponse> getGymLeaders(Long userId) {
    List<GymLeader> gymLeaders = gymLeaderRepository.findAllByOrderByChallengeOrderAsc();
    int userLevel = userQueryService.getLevel(userId);
    Set<String> ownedBadgeCodes = badgeQueryService.findOwnedBadgeCodes(userId);

    return gymLeaders.stream()
        .map(
            gymLeader ->
                toListResponse(
                    gymLeader, previousOf(gymLeaders, gymLeader), userLevel, ownedBadgeCodes))
        .toList();
  }

  @Transactional(readOnly = true)
  public GymLeaderDetailResponse getGymLeader(Long userId, Long gymLeaderId) {
    GymLeader gymLeader = getGymLeaderOrThrow(gymLeaderId);
    int userLevel = userQueryService.getLevel(userId);
    Set<String> ownedBadgeCodes = badgeQueryService.findOwnedBadgeCodes(userId);

    List<GymLeaderAnimalResponse> animals =
        gymLeaderAnimalRepository.findByGymLeaderIdOrderByOrderNoAsc(gymLeader.getId()).stream()
            .map(this::toAnimalResponse)
            .toList();

    return new GymLeaderDetailResponse(
        toProfileResponse(gymLeader, findPrevious(gymLeader), userLevel, ownedBadgeCodes), animals);
  }

  @Transactional(readOnly = true)
  public GymLeader getGymLeaderOrThrow(Long gymLeaderId) {
    return gymLeaderRepository
        .findById(gymLeaderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.GYM_LEADER_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public GymLeaderUnlock resolveUnlock(Long userId, GymLeader gymLeader) {
    return gymLeaderUnlockResolver.resolve(
        gymLeader,
        findPrevious(gymLeader),
        userQueryService.getLevel(userId),
        badgeQueryService.findOwnedBadgeCodes(userId));
  }

  private GymLeader findPrevious(GymLeader gymLeader) {
    if (gymLeader.getChallengeOrder() == GymLeader.FIRST_CHALLENGE_ORDER) {
      return null;
    }
    return gymLeaderRepository
        .findByChallengeOrder(gymLeader.getChallengeOrder() - 1)
        .orElseThrow(() -> new BusinessException(ErrorCode.GYM_LEADER_NOT_FOUND));
  }

  private GymLeader previousOf(List<GymLeader> gymLeaders, GymLeader gymLeader) {
    int index = gymLeaders.indexOf(gymLeader);
    return index <= 0 ? null : gymLeaders.get(index - 1);
  }

  private GymLeaderResponse toListResponse(
      GymLeader gymLeader,
      GymLeader previousGymLeader,
      int userLevel,
      Set<String> ownedBadgeCodes) {
    GymLeaderUnlock unlock =
        gymLeaderUnlockResolver.resolve(gymLeader, previousGymLeader, userLevel, ownedBadgeCodes);
    return new GymLeaderResponse(
        gymLeader.getId(),
        gymLeader.getName(),
        gymLeader.getChallengeOrder(),
        buildUrlOrNull(gymLeader.getThumbnailKey()),
        ownedBadgeCodes.contains(gymLeader.getBadgeCode()),
        unlock.isUnlocked());
  }

  private GymLeaderProfileResponse toProfileResponse(
      GymLeader gymLeader,
      GymLeader previousGymLeader,
      int userLevel,
      Set<String> ownedBadgeCodes) {
    GymLeaderUnlock unlock =
        gymLeaderUnlockResolver.resolve(gymLeader, previousGymLeader, userLevel, ownedBadgeCodes);
    return new GymLeaderProfileResponse(
        gymLeader.getId(),
        gymLeader.getCode(),
        gymLeader.getName(),
        gymLeader.getChallengeOrder(),
        buildUrlOrNull(gymLeader.getImageKey()),
        gymLeader.getBadgeCode(),
        ownedBadgeCodes.contains(gymLeader.getBadgeCode()),
        GymLeaderUnlockResponse.of(
            unlock.requiredLevel(),
            unlock.levelSatisfied(),
            unlock.previousBadgeCode(),
            unlock.previousBadgeSatisfied()));
  }

  private GymLeaderAnimalResponse toAnimalResponse(GymLeaderAnimal gymLeaderAnimal) {
    return new GymLeaderAnimalResponse(
        gymLeaderAnimal.getOrderNo(),
        gymLeaderAnimal.getAnimalName(),
        gymLeaderAnimal.getCardType(),
        gymLeaderAnimal.getTier(),
        buildUrlOrNull(gymLeaderAnimal.getImageKey()));
  }

  private String buildUrlOrNull(String key) {
    return key == null ? null : fileStorage.buildUrl(key);
  }
}
