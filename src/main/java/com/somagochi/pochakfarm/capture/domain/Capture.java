package com.somagochi.pochakfarm.capture.domain;

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
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "captures", indexes = @Index(name = "idx_captures_user_id", columnList = "user_id"))
@SQLRestriction("deleted_at is null")
public class Capture extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "animal_name")
  private String animalName;

  @Enumerated(EnumType.STRING)
  @Column(name = "card_type", nullable = false, updatable = false)
  private CardType cardType;

  @Enumerated(EnumType.STRING)
  @Column(name = "tier", nullable = false, updatable = false)
  private Tier tier;

  @Column(name = "skill_1")
  private Integer skill1;

  @Column(name = "skill_2")
  private Integer skill2;

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

  private Capture(Long userId, CardType cardType, Tier tier) {
    this.userId = Objects.requireNonNull(userId);
    this.cardType = Objects.requireNonNull(cardType);
    this.tier = Objects.requireNonNull(tier);
    this.generationStatus = GenerationStatus.WAITING_UPLOAD;
    this.gameStatus = GameStatus.PENDING;
  }

  public static Capture start(Long userId, CardType cardType, Tier tier) {
    return new Capture(userId, cardType, tier);
  }
}
