package com.somagochi.pochakfarm.develop.application;

import com.somagochi.pochakfarm.badge.domain.Badge;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.BadgeRepository;
import com.somagochi.pochakfarm.battle.domain.GymLeader;
import com.somagochi.pochakfarm.battle.domain.GymLeaderAnimal;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderAnimalRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.develop.dto.DevelopGymLeaderAnimalView;
import com.somagochi.pochakfarm.develop.dto.DevelopGymLeaderAssetView;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class DevelopGymLeaderAssetService {

  private final GymLeaderRepository gymLeaderRepository;
  private final GymLeaderAnimalRepository gymLeaderAnimalRepository;
  private final BadgeRepository badgeRepository;
  private final ImageUploadService imageUploadService;
  private final FileStorage fileStorage;

  @Transactional(readOnly = true)
  public List<DevelopGymLeaderAssetView> getAssets() {
    List<GymLeader> gymLeaders = gymLeaderRepository.findAllByOrderByChallengeOrderAsc();
    Map<Long, List<GymLeaderAnimal>> animalsByGymLeaderId =
        findAnimalsByGymLeaderId(gymLeaders.stream().map(GymLeader::getId).toList());
    Map<String, Badge> badgeByCode =
        findBadgesByCode(gymLeaders.stream().map(GymLeader::getBadgeCode).distinct().toList());
    return gymLeaders.stream()
        .map(gymLeader -> toView(gymLeader, animalsByGymLeaderId, badgeByCode))
        .toList();
  }

  @Transactional
  public void updateGymLeaderImages(
      Long gymLeaderId,
      String thumbnailKey,
      String thumbnailContentType,
      String imageKey,
      String imageContentType) {
    GymLeader gymLeader = findGymLeader(gymLeaderId);
    if (StringUtils.hasText(thumbnailKey)) {
      imageUploadService.validatePublicObject(thumbnailKey, thumbnailContentType);
      gymLeader.changeThumbnailKey(thumbnailKey);
    }
    if (StringUtils.hasText(imageKey)) {
      imageUploadService.validatePublicObject(imageKey, imageContentType);
      gymLeader.changeImageKey(imageKey);
    }
  }

  @Transactional
  public void updateGymLeaderAnimalImage(
      Long gymLeaderAnimalId, String imageKey, String imageContentType) {
    GymLeaderAnimal gymLeaderAnimal =
        gymLeaderAnimalRepository
            .findById(gymLeaderAnimalId)
            .orElseThrow(() -> new BusinessException(ErrorCode.GYM_LEADER_NOT_FOUND));
    imageUploadService.validatePublicObject(imageKey, imageContentType);
    gymLeaderAnimal.changeImageKey(imageKey);
  }

  @Transactional
  public void updateGymLeaderBadgeImage(
      Long gymLeaderId, String badgeImageKey, String badgeImageContentType) {
    GymLeader gymLeader = findGymLeader(gymLeaderId);
    Badge badge =
        badgeRepository
            .findByCode(gymLeader.getBadgeCode())
            .orElseThrow(() -> new BusinessException(ErrorCode.BADGE_NOT_FOUND));
    imageUploadService.validatePublicObject(badgeImageKey, badgeImageContentType);
    badge.changeImageKey(badgeImageKey);
  }

  private GymLeader findGymLeader(Long gymLeaderId) {
    return gymLeaderRepository
        .findById(gymLeaderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.GYM_LEADER_NOT_FOUND));
  }

  private DevelopGymLeaderAssetView toView(
      GymLeader gymLeader,
      Map<Long, List<GymLeaderAnimal>> animalsByGymLeaderId,
      Map<String, Badge> badgeByCode) {
    Badge badge = badgeByCode.get(gymLeader.getBadgeCode());
    List<DevelopGymLeaderAnimalView> animals =
        animalsByGymLeaderId.getOrDefault(gymLeader.getId(), List.of()).stream()
            .map(
                animal ->
                    DevelopGymLeaderAnimalView.of(animal, buildUrlOrNull(animal.getImageKey())))
            .toList();
    return DevelopGymLeaderAssetView.of(
        gymLeader,
        buildUrlOrNull(gymLeader.getThumbnailKey()),
        buildUrlOrNull(gymLeader.getImageKey()),
        badge == null ? null : badge.getName(),
        badge == null ? null : buildUrlOrNull(badge.getImageKey()),
        animals);
  }

  private Map<Long, List<GymLeaderAnimal>> findAnimalsByGymLeaderId(List<Long> gymLeaderIds) {
    if (gymLeaderIds.isEmpty()) {
      return Map.of();
    }
    return gymLeaderAnimalRepository
        .findByGymLeaderIdInOrderByGymLeaderIdAscOrderNoAsc(gymLeaderIds)
        .stream()
        .collect(Collectors.groupingBy(GymLeaderAnimal::getGymLeaderId));
  }

  private Map<String, Badge> findBadgesByCode(List<String> badgeCodes) {
    if (badgeCodes.isEmpty()) {
      return Map.of();
    }
    return badgeRepository.findByCodeIn(badgeCodes).stream()
        .collect(Collectors.toMap(Badge::getCode, Function.identity()));
  }

  private String buildUrlOrNull(String key) {
    return key == null ? null : fileStorage.buildUrl(key);
  }
}
