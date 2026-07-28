package com.somagochi.pochakfarm.capture.domain;

import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
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
    name = "captures",
    indexes = @Index(name = "idx_captures_user_id_created_at", columnList = "user_id, created_at"),
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_captures_user_id_client_request_id",
            columnNames = {"user_id", "client_request_id"}))
@SQLRestriction("deleted_at is null")
public class Capture extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "client_request_id", nullable = false, updatable = false, length = 36)
  private String clientRequestId;

  @Column(name = "original_image_key", nullable = false, updatable = false)
  private String originalImageKey;

  @Column(name = "original_image_content_type", nullable = false, updatable = false)
  private String originalImageContentType;

  @Column(name = "game_result_expires_at", nullable = false, updatable = false)
  private Instant gameResultExpiresAt;

  @Column(name = "animal_name")
  private String animalName;

  @Enumerated(EnumType.STRING)
  @Column(name = "card_type", nullable = false, updatable = false)
  private CardType cardType;

  @Enumerated(EnumType.STRING)
  @Column(name = "tier", nullable = false, updatable = false)
  private Tier tier;

  @Enumerated(EnumType.STRING)
  @Column(name = "skill_1")
  private CardSkill skill1;

  @Enumerated(EnumType.STRING)
  @Column(name = "skill_2")
  private CardSkill skill2;

  @Column(name = "card_no")
  private String cardNo;

  @Column(name = "card_image")
  private String cardImage;

  @Column(name = "animal_image")
  private String animalImage;

  @Enumerated(EnumType.STRING)
  @Column(name = "generation_status", nullable = false)
  private GenerationStatus generationStatus;

  @Column(name = "elapsed_ms")
  private Integer elapsedMs;

  @Column(name = "failure_reason")
  private String failureReason;

  @Enumerated(EnumType.STRING)
  @Column(name = "game_status", nullable = false)
  private GameStatus gameStatus;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  private Capture(
      Long userId,
      String clientRequestId,
      CardType cardType,
      Tier tier,
      String originalImageKey,
      String originalImageContentType,
      Instant gameResultExpiresAt) {
    this.userId = Objects.requireNonNull(userId);
    this.clientRequestId = Objects.requireNonNull(clientRequestId);
    this.cardType = Objects.requireNonNull(cardType);
    this.tier = Objects.requireNonNull(tier);
    this.originalImageKey = Objects.requireNonNull(originalImageKey);
    this.originalImageContentType = Objects.requireNonNull(originalImageContentType);
    this.gameResultExpiresAt = Objects.requireNonNull(gameResultExpiresAt);
    this.generationStatus = GenerationStatus.WAITING_UPLOAD;
    this.gameStatus = GameStatus.PENDING;
  }

  public static Capture create(
      Long userId,
      String clientRequestId,
      CardType cardType,
      Tier tier,
      String originalImageKey,
      String originalImageContentType,
      Instant gameResultExpiresAt) {
    return new Capture(
        userId,
        clientRequestId,
        cardType,
        tier,
        originalImageKey,
        originalImageContentType,
        gameResultExpiresAt);
  }
}
