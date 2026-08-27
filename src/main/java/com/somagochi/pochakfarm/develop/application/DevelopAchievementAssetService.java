package com.somagochi.pochakfarm.develop.application;

import com.somagochi.pochakfarm.achievement.domain.Achievement;
import com.somagochi.pochakfarm.achievement.domain.AchievementReward;
import com.somagochi.pochakfarm.achievement.domain.RewardType;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.AchievementRepository;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.AchievementRewardRepository;
import com.somagochi.pochakfarm.badge.domain.Badge;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.BadgeRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.develop.dto.DevelopAchievementAssetView;
import com.somagochi.pochakfarm.develop.dto.DevelopAchievementRewardView;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.util.Comparator;
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
public class DevelopAchievementAssetService {

  private final AchievementRepository achievementRepository;
  private final AchievementRewardRepository achievementRewardRepository;
  private final BadgeRepository badgeRepository;
  private final ImageUploadService imageUploadService;
  private final FileStorage fileStorage;

  @Transactional(readOnly = true)
  public List<DevelopAchievementAssetView> getAssets() {
    List<Achievement> achievements =
        achievementRepository.findAll().stream()
            .sorted(Comparator.comparing(Achievement::getId))
            .toList();
    Map<Long, List<AchievementReward>> rewardsByAchievementId =
        findRewardsByAchievementId(achievements.stream().map(Achievement::getId).toList());
    Map<String, Badge> badgeByCode = findBadgesByCode(rewardsByAchievementId);
    return achievements.stream()
        .map(
            achievement ->
                DevelopAchievementAssetView.of(
                    achievement,
                    buildUrlOrNull(achievement.getUnachievedImageKey()),
                    buildUrlOrNull(achievement.getAchievedImageKey()),
                    toRewardViews(
                        rewardsByAchievementId.getOrDefault(achievement.getId(), List.of()),
                        badgeByCode)))
        .toList();
  }

  @Transactional
  public void updateAchievementImages(
      Long achievementId,
      String unachievedImageKey,
      String unachievedImageContentType,
      String achievedImageKey,
      String achievedImageContentType) {
    Achievement achievement =
        achievementRepository
            .findById(achievementId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ACHIEVEMENT_NOT_FOUND));
    if (StringUtils.hasText(unachievedImageKey)) {
      imageUploadService.validatePublicObject(unachievedImageKey, unachievedImageContentType);
      achievement.changeUnachievedImageKey(unachievedImageKey);
    }
    if (StringUtils.hasText(achievedImageKey)) {
      imageUploadService.validatePublicObject(achievedImageKey, achievedImageContentType);
      achievement.changeAchievedImageKey(achievedImageKey);
    }
  }

  @Transactional
  public void updateRewardAmount(Long rewardId, long amount) {
    AchievementReward reward = findReward(rewardId);
    if (reward.getRewardType() == RewardType.BADGE || amount <= 0) {
      throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
    reward.changeAmount(amount);
  }

  @Transactional
  public void updateRewardBadgeImage(
      Long rewardId, String badgeImageKey, String badgeImageContentType) {
    AchievementReward reward = findReward(rewardId);
    if (reward.getRewardType() != RewardType.BADGE) {
      throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
    Badge badge =
        badgeRepository
            .findByCode(reward.getReferenceCode())
            .orElseThrow(() -> new BusinessException(ErrorCode.BADGE_NOT_FOUND));
    imageUploadService.validatePublicObject(badgeImageKey, badgeImageContentType);
    badge.changeImageKey(badgeImageKey);
  }

  private AchievementReward findReward(Long rewardId) {
    return achievementRewardRepository
        .findById(rewardId)
        .orElseThrow(() -> new BusinessException(ErrorCode.ACHIEVEMENT_NOT_FOUND));
  }

  private List<DevelopAchievementRewardView> toRewardViews(
      List<AchievementReward> rewards, Map<String, Badge> badgeByCode) {
    return rewards.stream()
        .sorted(Comparator.comparing(AchievementReward::getId))
        .map(
            reward -> {
              Badge badge =
                  reward.getReferenceCode() == null
                      ? null
                      : badgeByCode.get(reward.getReferenceCode());
              return DevelopAchievementRewardView.of(
                  reward, badge, badge == null ? null : buildUrlOrNull(badge.getImageKey()));
            })
        .toList();
  }

  private Map<Long, List<AchievementReward>> findRewardsByAchievementId(List<Long> achievementIds) {
    if (achievementIds.isEmpty()) {
      return Map.of();
    }
    return achievementRewardRepository.findByAchievementIdIn(achievementIds).stream()
        .collect(Collectors.groupingBy(AchievementReward::getAchievementId));
  }

  private Map<String, Badge> findBadgesByCode(
      Map<Long, List<AchievementReward>> rewardsByAchievementId) {
    List<String> badgeCodes =
        rewardsByAchievementId.values().stream()
            .flatMap(List::stream)
            .filter(reward -> reward.getRewardType() == RewardType.BADGE)
            .map(AchievementReward::getReferenceCode)
            .filter(StringUtils::hasText)
            .distinct()
            .toList();
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
