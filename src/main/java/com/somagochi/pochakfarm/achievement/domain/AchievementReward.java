package com.somagochi.pochakfarm.achievement.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "achievement_rewards",
    indexes =
        @Index(name = "idx_achievement_rewards_achievement_id", columnList = "achievement_id"))
@SQLRestriction("deleted_at is null")
public class AchievementReward extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "achievement_id", nullable = false, updatable = false)
  private Long achievementId;

  @Enumerated(EnumType.STRING)
  @Column(name = "reward_type", nullable = false, length = 32)
  private RewardType rewardType;

  @Column(name = "reference_code", length = 64)
  private String referenceCode;

  @Column(name = "amount")
  private Long amount;

  private AchievementReward(
      Long achievementId, RewardType rewardType, String referenceCode, Long amount) {
    this.achievementId = Objects.requireNonNull(achievementId);
    this.rewardType = Objects.requireNonNull(rewardType);
    this.referenceCode = referenceCode;
    this.amount = amount;
  }

  public static AchievementReward ofCoin(Long achievementId, long amount) {
    return new AchievementReward(achievementId, RewardType.COIN, null, amount);
  }

  public static AchievementReward ofExperience(Long achievementId, long amount) {
    return new AchievementReward(achievementId, RewardType.EXPERIENCE, null, amount);
  }

  public static AchievementReward ofBadge(Long achievementId, String badgeCode) {
    return new AchievementReward(achievementId, RewardType.BADGE, badgeCode, null);
  }

  public boolean isDefinitionValid() {
    return rewardType.supports(referenceCode, amount);
  }
}
