package com.somagochi.pochakfarm.characterization.domain;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "characterizations",
    indexes =
        @Index(name = "idx_characterizations_status_created_at", columnList = "status, created_at"))
public class Characterization extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "device_id", nullable = false, updatable = false)
  private Long deviceId;

  @Column(name = "animal_name", nullable = false)
  private String animalName;

  @Enumerated(EnumType.STRING)
  @Column(name = "card_type")
  private CardType cardType;

  @Column(name = "power")
  private Integer power;

  @Enumerated(EnumType.STRING)
  @Column(name = "skill_1")
  private CardSkill skill1;

  @Enumerated(EnumType.STRING)
  @Column(name = "skill_2")
  private CardSkill skill2;

  @Column(name = "card_no")
  private String cardNo;

  @Column(name = "result_image_key")
  private String resultImageKey;

  @Column(name = "provider")
  private String provider;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private CharacterizationStatus status;

  @Column(name = "elapsed_ms")
  private Integer elapsedMs;

  @Column(name = "failure_reason")
  private String failureReason;

  private Characterization(Long deviceId, String animalName, CardMetadata metadata) {
    this.deviceId = deviceId;
    this.animalName = animalName;
    this.cardType = metadata.cardType();
    this.power = metadata.power();
    this.skill1 = metadata.skill1();
    this.skill2 = metadata.skill2();
    this.cardNo = metadata.cardNo();
    this.status = CharacterizationStatus.PROCESSING;
  }

  public static Characterization start(Long deviceId, String animalName, CardMetadata metadata) {
    return new Characterization(deviceId, animalName, metadata);
  }

  public void cardNoAssigned(String cardNo) {
    this.cardNo = cardNo;
  }

  public void succeed(String resultImageKey, String provider, Integer elapsedMs) {
    this.resultImageKey = resultImageKey;
    this.provider = provider;
    this.elapsedMs = elapsedMs;
    this.status = CharacterizationStatus.SUCCEEDED;
    this.failureReason = null;
  }

  public void fail(String failureReason) {
    this.status = CharacterizationStatus.FAILED;
    this.failureReason = failureReason;
  }
}
