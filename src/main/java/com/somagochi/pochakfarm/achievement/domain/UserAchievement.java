package com.somagochi.pochakfarm.achievement.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "user_achievements",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_user_achievements_user_achievement",
            columnNames = {"user_id", "achievement_id"}))
@SQLRestriction("deleted_at is null")
public class UserAchievement extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  @Column(name = "achievement_id", nullable = false, updatable = false)
  private Long achievementId;

  @Column(name = "reward_claimed_at")
  private Instant rewardClaimedAt;

  private UserAchievement(Long userId, Long achievementId) {
    this.userId = Objects.requireNonNull(userId);
    this.achievementId = Objects.requireNonNull(achievementId);
  }

  public static UserAchievement achieve(Long userId, Long achievementId) {
    return new UserAchievement(userId, achievementId);
  }

  public Instant getAchievedAt() {
    return getCreatedAt();
  }

  public boolean isRewardClaimed() {
    return rewardClaimedAt != null;
  }
}
