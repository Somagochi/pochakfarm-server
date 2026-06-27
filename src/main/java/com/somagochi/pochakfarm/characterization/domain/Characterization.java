package com.somagochi.pochakfarm.characterization.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "characterizations")
public class Characterization extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "device_id", nullable = false, updatable = false)
  private Long deviceId;

  @Column(name = "animal_name", nullable = false)
  private String animalName;

  @Column(name = "original_image_key")
  private String originalImageKey;

  @Column(name = "result_image_key")
  private String resultImageKey;

  @Column(name = "provider")
  private String provider;

  @Column(name = "fallback_from")
  private String fallbackFrom;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private CharacterizationStatus status;

  @Column(name = "elapsed_ms")
  private Integer elapsedMs;

  @Column(name = "failure_reason")
  private String failureReason;

  private Characterization(Long deviceId, String animalName) {
    this.deviceId = deviceId;
    this.animalName = animalName;
    this.status = CharacterizationStatus.PROCESSING;
  }

  public static Characterization start(Long deviceId, String animalName) {
    return new Characterization(deviceId, animalName);
  }

  public void originalUploaded(String originalImageKey) {
    this.originalImageKey = originalImageKey;
  }

  public void succeed(
      String resultImageKey, String provider, String fallbackFrom, Integer elapsedMs) {
    this.resultImageKey = resultImageKey;
    this.provider = provider;
    this.fallbackFrom = fallbackFrom;
    this.elapsedMs = elapsedMs;
    this.status = CharacterizationStatus.SUCCEEDED;
    this.failureReason = null;
  }

  public void fail(String failureReason) {
    this.status = CharacterizationStatus.FAILED;
    this.failureReason = failureReason;
  }
}
