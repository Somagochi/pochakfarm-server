package com.somagochi.pochakfarm.capture.domain;

import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

  @Embedded private AnimalName animalName;

  @Enumerated(EnumType.STRING)
  @Column(name = "card_type", nullable = false, updatable = false)
  private CardType cardType;

  @Enumerated(EnumType.STRING)
  @Column(name = "tier", nullable = false, updatable = false)
  private Tier tier;

  @Enumerated(EnumType.STRING)
  @Column(name = "skill_1", nullable = false)
  private CardSkill skill1;

  @Enumerated(EnumType.STRING)
  @Column(name = "skill_2", nullable = false)
  private CardSkill skill2;

  @Column(name = "card_no", nullable = false)
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

  @Column(name = "granted_experience")
  private Long grantedExperience;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_type", updatable = false)
  private CapturePaymentType paymentType;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  private Capture(
      Long userId,
      String clientRequestId,
      CardType cardType,
      Tier tier,
      AnimalName animalName,
      CardSkill skill1,
      CardSkill skill2,
      String cardNo,
      String originalImageKey,
      String originalImageContentType,
      Instant gameResultExpiresAt,
      CapturePaymentType paymentType) {
    this.userId = Objects.requireNonNull(userId);
    this.clientRequestId = Objects.requireNonNull(clientRequestId);
    this.cardType = Objects.requireNonNull(cardType);
    this.tier = Objects.requireNonNull(tier);
    this.animalName = Objects.requireNonNull(animalName);
    this.skill1 = Objects.requireNonNull(skill1);
    this.skill2 = Objects.requireNonNull(skill2);
    this.cardNo = Objects.requireNonNull(cardNo);
    this.originalImageKey = Objects.requireNonNull(originalImageKey);
    this.originalImageContentType = Objects.requireNonNull(originalImageContentType);
    this.gameResultExpiresAt = Objects.requireNonNull(gameResultExpiresAt);
    this.paymentType = Objects.requireNonNull(paymentType);
    this.generationStatus = GenerationStatus.WAITING_UPLOAD;
    this.gameStatus = GameStatus.PENDING;
  }

  public static Capture create(
      Long userId,
      String clientRequestId,
      CardType cardType,
      Tier tier,
      AnimalName animalName,
      CardSkill skill1,
      CardSkill skill2,
      String cardNo,
      String originalImageKey,
      String originalImageContentType,
      Instant gameResultExpiresAt) {
    return create(
        userId,
        clientRequestId,
        cardType,
        tier,
        animalName,
        skill1,
        skill2,
        cardNo,
        originalImageKey,
        originalImageContentType,
        gameResultExpiresAt,
        CapturePaymentType.FREE);
  }

  public static Capture create(
      Long userId,
      String clientRequestId,
      CardType cardType,
      Tier tier,
      AnimalName animalName,
      CardSkill skill1,
      CardSkill skill2,
      String cardNo,
      String originalImageKey,
      String originalImageContentType,
      Instant gameResultExpiresAt,
      CapturePaymentType paymentType) {
    return new Capture(
        userId,
        clientRequestId,
        cardType,
        tier,
        animalName,
        skill1,
        skill2,
        cardNo,
        originalImageKey,
        originalImageContentType,
        gameResultExpiresAt,
        paymentType);
  }

  public String getAnimalName() {
    return animalName.value();
  }

  public CapturePaymentType getPaymentType() {
    return paymentType == null ? CapturePaymentType.FREE : paymentType;
  }

  public boolean isOwnedBy(Long userId) {
    return Objects.equals(this.userId, userId);
  }

  public boolean isWaitingUpload() {
    return generationStatus == GenerationStatus.WAITING_UPLOAD;
  }

  public boolean isGamePending() {
    return gameStatus == GameStatus.PENDING;
  }

  public boolean isGameResultExpired(Instant now) {
    return isGamePending() && !now.isBefore(gameResultExpiresAt);
  }

  public GameStatus gameStatusAt(Instant now) {
    return isGameResultExpired(now) ? GameStatus.EXPIRED : gameStatus;
  }

  public void expireGame() {
    if (isGamePending()) {
      this.gameStatus = GameStatus.EXPIRED;
    }
  }

  public void completeGame(GameStatus result, long grantedExperience) {
    if (!isGamePending() || (result != GameStatus.SUCCEEDED && result != GameStatus.FAILED)) {
      throw new IllegalStateException("Game result cannot be completed");
    }
    this.gameStatus = result;
    this.grantedExperience = grantedExperience;
  }

  public void markProcessing() {
    this.generationStatus = GenerationStatus.PROCESSING;
    this.failureReason = null;
  }

  public void cardNoAssigned(String cardNo) {
    this.cardNo = Objects.requireNonNull(cardNo);
  }

  public void succeed(String sceneImageKey, String cardImageKey, Integer elapsedMs) {
    this.animalImage = Objects.requireNonNull(sceneImageKey);
    this.cardImage = Objects.requireNonNull(cardImageKey);
    this.elapsedMs = elapsedMs;
    this.generationStatus = GenerationStatus.SUCCEEDED;
    this.failureReason = null;
  }

  public void fail(String failureReason) {
    this.animalImage = null;
    this.cardImage = null;
    this.generationStatus = GenerationStatus.FAILED;
    this.failureReason = failureReason;
  }
}
