package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.application.reward.RewardGranterResolver;
import com.somagochi.pochakfarm.achievement.domain.Achievement;
import com.somagochi.pochakfarm.achievement.domain.AchievementReward;
import com.somagochi.pochakfarm.achievement.domain.UserAchievement;
import com.somagochi.pochakfarm.achievement.dto.AchievementClaimResponse;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.AchievementRepository;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.UserAchievementRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.user.application.UserQueryService;
import com.somagochi.pochakfarm.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AchievementClaimService {

  private final AchievementRepository achievementRepository;
  private final UserAchievementRepository userAchievementRepository;
  private final AchievementStatsLoader achievementStatsLoader;
  private final AchievementRewardCatalog achievementRewardCatalog;
  private final RewardGranterResolver rewardGranterResolver;
  private final UserQueryService userQueryService;
  private final Clock clock;

  @Transactional
  public AchievementClaimResponse claim(Long userId, String code) {
    Achievement achievement =
        achievementRepository
            .findByCode(code)
            .filter(Achievement::isDefinitionValid)
            .orElseThrow(() -> new BusinessException(ErrorCode.ACHIEVEMENT_NOT_FOUND));

    UserAchievement record =
        userAchievementRepository
            .findByUserIdAndAchievementId(userId, achievement.getId())
            .orElseGet(() -> recordAchieved(userId, achievement));

    if (userAchievementRepository.markClaimed(record.getId(), Instant.now(clock)) == 0) {
      throw new BusinessException(ErrorCode.ACHIEVEMENT_REWARD_ALREADY_CLAIMED);
    }

    List<AchievementReward> rewards =
        achievementRewardCatalog.findByAchievementId(achievement.getId());
    User user = userQueryService.getForUpdate(userId);
    rewards.forEach(reward -> rewardGranterResolver.grant(user, reward));

    return AchievementClaimResponse.of(
        achievement.getCode(), achievementRewardCatalog.describe(rewards), user);
  }

  private UserAchievement recordAchieved(Long userId, Achievement achievement) {
    if (!achievement.isEnabled()
        || !achievement.isSatisfiedBy(achievementStatsLoader.load(userId))) {
      throw new BusinessException(ErrorCode.ACHIEVEMENT_NOT_ACHIEVED);
    }
    try {
      return userAchievementRepository.saveAndFlush(
          UserAchievement.achieve(userId, achievement.getId()));
    } catch (DataIntegrityViolationException e) {
      throw new BusinessException(ErrorCode.ACHIEVEMENT_REWARD_ALREADY_CLAIMED);
    }
  }
}
