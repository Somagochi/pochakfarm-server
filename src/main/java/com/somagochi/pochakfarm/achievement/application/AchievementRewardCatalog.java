package com.somagochi.pochakfarm.achievement.application;

import com.somagochi.pochakfarm.achievement.domain.AchievementReward;
import com.somagochi.pochakfarm.achievement.domain.RewardType;
import com.somagochi.pochakfarm.achievement.dto.AchievementRewardResponse;
import com.somagochi.pochakfarm.achievement.infrastructure.persistence.AchievementRewardRepository;
import com.somagochi.pochakfarm.badge.application.BadgeQueryService;
import com.somagochi.pochakfarm.badge.dto.BadgeResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AchievementRewardCatalog {

  private final AchievementRewardRepository achievementRewardRepository;
  private final BadgeQueryService badgeQueryService;

  @Transactional(readOnly = true)
  public List<AchievementReward> findByAchievementId(Long achievementId) {
    return validRewards(achievementRewardRepository.findByAchievementId(achievementId));
  }

  @Transactional(readOnly = true)
  public Map<Long, List<AchievementRewardResponse>> describeByAchievementIds(
      Collection<Long> achievementIds) {
    if (achievementIds.isEmpty()) {
      return Map.of();
    }
    List<AchievementReward> rewards =
        validRewards(achievementRewardRepository.findByAchievementIdIn(achievementIds));
    Map<String, BadgeResponse> badges = findBadges(rewards);
    Map<Long, List<AchievementRewardResponse>> byAchievementId = new HashMap<>();
    for (AchievementReward reward : rewards) {
      BadgeResponse badge = badgeOf(badges, reward);
      if (reward.getRewardType() == RewardType.BADGE && badge == null) {
        log.warn(
            "존재하지 않는 뱃지를 참조하는 보상 제외 rewardId={} referenceCode={}",
            reward.getId(),
            reward.getReferenceCode());
        continue;
      }
      byAchievementId
          .computeIfAbsent(reward.getAchievementId(), id -> new ArrayList<>())
          .add(AchievementRewardResponse.of(reward, badge));
    }
    return byAchievementId;
  }

  @Transactional(readOnly = true)
  public List<AchievementRewardResponse> describe(Collection<AchievementReward> rewards) {
    Map<String, BadgeResponse> badges = findBadges(rewards);
    return rewards.stream()
        .map(reward -> AchievementRewardResponse.of(reward, badgeOf(badges, reward)))
        .toList();
  }

  private List<AchievementReward> validRewards(List<AchievementReward> rewards) {
    List<AchievementReward> valid = new ArrayList<>();
    for (AchievementReward reward : rewards) {
      if (reward.isDefinitionValid()) {
        valid.add(reward);
      } else {
        log.warn(
            "유효하지 않은 보상 정의 제외 rewardId={} achievementId={} rewardType={}",
            reward.getId(),
            reward.getAchievementId(),
            reward.getRewardType());
      }
    }
    return valid;
  }

  private Map<String, BadgeResponse> findBadges(Collection<AchievementReward> rewards) {
    Set<String> badgeCodes =
        rewards.stream()
            .filter(reward -> reward.getRewardType() == RewardType.BADGE)
            .map(AchievementReward::getReferenceCode)
            .collect(Collectors.toSet());
    return badgeQueryService.findAllByCodes(badgeCodes);
  }

  private BadgeResponse badgeOf(Map<String, BadgeResponse> badges, AchievementReward reward) {
    return reward.getReferenceCode() == null ? null : badges.get(reward.getReferenceCode());
  }
}
