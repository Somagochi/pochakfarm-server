package com.somagochi.pochakfarm.battle.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "gym_leaders",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_gym_leaders_code",
          columnNames = {"code"}),
      @UniqueConstraint(
          name = "uk_gym_leaders_challenge_order",
          columnNames = {"challenge_order"}),
      @UniqueConstraint(
          name = "uk_gym_leaders_badge_code",
          columnNames = {"badge_code"})
    })
@SQLRestriction("deleted_at is null")
public class GymLeader extends BaseEntity {

  public static final int ANIMAL_COUNT = 3;
  public static final int FIRST_CHALLENGE_ORDER = 1;
  public static final int LAST_CHALLENGE_ORDER = 8;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "code", nullable = false, updatable = false, length = 64)
  private String code;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "challenge_order", nullable = false)
  private Integer challengeOrder;

  @Column(name = "badge_code", nullable = false, length = 64)
  private String badgeCode;

  @Column(name = "thumbnail_key")
  private String thumbnailKey;

  @Column(name = "image_key")
  private String imageKey;

  private GymLeader(
      String code,
      String name,
      Integer challengeOrder,
      String badgeCode,
      String thumbnailKey,
      String imageKey) {
    this.code = Objects.requireNonNull(code);
    this.name = Objects.requireNonNull(name);
    this.challengeOrder = validateChallengeOrder(challengeOrder);
    this.badgeCode = Objects.requireNonNull(badgeCode);
    this.thumbnailKey = thumbnailKey;
    this.imageKey = imageKey;
  }

  public static GymLeader create(
      String code,
      String name,
      Integer challengeOrder,
      String badgeCode,
      String thumbnailKey,
      String imageKey) {
    return new GymLeader(code, name, challengeOrder, badgeCode, thumbnailKey, imageKey);
  }

  public void changeThumbnailKey(String thumbnailKey) {
    this.thumbnailKey = thumbnailKey;
  }

  public void changeImageKey(String imageKey) {
    this.imageKey = imageKey;
  }

  private static Integer validateChallengeOrder(Integer challengeOrder) {
    Objects.requireNonNull(challengeOrder);
    if (challengeOrder < FIRST_CHALLENGE_ORDER || challengeOrder > LAST_CHALLENGE_ORDER) {
      throw new IllegalArgumentException("Challenge order is out of range: " + challengeOrder);
    }
    return challengeOrder;
  }
}
