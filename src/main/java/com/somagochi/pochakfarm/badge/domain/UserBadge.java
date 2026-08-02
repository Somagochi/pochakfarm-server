package com.somagochi.pochakfarm.badge.domain;

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
    name = "user_badges",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_user_badges_user_badge",
            columnNames = {"user_id", "badge_id"}))
@SQLRestriction("deleted_at is null")
public class UserBadge extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  @Column(name = "badge_id", nullable = false, updatable = false)
  private Long badgeId;

  private UserBadge(Long userId, Long badgeId) {
    this.userId = Objects.requireNonNull(userId);
    this.badgeId = Objects.requireNonNull(badgeId);
  }

  public static UserBadge acquire(Long userId, Long badgeId) {
    return new UserBadge(userId, badgeId);
  }

  public Instant getAcquiredAt() {
    return getCreatedAt();
  }
}
